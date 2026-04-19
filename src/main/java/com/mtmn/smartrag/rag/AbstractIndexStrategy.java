package com.mtmn.smartrag.rag;

import com.mtmn.smartrag.po.DocumentPo;
import com.mtmn.smartrag.po.KnowledgeBase;
import com.mtmn.smartrag.rag.config.IndexStrategyConfig;
import com.mtmn.smartrag.service.IndexingProgressCallback;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 索引策略抽象基类
 * <p>
 * 使用模板方法模式，定义索引构建的标准流程：
 * 1. 读取文档内容
 * 2. 处理内容（切分/解析）- 抽象方法，由子类实现
 * 3. 持久化（存储到向量数据库和关系数据库）- 抽象方法，由子类实现
 *
 * @author charmingdaidai
 * @version 3.0
 * @date 2025-11-24
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractIndexStrategy implements IndexStrategy {

    @Override
    public void buildIndex(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config) {
        log.info("Building index for document: id={}, type={}", document.getId(), getType());

        try {
            // 1. 通用步骤：读取文档内容
            String content = readDocumentContent(document);

            // 2. 差异化步骤：处理内容（由子类实现）
            // Naive: 简单切分
            // HiSem: 语义分层，构建树结构
            List<TextSegment> segments = processContent(content, config);

            // 3. 差异化步骤：持久化（由子类实现）
            // Naive: 只存 Milvus
            // HiSem: 存 Milvus + MySQL（树结构）
            persist(kb, segments, document.getId());

            log.info("Index built successfully: documentId={}, segments={}", document.getId(), segments.size());

        } catch (Exception e) {
            log.error("Failed to build index: documentId={}", document.getId(), e);
            throw new RuntimeException("Index building failed", e);
        }
    }

    @Override
    public void rebuildIndex(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config) {
        log.info("Rebuilding index for document: id={}", document.getId());

        // 1. 删除旧索引
        deleteIndex(kb, List.of(document.getId()));

        // 2. 重新构建
        buildIndex(kb, document, config);
    }

    @Override
    public void rebuildIndex(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config,
                             IndexingProgressCallback callback) {
        log.info("Rebuilding index for document: id={}", document.getId());
        deleteIndex(kb, List.of(document.getId()));
        buildIndex(kb, document, config, callback);
    }

    @Override
    public void rebuildIndexFromChunks(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config) {
        throw new UnsupportedOperationException("This strategy does not support rebuilding index from chunks.");
    }

    /**
     * 读取文档内容（通用步骤）
     */
    protected abstract String readDocumentContent(DocumentPo document);

    /**
     * 处理内容（差异化步骤，由子类实现）
     *
     * @param content 文档内容
     * @param config  索引策略配置
     * @return 处理后的片段列表
     */
    protected abstract List<TextSegment> processContent(String content, IndexStrategyConfig config);

    /**
     * 持久化（差异化步骤，由子类实现）
     *
     * @param segments   片段列表
     * @param kbId       知识库 ID
     * @param documentId 文档 ID
     */
    protected abstract void persist(KnowledgeBase kb, List<TextSegment> segments, Long documentId);
}