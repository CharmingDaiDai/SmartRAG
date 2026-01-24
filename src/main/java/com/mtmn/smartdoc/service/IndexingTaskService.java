package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.enums.IndexingTaskType;
import com.mtmn.smartdoc.po.IndexingTask;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 索引任务服务接口
 *
 * @author charmingdaidai
 * @version 2.0
 */
public interface IndexingTaskService {

    /**
     * 提交索引任务
     * 如果已有进行中的任务，返回现有任务；否则创建新任务
     *
     * @param userId      用户 ID
     * @param kbId        知识库 ID
     * @param taskType    任务类型
     * @param documentIds 文档 ID 列表
     * @return 任务实体
     */
    IndexingTask submitTask(Long userId, Long kbId, IndexingTaskType taskType, List<Long> documentIds);

    /**
     * 检查是否有进行中的任务
     *
     * @param userId 用户 ID
     * @param kbId   知识库 ID
     * @return 是否有进行中的任务
     */
    boolean hasRunningTask(Long userId, Long kbId);

    /**
     * 获取进行中的任务
     *
     * @param userId 用户 ID
     * @param kbId   知识库 ID
     * @return 任务实体，如果没有则返回 null
     */
    IndexingTask getRunningTask(Long userId, Long kbId);

    /**
     * 获取任务详情
     *
     * @param taskId 任务 ID
     * @return 任务实体
     */
    IndexingTask getTask(Long taskId);

    /**
     * 异步执行索引任务
     * 该方法会在独立线程中执行
     *
     * @param taskId 任务 ID
     */
    void executeIndexingAsync(Long taskId);

    /**
     * 订阅任务进度（创建 SSE 连接）
     *
     * @param userId 用户 ID
     * @param kbId   知识库 ID
     * @return SSE 发射器
     */
    SseEmitter subscribe(Long userId, Long kbId);
}
