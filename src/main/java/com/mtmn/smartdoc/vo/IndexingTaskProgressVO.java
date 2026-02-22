package com.mtmn.smartdoc.vo;

import com.mtmn.smartdoc.po.IndexingTask;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 索引任务进度 VO（供前端轮询接口使用）
 *
 * @author charmingdaidai
 * @version 1.0
 */
@Data
public class IndexingTaskProgressVO {

    private Long taskId;
    private String status;
    private Integer totalDocs;
    private Integer completedDocs;
    private Integer failedDocs;
    private Integer percentage;
    private Long currentDocId;
    private String currentDocName;
    private String currentStep;
    private String currentStepName;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public static IndexingTaskProgressVO from(IndexingTask task) {
        IndexingTaskProgressVO vo = new IndexingTaskProgressVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus() != null ? task.getStatus().name() : null);
        vo.setTotalDocs(task.getTotalDocs());
        vo.setCompletedDocs(task.getCompletedDocs());
        vo.setFailedDocs(task.getFailedDocs());
        int total = task.getTotalDocs() != null ? task.getTotalDocs() : 0;
        int completed = task.getCompletedDocs() != null ? task.getCompletedDocs() : 0;
        vo.setPercentage(total > 0 ? (completed * 100 / total) : 0);
        vo.setCurrentDocId(task.getCurrentDocId());
        vo.setCurrentDocName(task.getCurrentDocName());
        if (task.getCurrentStep() != null) {
            vo.setCurrentStep(task.getCurrentStep().name());
            vo.setCurrentStepName(task.getCurrentStep().getDisplayName());
        }
        vo.setErrorMessage(task.getErrorMessage());
        vo.setStartedAt(task.getStartedAt());
        vo.setCompletedAt(task.getCompletedAt());
        return vo;
    }
}
