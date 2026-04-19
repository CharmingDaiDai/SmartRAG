package com.mtmn.smartrag.rag.config;

import com.mtmn.smartrag.enums.IndexStrategyType;
import com.mtmn.smartrag.enums.SplitterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * NaiveRAG 索引策略配置
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
public class NaiveRagIndexConfig extends IndexStrategyConfig {

    /**
     * 切分器类型
     */
    @Builder.Default
    private SplitterType splitterType = SplitterType.BY_PARAGRAPH;

    /**
     * 切分块大小（字符数）
     */
    @Builder.Default
    private Integer chunkSize = 512;

    /**
     * 切分块重叠大小（字符数）
     */
    @Builder.Default
    private Integer chunkOverlap = 100;

    /**
     * 分隔符
     */
    @Builder.Default
    private String separator = "\n\n";

    @Override
    public IndexStrategyType getStrategyType() {
        return IndexStrategyType.NAIVE_RAG;
    }

    @Override
    public void validate() {
        if (chunkSize == null || chunkSize <= 0 || chunkSize > 8192) {
            throw new IllegalArgumentException("chunkSize must be between 1 and 8192");
        }
        if (chunkOverlap == null || chunkOverlap < 0 || chunkOverlap >= chunkSize * 0.2) {
            throw new IllegalArgumentException("chunkOverlap must be between 0 and chunkSize");
        }
        if (separator == null) {
            throw new IllegalArgumentException("separator cannot be null");
        }
    }
}