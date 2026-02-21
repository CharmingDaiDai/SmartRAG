package com.mtmn.smartdoc.rag.impl;

import com.mtmn.smartdoc.enums.EnhancementType;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.rag.Enhancement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * HyDE (Hypothetical Document Embeddings) 增强策略
 * 生成假设性答案文档，使用该文档进行向量检索
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HyDEEnhancement implements Enhancement {

    private static final String HYDE_PROMPT_TEMPLATE =
            "你是一个知识库专家。请根据以下问题，生成一个假设性的、详细的答案文档。" +
                    "这个假设答案将用于在知识库中检索真实答案。\n\n" +
                    "问题：%s\n\n" +
                    "请生成一个200-300字的假设答案，包含可能的关键信息和术语：\n";
    private final ModelFactory modelFactory;

    @Override
    public EnhancementType getType() {
        return EnhancementType.HYDE;
    }

    @Override
    public EnhancedQuery enhance(String originalQuery, EnhancementContext context) {
        log.info("Applying HyDE enhancement: originalQuery={}", originalQuery);

        try {
            // 1. 获取 LLM 客户端
            String llmModelId = context.getLlmModelId();
            if (llmModelId == null || llmModelId.isEmpty()) {
                log.warn("No LLM model specified, skipping HyDE enhancement");
                return createFallbackQuery(originalQuery);
            }

            LLMClient llmClient = modelFactory.createLLMClient(llmModelId);

            // 2. 构建 HyDE 提示
            String prompt = String.format(HYDE_PROMPT_TEMPLATE, originalQuery);

            // 3. 生成假设性文档
            String hypotheticalDoc = llmClient.chat(prompt).trim();

            // 4. 创建增强查询
            EnhancedQuery enhancedQuery = new EnhancedQuery();
            enhancedQuery.setQuery(hypotheticalDoc);  // 使用假设文档替代原始查询

            // 5. 将原始查询和假设文档都保存在附加参数中
            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.put("original_query", originalQuery);
            additionalParams.put("hypothetical_doc", hypotheticalDoc);
            additionalParams.put("use_hyde", true);
            enhancedQuery.setAdditionalParams(additionalParams);

            log.info("HyDE document generated: original='{}', doc_length={}",
                    originalQuery, hypotheticalDoc.length());

            return enhancedQuery;

        } catch (Exception e) {
            log.error("Failed to generate hypothetical document, using original query", e);
            return createFallbackQuery(originalQuery);
        }
    }

    @Override
    public boolean supports(IndexStrategyType strategyType) {
        // HyDE 主要适用于向量检索，支持所有策略
        return true;
    }

    /**
     * 创建回退查询（使用原始查询）
     */
    private EnhancedQuery createFallbackQuery(String originalQuery) {
        EnhancedQuery query = new EnhancedQuery();
        query.setQuery(originalQuery);

        Map<String, Object> params = new HashMap<>();
        params.put("use_hyde", false);
        query.setAdditionalParams(params);

        return query;
    }
}