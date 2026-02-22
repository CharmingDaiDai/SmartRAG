package com.mtmn.smartdoc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;
import com.mtmn.smartdoc.enums.DocumentIndexStatus;
import com.mtmn.smartdoc.enums.IndexingStep;
import com.mtmn.smartdoc.exception.ResourceNotFoundException;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.repository.KnowledgeBaseRepository;
import com.mtmn.smartdoc.service.IndexingService;
import com.mtmn.smartdoc.service.IndexingProgressCallback;
import com.mtmn.smartdoc.rag.IndexStrategy;
import com.mtmn.smartdoc.rag.factory.RAGStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
     * 执行索引构建
     */
    @Override
    public void executeIndexing(Long documentId, KnowledgeBase kb, IndexStrategyConfig indexConfig) {
        executeIndexing(documentId, kb, indexConfig, IndexingProgressCallback.NOOP);
    }

    /**
     * 执行索引构建（带进度回调）
     *
     * 注意：此方法故意不加 @Transactional。
     * 文档状态通过 self.updateDocumentStatus()（独立事务）更新，每步立即提交、对其他连接可见。
     * strategy.buildIndex() 是 IO 密集型操作（MinIO/LLM/Milvus），不应持有数据库连接。
     * 细粒度步骤由各策略内部通过 callback.onStepChanged() 上报，同时同步更新文档状态。
     */
    @Override
    public void executeIndexing(Long documentId, KnowledgeBase kb, IndexStrategyConfig indexConfig, IndexingProgressCallback callback) {
        log.info("Executing indexing: documentId={}, kbId={}", documentId, kb.getId());

        DocumentPo documentPo = null;
        try {
            documentPo = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

            // 包装回调：每次步骤变更同步更新文档状态
            IndexingProgressCallback wrappedCallback = wrapCallbackWithDocStatus(callback);

            // 委托给策略 — 策略内部上报细粒度步骤
            IndexStrategy strategy = ragStrategyFactory.getIndexStrategy(indexConfig.getStrategyType());
            strategy.buildIndex(kb, documentPo, indexConfig, wrappedCallback);

            self.updateDocumentStatus(documentId, DocumentIndexStatus.INDEXED);
            callback.onDocumentCompleted(documentId, documentPo.getFilename());

            log.info("Indexing completed: documentId={}", documentId);

        } catch (Exception e) {
            log.error("Indexing failed: documentId={}", documentId, e);
            self.updateDocumentStatus(documentId, DocumentIndexStatus.ERROR);
            String docName = documentPo != null ? documentPo.getFilename() : "unknown";
            callback.onDocumentFailed(documentId, docName, e.getMessage());
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
    public void executeRebuildIndexFromChunks(Long documentId, KnowledgeBase kb, IndexStrategyConfig indexConfig) {
        executeRebuildIndexFromChunks(documentId, kb, indexConfig, IndexingProgressCallback.NOOP);
    }

    /**
     * 执行从 Chunk 重建索引（带进度回调）
     *
     * 注意：同 executeIndexing，故意不加 @Transactional，状态更新各自独立事务立即提交。
     * 细粒度步骤由各策略内部通过 callback.onStepChanged() 上报，同时同步更新文档状态。
     */
    @Override
    public void executeRebuildIndexFromChunks(Long documentId, KnowledgeBase kb, IndexStrategyConfig indexConfig, IndexingProgressCallback callback) {
        log.info("Executing rebuild index from chunks: documentId={}, kbId={}", documentId, kb.getId());

        DocumentPo documentPo = null;
        try {
            documentPo = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

            // 包装回调：每次步骤变更同步更新文档状态
            IndexingProgressCallback wrappedCallback = wrapCallbackWithDocStatus(callback);

            // 委托给策略 — 策略内部上报细粒度步骤
            IndexStrategy strategy = ragStrategyFactory.getIndexStrategy(indexConfig.getStrategyType());
            strategy.rebuildIndexFromChunks(kb, documentPo, indexConfig, wrappedCallback);

            self.updateDocumentStatus(documentId, DocumentIndexStatus.INDEXED);
            callback.onDocumentCompleted(documentId, documentPo.getFilename());

            log.info("Rebuild index from chunks completed: documentId={}", documentId);

        } catch (Exception e) {
            log.error("Rebuild index from chunks failed: documentId={}", documentId, e);
            self.updateDocumentStatus(documentId, DocumentIndexStatus.ERROR);
            String docName = documentPo != null ? documentPo.getFilename() : "unknown";
            callback.onDocumentFailed(documentId, docName, e.getMessage());
            throw new RuntimeException("Rebuild index failed: " + e.getMessage(), e);
        }
    }

    /**
     * 包装回调：在原回调基础上，每次步骤变更同步更新文档的 indexStatus
     */
    private IndexingProgressCallback wrapCallbackWithDocStatus(IndexingProgressCallback delegate) {
        return new IndexingProgressCallback() {
            @Override
            public void onStepChanged(Long documentId, String documentName, IndexingStep step) {
                // 同步更新文档状态
                self.updateDocumentStatus(documentId, DocumentIndexStatus.fromIndexingStep(step));
                // 转发给原回调（更新 indexing_tasks 表）
                delegate.onStepChanged(documentId, documentName, step);
            }

            @Override
            public void onDocumentCompleted(Long documentId, String documentName) {
                delegate.onDocumentCompleted(documentId, documentName);
            }

            @Override
            public void onDocumentFailed(Long documentId, String documentName, String error) {
                delegate.onDocumentFailed(documentId, documentName, error);
            }
        };
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