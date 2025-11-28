# SmartRag 前端项目开发指南

## 1. 项目概述 (Project Overview)
SmartRag 是一个基于 RAG (检索增强生成) 技术的智能知识库系统。前端采用 React + TypeScript + Ant Design 技术栈构建，核心亮点是集成了 Ant Design X 提供的现代化 AI 对话界面。

### 技术栈 (Tech Stack)
- **核心框架**: React 18 + Vite (极速构建与热更新)
- **开发语言**: TypeScript (全量类型安全)
- **UI 组件库**: Ant Design (v5)
- **AI 对话 UI**: Ant Design X (@ant-design/x) - 专为 AI 场景设计的组件库
- **状态管理**: Zustand - 轻量级、易用的全局状态管理
- **路由管理**: React Router DOM (v6)
- **样式方案**: Tailwind CSS (原子化 CSS) + CSS Modules + AntD Token 系统
- **Markdown 渲染**: @ant-design/x-markdown (支持代码高亮、数学公式 LaTeX、Mermaid 图表)

## 2. 目录结构说明 (Directory Structure)

```
src/
├── components/         # 公共组件目录
│   ├── common/        # 通用基础组件 (Loading, Error, 按钮封装等)
│   ├── rag/           # RAG 业务特定组件 (思维链展示, 引用卡片等)
│   └── ReferenceViewer.tsx # 文档引用来源展示组件
├── config/            # 全局配置文件 (API 地址, 常量定义)
├── layouts/           # 页面布局组件
│   └── BasicLayout.tsx # 核心布局：包含侧边栏导航、顶部 Header、主题切换
├── pages/             # 页面路由组件 (视图层)
│   ├── auth/          # 认证模块 (登录/注册)
│   ├── Chat/          # 核心 RAG 对话界面 (生产环境)
│   ├── TestChat/      # 对话功能测试沙箱 (开发调试用)
│   ├── Dashboard/     # 系统概览仪表盘
│   ├── KnowledgeBase/ # 知识库管理 (上传、切片配置)
│   └── documents/     # 文档列表与管理
├── services/          # API 请求层 (Axios 封装)
│   ├── api.ts         # Axios 实例配置 (拦截器、Token 处理)
│   ├── kbService.ts   # 知识库相关接口
│   └── chatService.ts # 对话相关接口
├── store/             # 全局状态管理 (Zustand)
│   └── useAppStore.ts # 核心 Store：管理用户信息、主题模式、当前选中的知识库
├── types/             # TypeScript 类型定义
│   └── index.ts       # 全局共享类型 (User, ChatMessage, ReferenceItem 等)
├── utils/             # 工具函数库 (日期格式化, 数据转换等)
└── App.tsx            # 应用根组件 (路由配置, ConfigProvider 主题注入)
```

## 3. 核心功能与实现细节 (Key Features)

### 3.1 RAG 对话界面 (`src/pages/Chat`)
这是系统的核心功能区，实现了类似 ChatGPT 的流式对话体验。
- **核心逻辑**: 使用 `@ant-design/x-sdk` 中的 `useXChat` Hook 管理消息列表、加载状态和流式输入。
- **渲染引擎**: 使用 `XMarkdown` 组件渲染 AI 返回的 Markdown 内容。
- **插件集成**:
  - `HighlightCode`: 代码块语法高亮 (支持多种编程语言)。
  - `Latex`: 数学公式渲染 (基于 KaTeX)。
  - `Mermaid`: 流程图、时序图渲染。
- **流式响应**: 支持打字机效果 (Typewriter)，提升用户感知的响应速度。
- **自定义组件**:
  - `AnimatedThoughtChain`: 展示 AI 的 "思考过程" (CoT)，支持展开/折叠。
  - `ReferenceViewer`: 在回答下方展示引用的知识库文档片段及相关度分数。

### 3.2 主题系统 (深色/浅色模式)
实现了系统级的深色模式切换，并持久化存储。
- **状态管理**: 在 `useAppStore.ts` 中维护 `themeMode` ('light' | 'dark')。
- **持久化**: 状态变化时自动同步到 `localStorage`，刷新页面不丢失。
- **实现原理**:
  1.  **组件级**: `App.tsx` 使用 Ant Design 的 `ConfigProvider`，根据模式动态切换 `theme.defaultAlgorithm` (浅色) 和 `theme.darkAlgorithm` (深色)。
  2.  **全局级**: `App.tsx` 会在 `<html>` 标签上动态添加 `data-theme='dark'` 属性。
  3.  **样式覆盖**: `index.css` 使用 CSS 变量定义全局颜色 (如滚动条、背景色)，并针对 `[data-theme='dark']` 编写特定覆盖样式。
  4.  **切换入口**: `BasicLayout.tsx` 顶部导航栏包含切换按钮。

### 3.3 Markdown 高级渲染
为了解决深色模式下代码块看不清的问题，我们做了特殊处理。
- **配置位置**: `Chat/index.tsx` 和 `TestChat/index.tsx`。
- **样式定制**:
  - 引入了 `@ant-design/x-markdown` 的官方主题样式。
  - 在 `index.css` 中强制覆盖了代码块的背景色和字体颜色，使其在深色模式下对比度更高 (类似 GitHub Dark 主题)。
  - **自定义 Code 组件**: 通过 `components` 属性将 Markdown 中的 `code` 标签映射为自定义组件，确保语法高亮插件能正确工作。

## 4. 开发扩展指南 (How to Extend)

### 如何添加新页面
1.  **创建组件**: 在 `src/pages/` 下新建文件夹 (例如 `NewFeature`) 并创建 `index.tsx`。
2.  **配置路由**: 打开 `src/App.tsx`，在 `AppRoutes` 中添加新的 `<Route>`。确保它被包裹在 `<ProtectedRoute>` 中以进行权限控制。
3.  **添加菜单**: 打开 `src/layouts/BasicLayout.tsx`，在 `items` 数组中添加新的菜单项 (包含 `key`, `icon`, `label`)。

### 如何修改对话逻辑
- **处理消息发送**: 修改 `Chat/index.tsx` 中的 `handleRequest` 函数。这里负责调用后端 API。
- **调整 UI 样式**: 修改 `Bubble.List` 的 `items` 渲染逻辑，可以自定义用户和 AI 气泡的样式、头像等。
- **添加 Markdown 插件**:
  1.  从 `@ant-design/x-markdown` 导入新插件。
  2.  在 `MD_PLUGINS` 数组中注册。
  3.  如果插件需要自定义渲染组件 (如自定义图表)，在 `MD_COMPONENTS` 中进行映射。

### API 接口对接
1.  **定义类型**: 首先在 `src/types/index.ts` 中定义接口请求和响应的 TypeScript 类型。
2.  **封装服务**: 在 `src/services/` 下创建或修改对应的 Service 文件 (如 `userService.ts`)，使用 `api.get` 或 `api.post` 发起请求。
3.  **组件调用**: 在组件中使用 `useEffect` 获取数据，或在事件处理函数中调用。建议使用 `try-catch` 处理异常，并配合 `message.error` 提示用户。

## 5. 维护与最佳实践 (Maintenance)

- **类型安全**: 严禁使用 `any` 类型。所有数据结构都应在 `src/types` 中定义接口。
- **性能优化**: 对于复杂的列表渲染 (如聊天记录)，使用 `useMemo` 和 `useCallback` 避免不必要的重渲染。
- **深色模式适配**: 开发新组件时，不要写死颜色值 (如 `#fff`, `#000`)。请使用 Ant Design 的 Design Token (如 `token.colorBgContainer`, `token.colorText`)，这样组件能自动适配深色模式。
- **代码提交**: 保持 Git 提交原子性，每次提交只包含一个逻辑变更。

---
*文档最后更新时间: 2025-11-28*
