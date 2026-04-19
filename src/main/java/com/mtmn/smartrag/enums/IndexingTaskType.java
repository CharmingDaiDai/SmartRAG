package com.mtmn.smartrag.enums;

/**
 * 索引任务类型
 *
 * @author charmingdaidai
 * @version 2.0
 */
public enum IndexingTaskType {
    /**
     * 构建索引（完整流程：解析 -> 切分 -> 向量化 -> 存储）
     */
    INDEX,

    /**
     * 重建索引（基于现有 Chunk，只做向量化 -> 存储）
     */
    REBUILD
}
