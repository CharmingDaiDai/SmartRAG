package com.mtmn.smartdoc.enums;

/**
 * 知识库状态
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public enum KnowledgeBaseStatus {
    /**
     * 创建中 - 知识库正在初始化
     */
    CREATING,

    /**
     * 就绪 - 知识库已创建，可以上传文档
     */
    READY,

    /**
     * 索引中 - 正在构建索引
     */
    INDEXING,

    /**
     * 已索引 - 索引构建完成，可以进行问答
     */
    INDEXED,

    /**
     * 错误 - 索引构建失败或其他错误
     */
    ERROR
}