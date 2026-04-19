package com.mtmn.smartrag.rag.sadp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtmn.smartrag.vo.RetrievalResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * SADP DAG 任务节点
 *
 * <p>表示 SADP 有向无环图中的一个子任务单元，支持三种算子类型：
 * Scoped_Retrieve（范围检索）、Get_Summary（直读摘要）、Generate（LLM 生成）。
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Data
public class TaskNode {

    /**
     * 任务唯一标识（如 "T1", "T2"）
     */
    private String id;

    /**
     * 算子类型
     */
    private TaskType type;

    /**
     * 检索关键词（Scoped_Retrieve）或生成指令（Generate），Get_Summary 时为空
     */
    private String query;

    /**
     * 目标节点 ID，用于限制检索/摘要范围（Scoped_Retrieve / Get_Summary 使用）
     */
    private String nodeId;

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
     * Scoped_Retrieve 算子的原始检索结果（用于向前端传递引用，不序列化）
     */
    @JsonIgnore
    private List<RetrievalResult> rawResults;

    /**
     * 是否为最终 Generate 任务（由 executeDag 在执行前标记）。
     * 为 true 时，executeGenerate 直接返回 prompt 而非同步调用 LLM，
     * 由 RAGServiceImpl 使用该 prompt 发起流式调用，避免二次 LLM 调用。
     */
    @JsonIgnore
    private boolean terminalGenerate;

    /**
     * 算子类型枚举
     */
    public enum TaskType {
        /** 在指定 node_id 范围内进行向量检索 */
        Scoped_Retrieve,
        /** 直接读取指定 node_id 的摘要字段，不走向量检索 */
        Get_Summary,
        /** 综合前置任务结果，调用 LLM 生成回答或中间结论 */
        Generate
    }

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
