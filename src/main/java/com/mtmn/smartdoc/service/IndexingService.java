package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.enums.DocumentIndexStatus;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;

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
     * 更新文档索引状态（带事务）
     * 该方法需要在接口中声明，以便通过 self 代理调用
     *
     * @param documentId 文档 ID
     * @param status     状态
     */
    void updateDocumentStatus(Long documentId, DocumentIndexStatus status);
}