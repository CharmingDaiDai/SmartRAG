package com.mtmn.smartdoc.service.model;

import com.mtmn.smartdoc.service.model.client.LLMClient;
import com.mtmn.smartdoc.service.model.dto.ChatRequest;
import com.mtmn.smartdoc.service.model.dto.ChatResponse;
import com.mtmn.smartdoc.service.model.exception.ModelException;
import com.mtmn.smartdoc.service.model.factory.ModelFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 模型工厂测试类
 * 
 * <p>测试各个提供商的LLM客户端创建和基本功能</p>
 * 
 * @author charmingdaidai
 * @version 2.1
 * @since 2025-01-17
 */
@SpringBootTest
@Slf4j
class ModelFactoryTest {
    
    @Autowired
    private ModelFactory modelFactory;

    /**
     * 测试创建默认LLM客户端
     */
    @Test
    void testCreateDefaultLLMClient() {
        log.info("测试创建默认LLM客户端");
        
        LLMClient client = modelFactory.createDefaultLLMClient();
        
        assertNotNull(client, "默认LLM客户端不应为null");
        assertNotNull(client.getModelId(), "模型ID不应为null");
        assertNotNull(client.getModelName(), "模型名称不应为null");
        assertNotNull(client.getProviderType(), "提供商类型不应为null");
        
        log.info("默认LLM客户端: modelId={}, modelName={}, provider={}", 
                client.getModelId(), client.getModelName(), client.getProviderType());
    }
    
    /**
     * 测试创建指定的LLM客户端
     */
    @Test
    void testCreateSpecificLLMClient() {
        log.info("测试创建指定的LLM客户端");
        
        // 测试智谱AI
        LLMClient zhipuaiClient = modelFactory.createLLMClient("zhipuai@glm-4-flash");
        assertNotNull(zhipuaiClient);
        assertEquals("zhipuai@glm-4-flash", zhipuaiClient.getModelId());
        assertEquals("glm-4-flash", zhipuaiClient.getModelName());
        assertEquals("zhipuai", zhipuaiClient.getProviderType());
        
        log.info("创建智谱AI客户端成功: {}", zhipuaiClient.getModelId());
    }
    
    /**
     * 测试创建不存在的模型
     */
    @Test
    void testCreateNonExistentModel() {
        log.info("测试创建不存在的模型");
        
        assertThrows(ModelException.class, () -> {
            modelFactory.createLLMClient("invalid-model-id");
        }, "应该抛出ModelException");
        
        log.info("正确抛出了ModelException");
    }
    
    /**
     * 测试LLM同步聊天
     */
    @Test
    void testLLMSyncChat() {
        log.info("测试LLM同步聊天");
        
        LLMClient client = modelFactory.createDefaultLLMClient();
        
        String response = client.chat("你好，请简单介绍一下自己");
        
        assertNotNull(response, "响应不应为null");
        assertFalse(response.isEmpty(), "响应不应为空");
        
        log.info("LLM同步聊天响应: {}", response.substring(0, Math.min(100, response.length())));
    }
    
    /**
     * 测试LLM结构化聊天
     */
    @Test
    void testLLMStructuredChat() {
        log.info("测试LLM结构化聊天");
        
        LLMClient client = modelFactory.createDefaultLLMClient();
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatRequest.Message.builder()
                                .role("system")
                                .content("你是一个专业的AI助手")
                                .build(),
                        ChatRequest.Message.builder()
                                .role("user")
                                .content("什么是RAG?")
                                .build()
                ))
                .temperature(0.7)
                .build();
        
        ChatResponse response = client.chat(request);
        
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getContent(), "响应内容不应为null");
        assertFalse(response.getContent().isEmpty(), "响应内容不应为空");
        assertEquals(client.getModelName(), response.getModel(), "模型名称应该匹配");
        
        log.info("LLM结构化聊天响应: content={}, finishReason={}", 
                response.getContent().substring(0, Math.min(100, response.getContent().length())),
                response.getFinishReason());
        
        if (response.getTokenUsage() != null) {
            log.info("Token使用情况: prompt={}, completion={}, total={}", 
                    response.getTokenUsage().getPromptTokens(),
                    response.getTokenUsage().getCompletionTokens(),
                    response.getTokenUsage().getTotalTokens());
        }
    }
    
    /**
     * 测试LLM流式聊天
     */
    @Test
    void testLLMStreamChat() {
        log.info("测试LLM流式聊天");
        
        LLMClient client = modelFactory.createDefaultLLMClient();
        
        Flux<String> stream = client.streamChat("讲一个笑话");
        
        StepVerifier.create(stream)
                .expectNextCount(1) // 至少应该有一个token
                .thenConsumeWhile(token -> {
                    log.debug("收到token: {}", token);
                    return true;
                })
                .expectComplete()
                .verify();
        
        log.info("LLM流式聊天测试完成");
    }
    
    /**
     * 测试获取可用模型列表
     */
    @Test
    void testGetAvailableModels() {
        log.info("测试获取可用模型列表");
        
        List<String> llmModels = modelFactory.getAvailableLLMModels();
        List<String> embeddingModels = modelFactory.getAvailableEmbeddingModels();
        
        assertNotNull(llmModels, "LLM模型列表不应为null");
        assertNotNull(embeddingModels, "Embedding模型列表不应为null");
        assertFalse(llmModels.isEmpty(), "LLM模型列表不应为空");
        
        log.info("可用的LLM模型: {}", llmModels);
        log.info("可用的Embedding模型: {}", embeddingModels);
    }
    
    /**
     * 测试模型缓存
     */
    @Test
    void testModelCache() {
        log.info("测试模型缓存");
        
        LLMClient client1 = modelFactory.createLLMClient("zhipuai@glm-4-flash");
        LLMClient client2 = modelFactory.createLLMClient("zhipuai@glm-4-flash");
        
        // 应该返回相同的实例(从缓存中获取)
        assertSame(client1, client2, "应该返回相同的实例");
        
        log.info("缓存验证成功");
        
        // 清除缓存
        modelFactory.clearCache("zhipuai@glm-4-flash");
        
        LLMClient client3 = modelFactory.createLLMClient("zhipuai@glm-4-flash");
        
        // 清除缓存后应该是新实例
        assertNotSame(client1, client3, "清除缓存后应该是新实例");
        
        log.info("缓存清除验证成功");
    }
    
    /**
     * 测试模型可用性检查
     */
    @Test
    void testIsModelAvailable() {
        log.info("测试模型可用性检查");
        
        assertTrue(modelFactory.isModelAvailable("zhipuai@glm-4.5-flash"),
                "zhipuai@glm-4.5-flash应该可用");
        
        assertFalse(modelFactory.isModelAvailable("invalid-model"), 
                "invalid-model不应该可用");
        
        assertFalse(modelFactory.isModelAvailable("invalidformat"), 
                "格式错误的modelId不应该可用");
        
        log.info("模型可用性检查测试完成");
    }
    
    /**
     * 测试多个提供商
     */
    @Test
    void testMultipleProviders() {
        log.info("测试多个提供商");
        
        List<String> availableModels = modelFactory.getAvailableLLMModels();
        
        // 测试每个可用的模型
        for (String modelId : availableModels) {
            try {
                LLMClient client = modelFactory.createLLMClient(modelId);
                assertNotNull(client, "客户端不应为null: " + modelId);
                log.info("成功创建客户端: modelId={}, provider={}", 
                        modelId, client.getProviderType());
            } catch (Exception e) {
                log.warn("创建客户端失败: modelId={}, error={}", modelId, e.getMessage());
            }
        }
    }
    
    /**
     * 测试创建默认Embedding客户端(待实现)
     */
    @Test
    void testCreateDefaultEmbeddingClient() {
        log.info("测试创建默认Embedding客户端");
        
        // 这个测试会失败，因为Embedding客户端还未实现
        assertThrows(UnsupportedOperationException.class, () -> {
            modelFactory.createDefaultEmbeddingClient();
        }, "Embedding客户端尚未实现，应该抛出UnsupportedOperationException");
        
        log.info("Embedding客户端待实现");
    }
    
    /**
     * 性能测试 - 缓存效果
     */
    @Test
    void testCachePerformance() {
        log.info("测试缓存性能");
        
        String modelId = "zhipuai@glm-4-flash";
        
        // 第一次创建(无缓存)
        long startTime1 = System.currentTimeMillis();
        LLMClient client1 = modelFactory.createLLMClient(modelId);
        long time1 = System.currentTimeMillis() - startTime1;
        
        // 第二次创建(有缓存)
        long startTime2 = System.currentTimeMillis();
        LLMClient client2 = modelFactory.createLLMClient(modelId);
        long time2 = System.currentTimeMillis() - startTime2;
        
        log.info("第一次创建耗时: {}ms", time1);
        log.info("第二次创建耗时: {}ms (缓存)", time2);
        
        // 缓存应该显著快于第一次创建
        assertTrue(time2 <= time1, "缓存访问应该快于或等于首次创建");
        
        assertSame(client1, client2, "应该返回相同的实例");
    }
    
    /**
     * 测试清除所有缓存
     */
    @Test
    void testClearAllCache() {
        log.info("测试清除所有缓存");
        
        // 创建几个客户端
        LLMClient client1 = modelFactory.createLLMClient("zhipuai@glm-4-flash");
        
        // 清除所有缓存
        modelFactory.clearAllCache();
        
        // 再次创建应该是新实例
        LLMClient client2 = modelFactory.createLLMClient("zhipuai@glm-4-flash");
        
        assertNotSame(client1, client2, "清除所有缓存后应该是新实例");
        
        log.info("清除所有缓存测试完成");
    }
    
    // ========== 各提供商专项测试 ==========
    
    /**
     * 测试智谱AI客户端创建
     */
    @Test
    void testZhiPuAIClientCreation() {
        log.info("测试智谱AI客户端创建");
        
        // 测试GLM-4 Flash
        LLMClient glm4Client = modelFactory.createLLMClient("zhipuai@glm-4-flash");
        assertNotNull(glm4Client);
        assertEquals("zhipuai", glm4Client.getProviderType());
        assertEquals("glm-4-flash", glm4Client.getModelName());
        
        // 测试GLM-4.5 Flash
        LLMClient glm45Client = modelFactory.createLLMClient("zhipuai@glm-4.5-flash");
        assertNotNull(glm45Client);
        assertEquals("glm-4.5-flash", glm45Client.getModelName());
        
        log.info("智谱AI客户端创建测试完成");
    }
    
    /**
     * 测试通义千问客户端创建
     */
    @Test
    void testQwenClientCreation() {
        log.info("测试通义千问客户端创建");
        
        // 测试Qwen-Max
        LLMClient maxClient = modelFactory.createLLMClient("qwen@qwen-max");
        assertNotNull(maxClient);
        assertEquals("qwen", maxClient.getProviderType());
        assertEquals("qwen-max", maxClient.getModelName());
        
        // 测试Qwen-Plus
        LLMClient plusClient = modelFactory.createLLMClient("qwen@qwen-plus");
        assertNotNull(plusClient);
        assertEquals("qwen-plus", plusClient.getModelName());
        
        log.info("通义千问客户端创建测试完成");
    }
    
    /**
     * 测试Xinference客户端创建
     */
    @Test
    void testXinferenceClientCreation() {
        log.info("测试Xinference客户端创建");
        
        // 测试Qwen2.5模型
        LLMClient qwenClient = modelFactory.createLLMClient("xinference@qwen2.5-instruct");
        assertNotNull(qwenClient);
        assertEquals("xinference", qwenClient.getProviderType());
        assertEquals("qwen2.5-instruct", qwenClient.getModelName());
        
        log.info("Xinference客户端创建测试完成");
    }
    
    /**
     * 测试OpenAI客户端创建
     */
    @Test
    void testOpenAIClientCreation() {
        log.info("测试OpenAI客户端创建");
        
        // 测试GPT-4
        LLMClient gpt4Client = modelFactory.createLLMClient("openai@gpt-4");
        assertNotNull(gpt4Client);
        assertEquals("openai", gpt4Client.getProviderType());
        assertEquals("gpt-4", gpt4Client.getModelName());
        
        // 测试GPT-3.5-Turbo
        LLMClient gpt35Client = modelFactory.createLLMClient("openai@gpt-3.5-turbo");
        assertNotNull(gpt35Client);
        assertEquals("gpt-3.5-turbo", gpt35Client.getModelName());
        
        log.info("OpenAI客户端创建测试完成");
    }
    
    /**
     * 测试智谱AI同步聊天
     */
    @Test
    void testZhiPuAISyncChat() {
        log.info("测试智谱AI同步聊天");
        
        LLMClient client = modelFactory.createLLMClient("zhipuai@glm-4-flash");
        
        String response = client.chat("你好，请简单介绍一下自己");
        
        assertNotNull(response, "响应不应为null");
        assertFalse(response.isEmpty(), "响应不应为空");
        
        log.info("智谱AI同步聊天响应: {}", response.substring(0, Math.min(100, response.length())));
    }
    
    /**
     * 测试通义千问结构化聊天
     */
    @Test
    void testQwenStructuredChat() {
        log.info("测试通义千问结构化聊天");
        
        LLMClient client = modelFactory.createLLMClient("qwen@qwen-max");
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatRequest.Message.builder()
                                .role("system")
                                .content("你是一个专业的AI助手")
                                .build(),
                        ChatRequest.Message.builder()
                                .role("user")
                                .content("什么是RAG?")
                                .build()
                ))
                .temperature(0.7)
                .build();
        
        ChatResponse response = client.chat(request);
        
        assertNotNull(response, "响应不应为null");
        assertNotNull(response.getContent(), "响应内容不应为null");
        assertFalse(response.getContent().isEmpty(), "响应内容不应为空");
        
        log.info("通义千问结构化聊天响应: content={}, finishReason={}", 
                response.getContent().substring(0, Math.min(100, response.getContent().length())),
                response.getFinishReason());
    }
    
    /**
     * 测试Xinference流式聊天
     */
    @Test
    void testXinferenceStreamChat() {
        log.info("测试Xinference流式聊天");
        
        LLMClient client = modelFactory.createLLMClient("xinference@qwen2.5-instruct");
        
        Flux<String> stream = client.streamChat("讲一个笑话");
        
        StepVerifier.create(stream)
                .expectNextCount(1) // 至少应该有一个token
                .thenConsumeWhile(token -> {
                    log.debug("收到token: {}", token);
                    return true;
                })
                .expectComplete()
                .verify();
        
        log.info("Xinference流式聊天测试完成");
    }
    
    /**
     * 测试多提供商并发创建
     */
    @Test
    void testMultiProviderConcurrentCreation() {
        log.info("测试多提供商并发创建");
        
        List<String> modelIds = List.of(
                "zhipuai@glm-4-flash",
                "qwen@qwen-plus",
                "xinference@qwen2.5-instruct",
                "openai@gpt-3.5-turbo"
        );
        
        for (String modelId : modelIds) {
            try {
                LLMClient client = modelFactory.createLLMClient(modelId);
                assertNotNull(client, "客户端不应为null: " + modelId);
                assertEquals(modelId, client.getModelId(), "模型ID应该匹配");
                log.info("成功创建客户端: {}", modelId);
            } catch (Exception e) {
                log.warn("创建客户端失败: modelId={}, error={}", modelId, e.getMessage());
            }
        }
        
        log.info("多提供商并发创建测试完成");
    }
    
    /**
     * 测试模型切换
     */
    @Test
    void testModelSwitching() {
        log.info("测试模型切换");
        
        // 创建智谱模型
        LLMClient zhipuClient = modelFactory.createLLMClient("zhipuai@glm-4-flash");
        assertEquals("zhipuai", zhipuClient.getProviderType());
        
        // 切换到通义千问
        LLMClient qwenClient = modelFactory.createLLMClient("qwen@qwen-max");
        assertEquals("qwen", qwenClient.getProviderType());
        
        // 切换到Xinference
        LLMClient xinferenceClient = modelFactory.createLLMClient("xinference@qwen2.5-instruct");
        assertEquals("xinference", xinferenceClient.getProviderType());
        
        // 验证它们是不同的实例
        assertNotSame(zhipuClient, qwenClient);
        assertNotSame(qwenClient, xinferenceClient);
        
        log.info("模型切换测试完成");
    }
    
    /**
     * 测试错误的模型ID格式
     */
    @Test
    void testInvalidModelIdFormat() {
        log.info("测试错误的模型ID格式");
        
        // 测试缺少@分隔符
        assertThrows(ModelException.class, () -> {
            modelFactory.createLLMClient("zhipuai-glm4");
        }, "应该抛出ModelException");
        
        // 测试只有提供商名
        assertThrows(ModelException.class, () -> {
            modelFactory.createLLMClient("zhipuai");
        }, "应该抛出ModelException");
        
        // 测试空字符串
        assertThrows(Exception.class, () -> {
            modelFactory.createLLMClient("");
        }, "应该抛出异常");
        
        log.info("错误格式测试完成");
    }
    
    /**
     * 测试不存在的提供商
     */
    @Test
    void testNonExistentProvider() {
        log.info("测试不存在的提供商");
        
        assertThrows(ModelException.class, () -> {
            modelFactory.createLLMClient("unknown@some-model");
        }, "应该抛出ModelException");
        
        log.info("不存在的提供商测试完成");
    }
    
    /**
     * 测试不存在的模型
     */
    @Test
    void testNonExistentModel() {
        log.info("测试不存在的模型");
        
        assertThrows(ModelException.class, () -> {
            modelFactory.createLLMClient("zhipuai@non-existent-model");
        }, "应该抛出ModelException");
        
        log.info("不存在的模型测试完成");
    }
    
    /**
     * 测试获取所有可用模型
     */
    @Test
    void testGetAllAvailableModels() {
        log.info("测试获取所有可用模型");
        
        List<String> llmModels = modelFactory.getAvailableLLMModels();
        
        assertNotNull(llmModels);
        assertFalse(llmModels.isEmpty());
        
        // 验证模型ID格式
        for (String modelId : llmModels) {
            assertTrue(modelId.contains("@"), "模型ID应该包含@分隔符: " + modelId);
            String[] parts = modelId.split("@");
            assertEquals(2, parts.length, "模型ID应该是provider@modelName格式");
        }
        
        log.info("可用LLM模型: {}", llmModels);
    }
    
    /**
     * 测试批量模型可用性检查
     */
    @Test
    void testBatchModelAvailability() {
        log.info("测试批量模型可用性检查");
        
        List<String> testModels = List.of(
                "zhipuai@glm-4-flash",       // 应该可用
                "qwen@qwen-max",            // 应该可用
                "xinference@qwen2.5-instruct", // 应该可用
                "openai@gpt-4",               // 应该可用
                "invalid@model",              // 不可用
                "wrong-format"                 // 不可用
        );
        
        for (String modelId : testModels) {
            boolean available = modelFactory.isModelAvailable(modelId);
            log.info("模型 {} 可用性: {}", modelId, available);
        }
        
        log.info("批量模型可用性检查完成");
    }
}