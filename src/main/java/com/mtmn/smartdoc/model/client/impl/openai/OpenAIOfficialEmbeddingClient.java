package com.mtmn.smartdoc.model.client.impl.openai;

import com.mtmn.smartdoc.model.client.impl.AbstractEmbeddingClient;
import com.mtmn.smartdoc.model.config.ModelProperties;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.openaiofficial.OpenAiOfficialEmbeddingModel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * OpenAI 官方 Embedding客户端实现
 *
 * <p>使用 OpenAI 官方 SDK (openai-java) 实现，提供最原生的 OpenAI Embedding API 支持</p>
 * <p>支持的服务:</p>
 * <ul>
 *   <li>OpenAI 官方 API - 直接访问 OpenAI 服务</li>
 *   <li>Azure OpenAI - 通过 Azure 部署的 OpenAI 服务</li>
 *   <li>GitHub Models - GitHub 提供的模型服务</li>
 * </ul>
 *
 * <p>支持的模型:</p>
 * <ul>
 *   <li>text-embedding-3-small - 小型高效模型</li>
 *   <li>text-embedding-3-large - 大型高性能模型</li>
 *   <li>text-embedding-ada-002 - 经典模型</li>
 * </ul>
 *
 * <p>与 OpenAI 兼容格式的区别:</p>
 * <ul>
 *   <li>使用官方 SDK，功能更完整</li>
 *   <li>支持 Azure OpenAI 特定配置</li>
 *   <li>内置重试机制</li>
 *   <li>更好的类型安全</li>
 * </ul>
 *
 * <p>注意: 如果你使用的是第三方 OpenAI 兼容 API，请使用 OpenAIEmbeddingClient</p>
 *
 * @author charmingdaidai
 * @version 1.0
 * @since 2025-01-17
 */
@Slf4j
public class OpenAIOfficialEmbeddingClient extends AbstractEmbeddingClient {

    public OpenAIOfficialEmbeddingClient(
            String instanceId,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        super(instanceId, providerConfig, modelConfig);
    }

    @Override
    protected DimensionAwareEmbeddingModel buildEmbeddingModel() {
        log.info("构建OpenAI官方 Embedding模型: baseUrl={}, model={}",
                providerConfig.getBaseUrl(), modelConfig.getName());

        OpenAiOfficialEmbeddingModel.Builder builder =
                OpenAiOfficialEmbeddingModel.builder()
                        .apiKey(providerConfig.getApiKey())
                        .modelName(modelConfig.getName());

        // 设置 baseUrl（如果有）
        if (providerConfig.getBaseUrl() != null && !providerConfig.getBaseUrl().isEmpty()) {
            builder.baseUrl(providerConfig.getBaseUrl());
        }

        // 设置超时
        if (providerConfig.getTimeout() != null) {
            builder.timeout(parseDuration(providerConfig.getTimeout()));
        }

        // 设置最大重试
        if (providerConfig.getMaxRetries() > 0) {
            builder.maxRetries(providerConfig.getMaxRetries());
        }

        // 设置批量大小
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