package com.mtmn.smartdoc.utils;

import com.mtmn.smartdoc.common.IntentResult;
import com.mtmn.smartdoc.constants.AppConstants;
import com.mtmn.smartdoc.service.LLMService;
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
