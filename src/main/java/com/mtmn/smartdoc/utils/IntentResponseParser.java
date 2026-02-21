package com.mtmn.smartdoc.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.common.IntentResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description 意图识别解析
 * @date 2025/5/31 16:46
 */
@Component
@Slf4j
public class IntentResponseParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析JSON响应
     */
    public IntentResult parseJsonResponse(String response) {
        IntentResult result = new IntentResult();
        result.setRawResponse(response);

        try {
            // 策略1: 直接JSON解析
            JsonNode jsonNode = tryDirectJsonParse(response);
            if (jsonNode != null) {
                return buildResultFromJson(jsonNode, response);
            }

            // 策略2: 提取JSON片段
            jsonNode = tryExtractJsonFragment(response);
            if (jsonNode != null) {
                return buildResultFromJson(jsonNode, response);
            }

            // 策略3: 关键词提取
            return tryKeywordExtraction(response);

        } catch (Exception e) {
            log.error("JSON解析失败: {}", response, e);
            return createFallbackResult(response);
        }
    }

    /**
     * 直接JSON解析
     */
    private JsonNode tryDirectJsonParse(String response) {
        try {
            return objectMapper.readTree(response.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取JSON片段
     */
    private JsonNode tryExtractJsonFragment(String response) {
        try {
            // 查找JSON开始和结束位置
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');

            if (start >= 0 && end > start) {
                String jsonStr = response.substring(start, end + 1);
                return objectMapper.readTree(jsonStr);
            }
        } catch (Exception e) {
            log.debug("JSON片段提取失败", e);
        }
        return null;
    }

    /**
     * 从JSON构建结果
     */
    private IntentResult buildResultFromJson(JsonNode jsonNode, String rawResponse) {
        IntentResult result = new IntentResult();
        result.setRawResponse(rawResponse);
        result.setParseSuccess(true);

        // 解析各字段
        result.setNeedRetrieval(jsonNode.path("needRetrieval").asBoolean(true));
        result.setReason(jsonNode.path("reason").asText("未提供原因"));
        result.setQuestionType(jsonNode.path("questionType").asText("未分类"));

        return result;
    }

    /**
     * 关键词提取兜底
     */
    private IntentResult tryKeywordExtraction(String response) {
        IntentResult result = new IntentResult();
        result.setRawResponse(response);
        result.setParseSuccess(false);

        String lowerResponse = response.toLowerCase();

        // 提取needRetrieval
        result.setNeedRetrieval(lowerResponse.contains("\"needretrieval\": true") ||
                lowerResponse.contains("需要检索") ||
                lowerResponse.contains("true"));

        // 尝试提取reason
        String reason = extractReason(response);
        result.setReason(reason != null ? reason : "解析失败，使用关键词提取");

        result.setQuestionType("解析失败");

        return result;
    }

    /**
     * 提取原因字段
     */
    private String extractReason(String response) {
        try {
            // 简单的正则提取
            Pattern pattern = Pattern.compile("\"reason\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            log.debug("原因提取失败", e);
        }
        return null;
    }

    /**
     * 创建兜底结果
     */
    private IntentResult createFallbackResult(String response) {
        return IntentResult.builder()
                .needRetrieval(true) // 默认需要检索
                .reason("解析失败，采用保守策略")
                .questionType("解析异常")
                .parseSuccess(false)
                .rawResponse(response)
                .build();
    }
}