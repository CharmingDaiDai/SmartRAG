package com.mtmn.smartdoc.rag.impl;

import com.mtmn.smartdoc.enums.EnhancementType;
import com.mtmn.smartdoc.enums.IndexStrategyType;
import com.mtmn.smartdoc.rag.Enhancement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 混合检索增强策略
 * 结合向量检索和关键词检索，提取查询中的关键词
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025-11-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridRetrievalEnhancement implements Enhancement {

    // 停用词列表（简化版）
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "那", "么", "什么", "怎么", "为什么", "哪", "哪个", "吗", "呢"
    ));

    // 提取中文词语的正则（2-4个字的词）
    private static final Pattern CHINESE_WORD_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");

    // 提取英文单词的正则
    private static final Pattern ENGLISH_WORD_PATTERN = Pattern.compile("[a-zA-Z]{3,}");

    @Override
    public EnhancementType getType() {
        return EnhancementType.HYBRID_RETRIEVAL;
    }

    @Override
    public EnhancedQuery enhance(String originalQuery, EnhancementContext context) {
        log.info("Applying hybrid retrieval enhancement: originalQuery={}", originalQuery);

        try {
            // 1. 提取关键词
            List<String> keywords = extractKeywords(originalQuery);

            // 2. 创建增强查询
            EnhancedQuery enhancedQuery = new EnhancedQuery();
            enhancedQuery.setQuery(originalQuery);

            // 3. 设置混合检索参数
            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.put("use_hybrid", true);
            additionalParams.put("keywords", keywords);
            additionalParams.put("vector_weight", 0.7);  // 向量检索权重
            additionalParams.put("keyword_weight", 0.3); // 关键词检索权重
            enhancedQuery.setAdditionalParams(additionalParams);

            log.info("Hybrid retrieval configured: keywords={}", keywords);

            return enhancedQuery;

        } catch (Exception e) {
            log.error("Failed to configure hybrid retrieval, using vector-only", e);
            return createFallbackQuery(originalQuery);
        }
    }

    @Override
    public boolean supports(IndexStrategyType strategyType) {
        // 支持所有索引策略
        return true;
    }

    /**
     * 提取查询中的关键词
     */
    private List<String> extractKeywords(String query) {
        Set<String> keywordSet = new HashSet<>();

        // 1. 提取中文词语
        Matcher chineseMatcher = CHINESE_WORD_PATTERN.matcher(query);
        while (chineseMatcher.find()) {
            String word = chineseMatcher.group();
            if (!STOP_WORDS.contains(word)) {
                keywordSet.add(word);
            }
        }

        // 2. 提取英文单词
        Matcher englishMatcher = ENGLISH_WORD_PATTERN.matcher(query);
        while (englishMatcher.find()) {
            String word = englishMatcher.group().toLowerCase();
            keywordSet.add(word);
        }

        // 3. 提取数字
        Pattern numberPattern = Pattern.compile("\\d+");
        Matcher numberMatcher = numberPattern.matcher(query);
        while (numberMatcher.find()) {
            keywordSet.add(numberMatcher.group());
        }

        // 4. 按词频或长度排序（这里简单按长度倒序）
        List<String> keywords = new ArrayList<>(keywordSet);
        keywords.sort((a, b) -> Integer.compare(b.length(), a.length()));

        // 5. 限制关键词数量（最多10个）
        if (keywords.size() > 10) {
            keywords = keywords.subList(0, 10);
        }

        return keywords;
    }

    /**
     * 创建回退查询（仅使用向量检索）
     */
    private EnhancedQuery createFallbackQuery(String originalQuery) {
        EnhancedQuery query = new EnhancedQuery();
        query.setQuery(originalQuery);

        Map<String, Object> params = new HashMap<>();
        params.put("use_hybrid", false);
        params.put("vector_weight", 1.0);
        params.put("keyword_weight", 0.0);
        query.setAdditionalParams(params);

        return query;
    }
}