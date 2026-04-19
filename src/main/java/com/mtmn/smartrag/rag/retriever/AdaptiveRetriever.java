package com.mtmn.smartrag.rag.retriever;

import com.mtmn.smartrag.model.client.EmbeddingClient;
import com.mtmn.smartrag.model.client.SseEventBuilder;
import com.mtmn.smartrag.model.config.RagProperties;
import com.mtmn.smartrag.service.MilvusService;
import com.mtmn.smartrag.vo.RetrievalResult;
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
 *   <li>优先从根层（level=1）开始向量搜索；若无候选则按层级逐级回退（level=2..N）</li>
 *   <li>使用分布感知动态阈值公式筛选候选节点</li>
 *   <li>对通过阈值的非叶子节点递归检索其子节点（filter: parent_node_id == X）</li>
 *   <li>最终返回所有命中的叶子节点内容</li>
 * </ol>
 *
 * <p>动态阈值公式：θ = β·sMax - (1 - γ·CV)·(sMax - μ)
 * 其中 CV = σ/μ 为变异系数，用于感知分数分布的离散程度。
 *
 * @author charmingdaidai
 * @version 2.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdaptiveRetriever {

    private final MilvusService milvusService;
    private final RagProperties ragProperties;
        private static final int ROOT_LEVEL_MIN = 1;
        private static final int ROOT_LEVEL_MAX = 10;

    // =====================================================================
    // 公开返回类型
    // =====================================================================

    /**
     * retrieve() / retrieveFromScope() 的组合返回值
     *
     * @param results   命中的检索结果列表（叶子节点内容）
     * @param treeRoots 检索路径树的根节点列表（供前端可视化）
     */
    public record RetrievalBundle(List<RetrievalResult> results, List<RetrievalTreeNode> treeRoots) {}

    // =====================================================================
    // 私有内部类型
    // =====================================================================

    /** retrieveLevel() 的组合返回值 */
    private record LevelBundle(List<RetrievalResult> results, List<RetrievalTreeNode> treeNodes) {}

    // =====================================================================
    // 公开方法
    // =====================================================================

    /**
     * 执行自适应阈值层级检索（使用 hisemSadp 配置）
     *
     * <p>检索过程仅发送 2 条 thought 事件（开始 + 结束），
     * 详细的层级路径通过 {@code RetrievalBundle.treeRoots} 传递给前端可视化。
     *
     * @param queries         查询语句集合（支持多查询）
     * @param kbId            知识库ID
     * @param embeddingClient 向量化客户端
     * @param emitter         SSE 推送器（用于实时展示检索进度，可为 null）
     * @return 包含命中结果和检索树的 RetrievalBundle
     */
    public RetrievalBundle retrieve(Set<String> queries, Long kbId,
                                    EmbeddingClient embeddingClient,
                                    SseEmitter emitter) {
        RagProperties.ThresholdConfig tc = ragProperties.getHisemSadp().getThreshold();
        return retrieve(queries, kbId, embeddingClient, emitter,
                tc.getBeta(), tc.getGamma(), tc.getThetaMin(), tc.getKMin(), tc.getKMax());
    }

    /**
     * 执行自适应阈值层级检索（完整参数版）
     */
    public RetrievalBundle retrieve(Set<String> queries, Long kbId,
                                    EmbeddingClient embeddingClient,
                                    SseEmitter emitter,
                                    double beta, double gamma,
                                    double thetaMin, int kMin, int kMax) {
                if (queries == null || queries.isEmpty()) {
                        log.warn("Adaptive retrieval skipped: empty queries, kbId={}", kbId);
                        return new RetrievalBundle(Collections.emptyList(), Collections.emptyList());
                }

        log.debug("=== Adaptive retrieval start: kbId={}, queries={}, params[β={}, γ={}, θMin={}, kMin={}, kMax={}] ===",
                kbId, queries.size(), beta, gamma, thetaMin, kMin, kMax);

        SseEventBuilder.sendThoughtEvent(emitter, "processing", "正在执行层级检索...", "search");

        Map<String, RetrievalResult> uniqueResults = new LinkedHashMap<>();
        List<RetrievalTreeNode> allTreeRoots = new ArrayList<>();
                int failureCount = 0;
                Exception firstFailure = null;

        for (String query : queries) {
            try {
                log.debug("Processing query: '{}'", query.length() > 100 ? query.substring(0, 100) + "..." : query);
                Embedding queryVector = embeddingClient.embed(query);
                log.debug("Query embedded, dimension={}", queryVector.dimension());

                                // 根层搜索：优先 level=1，若无候选按层级逐级回退
                                LevelBundle levelBundle = retrieveFromRootLevels(
                                                queryVector, kbId, beta, gamma, thetaMin, kMin, kMax);

                log.debug("Query retrieval complete: {} results, {} tree nodes",
                        levelBundle.results().size(), levelBundle.treeNodes().size());

                for (RetrievalResult result : levelBundle.results()) {
                    String key = result.getSourceId();
                    if (!uniqueResults.containsKey(key) ||
                            result.getScore() > uniqueResults.get(key).getScore()) {
                        uniqueResults.put(key, result);
                    }
                }
                allTreeRoots.addAll(levelBundle.treeNodes());

            } catch (Exception e) {
                                failureCount++;
                                if (firstFailure == null) {
                                        firstFailure = e;
                                }
                log.error("Adaptive retrieval failed for query '{}': {}", query, e.getMessage(), e);
            }
        }

                if (failureCount == queries.size()) {
                        if (firstFailure instanceof RuntimeException runtimeException) {
                                throw runtimeException;
                        }
                        throw new RuntimeException("Adaptive retrieval failed for all queries", firstFailure);
                }
                if (failureCount > 0) {
                        log.warn("Adaptive retrieval partially failed: failed={}/{}", failureCount, queries.size());
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

        return new RetrievalBundle(finalResults, allTreeRoots);
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
     * @param emitter         SSE 推送器（可为 null）
     * @return 包含命中结果和检索树的 RetrievalBundle
     */
    public RetrievalBundle retrieveFromScope(String scopeNodeId, String query, Long kbId,
                                             EmbeddingClient embeddingClient, SseEmitter emitter) {
        RagProperties.ThresholdConfig tc = ragProperties.getHisemSadp().getThreshold();
        Embedding queryVector = embeddingClient.embed(query);

        log.debug("retrieveFromScope: scopeNodeId='{}', query='{}'", scopeNodeId, query);

        // 从 scopeNodeId 的子节点层开始（Milvus 原生 filter: parent_node_id == scopeNodeId）
        Filter childFilter = MetadataFilterBuilder.metadataKey("parent_node_id").isEqualTo(scopeNodeId);
        LevelBundle bundle = retrieveLevel(queryVector, kbId, childFilter,
                tc.getBeta(), tc.getGamma(), tc.getThetaMin(), tc.getKMin(), tc.getKMax());

        if (bundle.results().isEmpty()) {
            // 退化：scopeNodeId 可能是叶子节点，直接搜索它自身
            log.debug("retrieveFromScope: no child results for scopeNodeId='{}', searching node itself", scopeNodeId);
            Filter nodeFilter = MetadataFilterBuilder.metadataKey("node_id").isEqualTo(scopeNodeId);
            List<RetrievalResult> fallback = milvusService.search(kbId, queryVector, tc.getKMax(), 0.0, nodeFilter);
            log.debug("retrieveFromScope: {} fallback results for scopeNodeId='{}'", fallback.size(), scopeNodeId);
            // 退化时无树（直接命中叶节点，无层级结构）
            return new RetrievalBundle(fallback, Collections.emptyList());
        }

        log.debug("retrieveFromScope: {} results, {} tree nodes for scopeNodeId='{}'",
                bundle.results().size(), bundle.treeNodes().size(), scopeNodeId);
        return new RetrievalBundle(bundle.results(), bundle.treeNodes());
    }

        /**
         * 根层检索入口：优先从 level=1 开始，若无候选则按层级逐级回退。
         *
         * <p>目的：兼容文档最高标题不是 H1（例如从 H2 开始）的场景，
         * 避免根层固定 level=1 导致递归检索无法启动。
         */
        private LevelBundle retrieveFromRootLevels(Embedding queryVector, Long kbId,
                                                                                           double beta, double gamma,
                                                                                           double thetaMin, int kMin, int kMax) {
                for (int rootLevel = ROOT_LEVEL_MIN; rootLevel <= ROOT_LEVEL_MAX; rootLevel++) {
                        Filter rootFilter = MetadataFilterBuilder.metadataKey("level").isEqualTo(rootLevel);
                        LevelBundle levelBundle = retrieveLevel(
                                        queryVector, kbId, rootFilter, beta, gamma, thetaMin, kMin, kMax);

                        if (!levelBundle.results().isEmpty()) {
                                if (rootLevel > ROOT_LEVEL_MIN) {
                                        log.info("Adaptive retrieval root level fallback applied: kbId={}, rootLevel={}", kbId, rootLevel);
                                }
                                return levelBundle;
                        }

                        log.debug("No root candidates at level={}, trying next level", rootLevel);
                }

                log.debug("No root candidates found for kbId={} in levels [{}-{}]", kbId, ROOT_LEVEL_MIN, ROOT_LEVEL_MAX);
                return new LevelBundle(Collections.emptyList(), Collections.emptyList());
        }

    // =====================================================================
    // 私有方法：核心层级检索（不发 SSE 事件）
    // =====================================================================

    /**
     * 单层检索 + 自适应阈值 + 递归下钻（Milvus 原生 filter，无 Java 后过滤）
     *
     * <p>此方法不发送任何 SSE 事件，仅负责检索逻辑和树节点构建。
     * SSE 事件统一在 {@link #retrieve} 方法中发送（开始 + 结束各一条）。
     *
        * <p>使用 Milvus 原生 filter 精确限定本次搜索范围：
     * <ul>
        *   <li>根层调用：filter = level == L（L 由根层回退策略在 1..N 动态选择）</li>
     *   <li>子层调用：filter = parent_node_id == parentNodeId</li>
     * </ul>
     *
     * @param scopeFilter Milvus 原生过滤条件
     * @return 包含最终命中结果和本层树节点的 LevelBundle
     */
    private LevelBundle retrieveLevel(Embedding queryVector, Long kbId,
                                      Filter scopeFilter,
                                      double beta, double gamma,
                                      double thetaMin, int kMin, int kMax) {
        // 1. Milvus 原生 scoped 搜索（不再全量搜索后 Java 过滤）
        List<RetrievalResult> candidates = milvusService.search(kbId, queryVector, kMax, 0.0, scopeFilter);
        log.debug("Milvus scoped search returned {} candidates", candidates.size());

        if (candidates.isEmpty()) {
            return new LevelBundle(Collections.emptyList(), Collections.emptyList());
        }

        // 2. Debug：打印候选节点详情
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

        // 3. 计算动态阈值
        List<Double> scores = candidates.stream()
                .map(RetrievalResult::getScore)
                .collect(Collectors.toList());
        double threshold = calculateDynamicThreshold(scores, beta, gamma, thetaMin, kMin, kMax);

        DoubleSummaryStatistics stats = scores.stream().mapToDouble(d -> d).summaryStatistics();
        log.debug("Scores: min={}, max={}, avg={}, count={} → dynamicThreshold={}",
                String.format("%.4f", stats.getMin()), String.format("%.4f", stats.getMax()),
                String.format("%.4f", stats.getAverage()), stats.getCount(),
                String.format("%.4f", threshold));

        // 4. 按阈值筛选（通过阈值的节点进入下钻或直接收录）
        final double finalThreshold = threshold;
        List<RetrievalResult> passed = candidates.stream()
                .filter(r -> r.getScore() >= finalThreshold)
                .toList();

        log.debug("Threshold filter: {} / {} candidates passed", passed.size(), candidates.size());

        // 5. 处理每个通过阈值的节点
        List<RetrievalResult> finalResults = new ArrayList<>();
        List<RetrievalTreeNode> treeNodes = new ArrayList<>();

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
                treeNodes.add(RetrievalTreeNode.builder()
                        .nodeId(nodeId)
                        .titlePath(titlePath)
                        .score(result.getScore())
                        .passedThreshold(true)
                        .inResults(true)
                        .children(Collections.emptyList())
                        .build());
            } else {
                // 非叶子节点：递归检索子节点（Milvus 原生 filter: parent_node_id == nodeId）
                log.debug("  → Internal node, recursing into children via parent_node_id filter...");
                Filter childFilter = MetadataFilterBuilder.metadataKey("parent_node_id").isEqualTo(nodeId);
                LevelBundle childBundle = retrieveLevel(
                        queryVector, kbId, childFilter, beta, gamma, thetaMin, kMin, kMax);

                boolean hasChildren = !childBundle.results().isEmpty();
                if (hasChildren) {
                    log.debug("  → Got {} child results", childBundle.results().size());
                    finalResults.addAll(childBundle.results());
                } else {
                    // 子节点没有命中时，退化为当前节点
                    log.debug("  → No child results, falling back to current node");
                    finalResults.add(result);
                }

                treeNodes.add(RetrievalTreeNode.builder()
                        .nodeId(nodeId)
                        .titlePath(titlePath)
                        .score(result.getScore())
                        .passedThreshold(true)
                        .inResults(!hasChildren)   // fallback 时自身进入结果
                        .children(childBundle.treeNodes())
                        .build());
            }
        }

        log.debug("Level complete: {} final results, {} tree nodes", finalResults.size(), treeNodes.size());
        return new LevelBundle(finalResults, treeNodes);
    }

    // =====================================================================
    // 私有工具方法
    // =====================================================================

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
            log.debug("Threshold adjusted for kMin: {} passed < kMin={}, lowering to {}",
                    passCount, kMin, String.format("%.4f", sorted.get(kMin - 1)));
            result = sorted.get(kMin - 1);
        }

        // 限制最多 kMax 个结果（基于更新后的 result 重新计算）
        final double thetaForKMax = result;
        passCount = scores.stream().filter(s -> s >= thetaForKMax).count();
        if (passCount > kMax && sorted.size() >= kMax) {
            log.debug("Threshold adjusted for kMax: {} passed > kMax={}, raising to {}",
                    passCount, kMax, String.format("%.4f", sorted.get(kMax - 1)));
            result = sorted.get(kMax - 1);
        }

        log.debug("Final threshold: {}", String.format("%.4f", result));
        return result;
    }
}
