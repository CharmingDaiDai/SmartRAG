package com.mtmn.smartdoc.service.impl;

import com.mtmn.smartdoc.enums.DocumentIndexStatus;
import com.mtmn.smartdoc.enums.KnowledgeBaseStatus;
import com.mtmn.smartdoc.exception.ResourceNotFoundException;
import com.mtmn.smartdoc.po.Document;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.repository.KnowledgeBaseRepository;
import com.mtmn.smartdoc.service.IndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 索引构建服务实现
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
// TODO 待完善
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    // TODO: 注入 IndexStrategyFactory

    @Override
    public void submitIndexingTask(Long documentId, Long kbId) {
        log.info("Submitting indexing task: documentId={}, kbId={}", documentId, kbId);

        // 异步执行索引
        executeIndexing(documentId, kbId);
    }

    @Override
    public void submitBatchIndexingTask(Long kbId, List<Long> documentIds) {
        log.info("Submitting batch indexing task: kbId={}, count={}", kbId, documentIds.size());

        // 更新知识库状态
        updateKnowledgeBaseStatus(kbId, KnowledgeBaseStatus.INDEXING);

        // 异步执行批量索引
        for (Long documentId : documentIds) {
            executeIndexing(documentId, kbId);
        }
    }

    @Override
    @Async
    @Transactional
    public void executeIndexing(Long documentId, Long kbId) {
        log.info("Executing indexing: documentId={}, kbId={}", documentId, kbId);

        try {
            // 验证文档和知识库存在
            documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));
            knowledgeBaseRepository.findById(kbId)
                    .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", kbId));

            // 更新文档状态为 CHUNKING
            updateDocumentStatus(documentId, DocumentIndexStatus.CHUNKING);

            // TODO: 调用 IndexStrategy 进行文档解析和分块
            // IndexStrategy strategy = indexStrategyFactory.getStrategy(kb.getIndexStrategyType());
            // strategy.parseDocument(document, kb);

            // 更新状态为 CHUNKED
            updateDocumentStatus(documentId, DocumentIndexStatus.CHUNKED);

            // TODO: 构建向量索引
            // updateDocumentStatus(documentId, DocumentIndexStatus.INDEXING);
            // strategy.buildIndex(document, kb);

            // 更新状态为 INDEXED
            updateDocumentStatus(documentId, DocumentIndexStatus.INDEXED);

            log.info("Indexing completed successfully: documentId={}", documentId);

            // 检查知识库所有文档是否都已索引
            checkAndUpdateKnowledgeBaseStatus(kbId);

        } catch (Exception e) {
            log.error("Indexing failed: documentId={}", documentId, e);
            updateDocumentStatus(documentId, DocumentIndexStatus.ERROR);
        }
    }

    @Override
    @Transactional
    public void rebuildIndex(Long kbId) {
        log.info("Rebuilding index for KB: {}", kbId);

        // 验证知识库存在
        knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", kbId));

        // 更新知识库状态
        updateKnowledgeBaseStatus(kbId, KnowledgeBaseStatus.INDEXING);

        // TODO: 删除现有索引
        // TODO: 重新索引所有文档

        // 获取所有文档
        List<Document> documents = documentRepository.findByKbId(kbId);

        // 重置文档状态并重新索引
        for (Document document : documents) {
            updateDocumentStatus(document.getId(), DocumentIndexStatus.UPLOADED);
            executeIndexing(document.getId(), kbId);
        }
    }

    @Override
    public void cancelIndexing(Long documentId) {
        log.info("Canceling indexing: documentId={}", documentId);

        // TODO: 实现取消逻辑
        // 1. 标记任务为取消状态
        // 2. 中断正在执行的任务
        // 3. 清理部分生成的数据

        updateDocumentStatus(documentId, DocumentIndexStatus.UPLOADED);
    }

    /**
     * 更新文档索引状态
     */
    @Transactional
    protected void updateDocumentStatus(Long documentId, DocumentIndexStatus status) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setIndexStatus(status);
            documentRepository.save(document);
            log.debug("Document status updated: id={}, status={}", documentId, status);
        });
    }

    /**
     * 更新知识库状态
     */
    @Transactional
    protected void updateKnowledgeBaseStatus(Long kbId, KnowledgeBaseStatus status) {
        knowledgeBaseRepository.findById(kbId).ifPresent(kb -> {
            kb.setStatus(status);
            knowledgeBaseRepository.save(kb);
            log.debug("KnowledgeBase status updated: id={}, status={}", kbId, status);
        });
    }

    /**
     * 检查并更新知识库状态
     * 如果所有文档都已索引,则更新知识库状态为 INDEXED
     */
    @Transactional
    protected void checkAndUpdateKnowledgeBaseStatus(Long kbId) {
        long totalDocs = documentRepository.countByKbId(kbId);
        long indexedDocs = documentRepository.countByKbIdAndIndexStatus(kbId, DocumentIndexStatus.INDEXED);

        if (totalDocs > 0 && totalDocs == indexedDocs) {
            updateKnowledgeBaseStatus(kbId, KnowledgeBaseStatus.INDEXED);
            log.info("All documents indexed for KB: {}", kbId);
        }
    }
}