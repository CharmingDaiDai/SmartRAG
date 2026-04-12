package com.mtmn.smartdoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档文本预览响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档文本预览")
public class DocumentPreviewTextResponse {

    @Schema(description = "文档 ID")
    private Long documentId;

    @Schema(description = "当前页（0-based）")
    private int page;

    @Schema(description = "每页段数")
    private int size;

    @Schema(description = "文档总字符数")
    private long totalChars;

    @Schema(description = "总分段数")
    private int totalSegments;

    @Schema(description = "是否还有更多内容")
    private boolean hasMore;

    @Schema(description = "当前页文本段")
    private List<String> segments;
}
