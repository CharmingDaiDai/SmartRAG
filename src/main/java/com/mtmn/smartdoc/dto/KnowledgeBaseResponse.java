package com.mtmn.smartdoc.dto;

import com.mtmn.smartdoc.enums.IndexStrategyType;
import io.swagger.v3.oas.annotations.media.Schema;
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
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库响应")
public class KnowledgeBaseResponse {

    /**
     * 知识库 ID
     */
    @Schema(description = "知识库 ID")
    private Long id;

    /**
     * 知识库名称
     */
    @Schema(description = "知识库名称")
    private String name;

    /**
     * 知识库描述
     */
    @Schema(description = "知识库描述")
    private String description;

    /**
     * 索引策略类型
     */
    @Schema(description = "索引策略类型")
    private IndexStrategyType indexStrategyType;

    /**
     * 索引策略配置
     */
    @Schema(description = "索引策略配置")
    private String indexStrategyConfig;

    /**
     * Embedding 模型 ID
     */
    @Schema(description = "Embedding 模型 ID")
    private String embeddingModelId;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID")
    private Long userId;

    /**
     * 文档数量
     */
    @Schema(description = "文档数量")
    private Long documentCount;

    /**
     * 已索引文档数量
     */
    @Schema(description = "已索引文档数量")
    private Long indexedDocumentCount;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}