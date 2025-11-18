package com.mtmn.smartdoc.service.model.factory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mtmn.smartdoc.service.model.client.EmbeddingClient;
import com.mtmn.smartdoc.service.model.client.LLMClient;
import com.mtmn.smartdoc.service.model.client.RerankClient;
import com.mtmn.smartdoc.service.model.client.impl.openai.OpenAIEmbeddingClient;
import com.mtmn.smartdoc.service.model.client.impl.openai.OpenAILLMClient;
import com.mtmn.smartdoc.service.model.client.impl.openai.OpenAIOfficialEmbeddingClient;
import com.mtmn.smartdoc.service.model.client.impl.openai.OpenAIOfficialLLMClient;
import com.mtmn.smartdoc.service.model.client.impl.qwen.QwenEmbeddingClient;
import com.mtmn.smartdoc.service.model.client.impl.qwen.QwenLLMClient;
import com.mtmn.smartdoc.service.model.client.impl.xinference.XinferenceEmbeddingClient;
import com.mtmn.smartdoc.service.model.client.impl.xinference.XinferenceLLMClient;
import com.mtmn.smartdoc.service.model.client.impl.zhipuai.ZhiPuAIEmbeddingClient;
import com.mtmn.smartdoc.service.model.client.impl.zhipuai.ZhiPuAILLMClient;
import com.mtmn.smartdoc.service.model.config.ModelProperties;
import com.mtmn.smartdoc.service.model.exception.ModelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模型工厂实现类
 * 
 * <p>职责:</p>
 * <ul>
 *   <li>根据modelId创建对应的模型客户端</li>
 *   <li>管理模型实例缓存</li>
 *   <li>提供模型可用性查询</li>
 *   <li>支持同一提供商的多个实例配置</li>
 * </ul>
 * 
 * <p>模型标识格式: instanceId@modelName</p>
 * <ul>
 *   <li>instanceId: 在 application.yml 中配置的实例标识(如: xinference-gpu)</li>
 *   <li>modelName: 模型名称(如: qwen2.5-instruct)</li>
 * </ul>
 * 
 * @author charmingdaidai
 * @version 3.0
 * @since 2025-01-17
 */
@Service
@Slf4j
public class ModelFactoryImpl implements ModelFactory {
    
    private final ModelProperties modelProperties;
    private final Cache<String, LLMClient> llmCache;
    private final Cache<String, EmbeddingClient> embeddingCache;
    private final Cache<String, RerankClient> rerankCache;
    
    public ModelFactoryImpl(ModelProperties modelProperties) {
        this.modelProperties = modelProperties;
        
        // 初始化缓存
        if (modelProperties.getCache() != null && modelProperties.getCache().isEnabled()) {
            int maxSize = modelProperties.getCache().getMaxSize();
            Duration expireAfterAccess = parseDuration(
                    modelProperties.getCache().getExpireAfterAccess());
            
            this.llmCache = Caffeine.newBuilder()
                    .maximumSize(maxSize)
                    .expireAfterAccess(expireAfterAccess)
                    .build();
            
            this.embeddingCache = Caffeine.newBuilder()
                    .maximumSize(maxSize)
                    .expireAfterAccess(expireAfterAccess)
                    .build();
            
            this.rerankCache = Caffeine.newBuilder()
                    .maximumSize(maxSize)
                    .expireAfterAccess(expireAfterAccess)
                    .build();
            
            log.info("模型缓存已启用: maxSize={}, expireAfterAccess={}", 
                    maxSize, expireAfterAccess);
        } else {
            // 禁用缓存，使用简单的Map
            this.llmCache = Caffeine.newBuilder().maximumSize(0).build();
            this.embeddingCache = Caffeine.newBuilder().maximumSize(0).build();
            this.rerankCache = Caffeine.newBuilder().maximumSize(0).build();
            log.info("模型缓存已禁用");
        }
    }
    
    @Override
    public LLMClient createLLMClient(String modelId) {
        log.debug("创建LLM客户端: modelId={}", modelId);
        
        return llmCache.get(modelId, id -> {
            log.info("缓存未命中，创建新的LLM客户端: {}", id);
            
            // 解析modelId: instanceId@modelName
            String[] parts = id.split("@", 2);
            if (parts.length < 2) {
                throw new ModelException("无效的modelId格式: " + id + ", 期望格式: instanceId@modelName");
            }
            
            String instanceId = parts[0];
            String modelName = parts[1];
            
            // 获取实例配置
            ModelProperties.ProviderConfig providerConfig = 
                    modelProperties.getProviders().get(instanceId);
            
            if (providerConfig == null) {
                throw new ModelException("未找到实例配置: " + instanceId);
            }
            
            if (!providerConfig.isEnabled()) {
                throw new ModelException("实例已禁用: " + instanceId);
            }
            
            if (providerConfig.getType() == null || providerConfig.getType().isEmpty()) {
                throw new ModelException("实例未配置 type 字段: " + instanceId);
            }
            
            // 查找模型配置
            ModelProperties.ModelConfig modelConfig = findModelInList(
                    providerConfig.getLlmModels(), modelName);
            
            if (modelConfig == null) {
                throw new ModelException("未找到LLM模型配置: " + modelName + 
                        " (实例: " + instanceId + ")");
            }
            
            // 根据提供商类型创建对应的客户端
            return createLLMClientByProvider(instanceId, providerConfig, modelConfig);
        });
    }
    
    @Override
    public EmbeddingClient createEmbeddingClient(String modelId) {
        log.debug("创建Embedding客户端: modelId={}", modelId);
        
        return embeddingCache.get(modelId, id -> {
            log.info("缓存未命中，创建新的Embedding客户端: {}", id);
            
            // 解析modelId: instanceId@modelName
            String[] parts = id.split("@", 2);
            if (parts.length < 2) {
                throw new ModelException("无效的modelId格式: " + id + ", 期望格式: instanceId@modelName");
            }
            
            String instanceId = parts[0];
            String modelName = parts[1];
            
            // 获取实例配置
            ModelProperties.ProviderConfig providerConfig = 
                    modelProperties.getProviders().get(instanceId);
            
            if (providerConfig == null) {
                throw new ModelException("未找到实例配置: " + instanceId);
            }
            
            if (!providerConfig.isEnabled()) {
                throw new ModelException("实例已禁用: " + instanceId);
            }
            
            if (providerConfig.getType() == null || providerConfig.getType().isEmpty()) {
                throw new ModelException("实例未配置 type 字段: " + instanceId);
            }
            
            // 查找模型配置
            ModelProperties.ModelConfig modelConfig = findModelInList(
                    providerConfig.getEmbeddingModels(), modelName);
            
            if (modelConfig == null) {
                throw new ModelException("未找到Embedding模型配置: " + modelName + 
                        " (实例: " + instanceId + ")");
            }
            
            // 根据提供商类型创建对应的客户端
            return createEmbeddingClientByProvider(instanceId, providerConfig, modelConfig);
        });
    }
    
    @Override
    public RerankClient createRerankClient(String modelId) {
        throw new UnsupportedOperationException("Rerank功能暂未实现");
    }
    
    @Override
    public LLMClient createDefaultLLMClient() {
        String activeModelId = modelProperties.getActive().getLlm();
        if (activeModelId == null || activeModelId.isEmpty()) {
            throw new ModelException("未配置默认LLM模型");
        }
        log.debug("创建默认LLM客户端: {}", activeModelId);
        return createLLMClient(activeModelId);
    }
    
    @Override
    public EmbeddingClient createDefaultEmbeddingClient() {
        String activeModelId = modelProperties.getActive().getEmbedding();
        if (activeModelId == null || activeModelId.isEmpty()) {
            throw new ModelException("未配置默认Embedding模型");
        }
        log.debug("创建默认Embedding客户端: {}", activeModelId);
        return createEmbeddingClient(activeModelId);
    }
    
    @Override
    public RerankClient createDefaultRerankClient() {
        throw new UnsupportedOperationException("Rerank功能暂未实现");
    }
    
    @Override
    public List<String> getAvailableLLMModels() {
        List<String> models = new ArrayList<>();
        
        if (modelProperties.getProviders() != null) {
            for (Map.Entry<String, ModelProperties.ProviderConfig> entry : 
                    modelProperties.getProviders().entrySet()) {
                
                String instanceId = entry.getKey();
                ModelProperties.ProviderConfig provider = entry.getValue();
                
                if (provider.isEnabled() && provider.getLlmModels() != null) {
                    for (ModelProperties.ModelConfig model : provider.getLlmModels()) {
                        // 格式: instanceId@modelName
                        models.add(instanceId + "@" + model.getName());
                    }
                }
            }
        }
        
        return models;
    }
    
    @Override
    public List<String> getAvailableEmbeddingModels() {
        List<String> models = new ArrayList<>();
        
        if (modelProperties.getProviders() != null) {
            for (Map.Entry<String, ModelProperties.ProviderConfig> entry : 
                    modelProperties.getProviders().entrySet()) {
                
                String instanceId = entry.getKey();
                ModelProperties.ProviderConfig provider = entry.getValue();
                
                if (provider.isEnabled() && provider.getEmbeddingModels() != null) {
                    for (ModelProperties.ModelConfig model : provider.getEmbeddingModels()) {
                        // 格式: instanceId@modelName
                        models.add(instanceId + "@" + model.getName());
                    }
                }
            }
        }
        
        return models;
    }
    
    @Override
    public List<String> getAvailableRerankModels() {
        return new ArrayList<>();
    }
    
    @Override
    public void clearCache(String modelId) {
        llmCache.invalidate(modelId);
        embeddingCache.invalidate(modelId);
        rerankCache.invalidate(modelId);
        log.info("已清除模型缓存: {}", modelId);
    }
    
    @Override
    public void clearAllCache() {
        llmCache.invalidateAll();
        embeddingCache.invalidateAll();
        rerankCache.invalidateAll();
        log.info("已清除所有模型缓存");
    }
    
    @Override
    public boolean isModelAvailable(String modelId) {
        try {
            String[] parts = modelId.split("@", 2);
            if (parts.length < 2) {
                return false;
            }
            
            String instanceId = parts[0];
            String modelName = parts[1];
            
            ModelProperties.ProviderConfig providerConfig = 
                    modelProperties.getProviders().get(instanceId);
            
            if (providerConfig == null || !providerConfig.isEnabled()) {
                return false;
            }
            
            // 检查LLM模型
            if (providerConfig.getLlmModels() != null) {
                if (findModelInList(providerConfig.getLlmModels(), modelName) != null) {
                    return true;
                }
            }
            
            // 检查Embedding模型
            if (providerConfig.getEmbeddingModels() != null) {
                if (findModelInList(providerConfig.getEmbeddingModels(), modelName) != null) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            log.error("检查模型可用性失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 从模型列表中查找指定名称的模型
     */
    private ModelProperties.ModelConfig findModelInList(
            List<ModelProperties.ModelConfig> models, 
            String modelName) {
        
        if (models == null || modelName == null) {
            return null;
        }
        
        for (ModelProperties.ModelConfig model : models) {
            if (modelName.equals(model.getName())) {
                return model;
            }
        }
        
        return null;
    }
    
    /**
     * 根据提供商类型创建对应的LLM客户端
     */
    private LLMClient createLLMClientByProvider(
            String instanceId,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        
        String providerType = providerConfig.getType().toLowerCase();
        log.debug("根据提供商类型创建LLM客户端: instanceId={}, type={}", instanceId, providerType);
        
        return switch (providerType) {
            case "zhipuai" -> new ZhiPuAILLMClient(instanceId, providerConfig, modelConfig);
            case "qwen" -> new QwenLLMClient(instanceId, providerConfig, modelConfig);
            case "xinference" -> new XinferenceLLMClient(instanceId, providerConfig, modelConfig);
            case "openai" -> new OpenAILLMClient(instanceId, providerConfig, modelConfig);
            case "openai-official" -> new OpenAIOfficialLLMClient(instanceId, providerConfig, modelConfig);
            default -> {
                log.warn("未知的提供商类型: {}, 尝试使用OpenAI兼容接口", providerType);
                yield new OpenAILLMClient(instanceId, providerConfig, modelConfig);
            }
        };
    }
    
    /**
     * 根据提供商类型创建对应的Embedding客户端
     */
    private EmbeddingClient createEmbeddingClientByProvider(
            String instanceId,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        
        String providerType = providerConfig.getType().toLowerCase();
        log.debug("根据提供商类型创建Embedding客户端: instanceId={}, type={}", instanceId, providerType);
        
        return switch (providerType) {
            case "zhipuai" -> new ZhiPuAIEmbeddingClient(instanceId, providerConfig, modelConfig);
            case "qwen" -> new QwenEmbeddingClient(instanceId, providerConfig, modelConfig);
            case "xinference" -> new XinferenceEmbeddingClient(instanceId, providerConfig, modelConfig);
            case "openai" -> new OpenAIEmbeddingClient(instanceId, providerConfig, modelConfig);
            case "openai-official" -> new OpenAIOfficialEmbeddingClient(instanceId, providerConfig, modelConfig);
            default -> {
                log.warn("未知的提供商类型: {}, 尝试使用OpenAI兼容接口", providerType);
                yield new OpenAIEmbeddingClient(instanceId, providerConfig, modelConfig);
            }
        };
    }
    
    /**
     * 解析时间字符串
     */
    private Duration parseDuration(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return Duration.ofHours(1);
        }
        
        try {
            String value = timeStr.substring(0, timeStr.length() - 1);
            String unit = timeStr.substring(timeStr.length() - 1);
            
            int num = Integer.parseInt(value);
            
            return switch (unit.toLowerCase()) {
                case "s" -> Duration.ofSeconds(num);
                case "m" -> Duration.ofMinutes(num);
                case "h" -> Duration.ofHours(num);
                default -> Duration.ofHours(1);
            };
        } catch (Exception e) {
            log.warn("无法解析时间字符串: {}, 使用默认值1h", timeStr);
            return Duration.ofHours(1);
        }
    }
}
