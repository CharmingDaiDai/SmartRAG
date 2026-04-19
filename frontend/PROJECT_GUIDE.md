# SmartRag 前端项目开发指南

## 1. 项目概述 (Project Overview)
SmartRag 是一个基于 RAG (检索增强生成) 技术的智能知识库系统。前端采用 React + TypeScript + Ant Design 技术栈构建，核心亮点是集成了 Ant Design X 提供的现代化 AI 对话界面。

### 技术栈 (Tech Stack)
- **核心框架**: React 18 + Vite (极速构建与热更新)
- **开发语言**: TypeScript (全量类型安全，所有服务端交互已定义接口类型)
- **UI 组件库**: Ant Design (v6)
- **AI 对话 UI**: Ant Design X (@ant-design/x v2.2.2) - 专为 AI 场景设计的组件库，支持流式渲染和执行过程可视化
- **状态管理**: Zustand - 轻量级、易用的全局状态管理 (负责保存主题、鉴权 Token、选中的知识库)
- **路由管理**: React Router DOM (v6)，包含基于鉴权的 `ProtectedRoute`
- **样式方案**: Tailwind CSS (原子化 CSS) + CSS Modules + AntD Token 系统
- **动画与特效**: Framer Motion (页面过渡、元素进场动画) + tsParticles (Canvas 粒子背景)
- **Markdown 渲染**: @ant-design/x-markdown (支持代码高亮、数学公式 LaTeX、Mermaid 图表)

## 2. 目录结构说明 (Directory Structure)

```
src/
├── components/         # 公共组件目录 (高度复用的模块)
│   ├── common/        # 通用基础组件 (Loading, Error, LiquidGlassCard 等玻璃态组件)
│   ├── rag/           # RAG 业务特定组件 
│   │   └── AnimatedThoughtChain.tsx # AI 思考过程/调用工具思维链动画组件
│   ├── ReferenceViewer.tsx    # 文档引用来源展示组件 (高亮来源文本)
│   ├── RetrievalTreeViewer.tsx # 层级检索树可视化组件 (用于 HiSem 等策略)
│   ├── TokenUsagePanel.tsx    # Token 用量统计面板
│   ├── ChunkDrawer.tsx        # 文档 Chunk 分块抽屉管理组件
│   ├── ThemeBackground.tsx    # 全局 Canvas 粒子特效动态背景
│   └── ThemePopover.tsx       # 全局个性化设置面板 (更换字体、模式、色调等)
├── config/            # 全局配置文件
│   ├── ragConfig.ts   # 管理不同 RAG 策略枚举及其对应前端展现配置
│   └── themeConfig.ts # 管理所有的 UI 主题色、字体大小和个性化类型枚举
├── layouts/           # 页面布局组件
│   └── BasicLayout.tsx # 核心布局：包含侧边栏导航、顶部 Header、主题切换和主内容区
├── pages/             # 页面路由组件 (视图层)
│   ├── auth/          # 认证模块 (登录/注册/处理 GitHub Callback)
│   ├── Chat/          # 核心 RAG 对话界面 (生产环境，核心会话管理)
│   ├── Dashboard/     # 系统状态概览仪表盘
│   ├── KnowledgeBase/ # 知识库管理 (增删改查、配置知识库特定的切片与存储策略)
│   └── documents/     # 知识库下属的具体文档列表与上传模块
├── services/          # API 请求层 (Axios 二次封装)
│   ├── api.ts         # Axios 全局实例配置 (统一拦截器、Token 挂载、错误重定向逻辑)
│   ├── kbService.ts   # 知识库完整生命周期 CRUD 接口
│   ├── documentService.ts # 文档解析、状态查询与管理
│   ├── modelService.ts # 获取服务端支持的大语言或向量模型列表
│   └── conversationService.ts # 对话历史与会话详情管理接口
├── store/             # 全局状态管理 (Zustand)
│   └── useAppStore.ts # 核心 Store：管理鉴权 (userInfo)、当前选中的知识库及本地持久化的偏好设置
├── types/             # TypeScript 全局接口定义
│   └── index.ts       # 全局共享业务对象类型 (User, ChatMessage, ReferenceItem，TokenUsageReport 等)
├── utils/             # 工具函数库
│   ├── formatters.ts  # 日期/文件大小/文本截断格式化
│   └── SmartRagChatProvider.ts # 封装对 SSE 对话流的核心处理与组件状态派发
└── App.tsx            # 应用根组件 (路由中心配置, 权限拦截, ConfigProvider 主题与语言包注入)
```

## 3. 核心功能与实现细节 (Key Features)

### 3.1 RAG 对话界面 (`src/pages/Chat/index.tsx`)
这是系统的核心交互区，实现了类似 ChatGPT 的流式对话与过程透明化体验。
- **架构设计**: 基于响应式的左右分栏/三栏布局。左侧为对话历史线管理，中间为主对话面板，右侧为关联知识库的元数据展示及当前会话调整入口。
- **核心逻辑**: 摒弃了直接调用 API 接口，转为依靠 `SmartRAGChatProvider` （自定义的请求封装）配合 `@ant-design/x-sdk` 中的 `useXChat` Hook 管理消息列表、加载状态和流式输入。
- **渲染引擎**: 使用最新 `XMarkdown` 组件渲染 AI 返回的 Markdown 内容。
- **过程可视化组件**:
  - `AnimatedThoughtChain`: 实时动态展开 AI 的思维链（包含预处理、检索策略执行或重排序逻辑），提供业务透明度。
  - `ReferenceViewer`: 在最终回答下方提供抽屉式或弹出的文档分片引用查看器。
  - `RetrievalTreeViewer`: 以树形节点结构可视化展现复杂策略 (如 HiSem) 的文档检索路径和命中度。
  - `TokenUsagePanel`: 展现详细的提示词消耗与返回 Token。

### 3.2 主题系统与视觉引擎 (`src/store/useAppStore.ts` & `src/config/themeConfig.ts`)
实现了从深/浅色模式、多套高定主题色到字体家族与字号的完全解耦控制。
- **状态管理**: 在 `useAppStore` 中集中维护 `themeMode` ('light' | 'dark')、`colorTheme`、`uiStyle` 和 `fontFamily` 等属性。
- **持久化回落**: 写入 `localStorage` 保存（`SmartRAG_personalization`）。带有配置合法性校验及降级策略，遇到失效配置能自动回退到 `DEFAULT_PERSONALIZATION`。
- **技术实现**:
  - 利用 Ant Design 的 `ConfigProvider` 将主题色派发到全局，通过 `theme.defaultAlgorithm` 动态接管基础样式变体。
  - 特殊元素（如玻璃态透明度、非标字体等）则利用 CSS 变量（CSS Variables）与数据属性 `data-theme` 进行针对性控制覆盖（在 `index.css` 声明）。
  - `ThemePopover.tsx` 是面向终端用户的唯一控制跳板。

### 3.3 数据流机制与错误管控 (`src/services/api.ts`)
系统使用定制化的 Axios 隔离了直接的浏览器 Fetch：
- **请求鉴权**: HttpRequest 时，利用 `拦截器(interceptors.request)` 校验本地是否存在 Token，存在则附带 `Bearer` 头。
- **全局错误处理**: HttpResponse 遇到 401 剥夺访问权限时，系统主动调用 `localStorage.removeItem('token')` 且重定向回登陆页；其他非正常 Http 状态码（如 403, 500）由 Antd 的 `message` 提供气泡级警告。

## 4. 开发扩展指南 (How to Extend)

### 如何添加新页面
1.  **创建组件**: 在 `src/pages/` 下新建文件夹 (例如 `NewFeature`) 并创建 `index.tsx` 并实现基本业务。
2.  **配置路由**: 打开 `src/App.tsx`，在 `<Routes>` 下添加 `<Route>`。如需阻挡游客访问需套在 `<ProtectedRoute>` 内部。
3.  **添加导航**: 打开 `src/layouts/BasicLayout.tsx`，在左侧导航菜单配置 `items` 数组中添加对应的菜单项 (分配 `key`, `icon`, `label`)。

### 如何修改获取数据的请求逻辑
- **处理新端点**: 首先在 `src/types/index.ts` 定义该接口会抛出的 `interface`（请求载荷/回包）。
- **封装新端点**: 然后去 `services` 下找到对应域的服务文件（或新建 XXService.ts），对外 export 一个通过 `import request from './api'` 发起调用的异步函数。
- **业务消费**: React 组件中使用 `useEffect` 对该异步函数调用更新到局部 State 或结合 `AppStore` 对全局数据更新。

## 5. 维护与最佳实践 (Maintenance)

- **类型安全声明**: 严禁在业务对象中使用 `any` 与推导不明的泛型。一切实体（如文档项、模型列、检索回包引用项等）应归纳定型至 `types/index.ts`。
- **减少不必要的重渲染**: 诸如 `SmartRAGChatProvider` 等复杂 Context 传递与状态改变，可能引发表层组件海量刷新。请适当地使用 `useMemo`（保留对象引用）和 `useCallback` 约束。
- **禁止魔术字样覆写**: Ant Design v6 使用 CSS-in-JS 作为样式核心引擎（Token），**严禁**在组件直接附带行内 `style={{ color: '#000' }}` 强制颜色。必须从 `const { token } = theme.useToken();` 中取色（例如 `token.colorText` ），确保深色模式的顺畅游走。
- **关注组件业务粒度**: 若某个页面的文件行数超过 400 行或存在多于两个的跨层级 `Modal`/`Drawer`，这说明子业务过于耦合，应拆分进 `src/components/` 的相关分类目录内作为被依赖组件。

---
*文档最后更新时间: 2026-04-19*
