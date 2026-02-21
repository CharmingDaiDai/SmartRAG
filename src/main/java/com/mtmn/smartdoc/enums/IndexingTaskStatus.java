package com.mtmn.smartdoc.enums;

/**
 * 索引任务状态
 *
 * @author charmingdaidai
 * @version 2.0
 */
public enum IndexingTaskStatus {
    /**
     * 等待执行
     */
    PENDING,

    /**
     * 正在执行
     */
    RUNNING,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 失败
     */
    FAILED
}
