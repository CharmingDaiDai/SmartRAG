package com.mtmn.smartrag.po;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Conversation 对话历史实体类
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_kb_id", columnList = "kb_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_session", columnList = "session_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id")
    private String sessionId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String query;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(name = "retrieval_config", columnDefinition = "JSON")
    private String retrievalConfig;

    @Column(name = "llm_model_id")
    private String llmModelId;

    @Column(name = "rerank_model_id")
    private String rerankModelId;

    @Column(name = "retrieved_chunks", columnDefinition = "JSON")
    private String retrievedChunks;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}