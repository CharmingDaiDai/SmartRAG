package com.mtmn.smartrag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author charmingdaidai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "摘要生成结果")
public class SummaryResult {
    // 文档摘要结果
    @Schema(description = "文档摘要结果")
    private String summary;
    // 摘要生成时间戳
    @Schema(description = "摘要生成时间戳")
    private long timestamp;
    // 文档原始长度
    @Schema(description = "文档原始长度")
    private int originalLength;
    // 摘要长度
    @Schema(description = "摘要长度")
    private int summaryLength;
}