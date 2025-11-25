package com.mtmn.smartdoc.rag;

import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;

import java.util.List;

/**
 * 索引构建策略接口
 * <p>
 * 负责将文档内容解析、切分、向量化并存储到向量数据库
 * 不同的 RAG 方法（Naive、HiSem）有不同的索引构建方式
 *
 * @author charmingdaidai
 * @version 3.0
 * @date 2025-11-24
 */
public interface IndexStrategy {

    /**
     * 获取策略类型
     */
    IndexStrategyType getType();

    /**
     * 构建索引
     * <p>
     * 完整流程：读取文档 -> 切分/解析 -> 向量化 -> 持久化
     *
     * @param document 文档对象
     * @param config   索引策略配置（来自数据库的 JSON，已反序列化为具体的配置类）
     */
    void buildIndex(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config);

    /**
     * 删除索引
     *
     * @param documentIds 文档 ID 列表
     * @param kbId        知识库 ID
     */
    void deleteIndex(KnowledgeBase kb, List<Long> documentIds);

    /**
     * 重建索引
     *
     * @param document 文档对象
     * @param config   索引策略配置
     */
    void rebuildIndex(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config);
}