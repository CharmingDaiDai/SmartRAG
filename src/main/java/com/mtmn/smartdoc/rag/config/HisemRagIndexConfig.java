package com.mtmn.smartdoc.rag.config;

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
public class HisemRagIndexConfig extends IndexStrategyConfig {

    /**
     * 切分最大长度（字符数）
     */
    @Builder.Default
    private Integer maxLength = 2048;

    /**
     * 是否启用标题增强（自上而下路径传递）
     */
    @Builder.Default
    private Boolean enableTitleEnhancement = true;

    /**
     * 是否启用语义压缩（自下而上 LLM 摘要 + 知识点聚合）
     * 对应前端"语义压缩"开关，开启后需提供 llmModelId
     */
    @Builder.Default
    private Boolean enableSemanticCompression = true;

    /**
     * 用于提取摘要和知识点的 LLM 模型 ID（enableSemanticCompression=true 时必填）
     */
    private String llmModelId;

    /**
     * 最大树深度
     */
    @Builder.Default
    private Integer maxTreeDepth = 6;

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
        if (Boolean.TRUE.equals(enableSemanticCompression)
                && (llmModelId == null || llmModelId.isEmpty())) {
            throw new IllegalArgumentException("llmModelId is required when enableSemanticCompression is enabled");
        }
    }
}