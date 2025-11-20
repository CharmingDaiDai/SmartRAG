package com.mtmn.smartdoc.config;

import com.mtmn.smartdoc.enums.EnhancementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检索配置
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalConfig {

    /**
     * 返回前 K 个结果
     */
    @Builder.Default
    private Integer topK = 5;

    /**
     * 相似度阈值（0.0-1.0）
     */
    @Builder.Default
    private Double threshold = 0.7;

    /**
     * 最大检索数量
     */
    @Builder.Default
    private Integer maxResults = 10;

    /**
     * 启用的增强算法列表
     */
    private List<EnhancementType> enhancements;

    /**
     * 使用的 LLM 模型 ID
     */
    private String llmModelId;

    /**
     * 使用的 Rerank 模型 ID
     */
    private String rerankModelId;

    /**
     * 增强算法的额外参数
     */
    @Builder.Default
    private Map<String, Object> enhancementParams = new HashMap<>();

    /**
     * 验证配置
     */
    public void validate() {
        if (topK == null || topK < 1 || topK > 100) {
            throw new IllegalArgumentException("topK must be between 1 and 100");
        }
        if (threshold == null || threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold must be between 0.0 and 1.0");
        }
        if (maxResults == null || maxResults < 1 || maxResults > 1000) {
            throw new IllegalArgumentException("maxResults must be between 1 and 1000");
        }
        if (topK > maxResults) {
            throw new IllegalArgumentException("topK cannot be greater than maxResults");
        }
    }
}