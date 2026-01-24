package com.mtmn.smartdoc.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.common.QueryDecomposeResult;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description 问题分解解析器
 * @date 2025/8/3 15:10
 */
@Component
@Log4j2
public class QueryDecomposeParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析问题分解响应
     */
    public QueryDecomposeResult parseResponse(String response) {
        try {
            // 策略1: 直接JSON数组解析
            JsonNode arrayNode = tryDirectArrayParse(response);
            if (arrayNode != null && arrayNode.isArray()) {
                return buildResultFromJsonArray(arrayNode, response);
            }

            // 策略2: 提取JSON数组片段
            arrayNode = tryExtractJsonArrayFragment(response);
            if (arrayNode != null && arrayNode.isArray()) {
                return buildResultFromJsonArray(arrayNode, response);
            }

            // 策略3: 文本提取兜底
            return tryTextExtraction(response);

        } catch (Exception e) {
            log.error("问题分解解析失败: {}", response, e);
            return createFallbackResult(response);
        }
    }

    /**
     * 直接JSON数组解析
     */
    private JsonNode tryDirectArrayParse(String response) {
        try {
            return objectMapper.readTree(response.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 提取JSON数组片段
     */
    private JsonNode tryExtractJsonArrayFragment(String response) {
        try {
            // 查找JSON数组开始和结束位置
            int start = response.indexOf('[');
            int end = response.lastIndexOf(']');

            if (start >= 0 && end > start) {
                String jsonStr = response.substring(start, end + 1);
                return objectMapper.readTree(jsonStr);
            }
        } catch (Exception e) {
            log.debug("JSON数组片段提取失败", e);
        }
        return null;
    }

    /**
     * 从JSON数组构建结果
     */
    private QueryDecomposeResult buildResultFromJsonArray(JsonNode arrayNode, String rawResponse) {
        List<QueryDecomposeResult.QueryItem> queries = new ArrayList<>();

        for (JsonNode itemNode : arrayNode) {
            String type = itemNode.path("type").asText("检索");
            String query = itemNode.path("query").asText("");
            
            if (!query.isEmpty()) {
                queries.add(QueryDecomposeResult.QueryItem.builder()
                        .type(type)
                        .query(query)
                        .build());
            }
        }

        return QueryDecomposeResult.builder()
                .queries(queries)
                .success(true)
                .rawResponse(rawResponse)
                .build();
    }

    /**
     * 文本提取兜底
     */
    private QueryDecomposeResult tryTextExtraction(String response) {
        List<QueryDecomposeResult.QueryItem> queries = new ArrayList<>();

        try {
            // 尝试按行分割并提取查询
            String[] lines = response.split("\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("{") && !trimmedLine.startsWith("}") 
                    && !trimmedLine.startsWith("[") && !trimmedLine.startsWith("]")
                    && !trimmedLine.startsWith("\"type\"") && !trimmedLine.startsWith("\"query\"")) {
                    
                    // 如果包含中文问号或句号，可能是一个查询
                    if (trimmedLine.contains("？") || trimmedLine.contains("。") || trimmedLine.contains("?")) {
                        queries.add(QueryDecomposeResult.QueryItem.builder()
                                .type("检索")
                                .query(trimmedLine.replaceAll("^[\"']|[\"']$", "")) // 去掉首尾引号
                                .build());
                    }
                }
            }

            // 如果没有提取到任何查询，使用正则匹配
            if (queries.isEmpty()) {
                queries = extractByRegex(response);
            }

        } catch (Exception e) {
            log.debug("文本提取失败", e);
        }

        return QueryDecomposeResult.builder()
                .queries(queries)
                .success(!queries.isEmpty())
                .rawResponse(response)
                .reason(queries.isEmpty() ? "文本提取失败" : "文本提取成功")
                .build();
    }

    /**
     * 正则表达式提取查询
     */
    private List<QueryDecomposeResult.QueryItem> extractByRegex(String response) {
        List<QueryDecomposeResult.QueryItem> queries = new ArrayList<>();
        
        try {
            // 匹配 "query": "xxx" 格式
            Pattern pattern = Pattern.compile("\"query\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response);
            
            while (matcher.find()) {
                String query = matcher.group(1);
                queries.add(QueryDecomposeResult.QueryItem.builder()
                        .type("检索")
                        .query(query)
                        .build());
            }
        } catch (Exception e) {
            log.debug("正则提取失败", e);
        }
        
        return queries;
    }

    /**
     * 创建兜底结果
     */
    private QueryDecomposeResult createFallbackResult(String response) {
        // 将原始问题作为单个查询项返回
        List<QueryDecomposeResult.QueryItem> fallbackQueries = new ArrayList<>();
        fallbackQueries.add(QueryDecomposeResult.QueryItem.builder()
                .type("检索")
                .query("解析失败，使用原始查询")
                .build());

        return QueryDecomposeResult.builder()
                .queries(fallbackQueries)
                .success(false)
                .rawResponse(response)
                .reason("解析失败，使用兜底策略")
                .build();
    }
}
