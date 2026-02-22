package com.mtmn.smartdoc.service.impl;

import com.mtmn.smartdoc.constants.AppConstants;
import com.mtmn.smartdoc.dto.HisemRagChatRequest;
import com.mtmn.smartdoc.dto.NaiveRagChatRequest;
import com.mtmn.smartdoc.exception.MilvusConnectionException;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.model.client.SseEventBuilder;
import com.mtmn.smartdoc.model.dto.ReferenceDocument;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.rag.retriever.AdaptiveRetriever;
import com.mtmn.smartdoc.rag.sadp.SadpPlanner;
import com.mtmn.smartdoc.rag.sadp.TaskNode;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.repository.KnowledgeBaseRepository;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private final RAGQueryProcessor ragQueryProcessor;
    private final AdaptiveRetriever adaptiveRetriever;
    private final SadpPlanner sadpPlanner;

    /**
     * @param request
     * @return
     */
    @Override
    // TODO 实现历史对话和对话存储功能
    public SseEmitter naiveRagChat(NaiveRagChatRequest request) {
        log.info("收到 Naive RAG 对话请求: kbId={}, question={}", request.getKbId(), request.getQuestion());

        // 创建 SseEmitter，设置超时时间 (例如 5 分钟)
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // 异步执行，避免阻塞 Tomcat 线程
        CompletableFuture.runAsync(() -> {
            try {
                Long kbId = request.getKbId();
                String question = request.getQuestion();
                LLMClient llmClient = modelFactory.createLLMClient(request.getLlmModelId());
                EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(request.getEmbeddingModelId());

                // 1. 意图识别
                if (request.getEnableIntentRecognition()) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在分析查询意图...", "search");
                    boolean needRetrieval = ragQueryProcessor.analyzeIntent(question);
                    if (!needRetrieval) {
                        log.info("意图识别结果: 不需要检索");
                        // 直接聊天
                        llmClient.streamChatWithEmitter(question, emitter);
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
                    String rewritten = ragQueryProcessor.rewriteQuery(question);
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
                llmClient.streamChatWithEmitter(prompt, emitter);

            } catch (MilvusConnectionException e) {
                log.error("向量数据库连接失败: {}", e.getMessage());
                String errorMsg = "向量数据库服务暂时不可用，请稍后重试。(" + e.getMessage() + ")";
                try {
                    SseEventBuilder.sendThoughtEvent(emitter, "error", errorMsg, "error");
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("流式聊天启动失败: {}", e.getMessage(), e);
                try {
                    String userMsg = isRateLimitError(e)
                            ? "AI 服务请求频率超限，请稍后再试。"
                            : "服务异常，请稍后重试。";
                    SseEventBuilder.sendThoughtEvent(emitter, "error", userMsg, "error");
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
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

        List<CompletableFuture<List<RetrievalResult>>> futures = queries.stream()
                .map(query -> CompletableFuture.supplyAsync(() -> {
                    try {
                        Embedding queryVector = embeddingClient.embed(query);
                        return milvusService.search(kbId, queryVector, topK, threshold);
                    } catch (Exception e) {
                        log.error("检索失败: query={}, error={}", query, e.getMessage());
                        return new ArrayList<RetrievalResult>();
                    }
                }))
                .toList();

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 合并结果并去重
        Map<String, RetrievalResult> uniqueResults = new HashMap<>();
        for (CompletableFuture<List<RetrievalResult>> future : futures) {
            try {
                List<RetrievalResult> results = future.get();
                for (RetrievalResult result : results) {
                    // 使用 sourceId (chunkId) 作为去重键
                    // 如果同一个 chunk 被多次检索到，保留分数最高的那个（通常 Milvus 返回的就是最高分，这里简单覆盖即可，或者比较 score）
                    String key = result.getSourceId();
                    if (!uniqueResults.containsKey(key) || result.getScore() > uniqueResults.get(key).getScore()) {
                        uniqueResults.put(key, result);
                    }
                }
            } catch (Exception e) {
                log.error("获取检索结果失败", e);
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
    public SseEmitter hisemRagFastChat(HisemRagChatRequest request) {
        log.info("收到 HisemRAG Fast 对话请求: kbId={}, question={}", request.getKbId(), request.getQuestion());

        // 创建 SseEmitter，设置超时时间 (5 分钟)
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        // 异步执行，避免阻塞 Tomcat 线程
        CompletableFuture.runAsync(() -> {
            try {
                Long kbId = request.getKbId();
                String question = request.getQuestion();
                LLMClient llmClient = modelFactory.createLLMClient(request.getLlmModelId());
                EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(request.getEmbeddingModelId());

                // 1. 意图识别
                if (Boolean.TRUE.equals(request.getEnableIntentRecognition())) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在分析查询意图...", "search");
                    boolean needRetrieval = ragQueryProcessor.analyzeIntent(question);
                    if (!needRetrieval) {
                        log.info("意图识别结果: 不需要检索");
                        llmClient.streamChatWithEmitter(question, emitter);
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
                    String rewritten = ragQueryProcessor.rewriteQuery(question);
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
                llmClient.streamChatWithEmitter(prompt, emitter);

            } catch (MilvusConnectionException e) {
                log.error("向量数据库连接失败: {}", e.getMessage());
                String errorMsg = "向量数据库服务暂时不可用，请稍后重试。(" + e.getMessage() + ")";
                try {
                    SseEventBuilder.sendThoughtEvent(emitter, "error", errorMsg, "error");
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("HisemRAG Fast 对话失败: {}", e.getMessage(), e);
                try {
                    String userMsg = isRateLimitError(e)
                            ? "AI 服务请求频率超限，请稍后再试。"
                            : "服务异常，请稍后重试。";
                    SseEventBuilder.sendThoughtEvent(emitter, "error", userMsg, "error");
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
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
    public SseEmitter hisemRagChat(HisemRagChatRequest request) {
        log.info("收到 HisemRAG 对话请求: kbId={}, question={}", request.getKbId(), request.getQuestion());

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        CompletableFuture.runAsync(() -> {
            try {
                Long kbId = request.getKbId();
                String question = request.getQuestion();

                // 0. 文件类型校验：HiSem-SADP 仅支持 Markdown 文件
                List<DocumentPo> docs = documentRepository.findByKbId(kbId);
                boolean hasNonMarkdown = docs.stream()
                        .anyMatch(d -> d.getFileType() == null
                                || !AppConstants.FileTypes.MD.equals(d.getFileType()));
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

                // 1. 意图识别
                if (Boolean.TRUE.equals(request.getEnableIntentRecognition())) {
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在分析查询意图...", "search");
                    boolean needRetrieval = ragQueryProcessor.analyzeIntent(question);
                    if (!needRetrieval) {
                        log.info("意图识别结果: 不需要检索，直接聊天");
                        llmClient.streamChatWithEmitter(question, emitter);
                        return;
                    }
                    log.info("意图识别结果: 需要检索");
                }

                // 收集检索查询语句
                Set<String> searchQueries = Collections.singleton(question);

                // 2. SADP 复杂度判断
                SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在判断问题复杂度...", "edit");
                boolean isComplex = sadpPlanner.isComplexQuery(question, llmClient);
                log.info("SADP 复杂度判断: isComplex={}", isComplex);

                // maxTopK：限制最终传递给大模型的上下文片段数量
                int maxTopK = request.getMaxTopK() != null ? request.getMaxTopK() : 10;

                if (isComplex) {
                    // ======== SADP 多跳分支 ========
                    log.debug("HiSem SADP branch: complex query detected for kbId={}", kbId);
                    SseEventBuilder.sendThoughtEvent(emitter, "processing",
                            "检测到复杂多跳问题，正在规划任务...", "edit");

                    List<TaskNode> tasks = sadpPlanner.decomposeToDag(question, llmClient);
                    String taskCountMsg = String.format("任务拆解完成，共 %d 个子任务", tasks.size());
                    SseEventBuilder.sendThoughtEvent(emitter, "processing", taskCountMsg, "check");

                    // 执行 DAG（内部每个子任务完成时推送 thought 事件）
                    String finalAnswer = sadpPlanner.executeDag(
                            tasks, question, kbId, emitter, llmClient, embeddingClient);

                    SseEventBuilder.sendThoughtEvent(emitter, "success", "综合推理完成", "check");

                    // 流式发送最终答案（分片逐字推送）
                    llmClient.streamChatWithEmitter(
                            AppConstants.PromptTemplates.SADP_FINAL_SYNTHESIS
                                    .replace("{query}", question)
                                    .replace("{subtask_results}", finalAnswer),
                            emitter);

                } else {
                    // ======== 标准自适应检索分支 ========
                    log.debug("HiSem standard retrieval: {} queries for kbId={}", searchQueries.size(), kbId);
                    List<RetrievalResult> results = adaptiveRetriever.retrieve(
                            searchQueries, kbId, embeddingClient, emitter);
                    log.debug("HiSem retrieval complete: {} results", results.size());

                    // 按 maxTopK 截断结果（限制传递给大模型的上下文片段数量）
                    if (results.size() > maxTopK) {
                        log.info("截断检索结果: {} → {}", results.size(), maxTopK);
                        results = results.subList(0, maxTopK);
                    }

                    String hitMsg = String.format("检索完成，共命中 %d 个相关片段", results.size());
                    SseEventBuilder.sendThoughtEvent(emitter, "success", hitMsg, "check");

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

                    llmClient.streamChatWithEmitter(prompt, emitter);
                }

            } catch (MilvusConnectionException e) {
                log.error("向量数据库连接失败: {}", e.getMessage());
                try {
                    SseEventBuilder.sendThoughtEvent(emitter, "error",
                            "向量数据库服务暂时不可用，请稍后重试。(" + e.getMessage() + ")", "error");
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            } catch (Exception e) {
                log.error("HisemRAG 对话失败: {}", e.getMessage(), e);
                try {
                    String userMsg = isRateLimitError(e)
                            ? "AI 服务请求频率超限，请稍后再试。"
                            : "服务异常，请稍后重试。";
                    SseEventBuilder.sendThoughtEvent(emitter, "error", userMsg, "error");
                } catch (Exception ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @NotNull
    private static String getCollectionName(Long kbId) {
        return MILVUS_CHUNKS_COLLECTION_TEMPLATE.formatted(kbId);
    }

    /**
     * 判断异常是否为速率限制（429）
     */
    private static boolean isRateLimitError(Throwable e) {
        if (e == null) return false;
        String msg = e.getMessage();
        if (msg == null) return false;
        return msg.contains("429")
                || msg.toLowerCase().contains("rate limit")
                || msg.contains("速率限制")
                || msg.contains("请求频率");
    }
}