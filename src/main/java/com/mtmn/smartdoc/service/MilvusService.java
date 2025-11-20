package com.mtmn.smartdoc.service;

import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;

/**
 * Milvus向量数据库服务接口
 * 负责创建和管理Milvus嵌入存储
 *
 * @author charmingdaidai
 */
public interface MilvusService {

    /**
     * 获取Milvus嵌入存储实例
     *
     * @param collectionName 集合名称
     * @param dimension      向量维度
     * @return Milvus嵌入存储实例
     */
    MilvusEmbeddingStore getEmbeddingStore(String collectionName, Integer dimension);
}