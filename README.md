# 🤖 SmartDoc - 电力知识智能问答系统

<div align="center">

[![Java](https://img.shields.io/badge/Java-17%2B-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-green)]()
[![React](https://img.shields.io/badge/React-18.2-blue)]()
[![License](https://img.shields.io/badge/License-MIT-yellow)]()
[![Status](https://img.shields.io/badge/Status-Active-success)]()

一个基于 **RAG (检索增强生成)** 和 **HiSem (分层语义检索)** 算法的智能文档问答系统，为电力系统运检人员和安监人员提供智能知识查询服务。

[快速开始](#-快速开始) • [系统架构](#-系统架构) • [API 文档](#-api-端点) • [论文资源](#-论文资源) • [部署指南](#-部署指南)

</div>

---

## 📋 项目介绍

SmartDoc 是一个面向电力行业的**企业级知识智能问答系统**，采用现代化的分层架构设计：

- **前端**：React 18 + TypeScript + Vite，美观的 Ant Design 界面
- **后端**：Spring Boot 3.5.9，支持插件式 RAG 策略框架
- **数据存储**：MySQL（业务数据）+ Milvus（向量索引）+ MinIO（文档存储）
- **核心算法**：Naive RAG、HiSem RAG、HiSem-Fast RAG 三种策略
- **LLM 支持**：GLM、OpenAI GPT、Google Gemini、Alibaba Qwen、Xinference（私有模型）
- **特色功能**：过程可视化、实时进度推送、多模型灵活切换、多轮对话支持

## ✨ 核心功能特性

### 文档管理

- 📄 **多格式支持**：PDF、Word、Excel、Markdown、纯文本
- 📤 **单/批量上传**：支持拖拽上传和批量导入
- 🏷️ **元数据管理**：自动提取和管理文档信息
- 🔄 **异步索引**：后台异步处理，实时推送进度（SSE）

### 智能检索

- 🔍 **多 RAG 策略**：Naive RAG、HiSem RAG、HiSem-Fast RAG 灵活切换
- 🧠 **查询增强**：查询改写、查询分解、HyDE、意图识别
- 📊 **向量检索**：基于 Milvus 的高效相似度检索，支持 100 万+ 向量
- 🎯 **结果重排**：可选的重排模型进一步提升检索质量

### 流式问答

- 💬 **流式对话**：采用 SSE 实现流式响应，逐字推送生成过程
- 🔗 **引用溯源**：清晰展示答案的证据来源和相似度评分
- 🧩 **思维可视化**：展示 RAG 管道各环节的执行过程，提升系统可解释性
- 📝 **多轮对话**：支持上下文记忆和对话历史管理

### 系统特性

- 🔐 **安全认证**：GitHub OAuth 2.0 + JWT Token 管理
- 🎨 **现代界面**：Ant Design 组件库，支持亮色/暗色主题切换
- ⚡ **高性能**：首字延迟 < 800ms，单次问答 < 3s，QPS > 30
- 🔧 **易于扩展**：插件式 RAG 策略框架，支持灵活定制

## 🏗️ 系统架构

### 分层架构设计

```
┌──────────────────────────────────────────────────────────┐
│            展示层 (React 18 + TypeScript + Vite)          │
│  ├─ 聊天界面 ├─ 知识库管理 ├─ 文档上传 ├─ 仪表盘        │
└──────────────────────────┬───────────────────────────────┘
                           │ HTTP/SSE
┌──────────────────────────▼───────────────────────────────┐
│        业务服务层 (Spring Boot 3.5.9)                     │
│  ├─ REST API 控制层                                       │
│  ├─ 核心服务层 (RAG、索引、文档、知识库管理)             │
│  ├─ RAG 策略框架 (Naive/HiSem/HiSem-Fast)               │
│  └─ 模型客户端层 (LLM、Embedding、Rerank)               │
└──────────────────────────┬───────────────────────────────┘
                           │ SQL/TCP
     ┌─────────────┬───────┴──────┬──────────────┐
     │             │              │              │
┌────▼───┐  ┌─────▼──────┐ ┌─────▼───┐  ┌─────▼───┐
│ MySQL  │  │   Milvus   │ │  MinIO  │  │ Redis   │
│ 业务   │  │ 向量索引   │ │ 文件    │  │ 缓存    │
└────────┘  └────────────┘ └─────────┘  └─────────┘
```

### 数据库设计

- **MySQL**：8 张数据表（users、knowledge_bases、documents、chunks、tree_nodes 等）
- **Milvus**：按知识库分集合存储向量，支持 HNSW 索引
- **MinIO**：S3 兼容的文件存储，支持高可用

## 🛠️ 技术栈

| 层级               | 技术           | 版本         |
| ------------------ | -------------- | ------------ |
| **前端**     | React          | 18.2.0       |
|                    | TypeScript     | 5.9.3        |
|                    | Vite           | 7.2.4        |
|                    | Ant Design     | 6.0.0        |
|                    | Tailwind CSS   | 4.1.17       |
| **后端**     | Java           | 17+          |
|                    | Spring Boot    | 3.5.9        |
|                    | LangChain4j    | 1.9.1-beta17 |
|                    | Maven          | 3.8+         |
| **数据库**   | MySQL          | 8.0+         |
|                    | Milvus         | 2.4.0        |
|                    | MinIO          | latest       |
| **LLM**      | GLM 4          | Zhipu AI     |
|                    | GPT-4          | OpenAI       |
|                    | Gemini         | Google       |
|                    | Qwen           | Alibaba      |
|                    | Xinference     | Local        |
| **向量模型** | BGE-M3         | 1024 维      |
|                    | Qwen Embedding | 1536 维      |
| **部署**     | Docker         | 20.10+       |
|                    | Docker Compose | 1.29+        |

## 🚀 快速开始

### 环境要求

```bash
Java 17+
Node.js 16+
MySQL 8.0+
Docker & Docker Compose (推荐)
```

### 方式一：Docker Compose（推荐）

```bash
# 1. 克隆项目
git clone https://github.com/CharmingDaiDai/smartDoc.git
cd smartDoc

# 2. 复制环境文件
cp .env.example .env

# 3. 修改 .env 配置（填入你的 API Key 等）
# 编辑 .env 文件，配置：
# - GitHub OAuth (GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET)
# - LLM API Key (GLM_API_KEY, OPENAI_API_KEY 等)
# - 数据库密码

# 4. 一键启动所有服务
docker-compose up -d

# 5. 查看服务状态
docker-compose ps
```

### 方式二：本地开发

#### 后端启动

```bash
# 1. 确保 MySQL、Milvus、MinIO 正常运行
# 可使用 docker-compose -f docker-compose.deps.yml up

# 2. 编辑配置文件
# application.yml 中配置数据库、Milvus、MinIO 连接

# 3. 编译运行
mvn clean package
java -jar target/smartdoc-*.jar
```

#### 前端启动

```bash
cd frontend
npm install
npm run dev
```

### 访问应用

| 应用         | 地址                                  | 账号                  |
| ------------ | ------------------------------------- | --------------------- |
| 前端界面     | http://localhost:3000                 | GitHub OAuth 登录     |
| 后端 API     | http://localhost:8080                 | 无需登录              |
| API 文档     | http://localhost:8080/swagger-ui.html | -                     |
| Milvus 管理  | http://localhost:19530                | -                     |
| MinIO 控制台 | http://localhost:9001                 | minioadmin/minioadmin |

## 📂 项目结构

```
smartDoc/
├── backend/                          # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/smartdoc/
│   │       ├── controller/          # REST API 控制层
│   │       ├── service/             # 业务服务层
│   │       ├── strategy/            # RAG 策略框架
│   │       ├── model/               # 数据模型和客户端
│   │       ├── repository/          # 数据访问层
│   │       └── config/              # 配置管理
│   ├── pom.xml                      # Maven 配置
│   └── application.yml              # 应用配置
│
├── frontend/                         # React 前端
│   ├── src/
│   │   ├── pages/                   # 页面组件
│   │   │   ├── Chat/               # 聊天页面
│   │   │   ├── KnowledgeBase/      # 知识库管理
│   │   │   ├── Documents/          # 文档管理
│   │   │   └── Dashboard/          # 仪表盘
│   │   ├── components/              # 可复用组件
│   │   ├── services/                # API 服务
│   │   ├── config/                  # 配置文件
│   │   └── styles/                  # 全局样式
│   ├── package.json
│   └── vite.config.ts
│
├── docs/                             # 文档和论文资源
│   ├── Chapter_4_Thesis.md          # 硕士学位论文第四章
│   ├── *.html                       # 可视化流程图
│   └── README.md                    # 论文图表说明
│
├── docker-compose.yml               # 完整的生产配置
├── docker-compose.deps.yml          # 仅启动依赖服务
├── .env.example                     # 环境变量模板
└── README.md                        # 本文件
```

## 🔌 API 端点概览

### 认证相关

```
POST   /api/auth/login              # 用户登录
POST   /api/auth/register           # 用户注册
GET    /api/auth/callback/github    # GitHub OAuth 回调
POST   /api/auth/refresh            # 刷新 Token
```

### 知识库管理

```
POST   /api/knowledge-bases         # 创建知识库
GET    /api/knowledge-bases         # 获取知识库列表
GET    /api/knowledge-bases/{id}    # 获取知识库详情
PUT    /api/knowledge-bases/{id}    # 更新知识库
DELETE /api/knowledge-bases/{id}    # 删除知识库
```

### 文档管理

```
POST   /api/documents/upload        # 上传文档
POST   /api/documents/batch-upload  # 批量上传
GET    /api/documents               # 获取文档列表
GET    /api/documents/{id}          # 获取文档详情
DELETE /api/documents/{id}          # 删除文档
POST   /api/documents/{id}/index    # 触发索引
GET    /api/documents/progress/{kbId} # 获取索引进度 (SSE)
```

### 聊天和查询

```
POST   /api/chat/rag/naive          # Naive RAG 对话 (SSE)
POST   /api/chat/rag/hisem          # HiSem RAG 对话 (SSE)
POST   /api/chat/rag/hisem-fast     # HiSem-Fast RAG 对话 (SSE)
```

### 模型管理

```
GET    /api/models/llms             # 获取 LLM 列表
GET    /api/models/embeddings       # 获取嵌入模型列表
GET    /api/models/reranks          # 获取重排模型列表
```

详见 [API 完整文档](http://localhost:8080/swagger-ui.html)

## ⚙️ 环境配置说明

### .env 关键配置项

```bash
# ============ 数据库配置 ============
DB_HOST=localhost
DB_PORT=3306
DB_NAME=smartdoc
DB_USERNAME=root
DB_PASSWORD=your_password

# ============ Milvus 配置 ============
MILVUS_HOST=localhost
MILVUS_PORT=19530

# ============ MinIO 配置 ============
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# ============ JWT 配置 ============
JWT_SECRET=your_secret_key_here
JWT_EXPIRATION_TIME=86400000  # 24 小时

# ============ GitHub OAuth 配置 ============
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
GITHUB_REDIRECT_URI=http://localhost:3000/auth/callback/github

# ============ LLM 配置 ============
# Zhipu AI (GLM)
GLM_API_KEY=your_glm_api_key
GLM_API_BASE=https://open.bigmodel.cn/api/paas/v4

# OpenAI (GPT-4)
OPENAI_API_KEY=your_openai_api_key
OPENAI_API_BASE=https://api.openai.com/v1

# Google Gemini
GEMINI_API_KEY=your_gemini_api_key

# Alibaba Qwen
QWEN_API_KEY=your_qwen_api_key
QWEN_API_BASE=https://dashscope.aliyuncs.com/api/v1

# ============ 向量模型配置 ============
EMBEDDING_API_KEY=your_embedding_key
EMBEDDING_BASE_URL=http://localhost:8000  # Xinference 本地部署
EMBEDDING_MODEL_NAME=bge-m3
```

## 📊 系统性能指标

| 指标         | 目标值     | 实际值     |
| ------------ | ---------- | ---------- |
| 文档索引速度 | > 1 MB/s   | 1.5 MB/s   |
| 向量查询延迟 | < 500ms    | 145-350ms  |
| 首字响应延迟 | < 2s       | 600-800ms  |
| 完整答案生成 | < 3s       | 1.5-2.5s   |
| 系统吞吐量   | > 30 req/s | 31.2 req/s |
| 并发用户支持 | > 100      | 100+       |
| 系统可用性   | > 99%      | 99.5%+     |

## 🚢 部署指南

### Docker 一键部署

```bash
# 完整的生产部署
docker-compose -f docker-compose.yml up -d

# 仅启动依赖服务（用于本地开发）
docker-compose -f docker-compose.deps.yml up -d

# 查看所有容器
docker-compose ps

# 查看日志
docker-compose logs -f backend
docker-compose logs -f frontend
```

### Kubernetes 部署

```bash
# 创建命名空间
kubectl create namespace smartdoc

# 部署应用
kubectl apply -f k8s/deployment.yaml -n smartdoc

# 检查部署状态
kubectl get pods -n smartdoc
kubectl logs -f deployment/smartdoc-backend -n smartdoc
```

### 生产环境建议

- ✅ 使用强密码和 SSL/TLS 加密
- ✅ 配置数据库备份和恢复方案
- ✅ 使用 Redis 缓存提升性能
- ✅ 配置日志聚合和监控告警
- ✅ 定期轮换 API 密钥
- ✅ 启用 WAF 和 DDoS 防护

## 🤝 贡献指南

欢迎提交 Pull Request！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启 Pull Request

### 开发规范

- 代码风格遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)
- 所有代码需要通过单元测试和集成测试
- 新功能需要更新相应文档

## 📞 常见问题

**Q: 如何切换不同的 RAG 策略？**

A: 在知识库创建或编辑时选择：

- Naive RAG：基础向量检索，速度快
- HiSem RAG：层级语义检索，质量高
- HiSem-Fast RAG：平衡速度和质量

**Q: 支持哪些 LLM？**

A: 当前支持 GLM、OpenAI GPT-4、Google Gemini、Alibaba Qwen、本地 Xinference

**Q: 如何私有化部署（不上网）？**

A: 使用 Xinference 运行开源模型，所有请求都在本地处理

**Q: 如何提高检索质量？**

A: 可尝试：

1. 启用查询增强（改写、分解、HyDE）
2. 使用 HiSem RAG 策略
3. 启用结果重排
4. 优化分块参数

**Q: 支持多轮对话吗？**

A: 支持，对话历史已在数据库设计中，前端支持正在开发中

## 📄 许可证

本项目采用 [MIT License](LICENSE) - 详见 LICENSE 文件

## 🙏 致谢

感谢以下开源项目的支持：

- LangChain4j - RAG 框架
- Milvus - 向量数据库
- Apache Tika - 文档解析
- Ant Design - UI 组件库

## 📧 联系方式

- 📮 Issues: [GitHub Issues](https://github.com/CharmingDaiDai/smartDoc/issues)
- 💬 讨论: [GitHub Discussions](https://github.com/CharmingDaiDai/smartDoc/discussions)

---

<div align="center">

**⭐ 如果本项目对你有帮助，请给一个 Star！**

Made with ❤️ for Power System Intelligence

</div>
