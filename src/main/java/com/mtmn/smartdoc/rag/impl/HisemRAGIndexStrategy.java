package com.mtmn.smartdoc.rag.impl;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.common.MyNode;
import com.mtmn.smartdoc.constants.AppConstants;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.enums.IndexingStep;
import com.mtmn.smartdoc.enums.TreeNodeType;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.model.dto.IndexUpdateItem;
import com.mtmn.smartdoc.model.dto.VectorItem;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.po.TreeNode;
import com.mtmn.smartdoc.rag.AbstractIndexStrategy;
import com.mtmn.smartdoc.rag.config.HisemRagIndexConfig;
import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;
import com.mtmn.smartdoc.repository.TreeNodeRepository;
import com.mtmn.smartdoc.service.IndexingProgressCallback;
import com.mtmn.smartdoc.service.MilvusService;
import com.mtmn.smartdoc.service.MinioService;
import com.mtmn.smartdoc.utils.LlmJsonUtils;
import com.mtmn.smartdoc.utils.MarkdownProcessor;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HisemRAG 完整版索引策略实现
 *
 * <p>基于层次语义树的索引构建策略，支持双向语义传递：
 * <ul>
 *   <li>自上而下路径增强：通过 MarkdownProcessor.buildTitlePaths() 为每个节点填充完整 tPath</li>
 *   <li>自下而上 LLM 摘要聚合（可选）：按配置开关提取叶子节点 keyKnowledge/summary，再向上聚合</li>
 * </ul>
 *
 * @author charmingdaidai
 * @version 4.0
 * @date 2025-11-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HisemRAGIndexStrategy extends AbstractIndexStrategy {

    private final TreeNodeRepository treeNodeRepository;
    private final MinioService minioService;
    private final MilvusService milvusService;
    private final ModelFactory modelFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public IndexStrategyType getType() {
        return IndexStrategyType.HISEM_RAG;
    }

    @Override
    public void buildIndex(KnowledgeBase kb, DocumentPo documentPo, IndexStrategyConfig config) {
        buildIndex(kb, documentPo, config, IndexingProgressCallback.NOOP);
    }

    @Override
    public void buildIndex(KnowledgeBase kb, DocumentPo documentPo, IndexStrategyConfig config,
                           IndexingProgressCallback callback) {
        log.info("Building HisemRAG (full) index for document: id={}", documentPo.getId());
        Long docId = documentPo.getId();
        String docName = documentPo.getFilename();

        HisemRagIndexConfig hisemConfig = (config instanceof HisemRagIndexConfig)
                ? (HisemRagIndexConfig) config
                : new HisemRagIndexConfig();

        try {
            // 1. 读取文档内容
            callback.onStepChanged(docId, docName, IndexingStep.READING);
            String content;
            try (InputStream inputStream = minioService.getFileStream(documentPo.getFilePath())) {
                content = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }

            // 2. 解析 Markdown 结构，构建 MyNode 树
            callback.onStepChanged(docId, docName, IndexingStep.PARSING);
            int maxLevel = hisemConfig.getMaxTreeDepth() != null ? hisemConfig.getMaxTreeDepth() : 5;
            int maxBlockSize = hisemConfig.getMaxLength() != null ? hisemConfig.getMaxLength() : 2048;
            String docTitle = docName != null ? docName : "Document";

            Map.Entry<MyNode, Map<String, MyNode>> parseResult =
                    MarkdownProcessor.parseMarkdownContent(content, docTitle, maxLevel, maxBlockSize);

            MyNode rootNode = parseResult.getKey();
            Map<String, MyNode> nodesDict = parseResult.getValue();

            log.info("Document parsed: {} nodes total", nodesDict.size());

            // 3. 自上而下路径增强 + 转换为 TreeNode 列表
            callback.onStepChanged(docId, docName, IndexingStep.TREE_BUILDING);
            if (Boolean.TRUE.equals(hisemConfig.getEnableTitleEnhancement())) {
                // 从一级标题开始构建路径，跳过根节点（文档名），避免 title_path 以文档名开头
                for (String childId : rootNode.getChildren()) {
                    if (nodesDict.containsKey(childId)) {
                        MarkdownProcessor.buildTitlePaths(nodesDict.get(childId), nodesDict, "");
                    }
                }
                log.info("Title paths built for all nodes");
            }

            List<TreeNode> treeNodes = convertMyNodesToTreeNodes(nodesDict, kb.getId(), docId);

            // 4. 自下而上 LLM 摘要聚合（可选）
            boolean enableLlm = Boolean.TRUE.equals(hisemConfig.getEnableSemanticCompression())
                    && hisemConfig.getLlmModelId() != null && !hisemConfig.getLlmModelId().isEmpty();

            if (enableLlm) {
                callback.onStepChanged(docId, docName, IndexingStep.LLM_ENRICHING);
                log.info("Starting bottom-up LLM semantic enrichment...");
                LLMClient llmClient = modelFactory.createLLMClient(hisemConfig.getLlmModelId());
                buildBottomUpSummary(treeNodes, llmClient, callback, docId, docName);
                log.info("LLM semantic enrichment complete");
            }

            // 5. 清除旧数据并持久化
            callback.onStepChanged(docId, docName, IndexingStep.SAVING);
            treeNodeRepository.deleteByDocumentId(docId);
            List<TreeNode> savedNodes = treeNodeRepository.saveAll(treeNodes);
            log.info("Saved {} TreeNodes to DB", savedNodes.size());

            // 6. 批量向量化并存储
            callback.onStepChanged(docId, docName, IndexingStep.EMBEDDING);
            batchVectorizeAndStore(savedNodes, kb, callback, docId, docName);

            log.info("HisemRAG (full) index built: documentId={}, nodes={}, llmEnriched={}", docId, savedNodes.size(), enableLlm);

        } catch (Exception e) {
            log.error("Failed to build HisemRAG index: documentId={}", docId, e);
            throw new RuntimeException("HisemRAG index building failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected String readDocumentContent(DocumentPo document) {
        try (InputStream inputStream = minioService.getFileStream(document.getFilePath())) {
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read document content: documentId={}", document.getId(), e);
            throw new RuntimeException("Failed to read document content", e);
        }
    }

    @Override
    protected List<TextSegment> processContent(String content, IndexStrategyConfig config) {
        // Not used in full HiSem strategy — buildIndex handles everything
        return Collections.emptyList();
    }

    @Override
    protected void persist(KnowledgeBase kb, List<TextSegment> segments, Long documentId) {
        // Not used in full HiSem strategy — buildIndex handles everything
    }

    /**
     * HiSem 没有独立的 chunk 表可以从中重建，重建索引等同于完整重新解析构建。
     */
    @Override
    public void rebuildIndexFromChunks(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config) {
        rebuildIndex(kb, document, config);
    }

    @Override
    public void rebuildIndexFromChunks(KnowledgeBase kb, DocumentPo document, IndexStrategyConfig config,
                                       IndexingProgressCallback callback) {
        rebuildIndex(kb, document, config, callback);
    }

    @Override
    public void deleteIndex(KnowledgeBase kb, List<Long> documentIds) {
        Long kbId = kb.getId();
        log.info("Deleting HisemRAG index for documents: documentIds={}, kbId={}", documentIds, kbId);

        try {
            deleteVectorsByDocumentIds(kbId, documentIds);
            for (Long docId : documentIds) {
                treeNodeRepository.deleteByDocumentId(docId);
            }
            log.info("HisemRAG index deleted for {} documents", documentIds.size());
        } catch (Exception e) {
            log.error("Failed to delete HisemRAG index: documentIds={}", documentIds, e);
            throw new RuntimeException("HisemRAG index deletion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteIndex(KnowledgeBase kb) {
        Long kbId = kb.getId();
        log.info("Deleting entire HisemRAG index for knowledge base: kbId={}", kbId);

        try {
            String collectionName = AppConstants.Milvus.CHUNKS_TEMPLATE.formatted(kbId);
            milvusService.dropCollection(collectionName);
            treeNodeRepository.deleteByKbId(kbId);
            log.info("Entire HisemRAG index deleted for kbId={}", kbId);
        } catch (Exception e) {
            log.error("Failed to delete entire HisemRAG index: kbId={}", kbId, e);
            throw new RuntimeException("HisemRAG index deletion failed: " + e.getMessage(), e);
        }
    }

    // ==================== 私有核心方法 ====================

    /**
     * 将 MyNode 字典转换为 TreeNode 列表，保留父子关系
     */
    private List<TreeNode> convertMyNodesToTreeNodes(Map<String, MyNode> nodesDict, Long kbId, Long documentId) {
        Map<String, TreeNode> nodeMap = new LinkedHashMap<>();

        for (Map.Entry<String, MyNode> entry : nodesDict.entrySet()) {
            MyNode myNode = entry.getValue();
            TreeNode treeNode = new TreeNode();
            treeNode.setKbId(kbId);
            treeNode.setDocumentId(documentId);
            treeNode.setNodeId(myNode.getId());
            treeNode.setLevel(myNode.getLevel());
            treeNode.setContent(myNode.getPageContent());
            treeNode.setTitlePath(myNode.getTitle() != null ? myNode.getTitle() : "");

            if (myNode.getChildren() != null && !myNode.getChildren().isEmpty()) {
                treeNode.setChildrenIds(toJsonArray(myNode.getChildren()));
                treeNode.setNodeType(TreeNodeType.INTERNAL);
            } else {
                treeNode.setNodeType(TreeNodeType.LEAF);
            }

            treeNode.setCreatedAt(LocalDateTime.now());
            treeNode.setUpdatedAt(LocalDateTime.now());
            nodeMap.put(myNode.getId(), treeNode);
        }

        // Second pass: set parentNodeId and identify root nodes
        for (Map.Entry<String, MyNode> entry : nodesDict.entrySet()) {
            MyNode myNode = entry.getValue();
            TreeNode treeNode = nodeMap.get(myNode.getId());
            if (myNode.getParentId() != null && nodeMap.containsKey(myNode.getParentId())) {
                treeNode.setParentNodeId(myNode.getParentId());
            } else if (myNode.getParentId() == null) {
                treeNode.setNodeType(TreeNodeType.ROOT);
            }
        }

        return new ArrayList<>(nodeMap.values());
    }

    /**
     * 自下而上 LLM 摘要聚合：从叶子到根，后序遍历
     */
    private void buildBottomUpSummary(List<TreeNode> allNodes, LLMClient llmClient,
                                      IndexingProgressCallback callback, Long docId, String docName) {
        Map<String, TreeNode> nodeById = allNodes.stream()
                .collect(Collectors.toMap(TreeNode::getNodeId, n -> n));

        int maxLevel = allNodes.stream().mapToInt(TreeNode::getLevel).max().orElse(0);
        int totalNodes = allNodes.size();
        AtomicInteger processedCount = new AtomicInteger(0);

        // Process level by level from deepest to shallowest
        for (int level = maxLevel; level >= 0; level--) {
            final int currentLevel = level;
            List<TreeNode> levelNodes = allNodes.stream()
                    .filter(n -> n.getLevel() == currentLevel)
                    .toList();

            List<CompletableFuture<Void>> futures = levelNodes.stream()
                    .map(node -> CompletableFuture.runAsync(() -> {
                        try {
                            boolean isLeaf = node.getNodeType() == TreeNodeType.LEAF
                                    || node.getChildrenIds() == null
                                    || node.getChildrenIds().isEmpty()
                                    || "[]".equals(node.getChildrenIds());

                            if (isLeaf) {
                                extractLeafKnowledge(node, llmClient);
                            } else {
                                aggregateParentKnowledge(node, nodeById, llmClient);
                            }
                        } catch (Exception e) {
                            log.warn("LLM enrichment failed for node {}: {}", node.getNodeId(), e.getMessage());
                        }
                        int current = processedCount.incrementAndGet();
                        callback.onSubStepProgress(docId, docName, IndexingStep.LLM_ENRICHING, current, totalNodes);
                    }))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.debug("Level {} enrichment complete ({} nodes)", level, levelNodes.size());
        }
    }

    /**
     * 叶子节点：调用 LLM 提取 keyKnowledge 和 summary
     */
    private void extractLeafKnowledge(TreeNode node, LLMClient llmClient) {
        String content = node.getContent();
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        String prompt = AppConstants.PromptTemplates.HISEM_LEAF_EXTRACTION
                .replace("{content}", content);
        String llmOutput = llmClient.chat(prompt);

        Map<String, Object> result = LlmJsonUtils.parseMap(llmOutput);
        if (result.isEmpty()) {
            log.warn("Empty LLM result for leaf node {}", node.getNodeId());
            return;
        }

        if (result.containsKey("keyKnowledge")) {
            Object kk = result.get("keyKnowledge");
            if (kk instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> kkList = (List<String>) kk;
                node.setKeyKnowledge(toJsonArray(kkList));
            }
        }

        if (result.containsKey("summary")) {
            node.setSummary(String.valueOf(result.get("summary")));
        }
    }

    /**
     * 非叶子节点：聚合子节点的 summary 和 keyKnowledge
     */
    private void aggregateParentKnowledge(TreeNode node, Map<String, TreeNode> nodeById,
                                          LLMClient llmClient) {
        List<String> childIds = parseJsonArray(node.getChildrenIds());
        if (childIds.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        boolean hasChildData = false;

        for (String childId : childIds) {
            TreeNode child = nodeById.get(childId);
            if (child == null) continue;

            String title = child.getTitlePath() != null ? child.getTitlePath() : childId;
            String summary = child.getSummary();
            String kk = child.getKeyKnowledge();

            if ((summary != null && !summary.isEmpty()) || (kk != null && !kk.isEmpty())) {
                sb.append("【").append(title).append("】\n");
                if (summary != null && !summary.isEmpty()) {
                    sb.append("摘要：").append(summary).append("\n");
                }
                if (kk != null && !kk.isEmpty()) {
                    sb.append("知识点：").append(kk).append("\n");
                }
                sb.append("\n");
                hasChildData = true;
            }
        }

        if (!hasChildData) {
            return;
        }

        String prompt = AppConstants.PromptTemplates.HISEM_PARENT_AGGREGATION
                .replace("{children_summaries}", sb.toString());
        String llmOutput = llmClient.chat(prompt);

        Map<String, Object> result = LlmJsonUtils.parseMap(llmOutput);
        if (result.isEmpty()) {
            return;
        }

        if (result.containsKey("keyKnowledge")) {
            Object kk = result.get("keyKnowledge");
            if (kk instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> kkList = (List<String>) kk;
                node.setKeyKnowledge(toJsonArray(kkList));
            }
        }

        if (result.containsKey("summary")) {
            node.setSummary(String.valueOf(result.get("summary")));
        }
    }

    /**
     * 批量向量化并存储到 Milvus
     * 嵌入向量 = titlePath + keyKnowledge + summary（无增强信息时退化为 content）
     * Milvus text = titlePath + content
     */
    private void batchVectorizeAndStore(List<TreeNode> nodes, KnowledgeBase kb,
                                        IndexingProgressCallback callback, Long docId, String docName) {
        log.info("Batch vectorizing {} nodes for kb={}", nodes.size(), kb.getId());

        Long kbId = kb.getId();
        EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(kb.getEmbeddingModelId());
        List<VectorItem> vectorItems = new ArrayList<>();
        int totalNodes = nodes.size();

        for (TreeNode node : nodes) {
            String embedText = buildEmbedText(node);
            if (embedText == null || embedText.isBlank()) {
                continue;
            }

            try {
                Embedding embedding = embeddingClient.embed(embedText);
                String vectorId = UUID.randomUUID().toString();

                VectorItem item = new VectorItem();
                item.setId(vectorId);
                item.setEmbedding(embedding);

                // Milvus text 字段 = titlePath + content
                String titlePath = node.getTitlePath();
                String rawContent = node.getContent();
                boolean hasTitlePath = titlePath != null && !titlePath.isBlank();
                boolean hasContent = rawContent != null && !rawContent.isBlank();
                if (hasTitlePath && hasContent) {
                    item.setContent(titlePath + "\n" + rawContent);
                } else if (hasContent) {
                    item.setContent(rawContent);
                } else {
                    item.setContent(embedText);
                }

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("node_id", node.getNodeId());
                metadata.put("document_id", node.getDocumentId());
                metadata.put("kb_id", kbId);
                metadata.put("level", node.getLevel());
                metadata.put("title_path", node.getTitlePath() != null ? node.getTitlePath() : "");
                metadata.put("node_type", node.getNodeType() != null ? node.getNodeType().name() : "UNKNOWN");
                metadata.put("parent_node_id", node.getParentNodeId() != null ? node.getParentNodeId() : "");
                item.setMetadata(metadata);

                vectorItems.add(item);
                node.setVectorIds("[\"" + vectorId + "\"]");

                callback.onSubStepProgress(docId, docName, IndexingStep.EMBEDDING, vectorItems.size(), totalNodes);

            } catch (Exception e) {
                log.warn("Failed to vectorize node {}: {}", node.getNodeId(), e.getMessage());
            }
        }

        if (!vectorItems.isEmpty()) {
            milvusService.store(kbId, vectorItems);
            treeNodeRepository.saveAll(nodes);
            log.info("Stored {} vectors to Milvus for kb={}", vectorItems.size(), kbId);
        }
    }

    /**
     * 构建嵌入文本：titlePath + keyKnowledge + summary；退化时用 content
     */
    private String buildEmbedText(TreeNode node) {
        StringBuilder sb = new StringBuilder();

        if (node.getTitlePath() != null && !node.getTitlePath().isEmpty()) {
            sb.append(node.getTitlePath()).append("\n");
        }

        String kk = node.getKeyKnowledge();
        if (kk != null && !kk.isEmpty() && !"[]".equals(kk)) {
            List<String> kkList = parseJsonArray(kk);
            if (!kkList.isEmpty()) {
                sb.append(String.join("; ", kkList)).append("\n");
            }
        }

        if (node.getSummary() != null && !node.getSummary().isEmpty()) {
            sb.append(node.getSummary()).append("\n");
        }

        String result = sb.toString().trim();
        if (result.isEmpty()) {
            String content = node.getContent();
            return (content != null && !content.isBlank()) ? content : null;
        }

        return result;
    }

    /**
     * 删除向量数据
     */
    private void deleteVectorsByDocumentIds(Long kbId, List<Long> documentIds) {
        List<TreeNode> nodes = treeNodeRepository.findByDocumentIdIn(documentIds);
        if (nodes.isEmpty()) {
            return;
        }

        List<IndexUpdateItem> items = new ArrayList<>();
        for (TreeNode node : nodes) {
            String vectorIdsJson = node.getVectorIds();
            if (vectorIdsJson != null && !vectorIdsJson.isEmpty()) {
                try {
                    List<String> vectorIds = objectMapper.readValue(vectorIdsJson, new TypeReference<List<String>>() {
                    });
                    for (String vectorId : vectorIds) {
                        IndexUpdateItem item = new IndexUpdateItem();
                        item.setId(vectorId);
                        item.setUpdateType(IndexUpdateItem.UpdateType.DELETE);
                        items.add(item);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse vectorIds for node: {}", node.getId(), e);
                }
            }
        }

        if (!items.isEmpty()) {
            milvusService.update(kbId, items);
        }
    }

    // ==================== 工具方法 ====================

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}