package com.mtmn.smartrag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * @author charmingdaidai
 * @version 1.0
 * @date 2025/12/1 18:02
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Naive RAG 对话请求")
public class NaiveRagChatRequest extends RagChatRequest{

    @Schema(description = "检索结果数量，默认 5")
    private Integer topK = 5;

    @Schema(description = "相似度阈值，默认 0.5（余弦相似度范围 [-1, 1]，建议 0.3-0.7）")
    private Double threshold = 0.5;

    @Schema(description = "是否启用查询重写，默认 false")
    private Boolean enableQueryRewrite = false;

    @Schema(description = "是否启用查询分解，默认 false")
    private Boolean enableQueryDecomposition = false;

    @Schema(description = "是否启用意图识别，默认 false")
    private Boolean enableIntentRecognition = false;

    @Schema(description = "是否启用 Hyde，默认 false")
    private Boolean enableHyde = false;
}