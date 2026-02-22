package com.mtmn.smartdoc.rag.retriever;

import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.client.SseEventBuilder;
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

    // 默认阈值参数（可通过配置覆盖）
    private static final double DEFAULT_BETA = 0.9;
    private static final double DEFAULT_GAMMA = 0.8;
    private static final double DEFAULT_THETA_MIN = 0.3;
    private static final int DEFAULT_K_MIN = 1;
    private static final int DEFAULT_K_MAX = 20;
    private static final int DEFAULT_TOP_K = 50;

    private final MilvusService milvusService;
    private final TreeNodeRepository treeNodeRepository;

    /**
     * 执行自适应阈值层级检索
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
        return retrieve(queries, kbId, embeddingClient, emitter,
                DEFAULT_BETA, DEFAULT_GAMMA, DEFAULT_THETA_MIN, DEFAULT_K_MIN, DEFAULT_K_MAX);
    }

    /**
     * 执行自适应阈值层级检索（完整参数版）
     */
    public List<RetrievalResult> retrieve(Set<String> queries, Long kbId,
                                           EmbeddingClient embeddingClient,
                                           SseEmitter emitter,
                                           double beta, double gamma,
                                           double thetaMin, int kMin, int kMax) {
        Map<String, RetrievalResult> uniqueResults = new LinkedHashMap<>();

        for (String query : queries) {
            try {
                Embedding queryVector = embeddingClient.embed(query);
                List<RetrievalResult> queryResults = retrieveHierarchical(
                        queryVector, kbId, emitter, beta, gamma, thetaMin, kMin, kMax, 1);

                for (RetrievalResult result : queryResults) {
                    String key = result.getSourceId();
                    if (!uniqueResults.containsKey(key) ||
                            result.getScore() > uniqueResults.get(key).getScore()) {
                        uniqueResults.put(key, result);
                    }
                }
            } catch (Exception e) {
                log.error("Adaptive retrieval failed for query '{}': {}", query, e.getMessage());
            }
        }

        return uniqueResults.values().stream()
                .sorted(Comparator.comparingDouble(RetrievalResult::getScore).reversed())
                .collect(Collectors.toList());
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
        // 向量检索当前层级的所有节点（用大 topK 保证召回率，再用动态阈值筛选）
        List<RetrievalResult> candidates = milvusService.search(kbId, queryVector, DEFAULT_TOP_K, 0.0);

        if (candidates.isEmpty()) {
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
            log.debug("No candidates found at level {}", level);
            return Collections.emptyList();
        }

        // 推送检索进度 SSE 事件
        String thoughtMsg = String.format("正在层级检索（第 %d 层：%d 个候选节点）...", level, levelCandidates.size());
        SseEventBuilder.sendThoughtEvent(emitter, "processing", thoughtMsg, "search");
        log.debug("Level {} search: {} candidates", level, levelCandidates.size());

        // 计算动态阈值
        List<Double> scores = levelCandidates.stream()
                .map(RetrievalResult::getScore)
                .collect(Collectors.toList());
        double threshold = calculateDynamicThreshold(scores, beta, gamma, thetaMin, kMin, kMax);

        log.debug("Level {} dynamic threshold: {}", level, String.format("%.4f", threshold));

        // 按阈值筛选
        List<RetrievalResult> passed = levelCandidates.stream()
                .filter(r -> r.getScore() >= threshold)
                .toList();

        List<RetrievalResult> finalResults = new ArrayList<>();

        for (RetrievalResult result : passed) {
            Object nodeIdObj = result.getMetadata().get("node_id");
            String nodeId = nodeIdObj != null ? String.valueOf(nodeIdObj) : null;

            // 判断是否为叶子节点
            boolean isLeaf = isLeafNode(nodeId, kbId);

            if (isLeaf || nodeId == null) {
                // 叶子节点直接加入结果
                finalResults.add(result);
            } else {
                // 非叶子节点：递归检索子节点
                List<RetrievalResult> childResults = retrieveChildren(
                        nodeId, kbId, queryVector, emitter, beta, gamma, thetaMin, kMin, kMax, level + 1);

                if (childResults.isEmpty()) {
                    // 子节点没有命中时，退化为当前节点
                    finalResults.add(result);
                } else {
                    finalResults.addAll(childResults);
                }
            }
        }

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
        // 从 DB 获取子节点 ID 列表
        List<TreeNode> children = treeNodeRepository.findByKbIdAndParentNodeId(kbId, parentNodeId);
        if (children.isEmpty()) {
            return Collections.emptyList();
        }

        // 全量检索，然后过滤出属于这批子节点的结果
        List<RetrievalResult> candidates = milvusService.search(kbId, queryVector, DEFAULT_TOP_K, 0.0);

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
            return Collections.emptyList();
        }

        // 推送进度
        String thoughtMsg = String.format("正在层级检索（第 %d 层：%d 个候选节点）...", nextLevel, childCandidates.size());
        SseEventBuilder.sendThoughtEvent(emitter, "processing", thoughtMsg, "search");

        // 计算阈值并筛选
        List<Double> scores = childCandidates.stream()
                .map(RetrievalResult::getScore)
                .collect(Collectors.toList());
        double threshold = calculateDynamicThreshold(scores, beta, gamma, thetaMin, kMin, kMax);

        List<RetrievalResult> passed = childCandidates.stream()
                .filter(r -> r.getScore() >= threshold)
                .toList();

        List<RetrievalResult> finalResults = new ArrayList<>();

        for (RetrievalResult result : passed) {
            Object nodeIdObj = result.getMetadata().get("node_id");
            String nodeId = nodeIdObj != null ? String.valueOf(nodeIdObj) : null;
            boolean isLeaf = isLeafNode(nodeId, kbId);

            if (isLeaf || nodeId == null) {
                finalResults.add(result);
            } else {
                List<RetrievalResult> grandChildResults = retrieveChildren(
                        nodeId, kbId, queryVector, emitter, beta, gamma, thetaMin, kMin, kMax, nextLevel + 1);
                if (grandChildResults.isEmpty()) {
                    finalResults.add(result);
                } else {
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
            result = sorted.get(kMin - 1);
        }

        // 限制最多 kMax 个结果（基于更新后的 result 重新计算）
        final double thetaForKMax = result;
        passCount = scores.stream().filter(s -> s >= thetaForKMax).count();
        if (passCount > kMax && sorted.size() >= kMax) {
            result = sorted.get(kMax - 1);
        }

        return result;
    }
}