package com.mtmn.smartdoc.strategy.impl;

import com.mtmn.smartdoc.config.RetrievalConfig;
import com.mtmn.smartdoc.enums.EnhancementType;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.factory.EnhancementFactory;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.Chunk;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.repository.ChunkRepository;
import com.mtmn.smartdoc.strategy.Enhancement;
import com.mtmn.smartdoc.strategy.RetrievalStrategy;
import com.mtmn.smartdoc.strategy.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * NaiveRAG 检索策略
 * 基于分块的向量检索
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaiveRAGRetrievalStrategy implements RetrievalStrategy {

    private final StorageStrategy storageStrategy;
    private final ChunkRepository chunkRepository;
    private final ModelFactory modelFactory;
    private final EnhancementFactory enhancementFactory;

    @Override
    public RetrievalResult retrieve(KnowledgeBase kb, String query, RetrievalConfig config) {
        long startTime = System.currentTimeMillis();

        log.info("NaiveRAG retrieval started: kbId={}, query={}", kb.getId(), query);

        try {
            // 1. 应用查询增强
            String enhancedQuery = applyEnhancements(query, kb, config);

            // 2. 生成查询向量
            List<Float> queryVector = generateQueryVector(enhancedQuery, kb.getEmbeddingModelId());

            // 3. 向量检索
            String collectionName = buildCollectionName(kb);
            StorageStrategy.SearchRequest searchRequest = new StorageStrategy.SearchRequest();
            searchRequest.setQueryVector(queryVector);
            searchRequest.setTopK(config.getTopK());
            searchRequest.setThreshold(config.getThreshold());

            List<StorageStrategy.SearchResult> searchResults = storageStrategy.search(
                    collectionName,
                    searchRequest
            );

            // 4. 转换为检索结果
            List<RetrievedItem> items = convertToRetrievedItems(searchResults, kb.getId());

            // 5. 构建结果
            RetrievalResult result = new RetrievalResult();
            result.setItems(items);
            result.setTotalCount(items.size());
            result.setMaxScore(items.isEmpty() ? 0.0 : items.get(0).getScore());
            result.setRetrievalTimeMs(System.currentTimeMillis() - startTime);

            log.info("NaiveRAG retrieval completed: found {} items, time={}ms",
                    items.size(), result.getRetrievalTimeMs());

            return result;

        } catch (Exception e) {
            log.error("NaiveRAG retrieval failed: kbId={}, query={}", kb.getId(), query, e);
            throw new RuntimeException("Retrieval failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EnhancementType> getSupportedEnhancements() {
        return Arrays.asList(
                EnhancementType.QUERY_REWRITE,
                EnhancementType.QUERY_DECOMPOSE,
                EnhancementType.HYDE,
                EnhancementType.HYBRID_RETRIEVAL
        );
    }

    @Override
    public IndexStrategyType getStrategyType() {
        return IndexStrategyType.NAIVE_RAG;
    }

    /**
     * 应用查询增强
     */
    private String applyEnhancements(String query, KnowledgeBase kb, RetrievalConfig config) {
        if (config.getEnhancements() == null || config.getEnhancements().isEmpty()) {
            return query;
        }

        String currentQuery = query;

        for (EnhancementType type : config.getEnhancements()) {
            if (!enhancementFactory.isEnhancementAvailable(type)) {
                log.warn("Enhancement not available: {}", type);
                continue;
            }

            try {
                Enhancement enhancement = enhancementFactory.getEnhancement(type);

                // 构建增强上下文
                Enhancement.EnhancementContext context = new Enhancement.EnhancementContext();
                context.setKbId(kb.getId());
                context.setLlmModelId(config.getLlmModelId());
                context.setParams(config.getEnhancementParams());

                // 执行增强
                Enhancement.EnhancedQuery enhancedQuery = enhancement.enhance(currentQuery, context);

                // 使用增强后的查询
                if (enhancedQuery.getQuery() != null && !enhancedQuery.getQuery().isEmpty()) {
                    currentQuery = enhancedQuery.getQuery();
                }

                log.debug("Applied enhancement {}: original='{}', enhanced='{}'",
                        type, query, currentQuery);

            } catch (Exception e) {
                log.error("Enhancement failed: type={}", type, e);
                // 继续使用原查询
            }
        }

        return currentQuery;
    }

    /**
     * 生成查询向量
     */
    private List<Float> generateQueryVector(String query, String embeddingModelId) {
        try {
            EmbeddingClient client = modelFactory.createEmbeddingClient(embeddingModelId);
            return client.embed(query);
        } catch (Exception e) {
            log.error("Failed to generate query vector", e);
            throw new RuntimeException("Failed to generate query vector: " + e.getMessage(), e);
        }
    }

    /**
     * 构建集合名称
     */
    private String buildCollectionName(KnowledgeBase kb) {
        return "kb_" + kb.getId() + "_chunks";
    }

    /**
     * 转换为检索结果项
     */
    private List<RetrievedItem> convertToRetrievedItems(
            List<StorageStrategy.SearchResult> searchResults,
            Long kbId) {

        if (searchResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取所有 chunk IDs（从 metadata 中获取）
        List<Long> chunkIds = searchResults.stream()
                .map(result -> {
                    Map<String, Object> metadata = result.getMetadata();
                    if (metadata != null && metadata.containsKey("chunk_id")) {
                        Object chunkId = metadata.get("chunk_id");
                        return chunkId instanceof Number ? ((Number) chunkId).longValue() : null;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 批量查询数据库获取 chunk 内容
        List<Chunk> chunks = chunkRepository.findAllById(chunkIds);

        // 构建 ID -> Chunk 映射
        Map<Long, Chunk> chunkMap = chunks.stream()
                .collect(Collectors.toMap(
                        Chunk::getId,
                        chunk -> chunk
                ));

        // 转换为检索项
        return searchResults.stream()
                .map(result -> {
                    RetrievedItem item = new RetrievedItem();
                    item.setSourceType("chunk");
                    item.setScore(result.getScore());

                    // 从 metadata 获取 chunk_id
                    Long chunkId = null;
                    if (result.getMetadata() != null && result.getMetadata().containsKey("chunk_id")) {
                        Object id = result.getMetadata().get("chunk_id");
                        chunkId = id instanceof Number ? ((Number) id).longValue() : null;
                    }

                    item.setSourceId(chunkId != null ? String.valueOf(chunkId) : result.getId());

                    // 获取 chunk 内容
                    Chunk chunk = chunkId != null ? chunkMap.get(chunkId) : null;
                    if (chunk != null) {
                        item.setContent(chunk.getContent());

                        // 设置元数据
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("chunk_index", chunk.getChunkIndex());
                        metadata.put("document_id", chunk.getDocumentId());
                        item.setMetadata(metadata);
                    } else {
                        log.warn("Chunk not found in database: id={}", result.getId());
                        item.setContent("");
                    }

                    return item;
                })
                .collect(Collectors.toList());
    }
}