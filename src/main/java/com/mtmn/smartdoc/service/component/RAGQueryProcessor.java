package com.mtmn.smartdoc.service.component;

import com.mtmn.smartdoc.constants.AppConstants;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.utils.LlmJsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * RAG 查询处理组件
 * 负责查询前的预处理，包括意图识别、查询重写、问题分解、HyDE等
 *
 * @author charmingdaidai
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RAGQueryProcessor {

    private final ModelFactory modelFactory;

    /**
     * 意图识别
     *
     * @param question 用户问题
     * @return 是否需要检索
     */
    public boolean analyzeIntent(String question) {
        return analyzeIntent(question, "");
    }

    /**
     * 意图识别（带历史上下文）
     *
     * @param question    用户问题
     * @param historyText 历史对话文本
     * @return 是否需要检索
     */
    public boolean analyzeIntent(String question, String historyText) {
        try {
            String message = AppConstants.PromptTemplates.PROMPT_INTENT_RECOGNITION
                    .replace("{query}", question)
                    .replace("{history}", historyText == null ? "" : historyText);

            String resp = modelFactory.createDefaultLLMClient().chat(message);
            Map<String, Object> map = LlmJsonUtils.parseMap(resp);
            String action = (String) map.getOrDefault("action", "SEARCH");
            
            // 兼容 JSON 返回 needRetrieval 字段的情况 (根据 Prompt 定义)
            if (map.containsKey("needRetrieval")) {
                return Boolean.TRUE.equals(map.get("needRetrieval"));
            }

            return !Objects.equals(action, "CHAT");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                log.warn("意图识别触发速率限制，跳过此步骤: {}", e.getMessage());
            } else {
                log.error("意图识别失败: {}", e.getMessage());
            }
            return true; // 默认需要检索
        }
    }

    /**
     * 查询重写
     *
     * @param question 用户问题
     * @return 重写后的查询
     */
    public String rewriteQuery(String question) {
        return rewriteQuery(question, "");
    }

    /**
     * 查询重写（带历史上下文）
     *
     * @param question    用户问题
     * @param historyText 历史对话文本
     * @return 重写后的查询
     */
    public String rewriteQuery(String question, String historyText) {
        try {
            String message = AppConstants.PromptTemplates.PROMPT_QUERY_REWRITE
                    .replace("{query}", question)
                    .replace("{history}", historyText == null ? "" : historyText);

            String resp = modelFactory.createDefaultLLMClient().chat(message);
            // 尝试解析 JSON
            try {
                Map<String, Object> map = LlmJsonUtils.parseMap(resp);
                return (String) map.getOrDefault("rewrittenQuery", question);
            } catch (Exception e) {
                // 如果不是 JSON，可能直接返回了文本
                return LlmJsonUtils.parseString(resp);
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                log.warn("查询重写触发速率限制，跳过此步骤: {}", e.getMessage());
            } else {
                log.error("查询重写失败: {}", e.getMessage());
            }
            return question;
        }
    }

    /**
     * 问题分解
     *
     * @param question 用户问题
     * @return 子问题列表
     */
    public List<String> decomposeQuery(String question) {
        try {
            String message = AppConstants.PromptTemplates.PROMPT_QUERY_DECOMPOSITION
                    .replace("{query}", question);

            String resp = modelFactory.createDefaultLLMClient().chat(message);
            return LlmJsonUtils.parseList(resp, String.class);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                log.warn("问题分解触发速率限制，跳过此步骤: {}", e.getMessage());
            } else {
                log.error("问题分解失败: {}", e.getMessage());
            }
            return Collections.emptyList();
        }
    }

    /**
     * HyDE (Hypothetical Document Embeddings)
     * 生成假设性文档
     *
     * @param question 用户问题
     * @return 假设性文档内容
     */
    public String generateHydeDoc(String question) {
        try {
            String message = AppConstants.PromptTemplates.PROMPT_HYDE
                    .replace("{query}", question);

            String resp = modelFactory.createDefaultLLMClient().chat(message);
            return LlmJsonUtils.parseString(resp);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("429")) {
                log.warn("HyDE 生成触发速率限制，跳过此步骤: {}", e.getMessage());
            } else {
                log.error("HyDE 生成失败: {}", e.getMessage());
            }
            return null;
        }
    }
}
