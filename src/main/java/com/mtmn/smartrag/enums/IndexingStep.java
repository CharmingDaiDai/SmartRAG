package com.mtmn.smartrag.enums;

/**
 * 索引构建步骤
 *
 * @author charmingdaidai
 * @version 3.0
 */
public enum IndexingStep {
    /**
     * 从 MinIO 读取文件
     */
    READING("读取文档"),

    /**
     * 解析文档内容（Markdown / Tika）
     */
    PARSING("解析文档"),

    /**
     * 切分文档（NaiveRAG 简单分块）
     */
    CHUNKING("切分文档"),

    /**
     * 构建语义树（HiSem：MyNode→TreeNode + titlePath）
     */
    TREE_BUILDING("构建语义树"),

    /**
     * LLM 语义增强（HiSem：自下而上摘要聚合）
     */
    LLM_ENRICHING("语义增强中"),

    /**
     * 持久化到 MySQL（Chunk / TreeNode）
     */
    SAVING("保存数据"),

    /**
     * 调用 Embedding 模型向量化
     */
    EMBEDDING("向量化中"),

    /**
     * 写入 Milvus 向量库
     */
    STORING("存储向量");

    private final String displayName;

    IndexingStep(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
