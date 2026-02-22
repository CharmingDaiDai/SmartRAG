package com.mtmn.smartdoc.rag.impl;

import com.mtmn.smartdoc.constants.AppConstants;
import com.mtmn.smartdoc.enums.IndexingStep;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.factory.DocumentSplitterFactory;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.dto.IndexUpdateItem;
import com.mtmn.smartdoc.model.dto.VectorItem;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.Chunk;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.rag.AbstractIndexStrategy;
import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;
import com.mtmn.smartdoc.rag.config.NaiveRagIndexConfig;
import com.mtmn.smartdoc.repository.ChunkRepository;
import com.mtmn.smartdoc.service.IndexingProgressCallback;
import com.mtmn.smartdoc.service.MilvusService;
import com.mtmn.smartdoc.service.MinioService;
import com.mtmn.smartdoc.utils.DocumentParseUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NaiveRAG 索引策略实现
 * 简单分块策略：按固定大小切分文档
 *
 * @author charmingdaidai
 * @version 3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaiveRAGIndexStrategy extends AbstractIndexStrategy {

    private final ChunkRepository chunkRepository;
    private final MinioService minioService;
    private final MilvusService milvusService;
    private final ModelFactory modelFactory;

    @Override
    public IndexStrategyType getType() {
        return IndexStrategyType.NAIVE_RAG;
    }

    @Override
    public void buildIndex(KnowledgeBase kb, DocumentPo documentPo, IndexStrategyConfig config) {
        buildIndex(kb, documentPo, config, IndexingProgressCallback.NOOP);
    }

    @Override
    public void buildIndex(KnowledgeBase kb, DocumentPo documentPo, IndexStrategyConfig config,
                           IndexingProgressCallback callback) {
        log.info("Building index for document: id={}, type={}", documentPo.getId(), getType());
        Long docId = documentPo.getId();
        String docName = documentPo.getFilename();

        try {
            // 1. 读取文档
            callback.onStepChanged(docId, docName, IndexingStep.READING);
            String content = readDocumentContent(documentPo);

            // 2. 切分
            callback.onStepChanged(docId, docName, IndexingStep.CHUNKING);
            List<TextSegment> segments = processContent(content, config);

            // 3. 保存 Chunks 到 MySQL
            callback.onStepChanged(docId, docName, IndexingStep.SAVING);
            chunkRepository.deleteByDocumentId(docId);

            Long kbId = kb.getId();
            List<Chunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                Chunk chunk = new Chunk();
                chunk.setKbId(kbId);
                chunk.setDocumentId(docId);
                chunk.setChunkIndex(i);
                chunk.setContent(segment.text());
                chunk.setIsModified(false);
                chunk.setStrategyType("NAIVE_RAG");
                chunk.setCreatedAt(LocalDateTime.now());
                chunk.setUpdatedAt(LocalDateTime.now());
                chunkEntities.add(chunk);
            }
            List<Chunk> savedChunks = chunkRepository.saveAll(chunkEntities);

            // 4. 向量化并存储到 Milvus
            callback.onStepChanged(docId, docName, IndexingStep.EMBEDDING);
            vectorizeAndStore(savedChunks, kb, docId);

            log.info("Index built successfully: documentId={}, segments={}", docId, segments.size());
        } catch (Exception e) {
            log.error("Failed to build index: documentId={}", docId, e);
            throw new RuntimeException("Index building failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected String readDocumentContent(DocumentPo document) {
        String filePath = document.getFilePath();

        // 从 MinIO 获取存储时确定的 Content-Type
        String contentType = minioService.getFileMetadata(filePath).contentType();
        log.debug("Content-Type from MinIO: {}", contentType);

        try (InputStream inputStream = minioService.getFileStream(filePath)) {
            // 将 contentType 传给工具类
            return DocumentParseUtils.parse(inputStream, document.getFilename(), contentType);
        } catch (Exception e) {
            log.error("Failed to read document content: documentId={}", document.getId(), e);
            throw new RuntimeException("Failed to read document content", e);
        }
    }

    @Override
    protected List<TextSegment> processContent(String content, IndexStrategyConfig config) {
        NaiveRagIndexConfig naiveConfig = (NaiveRagIndexConfig) config;

        // 使用工厂创建切分器
        DocumentSplitter splitter = DocumentSplitterFactory.createSplitter(naiveConfig);

        // 创建 LangChain4j Document
        Document document = Document.from(content);

        // 使用切分器切分文档
        List<TextSegment> segments = splitter.split(document);

        // 添加 chunk_index 元数据
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            segment.metadata().put("chunk_index", i);
        }

        log.info("Document split into {} segments using {}", segments.size(), naiveConfig.getSplitterType());

        return segments;
    }

    @Override
    protected void persist(KnowledgeBase kb, List<TextSegment> segments, Long documentId) {
        try {
            // 删除文档原有的 chunks
            chunkRepository.deleteByDocumentId(documentId);

            Long kbId = kb.getId();
            List<Chunk> chunkEntities = new ArrayList<>();

            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                Chunk chunk = new Chunk();
                chunk.setKbId(kbId);
                chunk.setDocumentId(documentId);
                chunk.setChunkIndex(i);
                chunk.setContent(segment.text());
                chunk.setIsModified(false);
                chunk.setStrategyType("NAIVE_RAG");
                chunk.setCreatedAt(LocalDateTime.now());
                chunk.setUpdatedAt(LocalDateTime.now());
                chunkEntities.add(chunk);
            }

            List<Chunk> savedChunks = chunkRepository.saveAll(chunkEntities);
            vectorizeAndStore(savedChunks, kb, documentId);

            log.info("Persisted {} chunks for document: {}", savedChunks.size(), documentId);

        } catch (Exception e) {
            log.error("Failed to persist chunks: documentId={}", documentId, e);
            throw new RuntimeException("Failed to persist chunks: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteIndex(KnowledgeBase kb, List<Long> documentIds) {
        Long kbId = kb.getId();
        log.info("Deleting index for documents: documentIds={}, kbId={}", documentIds, kbId);

        try {
            // 1. 先删除向量（需要查询 Chunks 获取 vectorId）
            deleteVectorsByDocumentIds(kbId, documentIds);

            // 2. 再删除 Chunks
            for (Long docId : documentIds) {
                chunkRepository.deleteByDocumentId(docId);
            }

            log.info("✅ Index deleted successfully for {} documents", documentIds.size());

        } catch (Exception e) {
            log.error("❌ Failed to delete index: documentIds={}, kbId={}", documentIds, kbId, e);
            throw new RuntimeException("Index deletion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteIndex(KnowledgeBase kb) {
        Long kbId = kb.getId();
        log.info("Deleting entire index for knowledge base: kbId={}", kbId);

        try {
            // 1. 删除向量库中的集合
            String collectionName = AppConstants.Milvus.CHUNKS_TEMPLATE.formatted(kbId);
            milvusService.dropCollection(collectionName);

            // 2. 删除数据库中的 Chunks
            chunkRepository.deleteByKbId(kbId);

            log.info("✅ Entire index deleted successfully for kbId={}", kbId);

        } catch (Exception e) {
            log.error("❌ Failed to delete entire index: kbId={}", kbId, e);
            throw new RuntimeException("Index deletion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void rebuildIndexFromChunks(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config) {
        rebuildIndexFromChunks(kb, document, config, IndexingProgressCallback.NOOP);
    }

    @Override
    public void rebuildIndexFromChunks(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config,
                                       IndexingProgressCallback callback) {
        log.info("Rebuilding index from chunks for document: id={}", document.getId());
        Long docId = document.getId();
        String docName = document.getFilename();

        try {
            // 1. 获取现有 Chunks
            List<Chunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(docId);
            if (chunks.isEmpty()) {
                log.warn("No chunks found for document: {}, falling back to full build.", docId);
                buildIndex(kb, document, config, callback);
                return;
            }

            // 2. 删除旧向量 + 重新向量化
            callback.onStepChanged(docId, docName, IndexingStep.EMBEDDING);
            deleteVectorsByDocumentIds(kb.getId(), List.of(docId));
            vectorizeAndStore(chunks, kb, docId);

            log.info("Index rebuilt from chunks successfully: documentId={}, chunks={}", docId, chunks.size());

        } catch (Exception e) {
            log.error("Failed to rebuild index from chunks: documentId={}", docId, e);
            throw new RuntimeException("Index rebuild failed: " + e.getMessage(), e);
        }
    }

    /**
     * 向量化并存储到 Milvus
     */
    private void vectorizeAndStore(List<Chunk> chunks, KnowledgeBase kb, Long documentId) {
        log.info("Vectorizing {} chunks for document: {}", chunks.size(), documentId);

        Long kbId = kb.getId();

        try {
            String embeddingModelId = kb.getEmbeddingModelId();
            EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(embeddingModelId);

            List<String> contents = chunks.stream()
                    .map(Chunk::getContent)
                    .collect(Collectors.toList());
            List<Embedding> embeddings = embeddingClient.embedBatch(contents);

            List<VectorItem> vectorItems = new ArrayList<>(chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                Chunk chunk = chunks.get(i);
                Embedding embedding = embeddings.get(i);

                VectorItem item = new VectorItem();
                item.setId(chunk.getId().toString());
                item.setEmbedding(embedding);
                item.setContent(chunk.getContent());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunk_id", chunk.getId());
                metadata.put("document_id", documentId);
                metadata.put("kb_id", kbId);
                metadata.put("chunk_index", chunk.getChunkIndex());
                item.setMetadata(metadata);

                vectorItems.add(item);
            }

            milvusService.store(kbId, vectorItems);

        } catch (Exception e) {
            log.error("Failed to vectorize chunks: documentId={}", documentId, e);
            throw new RuntimeException("Vectorization failed: " + e.getMessage(), e);
        }
    }

    /**
     * 删除向量数据
     */
    private void deleteVectorsByDocumentIds(Long kbId, List<Long> documentIds) {
        try {
            log.debug("Deleting vectors from collection: {}, documentIds: {}", kbId, documentIds);

            // 1. 查询关联的 Chunks
            List<Chunk> chunks = chunkRepository.findByDocumentIdIn(documentIds);
            if (chunks.isEmpty()) {
                return;
            }

            // 2. 构建删除请求
            List<IndexUpdateItem> items = chunks.stream()
                    .map(chunk -> {
                        IndexUpdateItem item = new IndexUpdateItem();
                        // 使用 chunk ID 作为向量 ID
                        item.setId(chunk.getId().toString());
                        item.setUpdateType(IndexUpdateItem.UpdateType.DELETE);
                        return item;
                    })
                    .collect(Collectors.toList());

            // 3. 调用 MilvusService 删除向量
            milvusService.update(kbId, items);

        } catch (Exception e) {
            log.error("Failed to delete vectors: collection={}, documentIds={}", kbId, documentIds, e);
            throw new RuntimeException("Failed to delete vectors: " + e.getMessage(), e);
        }
    }
}