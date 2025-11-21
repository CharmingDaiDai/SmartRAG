package com.mtmn.smartdoc.repository;

import com.mtmn.smartdoc.po.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识库 Repository
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    /**
     * 根据用户ID查询知识库列表
     */
    List<KnowledgeBase> findByUserId(Long userId);

    /**
     * 根据用户ID和知识库ID查询
     */
    Optional<KnowledgeBase> findByIdAndUserId(Long id, Long userId);

    /**
     * 根据用户ID和名称查询
     */
    Optional<KnowledgeBase> findByUserIdAndName(Long userId, String name);

    /**
     * 检查知识库名称是否存在（同一用户）
     */
    boolean existsByUserIdAndName(Long userId, String name);
}