package com.mtmn.smartrag.service;

import com.mtmn.smartrag.enums.DocumentIndexStatus;
import com.mtmn.smartrag.po.KnowledgeBase;
import com.mtmn.smartrag.rag.config.IndexStrategyConfig;

import java.util.List;

/**
 * 索引构建服务接口
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public interface IndexingService {

    /**
     * 提交单个文档索引任务
     *
     * @param documentId 文档 ID
     * @param kbId       知识库 ID
     */
    void submitIndexingTask(Long documentId, Long kbId);

    /**
     * 提交批量索引任务
     *
     * @param kbId        知识库 ID
     * @param documentIds 文档 ID 列表
     */
    void submitBatchIndexingTask(Long kbId, List<Long> documentIds);

    /**
     * 重建知识库索引
     *
     * @param kbId 知识库 ID
     */
    void rebuildIndex(Long kbId);

    /**
     * 取消索引任务
     *
     * @param documentId 文档 ID
     */
    void cancelIndexing(Long documentId);

    /**
     * 执行索引构建（带事务）
     * 该方法需要在接口中声明，以便通过 self 代理调用
     *
     * @param documentId  文档 ID
     * @param kb          知识库
     * @param indexConfig 索引策略配置
     */
    void executeIndexing(Long documentId, KnowledgeBase kb, IndexStrategyConfig indexConfig);

    /**
     * 执行索引构建（带事务和进度回调）
     *
     * @param documentId  文档 ID
     * @param kb          知识库
     * @param indexConfig 索引策略配置
     * @param callback    进度回调
     */
    void executeIndexing(Long documentId, KnowledgeBase kb, IndexStrategyConfig indexConfig, IndexingProgressCallback callback);

    /**
     * 更新文档索引状态（带事务）
     * 该方法需要在接口中声明，以便通过 self 代理调用
     *
     * @param documentId 文档 ID
     * @param status     状态
     */
    void updateDocumentStatus(Long documentId, DocumentIndexStatus status);

    /**
     * 基于现有 Chunk 重建文档索引
     *
     * @param documentId 文档 ID
     * @param kbId       知识库 ID
     */
    void rebuildDocumentIndexFromChunks(Long documentId, Long kbId);

    /**
     * 批量基于现有 Chunk 重建文档索引
     *
     * @param kbId        知识库 ID
     * @param documentIds 文档 ID 列表
     */
    void batchRebuildDocumentIndexFromChunks(Long kbId, List<Long> documentIds);

    /**
     * 执行基于 Chunk 的索引重建（带事务）
     * 该方法需要在接口中声明，以便通过 self 代理调用
     *
     * @param documentId  文档 ID
     * @param kb          知识库
     * @param indexConfig 索引策略配置
     */
    void executeRebuildIndexFromChunks(Long documentId, KnowledgeBase kb, IndexStrategyConfig indexConfig);

    /**
     * 执行基于 Chunk 的索引重建（带事务和进度回调）
     *
     * @param documentId  文档 ID
     * @param kb          知识库
     * @param indexConfig 索引策略配置
     * @param callback    进度回调
     */
    void executeRebuildIndexFromChunks(Long documentId, KnowledgeBase kb, IndexStrategyConfig indexConfig, IndexingProgressCallback callback);
}