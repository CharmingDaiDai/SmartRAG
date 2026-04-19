package com.mtmn.smartrag.model.client.impl;

import com.mtmn.smartrag.model.client.EmbeddingClient;
import com.mtmn.smartrag.model.config.ModelProperties;
import com.mtmn.smartrag.model.dto.EmbeddingRequest;
import com.mtmn.smartrag.model.dto.EmbeddingResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Embedding客户端抽象基类
 *
 * <p>提供通用的Embedding调用实现，子类只需实现模型构建方法</p>
 * <p>支持批量处理和自定义批量大小配置</p>
 *
 * @author charmingdaidai
 * @version 1.0
 * @since 2025-01-17
 */
@Slf4j
public abstract class AbstractEmbeddingClient implements EmbeddingClient {

    protected final String instanceId;
    protected final ModelProperties.ProviderConfig providerConfig;
    protected final ModelProperties.ModelConfig modelConfig;
    protected final DimensionAwareEmbeddingModel embeddingModel;
    protected final int batchSize;

    protected AbstractEmbeddingClient(
            String instanceId,
            ModelProperties.ProviderConfig providerConfig,
            ModelProperties.ModelConfig modelConfig) {
        this.instanceId = instanceId;
        this.providerConfig = providerConfig;
        this.modelConfig = modelConfig;
        this.batchSize = modelConfig.getBatchSize() != null ? modelConfig.getBatchSize() : 8;

        log.info("初始化Embedding客户端: instanceId={}, model={}, batchSize={}",
                instanceId, modelConfig.getName(), batchSize);

        this.embeddingModel = buildEmbeddingModel();

        // 验证维度配置
        if (modelConfig.getDimension() != null) {
            int actualDimension = embeddingModel.dimension();
            if (actualDimension != modelConfig.getDimension()) {
                log.warn("配置的维度({})与模型实际维度({})不一致，将使用实际维度",
                        modelConfig.getDimension(), actualDimension);
            }
        }
    }

    /**
     * 构建Embedding模型
     * 子类实现具体的模型构建逻辑
     */
    protected abstract DimensionAwareEmbeddingModel buildEmbeddingModel();

    @Override
    public Embedding embed(String text) {
        log.debug("单文本向量化: text长度={}", text.length());

        try {
            return embeddingModel.embed(text).content();
        } catch (Exception e) {
            log.error("单文本向量化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Embedding调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Embedding> embedBatch(List<String> texts) {
        log.debug("批量文本向量化: texts数量={}, batchSize={}", texts.size(), batchSize);

        if (texts.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 分批处理
            List<Embedding> allResults = new ArrayList<>();

            for (int i = 0; i < texts.size(); i += batchSize) {
                int end = Math.min(i + batchSize, texts.size());
                List<String> batch = texts.subList(i, end);

                log.debug("处理批次: [{}-{}]/{}", i, end - 1, texts.size());

                // 转换为 TextSegment
                List<TextSegment> segments = batch.stream()
                        .map(TextSegment::from)
                        .collect(Collectors.toList());

                // 调用模型
                List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

                allResults.addAll(embeddings);
            }

            log.debug("批量向量化完成: 输入{}个文本, 输出{}个向量", texts.size(), allResults.size());
            return allResults;

        } catch (Exception e) {
            log.error("批量文本向量化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Embedding批量调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        log.debug("结构化向量化请求: texts数量={}", request.getTexts().size());

        try {
            List<Embedding> embeddings = embedBatch(request.getTexts());

            return EmbeddingResponse.builder()
                    .embeddings(embeddings)
                    .model(modelConfig.getName())
                    .dimension(getDimension())
                    .build();

        } catch (Exception e) {
            log.error("结构化向量化失败: {}", e.getMessage(), e);
            throw new RuntimeException("Embedding调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimension() {
        return embeddingModel.dimension();
    }

    @Override
    public String getProviderType() {
        return providerConfig.getType();
    }

    @Override
    public String getModelId() {
        // 格式: instanceId@modelName
        return instanceId + "@" + modelConfig.getName();
    }

    @Override
    public String getModelName() {
        return modelConfig.getName();
    }
}