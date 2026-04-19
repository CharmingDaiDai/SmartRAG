package com.mtmn.smartrag.rag.config;

import com.mtmn.smartrag.enums.IndexStrategyType;
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
public class HisemRagFastIndexConfig extends IndexStrategyConfig {

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

    @Override
    public IndexStrategyType getStrategyType() {
        return IndexStrategyType.HISEM_RAG_FAST;
    }

    @Override
    public void validate() {
        if (maxLength == null || maxLength <= 0 || maxLength > 32000) {
            throw new IllegalArgumentException("maxLength must be between 1 and 32000");
        }
    }
}