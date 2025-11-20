package com.mtmn.smartdoc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传文档请求 DTO
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadDocumentRequest {

    /**
     * 知识库 ID
     */
    private Long kbId;

    /**
     * 文档标题 (可选,默认使用文件名)
     */
    private String title;
}