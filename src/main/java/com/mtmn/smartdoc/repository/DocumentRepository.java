package com.mtmn.smartdoc.repository;

import com.mtmn.smartdoc.enums.DocumentIndexStatus;
import com.mtmn.smartdoc.po.DocumentPo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 文档 Repository
 *
 * @author charmingdaidai
 * @version 2.1
 * @date 2025-11-19
 */
@Repository
public interface DocumentRepository extends JpaRepository<DocumentPo, Long> {


    long countByUserId(Long userId);

    /**
     * 根据用户ID分页查询文档
     */
    Page<DocumentPo> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据知识库ID查询文档列表
     */
    List<DocumentPo> findByKbId(Long kbId);

    /**
     * 根据知识库ID分页查询文档
     */
    Page<DocumentPo> findByKbId(Long kbId, Pageable pageable);

    /**
     * 根据知识库ID和索引状态查询
     */
    List<DocumentPo> findByKbIdAndIndexStatus(Long kbId, DocumentIndexStatus indexStatus);

    /**
     * 根据知识库ID和多个索引状态查询
     */
    List<DocumentPo> findByKbIdAndIndexStatusIn(Long kbId, List<DocumentIndexStatus> indexStatuses);

    /**
     * 根据知识库ID统计文档数量
     */
    long countByKbId(Long kbId);

    /**
     * 根据知识库ID和索引状态统计
     */
    long countByKbIdAndIndexStatus(Long kbId, DocumentIndexStatus indexStatus);

    /**
     * 删除知识库下的所有文档
     */
    void deleteByKbId(Long kbId);

    /**
     * 【性能优化】批量统计多个知识库的文档总数
     * 一次查询替代 N 次查询，解决 N+1 问题
     *
     * @param kbIds 知识库ID列表
     * @return Map<知识库ID, 文档总数>
     */
    @Query("SELECT d.kbId as kbId, COUNT(d) as count " +
            "FROM DocumentPo d " +
            "WHERE d.kbId IN :kbIds " +
            "GROUP BY d.kbId")
    List<Map<String, Object>> countByKbIdIn(@Param("kbIds") List<Long> kbIds);

    /**
     * 【性能优化】批量统计多个知识库的已索引文档数
     * 一次查询替代 N 次查询
     *
     * @param kbIds  知识库ID列表
     * @param status 索引状态
     * @return Map<知识库ID, 已索引文档数>
     */
    @Query("SELECT d.kbId as kbId, COUNT(d) as count " +
            "FROM DocumentPo d " +
            "WHERE d.kbId IN :kbIds AND d.indexStatus = :status " +
            "GROUP BY d.kbId")
    List<Map<String, Object>> countByKbIdInAndIndexStatus(
            @Param("kbIds") List<Long> kbIds,
            @Param("status") DocumentIndexStatus status);
}