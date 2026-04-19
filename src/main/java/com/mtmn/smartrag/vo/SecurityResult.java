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
@Schema(description = "安全检查结果")
public class SecurityResult {
    // 敏感信息列表
    @Schema(description = "敏感信息列表")
    private List<SensitiveInfo> sensitiveInfoList;
    // 检测时间戳
    @Schema(description = "检测时间戳")
    private long timestamp;
    // 文档原始长度
    @Schema(description = "文档原始长度")
    private int originalLength;
    // 敏感信息总数
    @Schema(description = "敏感信息总数")
    private int totalCount;

    // 内部类：敏感信息
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "敏感信息")
    public static class SensitiveInfo {
        // 敏感信息类型（身份证号码、手机号码、银行卡信息等）
        @Schema(description = "敏感信息类型")
        private String type;
        // 敏感信息内容（可能会做脱敏处理）
        @Schema(description = "敏感信息内容")
        private String content;
        // 风险等级（高、中、低）
        @Schema(description = "风险等级")
        private String risk;
        // 敏感信息在文档中的位置
        @Schema(description = "敏感信息在文档中的位置")
        private Position position;
    }

    // 内部类：敏感信息位置
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "敏感信息位置")
    public static class Position {
        // 开始位置
        @Schema(description = "开始位置")
        private int start;
        // 结束位置
        @Schema(description = "结束位置")
        private int end;
    }
}