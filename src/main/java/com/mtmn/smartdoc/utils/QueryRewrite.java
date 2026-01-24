package com.mtmn.smartdoc.utils;

import com.mtmn.smartdoc.common.QueryRewriteResult;
import com.mtmn.smartdoc.service.LLMService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description TODO
 * @date 2025/6/1 13:23
 */
@Service
@Log4j2
public class QueryRewrite {

    @Autowired
    private QueryRewriteParser queryRewriteParser;

    @Autowired
    private LLMService llmService;

    @Value("${prompt.queryRewrite}")
    private String SIMPLE_REWRITE_PROMPT;

    public QueryRewriteResult rewriteQuery(String conversationHistory, String originalQuery) {
        try {
            String prompt = String.format(SIMPLE_REWRITE_PROMPT, conversationHistory, originalQuery);

            String response = llmService.createChatModel().chat(prompt);
            QueryRewriteResult result = queryRewriteParser.parseSimpleResponse(response);
            result.setOriginalQuery(originalQuery);

            // 记录日志
            log.info("查询重写 - 原始: {} | 重写: {} | 原因: {}",
                    originalQuery, result.getRewrittenQuery(), result.getReason());

            return result;

        } catch (Exception e) {
            log.error("查询重写失败", e);
            return QueryRewriteResult.builder().rewrittenQuery(originalQuery).build();
        }
    }
}