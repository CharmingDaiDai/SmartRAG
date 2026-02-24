package com.mtmn.smartdoc.po;

import com.mtmn.smartdoc.enums.IndexingStep;
import com.mtmn.smartdoc.enums.IndexingTaskStatus;
import com.mtmn.smartdoc.enums.IndexingTaskType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 索引任务实体类
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "indexing_tasks")
public class IndexingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private IndexingTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IndexingTaskStatus status;

    @Column(name = "total_docs", nullable = false)
    private Integer totalDocs;

    @Column(name = "completed_docs", nullable = false)
    private Integer completedDocs;

    @Column(name = "failed_docs", nullable = false)
    private Integer failedDocs;

    @Column(name = "current_doc_id")
    private Long currentDocId;

    @Column(name = "current_doc_name")
    private String currentDocName;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", columnDefinition = "VARCHAR(50)")
    private IndexingStep currentStep;

    /** 当前细粒度步骤已处理条目数（LLM_ENRICHING / EMBEDDING 时有值） */
    @Column(name = "current_step_processed")
    private Integer currentStepProcessed;

    /** 当前细粒度步骤总条目数 */
    @Column(name = "current_step_total")
    private Integer currentStepTotal;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "document_ids", columnDefinition = "JSON", nullable = false)
    private String documentIds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = IndexingTaskStatus.PENDING;
        }
        if (totalDocs == null) {
            totalDocs = 0;
        }
        if (completedDocs == null) {
            completedDocs = 0;
        }
        if (failedDocs == null) {
            failedDocs = 0;
        }
    }
}
