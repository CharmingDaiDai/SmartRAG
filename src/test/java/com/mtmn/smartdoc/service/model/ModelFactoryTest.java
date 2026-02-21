package com.mtmn.smartdoc.service.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtmn.smartdoc.model.client.LLMClient;
import com.mtmn.smartdoc.service.LLMService;
import com.mtmn.smartdoc.utils.SseUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SseUtil 单元测试
 * 适配 ServerSentEvent 和 Flux.concat 逻辑
 *
 * @author charmingdaidai
 * @version 2.0
 * @date 2025/11/26
 */
@ExtendWith(MockitoExtension.class)
class SseUtilTest {

    @Mock
    private LLMService llmService;

    @Mock
    private LLMClient mockLLMClient;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private SseUtil sseUtil;

    @BeforeEach
    void setUp() {
        // 宽松模式，因为某些测试可能不调用带参数的 createLLMClient
        lenient().when(llmService.createLLMClient()).thenReturn(mockLLMClient);
        lenient().when(llmService.createLLMClient(anyString())).thenReturn(mockLLMClient);
    }

    /**
     * 辅助方法：快速构建 ServerSentEvent 对象用于 Mock 返回
     */
    private ServerSentEvent<String> createSse(String data) {
        return ServerSentEvent.builder(data).build();
    }

    @Test
    void testBuildJsonSseMessage_WithContent() throws Exception {
        // Given
        String content = "测试内容";

        // When
        String result = sseUtil.buildJsonSseMessage(content, null);

        // Then
        assertThat(result).isNotNull();
        Map<String, Object> message = objectMapper.readValue(result, Map.class);
        assertThat(message).containsKey("id");
        assertThat(message).containsKey("object");
        assertThat(message).containsKey("choices");

        List<Map<String, Object>> choices = (List<Map<String, Object>>) message.get("choices");
        assertThat(choices).hasSize(1);

        Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
        assertThat(delta.get("content")).isEqualTo(content);
    }

    @Test
    void testBuildJsonSseMessage_WithDocs() throws Exception {
        // Given
        List<String> docs = Arrays.asList("文档1", "文档2", "文档3");

        // When
        String result = sseUtil.buildJsonSseMessage("", docs);

        // Then
        assertThat(result).isNotNull();
        Map<String, Object> message = objectMapper.readValue(result, Map.class);
        assertThat(message).containsKey("docs");

        List<String> returnedDocs = (List<String>) message.get("docs");
        assertThat(returnedDocs).hasSize(3);
        assertThat(returnedDocs).containsExactly("文档1", "文档2", "文档3");
    }

    @Test
    void testSendFluxMessage() {
        // Given
        String message = "测试消息";

        // When
        Flux<ServerSentEvent<String>> result = sseUtil.sendFluxMessage(message);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("测试消息"))
                .verifyComplete();
    }

    @Test
    void testHandleStreamingChatResponse_WithDefaultModel() {
        // Given
        String prompt = "你好";
        List<String> docs = Arrays.asList("文档1", "文档2");

        // 模拟 LLMClient 返回的是 SSE 流，且包含原始 token
        Flux<ServerSentEvent<String>> tokenStream = Flux.just(
                createSse("你"),
                createSse("好"),
                createSse("，"),
                createSse("世"),
                createSse("界")
        );

        when(mockLLMClient.streamChat(anyString())).thenReturn(tokenStream);

        // When
        Flux<ServerSentEvent<String>> result = sseUtil.handleStreamingChatResponse(prompt, docs);

        // Then
        StepVerifier.create(result)
                // 第一条消息应该是文档
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("docs"))
                // 后续是经过 SseUtil 包装成 JSON 的 token
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("你"))
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("好"))
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("，"))
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("世"))
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("界"))
                .verifyComplete();

        verify(llmService).createLLMClient();
        verify(mockLLMClient).streamChat(prompt);
    }

    @Test
    void testHandleStreamingChatResponse_WithSpecificModel() {
        // Given
        String prompt = "测试问题";
        String modelId = "zhipuai-glm4";
        Flux<ServerSentEvent<String>> tokenStream = Flux.just(
                createSse("回"),
                createSse("答")
        );

        when(mockLLMClient.streamChat(anyString())).thenReturn(tokenStream);

        // When
        Flux<ServerSentEvent<String>> result = sseUtil.handleStreamingChatResponse(prompt, null, modelId);

        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

        verify(llmService).createLLMClient(modelId);
    }

    @Test
    void testHandleStreamingChatResponse_WithoutDocs() {
        // Given
        String prompt = "问题";
        Flux<ServerSentEvent<String>> tokenStream = Flux.just(
                createSse("答"),
                createSse("案")
        );

        when(mockLLMClient.streamChat(anyString())).thenReturn(tokenStream);

        // When
        Flux<ServerSentEvent<String>> result = sseUtil.handleStreamingChatResponse(prompt, null);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("答"))
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("案"))
                .verifyComplete();
    }

    @Test
    void testHandleStreamingChatResponse_WithError() {
        // Given
        String prompt = "问题";
        Flux<ServerSentEvent<String>> errorStream = Flux.error(new RuntimeException("模型调用失败"));

        when(mockLLMClient.streamChat(anyString())).thenReturn(errorStream);

        // When
        Flux<ServerSentEvent<String>> result = sseUtil.handleStreamingChatResponse(prompt, null);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testCreateCustomStream() {
        // When
        // 注意：Sink 的泛型现在是 ServerSentEvent<String>
        Flux<ServerSentEvent<String>> result = sseUtil.createCustomStream(sink -> {
            sink.tryEmitNext(createSse("消息1"));
            sink.tryEmitNext(createSse("消息2"));
            sink.tryEmitNext(createSse("消息3"));
            sink.tryEmitComplete();
        });

        // Then
        StepVerifier.create(result)
                .expectNextMatches(sse -> "消息1".equals(sse.data()))
                .expectNextMatches(sse -> "消息2".equals(sse.data()))
                .expectNextMatches(sse -> "消息3".equals(sse.data()))
                .verifyComplete();
    }

    @Test
    void testCreateCustomStream_WithError() {
        // When
        Flux<ServerSentEvent<String>> result = sseUtil.createCustomStream(sink -> {
            sink.tryEmitNext(createSse("消息1"));
            throw new RuntimeException("自定义错误");
        });

        // Then
        StepVerifier.create(result)
                .expectNextMatches(sse -> "消息1".equals(sse.data()))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testHandleAdvancedStreamingResponse_WithAllParameters() {
        // Given
        String prompt = "复杂问题";
        List<String> docs = Arrays.asList("参考文档1", "参考文档2");
        List<String> thinkingSteps = Arrays.asList("步骤1", "步骤2");
        String modelId = "qwen-turbo";

        Flux<ServerSentEvent<String>> tokenStream = Flux.just(
                createSse("答"),
                createSse("案")
        );

        when(mockLLMClient.streamChat(anyString())).thenReturn(tokenStream);

        // When
        Flux<ServerSentEvent<String>> result = sseUtil.handleAdvancedStreamingResponse(prompt, docs, thinkingSteps, modelId);

        // Then
        StepVerifier.create(result)
                // 1. 验证文档
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("docs"))
                // 2. 验证思考步骤 (有两条)
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("thinking") && sse.data().contains("步骤1"))
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("thinking") && sse.data().contains("步骤2"))
                // 3. 验证 AI 响应
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("答"))
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("案"))
                .verifyComplete();

        verify(llmService).createLLMClient(modelId);
    }

    @Test
    void testHandleAdvancedStreamingResponse_WithoutThinking() {
        // Given
        String prompt = "问题";
        List<String> docs = Arrays.asList("文档");
        Flux<ServerSentEvent<String>> tokenStream = Flux.just(createSse("答"));

        when(mockLLMClient.streamChat(anyString())).thenReturn(tokenStream);

        // When
        Flux<ServerSentEvent<String>> result = sseUtil.handleAdvancedStreamingResponse(prompt, docs, null, null);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("docs"))
                .expectNextMatches(sse -> sse.data() != null && sse.data().contains("答"))
                .verifyComplete();
    }

    @Test
    void testBuildJsonSseMessage_WithSpecialCharacters() throws Exception {
        // Given
        String content = "包含特殊字符: \"引号\" 和 \n换行";

        // When
        String result = sseUtil.buildJsonSseMessage(content, null);

        // Then
        assertThat(result).isNotNull();
        Map<String, Object> message = objectMapper.readValue(result, Map.class);
        assertThat(message).isNotNull();
        // 确保转义后的内容能被正确反序列化出来
        List<Map<String, Object>> choices = (List<Map<String, Object>>) message.get("choices");
        Map<String, String> delta = (Map<String, String>) ((Map<String, Object>) choices.get(0)).get("delta");
        assertThat(delta.get("content")).isEqualTo(content);
    }

    @Test
    void testHandleStreamingChatResponse_NullTokenSafe() {
        // 边界测试：防止 NullPointerException 如果上游返回了 data 为 null 的 SSE
        String prompt = "Test";
        Flux<ServerSentEvent<String>> nullDataStream = Flux.just(
                ServerSentEvent.<String>builder().build() // data is null
        );

        when(mockLLMClient.streamChat(anyString())).thenReturn(nullDataStream);

        Flux<ServerSentEvent<String>> result = sseUtil.handleStreamingChatResponse(prompt, null);

        StepVerifier.create(result)
                .expectComplete() // 应该直接忽略并完成，不报错
                .verify();
    }
}