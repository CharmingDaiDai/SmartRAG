package com.mtmn.smartdoc.service;

import com.mtmn.smartdoc.dto.ChunkPreviewResponse;
import com.mtmn.smartdoc.dto.TreeNodePreviewResponse;

import java.util.List;

/**
 * Chunk 和 TreeNode 预览服务接口
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
public interface ChunkPreviewService {

    /**
     * 获取文档的 Chunk 列表 (用于 NaiveRAG)
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return Chunk 列表
     */
    List<ChunkPreviewResponse> listChunks(Long documentId, Long userId);

    /**
     * 获取知识库的所有 Chunk (分页)
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     * @param page   页码 (从0开始)
     * @param size   每页大小
     * @return Chunk 列表
     */
    List<ChunkPreviewResponse> listChunksByKb(Long kbId, Long userId, int page, int size);

    /**
     * 获取单个 Chunk 详情
     *
     * @param chunkId Chunk ID
     * @param userId  用户 ID
     * @return Chunk 详情
     */
    ChunkPreviewResponse getChunk(Long chunkId, Long userId);

    /**
     * 更新 Chunk 内容
     *
     * @param chunkId    Chunk ID
     * @param userId     用户 ID
     * @param newContent 新内容
     */
    void updateChunk(Long chunkId, Long userId, String newContent);

    /**
     * 获取文档的 TreeNode 树形结构 (用于 HisemRAG)
     *
     * @param documentId 文档 ID
     * @param userId     用户 ID
     * @return 树形结构 (根节点列表)
     */
    List<TreeNodePreviewResponse> getTreeStructure(Long documentId, Long userId);

    /**
     * 获取知识库的 TreeNode 树形结构
     *
     * @param kbId   知识库 ID
     * @param userId 用户 ID
     * @return 树形结构 (根节点列表)
     */
    List<TreeNodePreviewResponse> getTreeStructureByKb(Long kbId, Long userId);

    /**
     * 获取单个 TreeNode 详情
     *
     * @param nodeId TreeNode ID
     * @param userId 用户 ID
     * @return TreeNode 详情
     */
    TreeNodePreviewResponse getTreeNode(Long nodeId, Long userId);

    /**
     * 更新 TreeNode 内容
     *
     * @param nodeId     TreeNode ID
     * @param userId     用户 ID
     * @param newContent 新内容
     */
    void updateTreeNode(Long nodeId, Long userId, String newContent);
}