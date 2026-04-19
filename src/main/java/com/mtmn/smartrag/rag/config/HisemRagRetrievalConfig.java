package com.mtmn.smartrag.rag.config;

import com.mtmn.smartrag.enums.IndexStrategyType;
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
public class HisemRagRetrievalConfig extends RetrievalStrategyConfig {

    private Long id;

    private String embeddingModelId;

    private String llmModelId;

    @Builder.Default
    private Integer maxTopK = 10;

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
        return IndexStrategyType.HISEM_RAG;
    }
}