package com.mtmn.smartdoc.service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天请求DTO
 * 
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    /**
     * 消息列表
     */
    private List<Message> messages;
    
    /**
     * 温度参数 (0.0-1.0),控制生成的随机性
     */
    private Double temperature;
    
    /**
     * 最大token数
     */
    private Integer maxTokens;
    
    /**
     * Top P 采样参数
     */
    private Double topP;
    
    /**
     * 频率惩罚
     */
    private Double frequencyPenalty;
    
    /**
     * 存在惩罚
     */
    private Double presencePenalty;
    
    /**
     * 停止序列
     */
    private List<String> stop;
    
    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
    
    /**
     * 聊天消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色: system, user, assistant
         */
        private String role;
        
        /**
         * 消息内容
         */
        private String content;
        
        /**
         * 消息名称(可选)
         */
        private String name;
    }
}
