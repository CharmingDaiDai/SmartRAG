package com.mtmn.smartdoc.model.client.impl.qwen;

import com.mtmn.smartdoc.model.client.impl.AbstractEmbeddingClient;
import com.mtmn.smartdoc.model.config.ModelProperties;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import lombok.extern.slf4j.Slf4j;

/**
 * 通义千问 Embedding客户端实现
 *
 * <p>使用 LangChain4j 的通义千问 Embedding实现</p>
 * <p>支持的模型:</p>
 * <ul>
 *   <li>text-embedding-v1 - 标准文本嵌入模型</li>
 *   <li>text-embedding-v2 - 增强版文本嵌入模型</li>
 *   <li>text-embedding-v3 - 最新版文本嵌入模型</li>
 * </ul>
 *
 * <p>特点:</p>
 * <ul>
 *   <li>阿里云官方模型</li>
 *   <li>中英文双语支持</li>
 *   <li>高质量向量表示</li>
 *   <li>支持批量处理</li>
 * </ul>
 *
 * @author charmingdaidai
 * @version 1.0
 * @since 2025-01-17
 */
@Slf4j
public class QwenEmbeddingClient extends AbstractEmbeddingClient {

    public QwenEmbeddingClient(
            String instanceId,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        super(instanceId, providerConfig, modelConfig);
    }

    @Override
    protected DimensionAwareEmbeddingModel buildEmbeddingModel() {
        log.info("构建通义千问 Embedding模型: model={}", modelConfig.getName());

        QwenEmbeddingModel.QwenEmbeddingModelBuilder builder =
                QwenEmbeddingModel.builder()
                        .apiKey(providerConfig.getApiKey())
                        .modelName(modelConfig.getName());

        // Qwen 的 baseUrl 不需要设置，默认就是 DashScope 的地址
        if (providerConfig.getBaseUrl() != null && !providerConfig.getBaseUrl().isEmpty()) {
            builder.baseUrl(providerConfig.getBaseUrl());
        }

        return builder.build();
    }
}