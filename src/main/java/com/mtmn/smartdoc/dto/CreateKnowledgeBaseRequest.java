package com.mtmn.smartdoc.dto;

import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建知识库请求 DTO
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "创建知识库请求")
public class CreateKnowledgeBaseRequest {

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
     * 索引策略配置对象
     * <p>前端传递时需包含type字段用于多态识别:</p>
     * <pre>{@code
     * {
     *   "type": "NAIVE_RAG",
     *   "chunkSize": 1000,
     *   "chunkOverlap": 100,
     *   "separator": "\\n\\n"
     * }
     * }</pre>
     */
    @Schema(description = "索引策略配置对象")
    private IndexStrategyConfig indexStrategyConfig;

    /**
     * Embedding 模型 ID
     */
    @Schema(description = "Embedding 模型 ID")
    private String embeddingModelId;
}