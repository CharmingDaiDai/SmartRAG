//package com.mtmn.smartrag.service.impl;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mtmn.smartrag.dto.ChunkPreviewResponse;
//import com.mtmn.smartrag.dto.TreeNodePreviewResponse;
//import com.mtmn.smartrag.exception.ResourceNotFoundException;
//import com.mtmn.smartrag.po.Chunk;
//import com.mtmn.smartrag.po.TreeNode;
//import com.mtmn.smartrag.repository.ChunkRepository;
//import com.mtmn.smartrag.repository.TreeNodeRepository;
//import com.mtmn.smartrag.service.ChunkPreviewService;
//import com.mtmn.smartrag.service.DocumentService;
//import com.mtmn.smartrag.service.KnowledgeBaseService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
/// **
// * Chunk 和 TreeNode 预览服务实现
// *
// * @author charmingdaidai
// * @version 2.0
// * @date 2025-11-19
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ChunkPreviewServiceImpl implements ChunkPreviewService {
//
//    private final ChunkRepository chunkRepository;
//    private final TreeNodeRepository treeNodeRepository;
//    private final DocumentService documentService;
//    private final KnowledgeBaseService knowledgeBaseService;
//    private final ObjectMapper objectMapper;
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ChunkPreviewResponse> listChunks(Long documentId, Long userId) {
//        log.debug("Listing chunks for document: {}", documentId);
//
//        // 验证文档权限
//        documentService.getDocumentEntity(documentId, userId);
//
//        List<Chunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(documentId);
//
//        return chunks.stream()
//                .map(this::convertChunkToResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ChunkPreviewResponse> listChunksByKb(Long kbId, Long userId, int page, int size) {
//        log.debug("Listing chunks for KB: {}, page: {}, size: {}", kbId, page, size);
//
//        // 验证知识库权限
//        knowledgeBaseService.getKnowledgeBaseEntity(kbId, userId);
//
//        Pageable pageable = PageRequest.of(page, size);
//        List<Chunk> chunks = chunkRepository.findByKbId(kbId, pageable);
//
//        return chunks.stream()
//                .map(this::convertChunkToResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public ChunkPreviewResponse getChunk(Long chunkId, Long userId) {
//        log.debug("Getting chunk: {}", chunkId);
//
//        Chunk chunk = chunkRepository.findById(chunkId)
//                .orElseThrow(() -> new ResourceNotFoundException("Chunk", chunkId));
//
//        // 验证知识库权限
//        knowledgeBaseService.getKnowledgeBaseEntity(chunk.getKbId(), userId);
//
//        return convertChunkToResponse(chunk);
//    }
//
//    @Override
//    @Transactional
//    public void updateChunk(Long chunkId, Long userId, String newContent) {
//        log.info("Updating chunk: {}", chunkId);
//
//        Chunk chunk = chunkRepository.findById(chunkId)
//                .orElseThrow(() -> new ResourceNotFoundException("Chunk", chunkId));
//
//        // 验证知识库权限
//        knowledgeBaseService.getKnowledgeBaseEntity(chunk.getKbId(), userId);
//
//        // 更新内容
//        chunk.setContent(newContent);
//        chunk.setIsModified(true);
//
//        chunkRepository.save(chunk);
//
//        log.info("Chunk updated successfully: {}", chunkId);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<TreeNodePreviewResponse> getTreeStructure(Long documentId, Long userId) {
//        log.debug("Getting tree structure for document: {}", documentId);
//
//        // 验证文档权限
//        documentService.getDocumentEntity(documentId, userId);
//
//        // 获取根节点
//        List<TreeNode> rootNodes = treeNodeRepository.findRootNodes(documentId);
//
//        return rootNodes.stream()
//                .map(node -> buildTreeNodeResponse(node, documentId))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<TreeNodePreviewResponse> getTreeStructureByKb(Long kbId, Long userId) {
//        log.debug("Getting tree structure for KB: {}", kbId);
//
//        // 验证知识库权限
//        knowledgeBaseService.getKnowledgeBaseEntity(kbId, userId);
//
//        // 获取知识库的所有根节点
//        List<TreeNode> rootNodes = treeNodeRepository.findByKbIdAndParentNodeId(kbId, null);
//
//        // 按文档分组构建树
//        Map<Long, List<TreeNode>> nodesByDocument = rootNodes.stream()
//                .collect(Collectors.groupingBy(TreeNode::getDocumentId));
//
//        List<TreeNodePreviewResponse> result = new ArrayList<>();
//        for (Map.Entry<Long, List<TreeNode>> entry : nodesByDocument.entrySet()) {
//            Long documentId = entry.getKey();
//            for (TreeNode node : entry.getValue()) {
//                result.add(buildTreeNodeResponse(node, documentId));
//            }
//        }
//
//        return result;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public TreeNodePreviewResponse getTreeNode(Long nodeId, Long userId) {
//        log.debug("Getting tree node: {}", nodeId);
//
//        TreeNode node = treeNodeRepository.findById(nodeId)
//                .orElseThrow(() -> new ResourceNotFoundException("TreeNode", nodeId));
//
//        // 验证知识库权限
//        knowledgeBaseService.getKnowledgeBaseEntity(node.getKbId(), userId);
//
//        return buildTreeNodeResponse(node, node.getDocumentId());
//    }
//
//    @Override
//    @Transactional
//    public void updateTreeNode(Long nodeId, Long userId, String newContent) {
//        log.info("Updating tree node: {}", nodeId);
//
//        TreeNode node = treeNodeRepository.findById(nodeId)
//                .orElseThrow(() -> new ResourceNotFoundException("TreeNode", nodeId));
//
//        // 验证知识库权限
//        knowledgeBaseService.getKnowledgeBaseEntity(node.getKbId(), userId);
//
//        // 更新内容
//        node.setContent(newContent);
//
//        treeNodeRepository.save(node);
//
//        log.info("TreeNode updated successfully: {}", nodeId);
//    }
//
//    /**
//     * 转换 Chunk 为响应 DTO
//     */
//    private ChunkPreviewResponse convertChunkToResponse(Chunk chunk) {
//        return ChunkPreviewResponse.builder()
//                .id(chunk.getId())
//                .documentId(chunk.getDocumentId())
//                .chunkIndex(chunk.getChunkIndex())
//                .content(chunk.getContent())
//                .isModified(chunk.getIsModified())
//                .isVectorized(chunk.getVectorId() != null)
//                .createdAt(chunk.getCreatedAt())
//                .updatedAt(chunk.getUpdatedAt())
//                .build();
//    }
//
//    /**
//     * 递归构建 TreeNode 响应 (包含子节点)
//     */
//    private TreeNodePreviewResponse buildTreeNodeResponse(TreeNode node, Long documentId) {
//        // 解析 titlePath
//        List<String> titlePath = parseJsonArray(node.getTitlePath());
//
//        // 解析 childrenIds
//        List<String> childrenIds = parseJsonArray(node.getChildrenIds());
//
//        // 解析 vectorIds
//        List<String> vectorIds = parseJsonArray(node.getVectorIds());
//
//        // 递归获取子节点
//        List<TreeNodePreviewResponse> children = new ArrayList<>();
//        if (childrenIds != null && !childrenIds.isEmpty()) {
//            for (String childId : childrenIds) {
//                treeNodeRepository.findByKbIdAndNodeId(node.getKbId(), childId)
//                        .ifPresent(childNode -> children.add(buildTreeNodeResponse(childNode, documentId)));
//            }
//        }
//
//        // 截断内容 (预览模式)
//        String contentPreview = node.getContent();
//        if (contentPreview != null && contentPreview.length() > 500) {
//            contentPreview = contentPreview.substring(0, 500) + "...";
//        }
//
//        return TreeNodePreviewResponse.builder()
//                .nodeId(node.getNodeId())
//                .parentNodeId(node.getParentNodeId())
//                .titlePath(titlePath)
//                .level(node.getLevel())
//                .nodeType(node.getNodeType())
//                .keyKnowledge(node.getKeyKnowledge())
//                .summary(node.getSummary())
//                .content(contentPreview)
//                .children(children)
//                .isVectorized(vectorIds != null && !vectorIds.isEmpty())
//                .build();
//    }
//
//    /**
//     * 解析 JSON 数组字符串
//     */
//    private List<String> parseJsonArray(String jsonArray) {
//        if (jsonArray == null || jsonArray.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        try {
//            return objectMapper.readValue(jsonArray, new TypeReference<List<String>>() {});
//        } catch (Exception e) {
//            log.warn("Failed to parse JSON array: {}", jsonArray, e);
//            return Collections.emptyList();
//        }
//    }
//}