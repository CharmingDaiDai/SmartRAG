package com.mtmn.smartdoc.enums;

/**
 * 文档索引状态
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public enum DocumentIndexStatus {
    /**
     * 已上传 - 文档已上传到 MinIO，等待处理
     */
    UPLOADED,

    /**
     * 切分中 - 正在进行文档切分
     */
    CHUNKING,

    /**
     * 已切分 - 文档切分完成，可以预览
     */
    CHUNKED,

    /**
     * 索引中 - 正在进行向量化和索引构建
     */
    INDEXING,

    /**
     * 已索引 - 索引构建完成
     */
    INDEXED,

    /**
     * 错误 - 处理失败
     */
    ERROR
}