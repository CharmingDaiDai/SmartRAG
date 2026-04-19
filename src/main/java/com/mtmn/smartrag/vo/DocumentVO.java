package com.mtmn.smartrag.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author charmingdaidai
 * @version 1.0
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "知识库文档返回模型")
public class DocumentVO {
    @Schema(description = "文档ID")
    private Long id;
    @Schema(description = "标题")
    private String title;
    @Schema(description = "文件名")
    private String fileName;
    @Schema(description = "文件类型")
    private String fileType;
    @Schema(description = "文件大小")
    private Long fileSize;
    @Schema(description = "文件URL")
    private String fileUrl;
    @Schema(description = "是否已索引")
    private Boolean indexed;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}