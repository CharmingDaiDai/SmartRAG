package com.mtmn.smartdoc.dto;

import com.mtmn.smartdoc.enums.DocumentIndexStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档响应 DTO
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文档响应")
public class DocumentResponse {

    /**
     * 文档 ID
     */
    @Schema(description = "文档 ID")
    private Long id;

    /**
     * 知识库 ID
     */
    @Schema(description = "知识库 ID")
    private Long kbId;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String filename;

    /**
     * 文件路径
     */
    @Schema(description = "文件路径")
    private String filePath;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private Long fileSize;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 索引状态
     */
    @Schema(description = "索引状态")
    private DocumentIndexStatus indexStatus;

    /**
     * 元数据
     */
    @Schema(description = "元数据")
    private String metadata;

    /**
     * 上传时间
     */
    @Schema(description = "上传时间")
    private LocalDateTime uploadTime;
}