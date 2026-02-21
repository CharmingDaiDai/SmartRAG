package com.mtmn.smartdoc.service.impl;

import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.service.EmbeddingService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 嵌入模型服务实现
 * 负责创建和管理不同的嵌入模型，提供文本向量化功能
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final ModelFactory modelFactory;

    @Override
    public EmbeddingModel createEmbeddingModel() {
        return new LangChain4jEmbeddingModelAdapter(modelFactory.createDefaultEmbeddingClient());
    }

    @Override
    public EmbeddingModel createEmbeddingModel(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            return createEmbeddingModel();
        }
        EmbeddingClient client = modelFactory.createEmbeddingClient(modelId);
        return new LangChain4jEmbeddingModelAdapter(client);
    }

    @Override
    public EmbeddingStore<Embedding> createDocumentVectors(String content) {
        return createDocumentVectors(content, null);
    }

    @Override
    public EmbeddingStore<Embedding> createDocumentVectors(String content, String modelId) {
        log.info("创建文档向量，使用模型：{}", modelId == null ? "默认" : modelId);

        Document document = Document.from(content);
        EmbeddingModel embeddingModel = modelId == null ?
                createEmbeddingModel() : createEmbeddingModel(modelId);

        EmbeddingStore<Embedding> embeddingStore = new InMemoryEmbeddingStore<>();

        // 配置文档分割器
        DocumentSplitter splitter = new DocumentByParagraphSplitter(2048, 100);
        List<TextSegment> segments = splitter.split(document);

        // 批量嵌入文本段
        List<Embedding> embeddings = embedInBatches(embeddingModel, segments, 10);
        embeddingStore.addAll(embeddings);

        return embeddingStore;
    }

    /**
     * 批量嵌入文本段
     *
     * @param embeddingModel 嵌入模型
     * @param segments       文本段列表
     * @param batchSize      批量大小
     * @return 嵌入向量列表
     */
    private List<Embedding> embedInBatches(EmbeddingModel embeddingModel, List<TextSegment> segments, int batchSize) {
        if (segments == null || segments.isEmpty()) {
            return new ArrayList<>();
        }
        if (batchSize <= 0) {
            batchSize = 10;
        }

        List<Embedding> result = new ArrayList<>(segments.size());
        for (int i = 0; i < segments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, segments.size());
            List<TextSegment> batch = segments.subList(i, end);
            result.addAll(embeddingModel.embedAll(batch).content());
        }
        return result;
    }

    /**
     * LangChain4j EmbeddingModel适配器
     * 将新的EmbeddingClient适配为LangChain4j的EmbeddingModel接口
     */
    private static class LangChain4jEmbeddingModelAdapter implements EmbeddingModel {

        private final EmbeddingClient client;

        public LangChain4jEmbeddingModelAdapter(EmbeddingClient client) {
            this.client = client;
        }

        @Override
        public dev.langchain4j.model.output.Response<Embedding> embed(String text) {
            Embedding embedding = client.embed(text);
            return dev.langchain4j.model.output.Response.from(
                    embedding
            );
        }

        @Override
        public dev.langchain4j.model.output.Response<Embedding> embed(TextSegment textSegment) {
            return embed(textSegment.text());
        }

        @Override
        public dev.langchain4j.model.output.Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
            List<String> texts = textSegments.stream()
                    .map(TextSegment::text)
                    .collect(java.util.stream.Collectors.toList());

            List<Embedding> embeddings = client.embedBatch(texts);

            return dev.langchain4j.model.output.Response.from(embeddings);
        }

        @Override
        public int dimension() {
            return client.getDimension();
        }
    }
}