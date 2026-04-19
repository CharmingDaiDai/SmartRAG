package com.mtmn.smartrag.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartrag.model.client.EmbeddingClient;
import com.mtmn.smartrag.model.dto.IndexUpdateItem;
import com.mtmn.smartrag.model.factory.ModelFactory;
import com.mtmn.smartrag.po.Chunk;
import com.mtmn.smartrag.po.KnowledgeBase;
import com.mtmn.smartrag.po.TreeNode;
import com.mtmn.smartrag.repository.ChunkRepository;
import com.mtmn.smartrag.repository.KnowledgeBaseRepository;
import com.mtmn.smartrag.repository.TreeNodeRepository;
import com.mtmn.smartrag.service.ChunkManageService;
import com.mtmn.smartrag.service.IndexingService;
import com.mtmn.smartrag.service.MilvusService;
import com.mtmn.smartrag.vo.ChunkVO;
import com.mtmn.smartrag.vo.TreeNodeVO;
import dev.langchain4j.data.embedding.Embedding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 切分块查看与编辑服务实现
 *
 * @author charmingdaidai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkManageServiceImpl implements ChunkManageService {

    private final ChunkRepository chunkRepository;
    private final TreeNodeRepository treeNodeRepository;
    private final KnowledgeBaseRepository kbRepository;
    private final ModelFactory modelFactory;
    private final MilvusService milvusService;
    private final IndexingService indexingService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // =========================================================================
    // Chunk 相关
    // =========================================================================

    @Override
    public List<ChunkVO> listChunks(Long documentId, Long kbId) {
        List<Chunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
        return chunks.stream()
                .map(this::toChunkVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChunkVO updateChunk(Long chunkId, String newContent, Long kbId) {
        Chunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new IllegalArgumentException("Chunk not found: " + chunkId));

        // 1. 更新 DB
        chunk.setContent(newContent);
        chunk.setIsModified(true);
        chunkRepository.save(chunk);
        log.info("Chunk {} content updated in DB", chunkId);

        // 2. 重新向量化整个文档（从更新后的 DB chunks 重建索引）
        try {
            indexingService.rebuildDocumentIndexFromChunks(chunk.getDocumentId(), chunk.getKbId());
            log.info("Document {} index rebuilt from chunks after chunk {} update", chunk.getDocumentId(), chunkId);
        } catch (Exception e) {
            log.error("Failed to rebuild index for document {} after chunk {} update: {}",
                    chunk.getDocumentId(), chunkId, e.getMessage(), e);
            throw new RuntimeException("内容已更新，但索引重建失败：" + e.getMessage(), e);
        }

        return toChunkVO(chunk);
    }

    // =========================================================================
    // TreeNode 相关
    // =========================================================================

    @Override
    public List<TreeNodeVO> listTreeNodesAsTree(Long documentId, Long kbId) {
        List<TreeNode> all = treeNodeRepository.findByKbIdAndDocumentId(kbId, documentId);

        // 构建 nodeId → TreeNodeVO 映射
        Map<String, TreeNodeVO> voMap = new LinkedHashMap<>();
        for (TreeNode node : all) {
            voMap.put(node.getNodeId(), toTreeNodeVO(node));
        }

        // 按 parentNodeId 组装树形
        List<TreeNodeVO> roots = new ArrayList<>();
        for (TreeNode node : all) {
            TreeNodeVO vo = voMap.get(node.getNodeId());
            if (node.getParentNodeId() == null || node.getParentNodeId().isBlank()) {
                roots.add(vo);
            } else {
                TreeNodeVO parent = voMap.get(node.getParentNodeId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    // 父节点不在同文档内（异常情况），作为根节点处理
                    roots.add(vo);
                }
            }
        }

        return roots;
    }

    @Override
    @Transactional
    public void updateTreeNode(Long nodeDbId, String newContent, Long kbId) {
        TreeNode node = treeNodeRepository.findById(nodeDbId)
                .orElseThrow(() -> new IllegalArgumentException("TreeNode not found: " + nodeDbId));

        // 1. 更新 DB content
        node.setContent(newContent);
        treeNodeRepository.save(node);
        log.info("TreeNode {} content updated in DB", nodeDbId);

        // 2. 删除 Milvus 中的旧向量
        List<String> oldVectorIds = parseVectorIds(node.getVectorIds());
        if (!oldVectorIds.isEmpty()) {
            List<IndexUpdateItem> deleteItems = oldVectorIds.stream()
                    .map(vid -> {
                        IndexUpdateItem item = new IndexUpdateItem();
                        item.setId(vid);
                        item.setUpdateType(IndexUpdateItem.UpdateType.DELETE);
                        return item;
                    })
                    .collect(Collectors.toList());
            try {
                milvusService.update(node.getKbId(), deleteItems);
                log.info("Deleted {} old vectors for TreeNode {}", oldVectorIds.size(), nodeDbId);
            } catch (Exception e) {
                log.warn("Failed to delete old vectors for TreeNode {}: {}", nodeDbId, e.getMessage());
                // 继续尝试插入新向量
            }
        }

        // 3. 重新 embed 并插入新向量
        try {
            KnowledgeBase kb = kbRepository.findById(node.getKbId())
                    .orElseThrow(() -> new IllegalArgumentException("KnowledgeBase not found: " + node.getKbId()));
            EmbeddingClient embeddingClient = modelFactory.createEmbeddingClient(kb.getEmbeddingModelId());
            Embedding embedding = embeddingClient.embed(newContent);

            String newVectorId = UUID.randomUUID().toString();
            Map<String, Object> metadata = buildTreeNodeMetadata(node);

            IndexUpdateItem addItem = new IndexUpdateItem();
            addItem.setId(newVectorId);
            addItem.setEmbedding(embedding);
            addItem.setContent(newContent);
            addItem.setMetadata(metadata);
            addItem.setUpdateType(IndexUpdateItem.UpdateType.ADD);

            milvusService.update(node.getKbId(), List.of(addItem));
            log.info("Inserted new vector {} for TreeNode {}", newVectorId, nodeDbId);

            // 4. 更新 DB 中 vectorIds
            node.setVectorIds(OBJECT_MAPPER.writeValueAsString(List.of(newVectorId)));
            treeNodeRepository.save(node);
            log.info("TreeNode {} vectorIds updated to [{}]", nodeDbId, newVectorId);

        } catch (Exception e) {
            log.error("Failed to re-embed and update Milvus for TreeNode {}: {}", nodeDbId, e.getMessage(), e);
            throw new RuntimeException("内容已更新，但向量库同步失败：" + e.getMessage(), e);
        }
    }

    // =========================================================================
    // 私有工具方法
    // =========================================================================

    private ChunkVO toChunkVO(Chunk c) {
        return ChunkVO.builder()
                .id(c.getId())
                .kbId(c.getKbId())
                .documentId(c.getDocumentId())
                .chunkIndex(c.getChunkIndex())
                .content(c.getContent())
                .keyKnowledge(c.getKeyKnowledge())
                .summary(c.getSummary())
                .strategyType(c.getStrategyType())
                .build();
    }

    private TreeNodeVO toTreeNodeVO(TreeNode node) {
        return TreeNodeVO.builder()
                .id(node.getId())
                .nodeId(node.getNodeId())
                .parentNodeId(node.getParentNodeId())
                .titlePath(node.getTitlePath())
                .level(node.getLevel())
                .nodeType(node.getNodeType() != null ? node.getNodeType().name() : null)
                .content(node.getContent())
                .summary(node.getSummary())
                .children(new ArrayList<>())
                .build();
    }

    private List<String> parseVectorIds(String vectorIdsJson) {
        if (vectorIdsJson == null || vectorIdsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(vectorIdsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse vectorIds JSON '{}': {}", vectorIdsJson, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> buildTreeNodeMetadata(TreeNode node) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("node_id", node.getNodeId());
        metadata.put("parent_node_id", node.getParentNodeId() != null ? node.getParentNodeId() : "");
        metadata.put("level", node.getLevel());
        metadata.put("node_type", node.getNodeType() != null ? node.getNodeType().name() : "");
        metadata.put("title_path", node.getTitlePath() != null ? node.getTitlePath() : "");
        return metadata;
    }
}
