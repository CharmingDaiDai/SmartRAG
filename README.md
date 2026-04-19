# SmartDoc 3.0

<div align="center">

[![Java](https://img.shields.io/badge/Java-17%2B-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-green)]()
[![React](https://img.shields.io/badge/React-18.2-blue)]()
[![Frontend Version](https://img.shields.io/badge/Frontend-3.0-1677ff)]()
[![Backend Version](https://img.shields.io/badge/Backend-3.0-52c41a)]()
[![License](https://img.shields.io/badge/License-MIT-yellow)]()

基于 RAG、HiSem 与 SADP 的智能文档问答系统。

</div>

## 项目简介

SmartDoc 提供从文档上传、索引构建、检索问答到会话追踪的完整闭环能力：

- 前端：React + TypeScript + Vite + Ant Design
- 后端：Spring Boot + LangChain4j
- 存储：MySQL + Milvus + MinIO
- 检索策略：Naive RAG、HiSem Fast、HiSem-SADP（完整层级语义 + 规划执行）
- 交互：SSE 流式输出、参考文献展示、检索树可视化、Token 用量事件

## 系统架构图

```mermaid
flowchart TB
  U[用户浏览器] --> FE[前端 React Vite]
  FE -->|HTTP API| BE[后端 Spring Boot]
  FE -->|SSE| BE

  subgraph Core[后端核心]
    API[Controller API 层]
    SVC[Service 业务层]
    RAG[RAG 策略层<br/>Naive / HiSem Fast / HiSem-SADP]
    MODEL[模型客户端层<br/>LLM / Embedding / Rerank]
    API --> SVC --> RAG --> MODEL
  end

  BE --> MYSQL[(MySQL)]
  BE --> MILVUS[(Milvus)]
  BE --> MINIO[(MinIO)]
  MODEL --> LLM[(OpenAI/GLM/Gemini/Qwen/Xinference)]
```

## 检索策略与接口映射

| 逻辑能力 | 索引策略类型 | 对话接口 | 说明 |
|---|---|---|---|
| Naive RAG | NAIVE_RAG | POST /api/chat/rag/naive | 平铺 chunk 检索 |
| HiSem Fast | HISEM_RAG_FAST | POST /api/chat/rag/hisem-fast | Markdown 分层切块 + 标题增强 |
| HiSem-SADP 完整版 | HISEM_RAG | POST /api/chat/rag/hisem | 同一接口内根据意图路由：简单事实走自适应层级检索，复杂问题走 SADP DAG |

说明：当前代码里没有单独的 /api/chat/rag/hisem-sadp 路由；HiSem-SADP 能力由 /api/chat/rag/hisem 在复杂意图分支触发。

## 快速开始

### 1) Docker Compose

```bash
git clone https://github.com/CharmingDaiDai/SmartRAG.git
cd smartDoc

cp .env.example .env
# 按需填写 .env（数据库、JWT、模型 Key 等）

docker compose up -d --build
docker compose ps
```

默认访问地址：

- 前端：http://localhost:3000
- 后端：http://localhost:8080
- Swagger：http://localhost:8080/swagger-ui.html
- MinIO Console：http://localhost:9001
- Attu：http://localhost:8000

### 2) 本地开发

后端（默认激活 dev 配置，端口 18080）：

```bash
mvn spring-boot:run
```

前端（开发端口 3000，Vite 代理 /api 到 http://localhost:18080）：

```bash
cd frontend
npm install
npm run dev
```

## API 概览（按当前 Controller 实现）

### 认证

- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/login/github
- GET /api/auth/callback/github
- POST /api/auth/exchange-token
- POST /api/auth/refresh-token

### 知识库

- POST /api/knowledge-bases
- GET /api/knowledge-bases
- GET /api/knowledge-bases/{kbId}
- DELETE /api/knowledge-bases/{kbId}

### 文档

- POST /api/documents/upload
- POST /api/documents/batch-upload
- GET /api/documents
- GET /api/documents/{kbId}
- GET /api/documents/detail/{documentId}
- GET /api/documents/{documentId}/preview/meta
- GET /api/documents/{documentId}/preview/text
- GET /api/documents/{documentId}/preview/raw
- DELETE /api/documents/{documentId}
- DELETE /api/documents/batch
- GET /api/documents/index-progress
- POST /api/documents/{documentId}/index
- POST /api/documents/batch-index
- POST /api/documents/{documentId}/rebuild-index
- POST /api/documents/batch-rebuild-index

### Chunk / TreeNode 管理

- GET /api/chunks
- PUT /api/chunks/{id}
- GET /api/tree-nodes/tree
- PUT /api/tree-nodes/{id}

### 对话与模型

- POST /api/chat/rag/naive (SSE)
- POST /api/chat/rag/hisem-fast (SSE)
- POST /api/chat/rag/hisem (SSE)
- GET /api/models/llms
- GET /api/models/embeddings
- GET /api/models/reranks

### 会话、用户、仪表盘

- GET /api/conversations/sessions
- GET /api/conversations/sessions/{sessionId}
- DELETE /api/conversations/sessions/{sessionId}
- GET /api/profile
- PUT /api/profile
- POST /api/profile/avatar
- PUT /api/profile/password
- GET /api/dashboard/statistics

## HiSem-SADP 索引构建完整时序图

```mermaid
sequenceDiagram
  autonumber
  participant U as User
  participant FE as Frontend
  participant DC as DocumentController
  participant DS as DocumentService
  participant TASK as IndexingTaskService
  participant IS as IndexingService
  participant F as RAGStrategyFactory
  participant HS as HisemRAGIndexStrategy
  participant MIN as MinIO
  participant MP as MarkdownProcessor
  participant LLM as LLMClient
  participant TR as TreeNodeRepository
  participant EMB as EmbeddingClient
  participant MIL as Milvus

  U->>FE: 上传 Markdown 文档
  FE->>DC: POST /api/documents/upload
  DC->>DS: uploadDocument(kbId, file)
  DS->>MIN: uploadFile
  DS-->>DC: 文档入库(UPLOADED)
  DC-->>FE: 上传成功

  U->>FE: 触发索引
  FE->>DC: POST /api/documents/{id}/index
  DC->>DS: triggerIndexing(documentId)
  DS->>TASK: create indexing task
  TASK->>IS: submitIndexingTask(documentId, kbId)
  IS->>F: getIndexStrategy(HISEM_RAG)
  F-->>IS: HisemRAGIndexStrategy
  IS->>HS: buildIndex(..., callback)

  HS->>MIN: getFileStream(filePath)
  HS->>MP: parseMarkdownContent
  HS->>MP: buildTitlePaths

  opt 启用语义压缩
    loop 由深到浅遍历树节点
      HS->>LLM: 叶子提取/父节点聚合
      LLM-->>HS: keyKnowledge + summary
    end
  end

  HS->>TR: deleteByDocumentId
  HS->>TR: saveAll(treeNodes)

  loop 每个节点向量化
    HS->>EMB: embed(embedText)
    EMB-->>HS: embedding
  end

  HS->>MIL: store(vectorItems)
  HS->>TR: saveAll(with vectorIds)
  HS-->>IS: 索引完成
  IS-->>TASK: 更新任务状态
  IS-->>DS: 文档状态 INDEXED
  DS-->>FE: 索引完成
```

## HiSem-SADP 检索完整时序图

```mermaid
sequenceDiagram
  autonumber
  participant U as User
  participant FE as Frontend
  participant CC as ChatController
  participant RS as RAGServiceImpl
  participant QP as RAGQueryProcessor
  participant SP as SadpPlanner
  participant AR as AdaptiveRetriever
  participant EMB as EmbeddingClient
  participant MIL as Milvus
  participant LLM as LLMClient
  participant CONV as ConversationService

  U->>FE: 提问
  FE->>CC: POST /api/chat/rag/hisem (SSE)
  CC->>RS: hisemRagChat(userId, request)
  RS->>RS: 校验知识库权限 + 文档均为 .md

  opt 开启意图识别
    RS->>QP: analyzeIntent(query, history)
    QP-->>RS: 是否需要检索
  end

  RS->>SP: routeIntent(query)
  SP-->>RS: intent

  alt intent == 简单事实
    RS->>AR: retrieve(queries, kbId)
    loop 每个 query
      AR->>EMB: embed(query)
      EMB-->>AR: queryVector
      loop 根层回退扫描(level=1..10)
        AR->>MIL: search(filter=level)
        MIL-->>AR: candidates
      end
      loop 递归子层检索
        AR->>MIL: search(filter=parent_node_id)
        MIL-->>AR: child candidates
      end
    end
    AR-->>RS: RetrievalBundle(results, treeRoots)
    RS-->>FE: retrievalTree + references SSE 事件
    RS->>LLM: streamChatWithEmitter(RAG_ANSWER)
    LLM-->>FE: token/text 流
  else 多跳推理/对比分析/宏观总结
    RS->>SP: buildSkeleton(kbId)
    SP-->>RS: skeleton
    RS->>SP: planDag(query, skeleton)
    SP-->>RS: tasks
    RS->>SP: executeDag(tasks,...)
    loop 按依赖执行任务
      alt Scoped_Retrieve
        SP->>AR: retrieveFromScope(nodeId, query)
        AR->>EMB: embed(query)
        AR->>MIL: scoped search
        MIL-->>AR: scoped results
        AR-->>SP: retrieval bundle
      else Get_Summary
        SP->>SP: read summary from tree nodes
      else Generate
        SP->>LLM: chat(generate prompt)
        LLM-->>SP: task result
      end
    end
    SP-->>RS: final generate prompt
    RS->>LLM: streamChatWithEmitter(final prompt)
    LLM-->>FE: token/text 流
  end

  RS->>CONV: saveConversation(query, response, retrievedChunks)
  RS-->>FE: SSE complete
```

## 项目结构

```text
smartDoc/
├── src/main/java/com/mtmn/smartdoc/
│   ├── controller/
│   ├── service/
│   ├── rag/
│   │   ├── impl/
│   │   ├── retriever/
│   │   └── sadp/
│   └── model/
├── src/main/resources/
├── frontend/
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── docker-compose.yml
├── Dockerfile.backend
├── README-docker.md
└── pom.xml
```

## 说明

- Docker 详细部署、备份与运维建议见 README-docker.md。
- HiSem-SADP 对话入口为 /api/chat/rag/hisem；当问题被路由为复杂意图时，自动执行 SADP DAG 规划与检索。
- 注意：HiSem-SADP（完整版）链路要求知识库文档为 Markdown（.md）格式。

## 许可证

本项目采用 MIT License。
