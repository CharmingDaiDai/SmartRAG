package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.model.dto.IndexUpdateItem;
import com.mtmn.smartdoc.model.dto.VectorItem;
import com.mtmn.smartdoc.vo.RetrievalResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;

import java.util.List;

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

    /**
     * 删除集合
     *
     * @param collectionName 集合名称
     */
    void dropCollection(String collectionName);

    /**
     * 检索
     *
     * @param kbId        知识库ID
     * @param queryVector 查询向量
     * @param topK        返回数量
     * @param threshold   相似度阈值
     * @return 检索结果
     */
    List<RetrievalResult> search(Long kbId, Embedding queryVector, int topK, double threshold);

    /**
     * 带 Metadata Filter 的向量检索（用于 SADP Scoped_Retrieve 算子）
     *
     * @param kbId        知识库ID
     * @param queryVector 查询向量
     * @param topK        返回数量
     * @param threshold   相似度阈值
     * @param filter      Milvus 原生 metadata 过滤条件
     * @return 检索结果
     */
    List<RetrievalResult> search(Long kbId, Embedding queryVector, int topK, double threshold, Filter filter);

    /**
     * 批量存储向量
     *
     * @param kbId  知识库ID
     * @param items 向量项列表
     */
    void store(Long kbId, List<VectorItem> items);

    /**
     * 更新部分索引
     *
     * @param kbId  知识库ID
     * @param items 更新项列表
     */
    void update(Long kbId, List<IndexUpdateItem> items);

    /**
     * 根据文档ID删除向量数据
     *
     * @param kbId  知识库ID
     * @param docId 文档ID
     */
    void removeByDocumentId(Long kbId, Long docId);
}