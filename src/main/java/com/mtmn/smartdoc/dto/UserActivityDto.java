package com.mtmn.smartdoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * @author charmingdaidai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityDto {

    private Long id;

    private String type;

    private Long documentId;

    private String documentName;

    private String description;

    private LocalDateTime createdAt;

    // 友好的时间表示，如"2分钟前"
    private String timestamp;

    /**
     * 将LocalDateTime格式化为友好的时间表示
     */
    public static String formatTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        LocalDateTime now = LocalDateTime.now();
        long seconds = ChronoUnit.SECONDS.between(dateTime, now);

        if (seconds < 60) {
            return "刚刚";
        }

        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        if (minutes < 60) {
            return minutes + "分钟前";
        }

        long hours = ChronoUnit.HOURS.between(dateTime, now);
        if (hours < 24) {
            return hours + "小时前";
        }

        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days < 30) {
            return days + "天前";
        }

        // 如果时间太久远，则显示具体日期
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return dateTime.format(formatter);
    }
}