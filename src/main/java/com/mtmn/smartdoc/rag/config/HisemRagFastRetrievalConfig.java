package com.mtmn.smartdoc.rag.config;

import com.mtmn.smartdoc.enums.IndexStrategyType;
import lombok.*;

/**
 * HisemRAG Fast 检索策略配置
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-12-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HisemRagFastRetrievalConfig extends RetrievalStrategyConfig {

    private Long kbId;

    private String embeddingModelId;

    private String llmModelId;

    private String rerankModelId;

    /**
     * 最大检索结果数量
     */
    @Builder.Default
    private Integer maxTopK = 10;

    /**
     * 是否启用查询重写
     */
    @Builder.Default
    private Boolean enableQueryRewrite = false;

    /**
     * 是否启用查询分解
     */
    @Builder.Default
    private Boolean enableQueryDecomposition = false;

    /**
     * 是否启用意图识别
     */
    @Builder.Default
    private Boolean enableIntentRecognition = false;

    /**
     * 是否启用 HyDE
     */
    @Builder.Default
    private Boolean enableHyde = false;

    @Override
    public IndexStrategyType getStrategyType() {
        return IndexStrategyType.HISEM_RAG_FAST;
    }
}
