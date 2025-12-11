package com.mtmn.smartdoc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;
import com.mtmn.smartdoc.enums.DocumentIndexStatus;
import com.mtmn.smartdoc.exception.ResourceNotFoundException;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.repository.KnowledgeBaseRepository;
import com.mtmn.smartdoc.service.IndexingService;
import com.mtmn.smartdoc.rag.IndexStrategy;
import com.mtmn.smartdoc.rag.factory.RAGStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 索引构建服务实现
 *
 * @author charmingdaidai
 * @version 3.0
 * @date 2025-11-24
 */
@Slf4j
@Service
public class IndexingServiceImpl implements IndexingService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RAGStrategyFactory ragStrategyFactory;
    private final ObjectMapper objectMapper;
    
    // 自注入：获取代理对象，用于类内调用事务方法
    @Resource
    @Lazy
    private IndexingService self;

    public IndexingServiceImpl(DocumentRepository documentRepository,
                               KnowledgeBaseRepository knowledgeBaseRepository,
                               RAGStrategyFactory ragStrategyFactory,
                               ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.ragStrategyFactory = ragStrategyFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void submitIndexingTask(Long documentId, Long kbId) {
        log.info("Submitting indexing task: documentId={}, kbId={}", documentId, kbId);

        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", kbId));
        IndexStrategyConfig indexConfig = parseIndexStrategyConfig(knowledgeBase);

        // 使用 self 代理调用，确保事务生效
        self.executeIndexing(documentId, knowledgeBase, indexConfig);
    }

    @Override
    public void submitBatchIndexingTask(Long kbId, List<Long> documentIds) {
        log.info("Submitting batch indexing task: kbId={}, count={}", kbId, documentIds.size());

        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", kbId));
        IndexStrategyConfig indexConfig = parseIndexStrategyConfig(knowledgeBase);

        // 使用 self 代理调用，确保每个任务都有独立事务
        for (Long documentId : documentIds) {
            self.executeIndexing(documentId, knowledgeBase, indexConfig);
        }
    }

    /**
     * 执行索引构建（带事务）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeIndexing(Long documentId, KnowledgeBase kb, IndexStrategyConfig indexConfig) {
        log.info("Executing indexing with transaction: documentId={}, kbId={}", documentId, kb.getId());

        try {
            // 加载文档
            DocumentPo documentPo = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

            // TODO setIndexStatus 事务没有提交，状态不会改变，交给异步任务/其他方式去执行
            documentPo.setIndexStatus(DocumentIndexStatus.CHUNKING);
            documentRepository.save(documentPo);

            // 使用工厂获取对应的索引策略
            IndexStrategy strategy = ragStrategyFactory.getIndexStrategy(indexConfig.getStrategyType());

            // 构建索引（策略内部完成：读取、处理、向量化、存储）
            strategy.buildIndex(kb, documentPo, indexConfig);

            // TODO setIndexStatus 事务没有提交，状态不会改变，交给异步任务/其他方式去执行
            documentPo.setIndexStatus(DocumentIndexStatus.INDEXED);
            documentRepository.save(documentPo);

            log.info("✅ Indexing completed successfully: documentId={}", documentId);

        } catch (Exception e) {
            log.error("❌ Indexing failed: documentId={}", documentId, e);
            // 直接调用（已在同一事务中）
            DocumentPo doc = documentRepository.findById(documentId).orElse(null);
            if (doc != null) {
                doc.setIndexStatus(DocumentIndexStatus.ERROR);
                documentRepository.save(doc);
            }
            throw new RuntimeException("Indexing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void rebuildDocumentIndexFromChunks(Long documentId, Long kbId) {
        log.info("Submitting rebuild index from chunks task: documentId={}, kbId={}", documentId, kbId);
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", kbId));
        IndexStrategyConfig indexConfig = parseIndexStrategyConfig(knowledgeBase);
        self.executeRebuildIndexFromChunks(documentId, knowledgeBase, indexConfig);
    }

    @Override
    public void batchRebuildDocumentIndexFromChunks(Long kbId, List<Long> documentIds) {
        log.info("Submitting batch rebuild index from chunks task: kbId={}, count={}", kbId, documentIds.size());
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", kbId));
        IndexStrategyConfig indexConfig = parseIndexStrategyConfig(knowledgeBase);
        for (Long documentId : documentIds) {
            try {
                self.executeRebuildIndexFromChunks(documentId, knowledgeBase, indexConfig);
            } catch (Exception e) {
                log.error("Failed to rebuild index for document: {}", documentId, e);
                // Continue with other documents
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeRebuildIndexFromChunks(Long documentId, KnowledgeBase kb, IndexStrategyConfig indexConfig) {
        log.info("Executing rebuild index from chunks with transaction: documentId={}, kbId={}", documentId, kb.getId());
        try {
            DocumentPo documentPo = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

            documentPo.setIndexStatus(DocumentIndexStatus.CHUNKING);
            documentRepository.save(documentPo);

            IndexStrategy strategy = ragStrategyFactory.getIndexStrategy(indexConfig.getStrategyType());
            strategy.rebuildIndexFromChunks(kb, documentPo, indexConfig);

            documentPo.setIndexStatus(DocumentIndexStatus.INDEXED);
            documentRepository.save(documentPo);

            log.info("✅ Rebuild index from chunks completed successfully: documentId={}", documentId);
        } catch (Exception e) {
            log.error("❌ Rebuild index from chunks failed: documentId={}", documentId, e);
            DocumentPo doc = documentRepository.findById(documentId).orElse(null);
            if (doc != null) {
                doc.setIndexStatus(DocumentIndexStatus.ERROR);
                documentRepository.save(doc);
            }
            throw new RuntimeException("Rebuild index failed: " + e.getMessage(), e);
        }
    }

    /**
     * 解析知识库的索引策略配置（JSON 反序列化）
     */
    private IndexStrategyConfig parseIndexStrategyConfig(KnowledgeBase kb) {
        try {
            String configJson = kb.getIndexStrategyConfig();
            if (configJson == null || configJson.trim().isEmpty()) {
                throw new IllegalStateException("Knowledge base has no index strategy configured");
            }

            // 使用 Jackson 自动多态反序列化（基于 @JsonTypeInfo）
            return objectMapper.readValue(configJson, IndexStrategyConfig.class);

        } catch (Exception e) {
            log.error("Failed to parse index strategy config for kbId={}", kb.getId(), e);
            throw new RuntimeException("Failed to parse index strategy config: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildIndex(Long kbId) {
        log.info("Rebuilding index for KB: {}", kbId);

        try {
            // 验证知识库存在
            KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                    .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", kbId));

            // 获取所有文档
            List<DocumentPo> documents = documentRepository.findByKbId(kbId);

            if (documents.isEmpty()) {
                log.info("No documents to rebuild for kbId={}", kbId);
                return;
            }

            // 解析索引策略配置
            IndexStrategyConfig strategyConfig = parseIndexStrategyConfig(kb);
            IndexStrategy strategy = ragStrategyFactory.getIndexStrategy(strategyConfig.getStrategyType());

            // 删除现有索引
            List<Long> documentIds = documents.stream().map(DocumentPo::getId).toList();
            strategy.deleteIndex(kb, documentIds);

            IndexStrategyConfig indexConfig = parseIndexStrategyConfig(kb);

            // 重置文档状态并重新索引
            for (DocumentPo document : documents) {
                self.updateDocumentStatus(document.getId(), DocumentIndexStatus.UPLOADED);
                self.executeIndexing(document.getId(), kb, indexConfig);
            }

            log.info("Index rebuild initiated for {} documents", documents.size());

        } catch (Exception e) {
            log.error("Failed to rebuild index for kbId={}", kbId, e);
            throw new RuntimeException("Failed to rebuild index: " + e.getMessage(), e);
        }
    }

    @Override
    public void cancelIndexing(Long documentId) {
        log.info("Canceling indexing: documentId={}", documentId);

        // TODO: 实现取消逻辑
        // 1. 标记任务为取消状态
        // 2. 中断正在执行的任务
        // 3. 清理部分生成的数据

        self.updateDocumentStatus(documentId, DocumentIndexStatus.UPLOADED);
    }

    /**
     * 更新文档索引状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocumentStatus(Long documentId, DocumentIndexStatus status) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setIndexStatus(status);
            documentRepository.save(document);
            log.debug("Document status updated: id={}, status={}", documentId, status);
        });
    }
}