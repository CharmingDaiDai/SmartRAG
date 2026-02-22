package com.mtmn.smartdoc.rag.sadp;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * SADP DAG 任务节点
 *
 * <p>表示自适应任务规划图中的一个子任务单元，记录任务描述、
 * 依赖关系、执行状态和执行结果。
 *
 * @author charmingdaidai
 * @version 1.0
 */
@Data
public class TaskNode {

    /**
     * 任务唯一标识（如 "t1", "t2"）
     */
    private String id;

    /**
     * 任务描述（具体的检索查询或推理目标）
     */
    private String description;

    /**
     * 前置任务 ID 列表（空表示无依赖，可并行执行）
     */
    private List<String> dependsOn = new ArrayList<>();

    /**
     * 任务执行状态
     */
    private TaskStatus status = TaskStatus.PENDING;

    /**
     * 任务执行结果文本
     */
    private String result;

    /**
     * 任务执行状态枚举
     */
    public enum TaskStatus {
        PENDING,
        RUNNING,
        DONE,
        FAILED
    }
}
