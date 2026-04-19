package com.mtmn.smartrag.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 向量化请求DTO
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {

    /**
     * 待向量化的文本列表
     */
    private List<String> texts;

    /**
     * 批处理大小(某些模型有限制)
     */
    private Integer batchSize;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
}