package com.mtmn.smartrag.model.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * 模型配置属性类
 *
 * <p>从application.yml中加载models配置</p>
 * <p>模型标识格式: instanceId@modelName (如: xinference-gpu@qwen2.5-instruct)</p>
 * <p>支持多个相同提供商的不同实例，通过 type 字段区分提供商类型</p>
 *
 * @author charmingdaidai
 * @version 3.0
 * @since 2025-01-17
 */
@Configuration
@ConfigurationProperties(prefix = "models")
@Data
public class ModelProperties {

    /**
     * 当前激活的模型配置
     */
    private ActiveModels active;

    /**
     * 缓存配置
     */
    private CacheConfig cache;

    /**
     * 并发控制配置
     */
    private ConcurrencyConfig concurrency;

    /**
     * 模型提供商配置
     */
    private Map<String, ProviderConfig> providers;

    /**
     * 激活的模型配置
     * 格式: instanceId@modelName (如: xinference-gpu@qwen2.5-instruct)
     */
    @Data
    public static class ActiveModels {
        /**
         * 激活的LLM模型 (格式: instanceId@modelName)
         */
        private String llm;

        /**
         * 激活的Embedding模型 (格式: instanceId@modelName)
         */
        private String embedding;

        /**
         * 激活的Rerank模型(预留, 格式: instanceId@modelName)
         */
        private String rerank;
    }

    /**
     * 缓存配置
     */
    @Data
    public static class CacheConfig {
        /**
         * 是否启用缓存
         */
        private boolean enabled = true;

        /**
         * 最大缓存数量
         */
        private int maxSize = 10;

        /**
         * 缓存过期时间(如: 1h, 30m, 60s)
         */
        private String expireAfterAccess = "1h";
    }

    /**
     * 并发控制配置
     */
    @Data
    public static class ConcurrencyConfig {
        /**
         * 是否启用并发控制
         */
        private boolean enabled = true;

        /**
         * LLM 全局默认最大并发数 (默认1,即不并发)
         * 用于限制同时进行的LLM API调用数量
         * 可被单个模型的配置覆盖
         */
        private int defaultMaxConcurrentLlmRequests = 1;

        /**
         * Embedding 全局默认最大并发数 (默认1,即不并发)
         * 可被单个模型的配置覆盖
         */
        private int defaultMaxConcurrentEmbeddingRequests = 1;

        /**
         * 等待超时时间(秒)
         * 当并发数达到上限时,新请求等待的最大时间
         */
        private int acquireTimeout = 30;
    }

    /**
     * 提供商配置
     */
    @Data
    public static class ProviderConfig {
        /**
         * 提供商类型 (如: zhipuai, qwen, xinference, openai, openai-official)
         * 用于确定使用哪个客户端实现类
         */
        private String type;

        /**
         * 是否启用
         */
        private boolean enabled;

        /**
         * API密钥
         */
        private String apiKey;

        /**
         * 基础URL
         */
        private String baseUrl;

        /**
         * 超时时间(如: 60s, 2m)
         */
        private String timeout;

        /**
         * 最大重试次数
         */
        private int maxRetries = 3;

        /**
         * LLM模型列表
         */
        private List<ModelConfig> llmModels;

        /**
         * Embedding模型列表
         */
        private List<ModelConfig> embeddingModels;

        /**
         * Rerank模型列表(预留)
         */
        private List<ModelConfig> rerankModels;
    }

    /**
     * 单个模型配置
     */
    @Data
    public static class ModelConfig {
        /**
         * 模型名称(API调用使用，唯一标识)
         */
        private String name;

        /**
         * 向量维度(Embedding模型专用)
         */
        private Integer dimension;

        /**
         * 最大token数(LLM模型专用)
         */
        private Integer maxTokens;

        /**
         * 温度参数(LLM模型专用，默认值)
         */
        private Double temperature;

        /**
         * 批量处理大小(Embedding模型专用，默认为8)
         */
        private Integer batchSize = 8;

        /**
         * 最大并发数(可选)
         * 如果设置,将覆盖全局默认并发配置
         * null表示使用全局默认值
         */
        private Integer maxConcurrency;

        /**
         * 描述信息(可选)
         */
        private String description;
    }
}