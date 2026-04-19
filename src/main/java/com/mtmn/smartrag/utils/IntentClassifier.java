package com.mtmn.smartrag.utils;

import com.mtmn.smartrag.common.IntentResult;
import com.mtmn.smartrag.constants.AppConstants;
import com.mtmn.smartrag.service.LLMService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author charmingdaidai
 * @version 1.0
 * @description 意图识别器
 * @date 2025/5/31 16:44
 */
@Service
public class IntentClassifier {

    @Resource
    private LLMService llmService;

    @Resource
    private IntentResponseParser intentResponseParser;

    public IntentResult analyzeIntent(String modelId, String currentQuestion, String conversationHistory) {
        String prompt = String.format(AppConstants.PromptTemplates.INTENT_CLASSIFIER,
                conversationHistory != null ? conversationHistory : "",
                currentQuestion);

        String response = llmService.createLLMClient(modelId).chat(prompt);
        return intentResponseParser.parseJsonResponse(response);
    }
}
