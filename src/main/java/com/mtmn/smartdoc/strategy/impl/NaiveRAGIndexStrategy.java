package com.mtmn.smartdoc.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.config.IndexStrategyConfig;
import com.mtmn.smartdoc.config.NaiveRAGConfig;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.exception.ConfigValidationException;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.Chunk;
import com.mtmn.smartdoc.po.Document;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.repository.ChunkRepository;
import com.mtmn.smartdoc.service.MinioService;
import com.mtmn.smartdoc.strategy.IndexStrategy;
import com.mtmn.smartdoc.strategy.StorageStrategy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * NaiveRAG 索引策略实现
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaiveRAGIndexStrategy implements IndexStrategy {

    private final ChunkRepository chunkRepository;
    private final MinioService minioService;
    private final StorageStrategy storageStrategy;
    private final ModelFactory modelFactory;
    private final ObjectMapper objectMapper;

    @Override
    public IndexStrategyType getStrategyType() {
        return IndexStrategyType.NAIVE_RAG;
    }

    @Override
    @Transactional
    public IndexStructure parseDocument(Document document, IndexStrategyConfig config) {
        log.info("Parsing document with NaiveRAG: documentId={}", document.getId());

        try {
            // 1. 验证配置
            validateConfig(config);
            NaiveRAGConfig naiveConfig = objectMapper.convertValue(config, NaiveRAGConfig.class);

            // 2. 从 MinIO 读取文档内容
            String content;
            try (InputStream inputStream = minioService.getFileStream(document.getFilePath())) {
                content = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }

            // 3. 分块
            List<ChunkData> chunks = splitIntoChunks(content, naiveConfig);

            // 4. 创建并保存 Chunk 实体
            chunkRepository.deleteByDocumentId(document.getId());

            List<Chunk> chunkEntities = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                ChunkData chunkData = chunks.get(i);
                Chunk chunk = new Chunk();
                chunk.setKbId(document.getKbId());
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(chunkData.getContent());
                chunk.setIsModified(false);
                chunk.setCreatedAt(LocalDateTime.now());
                chunk.setUpdatedAt(LocalDateTime.now());

                chunkEntities.add(chunk);
            }

            chunkRepository.saveAll(chunkEntities);

            log.info("Document parsed successfully: documentId={}, chunks={}",
                    document.getId(), chunks.size());

            // 5. 返回索引结构（用于预览）
            NaiveRAGStructure structure = new NaiveRAGStructure();
            structure.setChunks(chunks);
            structure.setTotalChunks(chunks.size());

            return structure;

        } catch (Exception e) {
            log.error("Failed to parse document: documentId={}", document.getId(), e);
            throw new RuntimeException("Document parsing failed", e);
        }
    }

    @Override
    @Transactional
    public void buildIndex(KnowledgeBase kb, Document document, IndexStructure structure) {
        log.info("Building index for document: documentId={}", document.getId());

        try {
            // 1. 获取文档的所有 chunks
            List<Chunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(document.getId());

            // 2. 准备向量数据
            List<StorageStrategy.VectorItem> vectorItems = new ArrayList<>();
            EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(kb.getEmbeddingModelId());

            for (Chunk chunk : chunks) {
                // 生成向量
                List<Float> embedding = embeddingClient.embed(chunk.getContent());

                // 生成向量 ID
                String vectorId = UUID.randomUUID().toString();

                // 创建向量项
                StorageStrategy.VectorItem item = new StorageStrategy.VectorItem();
                item.setId(vectorId);
                item.setVector(embedding);
                item.setContent(chunk.getContent());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunk_id", chunk.getId());
                metadata.put("document_id", document.getId());
                metadata.put("kb_id", kb.getId());
                metadata.put("chunk_index", chunk.getChunkIndex());
                item.setMetadata(metadata);

                vectorItems.add(item);

                // 更新 chunk 的 vectorId
                chunk.setVectorId(vectorId);
            }

            // 3. 批量存储向量
            StorageStrategy.IndexData indexData = new StorageStrategy.IndexData();
            indexData.setVectors(vectorItems);

            Map<String, Object> globalMetadata = new HashMap<>();
            globalMetadata.put("kb_id", kb.getId());
            globalMetadata.put("document_id", document.getId());
            indexData.setMetadata(globalMetadata);

            storageStrategy.storeIndex(String.valueOf(kb.getId()), indexData);

            // 4. 保存更新后的 chunks
            chunkRepository.saveAll(chunks);

            log.info("Index built successfully: documentId={}, chunks={}",
                    document.getId(), chunks.size());

        } catch (Exception e) {
            log.error("Failed to build index: documentId={}", document.getId(), e);
            throw new RuntimeException("Index building failed", e);
        }
    }

    @Override
    @Transactional
    public void rebuildPartialIndex(KnowledgeBase kb, List<String> modifiedIds) {
        log.info("Rebuilding partial index: kbId={}, modifiedIds={}",
                kb.getId(), modifiedIds.size());

        try {
            // 将 String IDs 转换为 Long
            List<Long> chunkIds = modifiedIds.stream()
                    .map(Long::valueOf)
                    .toList();

            List<Chunk> chunks = chunkRepository.findAllById(chunkIds);
            EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(kb.getEmbeddingModelId());

            List<StorageStrategy.IndexUpdateItem> updateItems = new ArrayList<>();

            for (Chunk chunk : chunks) {
                // 生成新向量
                List<Float> embedding = embeddingClient.embed(chunk.getContent());
                String newVectorId = UUID.randomUUID().toString();

                // 创建更新项
                StorageStrategy.IndexUpdateItem item = new StorageStrategy.IndexUpdateItem();
                item.setId(newVectorId);
                item.setVector(embedding);
                item.setContent(chunk.getContent());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunk_id", chunk.getId());
                metadata.put("document_id", chunk.getDocumentId());
                metadata.put("kb_id", kb.getId());
                metadata.put("chunk_index", chunk.getChunkIndex());
                item.setMetadata(metadata);

                item.setUpdateType(StorageStrategy.IndexUpdateItem.UpdateType.UPDATE);
                updateItems.add(item);

                // 更新旧向量ID（先删除）
                if (chunk.getVectorId() != null) {
                    StorageStrategy.IndexUpdateItem deleteItem = new StorageStrategy.IndexUpdateItem();
                    deleteItem.setId(chunk.getVectorId());
                    deleteItem.setUpdateType(StorageStrategy.IndexUpdateItem.UpdateType.DELETE);
                    // 先删除
                    updateItems.add(0, deleteItem);
                }

                // 更新 chunk 的 vectorId
                chunk.setVectorId(newVectorId);
                chunk.setIsModified(false);
                chunk.setUpdatedAt(LocalDateTime.now());
            }

            // 执行批量更新
            storageStrategy.updatePartialIndex(String.valueOf(kb.getId()), updateItems);
            chunkRepository.saveAll(chunks);

            log.info("Partial index rebuilt successfully: kbId={}, chunks={}",
                    kb.getId(), chunks.size());

        } catch (Exception e) {
            log.error("Failed to rebuild partial index: kbId={}", kb.getId(), e);
            throw new RuntimeException("Partial index rebuilding failed", e);
        }
    }

    @Override
    @Transactional
    public void rebuildFullIndex(KnowledgeBase kb) {
        log.info("Rebuilding full index: kbId={}", kb.getId());

        try {
            // 1. 删除整个索引
            storageStrategy.deleteIndex(String.valueOf(kb.getId()));

            // 2. 获取知识库的所有 chunks
            List<Chunk> chunks = chunkRepository.findByKbId(kb.getId());

            if (chunks.isEmpty()) {
                log.info("No chunks to index for kbId={}", kb.getId());
                return;
            }

            // 3. 重新生成所有向量
            List<StorageStrategy.VectorItem> vectorItems = new ArrayList<>();
            EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(kb.getEmbeddingModelId());

            for (Chunk chunk : chunks) {
                List<Float> embedding = embeddingClient.embed(chunk.getContent());
                String vectorId = UUID.randomUUID().toString();

                StorageStrategy.VectorItem item = new StorageStrategy.VectorItem();
                item.setId(vectorId);
                item.setVector(embedding);
                item.setContent(chunk.getContent());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunk_id", chunk.getId());
                metadata.put("document_id", chunk.getDocumentId());
                metadata.put("kb_id", kb.getId());
                metadata.put("chunk_index", chunk.getChunkIndex());
                item.setMetadata(metadata);

                vectorItems.add(item);

                // 更新 chunk
                chunk.setVectorId(vectorId);
                chunk.setIsModified(false);
                chunk.setUpdatedAt(LocalDateTime.now());
            }

            // 4. 批量存储
            StorageStrategy.IndexData indexData = new StorageStrategy.IndexData();
            indexData.setVectors(vectorItems);

            Map<String, Object> globalMetadata = new HashMap<>();
            globalMetadata.put("kb_id", kb.getId());
            indexData.setMetadata(globalMetadata);

            storageStrategy.storeIndex(String.valueOf(kb.getId()), indexData);
            chunkRepository.saveAll(chunks);

            log.info("Full index rebuilt successfully: kbId={}, chunks={}",
                    kb.getId(), chunks.size());

        } catch (Exception e) {
            log.error("Failed to rebuild full index: kbId={}", kb.getId(), e);
            throw new RuntimeException("Full index rebuilding failed", e);
        }
    }

    @Override
    public void validateConfig(IndexStrategyConfig config) {
        if (!(config instanceof NaiveRAGConfig naiveConfig)) {
            throw new ConfigValidationException("Invalid config type for NaiveRAG");
        }

        if (naiveConfig.getChunkSize() <= 0) {
            throw new ConfigValidationException("Chunk size must be positive");
        }

        if (naiveConfig.getChunkOverlap() < 0) {
            throw new ConfigValidationException("Chunk overlap must be non-negative");
        }

        if (naiveConfig.getChunkOverlap() >= naiveConfig.getChunkSize()) {
            throw new ConfigValidationException("Chunk overlap must be less than chunk size");
        }
    }

    /**
     * 将文本分割成 chunks
     */
    private List<ChunkData> splitIntoChunks(String content, NaiveRAGConfig config) {
        List<ChunkData> chunks = new ArrayList<>();

        String separator = config.getSeparator() != null ? config.getSeparator() : "\n\n";
        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();

        // 简单按分隔符分割
        String[] segments = content.split(separator);
        StringBuilder currentChunk = new StringBuilder();
        int currentSize = 0;

        for (String segment : segments) {
            if (currentSize + segment.length() > chunkSize && !currentChunk.isEmpty()) {
                // 保存当前 chunk
                ChunkData chunkData = new ChunkData();
                chunkData.setContent(currentChunk.toString());
                chunks.add(chunkData);

                // 处理重叠
                if (overlap > 0) {
                    String overlapText = currentChunk.substring(
                            Math.max(0, currentChunk.length() - overlap));
                    currentChunk = new StringBuilder(overlapText);
                    currentSize = overlapText.length();
                } else {
                    currentChunk = new StringBuilder();
                    currentSize = 0;
                }
            }

            currentChunk.append(segment).append(separator);
            currentSize += segment.length() + separator.length();
        }

        // 添加最后一个 chunk
        if (!currentChunk.isEmpty()) {
            ChunkData chunkData = new ChunkData();
            chunkData.setContent(currentChunk.toString());
            chunks.add(chunkData);
        }

        return chunks;
    }

    /**
     * NaiveRAG 索引结构
     */
    @Data
    public static class NaiveRAGStructure implements IndexStructure {
        private List<ChunkData> chunks;
        private int totalChunks;
    }

    /**
     * Chunk 数据
     */
    @Data
    public static class ChunkData {
        private String content;
    }
}