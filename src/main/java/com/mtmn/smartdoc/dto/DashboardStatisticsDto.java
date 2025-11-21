package com.mtmn.smartdoc.dto;

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
@Schema(description = "仪表板统计数据")
public class DashboardStatisticsDto {
    // 用户文档总数
    @Schema(description = "用户文档总数")
    private long documents;

    // 文档分析总数
    @Schema(description = "文档分析总数")
    private long analysis;

    // 关键词提取数
    @Schema(description = "关键词提取数")
    private long keywords;

    // 安全检查数
    @Schema(description = "安全检查数")
    private long security;

    // 生成摘要数
    @Schema(description = "生成摘要数")
    private long summary;

    // 内容润色数
    @Schema(description = "内容润色数")
    private long polish;
}