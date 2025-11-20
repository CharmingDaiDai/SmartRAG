package com.mtmn.smartdoc.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.service.LLMService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.*;

/**
 * SSE 工具类
 * 提供流式消息推送功能,支持在同一个SSE连接中发送不同类型的消息
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025/11/20
 */
@Slf4j
@Component
public class SseUtil {

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private LLMService llmService;

    /**
     * 构建SSE消息响应格式
     *
     * @param content 消息内容
     * @return 格式化的SSE消息 Json字符串
     */
    public String buildJsonSseMessage(String content, List<String> docs) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("id", "chat" + UUID.randomUUID());
            message.put("object", "chat.completion.chunk");

            // 如果传入的 docs 非空（意味着先把检索到的文档列表一起推给前端）
            if (docs != null) {
                message.put("docs", docs);
                return objectMapper.writeValueAsString(message);
            }

            // 否则，正常构造一个“delta”片段，表示模型最新输出的一段 content
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
            // 如果 JSON 构建失败，返回一个简单的错误片段，前端可以收到 {"error":"构建消息失败"}
            return "data: {\"error\":\"构建消息失败\"}\n\n";
        }
    }

    /**
     * 创建包含消息的SSE流
     *
     * @param message 信息内容
     * @return 格式化的消息流
     */
    // FixMe 不能正确流式输出
    public Flux<String> sendFluxMessage(String message) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        // 先把传入的 message 通过 buildJsonSseMessage 转成 JSON
        sink.tryEmitNext(buildJsonSseMessage(message, null));
        // 然后立即告诉 Sink：数据推送完毕，可以完成（complete）了
        sink.tryEmitComplete();
        return sink.asFlux();
    }

    /**
     * 处理流式聊天响应 - 使用默认模型
     *
     * @param prompt      提示词
     * @param docContents 检索到的文档内容列表（可以为null）
     * @return 格式化的SSE消息流
     */
    public Flux<String> handleStreamingChatResponse(String prompt, List<String> docContents) {
        return handleStreamingChatResponse(prompt, docContents, null);
    }

    /**
     * 处理流式聊天响应 - 指定模型
     *
     * @param prompt      提示词
     * @param docContents 检索到的文档内容列表（可以为null）
     * @param modelId     模型ID（可以为null，使用默认模型）
     * @return 格式化的SSE消息流
     */
    public Flux<String> handleStreamingChatResponse(String prompt, List<String> docContents, String modelId) {
        // 1. 创建流式聊天客户端
        LLMClient llmClient = modelId == null ?
                llmService.createLLMClient() :
                llmService.createLLMClient(modelId);

        // 2. 创建一个只允许单订阅者、带缓冲的 Sink
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 3. 如果前面检索到的文档非空，先把 docs 按照 SSE 消息格式推给前端
        if (docContents != null && !docContents.isEmpty()) {
            sink.tryEmitNext(buildJsonSseMessage("", docContents));
        }

        // 4. 使用新的 LLMClient 流式调用接口
        llmClient.streamChat(prompt)
                .doOnNext(token -> {
                    // 模型每生成一小段文本（token），就会触发一次
                    // 先把里边的双引号、换行做转义，然后构造 JSON 片段
                    String escapedContent = token.replace("\"", "\\\"").replace("\n", "\\n");
                    sink.tryEmitNext(buildJsonSseMessage(escapedContent, null));
                })
                .doOnComplete(() -> {
                    // 当模型整次对话生成完毕后触发
                    log.debug("流式聊天响应完成");
                    sink.tryEmitComplete(); // 标记当前 Sink 的 Flux 流结束
                })
                .doOnError(error -> {
                    // 发生错误时触发
                    log.error("聊天响应处理出错", error);
                    sink.tryEmitError(error); // 把异常推给下游，Flux 会触发 onError
                })
                .subscribe(); // 订阅启动流式处理

        // 5. 返回这个 Sink 对应的 Flux，供外层（Controller）订阅
        return sink.asFlux();
    }

    /**
     * 自定义流式消息推送
     * 可以在同一个SSE连接中发送多种类型的消息
     *
     * @param messageProducer 消息生产者函数，接收sink作为参数，可以向其推送各种消息
     * @return 格式化的SSE消息流
     */
    public Flux<String> createCustomStream(java.util.function.Consumer<Sinks.Many<String>> messageProducer) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        try {
            // 执行自定义的消息推送逻辑
            messageProducer.accept(sink);
        } catch (Exception e) {
            log.error("自定义流式消息推送失败", e);
            sink.tryEmitError(e);
        }

        return sink.asFlux();
    }

    /**
     * 高级用法：组合多个数据源的流式推送
     * 例如：先推送检索文档 -> 再推送思考过程 -> 最后推送AI响应
     *
     * @param prompt        提示词
     * @param docContents   检索到的文档内容
     * @param thinkingSteps 思考步骤（可选）
     * @param modelId       模型ID（可选）
     * @return 格式化的SSE消息流
     */
    public Flux<String> handleAdvancedStreamingResponse(
            String prompt,
            List<String> docContents,
            List<String> thinkingSteps,
            String modelId) {

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 步骤1: 推送检索到的文档
        if (docContents != null && !docContents.isEmpty()) {
            sink.tryEmitNext(buildJsonSseMessage("", docContents));
        }

        // 步骤2: 推送思考步骤（如果有）
        if (thinkingSteps != null && !thinkingSteps.isEmpty()) {
            for (String step : thinkingSteps) {
                Map<String, Object> thinkingMessage = new HashMap<>();
                thinkingMessage.put("type", "thinking");
                thinkingMessage.put("content", step);
                try {
                    sink.tryEmitNext(objectMapper.writeValueAsString(thinkingMessage));
                } catch (Exception e) {
                    log.error("推送思考步骤失败", e);
                }
            }
        }

        // 步骤3: 推送AI流式响应
        LLMClient llmClient = modelId == null ?
                llmService.createLLMClient() :
                llmService.createLLMClient(modelId);

        llmClient.streamChat(prompt)
                .doOnNext(token -> {
                    String escapedContent = token.replace("\"", "\\\"").replace("\n", "\\n");
                    sink.tryEmitNext(buildJsonSseMessage(escapedContent, null));
                })
                .doOnComplete(() -> {
                    log.debug("高级流式响应完成");
                    sink.tryEmitComplete();
                })
                .doOnError(error -> {
                    log.error("高级流式响应处理出错", error);
                    sink.tryEmitError(error);
                })
                .subscribe();

        return sink.asFlux();
    }
}