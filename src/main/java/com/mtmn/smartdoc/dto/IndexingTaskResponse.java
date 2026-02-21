package com.mtmn.smartdoc.dto;

import com.mtmn.smartdoc.enums.IndexingStep;
import com.mtmn.smartdoc.enums.IndexingTaskStatus;
import com.mtmn.smartdoc.enums.IndexingTaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 索引任务响应 DTO
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "索引任务响应")
public class IndexingTaskResponse {

    @Schema(description = "任务 ID")
    private Long id;

    @Schema(description = "知识库 ID")
    private Long kbId;

    @Schema(description = "任务类型")
    private IndexingTaskType taskType;

    @Schema(description = "任务状态")
    private IndexingTaskStatus status;

    @Schema(description = "总文档数")
    private Integer totalDocs;

    @Schema(description = "已完成文档数")
    private Integer completedDocs;

    @Schema(description = "失败文档数")
    private Integer failedDocs;

    @Schema(description = "当前处理的文档 ID")
    private Long currentDocId;

    @Schema(description = "当前处理的文档名")
    private String currentDocName;

    @Schema(description = "当前步骤")
    private IndexingStep currentStep;

    @Schema(description = "当前步骤显示名称")
    private String currentStepName;

    @Schema(description = "进度百分比")
    private Integer percentage;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "是否为新创建的任务（用于前端判断是否需要订阅）")
    private Boolean isNew;
}
