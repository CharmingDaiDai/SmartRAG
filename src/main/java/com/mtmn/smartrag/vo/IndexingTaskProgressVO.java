package com.mtmn.smartrag.vo;

import com.mtmn.smartrag.enums.IndexingStep;
import com.mtmn.smartrag.enums.IndexingTaskStatus;
import com.mtmn.smartrag.po.IndexingTask;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 索引任务进度 VO（供前端轮询接口使用）
 *
 * @author charmingdaidai
 * @version 1.1
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
    /** 当前细粒度步骤已处理条目数（LLM_ENRICHING / EMBEDDING 时有值） */
    private Integer currentStepProcessed;
    /** 当前细粒度步骤总条目数 */
    private Integer currentStepTotal;
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

        int total     = Objects.requireNonNullElse(task.getTotalDocs(), 0);
        int completed = Objects.requireNonNullElse(task.getCompletedDocs(), 0);
        int stepProc  = Objects.requireNonNullElse(task.getCurrentStepProcessed(), 0);
        int stepTot   = Objects.requireNonNullElse(task.getCurrentStepTotal(), 0);

        double docFraction = stepFraction(task.getCurrentStep(), stepProc, stepTot);
        int pct = total > 0
            ? (int) Math.min(99, (completed + docFraction) * 100.0 / total)
            : 0;
        // 任务真正完成时才设为 100
        if (IndexingTaskStatus.COMPLETED.name().equals(vo.getStatus())) {
            pct = 100;
        }
        vo.setPercentage(pct);

        vo.setCurrentDocId(task.getCurrentDocId());
        vo.setCurrentDocName(task.getCurrentDocName());
        if (task.getCurrentStep() != null) {
            vo.setCurrentStep(task.getCurrentStep().name());
            vo.setCurrentStepName(task.getCurrentStep().getDisplayName());
        }
        vo.setCurrentStepProcessed(stepProc);
        vo.setCurrentStepTotal(stepTot);
        vo.setErrorMessage(task.getErrorMessage());
        vo.setStartedAt(task.getStartedAt());
        vo.setCompletedAt(task.getCompletedAt());
        return vo;
    }

    /**
     * 根据当前步骤和步内子进度，返回该文档已完成的比例（0.0–1.0）。
     *
     * <p>快速步骤（READING / PARSING / CHUNKING / TREE_BUILDING / SAVING / STORING）
     * 取固定起始值；慢步骤（LLM_ENRICHING / EMBEDDING）按子进度在区间内插值，
     * 使进度条在这两步中平滑推进。</p>
     *
     * <p>权重分配（HiSem with LLM 场景）：</p>
     * <ul>
     *   <li>READING + PARSING + TREE_BUILDING ≈ 10%</li>
     *   <li>LLM_ENRICHING：10% ~ 55%（细粒度）</li>
     *   <li>SAVING ≈ 2%</li>
     *   <li>EMBEDDING：60% ~ 95%（细粒度）</li>
     *   <li>STORING ≈ 4%</li>
     * </ul>
     */
    private static double stepFraction(IndexingStep step, int processed, int total) {
        if (step == null) return 0.0;
        double sub = (total > 0) ? (double) processed / total : 0.0;
        return switch (step) {
            case READING       -> 0.01;
            case PARSING       -> 0.03;
            case CHUNKING      -> 0.05;
            case TREE_BUILDING -> 0.07;
            case LLM_ENRICHING -> 0.10 + sub * 0.45;   // 10% ~ 55%
            case SAVING        -> 0.57;
            case EMBEDDING     -> 0.60 + sub * 0.35;   // 60% ~ 95%
            case STORING       -> 0.96;
        };
    }
}
