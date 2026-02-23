package com.mtmn.smartdoc.rag.retriever;

import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.client.SseEventBuilder;
import com.mtmn.smartdoc.model.config.RagProperties;
import com.mtmn.smartdoc.service.MilvusService;
import com.mtmn.smartdoc.vo.RetrievalResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 自适应阈值递归检索器（HiSem 完整版）
 *
 * <p>基于树形结构的层级检索策略：
 * <ol>
 *   <li>从最高层（level=1）开始向量搜索，使用 Milvus 原生 filter 精确限定层级</li>
 *   <li>使用分布感知动态阈值公式筛选候选节点</li>
 *   <li>对通过阈值的非叶子节点递归检索其子节点（filter: parent_node_id == X）</li>
 *   <li>最终返回所有命中的叶子节点内容</li>
 * </ol>
 *
 * <p>动态阈值公式：θ = β·sMax - (1 - γ·CV)·(sMax - μ)
 * 其中 CV = σ/μ 为变异系数，用于感知分数分布的离散程度。
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdaptiveRetriever {

    private final MilvusService milvusService;
    private final RagProperties ragProperties;

    /**
     * 执行自适应阈值层级检索（使用 hisemSadp 配置）
     *
     * @param queries         查询语句集合（支持多查询并行）
     * @param kbId            知识库ID
     * @param embeddingClient 向量化客户端
     * @param emitter         SSE 推送器（用于实时展示检索进度）
     * @return 命中的检索结果列表（叶子节点内容）
     */
    public List<RetrievalResult> retrieve(Set<String> queries, Long kbId,
                                           EmbeddingClient embeddingClient,
                                           SseEmitter emitter) {
        RagProperties.ThresholdConfig tc = ragProperties.getHisemSadp().getThreshold();
        return retrieve(queries, kbId, embeddingClient, emitter,
                tc.getBeta(), tc.getGamma(), tc.getThetaMin(), tc.getKMin(), tc.getKMax());
    }

    /**
     * 执行自适应阈值层级检索（完整参数版）
     */
    public List<RetrievalResult> retrieve(Set<String> queries, Long kbId,
                                           EmbeddingClient embeddingClient,
                                           SseEmitter emitter,
                                           double beta, double gamma,
                                           double thetaMin, int kMin, int kMax) {
        log.debug("=== Adaptive retrieval start: kbId={}, queries={}, params[β={}, γ={}, θMin={}, kMin={}, kMax={}] ===",
                kbId, queries.size(), beta, gamma, thetaMin, kMin, kMax);

        Map<String, RetrievalResult> uniqueResults = new LinkedHashMap<>();

        for (String query : queries) {
            try {
                log.debug("Processing query: '{}'", query.length() > 100 ? query.substring(0, 100) + "..." : query);
                Embedding queryVector = embeddingClient.embed(query);
                log.debug("Query embedded, dimension={}", queryVector.dimension());

                // 根层过滤：Milvus 原生 filter level == 1（不再全量搜索后 Java 过滤）
                Filter rootFilter = MetadataFilterBuilder.metadataKey("level").isEqualTo(1);
                List<RetrievalResult> queryResults = retrieveLevel(
                        queryVector, kbId, rootFilter, emitter, beta, gamma, thetaMin, kMin, kMax);

                log.debug("Query retrieval complete: {} results", queryResults.size());

                for (RetrievalResult result : queryResults) {
                    String key = result.getSourceId();
                    if (!uniqueResults.containsKey(key) ||
                            result.getScore() > uniqueResults.get(key).getScore()) {
                        uniqueResults.put(key, result);
                    }
                }
            } catch (Exception e) {
                log.error("Adaptive retrieval failed for query '{}': {}", query, e.getMessage(), e);
            }
        }

        List<RetrievalResult> finalResults = uniqueResults.values().stream()
                .sorted(Comparator.comparingDouble(RetrievalResult::getScore).reversed())
                .collect(Collectors.toList());

        log.debug("=== Adaptive retrieval complete: {} unique results ===", finalResults.size());
        if (log.isDebugEnabled()) {
            for (int i = 0; i < finalResults.size(); i++) {
                RetrievalResult r = finalResults.get(i);
                String titlePath = r.getMetadata() != null
                        ? String.valueOf(r.getMetadata().getOrDefault("title_path", ""))
                        : "";
                log.debug("  Result[{}]: score={}, sourceId={}, titlePath='{}'",
                        i, String.format("%.4f", r.getScore()), r.getSourceId(), titlePath);
            }
        }

        return finalResults;
    }

    /**
     * SADP Scoped_Retrieve 入口：从指定节点的子层开始自适应分层检索
     *
     * <p>LLM 已从骨架选定起始节点，此处跳过顶层全量扫描，直接从该节点的子节点层
     * 开始执行与标准路径完全一致的自适应阈值递归检索。
     *
     * @param scopeNodeId     LLM 从骨架选出的起始节点 ID
     * @param query           检索关键词
     * @param kbId            知识库 ID
     * @param embeddingClient 向量化客户端
     * @param emitter         SSE 推送器
     * @return 命中的检索结果列表
     */
    public List<RetrievalResult> retrieveFromScope(String scopeNodeId, String query, Long kbId,
                                                    EmbeddingClient embeddingClient, SseEmitter emitter) {
        RagProperties.ThresholdConfig tc = ragProperties.getHisemSadp().getThreshold();
        Embedding queryVector = embeddingClient.embed(query);

        log.debug("retrieveFromScope: scopeNodeId='{}', query='{}'", scopeNodeId, query);

        // 从 scopeNodeId 的子节点层开始（Milvus 原生 filter: parent_node_id == scopeNodeId）
        Filter childFilter = MetadataFilterBuilder.metadataKey("parent_node_id").isEqualTo(scopeNodeId);
        List<RetrievalResult> results = retrieveLevel(queryVector, kbId, childFilter, emitter,
                tc.getBeta(), tc.getGamma(), tc.getThetaMin(), tc.getKMin(), tc.getKMax());

        if (results.isEmpty()) {
            // 退化：scopeNodeId 可能是叶子节点，直接搜索它自身
            log.debug("retrieveFromScope: no child results for scopeNodeId='{}', searching node itself", scopeNodeId);
            Filter nodeFilter = MetadataFilterBuilder.metadataKey("node_id").isEqualTo(scopeNodeId);
            results = milvusService.search(kbId, queryVector, tc.getKMax(), 0.0, nodeFilter);
        }

        log.debug("retrieveFromScope: {} results for scopeNodeId='{}'", results.size(), scopeNodeId);
        return results;
    }

    /**
     * 单层检索 + 自适应阈值 + 递归下钻（Milvus 原生 filter，无 Java 后过滤）
     *
     * <p>使用 Milvus 原生 filter 精确限定本次搜索范围：
     * <ul>
     *   <li>根层调用：filter = level == 1</li>
     *   <li>子层调用：filter = parent_node_id == parentNodeId</li>
     * </ul>
     *
     * @param scopeFilter Milvus 原生过滤条件
     */
    private List<RetrievalResult> retrieveLevel(Embedding queryVector, Long kbId,
                                                 Filter scopeFilter, SseEmitter emitter,
                                                 double beta, double gamma,
                                                 double thetaMin, int kMin, int kMax) {
        // 1. Milvus 原生 scoped 搜索（不再全量搜索后 Java 过滤）
        List<RetrievalResult> candidates = milvusService.search(kbId, queryVector, kMax, 0.0, scopeFilter);
        log.debug("Milvus scoped search returned {} candidates", candidates.size());

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 收集 title_path 用于 SSE 展示
        List<String> candidateTitlePaths = candidates.stream()
                .map(r -> {
                    Object tp = r.getMetadata() != null ? r.getMetadata().get("title_path") : null;
                    return tp != null ? String.valueOf(tp) : "";
                })
                .filter(tp -> !tp.isBlank())
                .distinct()
                .toList();

        String thoughtMsg;
        if (!candidateTitlePaths.isEmpty()) {
            String pathsDisplay = candidateTitlePaths.size() <= 3
                    ? String.join("、", candidateTitlePaths)
                    : String.join("、", candidateTitlePaths.subList(0, 3)) + " 等";
            thoughtMsg = String.format("正在检索「%s」（%d 个候选节点）...", pathsDisplay, candidates.size());
        } else {
            thoughtMsg = String.format("正在层级检索（%d 个候选节点）...", candidates.size());
        }
        SseEventBuilder.sendThoughtEvent(emitter, "processing", thoughtMsg, "search");

        // 3. Debug：打印候选节点详情
        if (log.isDebugEnabled()) {
            candidates.forEach(r -> {
                Map<String, Object> meta = r.getMetadata();
                log.debug("  candidate: nodeId={}, score={}, titlePath='{}', nodeType={}",
                        meta != null ? meta.getOrDefault("node_id", "?") : "?",
                        String.format("%.4f", r.getScore()),
                        meta != null ? meta.getOrDefault("title_path", "") : "",
                        meta != null ? meta.getOrDefault("node_type", "") : "");
            });
        }

        // 4. 计算动态阈值
        List<Double> scores = candidates.stream()
                .map(RetrievalResult::getScore)
                .collect(Collectors.toList());
        double threshold = calculateDynamicThreshold(scores, beta, gamma, thetaMin, kMin, kMax);

        DoubleSummaryStatistics stats = scores.stream().mapToDouble(d -> d).summaryStatistics();
        log.debug("Scores: min={}, max={}, avg={}, count={} → dynamicThreshold={}",
                String.format("%.4f", stats.getMin()), String.format("%.4f", stats.getMax()),
                String.format("%.4f", stats.getAverage()), stats.getCount(),
                String.format("%.4f", threshold));

        // 5. 按阈值筛选
        List<RetrievalResult> passed = candidates.stream()
                .filter(r -> r.getScore() >= threshold)
                .toList();

        log.debug("Threshold filter: {} / {} candidates passed", passed.size(), candidates.size());

        // 6. 处理每个通过阈值的节点
        List<RetrievalResult> finalResults = new ArrayList<>();

        for (RetrievalResult result : passed) {
            Map<String, Object> meta = result.getMetadata();
            String nodeId = meta != null ? (String) meta.get("node_id") : null;
            String titlePath = meta != null ? String.valueOf(meta.getOrDefault("title_path", "")) : "";

            // 用 Milvus 结果中已有的 node_type 判断叶子节点，零额外 DB 查询
            boolean isLeaf = meta != null && "LEAF".equals(meta.get("node_type"));

            log.debug("  Processing node: nodeId={}, titlePath='{}', score={}, isLeaf={}",
                    nodeId, titlePath, String.format("%.4f", result.getScore()), isLeaf);

            if (isLeaf || nodeId == null) {
                // 叶子节点直接加入结果
                log.debug("  → Leaf node added to results");
                finalResults.add(result);
            } else {
                // 非叶子节点：递归检索子节点（Milvus 原生 filter: parent_node_id == nodeId）
                log.debug("  → Internal node, recursing into children via parent_node_id filter...");
                Filter childFilter = MetadataFilterBuilder.metadataKey("parent_node_id").isEqualTo(nodeId);
                List<RetrievalResult> childResults = retrieveLevel(
                        queryVector, kbId, childFilter, emitter, beta, gamma, thetaMin, kMin, kMax);

                if (childResults.isEmpty()) {
                    // 子节点没有命中时，退化为当前节点
                    log.debug("  → No child results, falling back to current node");
                    finalResults.add(result);
                } else {
                    log.debug("  → Got {} child results", childResults.size());
                    finalResults.addAll(childResults);
                }
            }
        }

        log.debug("Level complete: {} final results", finalResults.size());
        return finalResults;
    }

    /**
     * 动态阈值计算
     *
     * <p>公式：θ = β·sMax - (1 - γ·CV)·(sMax - μ)
     * <p>其中 CV = σ/μ 为变异系数
     *
     * @param scores   候选节点的相似度分数列表
     * @param beta     控制阈值与最高分的距离
     * @param gamma    控制分布离散程度的影响权重
     * @param thetaMin 最小阈值下界
     * @param kMin     至少保留的最小候选数量
     * @param kMax     最多保留的最大候选数量
     * @return 计算出的动态阈值
     */
    private double calculateDynamicThreshold(List<Double> scores, double beta, double gamma,
                                              double thetaMin, int kMin, int kMax) {
        if (scores.isEmpty()) return thetaMin;

        double sMax = Collections.max(scores);
        double mu = scores.stream().mapToDouble(d -> d).average().orElse(0.0);
        double variance = scores.stream()
                .mapToDouble(s -> Math.pow(s - mu, 2))
                .average()
                .orElse(0.0);
        double sigma = Math.sqrt(variance);

        // 变异系数（防除零）
        double cv = (mu == 0.0) ? 0.0 : sigma / mu;

        // 阈值公式
        double theta = beta * sMax - (1.0 - gamma * cv) * (sMax - mu);

        log.debug("Threshold calc: sMax={}, μ={}, σ={}, CV={}, raw θ={}",
                String.format("%.4f", sMax), String.format("%.4f", mu),
                String.format("%.4f", sigma), String.format("%.4f", cv),
                String.format("%.4f", theta));

        // 确保不低于最小阈值
        theta = Math.max(theta, thetaMin);

        // 确保至少 kMin 个结果
        List<Double> sorted = scores.stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        double result = theta;

        final double thetaForKMin = result;
        long passCount = scores.stream().filter(s -> s >= thetaForKMin).count();
        if (passCount < kMin && sorted.size() >= kMin) {
            log.debug("Threshold adjusted for kMin: {} passed < kMin={}, lowering to {}", passCount, kMin, String.format("%.4f", sorted.get(kMin - 1)));
            result = sorted.get(kMin - 1);
        }

        // 限制最多 kMax 个结果（基于更新后的 result 重新计算）
        final double thetaForKMax = result;
        passCount = scores.stream().filter(s -> s >= thetaForKMax).count();
        if (passCount > kMax && sorted.size() >= kMax) {
            log.debug("Threshold adjusted for kMax: {} passed > kMax={}, raising to {}", passCount, kMax, String.format("%.4f", sorted.get(kMax - 1)));
            result = sorted.get(kMax - 1);
        }

        log.debug("Final threshold: {}", String.format("%.4f", result));
        return result;
    }
}
