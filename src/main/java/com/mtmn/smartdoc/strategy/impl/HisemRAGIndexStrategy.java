package com.mtmn.smartdoc.strategy.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.config.HisemRAGConfig;
import com.mtmn.smartdoc.config.IndexStrategyConfig;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.enums.TreeNodeType;
import com.mtmn.smartdoc.exception.ConfigValidationException;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.Document;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.po.TreeNode;
import com.mtmn.smartdoc.repository.TreeNodeRepository;
import com.mtmn.smartdoc.service.MinioService;
import com.mtmn.smartdoc.strategy.IndexStrategy;
import com.mtmn.smartdoc.strategy.StorageStrategy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HisemRAG 索引策略实现（树结构）
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HisemRAGIndexStrategy implements IndexStrategy {

    // Markdown 标题正则
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private final TreeNodeRepository treeNodeRepository;
    private final MinioService minioService;
    private final StorageStrategy storageStrategy;
    private final ModelFactory modelFactory;
    private final ObjectMapper objectMapper;

    @Override
    public IndexStrategyType getStrategyType() {
        return IndexStrategyType.HISEM_RAG;
    }

    @Override
    @Transactional
    public IndexStructure parseDocument(Document document, IndexStrategyConfig config) {
        log.info("Parsing document with HisemRAG: documentId={}", document.getId());

        try {
            // 1. 验证配置
            validateConfig(config);
            HisemRAGConfig hisemConfig = objectMapper.convertValue(config, HisemRAGConfig.class);

            // 2. 从 MinIO 读取文档内容
            String content;
            try (InputStream inputStream = minioService.getFileStream(document.getFilePath())) {
                content = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }

            // 3. 解析文档章节
            List<DocumentSection> sections = parseDocumentSections(content);

            // 4. 构建树结构（返回所有节点）
            List<TreeNode> allNodes = buildTreeStructure(document, sections, hisemConfig);

            // 5. 删除旧节点并保存新树
            treeNodeRepository.deleteByDocumentId(document.getId());
            treeNodeRepository.saveAll(allNodes);

            log.info("Document parsed successfully with tree structure: documentId={}",
                    document.getId());

            // 6. 返回索引结构（用于预览）
            HisemRAGStructure structure = new HisemRAGStructure();
            structure.setRootNode(convertToNodeData(allNodes.get(0)));  // 第一个节点是根节点
            structure.setTotalNodes(allNodes.size());

            return structure;

        } catch (Exception e) {
            log.error("Failed to parse document: documentId={}", document.getId(), e);
            throw new RuntimeException("Document parsing failed", e);
        }
    }

    @Override
    @Transactional
    public void buildIndex(KnowledgeBase kb, Document document, IndexStructure structure) {
        log.info("Building index for document: documentId={}", document.getId());

        try {
            // 1. 获取文档的树节点
            List<TreeNode> nodes = treeNodeRepository.findByDocumentIdOrderByPathAsc(document.getId());

            // 2. 准备向量数据
            List<StorageStrategy.VectorItem> vectorItems = new ArrayList<>();
            EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(kb.getEmbeddingModelId());

            HisemRAGConfig hisemConfig = objectMapper.convertValue(
                    kb.getIndexStrategyConfig(), HisemRAGConfig.class);

            // 3. 如果启用摘要，使用 LLM 生成摘要
            LLMClient llmClient = null;
            if (hisemConfig.getEnableSummary()) {
                String llmModelId = hisemConfig.getLlmModelId();
                if (llmModelId != null && !llmModelId.isEmpty()) {
                    llmClient = modelFactory.createLLMClient(llmModelId);
                }
            }

            for (TreeNode node : nodes) {
                // 生成摘要（如果启用）
                if (hisemConfig.getEnableSummary() && llmClient != null) {
                    String summary = generateSummary(llmClient, node.getContent());
                    node.setSummary(summary);
                }

                // 生成 content 的向量
                List<Float> contentEmbedding = embeddingClient.embed(node.getContent());
                String contentVectorId = UUID.randomUUID().toString();

                StorageStrategy.VectorItem contentItem = new StorageStrategy.VectorItem();
                contentItem.setId(contentVectorId);
                contentItem.setVector(contentEmbedding);
                contentItem.setContent(node.getContent());

                Map<String, Object> contentMetadata = new HashMap<>();
                contentMetadata.put("node_id", node.getId());
                contentMetadata.put("document_id", document.getId());
                contentMetadata.put("kb_id", kb.getId());
                contentMetadata.put("node_type", node.getNodeType().name());
                contentMetadata.put("vector_type", "content");
                contentMetadata.put("title_path", node.getTitlePath());
                contentItem.setMetadata(contentMetadata);

                vectorItems.add(contentItem);

                // 更新节点的 vectorIds (JSON 格式)
                List<String> vectorIdList = parseVectorIds(node.getVectorIds());
                vectorIdList.add(contentVectorId);
                node.setVectorIds(serializeVectorIds(vectorIdList));

                // 如果有摘要，也生成摘要的向量
                if (node.getSummary() != null && !node.getSummary().isEmpty()) {
                    List<Float> summaryEmbedding = embeddingClient.embed(node.getSummary());
                    String summaryVectorId = UUID.randomUUID().toString();

                    StorageStrategy.VectorItem summaryItem = new StorageStrategy.VectorItem();
                    summaryItem.setId(summaryVectorId);
                    summaryItem.setVector(summaryEmbedding);
                    summaryItem.setContent(node.getSummary());

                    Map<String, Object> summaryMetadata = new HashMap<>();
                    summaryMetadata.put("node_id", node.getId());
                    summaryMetadata.put("document_id", document.getId());
                    summaryMetadata.put("kb_id", kb.getId());
                    summaryMetadata.put("node_type", node.getNodeType().name());
                    summaryMetadata.put("vector_type", "summary");
                    summaryMetadata.put("title_path", node.getTitlePath());
                    summaryItem.setMetadata(summaryMetadata);

                    vectorItems.add(summaryItem);

                    vectorIdList.add(summaryVectorId);
                    node.setVectorIds(serializeVectorIds(vectorIdList));
                }
            }

            // 4. 批量存储向量
            StorageStrategy.IndexData indexData = new StorageStrategy.IndexData();
            indexData.setVectors(vectorItems);

            Map<String, Object> globalMetadata = new HashMap<>();
            globalMetadata.put("kb_id", kb.getId());
            globalMetadata.put("document_id", document.getId());
            indexData.setMetadata(globalMetadata);

            storageStrategy.storeIndex(String.valueOf(kb.getId()), indexData);

            // 5. 保存更新后的节点
            treeNodeRepository.saveAll(nodes);

            log.info("Index built successfully: documentId={}, nodes={}",
                    document.getId(), nodes.size());

        } catch (Exception e) {
            log.error("Failed to build index: documentId={}", document.getId(), e);
            throw new RuntimeException("Index building failed", e);
        }
    }

    @Override
    @Transactional
    public void rebuildPartialIndex(KnowledgeBase kb, List<String> modifiedIds) {
        log.info("Rebuilding partial index: kbId={}, modifiedIds={}",
                kb.getId(), modifiedIds.size());

        try {
            List<Long> nodeIds = modifiedIds.stream()
                    .map(Long::valueOf)
                    .toList();

            List<TreeNode> nodes = treeNodeRepository.findAllById(nodeIds);
            EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(kb.getEmbeddingModelId());

            HisemRAGConfig hisemConfig = objectMapper.convertValue(
                    kb.getIndexStrategyConfig(), HisemRAGConfig.class);

            LLMClient llmClient = null;
            if (hisemConfig.getEnableSummary()) {
                String llmModelId = hisemConfig.getLlmModelId();
                if (llmModelId != null && !llmModelId.isEmpty()) {
                    llmClient = modelFactory.createLLMClient(llmModelId);
                }
            }

            List<StorageStrategy.IndexUpdateItem> updateItems = new ArrayList<>();

            for (TreeNode node : nodes) {
                // 删除旧向量
                List<String> oldVectorIds = parseVectorIds(node.getVectorIds());
                for (String oldVectorId : oldVectorIds) {
                    StorageStrategy.IndexUpdateItem deleteItem = new StorageStrategy.IndexUpdateItem();
                    deleteItem.setId(oldVectorId);
                    deleteItem.setUpdateType(StorageStrategy.IndexUpdateItem.UpdateType.DELETE);
                    updateItems.add(deleteItem);
                }

                // 重新生成摘要
                if (hisemConfig.getEnableSummary() && llmClient != null) {
                    String summary = generateSummary(llmClient, node.getContent());
                    node.setSummary(summary);
                }

                // 重新生成向量
                List<Float> contentEmbedding = embeddingClient.embed(node.getContent());
                String newContentVectorId = UUID.randomUUID().toString();

                StorageStrategy.IndexUpdateItem contentItem = new StorageStrategy.IndexUpdateItem();
                contentItem.setId(newContentVectorId);
                contentItem.setVector(contentEmbedding);
                contentItem.setContent(node.getContent());

                Map<String, Object> contentMetadata = new HashMap<>();
                contentMetadata.put("node_id", node.getId());
                contentMetadata.put("document_id", node.getDocumentId());
                contentMetadata.put("kb_id", kb.getId());
                contentMetadata.put("vector_type", "content");
                contentItem.setMetadata(contentMetadata);

                contentItem.setUpdateType(StorageStrategy.IndexUpdateItem.UpdateType.ADD);
                updateItems.add(contentItem);

                List<String> newVectorIds = new ArrayList<>();
                newVectorIds.add(newContentVectorId);

                // 摘要向量
                if (node.getSummary() != null && !node.getSummary().isEmpty()) {
                    List<Float> summaryEmbedding = embeddingClient.embed(node.getSummary());
                    String newSummaryVectorId = UUID.randomUUID().toString();

                    StorageStrategy.IndexUpdateItem summaryItem = new StorageStrategy.IndexUpdateItem();
                    summaryItem.setId(newSummaryVectorId);
                    summaryItem.setVector(summaryEmbedding);
                    summaryItem.setContent(node.getSummary());

                    Map<String, Object> summaryMetadata = new HashMap<>();
                    summaryMetadata.put("node_id", node.getId());
                    summaryMetadata.put("document_id", node.getDocumentId());
                    summaryMetadata.put("kb_id", kb.getId());
                    summaryMetadata.put("vector_type", "summary");
                    summaryItem.setMetadata(summaryMetadata);

                    summaryItem.setUpdateType(StorageStrategy.IndexUpdateItem.UpdateType.ADD);
                    updateItems.add(summaryItem);

                    newVectorIds.add(newSummaryVectorId);
                }

                node.setVectorIds(serializeVectorIds(newVectorIds));
            }

            // 执行批量更新
            storageStrategy.updatePartialIndex(String.valueOf(kb.getId()), updateItems);
            treeNodeRepository.saveAll(nodes);

            log.info("Partial index rebuilt successfully: kbId={}, nodes={}",
                    kb.getId(), nodes.size());

        } catch (Exception e) {
            log.error("Failed to rebuild partial index: kbId={}", kb.getId(), e);
            throw new RuntimeException("Partial index rebuilding failed", e);
        }
    }

    @Override
    @Transactional
    public void rebuildFullIndex(KnowledgeBase kb) {
        log.info("Rebuilding full index: kbId={}", kb.getId());

        try {
            // 删除整个索引
            storageStrategy.deleteIndex(String.valueOf(kb.getId()));

            // 获取知识库的所有节点
            List<TreeNode> nodes = treeNodeRepository.findByKbId(kb.getId());

            if (nodes.isEmpty()) {
                log.info("No nodes to index for kbId={}", kb.getId());
                return;
            }

            // 重新生成所有向量（与 buildIndex 类似）
            List<StorageStrategy.VectorItem> vectorItems = new ArrayList<>();
            EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(kb.getEmbeddingModelId());

            HisemRAGConfig hisemConfig = objectMapper.convertValue(
                    kb.getIndexStrategyConfig(), HisemRAGConfig.class);

            LLMClient llmClient = null;
            if (hisemConfig.getEnableSummary()) {
                String llmModelId = hisemConfig.getLlmModelId();
                if (llmModelId != null && !llmModelId.isEmpty()) {
                    llmClient = modelFactory.createLLMClient(llmModelId);
                }
            }

            for (TreeNode node : nodes) {
                // 生成摘要
                if (hisemConfig.getEnableSummary() && llmClient != null) {
                    String summary = generateSummary(llmClient, node.getContent());
                    node.setSummary(summary);
                }

                // Content 向量
                List<Float> contentEmbedding = embeddingClient.embed(node.getContent());
                String contentVectorId = UUID.randomUUID().toString();

                StorageStrategy.VectorItem contentItem = new StorageStrategy.VectorItem();
                contentItem.setId(contentVectorId);
                contentItem.setVector(contentEmbedding);
                contentItem.setContent(node.getContent());

                Map<String, Object> contentMetadata = new HashMap<>();
                contentMetadata.put("node_id", node.getId());
                contentMetadata.put("document_id", node.getDocumentId());
                contentMetadata.put("kb_id", kb.getId());
                contentMetadata.put("vector_type", "content");
                contentItem.setMetadata(contentMetadata);

                vectorItems.add(contentItem);

                List<String> vectorIdList = new ArrayList<>();
                vectorIdList.add(contentVectorId);

                // Summary 向量
                if (node.getSummary() != null && !node.getSummary().isEmpty()) {
                    List<Float> summaryEmbedding = embeddingClient.embed(node.getSummary());
                    String summaryVectorId = UUID.randomUUID().toString();

                    StorageStrategy.VectorItem summaryItem = new StorageStrategy.VectorItem();
                    summaryItem.setId(summaryVectorId);
                    summaryItem.setVector(summaryEmbedding);
                    summaryItem.setContent(node.getSummary());

                    Map<String, Object> summaryMetadata = new HashMap<>();
                    summaryMetadata.put("node_id", node.getId());
                    summaryMetadata.put("document_id", node.getDocumentId());
                    summaryMetadata.put("kb_id", kb.getId());
                    summaryMetadata.put("vector_type", "summary");
                    summaryItem.setMetadata(summaryMetadata);

                    vectorItems.add(summaryItem);
                    vectorIdList.add(summaryVectorId);
                }

                node.setVectorIds(serializeVectorIds(vectorIdList));
            }

            // 批量存储
            StorageStrategy.IndexData indexData = new StorageStrategy.IndexData();
            indexData.setVectors(vectorItems);

            Map<String, Object> globalMetadata = new HashMap<>();
            globalMetadata.put("kb_id", kb.getId());
            indexData.setMetadata(globalMetadata);

            storageStrategy.storeIndex(String.valueOf(kb.getId()), indexData);
            treeNodeRepository.saveAll(nodes);

            log.info("Full index rebuilt successfully: kbId={}, nodes={}",
                    kb.getId(), nodes.size());

        } catch (Exception e) {
            log.error("Failed to rebuild full index: kbId={}", kb.getId(), e);
            throw new RuntimeException("Full index rebuilding failed", e);
        }
    }

    @Override
    public void validateConfig(IndexStrategyConfig config) {
        if (!(config instanceof HisemRAGConfig)) {
            throw new ConfigValidationException("Invalid config type for HisemRAG");
        }

        HisemRAGConfig hisemConfig = (HisemRAGConfig) config;

        if (hisemConfig.getMaxTreeDepth() <= 0) {
            throw new ConfigValidationException("Max tree depth must be positive");
        }

        if (hisemConfig.getMaxTreeDepth() > 6) {
            throw new ConfigValidationException("Max tree depth cannot exceed 6 (Markdown header limit)");
        }
    }

    /**
     * 解析文档章节
     */
    private List<DocumentSection> parseDocumentSections(String content) {
        List<DocumentSection> sections = new ArrayList<>();
        Matcher matcher = HEADER_PATTERN.matcher(content);

        List<HeaderMatch> headers = new ArrayList<>();
        while (matcher.find()) {
            HeaderMatch header = new HeaderMatch();
            header.level = matcher.group(1).length();
            header.title = matcher.group(2).trim();
            header.startPos = matcher.start();
            headers.add(header);
        }

        // 为每个标题提取内容
        for (int i = 0; i < headers.size(); i++) {
            HeaderMatch header = headers.get(i);
            int contentStart = header.startPos;
            int contentEnd = (i < headers.size() - 1)
                    ? headers.get(i + 1).startPos
                    : content.length();

            String sectionContent = content.substring(contentStart, contentEnd).trim();

            DocumentSection section = new DocumentSection();
            section.setLevel(header.level);
            section.setTitle(header.title);
            section.setContent(sectionContent);
            sections.add(section);
        }

        return sections;
    }

    /**
     * 构建树结构（返回所有节点列表）
     */
    private List<TreeNode> buildTreeStructure(Document document, List<DocumentSection> sections,
                                              HisemRAGConfig config) {
        List<TreeNode> allNodes = new ArrayList<>();

        // 创建根节点
        TreeNode root = new TreeNode();
        root.setKbId(document.getKbId());
        root.setDocumentId(document.getId());
        root.setNodeType(TreeNodeType.ROOT);
        root.setNodeId(UUID.randomUUID().toString());
        root.setContent("Document: " + document.getFilename());
        root.setTitlePath("[\"" + document.getFilename() + "\"]");
        root.setLevel(0);
        root.setCreatedAt(LocalDateTime.now());
        root.setUpdatedAt(LocalDateTime.now());

        allNodes.add(root);

        // 使用栈来构建树
        List<TreeNode> stack = new ArrayList<>();
        stack.add(root);

        List<String> titlePathList = new ArrayList<>();
        titlePathList.add(document.getFilename());

        for (DocumentSection section : sections) {
            if (section.getLevel() > config.getMaxTreeDepth()) {
                continue; // 跳过超出最大深度的节点
            }

            TreeNode node = new TreeNode();
            node.setKbId(document.getKbId());
            node.setDocumentId(document.getId());
            node.setNodeId(UUID.randomUUID().toString());
            node.setContent(section.getContent());
            node.setLevel(section.getLevel());
            node.setCreatedAt(LocalDateTime.now());
            node.setUpdatedAt(LocalDateTime.now());

            // 找到父节点
            while (stack.size() > section.getLevel()) {
                stack.remove(stack.size() - 1);
                if (titlePathList.size() > section.getLevel()) {
                    titlePathList.remove(titlePathList.size() - 1);
                }
            }

            TreeNode parent = stack.get(stack.size() - 1);
            node.setParentNodeId(parent.getNodeId());

            // 构建 titlePath
            List<String> currentTitlePath = new ArrayList<>(titlePathList);
            currentTitlePath.add(section.getTitle());
            try {
                node.setTitlePath(objectMapper.writeValueAsString(currentTitlePath));
            } catch (Exception e) {
                log.error("Failed to serialize titlePath", e);
                node.setTitlePath("[]");
            }

            // 确定节点类型
            node.setNodeType(TreeNodeType.INTERNAL);

            // 添加到父节点的 childrenIds
            List<String> parentChildrenIds = parseChildrenIds(parent.getChildrenIds());
            parentChildrenIds.add(node.getNodeId());
            parent.setChildrenIds(serializeChildrenIds(parentChildrenIds));

            stack.add(node);
            allNodes.add(node);

            // 更新 titlePath 栈
            if (titlePathList.size() < section.getLevel()) {
                titlePathList.add(section.getTitle());
            }
        }

        return allNodes;
    }


    /**
     * 转换为节点数据（用于预览）
     */
    private NodeData convertToNodeData(TreeNode node) {
        NodeData data = new NodeData();
        try {
            List<String> titlePath = objectMapper.readValue(node.getTitlePath(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            data.setTitle(titlePath.isEmpty() ? "Root" : titlePath.get(titlePath.size() - 1));
        } catch (Exception e) {
            data.setTitle("Unknown");
        }
        data.setContent(node.getContent());
        data.setLevel(node.getLevel());
        data.setNodeType(node.getNodeType());

        // 不递归加载子节点，因为没有 @Transient children 字段
        // 需要时可以通过 childrenIds 查询

        return data;
    }

    /**
     * 使用 LLM 生成摘要
     */
    private String generateSummary(LLMClient llmClient, String content) {
        String prompt = "请为以下内容生成一个简洁的摘要（不超过200字）：\n\n" + content;

        try {
            return llmClient.chat(prompt);
        } catch (Exception e) {
            log.warn("Failed to generate summary, using truncated content", e);
            return content.substring(0, Math.min(200, content.length()));
        }
    }

    /**
     * 解析 vectorIds JSON 字符串
     */
    private List<String> parseVectorIds(String vectorIdsJson) {
        if (vectorIdsJson == null || vectorIdsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(vectorIdsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("Failed to parse vectorIds JSON: {}", vectorIdsJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 序列化 vectorIds 为 JSON 字符串
     */
    private String serializeVectorIds(List<String> vectorIds) {
        try {
            return objectMapper.writeValueAsString(vectorIds);
        } catch (Exception e) {
            log.error("Failed to serialize vectorIds", e);
            return "[]";
        }
    }

    /**
     * 解析 childrenIds JSON 字符串
     */
    private List<String> parseChildrenIds(String childrenIdsJson) {
        if (childrenIdsJson == null || childrenIdsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(childrenIdsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.warn("Failed to parse childrenIds JSON: {}", childrenIdsJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * 序列化 childrenIds 为 JSON 字符串
     */
    private String serializeChildrenIds(List<String> childrenIds) {
        try {
            return objectMapper.writeValueAsString(childrenIds);
        } catch (Exception e) {
            log.error("Failed to serialize childrenIds", e);
            return "[]";
        }
    }

    /**
     * 标题匹配类
     */
    private static class HeaderMatch {
        int level;
        String title;
        int startPos;
    }

    /**
     * 文档章节类
     */
    @Data
    public static class DocumentSection {
        private int level;
        private String title;
        private String content;
    }

    /**
     * HisemRAG 索引结构
     */
    @Data
    public static class HisemRAGStructure implements IndexStructure {
        private NodeData rootNode;
        private int totalNodes;
    }

    /**
     * 节点数据（用于预览）
     */
    @Data
    public static class NodeData {
        private String title;
        private String content;
        private Integer level;
        private TreeNodeType nodeType;
        private List<NodeData> children;
    }
}