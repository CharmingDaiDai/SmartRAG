package com.mtmn.smartrag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author charmingdaidai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "关键词提取结果")
public class KeywordsResult {
    // 提取的关键词列表
    @Schema(description = "提取的关键词列表")
    private List<String> keywords;
    // 关键词提取时间戳
    @Schema(description = "关键词提取时间戳")
    private long timestamp;
    // 文档原始长度
    @Schema(description = "文档原始长度")
    private int originalLength;
}