package com.mtmn.smartdoc.utils;

import com.mtmn.smartdoc.common.QueryDecomposeResult;
import com.mtmn.smartdoc.service.LLMService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description 问题分解
 * @date 2025/8/3 13:23
 */
@Service
@Log4j2
public class QueryDecompose {

    @Autowired
    private LLMService llmService;

    @Autowired
    private QueryDecomposeParser queryDecomposeParser;

    @Value("${prompt.queryDecompose2}")
    private String QUERY_DECOMPOSE_PROMPT;

    /**
     * 分解查询并返回结构化结果
     */
    public QueryDecomposeResult decomposeQuery(String userQuery) {
        try {
            String prompt = String.format(QUERY_DECOMPOSE_PROMPT, userQuery);

            String response = llmService.createChatModel().chat(prompt);
//            log.debug("问题分解 - 原始: {} | 分解: {}", userQuery, response);

            QueryDecomposeResult result = queryDecomposeParser.parseResponse(response);
            
            // 记录日志
            log.info("问题分解 - 原始: {} | 分解成功: {} | 查询数量: {}", 
                    userQuery, result.isSuccess(), result.getQueries().size());

            return result;

        } catch (Exception e) {
            log.error("问题分解失败", e);
            return QueryDecomposeResult.builder()
                    .queries(List.of(QueryDecomposeResult.QueryItem.builder()
                            .type("检索")
                            .query(userQuery)
                            .build()))
                    .success(false)
                    .reason("系统异常: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 兼容原有接口，返回字符串数组
     */
    @Deprecated
    public String[] decomposeQueryToArray(String userQuery) {
        QueryDecomposeResult result = decomposeQuery(userQuery);
        return result.getQueries().stream()
                .map(QueryDecomposeResult.QueryItem::getQuery)
                .toArray(String[]::new);
    }
}