package com.mtmn.smartdoc.strategy;

import java.util.List;
import java.util.Map;

/**
 * 存储策略接口（用于 Milvus 等向量存储）
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public interface StorageStrategy {

    /**
     * 存储索引数据
     *
     * @param kbId 知识库ID
     * @param data 索引数据
     */
    void storeIndex(String kbId, IndexData data);

    /**
     * 检索数据
     *
     * @param kbId    知识库ID
     * @param request 搜索请求
     * @return 搜索结果列表
     */
    List<SearchResult> search(String kbId, SearchRequest request);

    /**
     * 删除索引
     *
     * @param kbId 知识库ID
     */
    void deleteIndex(String kbId);

    /**
     * 更新部分索引
     *
     * @param kbId  知识库ID
     * @param items 更新项列表
     */
    void updatePartialIndex(String kbId, List<IndexUpdateItem> items);

    /**
     * 索引数据类
     */
    class IndexData {
        private List<VectorItem> vectors;
        private Map<String, Object> metadata;

        public List<VectorItem> getVectors() {
            return vectors;
        }

        public void setVectors(List<VectorItem> vectors) {
            this.vectors = vectors;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * 向量项类
     */
    class VectorItem {
        private String id;
        private List<Float> vector;
        private String content;
        private Map<String, Object> metadata;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<Float> getVector() {
            return vector;
        }

        public void setVector(List<Float> vector) {
            this.vector = vector;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * 搜索请求类
     */
    class SearchRequest {
        private List<Float> queryVector;
        private int topK;
        private double threshold;
        private Map<String, Object> filters;

        public List<Float> getQueryVector() {
            return queryVector;
        }

        public void setQueryVector(List<Float> queryVector) {
            this.queryVector = queryVector;
        }

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public Map<String, Object> getFilters() {
            return filters;
        }

        public void setFilters(Map<String, Object> filters) {
            this.filters = filters;
        }
    }

    /**
     * 搜索结果类
     */
    class SearchResult {
        private String id;
        private double score;
        private String content;
        private Map<String, Object> metadata;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }

    /**
     * 索引更新项类
     */
    class IndexUpdateItem {
        private String id;
        private List<Float> vector;
        private String content;
        private Map<String, Object> metadata;
        private UpdateType updateType;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<Float> getVector() {
            return vector;
        }

        public void setVector(List<Float> vector) {
            this.vector = vector;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }

        public UpdateType getUpdateType() {
            return updateType;
        }

        public void setUpdateType(UpdateType updateType) {
            this.updateType = updateType;
        }

        public enum UpdateType {
            ADD, UPDATE, DELETE
        }
    }
}