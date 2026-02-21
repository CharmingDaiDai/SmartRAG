package com.mtmn.smartdoc.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检索结果 VO
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {

    /**
     * 内容
     */
    private String content;

    /**
     * 相似度分数
     */
    private double score;

    /**
     * 来源 ID（chunk_id 或 node_id）
     */
    private String sourceId;

    /**
     * 来源类型（"chunk" 或 "node"）
     */
    private String sourceType;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;
}
