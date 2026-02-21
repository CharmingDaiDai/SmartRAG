package com.mtmn.smartdoc.model.dto;

import dev.langchain4j.data.embedding.Embedding;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 向量化响应DTO
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResponse {

    /**
     * 向量列表
     */
    private List<Embedding> embeddings;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 向量维度
     */
    private Integer dimension;

    /**
     * Token使用情况
     */
    private TokenUsage tokenUsage;

    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;

    /**
     * Token使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        /**
         * 总token数
         */
        private Integer totalTokens;
    }
}