package com.mtmn.smartdoc.rag.sadp;

import com.mtmn.smartdoc.constants.AppConstants;
import com.mtmn.smartdoc.enums.DocumentIndexStatus;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.model.client.SseEventBuilder;
import com.mtmn.smartdoc.model.client.TokenUsageLedger;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.po.TreeNode;
import com.mtmn.smartdoc.rag.retriever.AdaptiveRetriever;
import com.mtmn.smartdoc.repository.DocumentRepository;
import com.mtmn.smartdoc.repository.TreeNodeRepository;
import com.mtmn.smartdoc.utils.LlmJsonUtils;
import com.mtmn.smartdoc.vo.RetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * SADP（Structure-Aware Dynamic Planning）规划与执行引擎
 *
 * <p>实现严格的 5 步 SADP 流程：
 * <ol>
 *   <li>意图路由（4 类：简单事实 / 多跳推理 / 对比分析 / 宏观总结）</li>
 *   <li>文档骨架获取（从 tree_nodes 表读取前两级标题，按字数降级）</li>
 *   <li>DAG 任务规划（结合骨架约束，生成含算子类型的任务图）</li>
 *   <li>并行执行（Scoped_Retrieve / Get_Summary / Generate 三种算子）</li>
 *   <li>最终输出（最后一个 Generate 任务的结果）</li>
 * </ol>
 *
 * @author charmingdaidai
 * @version 2.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SadpPlanner {

    private final AdaptiveRetriever adaptiveRetriever;
    private final DocumentRepository documentRepository;
    private final TreeNodeRepository treeNodeRepository;

    /** 2 层骨架字数上限，超出则降级为 1 层 */
    private static final int SKELETON_2LEVEL_MAX_CHARS = 3000;
    /** 1 层骨架字数上限，超出则完全不提供骨架 */
    private static final int SKELETON_1LEVEL_MAX_CHARS = 1500;

    // =====================================================================
    // 公开方法
    // =====================================================================

    /**
     * 意图路由：将用户查询分类为 4 种意图之一
     *
     * @param query     用户原始问题
     * @param llmClient LLM 客户端
     * @param ledger    Token 用量账本，null 表示不追踪
     * @return "简单事实" | "多跳推理" | "对比分析" | "宏观总结"
     */
    public String routeIntent(String query, LLMClient llmClient, TokenUsageLedger ledger) {
        try {
            log.debug("SADP intent routing for query: '{}'", query);
            String prompt = AppConstants.PromptTemplates.SADP_INTENT_ROUTING
                    .replace("{query}", query);
            String llmOutput = llmClient.chat(prompt, "意图路由", ledger);
            log.debug("SADP intent routing LLM response: {}", llmOutput);

            Map<String, Object> result = LlmJsonUtils.parseMap(llmOutput);
            String intent = String.valueOf(result.getOrDefault("intent", "简单事实"));
            log.info("SADP 意图路由: intent={}, reason={}", intent, result.get("reason"));
            return intent;
        } catch (Exception e) {
            log.warn("SADP intent routing failed: {}, defaulting to 简单事实", e.getMessage());
            return "简单事实";
        }
    }

    /**
     * 构建文档骨架：从 tree_nodes 表读取前两级标题，拼装为树状文本
     *
     * <p>按字数自动降级：
     * <ul>
     *   <li>2 层骨架 ≤ 3000 字 → 返回 2 层</li>
     *   <li>1 层骨架 ≤ 1500 字 → 返回 1 层</li>
     *   <li>超出 → 返回空串（骨架过大，退化为无约束规划）</li>
     * </ul>
     *
     * @param kbId 知识库 ID
     * @return 骨架文本（供 DAG 规划提示词使用）
     */
    public String buildSkeleton(Long kbId) {
        List<DocumentPo> indexedDocuments = documentRepository
            .findByKbIdAndIndexStatus(kbId, DocumentIndexStatus.INDEXED);
        if (indexedDocuments.isEmpty()) {
            log.warn("SADP skeleton: no indexed documents for kbId={}, returning empty", kbId);
            return "";
        }

        Set<Long> activeDocIds = indexedDocuments.stream()
            .map(DocumentPo::getId)
            .collect(Collectors.toSet());

        List<TreeNode> activeNodes = treeNodeRepository.findByKbId(kbId).stream()
            .filter(n -> n.getDocumentId() != null && activeDocIds.contains(n.getDocumentId()))
            .filter(n -> n.getLevel() != null && n.getLevel() > 0)
            .sorted(Comparator.comparing(TreeNode::getTitlePath, Comparator.nullsLast(String::compareTo)))
            .toList();

        if (activeNodes.isEmpty()) {
            log.warn("SADP skeleton: no active tree nodes for kbId={}, returning empty", kbId);
            return "";
        }

        int rootLevel = activeNodes.stream()
            .map(TreeNode::getLevel)
            .filter(Objects::nonNull)
            .min(Integer::compareTo)
            .orElse(1);

        List<TreeNode> level1 = activeNodes.stream()
            .filter(n -> n.getLevel() == rootLevel)
            .toList();
        List<TreeNode> level2 = activeNodes.stream()
            .filter(n -> n.getLevel() == rootLevel + 1)
            .toList();

        // 建立 parentNodeId → children 映射
        Map<String, List<TreeNode>> childrenMap = level2.stream()
                .filter(n -> n.getParentNodeId() != null)
                .collect(Collectors.groupingBy(TreeNode::getParentNodeId));

        // 尝试 2 层骨架
        StringBuilder sb2 = new StringBuilder();
        for (TreeNode l1 : level1) {
            sb2.append("- [").append(l1.getNodeId()).append("] ")
               .append(l1.getTitlePath()).append("\n");
            List<TreeNode> children = childrenMap.getOrDefault(l1.getNodeId(), List.of());
            for (TreeNode l2 : children) {
                sb2.append("  - [").append(l2.getNodeId()).append("] ")
                   .append(l2.getTitlePath()).append("\n");
            }
        }
        if (sb2.length() <= SKELETON_2LEVEL_MAX_CHARS) {
            log.debug("SADP skeleton 2-level (rootLevel={}, {} L1, {} L2, {} chars)",
                rootLevel, level1.size(), level2.size(), sb2.length());
            return sb2.toString();
        }

        // 退化到 1 层骨架
        StringBuilder sb1 = new StringBuilder();
        for (TreeNode l1 : level1) {
            sb1.append("- [").append(l1.getNodeId()).append("] ")
               .append(l1.getTitlePath()).append("\n");
        }
        if (sb1.length() <= SKELETON_1LEVEL_MAX_CHARS) {
            log.info("SADP skeleton: 2-level too long ({}), using 1-level at rootLevel={} ({} chars)",
                sb2.length(), rootLevel, sb1.length());
            return sb1.toString();
        }

        // 完全退化（骨架过大，不提供约束）
        log.warn("SADP skeleton: 1-level also too long ({}), degrading to empty", sb1.length());
        return "";
    }

    /**
     * DAG 任务规划：结合文档骨架，让大模型生成含算子类型和 node_id 约束的任务图
     *
     * @param query     用户原始问题
     * @param skeleton  文档骨架文本
     * @param llmClient LLM 客户端
     * @param ledger    Token 用量账本，null 表示不追踪
     * @return 任务节点列表（拓扑顺序）
     */
    public List<TaskNode> planDag(String query, String skeleton, LLMClient llmClient, TokenUsageLedger ledger) {
        try {
            log.debug("SADP DAG planning for query: '{}'", query);
            String prompt = AppConstants.PromptTemplates.SADP_DAG_PLANNING
                    .replace("{query}", query)
                    .replace("{skeleton}", skeleton);
            String llmOutput = llmClient.chat(prompt, "任务规划", ledger);
            log.debug("SADP DAG planning LLM response: {}", llmOutput);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawTasks = (List<Map<String, Object>>) (List<?>)
                    LlmJsonUtils.parseList(llmOutput, LinkedHashMap.class);
            if (rawTasks == null || rawTasks.isEmpty()) {
                log.warn("SADP DAG planning returned empty task list, using fallback");
                return buildFallbackTasks(query);
            }

            List<TaskNode> tasks = new ArrayList<>();
            for (Map<String, Object> raw : rawTasks) {
                TaskNode node = new TaskNode();
                node.setId(String.valueOf(raw.getOrDefault("id", "T" + (tasks.size() + 1))));
                node.setType(parseTaskType(String.valueOf(raw.getOrDefault("type", "Scoped_Retrieve"))));
                node.setQuery(String.valueOf(raw.getOrDefault("query", "")));
                node.setNodeId(String.valueOf(raw.getOrDefault("node_id", "")));
                node.setDependsOn(parseDeps(raw.get("dependsOn")));
                tasks.add(node);
            }

            log.info("SADP DAG planning: {} tasks generated", tasks.size());
            if (log.isDebugEnabled()) {
                tasks.forEach(t -> log.debug("  Task[{}] type={} query='{}' nodeId='{}' deps={}",
                        t.getId(), t.getType(), t.getQuery(), t.getNodeId(), t.getDependsOn()));
            }
            return tasks;

        } catch (Exception e) {
            log.error("SADP DAG planning failed: {}, using fallback", e.getMessage());
            return buildFallbackTasks(query);
        }
    }

    /**
     * 执行 DAG 任务图，返回最后一个 Generate 算子的输出作为最终答案
     *
     * @param tasks           DAG 任务列表
     * @param originalQuery   用户原始问题（仅用于日志）
     * @param kbId            知识库 ID
     * @param emitter         SSE 推送器
     * @param llmClient       LLM 客户端
     * @param embeddingClient 向量化客户端
     * @param ledger          Token 用量账本，null 表示不追踪
     * @return 最终答案文本
     */
    public String executeDag(List<TaskNode> tasks, String originalQuery, Long kbId,
                              SseEmitter emitter, LLMClient llmClient, EmbeddingClient embeddingClient,
                              TokenUsageLedger ledger, String historyText, int perScopedTopK) {
        int totalTasks = tasks.size();
        int scopedTopK = perScopedTopK > 0 ? perScopedTopK : 10;

        // 标记最终 Generate 任务：执行时直接返回 prompt，由 RAGServiceImpl 发起流式调用
        tasks.stream()
                .filter(t -> t.getType() == TaskNode.TaskType.Generate)
                .reduce((a, b) -> b)
                .ifPresent(t -> t.setTerminalGenerate(true));

        Map<String, Integer> taskIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < tasks.size(); i++) {
            taskIndexMap.put(tasks.get(i).getId(), i + 1);
        }

        Map<String, CompletableFuture<String>> futures = new LinkedHashMap<>();

        // 按拓扑顺序调度任务
        for (TaskNode task : tasks) {
            List<CompletableFuture<String>> depFutures = task.getDependsOn().stream()
                    .map(depId -> futures.getOrDefault(depId, CompletableFuture.completedFuture("")))
                    .collect(Collectors.toList());

            CompletableFuture<String> taskFuture;

            if (depFutures.isEmpty()) {
                // 无依赖：直接并行执行
                taskFuture = CompletableFuture.supplyAsync(() ->
                        executeSubTask(task, Collections.emptyMap(), kbId,
                    emitter, llmClient, embeddingClient, taskIndexMap, totalTasks, ledger, historyText, scopedTopK));
            } else {
                // 有依赖：等待所有前置任务完成后执行
                CompletableFuture<Void> allDeps = CompletableFuture.allOf(
                        depFutures.toArray(CompletableFuture[]::new));

                taskFuture = allDeps.thenApplyAsync(ignored -> {
                    Map<String, String> priorResults = new LinkedHashMap<>();
                    for (String depId : task.getDependsOn()) {
                        CompletableFuture<String> depFuture = futures.get(depId);
                        if (depFuture != null) {
                            try {
                                priorResults.put(depId, depFuture.join());
                            } catch (Exception e) {
                                priorResults.put(depId, "（子任务执行失败）");
                            }
                        }
                    }
                    return executeSubTask(task, priorResults, kbId,
                            emitter, llmClient, embeddingClient, taskIndexMap, totalTasks, ledger, historyText, scopedTopK);
                });
            }

            futures.put(task.getId(), taskFuture);
        }

        // 等待所有任务完成，收集结果
        CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new)).join();

        for (TaskNode task : tasks) {
            CompletableFuture<String> future = futures.get(task.getId());
            if (future != null) {
                try {
                    task.setResult(future.join());
                    task.setStatus(TaskNode.TaskStatus.DONE);
                } catch (Exception e) {
                    task.setResult("（子任务执行失败：" + e.getMessage() + "）");
                    task.setStatus(TaskNode.TaskStatus.FAILED);
                }
            }
        }

        // 收集所有 Scoped_Retrieve 任务的原始检索结果，发送参考文档事件
        // 仅保留叶子节点，按 sourceId 去重（取分数最高的）
        Map<String, RetrievalResult> dedupMap = new LinkedHashMap<>();
        tasks.stream()
                .filter(t -> t.getType() == TaskNode.TaskType.Scoped_Retrieve)
                .filter(t -> t.getRawResults() != null)
                .flatMap(t -> t.getRawResults().stream())
                .filter(r -> r.getMetadata() != null && "LEAF".equals(r.getMetadata().get("node_type")))
                .forEach(r -> {
                    String key = r.getSourceId();
                    if (!dedupMap.containsKey(key) || r.getScore() > dedupMap.get(key).getScore()) {
                        dedupMap.put(key, r);
                    }
                });
        List<RetrievalResult> allRawResults = new ArrayList<>(dedupMap.values());

        if (!allRawResults.isEmpty()) {
            List<Map<String, Object>> refs = allRawResults.stream()
                    .map(r -> {
                        Map<String, Object> ref = new LinkedHashMap<>();
                        ref.put("title", r.getMetadata() != null
                                ? String.valueOf(r.getMetadata().getOrDefault("title_path", "Unknown"))
                                : "Unknown");
                        ref.put("score", r.getScore());
                        ref.put("content", r.getContent() != null ? r.getContent() : "");
                        return ref;
                    })
                    .collect(Collectors.toList());
            SseEventBuilder.sendRefEvent(emitter, refs);
            log.debug("SADP DAG sent {} ref documents", refs.size());
        }

        // 返回最后一个 Generate 任务的结果作为最终答案
        return tasks.stream()
                .filter(t -> t.getType() == TaskNode.TaskType.Generate)
                .reduce((a, b) -> b)
                .map(TaskNode::getResult)
                .orElseGet(() -> {
                    // 无 Generate 任务时，拼接所有结果
                    log.warn("SADP DAG has no Generate task, concatenating all results");
                    return tasks.stream()
                            .filter(t -> t.getResult() != null)
                            .map(t -> "[" + t.getId() + "] " + t.getResult())
                            .collect(Collectors.joining("\n\n"));
                });
    }

    // =====================================================================
    // 私有方法：算子执行
    // =====================================================================

    private String executeSubTask(TaskNode task, Map<String, String> priorResults,
                                   Long kbId, SseEmitter emitter,
                                   LLMClient llmClient, EmbeddingClient embeddingClient,
                                   Map<String, Integer> taskIndexMap, int totalTasks,
                                   TokenUsageLedger ledger, String historyText, int scopedTopK) {
        task.setStatus(TaskNode.TaskStatus.RUNNING);

        int taskIndex = taskIndexMap.getOrDefault(task.getId(), 0);
        String opLabel = task.getType() != null ? task.getType().name() : "Unknown";
        String thoughtMsg = String.format("执行子任务[%d/%d](%s): %s",
                taskIndex, totalTasks, opLabel,
                task.getQuery() != null && !task.getQuery().isBlank() ? task.getQuery() : task.getNodeId());
        SseEventBuilder.sendThoughtEvent(emitter, "processing", thoughtMsg, "search");
        log.info("Executing SADP subtask {} (type={}): query='{}' nodeId='{}'",
                task.getId(), task.getType(), task.getQuery(), task.getNodeId());

        TaskNode.TaskType type = task.getType() != null ? task.getType() : TaskNode.TaskType.Scoped_Retrieve;

        try {
            String result = switch (type) {
                case Scoped_Retrieve -> executeScopedRetrieve(task, kbId, embeddingClient, scopedTopK);
                case Get_Summary     -> executeGetSummary(task, kbId);
                case Generate        -> executeGenerate(task, priorResults, historyText, llmClient, ledger);
            };
            task.setStatus(TaskNode.TaskStatus.DONE);
            log.info("SADP subtask {} completed (length={})", task.getId(), result.length());
            return result;
        } catch (Exception e) {
            log.error("SADP subtask {} failed: {}", task.getId(), e.getMessage(), e);
            task.setStatus(TaskNode.TaskStatus.FAILED);
            // Scoped_Retrieve 出现异常通常是向量化/检索基础设施失败，不应静默降级。
            if (type == TaskNode.TaskType.Scoped_Retrieve) {
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new RuntimeException("SADP 检索子任务失败", e);
            }
            return "（子任务执行失败：" + e.getMessage() + "）";
        }
    }

    /**
     * Scoped_Retrieve 算子：
     * - 有 nodeId：从 LLM 选定节点的子层开始自适应分层检索（Milvus 原生 filter 逐层过滤）
     * - 无 nodeId：全知识库自适应分层检索（退化为标准路径）
     */
    private String executeScopedRetrieve(TaskNode task, Long kbId, EmbeddingClient embeddingClient, int scopedTopK) {
        String nodeId = task.getNodeId();
        AdaptiveRetriever.RetrievalBundle bundle;

        if (nodeId == null || nodeId.isBlank()) {
            // 无 scope：走完整自适应层级检索（全知识库）
            log.debug("Scoped_Retrieve: no nodeId, falling back to full-kb adaptive retrieval");
            bundle = adaptiveRetriever.retrieve(Set.of(task.getQuery()), kbId, embeddingClient, null);
        } else {
            // 有 scope：从 LLM 选定节点的子层开始层级检索
            log.debug("Scoped_Retrieve: nodeId='{}', using retrieveFromScope", nodeId);
            bundle = adaptiveRetriever.retrieveFromScope(nodeId, task.getQuery(), kbId, embeddingClient, null);
        }

        // 每个 Scoped_Retrieve 子任务最多保留 scopedTopK 个结果。
        // 先按 sourceId 去重（同 source 取高分），再按 score 降序截断。
        List<RetrievalResult> limitedResults = limitScopedResults(bundle.results(), scopedTopK);

        // 存储子任务原始结果（已按 scopedTopK 限制），供 executeDag 结束后发送 ref 事件
        task.setRawResults(limitedResults);

        if (limitedResults.isEmpty()) {
            return "（未在指定范围内检索到相关内容）";
        }

        return limitedResults.stream()
                .map(RetrievalResult::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 限制 Scoped_Retrieve 的最终结果数量（按 sourceId 去重后截断）。
     */
    private List<RetrievalResult> limitScopedResults(List<RetrievalResult> results, int scopedTopK) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }

        int limit = scopedTopK > 0 ? scopedTopK : 10;
        Map<String, RetrievalResult> dedup = new LinkedHashMap<>();

        for (RetrievalResult result : results) {
            if (result == null) {
                continue;
            }
            String sourceId = result.getSourceId();
            if (sourceId == null || sourceId.isBlank()) {
                Object nodeId = result.getMetadata() != null ? result.getMetadata().get("node_id") : null;
                sourceId = nodeId != null ? String.valueOf(nodeId) : UUID.randomUUID().toString();
            }
            RetrievalResult existing = dedup.get(sourceId);
            if (existing == null || result.getScore() > existing.getScore()) {
                dedup.put(sourceId, result);
            }
        }

        List<RetrievalResult> ranked = dedup.values().stream()
                .sorted(Comparator.comparingDouble(RetrievalResult::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        if (dedup.size() > ranked.size()) {
            log.debug("Scoped_Retrieve result capped: {} -> {} (perScopedTopK={})",
                    dedup.size(), ranked.size(), limit);
        }
        return ranked;
    }

    /**
     * Get_Summary 算子：直接从 tree_nodes 表读取 summary 字段，不走向量检索
     */
    private String executeGetSummary(TaskNode task, Long kbId) {
        String nodeId = task.getNodeId();
        return treeNodeRepository.findByKbIdAndNodeId(kbId, nodeId)
                .map(node -> {
                    if (node.getSummary() != null && !node.getSummary().isBlank()) {
                        return node.getSummary();
                    }
                    if (node.getContent() != null && !node.getContent().isBlank()) {
                        log.debug("Get_Summary: no summary for node '{}', falling back to content", nodeId);
                        return node.getContent();
                    }
                    return "（未找到摘要信息）";
                })
                .orElse("（节点 " + nodeId + " 不存在）");
    }

    /**
     * 构建 Generate 算子的 prompt（供内部 LLM 调用或外部流式调用共用）
     */
    private String buildGeneratePrompt(TaskNode task, Map<String, String> priorResults, String historyText) {
        String depsText = priorResults.isEmpty()
                ? "（无前置任务结果）"
                : priorResults.entrySet().stream()
                        .map(e -> "任务[" + e.getKey() + "]:\n" + e.getValue())
                        .collect(Collectors.joining("\n\n"));

        String resolvedHistory = historyText == null ? "" : historyText;

        return AppConstants.PromptTemplates.SADP_GENERATE_OPERATOR
                .replace("{history}", resolvedHistory)
                .replace("{query}", task.getQuery())
                .replace("{dependencies_results}", depsText);
    }

    /**
     * Generate 算子：综合前置任务结果，生成回答或中间结论。
     *
     * <p>若当前任务为最终 Generate 节点（{@code terminalGenerate=true}），
     * 直接返回 prompt 而不调用 LLM，由 RAGServiceImpl 使用该 prompt 发起流式调用，
     * 避免二次 LLM 调用（先同步生成 → 再将答案作为 prompt 流式输出）。</p>
     */
    private String executeGenerate(TaskNode task, Map<String, String> priorResults,
                                    String historyText, LLMClient llmClient, TokenUsageLedger ledger) {
        String prompt = buildGeneratePrompt(task, priorResults, historyText);
        if (task.isTerminalGenerate()) {
            log.debug("Terminal Generate task {}: returning prompt for streaming (skip sync LLM call)", task.getId());
            return prompt;
        }
        return llmClient.chat(prompt, "生成[" + task.getId() + "]", ledger);
    }

    // =====================================================================
    // 私有工具方法
    // =====================================================================

    /**
     * 解析算子类型字符串，解析失败时降级为 Scoped_Retrieve
     */
    private TaskNode.TaskType parseTaskType(String typeStr) {
        try {
            return TaskNode.TaskType.valueOf(typeStr);
        } catch (Exception e) {
            log.warn("Unknown task type '{}', defaulting to Scoped_Retrieve", typeStr);
            return TaskNode.TaskType.Scoped_Retrieve;
        }
    }

    /**
     * 解析依赖列表
     */
    @SuppressWarnings("unchecked")
    private List<String> parseDeps(Object depsObj) {
        if (depsObj instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 降级方案：当 LLM 规划失败时，生成单个 Scoped_Retrieve 任务（全库检索）
     */
    private List<TaskNode> buildFallbackTasks(String query) {
        TaskNode fallback = new TaskNode();
        fallback.setId("T1");
        fallback.setType(TaskNode.TaskType.Scoped_Retrieve);
        fallback.setQuery(query);
        fallback.setNodeId("");
        fallback.setDependsOn(Collections.emptyList());
        return Collections.singletonList(fallback);
    }
}
