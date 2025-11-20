package com.mtmn.smartdoc.service;

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
     * 执行单个文档索引 (异步)
     *
     * @param documentId 文档 ID
     * @param kbId       知识库 ID
     */
    void executeIndexing(Long documentId, Long kbId);

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
}