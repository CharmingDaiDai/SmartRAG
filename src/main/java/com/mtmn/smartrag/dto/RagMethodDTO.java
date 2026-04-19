package com.mtmn.smartrag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author charmingdaidai
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG方法数据传输对象")
public class RagMethodDTO {
    @Schema(description = "方法ID")
    private String id;
    @Schema(description = "方法名称")
    private String name;
    @Schema(description = "方法描述")
    private String description;
    @Schema(description = "索引参数")
    private Map<String, Object> indexParams;
    @Schema(description = "搜索参数")
    private Map<String, Object> searchParams;
}