package com.mtmn.smartrag.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartrag.common.QueryRewriteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description TODO
 * @date 2025/6/1 13:25
 */
@Component
@Slf4j
public class QueryRewriteParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public QueryRewriteResult parseSimpleResponse(String response) {
        try {
            // 尝试JSON解析
            JsonNode jsonNode = extractJson(response);
            if (jsonNode != null) {
                return QueryRewriteResult.builder()
                        .rewrittenQuery(jsonNode.path("rewrittenQuery").asText())
                        .reason(jsonNode.path("reason").asText("未提供原因"))
                        .success(true)
                        .build();
            }

            // 兜底：尝试文本提取
            return tryTextExtraction(response);

        } catch (Exception e) {
            log.error("解析查询重写响应失败: {}", response, e);
            return QueryRewriteResult.builder()
                    .success(false)
                    .reason("解析失败")
                    .build();
        }
    }

    private JsonNode extractJson(String response) {
        try {
            int start = response.indexOf('{');
            int end = response.lastIndexOf('}');

            if (start >= 0 && end > start) {
                String jsonStr = response.substring(start, end + 1);
                return objectMapper.readTree(jsonStr);
            }
        } catch (Exception e) {
            log.debug("JSON提取失败", e);
        }
        return null;
    }

    private QueryRewriteResult tryTextExtraction(String response) {
        // 简单的文本模式匹配
        String query = extractByPattern(response, "重写.*?[:：]\\s*(.+)");
        String reason = extractByPattern(response, "原因.*?[:：]\\s*(.+)");

        return QueryRewriteResult.builder()
                .rewrittenQuery(query)
                .reason(reason != null ? reason : "文本提取")
                .success(query != null)
                .build();
    }

    private String extractByPattern(String text, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            return m.find() ? m.group(1).trim() : null;
        } catch (Exception e) {
            return null;
        }
    }
}