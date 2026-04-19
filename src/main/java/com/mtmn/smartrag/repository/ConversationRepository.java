package com.mtmn.smartrag.repository;

import com.mtmn.smartrag.po.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
     * 根据用户ID统计对话总轮次
     */
    long countByUserId(Long userId);

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
     * 分页查询会话列表（每个 session 仅返回最新一条）
     */
    @Query(value = """
            SELECT c.*
            FROM conversations c
            INNER JOIN (
            SELECT MAX(id) AS id
            FROM conversations
            WHERE kb_id = :kbId
              AND user_id = :userId
              AND session_id IS NOT NULL
              AND session_id <> ''
            GROUP BY session_id
            ) latest ON c.id = latest.id
            ORDER BY c.created_at DESC
            """,
            countQuery = """
                SELECT COUNT(DISTINCT session_id)
                FROM conversations
                WHERE kb_id = :kbId
                  AND user_id = :userId
                  AND session_id IS NOT NULL
                  AND session_id <> ''
                """,
            nativeQuery = true)
    Page<Conversation> findLatestSessionsByKbIdAndUserId(
            @Param("kbId") Long kbId,
            @Param("userId") Long userId,
            Pageable pageable);

    /**
     * 查询指定会话的全部轮次（按时间升序）
     */
    List<Conversation> findByKbIdAndUserIdAndSessionIdOrderByCreatedAtAsc(Long kbId, Long userId, String sessionId);

    /**
     * 统计会话轮次数量
     */
    long countByKbIdAndUserIdAndSessionId(Long kbId, Long userId, String sessionId);

    /**
     * 删除指定会话
     */
    long deleteByKbIdAndUserIdAndSessionId(Long kbId, Long userId, String sessionId);

    /**
     * 根据时间范围查询
     */
    List<Conversation> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 按天统计用户对话轮次
     */
    @Query(value = """
            SELECT DATE(c.created_at) AS day, COUNT(*) AS count
            FROM conversations c
            WHERE c.user_id = :userId
              AND c.created_at >= :start
              AND c.created_at < :end
            GROUP BY DATE(c.created_at)
            """, nativeQuery = true)
    List<Map<String, Object>> countDailyByUserIdBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * 删除知识库的所有对话历史
     */
    void deleteByKbId(Long kbId);
}