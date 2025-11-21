package com.mtmn.smartdoc.config;

import com.mtmn.smartdoc.enums.IndexStrategyType;
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
public class NaiveRAGConfig extends IndexStrategyConfig {

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
        if (chunkSize == null || chunkSize <= 0 || chunkSize > 10000) {
            throw new IllegalArgumentException("chunkSize must be between 1 and 10000");
        }
        if (chunkOverlap == null || chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be between 0 and chunkSize");
        }
        if (separator == null) {
            throw new IllegalArgumentException("separator cannot be null");
        }
    }
}