package com.mtmn.smartdoc.rag.sadp;

import com.mtmn.smartdoc.constants.AppConstants;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.model.client.SseEventBuilder;
import com.mtmn.smartdoc.rag.retriever.AdaptiveRetriever;
import com.mtmn.smartdoc.utils.LlmJsonUtils;
import com.mtmn.smartdoc.vo.RetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * SADP（Self-Adaptive DAG Planner）规划与执行引擎
 *
 * <p>负责判断问题复杂度、将复杂多跳问题拆解为 DAG 任务图，
 * 并通过拓扑排序 + CompletableFuture 并行执行各子任务。
 *
 * <p>执行过程中通过 SseEmitter 实时推送进度 thought 事件。
 *
 * @author charmingdaidai
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SadpPlanner {

    private final AdaptiveRetriever adaptiveRetriever;

    /**
     * 判断问题是否需要多跳推理
     *
     * @param query     用户原始问题
     * @param llmClient LLM 客户端
     * @return true 表示复杂多跳问题，false 表示简单问题
     */
    public boolean isComplexQuery(String query, LLMClient llmClient) {
        try {
            log.debug("SADP complexity check for query: '{}'", query);
            String prompt = AppConstants.PromptTemplates.SADP_COMPLEXITY_CHECK
                    .replace("{query}", query);
            String llmOutput = llmClient.chat(prompt);
            log.debug("SADP complexity LLM response: {}", llmOutput);

            Map<String, Object> result = LlmJsonUtils.parseMap(llmOutput);
            if (result.isEmpty()) {
                log.warn("SADP complexity check returned empty result, defaulting to simple");
                return false;
            }

            Object complexObj = result.get("complex");
            boolean isComplex;
            if (complexObj instanceof Boolean b) {
                isComplex = b;
            } else if (complexObj instanceof String s) {
                isComplex = Boolean.parseBoolean(s);
            } else {
                isComplex = false;
            }

            log.debug("SADP complexity result: isComplex={}, reason={}", isComplex, result.get("reason"));
            return isComplex;
        } catch (Exception e) {
            log.warn("SADP complexity check failed: {}, defaulting to simple query", e.getMessage());
            return false;
        }
    }

    /**
     * 将复杂问题拆解为 DAG 任务列表
     *
     * @param query     用户原始问题
     * @param llmClient LLM 客户端
     * @return 任务节点列表（已按拓扑顺序排列）
     */
    public List<TaskNode> decomposeToDag(String query, LLMClient llmClient) {
        try {
            log.debug("SADP decomposing query: '{}'", query);
            String prompt = AppConstants.PromptTemplates.SADP_DAG_DECOMPOSITION
                    .replace("{query}", query);
            String llmOutput = llmClient.chat(prompt);
            log.debug("SADP decomposition LLM response: {}", llmOutput);

            // Parse list of task maps
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawTasks = (List<Map<String, Object>>) (List<?>)
                    LlmJsonUtils.parseList(llmOutput, LinkedHashMap.class);
            if (rawTasks == null || rawTasks.isEmpty()) {
                log.warn("SADP decomposition returned empty task list");
                return buildFallbackTasks(query);
            }

            List<TaskNode> tasks = new ArrayList<>();
            for (Map<String, Object> rawTask : rawTasks) {
                TaskNode node = new TaskNode();
                node.setId(String.valueOf(rawTask.getOrDefault("id", "t" + (tasks.size() + 1))));
                node.setDescription(String.valueOf(rawTask.getOrDefault("description", query)));

                Object depsObj = rawTask.get("dependsOn");
                if (depsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> deps = ((List<?>) depsObj).stream()
                            .map(Object::toString)
                            .collect(Collectors.toList());
                    node.setDependsOn(deps);
                }

                tasks.add(node);
            }

            log.info("SADP decomposed query into {} tasks", tasks.size());
            if (log.isDebugEnabled()) {
                for (TaskNode task : tasks) {
                    log.debug("  Task[{}]: '{}', dependsOn={}", task.getId(), task.getDescription(), task.getDependsOn());
                }
            }
            return tasks;

        } catch (Exception e) {
            log.error("SADP decomposition failed: {}", e.getMessage());
            return buildFallbackTasks(query);
        }
    }

    /**
     * 执行 DAG 任务图并返回最终综合答案
     *
     * <p>使用拓扑排序 + CompletableFuture 并发执行无依赖子任务，
     * 每个子任务完成时推送 SSE thought 事件。
     *
     * @param tasks           DAG 任务列表
     * @param originalQuery   用户原始问题
     * @param kbId            知识库 ID
     * @param emitter         SSE 推送器
     * @param llmClient       LLM 客户端
     * @param embeddingClient 向量化客户端
     * @return 最终综合答案文本
     */
    public String executeDag(List<TaskNode> tasks, String originalQuery, Long kbId,
                              SseEmitter emitter, LLMClient llmClient, EmbeddingClient embeddingClient) {
        int totalTasks = tasks.size();

        // 按插入顺序记录任务索引（1-based），保证 SSE 进度消息顺序正确
        Map<String, Integer> taskIndexMap = new LinkedHashMap<>();
        for (int i = 0; i < tasks.size(); i++) {
            taskIndexMap.put(tasks.get(i).getId(), i + 1);
        }

        Map<String, CompletableFuture<String>> futures = new LinkedHashMap<>();
        Map<String, TaskNode> taskMap = tasks.stream()
                .collect(Collectors.toMap(TaskNode::getId, t -> t));

        // 按拓扑顺序调度任务
        for (TaskNode task : tasks) {
            List<CompletableFuture<String>> depFutures = task.getDependsOn().stream()
                    .map(depId -> futures.getOrDefault(depId,
                            CompletableFuture.completedFuture("")))
                    .collect(Collectors.toList());

            CompletableFuture<String> taskFuture;

            if (depFutures.isEmpty()) {
                // 无依赖：直接异步执行
                taskFuture = CompletableFuture.supplyAsync(() ->
                        executeSubTask(task, Collections.emptyMap(), kbId,
                                emitter, llmClient, embeddingClient, taskMap, taskIndexMap, totalTasks));
            } else {
                // 有依赖：等待依赖完成后执行
                CompletableFuture<Void> allDeps = CompletableFuture.allOf(
                        depFutures.toArray(CompletableFuture[]::new));

                taskFuture = allDeps.thenApplyAsync(ignored -> {
                    // 收集依赖结果
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
                            emitter, llmClient, embeddingClient, taskMap, taskIndexMap, totalTasks);
                });
            }

            futures.put(task.getId(), taskFuture);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new)).join();

        // 收集所有子任务结果
        StringBuilder subtaskResults = new StringBuilder();
        for (TaskNode task : tasks) {
            CompletableFuture<String> future = futures.get(task.getId());
            String result = "";
            if (future != null) {
                try {
                    result = future.join();
                    task.setResult(result);
                    task.setStatus(TaskNode.TaskStatus.DONE);
                } catch (Exception e) {
                    result = "（子任务执行失败：" + e.getMessage() + "）";
                    task.setStatus(TaskNode.TaskStatus.FAILED);
                }
            }
            subtaskResults.append("子任务[").append(task.getId()).append("]: ")
                    .append(task.getDescription()).append("\n")
                    .append("结果: ").append(result).append("\n\n");
        }

        // 调用 LLM 综合生成最终答案
        String finalPrompt = AppConstants.PromptTemplates.SADP_FINAL_SYNTHESIS
                .replace("{query}", originalQuery)
                .replace("{subtask_results}", subtaskResults.toString());

        return llmClient.chat(finalPrompt);
    }

    /**
     * 执行单个子任务
     */
    private String executeSubTask(TaskNode task, Map<String, String> priorResults,
                                   Long kbId, SseEmitter emitter,
                                   LLMClient llmClient, EmbeddingClient embeddingClient,
                                   Map<String, TaskNode> taskMap,
                                   Map<String, Integer> taskIndexMap, int totalTasks) {
        task.setStatus(TaskNode.TaskStatus.RUNNING);

        int taskIndex = taskIndexMap.getOrDefault(task.getId(), 0);
        String thoughtMsg = String.format("执行子任务[%d/%d]: %s...", taskIndex, totalTasks, task.getDescription());
        SseEventBuilder.sendThoughtEvent(emitter, "processing", thoughtMsg, "search");
        log.info("Executing SADP subtask {}: {}", task.getId(), task.getDescription());

        try {
            // 自适应检索
            Set<String> querySet = Collections.singleton(task.getDescription());
            log.debug("SADP subtask {} starting adaptive retrieval for: '{}'", task.getId(), task.getDescription());
            List<RetrievalResult> results = adaptiveRetriever.retrieve(
                    querySet, kbId, embeddingClient, emitter);
            log.debug("SADP subtask {} retrieval complete: {} results", task.getId(), results.size());

            // 构建上下文
            String context = buildContext(results);

            // 构建前置任务结果描述
            String priorResultsText = priorResults.isEmpty()
                    ? "无"
                    : priorResults.entrySet().stream()
                    .map(e -> taskMap.containsKey(e.getKey())
                            ? taskMap.get(e.getKey()).getDescription() + ": " + e.getValue()
                            : e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining("\n"));

            // 调用 LLM 回答子任务
            String subPrompt = AppConstants.PromptTemplates.SADP_SUBTASK_ANSWER
                    .replace("{subtask}", task.getDescription())
                    .replace("{prior_results}", priorResultsText)
                    .replace("{context}", context);

            String answer = llmClient.chat(subPrompt);
            task.setStatus(TaskNode.TaskStatus.DONE);

            log.info("SADP subtask {} completed, answer length={}", task.getId(), answer.length());
            return answer;

        } catch (Exception e) {
            log.error("SADP subtask {} failed: {}", task.getId(), e.getMessage());
            task.setStatus(TaskNode.TaskStatus.FAILED);
            return "（检索失败：" + e.getMessage() + "）";
        }
    }

    /**
     * 将检索结果构建为上下文字符串
     */
    private String buildContext(List<RetrievalResult> results) {
        if (results.isEmpty()) {
            return "（未检索到相关文档）";
        }

        return IntStream.range(0, results.size())
                .mapToObj(i -> {
                    RetrievalResult r = results.get(i);
                    String safeContent = r.getContent() != null
                            ? r.getContent().replace("<", "&lt;").replace(">", "&gt;")
                            : "";
                    return String.format("<doc id=\"%d\">%s</doc>", i + 1, safeContent);
                })
                .collect(Collectors.joining("\n"));
    }

    /**
     * 降级方案：单个子任务（当 LLM 分解失败时）
     */
    private List<TaskNode> buildFallbackTasks(String query) {
        TaskNode fallback = new TaskNode();
        fallback.setId("t1");
        fallback.setDescription(query);
        fallback.setDependsOn(Collections.emptyList());
        return Collections.singletonList(fallback);
    }
}
