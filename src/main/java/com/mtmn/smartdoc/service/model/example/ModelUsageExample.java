package com.mtmn.smartdoc.service.model.example;

import com.mtmn.smartdoc.service.model.client.LLMClient;
import com.mtmn.smartdoc.service.model.dto.ChatRequest;
import com.mtmn.smartdoc.service.model.dto.ChatResponse;
import com.mtmn.smartdoc.service.model.factory.ModelFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 模型系统使用示例
 * 
 * <p>演示如何使用新的模型系统</p>
 * 
 * @author charmingdaidai
 * @version 2.0
 * @since 2025-01-17
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelUsageExample {
    
    private final ModelFactory modelFactory;
    
    /**
     * 示例1: 基本使用 - 默认模型
     */
    public void example1_BasicUsage() {
        log.info("=== 示例1: 基本使用 ===");
        
        // 创建默认LLM客户端
        LLMClient llm = modelFactory.createDefaultLLMClient();
        
        // 简单聊天
        String response = llm.chat("你好，请介绍一下自己");
        
        log.info("回复: {}", response);
    }
    
    /**
     * 示例2: 使用指定模型
     */
    public void example2_SpecificModel() {
        log.info("=== 示例2: 使用指定模型 ===");
        
        // 使用智谱AI
        LLMClient zhipuai = modelFactory.createLLMClient("zhipuai-glm4");
        String response1 = zhipuai.chat("什么是RAG?");
        log.info("智谱AI回复: {}", response1);
        
        // 使用通义千问
        LLMClient qwen = modelFactory.createLLMClient("qwen-turbo");
        String response2 = qwen.chat("什么是RAG?");
        log.info("通义千问回复: {}", response2);
    }
    
    /**
     * 示例3: 结构化请求
     */
    public void example3_StructuredRequest() {
        log.info("=== 示例3: 结构化请求 ===");
        
        LLMClient llm = modelFactory.createDefaultLLMClient();
        
        // 构建结构化请求
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatRequest.Message.builder()
                                .role("system")
                                .content("你是一个专业的技术顾问，擅长解释复杂的技术概念")
                                .build(),
                        ChatRequest.Message.builder()
                                .role("user")
                                .content("请用简单的语言解释什么是向量数据库")
                                .build()
                ))
                .temperature(0.7)
                .maxTokens(500)
                .build();
        
        // 发送请求
        ChatResponse response = llm.chat(request);
        
        log.info("回复: {}", response.getContent());
        log.info("模型: {}", response.getModel());
        
        if (response.getTokenUsage() != null) {
            log.info("Token使用: 输入={}, 输出={}, 总计={}", 
                    response.getTokenUsage().getPromptTokens(),
                    response.getTokenUsage().getCompletionTokens(),
                    response.getTokenUsage().getTotalTokens());
        }
    }
    
    /**
     * 示例4: 流式响应
     */
    public void example4_StreamingResponse() {
        log.info("=== 示例4: 流式响应 ===");
        
        LLMClient llm = modelFactory.createDefaultLLMClient();
        
        // 流式聊天
        Flux<String> stream = llm.streamChat("写一首关于春天的诗");
        
        // 处理流式响应
        StringBuilder fullResponse = new StringBuilder();
        stream.subscribe(
                token -> {
                    System.out.print(token); // 实时打印
                    fullResponse.append(token);
                },
                error -> log.error("流式响应错误", error),
                () -> {
                    System.out.println();
                    log.info("流式响应完成，总长度: {}", fullResponse.length());
                }
        );
    }
    
    /**
     * 示例5: 多轮对话
     */
    public void example5_MultiTurnConversation() {
        log.info("=== 示例5: 多轮对话 ===");
        
        LLMClient llm = modelFactory.createDefaultLLMClient();
        
        // 第一轮对话
        ChatRequest request1 = ChatRequest.builder()
                .messages(List.of(
                        ChatRequest.Message.builder()
                                .role("user")
                                .content("我叫小明，今年25岁")
                                .build()
                ))
                .build();
        
        ChatResponse response1 = llm.chat(request1);
        log.info("第一轮AI回复: {}", response1.getContent());
        
        // 第二轮对话（带上下文）
        ChatRequest request2 = ChatRequest.builder()
                .messages(List.of(
                        ChatRequest.Message.builder()
                                .role("user")
                                .content("我叫小明，今年25岁")
                                .build(),
                        ChatRequest.Message.builder()
                                .role("assistant")
                                .content(response1.getContent())
                                .build(),
                        ChatRequest.Message.builder()
                                .role("user")
                                .content("我叫什么名字？")
                                .build()
                ))
                .build();
        
        ChatResponse response2 = llm.chat(request2);
        log.info("第二轮AI回复: {}", response2.getContent());
    }
    
    /**
     * 示例6: 查询可用模型
     */
    public void example6_QueryAvailableModels() {
        log.info("=== 示例6: 查询可用模型 ===");
        
        // 获取所有可用的LLM模型
        List<String> llmModels = modelFactory.getAvailableLLMModels();
        log.info("可用的LLM模型: {}", llmModels);
        
        // 获取所有可用的Embedding模型
        List<String> embeddingModels = modelFactory.getAvailableEmbeddingModels();
        log.info("可用的Embedding模型: {}", embeddingModels);
        
        // 检查特定模型是否可用
        boolean available = modelFactory.isModelAvailable("zhipuai-glm4");
        log.info("zhipuai-glm4是否可用: {}", available);
    }
    
    /**
     * 示例7: 错误处理
     */
    public void example7_ErrorHandling() {
        log.info("=== 示例7: 错误处理 ===");
        
        try {
            // 尝试创建不存在的模型
            LLMClient client = modelFactory.createLLMClient("non-existent-model");
        } catch (Exception e) {
            log.error("创建模型失败: {}", e.getMessage());
        }
        
        try {
            // 模型调用失败处理
            LLMClient llm = modelFactory.createDefaultLLMClient();
            String response = llm.chat("测试");
            log.info("调用成功: {}", response);
        } catch (Exception e) {
            log.error("模型调用失败: {}", e.getMessage());
            // 这里可以实现降级逻辑，比如切换到备用模型
        }
    }
    
    /**
     * 示例8: 缓存管理
     */
    public void example8_CacheManagement() {
        log.info("=== 示例8: 缓存管理 ===");
        
        String modelId = "zhipuai-glm4";
        
        // 第一次创建（无缓存）
        long start1 = System.currentTimeMillis();
        LLMClient client1 = modelFactory.createLLMClient(modelId);
        long time1 = System.currentTimeMillis() - start1;
        log.info("第一次创建耗时: {}ms", time1);
        
        // 第二次创建（从缓存）
        long start2 = System.currentTimeMillis();
        LLMClient client2 = modelFactory.createLLMClient(modelId);
        long time2 = System.currentTimeMillis() - start2;
        log.info("第二次创建耗时: {}ms (缓存)", time2);
        
        log.info("两次是同一个实例: {}", client1 == client2);
        
        // 清除缓存
        modelFactory.clearCache(modelId);
        log.info("已清除缓存");
        
        // 清除缓存后再次创建
        long start3 = System.currentTimeMillis();
        LLMClient client3 = modelFactory.createLLMClient(modelId);
        long time3 = System.currentTimeMillis() - start3;
        log.info("清除缓存后创建耗时: {}ms", time3);
        
        log.info("清除缓存后是否为新实例: {}", client1 != client3);
    }
    
    /**
     * 运行所有示例
     */
    public void runAllExamples() {
        try {
            example1_BasicUsage();
            example2_SpecificModel();
            example3_StructuredRequest();
            example4_StreamingResponse();
            example5_MultiTurnConversation();
            example6_QueryAvailableModels();
            example7_ErrorHandling();
            example8_CacheManagement();
        } catch (Exception e) {
            log.error("运行示例失败", e);
        }
    }
}
