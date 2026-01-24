package com.mtmn.smartdoc.utils;

import com.mtmn.smartdoc.common.IntentResult;
import com.mtmn.smartdoc.service.LLMService;
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

    @Autowired
    private LLMService llmService;

    @Autowired
    private IntentResponseParser intentResponseParser;

    @Value("${prompt.intentClassifier}")
    public String INTENT_PROMPT;

    public IntentResult analyzeIntent(String currentQuestion, String conversationHistory) {
        String prompt = String.format(INTENT_PROMPT,
                conversationHistory != null ? conversationHistory : "",
                currentQuestion);

        String response = llmService.createChatModel().chat(prompt);
        return intentResponseParser.parseJsonResponse(response);
    }
}