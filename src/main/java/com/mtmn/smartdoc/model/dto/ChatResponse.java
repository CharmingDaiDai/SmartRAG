package com.mtmn.smartdoc.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 聊天响应DTO
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * AI生成的回复内容
     */
    private String content;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 模型名称
     */
    private String model;

    /**
     * Token使用情况
     */
    private TokenUsage tokenUsage;

    /**
     * 完成原因: stop, length, content_filter等
     */
    private String finishReason;

    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;

    /**
     * Token使用统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        /**
         * 提示token数
         */
        private Integer promptTokens;

        /**
         * 完成token数
         */
        private Integer completionTokens;

        /**
         * 总token数
         */
        private Integer totalTokens;
    }
}