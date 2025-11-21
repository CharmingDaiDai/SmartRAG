package com.mtmn.smartdoc.controller;

import com.mtmn.smartdoc.common.ApiResponse;
import com.mtmn.smartdoc.model.factory.ModelFactory;
import com.mtmn.smartdoc.po.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模型控制器
 *
 * @author charmingdaidai
 * @version 1.0
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/models")
public class ModelController {

    private final ModelFactory modelFactory;

    @GetMapping("/llms")
    public ApiResponse<List<String>> getAvailableLLMModels(@AuthenticationPrincipal User user) {
        log.info("User {} is requesting available LLM models", user.getId());

        List<String> availableLLMModels = modelFactory.getAvailableLLMModels();

        return ApiResponse.success(availableLLMModels);
    }

    @GetMapping("/embeddings")
    public ApiResponse<List<String>> getAvailableEmbeddingModels(@AuthenticationPrincipal User user) {
        log.info("User {} is requesting available embedding models", user.getId());

        List<String> availableEmbeddingModels = modelFactory.getAvailableEmbeddingModels();

        return ApiResponse.success(availableEmbeddingModels);
    }

    @GetMapping("/reranks")
    public ApiResponse<List<String>> getAvailableRerankModels(@AuthenticationPrincipal User user) {
        log.info("User {} is requesting available rerank models", user.getId());

        List<String> availableRerankModels = modelFactory.getAvailableRerankModels();

        return ApiResponse.success(availableRerankModels);
    }

}