package com.mtmn.smartrag.rag;

import com.mtmn.smartrag.enums.IndexStrategyType;
import com.mtmn.smartrag.po.DocumentPo;
import com.mtmn.smartrag.po.KnowledgeBase;
import com.mtmn.smartrag.rag.config.IndexStrategyConfig;
import com.mtmn.smartrag.service.IndexingProgressCallback;

import java.util.List;

/**
 * 索引构建策略接口
 * <p>
 * 负责将文档内容解析、切分、向量化并存储到向量数据库
 * 不同的 RAG 方法（Naive、HiSem）有不同的索引构建方式
 *
 * @author charmingdaidai
 * @version 3.0
 * @date 2025-11-24
 */
public interface IndexStrategy {

    /**
     * 获取策略类型
     */
    IndexStrategyType getType();

    /**
     * 构建索引
     * <p>
     * 完整流程：读取文档 -> 切分/解析 -> 向量化 -> 持久化
     *
     * @param document 文档对象
     * @param config   索引策略配置（来自数据库的 JSON，已反序列化为具体的配置类）
     */
    void buildIndex(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config);

    /**
     * 构建索引（带进度回调）
     */
    default void buildIndex(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config,
                            IndexingProgressCallback callback) {
        buildIndex(kb, document, config);
    }

    /**
     * 删除索引
     *
     * @param documentIds 文档 ID 列表
     * @param kbId        知识库 ID
     */
    void deleteIndex(KnowledgeBase kb, List<Long> documentIds);

    /**
     * 删除整个知识库的索引
     *
     * @param kb 知识库对象
     */
    void deleteIndex(KnowledgeBase kb);

    /**
     * 重建索引
     *
     * @param document 文档对象
     * @param config   索引策略配置
     */
    void rebuildIndex(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config);

    /**
     * 重建索引（带进度回调）
     */
    default void rebuildIndex(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config,
                              IndexingProgressCallback callback) {
        rebuildIndex(kb, document, config);
    }

    /**
     * 基于现有 Chunk 重建索引
     *
     * @param kb       知识库
     * @param document 文档
     * @param config   索引策略配置
     */
    void rebuildIndexFromChunks(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config);

    /**
     * 基于现有 Chunk 重建索引（带进度回调）
     */
    default void rebuildIndexFromChunks(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config,
                                        IndexingProgressCallback callback) {
        rebuildIndexFromChunks(kb, document, config);
    }
}