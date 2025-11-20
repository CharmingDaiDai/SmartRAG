package com.mtmn.smartdoc.strategy;

import com.mtmn.smartdoc.config.IndexStrategyConfig;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.po.Document;
import com.mtmn.smartdoc.po.KnowledgeBase;

import java.util.List;

/**
 * 索引构建策略接口
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public interface IndexStrategy {

    /**
     * 获取策略类型
     */
    IndexStrategyType getStrategyType();

    /**
     * 解析文档并构建索引结构（不包含向量化）
     * 返回可预览的结构化数据
     *
     * @param document 文档
     * @param config   索引策略配置
     * @return 索引结构数据
     */
    IndexStructure parseDocument(Document document, IndexStrategyConfig config);

    /**
     * 完整构建索引（包含向量化和存储）
     *
     * @param kb        知识库
     * @param document  文档
     * @param structure 已解析的索引结构
     */
    void buildIndex(KnowledgeBase kb, Document document, IndexStructure structure);

    /**
     * 部分重构索引（针对修改的 chunks/nodes）
     *
     * @param kb          知识库
     * @param modifiedIds 修改的 chunk 或 node IDs
     */
    void rebuildPartialIndex(KnowledgeBase kb, List<String> modifiedIds);

    /**
     * 完全重构索引
     *
     * @param kb 知识库
     */
    void rebuildFullIndex(KnowledgeBase kb);

    /**
     * 验证配置是否有效
     *
     * @param config 索引策略配置
     */
    void validateConfig(IndexStrategyConfig config);

    /**
     * 索引结构数据接口（标记接口）
     */
    interface IndexStructure {
        // 标记接口，具体实现由各策略定义
    }
}