package com.mtmn.smartrag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author charmingdaidai
 */
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表板统计数据")
public class DashboardStatisticsDto {

    @Schema(description = "知识库数量")
    private long knowledgeBases;

    @Schema(description = "文档总数")
    private long documents;

    @Schema(description = "对话统计信息")
    private ConversationStats conversationStats;

    @Schema(description = "热门查询词云")
    private List<WordCloudItem> wordCloud;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "对话统计")
    public static class ConversationStats {

        @Schema(description = "对话总次数")
        private long total;

        @Schema(description = "最近7天每天的对话次数")
        private List<DailyConversationCount> last7Days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "每天的对话次数")
    public static class DailyConversationCount {

        @Schema(description = "日期，格式 yyyy-MM-dd")
        private String date;

        @Schema(description = "当天对话次数")
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "词云项")
    public static class WordCloudItem {

        @Schema(description = "展示文本")
        private String text;

        @Schema(description = "所属的用户或来源名称")
        private String name;

        @Schema(description = "热度值")
        private double value;
    }
}