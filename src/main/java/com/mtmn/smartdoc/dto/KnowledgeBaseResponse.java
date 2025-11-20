package com.mtmn.smartdoc.dto;

import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.enums.KnowledgeBaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库响应 DTO
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseResponse {

    /**
     * 知识库 ID
     */
    private Long id;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 索引策略类型
     */
    private IndexStrategyType indexStrategyType;

    /**
     * 索引策略配置
     */
    private String indexStrategyConfig;

    /**
     * Embedding 模型 ID
     */
    private String embeddingModelId;

    /**
     * 状态
     */
    private KnowledgeBaseStatus status;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 文档数量
     */
    private Long documentCount;

    /**
     * 已索引文档数量
     */
    private Long indexedDocumentCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}