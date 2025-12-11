package com.mtmn.smartdoc.rag.impl;

import com.mtmn.smartdoc.enums.EnhancementType;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.rag.Enhancement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 查询重写增强策略
 * 使用 LLM 重写用户查询，提高检索准确性
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryRewriteEnhancement implements Enhancement {

    private static final String REWRITE_PROMPT_TEMPLATE =
            "你是一个专业的查询优化助手。请将以下用户查询重写为更清晰、更具体的问题，" +
                    "以便在知识库中进行更准确的检索。\n\n" +
                    "用户查询：%s\n\n" +
                    "重写后的查询（只返回重写后的问题，不要解释）：";
    private final ModelFactory modelFactory;

    @Override
    public EnhancementType getType() {
        return EnhancementType.QUERY_REWRITE;
    }

    @Override
    public EnhancedQuery enhance(String originalQuery, EnhancementContext context) {
        log.info("Applying query rewrite enhancement: originalQuery={}", originalQuery);

        try {
            // 1. 获取 LLM 客户端
            String llmModelId = context.getLlmModelId();
            if (llmModelId == null || llmModelId.isEmpty()) {
                log.warn("No LLM model specified, skipping query rewrite");
                return createFallbackQuery(originalQuery);
            }

            LLMClient llmClient = modelFactory.createLLMClient(llmModelId);

            // 2. 构建重写提示
            String prompt = String.format(REWRITE_PROMPT_TEMPLATE, originalQuery);

            // 3. 调用 LLM 进行重写
            String rewrittenQuery = llmClient.chat(prompt).trim();

            // 4. 创建增强查询
            EnhancedQuery enhancedQuery = new EnhancedQuery();
            enhancedQuery.setQuery(rewrittenQuery);

            log.info("Query rewritten: original='{}', rewritten='{}'",
                    originalQuery, rewrittenQuery);

            return enhancedQuery;

        } catch (Exception e) {
            log.error("Failed to rewrite query, using original", e);
            return createFallbackQuery(originalQuery);
        }
    }

    @Override
    public boolean supports(IndexStrategyType strategyType) {
        // 支持所有索引策略
        return true;
    }

    /**
     * 创建回退查询（使用原始查询）
     */
    private EnhancedQuery createFallbackQuery(String originalQuery) {
        EnhancedQuery query = new EnhancedQuery();
        query.setQuery(originalQuery);
        return query;
    }
}