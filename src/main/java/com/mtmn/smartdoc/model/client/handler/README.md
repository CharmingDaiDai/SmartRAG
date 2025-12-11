# StreamEventHandler 使用指南

## 📖 设计思想

将流式输出的控制权交给调用方，支持不同的对话场景：
- **普通对话**：直接输出 LLM 响应文本
- **RAG 对话**：先发送检索步骤，再输出 LLM 响应（SSE 格式）
- **Agent 对话**：发送工具调用步骤
- **自定义场景**：根据业务需求自由扩展

## 🚀 快速开始

### 1. 普通对话（默认）

```java
// 使用默认的 streamChat，不发送任何额外事件
Flux<String> response = llmClient.streamChat("什么是 RAG");

// 前端收到：
// "R" "A" "G" " " "是" "检" "索" "增" "强" "生" "成" "..."
```

### 2. 简单 RAG 对话

```java
import com.mtmn.smartdoc.model.client.handler.SimpleRagEventHandler;

// 使用 SimpleRagEventHandler
Flux<String> response = llmClient.streamChat("什么是 RAG", new SimpleRagEventHandler());

// 前端收到：
// event: thought
// data: {"status":"processing","content":"正在解析用户意图...","icon":"search"}
//
// event: thought
// data: {"status":"processing","content":"正在检索向量数据库...","icon":"search"}
//
// event: thought
// data: {"status":"success","content":"检索完成，正在生成回答...","icon":"check"}
//
// event: message
// data: {"delta":"R"}
//
// event: message
// data: {"delta":"A"}
// ...
// event: done
// data: [DONE]
```

### 3. 高级 RAG 对话

```java
import com.mtmn.smartdoc.model.client.handler.AdvancedRagEventHandler;

// 使用 AdvancedRagEventHandler（包含查询扩展、多路检索、重排序等步骤）
Flux<String> response = llmClient.streamChat("什么是 RAG", new AdvancedRagEventHandler());

// 前端收到更详细的检索步骤：
// event: thought (查询扩展)
// event: thought (意图识别)
// event: thought (多路检索)
// event: thought (重排序)
// event: thought (准备生成)
// event: message (AI 响应)
// event: done
```

### 4. 自定义 RAG 对话（带检索结果）

```java
import com.mtmn.smartdoc.model.client.handler.CustomRagEventHandler;
import com.mtmn.smartdoc.model.dto.ReferenceDocument;

// 先执行检索，构建参考文档列表
List<ReferenceDocument> retrievedDocs = Arrays.asList(
    ReferenceDocument.builder()
        .title("国家电网输变电工程标准工艺.pdf")
        .score(0.95)
        .content("变电工程电气分册相关内容...")
        .documentId(1001L)
        .chunkId("chunk_001")
        .build(),
    ReferenceDocument.builder()
        .title("设计模式.md")
        .score(0.87)
        .content("策略模式的实现...")
        .documentId(1002L)
        .chunkId("chunk_005")
        .build()
);

// 创建自定义 Handler，传入检索结果
StreamEventHandler handler = new CustomRagEventHandler(retrievedDocs);
Flux<String> response = llmClient.streamChat(prompt, handler);

// 前端收到：
// event: thought (检索步骤)
// event: ref (参考文档列表)
// data: [{"title":"国家电网...","score":0.95,"content":"..."},{"title":"设计模式...","score":0.87,"content":"..."}]
//
// event: message (AI 响应)
// event: done
```

## 🎨 自定义事件处理器

### 示例1: Agent 工具调用

```java
public class AgentToolEventHandler implements StreamEventHandler {
    
    private final List<String> toolCalls;
    
    @Override
    public void onBeforeChat(FluxSink<String> sink, String prompt) {
        // 发送工具调用步骤
        sink.next(SseEventBuilder.buildThoughtEvent("processing", "正在分析需要调用的工具...", "tool"));
        
        for (String tool : toolCalls) {
            sink.next(SseEventBuilder.buildThoughtEvent("processing", "正在调用工具: " + tool, "api"));
            // 执行工具调用...
        }
        
        sink.next(SseEventBuilder.buildThoughtEvent("success", "工具调用完成", "check"));
    }
    
    @Override
    public void onAfterChat(FluxSink<String> sink) {
        sink.next(SseEventBuilder.buildDoneEvent());
    }
}
```

### 示例2: 带统计信息的处理器

```java
public class StatisticsEventHandler implements StreamEventHandler {
    
    private long startTime;
    
    @Override
    public void onBeforeChat(FluxSink<String> sink, String prompt) {
        startTime = System.currentTimeMillis();
    }
    
    @Override
    public void onAfterChat(FluxSink<String> sink) {
        long duration = System.currentTimeMillis() - startTime;
        
        // 发送统计信息
        Map<String, Object> stats = new HashMap<>();
        stats.put("duration", duration);
        stats.put("timestamp", System.currentTimeMillis());
        
        sink.next(SseEventBuilder.buildCustomEvent("statistics", stats));
        sink.next(SseEventBuilder.buildDoneEvent());
    }
}
```

### 示例3: 混合检索策略

```java
public class HybridSearchEventHandler implements StreamEventHandler {
    
    private final String searchStrategy; // "vector" | "keyword" | "hybrid"
    
    @Override
    public void onBeforeChat(FluxSink<String> sink, String prompt) {
        switch (searchStrategy) {
            case "vector":
                sink.next(SseEventBuilder.buildThoughtEvent("processing", "正在执行向量检索...", "search"));
                break;
            case "keyword":
                sink.next(SseEventBuilder.buildThoughtEvent("processing", "正在执行关键词检索...", "search"));
                break;
            case "hybrid":
                sink.next(SseEventBuilder.buildThoughtEvent("processing", "正在执行向量检索...", "search"));
                sink.next(SseEventBuilder.buildThoughtEvent("processing", "正在执行关键词检索...", "search"));
                sink.next(SseEventBuilder.buildThoughtEvent("processing", "正在融合检索结果...", "merge"));
                break;
        }
        sink.next(SseEventBuilder.buildThoughtEvent("success", "检索完成", "check"));
    }
    
    @Override
    public void onAfterChat(FluxSink<String> sink) {
        sink.next(SseEventBuilder.buildDoneEvent());
    }
}
```

## 🔧 Controller 层使用

### 普通对话接口

```java
@PostMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
public Flux<String> chat(@RequestBody String prompt) {
    // 默认使用 NoOpHandler，直接返回文本
    return llmClient.streamChat(prompt);
}
```

### RAG 对话接口

```java
@PostMapping(value = "/rag/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> ragChat(@RequestBody RagChatRequest request) {
    // 1. 执行检索
    List<String> docs = ragService.retrieve(request.getQuery());
    
    // 2. 构建提示词
    String prompt = buildPrompt(request.getQuery(), docs);
    
    // 3. 使用 CustomRagEventHandler
    StreamEventHandler handler = new CustomRagEventHandler(docs);
    
    // 4. 流式生成（返回 SSE 格式）
    return llmClient.streamChat(prompt, handler);
}
```

### 动态选择策略

```java
@PostMapping(value = "/chat/smart", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> smartChat(@RequestBody ChatRequest request) {
    
    StreamEventHandler handler;
    
    if (request.isEnableRag()) {
        // RAG 场景
        List<String> docs = ragService.retrieve(request.getQuery());
        handler = new CustomRagEventHandler(docs);
    } else if (request.isEnableAgent()) {
        // Agent 场景
        handler = new AgentToolEventHandler(request.getTools());
    } else {
        // 普通对话
        handler = new StreamEventHandler.NoOpHandler();
    }
    
    return llmClient.streamChat(request.getPrompt(), handler);
}
```

## 📝 SSE 事件格式说明

### event: thought（思考/检索步骤）

```
event: thought
data: {
  "status": "processing" | "success" | "error",
  "content": "步骤描述",
  "icon": "search" | "edit" | "check" | "tool" | "api" | "sort"
}
```

### event: message（AI 响应增量）

```
event: message
data: {
  "delta": "生成的文本片段"
}
```

### event: documents（检索到的文档）

```
event: ref
data: [
  {
    "title": "国家电网输变电工程标准工艺.pdf",
    "score": 0.95,
    "content": "变电工程电气分册相关内容...",
    "documentId": 1001,
    "chunkId": "chunk_001"
  },
  {
    "title": "设计模式.md",
    "score": 0.87,
    "content": "策略模式的实现...",
    "documentId": 1002,
    "chunkId": "chunk_005"
  }
]
```

**字段说明**：
- `title`: 文档标题/名称
- `score`: 相关性分数（0.0 - 1.0），表示与查询的相关程度
- `content`: 文档块的实际内容
- `documentId`: 文档ID（可选）
- `chunkId`: 文档块ID（可选）
- `metadata`: 其他元数据（可选），如页码、章节等

### event: done（完成标识）

```
event: done
data: [DONE]
```

### event: error（错误信息）

```
event: error
data: {
  "error": "错误描述"
}
```

## 🎯 最佳实践

1. **普通对话**：使用默认的 `streamChat(prompt)`，返回纯文本
2. **RAG 对话**：使用 `streamChat(prompt, handler)`，返回 SSE 格式
3. **前端适配**：根据 `Content-Type` 区分处理
   - `text/plain`：直接显示文本流
   - `text/event-stream`：解析 SSE 事件，展示思维链
4. **性能优化**：避免在 `onBeforeChat` 中执行耗时操作，建议预先检索
5. **错误处理**：在 Handler 中捕获异常，发送友好的错误事件

## 🌟 扩展建议

根据不同的 RAG 策略，可以创建对应的 Handler：

- `QueryExpansionEventHandler`：查询扩展步骤
- `MultiRouteEventHandler`：多路由检索步骤
- `RerankEventHandler`：重排序步骤
- `HypotheticalDocumentEventHandler`：假设文档生成步骤
- `IterativeRetrievalEventHandler`：迭代检索步骤

每个 Handler 都可以独立使用，也可以组合使用（通过组合模式）。
