package com.mtmn.smartrag.service;

import com.mtmn.smartrag.vo.ChunkVO;
import com.mtmn.smartrag.vo.TreeNodeVO;

import java.util.List;

/**
 * 切分块查看与编辑服务接口
 *
 * @author charmingdaidai
 */
public interface ChunkManageService {

    /**
     * 按 chunkIndex 升序返回文档的所有 chunk（NAIVE_RAG / HISEM_RAG_FAST）
     *
     * @param documentId 文档 ID
     * @param kbId       知识库 ID
     */
    List<ChunkVO> listChunks(Long documentId, Long kbId);

    /**
     * 更新 chunk 内容：DB 更新 + 重新向量化整个文档
     *
     * @param chunkId    Chunk DB 主键
     * @param newContent 新内容
     * @param kbId       知识库 ID
     */
    ChunkVO updateChunk(Long chunkId, String newContent, Long kbId);

    /**
     * 返回文档的 TreeNode 树形结构（HISEM_RAG）
     *
     * @param documentId 文档 ID
     * @param kbId       知识库 ID
     */
    List<TreeNodeVO> listTreeNodesAsTree(Long documentId, Long kbId);

    /**
     * 更新 TreeNode 内容：DB 更新 + 删除旧向量 + 重新 embed 并插入新向量
     *
     * @param nodeDbId   TreeNode DB 主键
     * @param newContent 新内容
     * @param kbId       知识库 ID
     */
    void updateTreeNode(Long nodeDbId, String newContent, Long kbId);
}
