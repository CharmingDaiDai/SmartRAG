package com.mtmn.smartdoc.config;

import com.mtmn.smartdoc.enums.IndexStrategyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * HisemRAG 索引策略配置
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HisemRAGConfig extends IndexStrategyConfig {

    /**
     * 切分最大长度（字符数）
     */
    @Builder.Default
    private Integer maxLength = 2048;

    /**
     * 是否启用标题增强
     */
    @Builder.Default
    private Boolean enableTitleEnhancement = true;

    // TODO 摘要和知识点提取是一个功能，需要进行合并
    /**
     * 是否启用摘要提取
     */
    @Builder.Default
    private Boolean enableSummary = true;

    /**
     * 是否启用核心知识点提取
     */
    @Builder.Default
    private Boolean enableKeyKnowledge = true;

    /**
     * 用于提取摘要和知识点的 LLM 模型 ID
     */
    private String llmModelId;

    /**
     * 最大树深度
     */
    @Builder.Default
    private Integer maxTreeDepth = 5;

    @Override
    public IndexStrategyType getStrategyType() {
        return IndexStrategyType.HISEM_RAG;
    }

    @Override
    public void validate() {
        if (maxLength == null || maxLength <= 0 || maxLength > 32000) {
            throw new IllegalArgumentException("maxLength must be between 1 and 32000");
        }
        if (maxTreeDepth == null || maxTreeDepth < 1 || maxTreeDepth > 10) {
            throw new IllegalArgumentException("maxTreeDepth must be between 1 and 10");
        }
        if ((enableSummary || enableKeyKnowledge) && (llmModelId == null || llmModelId.isEmpty())) {
            throw new IllegalArgumentException("llmModelId is required when summary or keyKnowledge is enabled");
        }
    }
}