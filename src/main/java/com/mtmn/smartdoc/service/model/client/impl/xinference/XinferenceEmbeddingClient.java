package com.mtmn.smartdoc.service.model.client.impl.xinference;

import com.mtmn.smartdoc.service.model.client.impl.AbstractEmbeddingClient;
import com.mtmn.smartdoc.service.model.config.ModelProperties;
import dev.langchain4j.community.model.xinference.XinferenceEmbeddingModel;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * Xinference Embedding客户端实现
 * 
 * <p>使用 LangChain4j 的Xinference实现，支持本地部署的各种开源Embedding模型</p>
 * <p>支持的模型系列:</p>
 * <ul>
 *   <li>bge-m3 - 多语言嵌入模型</li>
 *   <li>bge-large-zh - 中文嵌入模型</li>
 *   <li>text-embedding-ada-002 兼容版</li>
 *   <li>其他开源Embedding模型</li>
 * </ul>
 * 
 * <p>特点:</p>
 * <ul>
 *   <li>支持本地部署，数据更安全</li>
 *   <li>无API调用费用</li>
 *   <li>支持GPU加速</li>
 *   <li>支持批量向量化</li>
 * </ul>
 * 
 * @author charmingdaidai
 * @version 1.0
 * @since 2025-01-17
 */
@Slf4j
public class XinferenceEmbeddingClient extends AbstractEmbeddingClient {
    
    public XinferenceEmbeddingClient(
            String instanceId,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        super(instanceId, providerConfig, modelConfig);
    }
    
    @Override
    protected DimensionAwareEmbeddingModel buildEmbeddingModel() {
        log.info("构建Xinference Embedding模型: baseUrl={}, model={}", 
                providerConfig.getBaseUrl(), modelConfig.getName());
        
        XinferenceEmbeddingModel.XinferenceEmbeddingModelBuilder builder = 
                XinferenceEmbeddingModel.builder()
                        .apiKey(providerConfig.getApiKey())
                        .baseUrl(providerConfig.getBaseUrl())
                        .modelName(modelConfig.getName());
        
        // 设置超时
        if (providerConfig.getTimeout() != null) {
            builder.timeout(parseDuration(providerConfig.getTimeout()));
        } else {
            builder.timeout(Duration.ofSeconds(60));
        }
        
        // 设置最大重试
        if (providerConfig.getMaxRetries() > 0) {
            builder.maxRetries(providerConfig.getMaxRetries());
        }
        
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
