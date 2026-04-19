package com.mtmn.smartrag.service;

import com.mtmn.smartrag.enums.IndexingTaskType;
import com.mtmn.smartrag.po.IndexingTask;
import com.mtmn.smartrag.vo.IndexingTaskProgressVO;

import java.util.List;

/**
 * 索引任务服务接口
 *
 * @author charmingdaidai
 * @version 3.0
 */
public interface IndexingTaskService {

    /**
     * 提交索引任务
     * 如果已有进行中的任务，返回现有任务；否则创建新任务
     */
    IndexingTask submitTask(Long userId, Long kbId, IndexingTaskType taskType, List<Long> documentIds);

    /**
     * 检查是否有进行中的任务
     */
    boolean hasRunningTask(Long userId, Long kbId);

    /**
     * 获取进行中的任务
     */
    IndexingTask getRunningTask(Long userId, Long kbId);

    /**
     * 获取任务详情
     */
    IndexingTask getTask(Long taskId);

    /**
     * 异步执行索引任务（在独立线程中执行）
     */
    void executeIndexingAsync(Long taskId);

    /**
     * 查询最近一次任务进度（供前端轮询）
     *
     * @param userId 用户 ID
     * @param kbId   知识库 ID
     * @return 任务进度 VO，无任务时返回 null
     */
    IndexingTaskProgressVO getLatestTaskProgress(Long userId, Long kbId);
}

