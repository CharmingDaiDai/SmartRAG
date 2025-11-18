package com.mtmn.smartdoc.service.model.client.impl.openai;

import com.mtmn.smartdoc.service.model.client.impl.AbstractEmbeddingClient;
import com.mtmn.smartdoc.service.model.config.ModelProperties;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * OpenAI 兼容格式 Embedding客户端实现
 * 
 * <p>使用 OpenAI 兼容格式接口，适用于所有支持 OpenAI Embedding API 格式的服务</p>
 * <p>适用场景:</p>
 * <ul>
 *   <li>第三方 OpenAI 兼容 API 服务</li>
 *   <li>自建的 OpenAI 代理服务</li>
 *   <li>其他实现了 OpenAI Embedding API 格式的服务</li>
 * </ul>
 * 
 * <p>注意事项:</p>
 * <ul>
 *   <li>如需使用 OpenAI 官方 API，建议使用 OpenAIOfficialEmbeddingClient</li>
 *   <li>智谱AI、通义千问、Xinference 应使用各自专门的实现类</li>
 *   <li>此类适合通用的 OpenAI 兼容服务</li>
 * </ul>
 * 
 * @author charmingdaidai
 * @version 1.0
 * @since 2025-01-17
 */
@Slf4j
public class OpenAIEmbeddingClient extends AbstractEmbeddingClient {
    
    public OpenAIEmbeddingClient(
            String instanceId,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        super(instanceId, providerConfig, modelConfig);
    }
    
    @Override
    protected DimensionAwareEmbeddingModel buildEmbeddingModel() {
        log.info("构建OpenAI Embedding模型: baseUrl={}, model={}", 
                providerConfig.getBaseUrl(), modelConfig.getName());
        
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = 
                OpenAiEmbeddingModel.builder()
                        .apiKey(providerConfig.getApiKey())
                        .baseUrl(providerConfig.getBaseUrl())
                        .modelName(modelConfig.getName());
        
        // 设置超时
        if (providerConfig.getTimeout() != null) {
            builder.timeout(parseDuration(providerConfig.getTimeout()));
        }
        
        // 设置最大重试
        if (providerConfig.getMaxRetries() > 0) {
            builder.maxRetries(providerConfig.getMaxRetries());
        }
        
        // 设置批量大小 (OpenAI 默认支持很大的批次)
        builder.maxSegmentsPerBatch(batchSize);
        
        return builder.build();
    }
    
    /**
     * 解析超时时间字符串
     * 支持格式: 60s, 2m, 1h
     */
    private Duration parseDuration(String timeout) {
        if (timeout == null || timeout.isEmpty()) {
            return Duration.ofSeconds(60);
        }
        
        try {
            String value = timeout.substring(0, timeout.length() - 1);
            String unit = timeout.substring(timeout.length() - 1);
            
            int num = Integer.parseInt(value);
            
            return switch (unit.toLowerCase()) {
                case "s" -> Duration.ofSeconds(num);
                case "m" -> Duration.ofMinutes(num);
                case "h" -> Duration.ofHours(num);
                default -> Duration.ofSeconds(60);
            };
        } catch (Exception e) {
            log.warn("无法解析超时时间: {}, 使用默认值60s", timeout);
            return Duration.ofSeconds(60);
        }
    }
}
