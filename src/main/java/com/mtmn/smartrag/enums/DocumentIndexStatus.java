package com.mtmn.smartrag.enums;

/**
 * 文档索引状态
 *
 * @author charmingdaidai
 * @version 3.0
 */
public enum DocumentIndexStatus {
    /**
     * 已上传 - 文档已上传到 MinIO，等待处理
     */
    UPLOADED("已上传"),

    /**
     * 读取中 - 正在从 MinIO 读取文件
     */
    READING("读取中"),

    /**
     * 解析中 - 正在解析文档内容
     */
    PARSING("解析中"),

    /**
     * 切分中 - 正在切分文档
     */
    CHUNKING("切分中"),

    /**
     * 构建语义树 - HiSem 专有
     */
    TREE_BUILDING("构建语义树"),

    /**
     * 语义增强中 - HiSem 专有，LLM 提取摘要
     */
    LLM_ENRICHING("语义增强中"),

    /**
     * 保存中 - 持久化到 MySQL
     */
    SAVING("保存中"),

    /**
     * 向量化中 - 调用 Embedding 模型
     */
    EMBEDDING("向量化中"),

    /**
     * 存储向量 - 写入 Milvus
     */
    STORING("存储向量"),

    /**
     * 已索引 - 索引构建完成
     */
    INDEXED("已索引"),

    /**
     * 错误 - 处理失败
     */
    ERROR("错误");

    private final String displayName;

    DocumentIndexStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 从 IndexingStep 映射到 DocumentIndexStatus
     */
    public static DocumentIndexStatus fromIndexingStep(IndexingStep step) {
        return switch (step) {
            case READING -> READING;
            case PARSING -> PARSING;
            case CHUNKING -> CHUNKING;
            case TREE_BUILDING -> TREE_BUILDING;
            case LLM_ENRICHING -> LLM_ENRICHING;
            case SAVING -> SAVING;
            case EMBEDDING -> EMBEDDING;
            case STORING -> STORING;
        };
    }
}
