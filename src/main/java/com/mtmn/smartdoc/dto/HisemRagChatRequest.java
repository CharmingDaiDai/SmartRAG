package com.mtmn.smartdoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author charmingdaidai
 * @version 1.0
 * @date 2025/12/1 18:02
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hisem RAG / Hisem RAG Fast 对话请求")
public class HisemRagChatRequest extends RagChatRequest{

    @Schema(description = "最大检索结果数量，默认 10")
    private Integer maxTopK = 10;

    @Schema(description = "是否启用查询重写，默认 false")
    private Boolean enableQueryRewrite = false;

    @Schema(description = "是否启用查询分解，默认 false")
    private Boolean enableQueryDecomposition = false;

    @Schema(description = "是否启用意图识别，默认 false")
    private Boolean enableIntentRecognition = false;

    @Schema(description = "是否启用 Hyde，默认 false")
    private Boolean enableHyde = false;
}