package com.mtmn.smartdoc.strategy.impl;

import com.mtmn.smartdoc.service.MilvusService;
import com.mtmn.smartdoc.strategy.StorageStrategy;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Milvus 向量存储策略实现
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusStorageStrategy implements StorageStrategy {

    // 默认向量维度（根据embedding模型调整）
    private static final int DEFAULT_DIMENSION = 1024;
    private final MilvusService milvusService;
    // 缓存 MilvusEmbeddingStore 实例
    private final Map<String, MilvusEmbeddingStore> storeCache = new ConcurrentHashMap<>();

    @Override
    public void storeIndex(String kbId, IndexData data) {
        log.info("Storing index: kbId={}, vectors={}", kbId,
                data.getVectors().size());

        try {
            String collectionName = getCollectionName(kbId);
            MilvusEmbeddingStore store = getOrCreateStore(collectionName,
                    data.getVectors().get(0).getVector().size());

            // 批量插入向量
            for (VectorItem item : data.getVectors()) {
                // 构建 embedding (使用 float 数组)
                float[] floatArray = new float[item.getVector().size()];
                for (int i = 0; i < item.getVector().size(); i++) {
                    floatArray[i] = item.getVector().get(i);
                }
                Embedding embedding = Embedding.from(floatArray);

                // 构建 TextSegment
                TextSegment segment = TextSegment.from(item.getContent());

                // 插入 (MilvusEmbeddingStore.add 只接受 embedding)
                store.add(embedding, segment);
            }

            log.info("Index stored successfully: kbId={}, vectors={}",
                    kbId, data.getVectors().size());

        } catch (Exception e) {
            log.error("Failed to store index: kbId={}", kbId, e);
            throw new RuntimeException("Failed to store vector index", e);
        }
    }

    @Override
    public List<SearchResult> search(String kbId, SearchRequest request) {
        log.debug("Searching: kbId={}, topK={}", kbId, request.getTopK());

        try {
            String collectionName = getCollectionName(kbId);
            MilvusEmbeddingStore store = getOrCreateStore(collectionName, DEFAULT_DIMENSION);

            // 构建查询 embedding
            float[] queryArray = new float[request.getQueryVector().size()];
            for (int i = 0; i < request.getQueryVector().size(); i++) {
                queryArray[i] = request.getQueryVector().get(i);
            }
            Embedding queryEmbedding = Embedding.from(queryArray);

            // 构建搜索请求
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(request.getTopK())
                    .minScore(request.getThreshold())
                    .build();

            // 执行搜索
            EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);

            // 转换结果并应用阈值过滤
            return searchResult.matches().stream()
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to search: kbId={}", kbId, e);
            throw new RuntimeException("Failed to search vector index", e);
        }
    }

    @Override
    public void deleteIndex(String kbId) {
        log.info("Deleting index: kbId={}", kbId);

        try {
            String collectionName = getCollectionName(kbId);

            // 从缓存中移除
            storeCache.remove(collectionName);

            // 注意: MilvusEmbeddingStore 不直接提供删除集合的方法
            // 需要通过底层 Milvus SDK 或重新创建来清空
            log.info("Removed collection from cache: {}", collectionName);

        } catch (Exception e) {
            log.error("Failed to delete index: kbId={}", kbId, e);
            throw new RuntimeException("Failed to delete vector index", e);
        }
    }

    @Override
    public void updatePartialIndex(String kbId, List<IndexUpdateItem> items) {
        log.info("Updating partial index: kbId={}, items={}", kbId, items.size());

        try {
            String collectionName = getCollectionName(kbId);
            MilvusEmbeddingStore store = getOrCreateStore(collectionName, DEFAULT_DIMENSION);

            for (IndexUpdateItem item : items) {
                switch (item.getUpdateType()) {
                    case DELETE:
                        store.remove(item.getId());
                        break;

                    case ADD:
                    case UPDATE:
                        // UPDATE 先删除再添加
                        if (item.getUpdateType() == IndexUpdateItem.UpdateType.UPDATE) {
                            store.remove(item.getId());
                        }

                        float[] floatArray = new float[item.getVector().size()];
                        for (int i = 0; i < item.getVector().size(); i++) {
                            floatArray[i] = item.getVector().get(i);
                        }
                        Embedding embedding = Embedding.from(floatArray);
                        TextSegment segment = TextSegment.from(item.getContent());
                        store.add(embedding, segment);
                        break;
                }
            }

            log.info("Partial index updated successfully: items={}", items.size());

        } catch (Exception e) {
            log.error("Failed to update partial index: kbId={}", kbId, e);
            throw new RuntimeException("Failed to update vector index", e);
        }
    }

    /**
     * 获取或创建 MilvusEmbeddingStore
     */
    private MilvusEmbeddingStore getOrCreateStore(String collectionName, int dimension) {
        return storeCache.computeIfAbsent(collectionName,
                k -> milvusService.getEmbeddingStore(k, dimension));
    }

    /**
     * 获取集合名称
     * 注意：kbId 参数实际上已经是完整的 collectionName（如 kb_123_chunks 或 kb_123_nodes）
     */
    private String getCollectionName(String kbId) {
        return kbId;
    }

    /**
     * 转换搜索结果
     */
    private SearchResult convertToSearchResult(EmbeddingMatch<TextSegment> match) {
        SearchResult result = new SearchResult();
        result.setId(match.embeddingId());
        result.setScore(match.score());

        if (match.embedded() != null) {
            result.setContent(match.embedded().text());
            // metadata 转换为 Map
            result.setMetadata(new HashMap<>());
        }

        return result;
    }
}