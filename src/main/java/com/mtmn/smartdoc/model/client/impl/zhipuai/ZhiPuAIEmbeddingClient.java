package com.mtmn.smartdoc.model.client.impl.zhipuai;

import com.mtmn.smartdoc.model.client.impl.AbstractEmbeddingClient;
import com.mtmn.smartdoc.model.config.ModelProperties;
import dev.langchain4j.community.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * 智谱AI Embedding客户端实现
 *
 * <p>使用 LangChain4j 的智谱AI Embedding实现</p>
 * <p>支持的模型:</p>
 * <ul>
 *   <li>embedding-2 - 标准嵌入模型</li>
 *   <li>embedding-3 - 新一代嵌入模型</li>
 * </ul>
 *
 * <p>特点:</p>
 * <ul>
 *   <li>中文语义理解能力强</li>
 *   <li>支持长文本</li>
 *   <li>向量质量高</li>
 * </ul>
 *
 * @author charmingdaidai
 * @version 1.0
 * @since 2025-01-17
 */
@Slf4j
public class ZhiPuAIEmbeddingClient extends AbstractEmbeddingClient {

    public ZhiPuAIEmbeddingClient(
            String instanceId,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        super(instanceId, providerConfig, modelConfig);
    }

    @Override
    protected DimensionAwareEmbeddingModel buildEmbeddingModel() {
        log.info("构建智谱AI Embedding模型: baseUrl={}, model={}",
                providerConfig.getBaseUrl(), modelConfig.getName());

        ZhipuAiEmbeddingModel.ZhipuAiEmbeddingModelBuilder builder =
                ZhipuAiEmbeddingModel.builder()
                        .apiKey(providerConfig.getApiKey())
                        .baseUrl(providerConfig.getBaseUrl())
                        .model(modelConfig.getName());

        // 设置超时
        if (providerConfig.getTimeout() != null) {
            Duration timeout = parseDuration(providerConfig.getTimeout());
            builder.connectTimeout(timeout)
                    .readTimeout(timeout);
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