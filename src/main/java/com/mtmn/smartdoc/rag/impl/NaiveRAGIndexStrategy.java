package com.mtmn.smartdoc.rag.impl;

import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;
import com.mtmn.smartdoc.rag.config.NaiveRAGConfig;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.Chunk;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.repository.ChunkRepository;
import com.mtmn.smartdoc.service.MinioService;
import com.mtmn.smartdoc.rag.AbstractIndexStrategy;
import com.mtmn.smartdoc.rag.StorageStrategy;
import com.mtmn.smartdoc.utils.DocumentParseUtils;
import com.mtmn.smartdoc.factory.DocumentSplitterFactory;
import dev.langchain4j.data.document.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
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
    private final StorageStrategy storageStrategy;
    private final ModelFactory modelFactory;

    @Override
    public IndexStrategyType getType() {
        return IndexStrategyType.NAIVE_RAG;
    }

    @Override
    public void buildIndex(KnowledgeBase kb, DocumentPo documentPo, IndexStrategyConfig config) {
        log.info("Building index for document: id={}, type={}", documentPo.getId(), getType());

        try {
            String content = readDocumentContent(documentPo);
            List<TextSegment> segments = processContent(content, config);
            persist(kb, segments, documentPo.getId());

            log.info("✅ Index built successfully: documentId={}, segments={}", documentPo.getId(), segments.size());

        } catch (Exception e) {
            log.error("❌ Failed to build index: documentId={}", documentPo.getId(), e);
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
        NaiveRAGConfig naiveConfig = (NaiveRAGConfig) config;
        
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
            // TODO 优化 chunks 存储位置（存入 Mysql 压力太大）
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
            for (Long docId : documentIds) {
                chunkRepository.deleteByDocumentId(docId);
            }

            String collectionName = "kb_" + kbId + "_chunks";
            deleteVectorsByDocumentIds(collectionName, documentIds);

            log.info("✅ Index deleted successfully for {} documents", documentIds.size());

        } catch (Exception e) {
            log.error("❌ Failed to delete index: documentIds={}, kbId={}", documentIds, kbId, e);
            throw new RuntimeException("Index deletion failed: " + e.getMessage(), e);
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

            List<StorageStrategy.VectorItem> vectorItems = new ArrayList<>(chunks.size());

            for (int i = 0; i < chunks.size(); i++) {
                Chunk chunk = chunks.get(i);
                Embedding embedding = embeddings.get(i);
                String vectorId = UUID.randomUUID().toString();

                StorageStrategy.VectorItem item = new StorageStrategy.VectorItem();
                item.setId(vectorId);
                item.setEmbedding(embedding);
                item.setContent(chunk.getContent());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunk_id", chunk.getId());
                metadata.put("document_id", documentId);
                metadata.put("kb_id", kbId);
                metadata.put("chunk_index", chunk.getChunkIndex());
                item.setMetadata(metadata);

                vectorItems.add(item);
                chunk.setVectorId(vectorId);
            }

            String collectionName = "kb_" + kbId + "_chunks";
            StorageStrategy.IndexData indexData = new StorageStrategy.IndexData();
            indexData.setVectors(vectorItems);

            Map<String, Object> globalMetadata = new HashMap<>();
            globalMetadata.put("kb_id", kbId);
            globalMetadata.put("document_id", documentId);
            indexData.setMetadata(globalMetadata);

            storageStrategy.storeIndex(collectionName, indexData);

        } catch (Exception e) {
            log.error("Failed to vectorize chunks: documentId={}", documentId, e);
            throw new RuntimeException("Vectorization failed: " + e.getMessage(), e);
        }
    }

    /**
     * 删除向量数据
     */
    private void deleteVectorsByDocumentIds(String collectionName, List<Long> documentIds) {
        try {
            log.debug("Deleting vectors from collection: {}, documentIds: {}", collectionName, documentIds);
        } catch (Exception e) {
            log.error("Failed to delete vectors: collection={}, documentIds={}", collectionName, documentIds, e);
            throw new RuntimeException("Failed to delete vectors: " + e.getMessage(), e);
        }
    }
}