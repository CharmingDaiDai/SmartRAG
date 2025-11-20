package com.mtmn.smartdoc.enums;

/**
 * 索引构建策略类型
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public enum IndexStrategyType {
    /**
     * 朴素 RAG - 简单的文档切分和向量检索
     */
    NAIVE_RAG,

    /**
     * 层次语义 RAG - 基于文档层次结构的语义检索
     */
    HISEM_RAG
}