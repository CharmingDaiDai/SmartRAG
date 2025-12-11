package com.mtmn.smartdoc.rag;

import com.mtmn.smartdoc.enums.EnhancementType;

/**
 * 检索增强算法接口
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public interface Enhancement {

    /**
     * 获取增强类型
     */
    EnhancementType getType();

    /**
     * 应用增强
     *
     * @param originalQuery 原始查询
     * @param context       增强上下文
     * @return 增强后的查询
     */
    EnhancedQuery enhance(String originalQuery, EnhancementContext context);

    /**
     * 是否支持指定的索引策略
     *
     * @param strategyType 索引策略类型
     * @return 是否支持
     */
    boolean supports(com.mtmn.smartdoc.enums.IndexStrategyType strategyType);

    /**
     * 增强后的查询类
     */
    class EnhancedQuery {
        private String query;
        private java.util.List<String> subQueries;
        private java.util.Map<String, Object> additionalParams;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public java.util.List<String> getSubQueries() {
            return subQueries;
        }

        public void setSubQueries(java.util.List<String> subQueries) {
            this.subQueries = subQueries;
        }

        public java.util.Map<String, Object> getAdditionalParams() {
            return additionalParams;
        }

        public void setAdditionalParams(java.util.Map<String, Object> additionalParams) {
            this.additionalParams = additionalParams;
        }
    }

    /**
     * 增强上下文类
     */
    class EnhancementContext {
        private Long kbId;
        private String llmModelId;
        private java.util.Map<String, Object> params;

        public Long getKbId() {
            return kbId;
        }

        public void setKbId(Long kbId) {
            this.kbId = kbId;
        }

        public String getLlmModelId() {
            return llmModelId;
        }

        public void setLlmModelId(String llmModelId) {
            this.llmModelId = llmModelId;
        }

        public java.util.Map<String, Object> getParams() {
            return params;
        }

        public void setParams(java.util.Map<String, Object> params) {
            this.params = params;
        }
    }
}