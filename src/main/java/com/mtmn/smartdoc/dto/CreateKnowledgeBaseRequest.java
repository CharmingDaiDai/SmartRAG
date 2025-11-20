package com.mtmn.smartdoc.dto;

import com.mtmn.smartdoc.enums.IndexStrategyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建知识库请求 DTO
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeBaseRequest {

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
     * 索引策略配置（JSON 字符串）
     */
    private String indexStrategyConfig;

    /**
     * Embedding 模型 ID
     */
    private String embeddingModelId;
}