package com.mtmn.smartdoc.rag.retriever;

import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.client.SseEventBuilder;
import com.mtmn.smartdoc.model.config.RagProperties;
import com.mtmn.smartdoc.po.TreeNode;
import com.mtmn.smartdoc.repository.TreeNodeRepository;
import com.mtmn.smartdoc.service.MilvusService;
import com.mtmn.smartdoc.vo.RetrievalResult;
import dev.langchain4j.data.embedding.Embedding;
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
 *   <li>从最高层（level=1）开始向量搜索</li>
 *   <li>使用分布感知动态阈值公式筛选候选节点</li>
 *   <li>对通过阈值的非叶子节点递归检索其子节点</li>
 *   <li>最终返回所有命中的叶子节点内容</li>
 * </ol>
 *
 * <p>动态阈值公式：θ = β·sMax - (1 - γ·CV)·(sMax - μ)
 * 其中 CV = σ/μ 为变异系数，用于感知分数分布的离散程度。
 *
 * @author charmingdaidai
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdaptiveRetriever {

    private final MilvusService milvusService;
    private final TreeNodeRepository treeNodeRepository;
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

                List<RetrievalResult> queryResults = retrieveHierarchical(
                        queryVector, kbId, emitter, beta, gamma, thetaMin, kMin, kMax, 1);

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
     * 递归层级检索核心逻辑
     *
     * @param queryVector 查询向量
     * @param kbId        知识库 ID
     * @param emitter     SSE 推送器
     * @param beta        阈值参数 β
     * @param gamma       阈值参数 γ
     * @param thetaMin    最小阈值
     * @param kMin        最少保留数量
     * @param kMax        最多保留数量
     * @param level       当前检索层级（从 1 开始）
     * @return 命中的结果列表
     */
    private List<RetrievalResult> retrieveHierarchical(Embedding queryVector, Long kbId,
                                                        SseEmitter emitter,
                                                        double beta, double gamma,
                                                        double thetaMin, int kMin, int kMax,
                                                        int level) {
        log.debug("--- Hierarchical search at level {} ---", level);

        // 向量检索当前层级的所有节点（用 kMax 作为 topK 保证召回率，再用动态阈值筛选）
        List<RetrievalResult> candidates = milvusService.search(kbId, queryVector, kMax, 0.0);
        log.debug("Milvus returned {} total candidates for kbId={}", candidates.size(), kbId);

        if (candidates.isEmpty()) {
            log.debug("No candidates from Milvus, returning empty");
            return Collections.emptyList();
        }

        // 过滤：只保留当前层级的节点
        List<RetrievalResult> levelCandidates = candidates.stream()
                .filter(r -> {
                    Object lvl = r.getMetadata().get("level");
                    if (lvl == null) return false;
                    int nodeLevel = lvl instanceof Number ? ((Number) lvl).intValue() : -1;
                    return nodeLevel == level;
                })
                .toList();

        if (levelCandidates.isEmpty()) {
            log.debug("No candidates at level {} (total candidates across all levels: {})", level, candidates.size());
            return Collections.emptyList();
        }

        // 收集本层候选节点的 title_path 信息用于 SSE 展示
        List<String> candidateTitlePaths = levelCandidates.stream()
                .map(r -> {
                    Object tp = r.getMetadata().get("title_path");
                    return tp != null ? String.valueOf(tp) : "";
                })
                .filter(tp -> !tp.isBlank())
                .distinct()
                .toList();

        // 推送检索进度 SSE 事件 — 展示 title_path 而非层级号
        String thoughtMsg;
        if (!candidateTitlePaths.isEmpty()) {
            String pathsDisplay = candidateTitlePaths.size() <= 3
                    ? String.join("、", candidateTitlePaths)
                    : String.join("、", candidateTitlePaths.subList(0, 3)) + " 等";
            thoughtMsg = String.format("正在检索「%s」（%d 个候选节点）...", pathsDisplay, levelCandidates.size());
        } else {
            thoughtMsg = String.format("正在层级检索（第 %d 层：%d 个候选节点）...", level, levelCandidates.size());
        }
        SseEventBuilder.sendThoughtEvent(emitter, "processing", thoughtMsg, "search");

        // Debug：打印候选节点详情
        if (log.isDebugEnabled()) {
            log.debug("Level {} candidates ({}):", level, levelCandidates.size());
            for (RetrievalResult r : levelCandidates) {
                String tp = r.getMetadata() != null
                        ? String.valueOf(r.getMetadata().getOrDefault("title_path", ""))
                        : "";
                String nodeType = r.getMetadata() != null
                        ? String.valueOf(r.getMetadata().getOrDefault("node_type", ""))
                        : "";
                log.debug("  nodeId={}, score={}, titlePath='{}', nodeType={}",
                        r.getMetadata().getOrDefault("node_id", "?"),
                        String.format("%.4f", r.getScore()), tp, nodeType);
            }
        }

        // 计算动态阈值
        List<Double> scores = levelCandidates.stream()
                .map(RetrievalResult::getScore)
                .collect(Collectors.toList());
        double threshold = calculateDynamicThreshold(scores, beta, gamma, thetaMin, kMin, kMax);

        DoubleSummaryStatistics stats = scores.stream().mapToDouble(d -> d).summaryStatistics();
        log.debug("Level {} scores: min={}, max={}, avg={}, count={} → dynamicThreshold={}",
                level, String.format("%.4f", stats.getMin()), String.format("%.4f", stats.getMax()),
                String.format("%.4f", stats.getAverage()), stats.getCount(),
                String.format("%.4f", threshold));

        // 按阈值筛选
        List<RetrievalResult> passed = levelCandidates.stream()
                .filter(r -> r.getScore() >= threshold)
                .toList();

        log.debug("Level {} threshold filter: {} / {} candidates passed", level, passed.size(), levelCandidates.size());

        List<RetrievalResult> finalResults = new ArrayList<>();

        for (RetrievalResult result : passed) {
            Object nodeIdObj = result.getMetadata().get("node_id");
            String nodeId = nodeIdObj != null ? String.valueOf(nodeIdObj) : null;
            String titlePath = result.getMetadata() != null
                    ? String.valueOf(result.getMetadata().getOrDefault("title_path", ""))
                    : "";

            // 判断是否为叶子节点
            boolean isLeaf = isLeafNode(nodeId, kbId);
            log.debug("  Processing node: nodeId={}, titlePath='{}', score={}, isLeaf={}",
                    nodeId, titlePath, String.format("%.4f", result.getScore()), isLeaf);

            if (isLeaf || nodeId == null) {
                // 叶子节点直接加入结果
                log.debug("  → Leaf node added to results");
                finalResults.add(result);
            } else {
                // 非叶子节点：递归检索子节点
                log.debug("  → Internal node, recursing into children...");
                List<RetrievalResult> childResults = retrieveChildren(
                        nodeId, kbId, queryVector, emitter, beta, gamma, thetaMin, kMin, kMax, level + 1);

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

        log.debug("--- Level {} complete: {} final results ---", level, finalResults.size());
        return finalResults;
    }

    /**
     * 检索指定父节点的子节点
     */
    private List<RetrievalResult> retrieveChildren(String parentNodeId, Long kbId,
                                                    Embedding queryVector, SseEmitter emitter,
                                                    double beta, double gamma,
                                                    double thetaMin, int kMin, int kMax,
                                                    int nextLevel) {
        log.debug("Retrieving children of parentNode='{}' at level {}", parentNodeId, nextLevel);

        // 从 DB 获取子节点 ID 列表
        List<TreeNode> children = treeNodeRepository.findByKbIdAndParentNodeId(kbId, parentNodeId);
        if (children.isEmpty()) {
            log.debug("No children found in DB for parentNode='{}'", parentNodeId);
            return Collections.emptyList();
        }

        log.debug("Found {} children in DB for parentNode='{}': {}",
                children.size(), parentNodeId,
                children.stream().map(c -> c.getNodeId() + "(" + c.getTitlePath() + ")").collect(Collectors.joining(", ")));

        // 全量检索，然后过滤出属于这批子节点的结果
        List<RetrievalResult> candidates = milvusService.search(kbId, queryVector, kMax, 0.0);

        Set<String> childNodeIds = children.stream()
                .map(TreeNode::getNodeId)
                .collect(Collectors.toSet());

        List<RetrievalResult> childCandidates = candidates.stream()
                .filter(r -> {
                    Object nodeIdObj = r.getMetadata().get("node_id");
                    return nodeIdObj != null && childNodeIds.contains(String.valueOf(nodeIdObj));
                })
                .toList();

        if (childCandidates.isEmpty()) {
            log.debug("No matching child candidates from Milvus for parentNode='{}'", parentNodeId);
            return Collections.emptyList();
        }

        // 收集子节点的 title_path 信息用于 SSE 展示
        List<String> childTitlePaths = childCandidates.stream()
                .map(r -> {
                    Object tp = r.getMetadata().get("title_path");
                    return tp != null ? String.valueOf(tp) : "";
                })
                .filter(tp -> !tp.isBlank())
                .distinct()
                .toList();

        // 推送进度 — 展示 title_path
        String thoughtMsg;
        if (!childTitlePaths.isEmpty()) {
            String pathsDisplay = childTitlePaths.size() <= 3
                    ? String.join("、", childTitlePaths)
                    : String.join("、", childTitlePaths.subList(0, 3)) + " 等";
            thoughtMsg = String.format("正在深入检索「%s」（%d 个候选节点）...", pathsDisplay, childCandidates.size());
        } else {
            thoughtMsg = String.format("正在层级检索（第 %d 层：%d 个候选节点）...", nextLevel, childCandidates.size());
        }
        SseEventBuilder.sendThoughtEvent(emitter, "processing", thoughtMsg, "search");

        // Debug：打印子候选节点详情
        if (log.isDebugEnabled()) {
            log.debug("Child candidates for parentNode='{}' ({}):", parentNodeId, childCandidates.size());
            for (RetrievalResult r : childCandidates) {
                log.debug("  nodeId={}, score={}, titlePath='{}'",
                        r.getMetadata().getOrDefault("node_id", "?"),
                        String.format("%.4f", r.getScore()),
                        r.getMetadata().getOrDefault("title_path", ""));
            }
        }

        // 计算阈值并筛选
        List<Double> scores = childCandidates.stream()
                .map(RetrievalResult::getScore)
                .collect(Collectors.toList());
        double threshold = calculateDynamicThreshold(scores, beta, gamma, thetaMin, kMin, kMax);

        log.debug("Children threshold for parentNode='{}': {}, candidates: {}",
                parentNodeId, String.format("%.4f", threshold), childCandidates.size());

        List<RetrievalResult> passed = childCandidates.stream()
                .filter(r -> r.getScore() >= threshold)
                .toList();

        log.debug("Children threshold filter: {} / {} candidates passed", passed.size(), childCandidates.size());

        List<RetrievalResult> finalResults = new ArrayList<>();

        for (RetrievalResult result : passed) {
            Object nodeIdObj = result.getMetadata().get("node_id");
            String nodeId = nodeIdObj != null ? String.valueOf(nodeIdObj) : null;
            String titlePath = result.getMetadata() != null
                    ? String.valueOf(result.getMetadata().getOrDefault("title_path", ""))
                    : "";
            boolean isLeaf = isLeafNode(nodeId, kbId);

            log.debug("  Processing child node: nodeId={}, titlePath='{}', score={}, isLeaf={}",
                    nodeId, titlePath, String.format("%.4f", result.getScore()), isLeaf);

            if (isLeaf || nodeId == null) {
                finalResults.add(result);
            } else {
                List<RetrievalResult> grandChildResults = retrieveChildren(
                        nodeId, kbId, queryVector, emitter, beta, gamma, thetaMin, kMin, kMax, nextLevel + 1);
                if (grandChildResults.isEmpty()) {
                    log.debug("  → No grandchild results, falling back to current child node");
                    finalResults.add(result);
                } else {
                    log.debug("  → Got {} grandchild results", grandChildResults.size());
                    finalResults.addAll(grandChildResults);
                }
            }
        }

        return finalResults;
    }

    /**
     * 判断节点是否为叶子节点（无子节点）
     */
    private boolean isLeafNode(String nodeId, Long kbId) {
        if (nodeId == null) return true;
        List<TreeNode> children = treeNodeRepository.findByKbIdAndParentNodeId(kbId, nodeId);
        return children.isEmpty();
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