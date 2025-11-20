package com.mtmn.smartdoc.strategy.impl;

import com.mtmn.smartdoc.enums.EnhancementType;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.strategy.Enhancement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 查询分解增强策略
 * 将复杂查询分解为多个子查询
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryDecomposeEnhancement implements Enhancement {

    private static final String DECOMPOSE_PROMPT_TEMPLATE =
            "你是一个专业的问题分析助手。请将以下复杂问题分解为 2-4 个更简单、更具体的子问题。\n\n" +
                    "原始问题：%s\n\n" +
                    "请按以下格式输出子问题（每行一个）：\n" +
                    "1. 子问题1\n" +
                    "2. 子问题2\n" +
                    "...\n\n" +
                    "子问题列表：";
    private final ModelFactory modelFactory;

    @Override
    public EnhancementType getType() {
        return EnhancementType.QUERY_DECOMPOSE;
    }

    @Override
    public EnhancedQuery enhance(String originalQuery, EnhancementContext context) {
        log.info("Applying query decompose enhancement: originalQuery={}", originalQuery);

        try {
            // 1. 获取 LLM 客户端
            String llmModelId = context.getLlmModelId();
            if (llmModelId == null || llmModelId.isEmpty()) {
                log.warn("No LLM model specified, skipping query decomposition");
                return createFallbackQuery(originalQuery);
            }

            LLMClient llmClient = modelFactory.createLLMClient(llmModelId);

            // 2. 构建分解提示
            String prompt = String.format(DECOMPOSE_PROMPT_TEMPLATE, originalQuery);

            // 3. 调用 LLM 进行分解
            String response = llmClient.chat(prompt).trim();

            // 4. 解析子问题
            List<String> subQueries = parseSubQueries(response);

            // 5. 创建增强查询
            EnhancedQuery enhancedQuery = new EnhancedQuery();
            enhancedQuery.setQuery(originalQuery);
            enhancedQuery.setSubQueries(subQueries);

            log.info("Query decomposed: original='{}', subQueries={}",
                    originalQuery, subQueries.size());

            return enhancedQuery;

        } catch (Exception e) {
            log.error("Failed to decompose query, using original", e);
            return createFallbackQuery(originalQuery);
        }
    }

    @Override
    public boolean supports(IndexStrategyType strategyType) {
        // 支持所有索引策略
        return true;
    }

    /**
     * 解析 LLM 返回的子问题列表
     */
    private List<String> parseSubQueries(String response) {
        List<String> subQueries = new ArrayList<>();

        // 按行分割
        String[] lines = response.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();

            // 跳过空行
            if (trimmed.isEmpty()) {
                continue;
            }

            // 移除序号（1. 2. 3. 等）
            String cleaned = trimmed.replaceFirst("^\\d+\\.\\s*", "")
                    .replaceFirst("^[-*]\\s*", "");

            if (!cleaned.isEmpty()) {
                subQueries.add(cleaned);
            }
        }

        // 如果没有解析到子问题，返回原始响应作为单个子问题
        if (subQueries.isEmpty()) {
            subQueries.add(response);
        }

        return subQueries;
    }

    /**
     * 创建回退查询（使用原始查询）
     */
    private EnhancedQuery createFallbackQuery(String originalQuery) {
        EnhancedQuery query = new EnhancedQuery();
        query.setQuery(originalQuery);
        query.setSubQueries(Arrays.asList(originalQuery));
        return query;
    }
}