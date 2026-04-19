package com.mtmn.smartrag.service.impl;

import com.mtmn.smartrag.constants.AppConstants;
import com.mtmn.smartrag.exception.MilvusConnectionException;
import com.mtmn.smartrag.model.dto.IndexUpdateItem;
import com.mtmn.smartrag.model.dto.VectorItem;
import com.mtmn.smartrag.service.MilvusService;
import com.mtmn.smartrag.vo.RetrievalResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.grpc.StatusRuntimeException;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.collection.DropCollectionParam;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Milvus向量数据库服务实现
 * 负责创建和管理Milvus嵌入存储
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Slf4j
@Service
public class MilvusServiceImpl implements MilvusService {

    @Value("${milvus.host}")
    String host;

    @Value("${milvus.port}")
    Integer port;

    private MilvusServiceClient milvusClient;
    private final Map<String, MilvusEmbeddingStore> storeCache = new ConcurrentHashMap<>();

    // ===================== 常量定义 =====================
    private static final int MILVUS_INSERT_BATCH_SIZE = AppConstants.Milvus.INSERT_BATCH_SIZE;
    private static final int RETRY_MAX_ATTEMPTS = AppConstants.Retry.MAX_ATTEMPTS;
    private static final long RETRY_DELAY_MS = AppConstants.Retry.RETRY_DELAY_MS;
    private static final String MILVUS_CHUNKS_COLLECTION_TEMPLATE = AppConstants.Milvus.CHUNKS_TEMPLATE;

    @PostConstruct
    public void init() {
        try {
            milvusClient = new MilvusServiceClient(
                    ConnectParam.newBuilder()
                            .withHost(host)
                            .withPort(port)
                            .build()
            );
            log.info("✅ Milvus 客户端初始化成功");
        } catch (Exception e) {
            log.error("❌ Milvus 客户端初始化失败: {}", e.getMessage());
        }
    }

    @Override
    public MilvusEmbeddingStore getEmbeddingStore(String collectionName, Integer dimension) {
        return storeCache.computeIfAbsent(collectionName, k -> {
            // 使用重试机制创建 store
            return executeWithRetry(() -> MilvusEmbeddingStore.builder()
                    .host(host)
                    .port(port)
                    .collectionName(collectionName)
                    .dimension(dimension)
                    .indexType(IndexType.FLAT)
                    .metricType(MetricType.COSINE)
                    .consistencyLevel(ConsistencyLevelEnum.EVENTUALLY)
                    .autoFlushOnInsert(false)
                    .idFieldName("id")
                    .textFieldName("text")
                    .metadataFieldName("metadata")
                    .vectorFieldName("vector")
                    .build(), "create embedding store");
        });
    }

    @Override
    public void dropCollection(String collectionName) {
        log.info("🗑️  删除 Milvus 集合: {}", collectionName);

        if (milvusClient == null) {
            log.warn("⚠️  Milvus 客户端未初始化，尝试重新初始化...");
            init();
            if (milvusClient == null) {
                log.warn("⚠️  Milvus 服务不可用，跳过删除集合: {}", collectionName);
                return;
            }
        }

        try {
            executeWithRetry(() -> {
                milvusClient.dropCollection(
                        DropCollectionParam.newBuilder()
                                .withCollectionName(collectionName)
                                .build()
                );
                return null;
            }, "drop collection");
            
            storeCache.remove(collectionName);
            log.info("✅ 集合删除成功: {}", collectionName);
        } catch (Exception e) {
            log.warn("⚠️  集合删除失败: {}, 错误: {}", collectionName, e.getMessage());
        }
    }

    @Override
    public List<RetrievalResult> search(Long kbId, Embedding queryVector, int topK, double threshold) {
        String collectionName = MILVUS_CHUNKS_COLLECTION_TEMPLATE.formatted(kbId);
        int dimension = queryVector.dimension();
        
        long startTime = System.currentTimeMillis();
        log.debug("🔍 Milvus向量搜索开始: collection={}, dimension={}, topK={}, threshold={}", 
                  collectionName, dimension, topK, threshold);
        
        MilvusEmbeddingStore store = getEmbeddingStore(collectionName, dimension);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryVector)
                .maxResults(topK)
                .minScore(threshold)
                .build();

        EmbeddingSearchResult<TextSegment> result = executeWithRetry(
                () -> store.search(request),
                "search vectors in " + collectionName
        );
        
        List<RetrievalResult> results = result.matches().stream()
                .map(this::convertToRetrievalResult)
                .collect(Collectors.toList());
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Milvus向量搜索完成: collection={}, 耗时={}ms, 结果数={}", 
                 collectionName, duration, results.size());
        if (!results.isEmpty()) {
            log.debug("  TOP3结果: {} | {} | {}",
                    results.get(0).getScore() + " (" + results.get(0).getContent().substring(0, Math.min(20, results.get(0).getContent().length())) + ")",
                    results.size() > 1 ? results.get(1).getScore() : "N/A",
                    results.size() > 2 ? results.get(2).getScore() : "N/A");
        }

        return results;
    }

    @Override
    public List<RetrievalResult> search(Long kbId, Embedding queryVector, int topK,
                                         double threshold, Filter filter) {
        String collectionName = MILVUS_CHUNKS_COLLECTION_TEMPLATE.formatted(kbId);
        int dimension = queryVector.dimension();
        
        long startTime = System.currentTimeMillis();
        log.debug("🔍 Milvus过滤向量搜索开始: collection={}, dimension={}, topK={}, threshold={}, filter={}", 
                  collectionName, dimension, topK, threshold, filter != null ? "applied" : "none");
        
        MilvusEmbeddingStore store = getEmbeddingStore(collectionName, dimension);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryVector)
                .maxResults(topK)
                .minScore(threshold)
                .filter(filter)
                .build();

        EmbeddingSearchResult<TextSegment> result = executeWithRetry(
                () -> store.search(request),
                "search vectors with filter in " + collectionName
        );

        List<RetrievalResult> results = result.matches().stream()
                .map(this::convertToRetrievalResult)
                .collect(Collectors.toList());
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Milvus过滤向量搜索完成: collection={}, 耗时={}ms, 结果数={}", 
                 collectionName, duration, results.size());

        return results;
    }

    @Override
    public void store(Long kbId, List<VectorItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String collectionName = MILVUS_CHUNKS_COLLECTION_TEMPLATE.formatted(kbId);
        int dimension = items.get(0).getEmbedding().dimension();
        
        // 检查维度一致性
        if (!items.stream().allMatch(item -> item.getEmbedding().dimension() == dimension)) {
            throw new IllegalArgumentException("All vectors must have the same dimension");
        }

        MilvusEmbeddingStore store = getEmbeddingStore(collectionName, dimension);
        log.info("📊 开始存储向量: collectionName={}, totalItems={}, dimension={}", 
                 collectionName, items.size(), dimension);

        List<String> ids = items.stream().map(VectorItem::getId).collect(Collectors.toList());
        List<Embedding> embeddings = items.stream().map(VectorItem::getEmbedding).collect(Collectors.toList());
        List<TextSegment> segments = items.stream()
                .map(item -> TextSegment.from(item.getContent(), dev.langchain4j.data.document.Metadata.from(item.getMetadata())))
                .collect(Collectors.toList());

        // 分批插入
        int batchCount = 0;
        for (int i = 0; i < ids.size(); i += MILVUS_INSERT_BATCH_SIZE) {
            int end = Math.min(i + MILVUS_INSERT_BATCH_SIZE, ids.size());
            List<String> batchIds = ids.subList(i, end);
            List<Embedding> batchEmbeddings = embeddings.subList(i, end);
            List<TextSegment> batchSegments = segments.subList(i, end);
            
            int batchNum = batchCount + 1;
            int totalBatches = (ids.size() + MILVUS_INSERT_BATCH_SIZE - 1) / MILVUS_INSERT_BATCH_SIZE;

            executeWithRetry(() -> {
                store.addAll(batchIds, batchEmbeddings, batchSegments);
                log.debug("✅ 向量批次存储成功: 批次 {}/{}, 数量={}", 
                          batchNum, totalBatches, batchIds.size());
                return null;
            }, "store vectors batch [" + batchNum + "/" + totalBatches + "]");
            
            batchCount++;
        }
        
        log.info("✅ 向量存储完成: collectionName={}, totalItems={}, batches={}", 
                 collectionName, items.size(), batchCount);
    }

    @Override
    public void update(Long kbId, List<IndexUpdateItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        String collectionName = MILVUS_CHUNKS_COLLECTION_TEMPLATE.formatted(kbId);
        // 假设维度一致，取第一个非空的 embedding 维度，或者默认维度
        int dimension = AppConstants.DEFAULT_EMBEDDING_DIMENSION;
        for (IndexUpdateItem item : items) {
            if (item.getEmbedding() != null) {
                dimension = item.getEmbedding().dimension();
                break;
            }
        }

        MilvusEmbeddingStore store = getEmbeddingStore(collectionName, dimension);

        for (IndexUpdateItem item : items) {
            executeWithRetry(() -> {
                switch (item.getUpdateType()) {
                    case DELETE:
                        store.remove(item.getId());
                        break;
                    case ADD:
                    case UPDATE:
                        if (item.getUpdateType() == IndexUpdateItem.UpdateType.UPDATE) {
                            store.remove(item.getId());
                        }
                        if (item.getEmbedding() != null && item.getContent() != null) {
                             // 使用 addAll 插入单个元素以支持指定 ID
                             store.addAll(
                                 Collections.singletonList(item.getId()),
                                 Collections.singletonList(item.getEmbedding()),
                                 Collections.singletonList(TextSegment.from(item.getContent(), dev.langchain4j.data.document.Metadata.from(item.getMetadata())))
                             );
                        }
                        break;
                }
                return null;
            }, "update index item");
        }
    }

    @Override
    public void removeByDocumentId(Long kbId, Long docId) {
        if (kbId == null || docId == null) {
            return;
        }
        String collectionName = MILVUS_CHUNKS_COLLECTION_TEMPLATE.formatted(kbId);
        // 获取 Store 实例，使用默认维度（删除操作不依赖维度，但获取实例需要）
        MilvusEmbeddingStore store = getEmbeddingStore(collectionName, AppConstants.DEFAULT_EMBEDDING_DIMENSION);

        executeWithRetry(() -> {
            // 构建过滤条件：metadata.document_id == docId
            Filter filter = MetadataFilterBuilder.metadataKey(AppConstants.Milvus.METADATA_FIELD_DOC_ID).isEqualTo(docId);
            store.removeAll(filter);
            return null;
        }, "remove by document id");
        
        log.info("✅ 向量删除成功: 文档id={}, 知识库id={}", docId, kbId);
    }

    // ===================== 私有辅助方法 =====================

    private RetrievalResult convertToRetrievalResult(EmbeddingMatch<TextSegment> match) {
        RetrievalResult rr = new RetrievalResult();
        rr.setScore(match.score());
        rr.setSourceId(match.embeddingId());
        rr.setSourceType("chunk");
        if (match.embedded() != null) {
            rr.setContent(match.embedded().text());
            rr.setMetadata(match.embedded().metadata().toMap());
        }
        return rr;
    }

    private <T> T executeWithRetry(RetryableOperation<T> operation, String operationName) {
        for (int attempt = 1; attempt <= RETRY_MAX_ATTEMPTS; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                // 检查是否为可重试的异常
                if (isRetryable(e)) {
                    String errorMsg = buildMilvusErrorMessage(e, attempt);
                    log.warn("⚠️ {} 执行失败: {}", operationName, errorMsg);

                    if (attempt == RETRY_MAX_ATTEMPTS) {
                        throw new MilvusConnectionException(
                                operationName + " 失败（已重试 " + RETRY_MAX_ATTEMPTS + " 次）: " + getRootCauseMessage(e),
                                e
                        );
                    }
                    sleepQuietly(RETRY_DELAY_MS * attempt);
                } else {
                    // 不可重试的异常，直接抛出
                    log.error("❌ {} 执行出错: {}", operationName, e.getMessage());
                    throw new RuntimeException(operationName + " 执行失败: " + e.getMessage(), e);
                }
            }
        }
        throw new MilvusConnectionException(operationName + " 超时，已重试 " + RETRY_MAX_ATTEMPTS + " 次");
    }

    private boolean isRetryable(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof StatusRuntimeException) {
                return true;
            }
            // 检查异常消息是否包含连接相关错误
            String msg = cause.getMessage();
            if (msg != null && (
                    msg.contains("DEADLINE_EXCEEDED") ||
                    msg.contains("UNAVAILABLE") ||
                    msg.contains("ConnectException") ||
                    msg.contains("Failed to initialize connection")
            )) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String getRootCauseMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    private String buildMilvusErrorMessage(Exception e, int attempt) {
        // 尝试找到 StatusRuntimeException
        Throwable cause = e;
        StatusRuntimeException statusEx = null;
        while (cause != null) {
            if (cause instanceof StatusRuntimeException) {
                statusEx = (StatusRuntimeException) cause;
                break;
            }
            cause = cause.getCause();
        }

        if (statusEx != null) {
            String status = statusEx.getStatus().getCode().name();
            if (status.contains("DEADLINE_EXCEEDED")) {
                return String.format("连接超时（尝试 %d/%d）- Milvus 服务可能未启动或网络不可达", attempt, RETRY_MAX_ATTEMPTS);
            } else if (status.contains("UNAVAILABLE")) {
                return String.format("服务不可用（尝试 %d/%d）- Milvus 服务可能正在启动中", attempt, RETRY_MAX_ATTEMPTS);
            } else {
                return String.format("连接错误（尝试 %d/%d）- %s: %s", attempt, RETRY_MAX_ATTEMPTS, status, statusEx.getMessage());
            }
        }
        
        return String.format("操作失败（尝试 %d/%d）- %s", attempt, RETRY_MAX_ATTEMPTS, e.getMessage());
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("⚠️  重试等待被中断");
        }
    }

    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}