package com.mtmn.smartdoc.repository;

import com.mtmn.smartdoc.enums.TreeNodeType;
import com.mtmn.smartdoc.po.TreeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TreeNode Repository
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Repository
public interface TreeNodeRepository extends JpaRepository<TreeNode, Long> {

    /**
     * 根据知识库ID和节点ID查询
     */
    Optional<TreeNode> findByKbIdAndNodeId(Long kbId, String nodeId);

    /**
     * 根据知识库ID查询所有节点
     */
    List<TreeNode> findByKbId(Long kbId);

    /**
     * 根据文档ID查询所有节点
     */
    List<TreeNode> findByDocumentId(Long documentId);

    /**
     * 根据文档ID查询所有节点（按路径排序）
     */
    List<TreeNode> findByDocumentIdOrderByTitlePathAsc(Long documentId);

    /**
     * 根据知识库ID和文档ID查询
     */
    List<TreeNode> findByKbIdAndDocumentId(Long kbId, Long documentId);

    /**
     * 根据父节点ID查询子节点
     */
    List<TreeNode> findByKbIdAndParentNodeId(Long kbId, String parentNodeId);

    /**
     * 根据层级查询节点
     */
    List<TreeNode> findByKbIdAndLevel(Long kbId, Integer level);

    /**
     * 根据节点类型查询
     */
    List<TreeNode> findByKbIdAndNodeType(Long kbId, TreeNodeType nodeType);

    /**
     * 查询根节点
     */
    @Query("SELECT t FROM TreeNode t WHERE t.kbId = ?1 AND t.parentNodeId IS NULL")
    List<TreeNode> findRootNodes(Long kbId);

    /**
     * 删除文档的所有树节点
     */
    void deleteByDocumentId(Long documentId);

    /**
     * 删除知识库的所有树节点
     */
    void deleteByKbId(Long kbId);

    List<TreeNode> findByDocumentIdIn(List<Long> documentIds);
}