package com.mtmn.smartdoc.rag;

import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.rag.config.RetrievalStrategyConfig;
import com.mtmn.smartdoc.vo.RetrievalResult;

import java.util.List;

/**
 * 检索策略接口
 * <p>
 * 负责根据查询从向量数据库检索相关内容
 * 不同的 RAG 方法（Naive、HiSem）有不同的检索方式
 *
 * @author charmingdaidai
 * @version 3.0
 * @date 2025-11-24
 */
public interface RetrievalStrategy {

    /**
     * 获取策略类型
     */
    IndexStrategyType getType();

    /**
     * 检索相关内容
     * <p>
     * 核心流程：查询向量化 -> 向量检索 -> 结果排序
     *
     * @param queries 查询列表（支持查询重写后的多个查询）
     * @param kbId    知识库 ID
     * @param config  检索配置（来自前端参数，包含 topK、similarityThreshold 等）
     * @return 检索结果列表
     */
    List<RetrievalResult> search(List<String> queries, Long kbId, RetrievalStrategyConfig config);
}