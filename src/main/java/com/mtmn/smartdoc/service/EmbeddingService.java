package com.mtmn.smartdoc.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * 嵌入模型服务接口
 * 负责创建和管理不同的嵌入模型，提供文本向量化功能
 *
 * @author charmingdaidai
 */
public interface EmbeddingService {

    /**
     * 创建默认的嵌入模型
     *
     * @return 嵌入模型实例
     */
    EmbeddingModel createEmbeddingModel();

    /**
     * 创建指定模型的嵌入模型
     *
     * @param modelId 模型ID，null时使用默认模型
     * @return 嵌入模型实例
     */
    EmbeddingModel createEmbeddingModel(String modelId);

    /**
     * 从文档内容创建向量存储（使用默认模型）
     *
     * @param content 文档内容
     * @return 向量存储实例
     */
    EmbeddingStore<Embedding> createDocumentVectors(String content);

    /**
     * 从文档内容创建向量存储（使用指定模型）
     *
     * @param content 文档内容
     * @param modelId 模型ID，null时使用默认模型
     * @return 向量存储实例
     */
    EmbeddingStore<Embedding> createDocumentVectors(String content, String modelId);
}