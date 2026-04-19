package com.mtmn.smartrag.enums;

/**
 * 检索增强算法类型
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public enum EnhancementType {
    /**
     * 问题重写 - 使用 LLM 重写用户问题以提高检索准确性
     */
    QUERY_REWRITE,

    /**
     * 问题分解 - 将复杂问题分解为多个子问题
     */
    QUERY_DECOMPOSE,

    /**
     * HyDE - 假设文档嵌入，生成假设答案进行检索
     */
    HYDE,

    /**
     * 混合检索 - 结合向量检索和关键词检索
     */
    HYBRID_RETRIEVAL
}