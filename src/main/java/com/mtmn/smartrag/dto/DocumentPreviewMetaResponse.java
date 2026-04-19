package com.mtmn.smartrag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档预览元信息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档预览元信息")
public class DocumentPreviewMetaResponse {

    @Schema(description = "文档 ID")
    private Long documentId;

    @Schema(description = "知识库 ID")
    private Long kbId;

    @Schema(description = "文档名称")
    private String filename;

    @Schema(description = "文档 MIME 类型")
    private String fileType;

    @Schema(description = "文档大小（字节）")
    private Long fileSize;

    @Schema(description = "索引状态")
    private String indexStatus;

    @Schema(description = "知识库索引策略")
    private String indexStrategyType;

    @Schema(description = "预览类型：RAW | MARKDOWN | TEXT | UNSUPPORTED")
    private String previewType;

    @Schema(description = "是否支持原样预览")
    private boolean supportsRawPreview;

    @Schema(description = "是否支持文本预览")
    private boolean supportsTextPreview;
}
