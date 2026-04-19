package com.mtmn.smartrag.repository;

import com.mtmn.smartrag.po.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Chunk Repository
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Repository
public interface ChunkRepository extends JpaRepository<Chunk, Long> {

    /**
     * 根据知识库ID查询所有切分块
     */
    List<Chunk> findByKbId(Long kbId);

    /**
     * 根据文档ID查询切分块
     */
    List<Chunk> findByDocumentId(Long documentId);

    /**
     * 根据文档ID列表查询切分块
     */
    List<Chunk> findByDocumentIdIn(List<Long> documentIds);

    /**
     * 根据文档ID查询切分块（按索引排序）
     */
    List<Chunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    /**
     * 根据知识库ID和文档ID查询
     */
    List<Chunk> findByKbIdAndDocumentId(Long kbId, Long documentId);

    /**
     * 根据向量ID查询
     */
    Chunk findByVectorId(String vectorId);

    /**
     * 查询被修改的切分块
     */
    List<Chunk> findByKbIdAndIsModified(Long kbId, Boolean isModified);

    /**
     * 删除文档的所有切分块
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Chunk c WHERE c.documentId = ?1")
    void deleteByDocumentId(Long documentId);

    /**
     * 删除知识库的所有切分块
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Chunk c WHERE c.kbId = ?1")
    void deleteByKbId(Long kbId);
}