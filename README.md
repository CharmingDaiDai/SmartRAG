# SmartDoc - 智能文档问答系统

<div align="center">

[![Java](https://img.shields.io/badge/Java-17%2B-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-green)]()
[![React](https://img.shields.io/badge/React-18.2-blue)]()
[![License](https://img.shields.io/badge/License-MIT-yellow)]()
[![Status](https://img.shields.io/badge/Status-Active-success)]()

一个基于 **RAG (检索增强生成)**、**HiSem (层级语义检索)** 和 **SADP (智能自适应双路径规划)** 算法的智能文档问答系统，为知识密集型场景提供高质量的文档理解与智能问答服务。

[快速开始](#-快速开始) · [系统架构](#-系统架构) · [API 文档](#-api-端点概览) · [部署指南](#-部署指南)

</div>

---

## 📋 项目介绍

SmartDoc 是一个**企业级智能文档问答系统**，采用现代化的分层架构设计：

- **前端**：React 18 + TypeScript + Vite，Ant Design v6 + @ant-design/x 对话组件
- **后端**：Spring Boot 3.5.9 + LangChain4j，插件式 RAG 策略框架
- **数据存储**：MySQL（业务数据）+ Milvus（向量索引）+ MinIO（文档存储）
- **核心算法**：Naive RAG、HiSem RAG、HiSem-SADP 三种策略，覆盖精度与性能的不同权衡
- **LLM 支持**：GLM (Zhipu AI)、OpenAI GPT、Google Gemini、Alibaba Qwen、Xinference（私有部署）
- **特色功能**：流式响应、检索过程可视化、思维链展示、Token 用量追踪、多轮对话

## ✨ 核心功能特性

### 文档管理

- 📄 **多格式支持**：PDF、Word、Excel、Markdown、纯文本
- 📤 **上传与管理**：支持拖拽上传，文档状态实时追踪
- 🔄 **异步索引**：后台异步处理，SSE 实时推送索引进度
- ✂️ **Chunk 管理**：细粒度文档分块查看与管理

### 智能检索

- 🔍 **三种 RAG 策略**：
  - **Naive RAG**：基础向量检索，速度快，适合简单场景
  - **HiSem RAG**：层级语义检索，构建文档树结构，检索质量高
  - **HiSem-SADP**：智能自适应双路径规划，综合调度多种检索路径，效果最佳
- 🧠 **查询增强**：查询改写、查询分解、HyDE、意图识别
- 📊 **向量检索**：基于 Milvus 的高效相似度检索
- 🎯 **结果重排**：可选重排模型进一步提升检索质量

### 流式问答

- 💬 **流式对话**：SSE 实现逐字流式响应，打字机效果
- 🔗 **引用溯源**：清晰展示答案证据来源与相似度评分
- 🌳 **检索树可视化**：HiSem 层级检索路径树形展示
- 🧩 **思维链展示**：SADP 规划过程与工具调用实时可视化
- 📈 **Token 用量追踪**：每次对话的 Prompt/Completion/Total Token 统计
- 📝 **多轮对话**：支持上下文记忆和对话历史管理

### 系统特性

- 🔐 **安全认证**：本地账号注册登录 + GitHub OAuth 2.0 + JWT Token 管理
- 🎨 **现代界面**：Ant Design v6 组件库，支持亮色/暗色主题切换与多套主题色
- ⚡ **高性能**：首字延迟 < 800ms，单次问答 < 3s
- 🔧 **易于扩展**：插件式 RAG 策略框架，支持灵活定制

## 🏗️ 系统架构

### 分层架构设计

```
┌──────────────────────────────────────────────────────────┐
│          展示层 (React 18 + TypeScript + Vite)            │
│  ├─ 聊天界面   ├─ 知识库管理  ├─ 文档上传  ├─ 仪表盘     │
└──────────────────────────┬───────────────────────────────┘
                           │ HTTP / SSE
┌──────────────────────────▼───────────────────────────────┐
│           业务服务层 (Spring Boot 3.5.9)                   │
│  ├─ REST API 控制层                                        │
│  ├─ 核心服务层 (RAG、索引、文档、知识库管理)              │
│  ├─ RAG 策略框架 (Naive / HiSem / HiSem-SADP)            │
│  └─ 模型客户端层 (LLM、Embedding、Rerank)                 │
└──────────────────────────┬───────────────────────────────┘
                           │ SQL / TCP
     ┌─────────────┬───────┴──────┬──────────────┐
     │             │              │              │
┌────▼───┐  ┌─────▼──────┐ ┌─────▼───┐  ┌─────▼──────────┐
│ MySQL  │  │   Milvus   │ │  MinIO  │  │  Xinference /  │
│ 业务   │  │ 向量索引   │ │ 文件    │  │  API 模型服务  │
└────────┘  └────────────┘ └─────────┘  └────────────────┘
```

### RAG 策略对比

| 策略 | 索引方式 | 检索方式 | 适用场景 |
|------|---------|---------|---------|
| Naive RAG | 平铺分块向量化 | Top-K 向量检索 | 简单问答，速度优先 |
| HiSem RAG | 层级语义树构建 | 树形自适应检索 | 复杂文档，精度优先 |
| HiSem-SADP | 层级语义树构建 | SADP 双路径规划检索 | 最高质量，综合最优 |

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **前端** | React | 18.2.0 |
| | TypeScript | 5.9.3 |
| | Vite | 7.2.4 |
| | Ant Design | 6.0.0 |
| | @ant-design/x | 2.2.2 |
| | Tailwind CSS | 4.x |
| | Framer Motion | 12.x |
| | Zustand | 5.x |
| **后端** | Java | 17+ |
| | Spring Boot | 3.5.9 |
| | LangChain4j | 1.11.0-beta19 |
| | Maven | 3.8+ |
| **数据库** | MySQL | 8.0+ |
| | Milvus | 2.4.0 |
| | MinIO | latest |
| **LLM** | GLM (Zhipu AI) | glm-4-flash 等 |
| | GPT | OpenAI |
| | Gemini | Google |
| | Qwen | Alibaba |
| | Xinference | 本地私有 |
| **向量模型** | BGE-M3 | 1024 维 (Xinference) |
| | Qwen3-Embedding | 2048 维 (Dashscope) |
| **部署** | Docker | 20.10+ |
| | Docker Compose | 2.0+ |

## 🚀 快速开始

### 环境要求

```
Java 17+
Node.js 20+
MySQL 8.0+
Docker & Docker Compose（推荐）
```

### 方式一：Docker Compose（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/CharmingDaiDai/smartDoc.git
cd smartDoc

# 2. 编辑 docker-compose.yml，填入你的 API Key 等配置
# 找到 backend service 下的 environment 部分，修改：
#   GLM_API_KEY: your_actual_glm_api_key
#   OPENAI_API_KEY: your_actual_openai_api_key（可选）
#   DB_PASSWORD: your_strong_password（生产环境建议修改）
#   JWT_SECRET: your_strong_jwt_secret（生产环境建议修改）

# 3. 一键构建并启动所有服务
docker compose up -d --build

# 4. 查看服务状态
docker compose ps

# 5. 查看后端启动日志
docker compose logs -f backend
```

> 详细的 Docker 部署说明（含生产环境配置、数据备份等）请参阅 [README-docker.md](README-docker.md)。

### 方式二：本地开发

#### 前置依赖

确保以下服务已运行（可使用 Docker 单独启动）：
- MySQL 8.0+
- Milvus 2.4.0
- MinIO

#### 后端启动

```bash
# 编辑 src/main/resources/application.yml 或 application-dev.yml
# 配置数据库、Milvus、MinIO 连接和模型 API Key

# 编译运行
mvn spring-boot:run
```

#### 前端启动

```bash
cd frontend
npm install
npm run dev
# 浏览器访问 http://localhost:3000
```

### 访问应用

| 组件 | 地址 | 说明 |
|------|------|------|
| 前端界面 | http://localhost:3000 | 用户交互界面 |
| 后端 API | http://localhost:8080 | REST API |
| API 文档 (Swagger) | http://localhost:8080/swagger-ui.html | 接口文档 |
| MinIO 控制台 | http://localhost:9001 | 文件存储管理（账号: root / 12345678） |
| Milvus 监控 | http://localhost:9091 | 向量库监控指标 |

## 📂 项目结构

```
smartDoc/
├── src/                              # 后端 Java 源代码
│   └── main/
│       ├── java/com/mtmn/smartdoc/
│       │   ├── controller/           # REST API 控制层
│       │   ├── service/              # 业务服务层
│       │   ├── rag/                  # RAG 策略框架
│       │   │   ├── impl/             # Naive/HiSem/HiSem-Fast 策略实现
│       │   │   ├── sadp/             # SADP 规划器 (SadpPlanner, TaskNode)
│       │   │   └── retriever/        # AdaptiveRetriever, RetrievalTreeNode
│       │   ├── model/client/         # LLM/Embedding 客户端 (OpenAI/Qwen/GLM/Xinference)
│       │   ├── pipeline/             # 检索管道 (QueryExpansion, Reranking)
│       │   ├── po/                   # JPA 实体类
│       │   ├── dto/                  # 请求 DTO
│       │   ├── vo/                   # 响应 VO
│       │   └── config/               # 配置类 (Security, Minio, Async 等)
│       └── resources/
│           ├── application.yml       # 主配置
│           └── application-dev.yml   # 开发环境配置
│
├── frontend/                         # React 前端
│   ├── src/
│   │   ├── pages/                    # 页面组件
│   │   │   ├── Chat/                 # 核心 RAG 对话界面
│   │   │   ├── KnowledgeBase/        # 知识库管理
│   │   │   ├── documents/            # 文档管理
│   │   │   ├── Dashboard/            # 仪表盘
│   │   │   └── auth/                 # 登录/注册
│   │   ├── components/               # 公共组件
│   │   │   ├── rag/                  # RAG 相关组件
│   │   │   │   └── AnimatedThoughtChain.tsx  # 思维链可视化
│   │   │   ├── RetrievalTreeViewer.tsx       # 检索树可视化
│   │   │   ├── TokenUsagePanel.tsx           # Token 用量面板
│   │   │   ├── ChunkDrawer.tsx               # Chunk 管理抽屉
│   │   │   └── ReferenceViewer.tsx           # 引用来源展示
│   │   ├── services/                 # API 请求层
│   │   ├── store/                    # Zustand 状态管理
│   │   ├── types/                    # TypeScript 类型定义
│   │   └── utils/                    # 工具函数
│   ├── Dockerfile.frontend
│   ├── nginx.conf
│   └── package.json
│
├── docs/                             # 文档资源
├── docker-compose.yml                # 完整服务编排（构建 + 运行）
├── docker-compose.runtime.yml        # 生产运行时编排（使用预构建镜像）
├── Dockerfile.backend                # 后端 Docker 镜像
├── README-docker.md                  # Docker 部署详细指南
└── pom.xml                           # Maven 项目配置
```

## 🔌 API 端点概览

### 认证相关

```
POST   /api/auth/register             # 用户注册
POST   /api/auth/login                # 用户登录
GET    /api/auth/callback/github      # GitHub OAuth 回调
POST   /api/auth/refresh              # 刷新 Token
```

### 知识库管理

```
POST   /api/knowledge-bases           # 创建知识库
GET    /api/knowledge-bases           # 获取知识库列表
GET    /api/knowledge-bases/{id}      # 获取知识库详情
PUT    /api/knowledge-bases/{id}      # 更新知识库
DELETE /api/knowledge-bases/{id}      # 删除知识库
```

### 文档管理

```
POST   /api/documents/upload          # 上传文档
POST   /api/documents/batch-upload    # 批量上传
GET    /api/documents                 # 获取文档列表
DELETE /api/documents/{id}            # 删除文档
POST   /api/documents/{id}/index      # 触发索引
GET    /api/documents/progress/{kbId} # 获取索引进度（SSE）
```

### Chunk 管理

```
GET    /api/chunks                    # 获取 Chunk 列表
GET    /api/chunks/{id}               # 获取 Chunk 详情
PUT    /api/chunks/{id}               # 更新 Chunk 内容
DELETE /api/chunks/{id}               # 删除 Chunk
```

### 聊天与查询（均为 SSE 流式接口）

```
POST   /api/chat/rag/naive            # Naive RAG 对话
POST   /api/chat/rag/hisem            # HiSem RAG 对话
POST   /api/chat/rag/hisem-sadp       # HiSem-SADP 对话（效果最佳）
```

### 模型管理

```
GET    /api/models/llms               # 获取可用 LLM 列表
GET    /api/models/embeddings         # 获取嵌入模型列表
GET    /api/models/reranks            # 获取重排模型列表
```

完整 API 文档见：http://localhost:8080/swagger-ui.html

## ⚙️ 关键配置说明

所有配置直接在 `docker-compose.yml` 的 `environment` 部分定义，无需额外的 `.env` 文件。

### 必须配置项

```yaml
# AI 模型（至少配置一个 LLM）
GLM_API_KEY: your_zhipuai_api_key         # 智谱 GLM（推荐）
OPENAI_API_KEY: your_openai_api_key       # OpenAI（可选）
GEMINI_API_KEY: your_gemini_api_key       # Google Gemini（可选）

# 向量嵌入模型（至少配置一个）
EMBEDDING_API_KEY: notnull                # Xinference 本地部署时可填 notnull
EMBEDDING_BASE_URL: http://embedding-service:9997/v1/   # 嵌入服务地址
EMBEDDING_MODEL_NAME: bge-m3              # 嵌入模型名称
```

### 生产环境建议修改

```yaml
DB_PASSWORD: your_strong_password         # 数据库密码
JWT_SECRET: your_strong_random_secret     # JWT 签名密钥（建议 openssl rand -base64 32 生成）
MINIO_ACCESS_KEY: your_minio_key          # MinIO 访问密钥
MINIO_SECRET_KEY: your_minio_secret       # MinIO 密钥
```

### 嵌入模型配置

系统支持多种嵌入方案：

| 方案 | 配置说明 | 适用场景 |
|------|---------|---------|
| Xinference (bge-m3) | 本地部署，高性能，无成本 | 生产/私有化部署 |
| Qwen Embedding (Dashscope) | 在线 API，无需本地 GPU | 快速体验 |

## 🚢 部署指南

### Docker Compose 一键部署

```bash
# 启动所有服务（含构建）
docker compose up -d --build

# 仅重启特定服务
docker compose restart backend

# 查看日志
docker compose logs -f backend
docker compose logs -f frontend

# 停止服务（保留数据）
docker compose down
```

### 生产环境

生产环境使用 `docker-compose.runtime.yml`（使用预构建镜像，不在服务器上编译）：

```bash
docker compose -f docker-compose.runtime.yml up -d
```

> 详细部署说明、数据备份、反向代理配置等，请参阅 **[README-docker.md](README-docker.md)**。

## 📊 系统性能指标

| 指标 | 目标值 | 参考值 |
|------|--------|--------|
| 文档索引速度 | > 1 MB/s | ~1.5 MB/s |
| 向量查询延迟 | < 500ms | 145–350ms |
| 首字响应延迟 | < 2s | 600–800ms |
| 完整答案生成 | < 3s | 1.5–2.5s |
| 系统吞吐量 | > 30 req/s | ~31 req/s |

## ❓ 常见问题

**Q: 如何选择 RAG 策略？**

- **Naive RAG**：简单问答，追求速度，文档结构简单时使用
- **HiSem RAG**：文档层级结构清晰（如技术手册、报告），追求检索精度
- **HiSem-SADP**：复杂多跳问题，需要综合多段落的答案，效果最佳

> 注意：HiSem/HiSem-SADP 仅支持 Markdown 格式文档索引。

**Q: 支持哪些 LLM？**

GLM（Zhipu AI）、OpenAI GPT、Google Gemini、Alibaba Qwen、本地 Xinference 部署的开源模型。

**Q: 如何私有化部署（无外网）？**

使用 Xinference 在本地运行开源 LLM 和 Embedding 模型，所有请求均在内网处理。

**Q: 如何提高检索质量？**

1. 使用 HiSem-SADP 策略
2. 启用查询增强（改写、分解、HyDE）
3. 启用结果重排（需要配置重排模型）
4. 优化文档分块参数（chunkSize、titleEnhance 等）

**Q: 支持多轮对话吗？**

是的，系统支持多轮对话，前端维护对话历史并在每次请求时携带上下文。

## 🤝 贡献指南

欢迎提交 Pull Request：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启 Pull Request

提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范。

## 📄 许可证

本项目采用 [MIT License](LICENSE) - 详见 LICENSE 文件。

## 🙏 致谢

- [LangChain4j](https://github.com/langchain4j/langchain4j) - Java RAG 框架
- [Milvus](https://milvus.io/) - 向量数据库
- [Apache Tika](https://tika.apache.org/) - 文档解析
- [Ant Design](https://ant.design/) / [@ant-design/x](https://x.ant.design/) - UI 组件库

## 📧 联系方式

- Issues: [GitHub Issues](https://github.com/CharmingDaiDai/smartDoc/issues)
- Discussions: [GitHub Discussions](https://github.com/CharmingDaiDai/smartDoc/discussions)

---

<div align="center">

**⭐ 如果本项目对你有帮助，请给一个 Star！**

</div>
