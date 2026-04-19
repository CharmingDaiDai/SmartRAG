package com.mtmn.smartrag.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartrag.model.client.LLMClient;
import com.mtmn.smartrag.service.LLMService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * SSE 工具类
 * 改用 Flux.concat 组合流，避免在非阻塞上下文中显式调用 subscribe()
 *
 * @author charmingdaidai
 * @version 2.2
 * @date 2025/11/26
 */
@Slf4j
@Component
public class SseUtil {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private LLMService llmService;

    /**
     * 辅助方法：将 JSON 字符串包装为 ServerSentEvent 对象
     */
    private ServerSentEvent<String> wrapSse(String jsonContent) {
        return ServerSentEvent.builder(jsonContent).build();
    }

    /**
     * 构建SSE消息响应格式 (逻辑保持不变)
     */
    public String buildJsonSseMessage(String content, List<String> docs) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("id", "chat" + UUID.randomUUID());
            message.put("object", "chat.completion.chunk");

            if (docs != null) {
                message.put("docs", docs);
                return objectMapper.writeValueAsString(message);
            }

            List<Map<String, Object>> choices = new ArrayList<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, String> delta = new HashMap<>();

            delta.put("content", content);
            choice.put("delta", delta);
            choice.put("role", "assistant");
            choices.add(choice);
            message.put("choices", choices);

            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("构建SSE消息失败", e);
            return "{\"error\":\"构建消息失败\"}";
        }
    }

    /**
     * 创建包含消息的SSE流
     * 改为直接返回 Flux.just，无需 Sink
     */
    public Flux<ServerSentEvent<String>> sendFluxMessage(String message) {
        String json = buildJsonSseMessage(message, null);
        return Flux.just(wrapSse(json));
    }

    /**
     * 处理流式聊天响应 - 使用默认模型
     */
    public Flux<ServerSentEvent<String>> handleStreamingChatResponse(String prompt, List<String> docContents) {
        return handleStreamingChatResponse(prompt, docContents, null);
    }

    /**
     * 处理流式聊天响应 - 指定模型
     * 核心修改：使用 Flux.concat 拼接文档流和聊天流
     */
    public Flux<ServerSentEvent<String>> handleStreamingChatResponse(String prompt, List<String> docContents, String modelId) {
        // 1. 创建 LLM 客户端
        LLMClient llmClient = modelId == null ?
                llmService.createLLMClient() :
                llmService.createLLMClient(modelId);

        // 2. 构建文档消息流 (如果 docContents 为空，则是一个空流)
        Flux<ServerSentEvent<String>> docsFlux = Flux.empty();
        if (docContents != null && !docContents.isEmpty()) {
            String docsJson = buildJsonSseMessage("", docContents);
            docsFlux = Flux.just(wrapSse(docsJson));
        }

        // 3. 构建聊天消息流 (通过 map 转换上游数据，而不是 doOnNext + sink)
        Flux<ServerSentEvent<String>> chatFlux = llmClient.streamChat(prompt)
                .map(clientSse -> {
                    String token = clientSse.data();
                    // 处理 null 情况，避免 map 返回 null (Reactor 不允许)
                    if (token == null) {
                        return "";
                    }
                    // 保留原有的转义逻辑
                    String escapedContent = token.replace("\"", "\\\"").replace("\n", "\\n");
                    return buildJsonSseMessage(escapedContent, null);
                })
                .filter(json -> !json.isEmpty()) // 过滤掉空内容
                .map(this::wrapSse) // 包装成 ServerSentEvent
                .doOnComplete(() -> log.debug("流式聊天响应完成"))
                .doOnError(e -> log.error("聊天响应处理出错", e));

        // 4. 按顺序拼接：先发文档，再发聊天
        return Flux.concat(docsFlux, chatFlux);
    }

    /**
     * 自定义流式消息推送
     * 既然是自定义 Producer，这里保留 Sink 是合理的，因为需要桥接命令式代码
     * 但要注意调用方必须正确处理 sink
     */
    public Flux<ServerSentEvent<String>> createCustomStream(java.util.function.Consumer<reactor.core.publisher.Sinks.Many<ServerSentEvent<String>>> messageProducer) {
        reactor.core.publisher.Sinks.Many<ServerSentEvent<String>> sink = reactor.core.publisher.Sinks.many().unicast().onBackpressureBuffer();
        try {
            messageProducer.accept(sink);
        } catch (Exception e) {
            log.error("自定义流式消息推送失败", e);
            sink.tryEmitError(e);
        }
        return sink.asFlux();
    }

    /**
     * 高级用法：组合多个数据源的流式推送
     * 核心修改：使用 Flux.concat 拼接三个流
     */
    public Flux<ServerSentEvent<String>> handleAdvancedStreamingResponse(
            String prompt,
            List<String> docContents,
            List<String> thinkingSteps,
            String modelId) {

        // 1. 文档流
        Flux<ServerSentEvent<String>> docsFlux = Flux.empty();
        if (docContents != null && !docContents.isEmpty()) {
            docsFlux = Flux.just(wrapSse(buildJsonSseMessage("", docContents)));
        }

        // 2. 思考过程流
        Flux<ServerSentEvent<String>> thinkingFlux = Flux.empty();
        if (thinkingSteps != null && !thinkingSteps.isEmpty()) {
            // 将 List 转为 Flux
            thinkingFlux = Flux.fromIterable(thinkingSteps)
                    .map(step -> {
                        Map<String, Object> thinkingMessage = new HashMap<>();
                        thinkingMessage.put("type", "thinking");
                        thinkingMessage.put("content", step);
                        try {
                            return objectMapper.writeValueAsString(thinkingMessage);
                        } catch (Exception e) {
                            log.error("序列化思考步骤失败", e);
                            return "";
                        }
                    })
                    .filter(json -> !json.isEmpty())
                    .map(this::wrapSse);
        }

        // 3. AI 响应流
        LLMClient llmClient = modelId == null ?
                llmService.createLLMClient() :
                llmService.createLLMClient(modelId);

        Flux<ServerSentEvent<String>> chatFlux = llmClient.streamChat(prompt)
                .map(clientSse -> {
                    String token = clientSse.data();
                    if (token == null) {
                        return "";
                    }
                    String escapedContent = token.replace("\"", "\\\"").replace("\n", "\\n");
                    return buildJsonSseMessage(escapedContent, null);
                })
                .filter(json -> !json.isEmpty())
                .map(this::wrapSse)
                .doOnError(e -> log.error("高级流式响应出错", e));

        // 4. 依次拼接：文档 -> 思考 -> 聊天
        return Flux.concat(docsFlux, thinkingFlux, chatFlux);
    }
}