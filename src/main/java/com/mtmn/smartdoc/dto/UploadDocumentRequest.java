package com.mtmn.smartdoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传文档请求 DTO
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "上传文档请求")
public class UploadDocumentRequest {

    /**
     * 知识库 ID
     */
    @Schema(description = "知识库 ID")
    private Long kbId;

    /**
     * 文档标题 (可选,默认使用文件名)
     */
    @Schema(description = "文档标题（可选，默认使用文件名）")
    private String title;
}