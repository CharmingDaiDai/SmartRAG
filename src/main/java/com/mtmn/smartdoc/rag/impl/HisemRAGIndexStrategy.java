package com.mtmn.smartdoc.rag.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.constants.AppConstants;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.enums.TreeNodeType;
import com.mtmn.smartdoc.model.client.EmbeddingClient;
import com.mtmn.smartdoc.model.dto.IndexUpdateItem;
import com.mtmn.smartdoc.model.dto.VectorItem;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.DocumentPo;
import com.mtmn.smartdoc.po.KnowledgeBase;
import com.mtmn.smartdoc.po.TreeNode;
import com.mtmn.smartdoc.rag.AbstractIndexStrategy;
import com.mtmn.smartdoc.rag.config.IndexStrategyConfig;
import com.mtmn.smartdoc.repository.TreeNodeRepository;
import com.mtmn.smartdoc.service.MilvusService;
import com.mtmn.smartdoc.service.MinioService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * HisemRAG 索引策略实现
 * 基于层次语义树的索引构建策略
 *
 * @author charmingdaidai
 * @version 3.0
 * @date 2025-11-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HisemRAGIndexStrategy extends AbstractIndexStrategy {

    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

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
        log.info("Building HisemRAG index for document: id={}, type={}", documentPo.getId(), getType());

        try {
            String content = readDocumentContent(documentPo);
            List<TextSegment> segments = processContent(content, config);
            persist(kb, segments, documentPo.getId());

            log.info("✅ HisemRAG index built successfully: documentId={}, segments={}", documentPo.getId(), segments.size());

        } catch (Exception e) {
            log.error("❌ Failed to build HisemRAG index: documentId={}", documentPo.getId(), e);
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
        // 解析文档章节
        List<DocumentSection> sections = parseDocumentSections(content);

        // 构建树结构（扁平化为 TextSegment 列表）
        List<TextSegment> segments = new ArrayList<>();
        
        for (int i = 0; i < sections.size(); i++) {
            DocumentSection section = sections.get(i);
            
            Metadata metadata = new Metadata()
                .put("level", section.level)
                .put("title", section.title)
                .put("section_index", i);
            
            segments.add(TextSegment.from(section.content, metadata));
        }

        return segments;
    }

    @Override
    protected void persist(KnowledgeBase kb, List<TextSegment> segments, Long documentId) {
        try {
            Long kbId = kb.getId();
            treeNodeRepository.deleteByDocumentId(documentId);

            List<TreeNode> treeNodes = buildTreeNodes(segments, kbId, documentId);
            List<TreeNode> savedNodes = treeNodeRepository.saveAll(treeNodes);
            vectorizeAndStore(savedNodes, kb, documentId);

            log.info("Persisted {} nodes for document: {}", savedNodes.size(), documentId);

        } catch (Exception e) {
            log.error("Failed to persist nodes: documentId={}", documentId, e);
            throw new RuntimeException("Failed to persist nodes: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteIndex(KnowledgeBase kb, List<Long> documentIds) {
        Long kbId = kb.getId();
        log.info("Deleting HisemRAG index for documents: documentIds={}, kbId={}", documentIds, kbId);

        try {
            // 1. 删除向量
            deleteVectorsByDocumentIds(kbId, documentIds);

            // 2. 删除数据库中的 TreeNodes
            for (Long docId : documentIds) {
                treeNodeRepository.deleteByDocumentId(docId);
            }

            log.info("✅ HisemRAG index deleted successfully for {} documents", documentIds.size());

        } catch (Exception e) {
            log.error("❌ Failed to delete HisemRAG index: documentIds={}, kbId={}", documentIds, kbId, e);
            throw new RuntimeException("HisemRAG index deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * @param kb 知识库对象
     */
    @Override
    public void deleteIndex(KnowledgeBase kb) {
        Long kbId = kb.getId();
        log.info("Deleting entire HisemRAG index for knowledge base: kbId={}", kbId);

        try {
            // 1. 删除向量库中的集合
            String collectionName = AppConstants.Milvus.CHUNKS_TEMPLATE.formatted(kbId);
            milvusService.dropCollection(collectionName);

            // 2. 删除数据库中的 TreeNodes
            treeNodeRepository.deleteByKbId(kbId);

            log.info("✅ Entire HisemRAG index deleted successfully for kbId={}", kbId);

        } catch (Exception e) {
            log.error("❌ Failed to delete entire HisemRAG index: kbId={}", kbId, e);
            throw new RuntimeException("HisemRAG index deletion failed: " + e.getMessage(), e);
        }
    }

    /**
     * 构建树节点
     */
    private List<TreeNode> buildTreeNodes(List<TextSegment> segments, Long kbId, Long documentId) {
        List<TreeNode> nodes = new ArrayList<>();

        TreeNode root = new TreeNode();
        root.setKbId(kbId);
        root.setDocumentId(documentId);
        root.setNodeType(TreeNodeType.ROOT);
        root.setNodeId(UUID.randomUUID().toString());
        root.setContent("Document Root");
        root.setTitlePath("[\"Root\"]");
        root.setLevel(0);
        root.setCreatedAt(LocalDateTime.now());
        root.setUpdatedAt(LocalDateTime.now());
        nodes.add(root);

        String parentNodeId = root.getNodeId();
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            Metadata metadata = segment.metadata();

            TreeNode node = new TreeNode();
            node.setKbId(kbId);
            node.setDocumentId(documentId);
            node.setNodeType(TreeNodeType.INTERNAL);
            node.setNodeId(UUID.randomUUID().toString());
            node.setParentNodeId(parentNodeId);
            node.setContent(segment.text());
            node.setLevel(metadata.getInteger("level") != null ? metadata.getInteger("level") : 1);
            node.setTitlePath("[\"" + metadata.getString("title") + "\"]");
            node.setCreatedAt(LocalDateTime.now());
            node.setUpdatedAt(LocalDateTime.now());

            nodes.add(node);
        }

        return nodes;
    }

    /**
     * 向量化并存储
     */
    private void vectorizeAndStore(List<TreeNode> nodes, KnowledgeBase kb, Long documentId) {
        log.info("Vectorizing {} nodes for document: {}", nodes.size(), documentId);

        Long kbId = kb.getId();

        try {
            String embeddingModelId = kb.getEmbeddingModelId();
            EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(embeddingModelId);

            List<VectorItem> vectorItems = new ArrayList<>();
            for (TreeNode node : nodes) {
                Embedding embedding = embeddingClient.embed(node.getContent());
                String vectorId = UUID.randomUUID().toString();

                VectorItem item = new VectorItem();
                item.setId(vectorId);
                item.setEmbedding(embedding);
                item.setContent(node.getContent());

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("node_id", node.getId());
                metadata.put("document_id", documentId);
                metadata.put("kb_id", kbId);
                metadata.put("level", node.getLevel());
                metadata.put("title_path", node.getTitlePath());
                item.setMetadata(metadata);

                vectorItems.add(item);
                node.setVectorIds("[\"" + vectorId + "\"]");
            }

            milvusService.store(kbId, vectorItems);
            treeNodeRepository.saveAll(nodes);

        } catch (Exception e) {
            log.error("Failed to vectorize nodes: documentId={}", documentId, e);
            throw new RuntimeException("Vectorization failed: " + e.getMessage(), e);
        }
    }

    /**
     * 删除向量数据
     */
    private void deleteVectorsByDocumentIds(Long kbId, List<Long> documentIds) {
        try {
            log.debug("Deleting vectors from collection: {}, documentIds: {}", kbId, documentIds);
            
            // 1. 查询关联的 TreeNodes
            List<TreeNode> nodes = treeNodeRepository.findByDocumentIdIn(documentIds);
            if (nodes.isEmpty()) {
                return;
            }

            // 2. 提取 vectorIds
            List<IndexUpdateItem> items = new ArrayList<>();
            for (TreeNode node : nodes) {
                String vectorIdsJson = node.getVectorIds();
                if (vectorIdsJson != null && !vectorIdsJson.isEmpty()) {
                    try {
                        List<String> vectorIds = objectMapper.readValue(vectorIdsJson, new TypeReference<List<String>>() {});
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

            // 3. 调用 MilvusService 删除向量
            if (!items.isEmpty()) {
                milvusService.update(kbId, items);
            }

        } catch (Exception e) {
            log.error("Failed to delete vectors: collection={}, documentIds={}", kbId, documentIds, e);
            throw new RuntimeException("Failed to delete vectors: " + e.getMessage(), e);
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

        for (int i = 0; i < headers.size(); i++) {
            HeaderMatch header = headers.get(i);
            int contentStart = header.startPos;
            int contentEnd = (i < headers.size() - 1)
                    ? headers.get(i + 1).startPos
                    : content.length();

            String sectionContent = content.substring(contentStart, contentEnd).trim();

            DocumentSection section = new DocumentSection();
            section.level = header.level;
            section.title = header.title;
            section.content = sectionContent;
            sections.add(section);
        }

        return sections;
    }

    // 内部数据结构
    private static class DocumentSection {
        int level;
        String title;
        String content;
    }

    private static class HeaderMatch {
        int level;
        String title;
        int startPos;
    }
}