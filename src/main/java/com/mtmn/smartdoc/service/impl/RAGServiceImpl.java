package com.mtmn.smartdoc.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.constants.AppConstants;
import com.mtmn.smartdoc.dto.HisemRagChatRequest;
import com.mtmn.smartdoc.dto.NaiveRagChatRequest;
import com.mtmn.smartdoc.dto.RagChatRequest;
import com.mtmn.smartdoc.exception.ResourceNotFoundException;
import com.mtmn.smartdoc.exception.UnauthorizedAccessException;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.model.client.SseEventBuilder;
import com.mtmn.smartdoc.model.client.StreamEventHandler;
import com.mtmn.smartdoc.model.client.TokenUsageLedger;
import com.mtmn.smartdoc.model.dto.ReferenceDocument;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.Conversation;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.rag.retriever.AdaptiveRetriever;
import com.mtmn.smartdoc.rag.retriever.RetrievalTreeNode;
import com.mtmn.smartdoc.rag.sadp.SadpPlanner;
import com.mtmn.smartdoc.rag.support.RagStreamErrorMapper;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.repository.KnowledgeBaseRepository;
import com.mtmn.smartdoc.service.ConversationService;
import com.mtmn.smartdoc.service.MilvusService;
import com.mtmn.smartdoc.service.RAGService;
import com.mtmn.smartdoc.service.component.RAGQueryProcessor;
import com.mtmn.smartdoc.vo.RetrievalResult;
import dev.langchain4j.data.embedding.Embedding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * RAG 检索服务实现 - 使用管道模式组装增强功能
 *
 * @author charmingdaidai
 * @version 1.0
 * @date 2025-11-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGServiceImpl implements RAGService {

    private static final String MILVUS_CHUNKS_COLLECTION_TEMPLATE = AppConstants.Milvus.CHUNKS_TEMPLATE;
    private final ModelFactory modelFactory;
    private final MilvusService milvusService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final DocumentRepository documentRepository;
    private final ConversationService conversationService;
    private final RAGQueryProcessor ragQueryProcessor;
    private final AdaptiveRetriever adaptiveRetriever;
    private final SadpPlanner sadpPlanner;
    private final ObjectMapper objectMapper;

    /**
     * @param request
     * @return
     */
    @Override
    public SseEmitter naiveRagChat(Long userId, NaiveRagChatRequest request) {
        log.info("收到 Naive RAG 对话请求: userId={}, kbId={}, question={}", userId, request.getKbId(), request.getQuestion());

        // 创建 SseEmitter，设置超时时间 (例如 5 分钟)
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        Long kbId = request.getKbId();
        validateKnowledgeBaseAccess(kbId, userId);
        String sessionId = conversationService.resolveSessionId(request.getSessionId());
        request.setSessionId(sessionId);
        String historyText = conversationService.buildHistoryText(kbId, userId, sessionId, request.getHistoryWindow());

        // 异步执行，避免阻塞 Tomcat 线程
        CompletableFuture.runAsync(() -> {
            try {
                String question = request.getQuestion();
                LLMClient llmClient = modelFactory.createLLMClient(request.getLlmModelId());
                EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(request.getEmbeddingModelId());

                // 1. 意图识别
                if (request.getEnableIntentRecognition()) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在分析查询意图...", "search");
                    boolean needRetrieval = ragQueryProcessor.analyzeIntent(question, historyText);
                    if (!needRetrieval) {
                        log.info("意图识别结果: 不需要检索");
                        // 直接聊天
                        llmClient.streamChatWithEmitter(
                                question,
                                emitter,
                                buildPersistenceHandler(userId, kbId, sessionId, request, Collections.emptyList())
                        );
                        return;
                    }
                    log.info("意图识别结果: 需要检索");
                }

                // 收集所有需要检索的查询语句
                Set<String> searchQueries = new HashSet<>();

                // 2. 查询重写
                String finalQuery = question;
                if (request.getEnableQueryRewrite()) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在重写查询语句...", "edit");
                    String rewritten = ragQueryProcessor.rewriteQuery(question, historyText);
                    if (rewritten != null && !rewritten.equals(question)) {
                        finalQuery = rewritten;
                        log.info("查询重写结果: {}", finalQuery);
                    }
                }
                searchQueries.add(finalQuery);

                // 3. 问题分解
                if (request.getEnableQueryDecomposition()) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在问题分解...", "edit");
                    List<String> subQueries = ragQueryProcessor.decomposeQuery(question);
                    if (subQueries != null && !subQueries.isEmpty()) {
                        searchQueries.addAll(subQueries);
                        log.info("问题分解结果: {}", subQueries);
                    }
                }

                // 4. HyDE
                if (request.getEnableHyde()) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在生成假想答案(HyDE)...", "think");
                    // 对所有查询进行 HyDE 转换
                    List<CompletableFuture<String>> hydeFutures = searchQueries.stream()
                            .map(q -> CompletableFuture.supplyAsync(() -> {
                                String doc = ragQueryProcessor.generateHydeDoc(q);
                                // 如果生成失败，保留原问题
                                return (doc != null && !doc.isEmpty()) ? doc : q;
                            }))
                            .toList();

                    CompletableFuture.allOf(hydeFutures.toArray(new CompletableFuture[0])).join();

                    // 替换原查询集合
                    searchQueries = hydeFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toSet());

                    log.info("HyDE处理完成，生成 {} 个假设文档用于检索", searchQueries.size());
                }

                // 5. 并行向量检索
                SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在检索中...", "search");
                int topK = request.getTopK() != null ? request.getTopK() : 10;

                // 如果启用了问题分解，每个子问题都检索 topK 个结果，确保每个子知识点都能被检索到
                List<RetrievalResult> results = parallelSearch(kbId, searchQueries, embeddingClient, topK, request.getThreshold());

                // 如果没有启用问题分解，才需要截断到 topK
                // 启用问题分解时，保留所有结果（每个子问题的 topK），确保覆盖所有子知识点
                if (!Boolean.TRUE.equals(request.getEnableQueryDecomposition()) && results.size() > topK) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在对结果重排序...", "sort");
                    results = results.stream().limit(topK).toList();
                }

                log.info("检索完成: 查询数={}, 结果数={}, 问题分解={}",
                        searchQueries.size(), results.size(), request.getEnableQueryDecomposition());

                // 获取文档标题
                List<Long> docIds = results.stream()
                        .map(r -> {
                            Object docIdObj = r.getMetadata().get("document_id");
                            return docIdObj instanceof Number ? ((Number) docIdObj).longValue() : null;
                        })
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());

                Map<Long, String> docTitles = documentRepository.findAllById(docIds).stream()
                        .collect(Collectors.toMap(DocumentPo::getId, DocumentPo::getFilename));

                // 转换为 ReferenceDocument
                List<ReferenceDocument> documents = results.stream()
                        .map(r -> {
                            Long docId = null;
                            Object docIdObj = r.getMetadata().get("document_id");
                            if (docIdObj instanceof Number) {
                                docId = ((Number) docIdObj).longValue();
                            }

                            return ReferenceDocument.builder()
                                    .title(docId != null ? docTitles.getOrDefault(docId, "Unknown Document") : "Unknown Document")
                                    .score(r.getScore())
                                    .content(r.getContent())
                                    .documentId(docId)
                                    .chunkId(r.getSourceId())
                                    .build();
                        })
                        .collect(Collectors.toList());

                // 检索完成
                String message = String.format("检索完成，找到 %d 个相关片段", documents.size());
                SseEventBuilder.sendThoughtEvent(emitter, "success", message, "check");

                // 发送参考文档
                if (!documents.isEmpty()) {
                    SseEventBuilder.sendRefEvent(emitter, documents);
                }

                // 构建 Context (带 XML 标签和 ID)
                String context = IntStream.range(0, documents.size())
                        .mapToObj(i -> {
                            ReferenceDocument doc = documents.get(i);
                            // 索引从 1 开始，符合人类阅读习惯
                            int docId = i + 1;
                            // 移除内容中可能破坏 XML 结构的特殊字符（可选，视情况而定）
                            String safeContent = doc.getContent().replace("<", "&lt;").replace(">", "&gt;");

                            // 使用 XML 格式包裹单篇文档
                            return String.format("""
                                            <doc id="%d" title="%s">
                                            %s
                                            </doc>""",
                                    docId,
                                    doc.getTitle(),
                                    safeContent
                            );
                        })
                        .collect(Collectors.joining("\n"));

                // 替换 Prompt 变量
                String prompt = AppConstants.PromptTemplates.RAG_ANSWER
                        .replace("{query}", question)
                        .replace("{context}", context);

                // 大模型回答
                llmClient.streamChatWithEmitter(
                    prompt,
                    emitter,
                    buildPersistenceHandler(userId, kbId, sessionId, request, documents)
                );

            } catch (Exception e) {
                handleStreamException(emitter, "NaiveRAG 对话失败", e);
            }
        });

        return emitter;
    }

    /**
     * 并行执行向量检索
     *
     * @param kbId            知识库ID
     * @param queries         查询语句集合
     * @param embeddingClient Embedding客户端
     * @param topK            每个查询的TopK
     * @param threshold       相似度阈值
     * @return 合并去重后的结果列表
     */
    private List<RetrievalResult> parallelSearch(Long kbId, Set<String> queries, EmbeddingClient embeddingClient, int topK, double threshold) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyList();
        }

        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>(null);

        List<CompletableFuture<List<RetrievalResult>>> futures = queries.stream()
                .map(query -> CompletableFuture.supplyAsync(() -> {
                    Embedding queryVector = embeddingClient.embed(query);
                    return milvusService.search(kbId, queryVector, topK, threshold);
                }).handle((results, throwable) -> {
                    if (throwable == null) {
                        return results == null ? Collections.<RetrievalResult>emptyList() : results;
                    }

                    failureCount.incrementAndGet();
                    Throwable root = RagStreamErrorMapper.unwrap(throwable);
                    firstFailure.compareAndSet(null, root);
                    log.warn("检索失败: query={}, error={}",
                            query,
                            root != null ? root.getMessage() : throwable.getMessage());
                        return Collections.<RetrievalResult>emptyList();
                }))
                .toList();

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        if (failureCount.get() == queries.size()) {
            throw new RuntimeException("全部检索任务执行失败", firstFailure.get());
        }
        if (failureCount.get() > 0) {
            log.warn("并行检索部分失败: failed={}/{}", failureCount.get(), queries.size());
        }

        // 合并结果并去重
        Map<String, RetrievalResult> uniqueResults = new HashMap<>();
        for (CompletableFuture<List<RetrievalResult>> future : futures) {
            List<RetrievalResult> results = future.join();
            for (RetrievalResult result : results) {
                // 使用 sourceId (chunkId) 作为去重键
                // 如果同一个 chunk 被多次检索到，保留分数最高的那个（通常 Milvus 返回的就是最高分，这里简单覆盖即可，或者比较 score）
                String key = result.getSourceId();
                if (!uniqueResults.containsKey(key) || result.getScore() > uniqueResults.get(key).getScore()) {
                    uniqueResults.put(key, result);
                }
            }
        }

        // 排序并返回
        return uniqueResults.values().stream()
                .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore())) // 降序
                .collect(Collectors.toList());
    }

    /**
     * HisemRAG Fast 对话
     * 检索逻辑与 NaiveRAG 类似，都是从 Chunk 表检索
     * 区别在于索引构建时使用了 Markdown 结构化解析和标题增强
     *
     * @param request 对话请求
     * @return SSE 事件流
     */
    @Override
    public SseEmitter hisemRagFastChat(Long userId, HisemRagChatRequest request) {
        log.info("收到 HisemRAG Fast 对话请求: userId={}, kbId={}, question={}", userId, request.getKbId(), request.getQuestion());

        // 创建 SseEmitter，设置超时时间 (5 分钟)
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        Long kbId = request.getKbId();
        validateKnowledgeBaseAccess(kbId, userId);
        String sessionId = conversationService.resolveSessionId(request.getSessionId());
        request.setSessionId(sessionId);
        String historyText = conversationService.buildHistoryText(kbId, userId, sessionId, request.getHistoryWindow());

        // 异步执行，避免阻塞 Tomcat 线程
        CompletableFuture.runAsync(() -> {
            try {
                String question = request.getQuestion();
                LLMClient llmClient = modelFactory.createLLMClient(request.getLlmModelId());
                EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(request.getEmbeddingModelId());

                // 1. 意图识别
                if (Boolean.TRUE.equals(request.getEnableIntentRecognition())) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在分析查询意图...", "search");
                    boolean needRetrieval = ragQueryProcessor.analyzeIntent(question, historyText);
                    if (!needRetrieval) {
                        log.info("意图识别结果: 不需要检索");
                        llmClient.streamChatWithEmitter(
                                question,
                                emitter,
                                buildPersistenceHandler(userId, kbId, sessionId, request, Collections.emptyList())
                        );
                        return;
                    }
                    log.info("意图识别结果: 需要检索");
                }

                // 收集所有需要检索的查询语句
                Set<String> searchQueries = new HashSet<>();

                // 2. 查询重写
                String finalQuery = question;
                if (Boolean.TRUE.equals(request.getEnableQueryRewrite())) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在重写查询语句...", "edit");
                    String rewritten = ragQueryProcessor.rewriteQuery(question, historyText);
                    if (rewritten != null && !rewritten.equals(question)) {
                        finalQuery = rewritten;
                        log.info("查询重写结果: {}", finalQuery);
                    }
                }
                searchQueries.add(finalQuery);

                // 3. 问题分解
                if (Boolean.TRUE.equals(request.getEnableQueryDecomposition())) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在问题分解...", "edit");
                    List<String> subQueries = ragQueryProcessor.decomposeQuery(question);
                    if (subQueries != null && !subQueries.isEmpty()) {
                        searchQueries.addAll(subQueries);
                        log.info("问题分解结果: {}", subQueries);
                    }
                }

                // 4. HyDE
                if (Boolean.TRUE.equals(request.getEnableHyde())) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在生成假想答案(HyDE)...", "think");
                    List<CompletableFuture<String>> hydeFutures = searchQueries.stream()
                            .map(q -> CompletableFuture.supplyAsync(() -> {
                                String doc = ragQueryProcessor.generateHydeDoc(q);
                                return (doc != null && !doc.isEmpty()) ? doc : q;
                            }))
                            .toList();

                    CompletableFuture.allOf(hydeFutures.toArray(new CompletableFuture[0])).join();

                    searchQueries = hydeFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toSet());

                    log.info("HyDE处理完成，生成 {} 个假设文档用于检索", searchQueries.size());
                }

                // 5. 并行向量检索
                SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在检索中...", "search");
                int topK = request.getMaxTopK() != null ? request.getMaxTopK() : 10;

                // 如果启用了问题分解，每个子问题都检索 topK 个结果，确保每个子知识点都能被检索到
                // 否则所有查询共享 topK 限制
                int perQueryTopK = Boolean.TRUE.equals(request.getEnableQueryDecomposition()) ? topK : topK;
                List<RetrievalResult> results = parallelSearch(kbId, searchQueries, embeddingClient, perQueryTopK, 0.0);

                // 如果没有启用问题分解，才需要截断到 topK
                // 启用问题分解时，保留所有结果（每个子问题的 topK），确保覆盖所有子知识点
                if (!Boolean.TRUE.equals(request.getEnableQueryDecomposition()) && results.size() > topK) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在对结果重排序...", "sort");
                    results = results.stream().limit(topK).toList();
                }

                log.info("检索完成: 查询数={}, 结果数={}, 问题分解={}",
                        searchQueries.size(), results.size(), request.getEnableQueryDecomposition());

                // 获取文档标题
                List<Long> docIds = results.stream()
                        .map(r -> {
                            Object docIdObj = r.getMetadata().get("document_id");
                            return docIdObj instanceof Number ? ((Number) docIdObj).longValue() : null;
                        })
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());

                Map<Long, String> docTitles = documentRepository.findAllById(docIds).stream()
                        .collect(Collectors.toMap(DocumentPo::getId, DocumentPo::getFilename));

                // 转换为 ReferenceDocument
                List<ReferenceDocument> documents = results.stream()
                        .map(r -> {
                            Long docId = null;
                            Object docIdObj = r.getMetadata().get("document_id");
                            if (docIdObj instanceof Number) {
                                docId = ((Number) docIdObj).longValue();
                            }

                            return ReferenceDocument.builder()
                                    .title(docId != null ? docTitles.getOrDefault(docId, "Unknown Document") : "Unknown Document")
                                    .score(r.getScore())
                                    .content(r.getContent())
                                    .documentId(docId)
                                    .chunkId(r.getSourceId())
                                    .build();
                        })
                        .collect(Collectors.toList());

                // 检索完成
                String message = String.format("检索完成，找到 %d 个相关片段", documents.size());
                SseEventBuilder.sendThoughtEvent(emitter, "success", message, "check");

                // 发送参考文档
                if (!documents.isEmpty()) {
                    SseEventBuilder.sendRefEvent(emitter, documents);
                }

                // 构建 Context
                String context = IntStream.range(0, documents.size())
                        .mapToObj(i -> {
                            ReferenceDocument doc = documents.get(i);
                            int docId = i + 1;
                            String safeContent = doc.getContent().replace("<", "&lt;").replace(">", "&gt;");
                            return String.format("""
                                            <doc id="%d" title="%s">
                                            %s
                                            </doc>""",
                                    docId,
                                    doc.getTitle(),
                                    safeContent
                            );
                        })
                        .collect(Collectors.joining("\n"));

                // 替换 Prompt 变量
                String prompt = AppConstants.PromptTemplates.RAG_ANSWER
                        .replace("{query}", question)
                        .replace("{context}", context);

                // 大模型回答
                llmClient.streamChatWithEmitter(
                    prompt,
                    emitter,
                    buildPersistenceHandler(userId, kbId, sessionId, request, documents)
                );

            } catch (Exception e) {
                handleStreamException(emitter, "HisemRAG Fast 对话失败", e);
            }
        });

        return emitter;
    }

    /**
     * HisemRAG 完整版对话（含自适应层级检索 + SADP 多跳规划）
     *
     * @param request 对话请求
     * @return SSE 事件流
     */
    @Override
    public SseEmitter hisemRagChat(Long userId, HisemRagChatRequest request) {
        log.info("收到 HisemRAG 对话请求: userId={}, kbId={}, question={}", userId, request.getKbId(), request.getQuestion());

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        Long kbId = request.getKbId();
        validateKnowledgeBaseAccess(kbId, userId);
        String sessionId = conversationService.resolveSessionId(request.getSessionId());
        request.setSessionId(sessionId);
        String historyText = conversationService.buildHistoryText(kbId, userId, sessionId, request.getHistoryWindow());

        CompletableFuture.runAsync(() -> {
            try {
                String question = request.getQuestion();

                // 0. 文件类型校验：HiSem-SADP 仅支持 Markdown 文件
                // 注意：浏览器上传 .md 文件时 MIME 类型可能为 application/octet-stream，
                // 因此使用文件名后缀（而非 MIME 类型）来判断是否为 Markdown 文件。
                List<DocumentPo> docs = documentRepository.findByKbId(kbId);
                boolean hasNonMarkdown = docs.stream()
                        .anyMatch(d -> {
                            String name = d.getFilename();
                            return name == null || !name.toLowerCase().endsWith(".md");
                        });
                if (hasNonMarkdown) {
                    log.warn("HiSem-SADP 文件类型校验失败: kbId={} 包含非 Markdown 文件", kbId);
                    SseEventBuilder.sendThoughtEvent(emitter, "error",
                            "HiSem-SADP 方法仅支持 Markdown (.md) 格式的文档，当前知识库包含不支持的文件类型，请改用普通 RAG 方法。",
                            "error");
                    emitter.complete();
                    return;
                }

                LLMClient llmClient = modelFactory.createLLMClient(request.getLlmModelId());
                EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(request.getEmbeddingModelId());

                // Token 用量账本：追踪本次请求所有 LLM 调用的 token 消耗
                TokenUsageLedger ledger = new TokenUsageLedger();
                StreamEventHandler tokenHandler = new StreamEventHandler.NoOpHandler() {
                    @Override
                    public void onTokenUsage(SseEmitter em, int input, int output, int total, long durationMs) {
                        ledger.record("综合生成", input, output, total, durationMs);
                        SseEventBuilder.sendTokenUsageEvent(em, ledger);
                    }
                };

                // 1. 意图识别（是否需要检索）
                if (Boolean.TRUE.equals(request.getEnableIntentRecognition())) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在分析查询意图...", "search");
                    boolean needRetrieval = ragQueryProcessor.analyzeIntent(question, historyText);
                    if (!needRetrieval) {
                        log.info("意图识别结果: 不需要检索，直接聊天");
                        llmClient.streamChatWithEmitter(
                                question,
                                emitter,
                                wrapHandlers(tokenHandler, buildPersistenceHandler(userId, kbId, sessionId, request, Collections.emptyList()))
                        );
                        return;
                    }
                    log.info("意图识别结果: 需要检索");
                }

                // 2. SADP 意图路由（4 类）
                SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在分析问题类型...", "edit");
                String intent = sadpPlanner.routeIntent(question, llmClient, ledger);
                log.info("SADP 意图路由结果: {}", intent);

                // maxTopK：限制最终传递给大模型的上下文片段数量（仅标准检索分支使用）
                int maxTopK = request.getMaxTopK() != null ? request.getMaxTopK() : 10;

                if ("简单事实".equals(intent)) {
                    // ======== 标准自适应检索分支 ========
                    Set<String> searchQueries = Collections.singleton(question);
                    log.debug("HiSem standard retrieval: kbId={}", kbId);
                    // retrieve() 内部发送开始 thought 事件
                    AdaptiveRetriever.RetrievalBundle bundle = adaptiveRetriever.retrieve(
                            searchQueries, kbId, embeddingClient, emitter);
                    // 仅保留叶子节点作为参考文献（非叶子节点为中间检索层，不作为参考依据）
                    List<RetrievalResult> results = bundle.results().stream()
                            .filter(r -> r.getMetadata() != null
                                    && "LEAF".equals(r.getMetadata().get("node_type")))
                            .collect(Collectors.toList());
                    log.debug("HiSem retrieval complete: {} leaf results (from {} total)",
                            results.size(), bundle.results().size());

                    // 按 maxTopK 截断结果（限制传递给大模型的上下文片段数量）
                    if (results.size() > maxTopK) {
                        log.info("截断检索结果: {} → {}", results.size(), maxTopK);
                        results = results.subList(0, maxTopK);
                    }

                    // 发送完成 thought：此时 results 已过滤+截断，数量与参考文献一致
                    SseEventBuilder.sendThoughtEvent(emitter, "success",
                            String.format("检索完成，共命中 %d 个相关片段", results.size()), "check");

                    // 发送检索路径树：剪枝到仅包含最终参考文献节点的路径
                    // 注意：sourceId 是 Milvus embeddingId，与树节点的 node_id 不同，
                    // 必须从 metadata.node_id 取值才能与树节点匹配
                    Set<String> finalNodeIds = results.stream()
                            .filter(r -> r.getMetadata() != null
                                    && r.getMetadata().get("node_id") != null)
                            .map(r -> String.valueOf(r.getMetadata().get("node_id")))
                            .collect(Collectors.toSet());
                    List<RetrievalTreeNode> prunedTree = pruneTreeByNodeIds(
                            bundle.treeRoots(), finalNodeIds);
                    SseEventBuilder.sendRetrievalTreeEvent(emitter, prunedTree);

                    // 获取文档标题
                    List<Long> docIds = results.stream()
                            .map(r -> {
                                Object docIdObj = r.getMetadata().get("document_id");
                                return docIdObj instanceof Number ? ((Number) docIdObj).longValue() : null;
                            })
                            .filter(java.util.Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());

                    Map<Long, String> docTitles = documentRepository.findAllById(docIds).stream()
                            .collect(Collectors.toMap(DocumentPo::getId, DocumentPo::getFilename));

                    // 转换为 ReferenceDocument
                    List<ReferenceDocument> documents = results.stream()
                            .map(r -> {
                                Object docIdObj = r.getMetadata().get("document_id");
                                Long docId = docIdObj instanceof Number
                                        ? ((Number) docIdObj).longValue() : null;
                                return ReferenceDocument.builder()
                                        .title(docId != null
                                                ? docTitles.getOrDefault(docId, "Unknown Document")
                                                : "Unknown Document")
                                        .score(r.getScore())
                                        .content(r.getContent())
                                        .documentId(docId)
                                        .chunkId(r.getSourceId())
                                        .build();
                            })
                            .collect(Collectors.toList());

                    // 发送参考文档
                    if (!documents.isEmpty()) {
                        SseEventBuilder.sendRefEvent(emitter, documents);
                    }

                    // 构建 Context
                    String context = IntStream.range(0, documents.size())
                            .mapToObj(i -> {
                                ReferenceDocument doc = documents.get(i);
                                String safeContent = doc.getContent()
                                        .replace("<", "&lt;").replace(">", "&gt;");
                                return String.format("""
                                        <doc id="%d" title="%s">
                                        %s
                                        </doc>""", i + 1, doc.getTitle(), safeContent);
                            })
                            .collect(Collectors.joining("\n"));

                    String prompt = AppConstants.PromptTemplates.RAG_ANSWER
                            .replace("{query}", question)
                            .replace("{context}", context);

                        llmClient.streamChatWithEmitter(
                            prompt,
                            emitter,
                            wrapHandlers(tokenHandler, buildPersistenceHandler(userId, kbId, sessionId, request, documents))
                        );

                } else {
                    // ======== SADP 多跳分支（多跳推理 / 对比分析 / 宏观总结） ========
                    log.debug("HiSem SADP branch: intent='{}' for kbId={}", intent, kbId);
                    String intentMsg = String.format("检测到「%s」类型问题，正在构建任务规划...", intent);
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", intentMsg, "edit");

                    // 获取文档骨架（tree_nodes 前两级标题 + node_id）
                    String skeleton = sadpPlanner.buildSkeleton(kbId);

                    // DAG 任务规划（含算子类型和 node_id 范围约束）
                    var tasks = sadpPlanner.planDag(question, skeleton, llmClient, ledger);
                    SseEventBuilder.sendThoughtEvent(emitter, "processing",
                            String.format("任务规划完成，共 %d 个子任务", tasks.size()), "check");

                    // DAG 并行执行，返回最后一个 Generate 算子的输出
                    String finalAnswer = sadpPlanner.executeDag(
                            tasks, question, kbId, emitter, llmClient, embeddingClient, ledger);

                    SseEventBuilder.sendThoughtEvent(emitter, "success", "综合推理完成", "check");

                    // 流式输出最终答案
                        llmClient.streamChatWithEmitter(
                            finalAnswer,
                            emitter,
                            wrapHandlers(tokenHandler, buildPersistenceHandler(userId, kbId, sessionId, request, Collections.emptyList()))
                        );
                }

            } catch (Exception e) {
                handleStreamException(emitter, "HisemRAG 对话失败", e);
            }
        });

        return emitter;
    }

    private StreamEventHandler wrapHandlers(StreamEventHandler... handlers) {
        return new StreamEventHandler.NoOpHandler() {
            @Override
            public void onAfterChat(SseEmitter emitter) {
                for (StreamEventHandler handler : handlers) {
                    handler.onAfterChat(emitter);
                }
            }

            @Override
            public void onTokenUsage(SseEmitter emitter, int inputTokens, int outputTokens, int totalTokens, long durationMs) {
                for (StreamEventHandler handler : handlers) {
                    handler.onTokenUsage(emitter, inputTokens, outputTokens, totalTokens, durationMs);
                }
            }

            @Override
            public void onPartialResponse(SseEmitter emitter, String partialResponse) {
                for (StreamEventHandler handler : handlers) {
                    handler.onPartialResponse(emitter, partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(SseEmitter emitter, String fullResponse) {
                for (StreamEventHandler handler : handlers) {
                    handler.onCompleteResponse(emitter, fullResponse);
                }
            }
        };
    }

    private StreamEventHandler buildPersistenceHandler(
            Long userId,
            Long kbId,
            String sessionId,
            RagChatRequest request,
            List<ReferenceDocument> documents) {
        return new StreamEventHandler.NoOpHandler() {
            private String fullResponse = "";

            @Override
            public void onCompleteResponse(SseEmitter emitter, String response) {
                this.fullResponse = response == null ? "" : response;
            }

            @Override
            public void onAfterChat(SseEmitter emitter) {
                saveConversationRecord(userId, kbId, sessionId, request, fullResponse, documents);
            }
        };
    }

    private void saveConversationRecord(
            Long userId,
            Long kbId,
            String sessionId,
            RagChatRequest request,
            String response,
            List<ReferenceDocument> documents) {
        try {
            Conversation conversation = Conversation.builder()
                    .kbId(kbId)
                    .userId(userId)
                    .sessionId(sessionId)
                    .query(request.getQuestion())
                    .response(response)
                    .retrievalConfig(toJsonSafe(request))
                    .llmModelId(request.getLlmModelId())
                    .rerankModelId(request.getRerankModelId())
                    .retrievedChunks(toRetrievedChunksJson(documents))
                    .createdAt(LocalDateTime.now())
                    .build();

            conversationService.saveConversation(conversation);
        } catch (Exception e) {
            log.warn("保存会话记录失败: kbId={}, userId={}, sessionId={}, error={}",
                    kbId, userId, sessionId, e.getMessage());
        }
    }

    private String toRetrievedChunksJson(List<ReferenceDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return null;
        }

        List<Map<String, Object>> chunkMeta = documents.stream()
                .map(doc -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("documentId", doc.getDocumentId());
                    item.put("chunkId", doc.getChunkId());
                    item.put("title", doc.getTitle());
                    item.put("score", doc.getScore());
                    return item;
                })
                .collect(Collectors.toList());

        return toJsonSafe(chunkMeta);
    }

    private String toJsonSafe(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.debug("对象序列化失败: {}", e.getMessage());
            return null;
        }
    }

    @NotNull
    private static String getCollectionName(Long kbId) {
        return MILVUS_CHUNKS_COLLECTION_TEMPLATE.formatted(kbId);
    }

    private void validateKnowledgeBaseAccess(Long kbId, Long userId) {
        var kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new ResourceNotFoundException("KnowledgeBase", kbId));

        if (!Objects.equals(kb.getUserId(), userId)) {
            throw new UnauthorizedAccessException(userId, "KnowledgeBase", kbId);
        }
    }

    /**
     * 统一发送流式错误事件并结束连接。
     */
    private void handleStreamException(SseEmitter emitter, String scene, Throwable error) {
        Throwable root = RagStreamErrorMapper.unwrap(error);
        String rootMessage = root == null ? "unknown" : root.getMessage();
        log.error("{}: {}", scene, rootMessage, error);
        SseEventBuilder.sendErrorEvent(emitter, RagStreamErrorMapper.toUserMessage(error));
        emitter.complete();
    }

    /**
     * 按最终参考文献的 node_id 集合剪枝检索树。
     *
     * <p>只保留 nodeId 在 nodeIds 中的叶节点及其全部祖先路径，
     * 其余分支整体移除。同时将 inResults 标志按最终结果集重新标记。
     *
     * @param nodes   待剪枝的节点列表
     * @param nodeIds 最终参考文献的 node_id 集合（来自 metadata.node_id）
     * @return 剪枝后的节点列表
     */
    private static List<RetrievalTreeNode> pruneTreeByNodeIds(
            List<RetrievalTreeNode> nodes, Set<String> nodeIds) {
        List<RetrievalTreeNode> result = new ArrayList<>();
        for (RetrievalTreeNode node : nodes) {
            boolean inFinal = nodeIds.contains(node.getNodeId());
            List<RetrievalTreeNode> prunedChildren =
                    pruneTreeByNodeIds(node.getChildren(), nodeIds);
            if (inFinal || !prunedChildren.isEmpty()) {
                result.add(RetrievalTreeNode.builder()
                        .nodeId(node.getNodeId())
                        .titlePath(node.getTitlePath())
                        .score(node.getScore())
                        .passedThreshold(node.isPassedThreshold())
                        .inResults(inFinal)
                        .children(prunedChildren)
                        .build());
            }
        }
        return result;
    }
}