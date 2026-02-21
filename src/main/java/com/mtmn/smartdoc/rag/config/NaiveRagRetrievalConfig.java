package com.mtmn.smartdoc.rag.config;

import com.mtmn.smartdoc.enums.IndexStrategyType;
import lombok.*;

/**
 * NaiveRAG 检索策略配置
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-12-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NaiveRagRetrievalConfig extends RetrievalStrategyConfig {

    private Long kbId;

    private String embeddingModelId;

    private String llmModelId;

    private String rerankModelId;

    @Builder.Default
    private Integer topK = 5;

    @Builder.Default
    private Double threshold = 0.0;

    @Builder.Default
    private Boolean enableQueryRewrite = false;

    @Builder.Default
    private Boolean enableQueryDecomposition = false;

    @Builder.Default
    private Boolean enableIntentRecognition = false;

    @Builder.Default
    private Boolean enableHyde = false;

    @Override
    public IndexStrategyType getStrategyType() {
        return IndexStrategyType.NAIVE_RAG;
    }
}