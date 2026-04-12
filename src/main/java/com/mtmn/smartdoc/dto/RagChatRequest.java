package com.mtmn.smartdoc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * @author charmingdaidai
 * @version 1.0
 * @date 2025/12/1 18:02
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG 对话请求")
public class RagChatRequest {

    @Schema(description = "知识库 id")
    @Required
    private Long kbId;

    @Schema(description = "用户问题")
    @Required
    private String question;

    @Required
    @Schema(description = "Embedding 模型 ID")
    private String embeddingModelId;

    @Required
    @Schema(description = "LLM 模型 ID")
    private String llmModelId;

    @Schema(description = "Rerank 模型 ID，可为空")
    private String rerankModelId;

    @Schema(description = "会话 ID，用于关联历史对话")
    private String sessionId;

    @Schema(description = "历史对话窗口（轮数），默认 8")
    private Integer historyWindow = 8;
}