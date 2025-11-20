package com.mtmn.smartdoc.repository;

import com.mtmn.smartdoc.po.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Conversation Repository
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * 根据知识库ID查询对话历史
     */
    List<Conversation> findByKbId(Long kbId);

    /**
     * 根据用户ID查询对话历史
     */
    List<Conversation> findByUserId(Long userId);

    /**
     * 根据会话ID查询对话历史
     */
    List<Conversation> findBySessionId(String sessionId);

    /**
     * 根据知识库ID和用户ID查询
     */
    List<Conversation> findByKbIdAndUserId(Long kbId, Long userId);

    /**
     * 根据知识库ID和会话ID查询
     */
    List<Conversation> findByKbIdAndSessionId(Long kbId, String sessionId);

    /**
     * 根据时间范围查询
     */
    List<Conversation> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 删除知识库的所有对话历史
     */
    void deleteByKbId(Long kbId);
}