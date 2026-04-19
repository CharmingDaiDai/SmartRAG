package com.mtmn.smartrag.model.concurrency;

import com.mtmn.smartrag.model.config.ModelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 模型并发管理器
 * 
 * <p>使用信号量(Semaphore)控制同时进行的模型API调用数量</p>
 * <p>支持全局默认配置 + 每个模型独立配置</p>
 * 
 * <h3>配置优先级</h3>
 * <pre>
 * 1. 模型独立配置 (models.providers.xxx.llm-models[].max-concurrency)
 * 2. 全局默认配置 (models.concurrency.default-max-concurrent-llm-requests)
 * </pre>
 * 
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 获取指定模型的许可
 * try (var permit = concurrencyManager.acquireModelPermit("zhipuai@glm-4", true)) {
 *     return llmClient.chat("问题");
 * }
 * }</pre>
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-21
 */
@Slf4j
@Component
public class ModelConcurrencyManager {

    /**
     * 每个模型的信号量缓存
     * Key: modelId (格式: providerId@modelName)
     * Value: 信号量
     */
    private final Map<String, Semaphore> modelSemaphores = new ConcurrentHashMap<>();

    /**
     * 是否启用并发控制
     */
    private final boolean enabled;

    /**
     * 获取permit的超时时间(秒)
     */
    private final int acquireTimeout;

    /**
     * 全局默认LLM并发数
     */
    private final int defaultLlmConcurrency;

    /**
     * 全局默认Embedding并发数
     */
    private final int defaultEmbeddingConcurrency;

    /**
     * 模型配置属性(用于查询每个模型的并发配置)
     */
    private final ModelProperties properties;

    /**
     * 构造函数
     *
     * @param properties 模型配置属性
     */
    public ModelConcurrencyManager(ModelProperties properties) {
        this.properties = properties;
        ModelProperties.ConcurrencyConfig config = properties.getConcurrency();
        
        if (config != null && config.isEnabled()) {
            this.enabled = true;
            this.acquireTimeout = config.getAcquireTimeout();
            this.defaultLlmConcurrency = config.getDefaultMaxConcurrentLlmRequests();
            this.defaultEmbeddingConcurrency = config.getDefaultMaxConcurrentEmbeddingRequests();
            
            log.info("模型并发控制已启用: 默认LLM={}, 默认Embedding={}, Timeout={}s",
                    defaultLlmConcurrency, defaultEmbeddingConcurrency, acquireTimeout);
        } else {
            this.enabled = false;
            this.acquireTimeout = 0;
            this.defaultLlmConcurrency = 1;
            this.defaultEmbeddingConcurrency = 1;
            log.info("模型并发控制已禁用");
        }
    }

    /**
     * 获取或创建指定模型的信号量
     *
     * @param modelId 模型ID (格式: providerId@modelName)
     * @param isLlm 是否是LLM模型(true) 或 Embedding模型(false)
     * @return 信号量实例
     */
    private Semaphore getOrCreateSemaphore(String modelId, boolean isLlm) {
        return modelSemaphores.computeIfAbsent(modelId, id -> {
            int maxConcurrency = getModelConcurrency(modelId, isLlm);
            log.info("为模型 {} 创建信号量: permits={}", modelId, maxConcurrency);
            return new Semaphore(maxConcurrency, true); // fair=true,先进先出
        });
    }

    /**
     * 获取指定模型的并发配置
     *
     * @param modelId 模型ID
     * @param isLlm 是否是LLM模型
     * @return 并发数
     */
    private int getModelConcurrency(String modelId, boolean isLlm) {
        // 解析 providerId@modelName
        String[] parts = modelId.split("@");
        if (parts.length != 2) {
            log.warn("无效的模型ID格式: {}, 使用全局默认值", modelId);
            return isLlm ? defaultLlmConcurrency : defaultEmbeddingConcurrency;
        }

        String providerId = parts[0];
        String modelName = parts[1];

        // 从配置中查找模型的独立配置
        ModelProperties.ProviderConfig providerConfig = properties.getProviders().get(providerId);
        if (providerConfig == null) {
            log.warn("未找到Provider配置: {}, 使用全局默认值", providerId);
            return isLlm ? defaultLlmConcurrency : defaultEmbeddingConcurrency;
        }

        // 查找模型配置
        List<ModelProperties.ModelConfig> modelConfigs = 
                isLlm ? providerConfig.getLlmModels() : providerConfig.getEmbeddingModels();
        
        if (modelConfigs == null || modelConfigs.isEmpty()) {
            return isLlm ? defaultLlmConcurrency : defaultEmbeddingConcurrency;
        }

        // 在List中查找匹配的模型
        ModelProperties.ModelConfig modelConfig = modelConfigs.stream()
                .filter(config -> modelName.equals(config.getName()))
                .findFirst()
                .orElse(null);
        
        if (modelConfig == null) {
            log.warn("未找到模型配置: {}@{}, 使用全局默认值", providerId, modelName);
            return isLlm ? defaultLlmConcurrency : defaultEmbeddingConcurrency;
        }

        // 优先使用模型独立配置,否则使用全局默认值
        Integer maxConcurrency = modelConfig.getMaxConcurrency();
        if (maxConcurrency != null && maxConcurrency > 0) {
            log.debug("模型 {} 使用独立配置: {}", modelId, maxConcurrency);
            return maxConcurrency;
        }

        int defaultValue = isLlm ? defaultLlmConcurrency : defaultEmbeddingConcurrency;
        log.debug("模型 {} 使用全局默认值: {}", modelId, defaultValue);
        return defaultValue;
    }

    /**
     * 获取指定模型的许可
     *
     * @param modelId 模型ID (格式: providerId@modelName)
     * @param isLlm 是否是LLM模型
     * @return PermitHolder,使用完后自动释放
     * @throws InterruptedException 等待中断
     * @throws ConcurrencyLimitException 获取许可超时
     */
    public PermitHolder acquireModelPermit(String modelId, boolean isLlm) 
            throws InterruptedException {
        if (!enabled) {
            return PermitHolder.NOOP;
        }

        Semaphore semaphore = getOrCreateSemaphore(modelId, isLlm);
        log.debug("尝试获取模型 {} 的许可,当前可用数: {}", modelId, semaphore.availablePermits());

        boolean acquired = semaphore.tryAcquire(acquireTimeout, TimeUnit.SECONDS);
        if (!acquired) {
            throw new ConcurrencyLimitException(
                    String.format("获取模型 %s 的许可超时(%ds),当前并发数已达上限", 
                            modelId, acquireTimeout)
            );
        }

        log.debug("成功获取模型 {} 的许可,剩余可用数: {}", modelId, semaphore.availablePermits());
        return new PermitHolder(() -> releaseModelPermit(modelId, isLlm));
    }

    /**
     * 释放指定模型的许可
     *
     * @param modelId 模型ID
     * @param isLlm 是否是LLM模型
     */
    private void releaseModelPermit(String modelId, boolean isLlm) {
        if (!enabled) {
            return;
        }

        Semaphore semaphore = modelSemaphores.get(modelId);
        if (semaphore != null) {
            semaphore.release();
            log.debug("释放模型 {} 的许可,当前可用数: {}", modelId, semaphore.availablePermits());
        }
    }

    /**
     * 获取LLM调用许可 (兼容旧版API,不推荐使用)
     * 
     * @return 许可持有者(实现AutoCloseable,支持try-with-resources)
     * @throws InterruptedException 等待被中断
     * @throws ConcurrencyLimitException 超时未获取到许可
     * @deprecated 使用 {@link #acquireModelPermit(String, boolean)} 代替
     */
    @Deprecated
    public PermitHolder acquireLlmPermit() throws InterruptedException {
        return acquireLlmPermit("unknown");
    }

    /**
     * 获取指定LLM模型的许可
     *
     * @param modelId 模型ID
     * @return PermitHolder
     * @throws InterruptedException 等待被中断
     * @deprecated 使用 {@link #acquireModelPermit(String, boolean)} 代替
     */
    @Deprecated
    public PermitHolder acquireLlmPermit(String modelId) throws InterruptedException {
        return acquireModelPermit(modelId, true);
    }

    /**
     * 释放LLM调用许可 (兼容旧版API)
     * 
     * @deprecated 使用 try-with-resources 自动释放
     */
    @Deprecated
    public void releaseLlmPermit() {
        // 空实现,实际释放由PermitHolder处理
    }

    /**
     * 获取Embedding调用许可 (兼容旧版API,不推荐使用)
     * 
     * @return 许可持有者
     * @throws InterruptedException 等待被中断
     * @throws ConcurrencyLimitException 超时未获取到许可
     * @deprecated 使用 {@link #acquireModelPermit(String, boolean)} 代替
     */
    @Deprecated
    public PermitHolder acquireEmbeddingPermit() throws InterruptedException {
        return acquireEmbeddingPermit("unknown");
    }

    /**
     * 获取指定Embedding模型的许可
     *
     * @param modelId 模型ID
     * @return PermitHolder
     * @throws InterruptedException 等待被中断
     * @deprecated 使用 {@link #acquireModelPermit(String, boolean)} 代替
     */
    @Deprecated
    public PermitHolder acquireEmbeddingPermit(String modelId) throws InterruptedException {
        return acquireModelPermit(modelId, false);
    }

    /**
     * 释放Embedding调用许可 (兼容旧版API)
     * 
     * @deprecated 使用 try-with-resources 自动释放
     */
    @Deprecated
    public void releaseEmbeddingPermit() {
        // 空实现,实际释放由PermitHolder处理
    }

    /**
     * 获取指定模型的可用许可数
     *
     * @param modelId 模型ID
     * @param isLlm 是否是LLM模型
     * @return 可用许可数
     */
    public int getAvailablePermits(String modelId, boolean isLlm) {
        if (!enabled) {
            return Integer.MAX_VALUE;
        }
        Semaphore semaphore = modelSemaphores.get(modelId);
        return semaphore != null ? semaphore.availablePermits() : 
                getModelConcurrency(modelId, isLlm);
    }

    /**
     * 获取LLM当前可用许可数 (兼容旧版API)
     * 
     * @deprecated 使用 {@link #getAvailablePermits(String, boolean)} 代替
     */
    @Deprecated
    public int getAvailableLlmPermits() {
        return Integer.MAX_VALUE;
    }

    /**
     * 获取Embedding当前可用许可数 (兼容旧版API)
     * 
     * @deprecated 使用 {@link #getAvailablePermits(String, boolean)} 代替
     */
    @Deprecated
    public int getAvailableEmbeddingPermits() {
        return Integer.MAX_VALUE;
    }

    /**
     * 许可持有者
     * 实现AutoCloseable以支持try-with-resources自动释放
     */
    public static class PermitHolder implements AutoCloseable {
        private static final PermitHolder NOOP = new PermitHolder(null);
        
        private final Runnable releaseAction;

        public PermitHolder(Runnable releaseAction) {
            this.releaseAction = releaseAction;
        }

        @Override
        public void close() {
            if (releaseAction != null) {
                releaseAction.run();
            }
        }
    }

    /**
     * 并发限制异常
     */
    public static class ConcurrencyLimitException extends RuntimeException {
        public ConcurrencyLimitException(String message) {
            super(message);
        }
    }
}
