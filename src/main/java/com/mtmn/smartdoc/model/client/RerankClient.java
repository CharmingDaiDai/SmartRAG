package com.mtmn.smartdoc.model.client;

import java.util.List;

/**
 * Rerank客户端统一接口
 *
 * <p>重排序功能,用于对检索结果进行重新排序,提升检索质量</p>
 * <p>当前为预留接口,未来实现</p>
 *
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
public interface RerankClient {

    /**
     * 文档重排序
     *
     * @param query     查询文本
     * @param documents 候选文档列表
     * @param topN      返回前N个结果
     * @return 重排序后的文档列表, 包含相关性分数
     */
    List<ScoredDocument> rerank(String query, List<String> documents, int topN);

    /**
     * 获取模型提供商类型
     */
    String getProviderType();

    /**
     * 获取模型ID
     */
    String getModelId();

    /**
     * 获取模型名称
     */
    String getModelName();

    /**
     * 评分文档
     */
    class ScoredDocument {
        private final String content;
        private final double score;
        private final int index;

        public ScoredDocument(String content, double score, int index) {
            this.content = content;
            this.score = score;
            this.index = index;
        }

        public String getContent() {
            return content;
        }

        public double getScore() {
            return score;
        }

        public int getIndex() {
            return index;
        }
    }
}