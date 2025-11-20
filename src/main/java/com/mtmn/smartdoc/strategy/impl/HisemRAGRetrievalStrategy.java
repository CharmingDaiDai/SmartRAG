package com.mtmn.smartdoc.strategy.impl;

import com.mtmn.smartdoc.config.RetrievalConfig;
import com.mtmn.smartdoc.enums.EnhancementType;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.factory.EnhancementFactory;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.po.TreeNode;
import com.mtmn.smartdoc.repository.TreeNodeRepository;
import com.mtmn.smartdoc.strategy.Enhancement;
import com.mtmn.smartdoc.strategy.RetrievalStrategy;
import com.mtmn.smartdoc.strategy.StorageStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HisemRAG 检索策略
 * 基于层次树结构的向量检索
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HisemRAGRetrievalStrategy implements RetrievalStrategy {

    private final StorageStrategy storageStrategy;
    private final TreeNodeRepository treeNodeRepository;
    private final ModelFactory modelFactory;
    private final EnhancementFactory enhancementFactory;

    @Override
    public RetrievalResult retrieve(KnowledgeBase kb, String query, RetrievalConfig config) {
        long startTime = System.currentTimeMillis();

        log.info("HisemRAG retrieval started: kbId={}, query={}", kb.getId(), query);

        try {
            // 1. 应用查询增强
            String enhancedQuery = applyEnhancements(query, kb, config);

            // 2. 生成查询向量
            List<Float> queryVector = generateQueryVector(enhancedQuery, kb.getEmbeddingModelId());

            // 3. 向量检索（扩大检索范围以支持层次过滤）
            String collectionName = buildCollectionName(kb);
            int expandedTopK = Math.min(config.getTopK() * 3, config.getMaxResults());

            StorageStrategy.SearchRequest searchRequest = new StorageStrategy.SearchRequest();
            searchRequest.setQueryVector(queryVector);
            searchRequest.setTopK(expandedTopK);
            searchRequest.setThreshold(config.getThreshold() * 0.8);  // 降低阈值以获取更多候选

            List<StorageStrategy.SearchResult> searchResults = storageStrategy.search(
                    collectionName,
                    searchRequest
            );

            // 4. 层次化过滤和重排序
            List<RetrievedItem> items = hierarchicalFiltering(searchResults, kb.getId(), config.getTopK());

            // 5. 构建结果
            RetrievalResult result = new RetrievalResult();
            result.setItems(items);
            result.setTotalCount(items.size());
            result.setMaxScore(items.isEmpty() ? 0.0 : items.get(0).getScore());
            result.setRetrievalTimeMs(System.currentTimeMillis() - startTime);

            log.info("HisemRAG retrieval completed: found {} items, time={}ms",
                    items.size(), result.getRetrievalTimeMs());

            return result;

        } catch (Exception e) {
            log.error("HisemRAG retrieval failed: kbId={}, query={}", kb.getId(), query, e);
            throw new RuntimeException("Retrieval failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EnhancementType> getSupportedEnhancements() {
        return Arrays.asList(
                EnhancementType.QUERY_REWRITE,
                EnhancementType.HYDE
                // HisemRAG 不太适合 QUERY_DECOMPOSE，因为层次结构本身已经提供了结构化信息
        );
    }

    @Override
    public IndexStrategyType getStrategyType() {
        return IndexStrategyType.HISEM_RAG;
    }

    /**
     * 应用查询增强
     */
    private String applyEnhancements(String query, KnowledgeBase kb, RetrievalConfig config) {
        if (config.getEnhancements() == null || config.getEnhancements().isEmpty()) {
            return query;
        }

        String currentQuery = query;

        for (EnhancementType type : config.getEnhancements()) {
            if (!enhancementFactory.isEnhancementAvailable(type)) {
                log.warn("Enhancement not available: {}", type);
                continue;
            }

            try {
                Enhancement enhancement = enhancementFactory.getEnhancement(type);

                // 检查是否支持该增强
                if (!enhancement.supports(IndexStrategyType.HISEM_RAG)) {
                    log.debug("Enhancement {} not supported for HISEM_RAG", type);
                    continue;
                }

                // 构建增强上下文
                Enhancement.EnhancementContext context = new Enhancement.EnhancementContext();
                context.setKbId(kb.getId());
                context.setLlmModelId(config.getLlmModelId());
                context.setParams(config.getEnhancementParams());

                // 执行增强
                Enhancement.EnhancedQuery enhancedQuery = enhancement.enhance(currentQuery, context);

                // 使用增强后的查询
                if (enhancedQuery.getQuery() != null && !enhancedQuery.getQuery().isEmpty()) {
                    currentQuery = enhancedQuery.getQuery();
                }

                log.debug("Applied enhancement {}: original='{}', enhanced='{}'",
                        type, query, currentQuery);

            } catch (Exception e) {
                log.error("Enhancement failed: type={}", type, e);
                // 继续使用原查询
            }
        }

        return currentQuery;
    }

    /**
     * 生成查询向量
     */
    private List<Float> generateQueryVector(String query, String embeddingModelId) {
        try {
            EmbeddingClient client = modelFactory.createEmbeddingClient(embeddingModelId);
            return client.embed(query);
        } catch (Exception e) {
            log.error("Failed to generate query vector", e);
            throw new RuntimeException("Failed to generate query vector: " + e.getMessage(), e);
        }
    }

    /**
     * 构建集合名称
     */
    private String buildCollectionName(KnowledgeBase kb) {
        return "kb_" + kb.getId() + "_nodes";
    }

    /**
     * 层次化过滤和重排序
     * <p>
     * 策略：
     * 1. 优先返回叶子节点（最详细的内容）
     * 2. 如果叶子节点不够，再返回父节点（更概括的内容）
     * 3. 同一路径上只保留最相关的节点
     */
    private List<RetrievedItem> hierarchicalFiltering(
            List<StorageStrategy.SearchResult> searchResults,
            Long kbId,
            int topK) {

        if (searchResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 提取所有 node IDs（从 metadata 中获取）
        List<Long> nodeIds = searchResults.stream()
                .map(result -> {
                    Map<String, Object> metadata = result.getMetadata();
                    if (metadata != null && metadata.containsKey("node_id")) {
                        Object nodeId = metadata.get("node_id");
                        return nodeId instanceof Number ? ((Number) nodeId).longValue() : null;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 2. 批量查询数据库获取 node 信息
        List<TreeNode> nodes = treeNodeRepository.findAllById(nodeIds);

        // 3. 构建 ID -> Score 映射（使用 node 的数据库 ID）
        Map<Long, Double> scoreMap = new HashMap<>();
        for (StorageStrategy.SearchResult result : searchResults) {
            if (result.getMetadata() != null && result.getMetadata().containsKey("node_id")) {
                Object nodeId = result.getMetadata().get("node_id");
                if (nodeId instanceof Number) {
                    scoreMap.put(((Number) nodeId).longValue(), result.getScore());
                }
            }
        }

        // 4. 分离叶子节点和非叶子节点
        List<TreeNode> leafNodes = new ArrayList<>();
        List<TreeNode> nonLeafNodes = new ArrayList<>();

        for (TreeNode node : nodes) {
            if (isLeafNode(node)) {
                leafNodes.add(node);
            } else {
                nonLeafNodes.add(node);
            }
        }

        // 5. 按分数排序
        Comparator<TreeNode> scoreComparator = (n1, n2) -> {
            double score1 = scoreMap.getOrDefault(n1.getId(), 0.0);
            double score2 = scoreMap.getOrDefault(n2.getId(), 0.0);
            return Double.compare(score2, score1);  // 降序
        };

        leafNodes.sort(scoreComparator);
        nonLeafNodes.sort(scoreComparator);

        // 6. 选择结果（优先叶子节点）
        List<TreeNode> selectedNodes = new ArrayList<>();
        selectedNodes.addAll(leafNodes.stream().limit(topK).collect(Collectors.toList()));

        // 如果叶子节点不够，补充非叶子节点
        if (selectedNodes.size() < topK) {
            int remaining = topK - selectedNodes.size();
            selectedNodes.addAll(nonLeafNodes.stream().limit(remaining).collect(Collectors.toList()));
        }

        // 7. 转换为检索项
        return selectedNodes.stream()
                .map(node -> {
                    RetrievedItem item = new RetrievedItem();
                    item.setSourceId(String.valueOf(node.getId()));
                    item.setSourceType("node");
                    item.setScore(scoreMap.getOrDefault(node.getId(), 0.0));
                    item.setContent(node.getContent());

                    // 设置元数据
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("node_id", node.getNodeId());
                    metadata.put("level", node.getLevel());
                    metadata.put("title_path", node.getTitlePath());
                    metadata.put("document_id", node.getDocumentId());
                    metadata.put("is_leaf", isLeafNode(node));
                    metadata.put("node_type", node.getNodeType());
                    item.setMetadata(metadata);

                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 判断是否为叶子节点
     */
    private boolean isLeafNode(TreeNode node) {
        String childrenIds = node.getChildrenIds();
        return childrenIds == null || childrenIds.equals("[]") || childrenIds.trim().isEmpty();
    }
}