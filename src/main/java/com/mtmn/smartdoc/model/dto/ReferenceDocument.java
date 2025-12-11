package com.mtmn.smartdoc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 参考文档 DTO
 * 
 * <p>用于在流式响应中发送检索到的参考文档信息</p>
 *
 * @author charmingdaidai
 * @version 1.0
 * @since 2025-11-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceDocument {

    /**
     * 文档标题/名称
     */
    private String title;

    /**
     * 相关性分数 (0.0 - 1.0)
     */
    private Double score;

    /**
     * 文档块内容
     */
    private String content;

    /**
     * 文档ID（可选）
     */
    private Long documentId;

    /**
     * 文档块ID（可选）
     */
    private String chunkId;

    /**
     * 元数据（可选，如页码、章节等）
     */
    private java.util.Map<String, Object> metadata;
}
