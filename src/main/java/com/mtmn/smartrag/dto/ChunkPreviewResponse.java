package com.mtmn.smartrag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Chunk 预览 DTO
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chunk 预览响应")
public class ChunkPreviewResponse {

    /**
     * Chunk ID
     */
    @Schema(description = "Chunk ID")
    private Long id;

    /**
     * 文档 ID
     */
    @Schema(description = "文档 ID")
    private Long documentId;

    /**
     * Chunk 序号
     */
    @Schema(description = "Chunk 序号")
    private Integer chunkIndex;

    /**
     * 内容
     */
    @Schema(description = "内容")
    private String content;

    /**
     * 是否被修改
     */
    @Schema(description = "是否被修改")
    private Boolean isModified;

    /**
     * 是否已向量化
     */
    @Schema(description = "是否已向量化")
    private Boolean isVectorized;

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