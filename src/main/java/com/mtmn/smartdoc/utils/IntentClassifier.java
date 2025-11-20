package com.mtmn.smartdoc.utils;

import com.mtmn.smartdoc.common.IntentResult;
import com.mtmn.smartdoc.service.LLMService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description 意图识别器
 * @date 2025/5/31 16:44
 */
@Service
public class IntentClassifier {

    @Value("${prompt.intentClassifier}")
    public String INTENT_PROMPT;

    @Resource
    private LLMService llmService;

    @Resource
    private IntentResponseParser intentResponseParser;

    public IntentResult analyzeIntent(String modelId, String currentQuestion, String conversationHistory) {
        String prompt = String.format(INTENT_PROMPT,
                conversationHistory != null ? conversationHistory : "",
                currentQuestion);

        String response = llmService.createLLMClient(modelId).chat(prompt);
        return intentResponseParser.parseJsonResponse(response);
    }
}