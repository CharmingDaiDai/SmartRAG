package com.mtmn.smartdoc.strategy;

import com.mtmn.smartdoc.config.RetrievalConfig;
import com.mtmn.smartdoc.enums.EnhancementType;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.po.KnowledgeBase;

import java.util.List;

/**
 * 检索策略接口
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public interface RetrievalStrategy {

    /**
     * 检索相关内容
     *
     * @param kb     知识库
     * @param query  查询字符串
     * @param config 检索配置
     * @return 检索结果
     */
    RetrievalResult retrieve(KnowledgeBase kb, String query, RetrievalConfig config);

    /**
     * 获取支持的增强算法类型
     */
    List<EnhancementType> getSupportedEnhancements();

    /**
     * 获取策略类型
     */
    IndexStrategyType getStrategyType();

    /**
     * 检索结果类
     */
    class RetrievalResult {
        private List<RetrievedItem> items;
        private int totalCount;
        private double maxScore;
        private long retrievalTimeMs;

        // Getters and Setters
        public List<RetrievedItem> getItems() {
            return items;
        }

        public void setItems(List<RetrievedItem> items) {
            this.items = items;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public double getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(double maxScore) {
            this.maxScore = maxScore;
        }

        public long getRetrievalTimeMs() {
            return retrievalTimeMs;
        }

        public void setRetrievalTimeMs(long retrievalTimeMs) {
            this.retrievalTimeMs = retrievalTimeMs;
        }
    }

    /**
     * 检索项类
     */
    class RetrievedItem {
        private String content;
        private double score;
        private String sourceId;  // chunk_id 或 node_id
        private String sourceType; // "chunk" 或 "node"
        private Object metadata;

        // Getters and Setters
        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public Object getMetadata() {
            return metadata;
        }

        public void setMetadata(Object metadata) {
            this.metadata = metadata;
        }
    }
}