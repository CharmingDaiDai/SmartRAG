package com.mtmn.smartdoc.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description 问题分解结果
 * @date 2025/8/3 15:10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryDecomposeResult {
    
    /**
     * 分解后的查询项列表
     */
    private List<QueryItem> queries;
    
    /**
     * 解析是否成功
     */
    private boolean success;
    
    /**
     * 原始响应
     */
    private String rawResponse;
    
    /**
     * 解析失败原因
     */
    private String reason;
    
    /**
     * 查询项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueryItem {
        /**
         * 查询类型：检索、回答等
         */
        private String type;
        
        /**
         * 查询内容
         */
        private String query;
    }
}
