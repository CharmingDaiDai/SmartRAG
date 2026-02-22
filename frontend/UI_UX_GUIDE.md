# SmartDoc 前端 UI/UX 开发指南

> 面向后续开发者的说明文档，涵盖设计系统、动画、组件库的使用规范和修改方式。

---

## 目录

1. [技术栈总览](#1-技术栈总览)
2. [设计系统：颜色与主题](#2-设计系统颜色与主题)
3. [字体系统](#3-字体系统)
4. [全局样式 CSS 变量](#4-全局样式-css-变量)
5. [Ant Design 主题配置](#5-ant-design-主题配置)
6. [Tailwind CSS 配置](#6-tailwind-css-配置)
7. [动画系统（Framer Motion）](#7-动画系统framer-motion)
8. [布局系统](#8-布局系统)
9. [页面级说明](#9-页面级说明)
10. [组件说明](#10-组件说明)
11. [常见修改场景](#11-常见修改场景)

---

## 1. 技术栈总览

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | React | 18.2 | 核心框架 |
| 语言 | TypeScript | 5.x | 类型安全 |
| 构建 | Vite | 6.x | 开发/构建工具 |
| 组件库 | Ant Design | 6.0 | 通用 UI 组件 |
| AI 组件 | @ant-design/x | 1.x | 对话气泡、输入框、Markdown 渲染 |
| 动画 | Framer Motion | 12.x | 页面/元素动画 |
| 样式 | Tailwind CSS | 4.x | 工具类样式 |
| 状态管理 | Zustand | 5.x | 全局状态（主题、用户、知识库） |
| 路由 | React Router | 6.x | 页面路由 |
| 图标 | @ant-design/icons | 6.x | Ant Design 图标 |
| Markdown | @ant-design/x-markdown | 1.x | AI 消息的 Markdown + 代码高亮 |

---

## 2. 设计系统：颜色与主题

### 设计理念

整体风格为**温暖米白 + 靛紫主色**，参考 Notion/Bear 的极简质感。
- 背景使用暖调灰（Stone 系列），非冷灰
- 主色使用 Indigo（#6366f1），带紫色调，有书卷气
- 两套主题（亮色/暗色）同等重视，均精心设计

### 主色板（Indigo）

```
#eef2ff  ← primary-50  背景色（极浅）
#e0e7ff  ← primary-100 柔和背景
#c7d2fe  ← primary-200
#a5b4fc  ← primary-300
#818cf8  ← primary-400 暗色模式主色
#6366f1  ← primary-500 ★ 亮色模式主色（colorPrimary）
#4f46e5  ← primary-600 hover 状态
#4338ca  ← primary-700
```

### 中性色板（暖调灰 / Stone 系列）

```
#fafaf9  ← neutral-50  亮色页面背景
#f5f5f4  ← neutral-100 次级背景、表格 header
#e7e5e4  ← neutral-200 边框颜色
#d6d3d1  ← neutral-300 滚动条（亮色）
#a8a29e  ← neutral-400 次要文字
#78716c  ← neutral-500
#57534e  ← neutral-600 正文辅助色
#44403c  ← neutral-700 滚动条（暗色）
#292524  ← neutral-800 暗色次级背景
#1c1917  ← neutral-900 暗色卡片背景
#141210  ← neutral-950 暗色最底层背景
```

### 状态色

```
success: #10b981 （Emerald）
warning: #f59e0b （Amber）
error:   #ef4444 （Red）
info:    #6366f1 （Indigo，与主色一致）
```

### 如何修改颜色

**修改主色**：打开 [src/App.tsx](src/App.tsx)，找到 `lightToken` 对象，修改 `colorPrimary` 值。暗色模式主色修改 `darkToken` 中对应字段（暗色建议更浅，如 `#818cf8`）。

**修改背景色**：修改 `lightToken.colorBgLayout`（亮色页面底色）和 `darkToken.colorBgLayout`（暗色底色）。

---

## 3. 字体系统

### 字体配置

| 用途 | 字体 | 加载方式 |
|------|------|------|
| 正文 / UI | LXGW WenKai Screen（霞鹜文楷）| CDN（jsdelivr） |
| 降级 | PingFang SC → Noto Sans SC → system | 系统字体 |
| 代码 | JetBrains Mono | Google Fonts |

### 字体在哪里配置

**CDN 引入**：[index.html](index.html) 的 `<head>` 末尾

```html
<!-- JetBrains Mono（代码字体）-->
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500&display=swap">

<!-- LXGW WenKai Screen（中文字体）-->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/lxgw-wenkai-screen-web@1.1.0/lxgwwenkaiscreenr.css">
```

**Ant Design token**：[src/App.tsx](src/App.tsx) 的 `lightToken` 中

```typescript
fontFamily: "'LXGW WenKai Screen', 'PingFang SC', 'Noto Sans SC', -apple-system, sans-serif",
fontFamilyCode: "'JetBrains Mono', ui-monospace, Consolas, monospace",
```

**Tailwind**：[tailwind.config.js](tailwind.config.js) 的 `theme.extend.fontFamily`

```javascript
fontFamily: {
  sans: ["'LXGW WenKai Screen'", "'PingFang SC'", 'sans-serif'],
  mono: ["'JetBrains Mono'", 'ui-monospace', 'Consolas'],
},
```

### 如何修改字体

1. 替换 `index.html` 中的 CDN 链接为新字体
2. 同步修改 `src/App.tsx` 的 `fontFamily` token
3. 同步修改 `tailwind.config.js` 的 `fontFamily`

---

## 4. 全局样式 CSS 变量

**文件**：[src/index.css](src/index.css)

所有颜色通过 CSS 变量系统管理，避免硬编码，主题切换时自动生效。

### 变量定义

```css
:root, [data-theme='light'] {
  --color-bg-base: #fafaf9;        /* 页面底色 */
  --color-bg-elevated: #ffffff;    /* 卡片、容器 */
  --color-bg-muted: #f5f5f4;       /* 次级区块 */
  --color-border: #e7e5e4;         /* 主边框 */
  --color-border-subtle: #f5f5f4;  /* 细边框 */
  --color-text: #1c1917;           /* 主文字 */
  --color-text-secondary: #57534e; /* 辅助文字 */
  --color-text-muted: #a8a29e;     /* 次要文字 */
  --color-primary: #6366f1;        /* 主色 */
  --color-primary-hover: #4f46e5;  /* 主色 hover */
  --color-primary-soft: rgba(99,102,241,0.08); /* 主色浅底 */
  --color-primary-border: rgba(99,102,241,0.20); /* 主色边框 */
  --scrollbar-thumb: #d6d3d1;
  /* Markdown 样式变量 */
  --md-table-border: #e7e5e4;
  --md-code-bg: #f5f5f4;
  --md-pre-bg: #f6f8fa;
  /* 参考文档条目 */
  --ref-item-bg: #ffffff;
  --ref-item-border: #e7e5e4;
}

[data-theme='dark'] {
  /* 对应的暗色值 */
  --color-bg-base: #141210;
  --color-primary: #818cf8;
  /* ... */
}
```

### index.css 中还包含

- Markdown 表格样式（`.ant-markdown table`）
- 代码块样式（`.x-markdown-light`, `.x-markdown-dark`）
- 滚动条美化（`::-webkit-scrollbar`）
- 流式输出光标动画（`.streaming-cursor::after`）
- 主题过渡动画（`.ant-layout`, `.ant-card` 等）
- 参考文档悬停样式（`.reference-item`）

---

## 5. Ant Design 主题配置

**文件**：[src/App.tsx](src/App.tsx)

这是整个 UI 风格的核心配置文件。通过 `ConfigProvider` 的 `theme` 属性注入全局 token。

### 结构

```typescript
// 亮色 token（基础）
const lightToken = { ... };

// 暗色 token（覆盖亮色中差异的部分）
const darkToken = { ...lightToken, ... };

// 组件级 token（按钮、输入框、卡片等）
const componentTokens = { ... };

// 在 ConfigProvider 中使用
<ConfigProvider theme={{
  algorithm: themeMode === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm,
  token: themeMode === 'dark' ? darkToken : lightToken,
  components: componentTokens,
}}>
```

### 关键 token 说明

| Token | 值 | 作用 |
|-------|-----|------|
| `colorPrimary` | `#6366f1` | 主色（按钮、链接、焦点环） |
| `colorBgLayout` | `#fafaf9` | Layout 背景 |
| `colorBgContainer` | `#ffffff` | 卡片/内容区背景 |
| `colorBorder` | `#e7e5e4` | 边框颜色 |
| `colorBorderSecondary` | `#f5f5f4` | 细边框（侧边栏分割线等） |
| `borderRadius` | `8` | 基础圆角 |
| `borderRadiusLG` | `12` | 大圆角（卡片） |
| `controlHeight` | `36` | 表单控件高度（Input/Select/Button） |
| `motionDurationFast` | `'0.15s'` | 快速动画时长 |

### 组件级 token（componentTokens）

- **Menu**：选中项背景 `rgba(99,102,241,0.10)`，高度 40px，圆角 8px
- **Card**：圆角 12px，内边距 20px
- **Button**：字重 500，圆角 8px
- **Input**：圆角 8px，聚焦光晕 `rgba(99,102,241,0.12)`
- **Select**：选中项背景 `rgba(99,102,241,0.08)`
- **Table**：行高 12px，header 背景 `#f5f5f4`
- **Modal**：圆角 16px，内边距 28px
- **Slider**：轨道色 `#e7e5e4`，滑块色 `#6366f1`

### 如何修改 Ant Design 样式

1. **修改全局效果**：在 `lightToken` / `darkToken` 中修改对应 token
2. **修改某个组件**：在 `componentTokens` 的对应组件对象中修改
3. **某个页面特殊样式**：使用该组件的 `style` prop 或在页面 CSS 中覆盖

> **注意**：不要直接写 `!important` 覆盖 Ant Design 样式，优先使用 token 系统，否则暗色模式会失效。

---

## 6. Tailwind CSS 配置

**文件**：[tailwind.config.js](tailwind.config.js)

项目使用 Tailwind v4，`tailwind.config.js` 方式仍然兼容。

### 自定义色阶

```javascript
// tailwind.config.js
theme: {
  extend: {
    colors: {
      primary: {
        50: '#eef2ff',
        100: '#e0e7ff',
        500: '#6366f1',  // 主色
        600: '#4f46e5',  // hover
      },
      neutral: {
        50: '#fafaf9',
        100: '#f5f5f4',
        200: '#e7e5e4',
        400: '#a8a29e',
        700: '#44403c',
        900: '#1c1917',
        950: '#0c0a09',
      },
    },
    fontFamily: {
      sans: ["'LXGW WenKai Screen'", "'PingFang SC'", 'sans-serif'],
      mono: ["'JetBrains Mono'", 'ui-monospace', 'Consolas'],
    },
  },
},
```

### 使用 Tailwind 的场景

Tailwind 工具类主要用于**布局、间距**，颜色尽量使用 Ant Design token（`token.colorBorder` 等），避免直接用 `text-gray-500` 这样硬编码暗色无法适配的类。

---

## 7. 动画系统（Framer Motion）

### 动画组件库

**文件**：[src/components/common/Motion.tsx](src/components/common/Motion.tsx)

封装了所有通用动画组件，**全项目统一使用这个文件**，不要在页面中内联写 Framer Motion 动画，保持一致性。

### 统一的缓动曲线

```typescript
const EASE_OUT   = [0.25, 0.46, 0.45, 0.94]  // 标准出场（大多数动画）
const EASE_IN_OUT = [0.4, 0, 0.2, 1]           // 交互动画（hover）
```

> **为什么不用弹簧（spring）？** 弹簧动画在快速操作时会产生不自然的"弹跳"，贝塞尔曲线更克制、更专业。

### 可用动画组件

#### 进场动画

| 组件 | 效果 | 适用场景 |
|------|------|------|
| `<FadeIn>` | 淡入 | 页面级内容、弹窗 |
| `<SlideInUp>` | 从下方滑入（y: 12） | 卡片列表、表单 |
| `<SlideInDown>` | 从上方滑入 | 下拉提示 |
| `<SlideInLeft>` | 从左滑入（x: -20） | 侧边抽屉 |
| `<SlideInRight>` | 从右滑入（x: 20） | 右侧面板 |
| `<ScaleIn>` | 缩放淡入（scale: 0.96→1） | Modal 内容 |
| `<FadeInScale>` | 渐显+缩放 | Modal 内容（同上） |
| `<FadeInSlideUp>` | 渐显+上移（y: 14） | 通知、卡片 |

#### 列表动画

```tsx
// 子元素依次出现（stagger 效果）
<StaggerContainer>
  {items.map(item => (
    <StaggerItem key={item.id}>
      <YourCard />
    </StaggerItem>
  ))}
</StaggerContainer>
```

`StaggerContainer` 参数：`staggerChildren: 0.06s`，`delayChildren: 0.04s`

#### 交互动画

| 组件 | 效果 | 说明 |
|------|------|------|
| `<HoverCard>` | hover 时上移 3px + 阴影加深 | 卡片悬停，**不用 scale** 避免膨胀感 |
| `<HoverLift>` | hover 时上移 3px | 轻量悬停 |
| `<HoverScale>` | hover 时放大 1.03x | 图标、小元素（慎用于卡片） |

#### 状态动画

| 组件 | 效果 | 适用场景 |
|------|------|------|
| `<Pulse>` | 脉冲（opacity 0.45→1→0.45） | 加载骨架 |
| `<Spin>` | 旋转 360° | 加载指示器 |
| `<Bounce>` | 上下弹跳 | 等待提示 |
| `<Shake trigger={bool}>` | 左右抖动 | 表单验证错误 |

#### 动画变体（用于 `motion.div`）

```tsx
import { cardGridVariants, cardItemVariants, pageVariants } from '../components/common/Motion';

// 卡片网格
<motion.div variants={cardGridVariants} initial="hidden" animate="visible">
  <motion.div variants={cardItemVariants}>...</motion.div>
</motion.div>

// 页面过渡
<motion.div variants={pageVariants} initial="initial" animate="animate" exit="exit">
  <PageContent />
</motion.div>
```

### 使用示例

```tsx
import { FadeIn, SlideInUp, StaggerContainer, StaggerItem, HoverCard } from '../components/common/Motion';

// 页面整体淡入
<FadeIn>
  <PageContent />
</FadeIn>

// 列表卡片依次进场
<StaggerContainer style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
  {kbList.map(kb => (
    <StaggerItem key={kb.id}>
      <HoverCard style={{ borderRadius: 12 }}>
        <KbCard data={kb} />
      </HoverCard>
    </StaggerItem>
  ))}
</StaggerContainer>
```

### 添加新动画

在 `Motion.tsx` 末尾添加新的导出组件，遵循现有格式：

```tsx
export const YourAnimation: React.FC<HTMLMotionProps<"div">> = ({ children, ...props }) => (
  <motion.div
    initial={{ opacity: 0, y: 12 }}
    animate={{ opacity: 1, y: 0 }}
    exit={{ opacity: 0, y: -8 }}
    transition={{ duration: 0.28, ease: EASE_OUT }}
    {...props}
  >
    {children}
  </motion.div>
);
```

### 性能注意事项

- **Chat 消息列表**：不要对每条 Bubble 添加 Framer Motion 包装，流式更新时会触发大量重渲染
- **菜单项**：用纯 CSS `transition`，不用 Framer Motion
- **列表 stagger**：超过 20 项时减小 `staggerChildren`（0.03-0.04），否则最后一项进场太慢

---

## 8. 布局系统

**文件**：[src/layouts/BasicLayout.tsx](src/layouts/BasicLayout.tsx)

### 整体结构

```
<Layout> (height: 100vh, overflow: hidden)
├── <Sider width=220, collapsedWidth=64>
│   ├── Logo 区域 (height: 68px)
│   └── <Menu> (分三组：工作台 / 知识库 / AI 工具)
└── <Layout>
    ├── <Header height=56px>
    │   ├── 左：折叠按钮 + 页面标题
    │   └── 右：主题切换 + 用户菜单
    └── <Content>
        └── <Outlet /> (路由页面内容)
```

### 主题切换机制

```typescript
const { themeMode, toggleTheme } = useAppStore();
// themeMode: 'light' | 'dark'
// Ant Design: html[data-theme='...'] 属性
// 切换时：两套 token 自动生效
```

### 页面判断

部分页面（Chat、TestChat）需要全屏显示（无内边距、无圆角）：

```typescript
const isFullPage = location.pathname === '/chat' || location.pathname === '/test-chat';
// isFullPage = true 时，Content 的 margin/padding/borderRadius 均为 0
```

### 添加新菜单项

在 `menuItems` 数组中找到合适的 group，添加新的 item：

```typescript
{
  key: '/your-page',
  icon: <YourIcon />,
  label: '你的页面',
  onClick: () => navigate('/your-page'),
},
```

同时在 `PAGE_NAMES` 中添加对应标题：

```typescript
const PAGE_NAMES: Record<string, string> = {
  '/your-page': '你的页面名称',
  // ...
};
```

---

## 9. 页面级说明

### 认证页面

**文件**：[src/pages/auth/Login.tsx](src/pages/auth/Login.tsx)、[src/pages/auth/Register.tsx](src/pages/auth/Register.tsx)
**样式**：[src/pages/auth/login.css](src/pages/auth/login.css)

- 亮色背景：`#fafaf9` + 点阵纹理（radial-gradient，24px 间距）
- 暗色背景：`#141210` + 细线网格（40px 间距）
- 左侧插图区：3 个特性介绍列表（图标 + 标题 + 说明），Indigo 渐变背景
- 右侧表单区：登录/注册表单，提交按钮为 Indigo 渐变

### 仪表盘

**文件**：[src/pages/Dashboard/index.tsx](src/pages/Dashboard/index.tsx)

- 统计卡片：顶部 3px 彩色竖条（知识库 Indigo、文档 Emerald、对话 Amber）
- 图标容器：对应彩色浅底
- 数字字体：JetBrains Mono + `fontVariantNumeric: 'tabular-nums'`（数字等宽对齐）
- 折线图：fill 渐变 `rgba(99,102,241,0.25)`，stroke `#6366f1`
- 词云图：Indigo 色阶 `['#c7d2fe', '#a5b4fc', '#818cf8', '#6366f1', '#4f46e5', '#4338ca']`

### 知识库列表

**文件**：[src/pages/KnowledgeBase/index.tsx](src/pages/KnowledgeBase/index.tsx)

- 卡片顶部 3px 彩色条：NAIVE_RAG=Indigo, HISEM_RAG_FAST=Cyan, HISEM_RAG=Violet
- 操作菜单：`···` Dropdown（替代原来 4 个链接按钮）
- 创建知识库 Modal：RAG 策略用 3 张可点击卡片选择（替代 Select）
- 空状态：Ant Design `<Empty>` + 创建按钮

### 知识库详情

**文件**：[src/pages/KnowledgeBase/KnowledgeBaseDetail.tsx](src/pages/KnowledgeBase/KnowledgeBaseDetail.tsx)

- 顶部：`<Breadcrumb>` 面包屑（知识库列表 / 当前知识库名）
- 信息展示：4 张 mini 统计卡（文档数、已索引、RAG策略、Embedding模型）
- 文档状态：彩色圆点 + 文字（替代原来的 Tag）
- 索引进度：`<IndexingProgress>` Banner 样式（横条，非卡片）

### Chat 对话页

**文件**：[src/pages/Chat/index.tsx](src/pages/Chat/index.tsx)

三栏布局：
- **左侧（240px）**：历史对话，按「今天/昨天/更早」分组，hover 时显示删除按钮
- **中间**：消息区域 + 输入框。空状态有欢迎语 + 3 张建议问题卡
- **右侧（300px）**：知识库选择 + Collapse 折叠的参数配置（模型配置/检索配置/高级配置）

用户消息气泡样式：
```typescript
{
  background: 'rgba(99, 102, 241, 0.08)',
  border: '1px solid rgba(99, 102, 241, 0.18)',
  borderRadius: 12,
}
```

### 用户资料页

**文件**：[src/pages/User/index.tsx](src/pages/User/index.tsx)

- 头像：96px，hover 时出现半透明遮罩 + 相机图标
- 密码修改：用 `<Collapse>` 折叠（默认不展开）
- 布局：`overflowY: auto`（不撑破外层 Layout）

---

## 10. 组件说明

### AnimatedThoughtChain（思考链）

**文件**：[src/components/rag/AnimatedThoughtChain.tsx](src/components/rag/AnimatedThoughtChain.tsx)

可展开/收起的执行流程可视化，用于 AI 消息的 header 位置。

```tsx
<AnimatedThoughtChain
  items={thoughtItems}   // ThoughtItem[]
  title="执行流程"       // 可选，默认"执行流程"
/>
```

`ThoughtItem` 结构：
```typescript
interface ThoughtItem {
  title: string;
  status: 'pending' | 'processing' | 'success' | 'error';
  icon?: React.ReactNode;
  duration?: number;    // 耗时（毫秒）
  content?: string;     // 详细内容（Markdown）
}
```

样式：Indigo 浅底（`rgba(99,102,241,0.04)`）+ 浅边框，处理中节点用 `rgba(99,102,241,0.6)` 色点 + 脉冲动画。

### ReferenceViewer（参考文档）

**文件**：[src/components/ReferenceViewer.tsx](src/components/ReferenceViewer.tsx)

展示 AI 回答引用的文档片段，点击可查看全文（Modal）。

```tsx
<ReferenceViewer references={referenceItems} />
```

`ReferenceItem` 结构：
```typescript
interface ReferenceItem {
  title: string;
  score: number;   // 相关度（0-1）
  content?: string;
  id?: string | number;
}
```

样式：`.reference-item` CSS 类（在 `index.css` 中定义），hover 边框变为 Indigo。

### IndexingProgress（索引进度）

**文件**：[src/components/IndexingProgress.tsx](src/components/IndexingProgress.tsx)

通过 SSE 实时接收索引进度，Banner 样式横条显示。

```tsx
<IndexingProgress
  kbId="kb-001"
  onComplete={() => fetchData()}  // 索引完成回调
/>
```

- 进行中：Indigo 背景 + 进度条
- 成功：绿色背景
- 失败：红色背景，显示错误列表

### Motion 组件

见第 7 节动画系统。

---

## 11. 常见修改场景

### 修改某个颜色不对劲

1. 先检查是否是 Ant Design token 控制：用浏览器 DevTools 看元素的 class，如果是 `ant-*` 开头，在 [src/App.tsx](src/App.tsx) 的 `componentTokens` 中找对应 token
2. 如果是自定义样式：在对应页面的 `style={}` 中直接改，或改 CSS 变量（`src/index.css`）
3. 如果暗色模式颜色不对：确保没有硬编码颜色（如 `#303030`），改为 `token.colorBorderSecondary` 等 token 值

### 添加一个新页面

1. 在 `src/pages/` 下新建目录和 `index.tsx`
2. 在 [src/App.tsx](src/App.tsx) 的路由配置中添加路由
3. 在 [src/layouts/BasicLayout.tsx](src/layouts/BasicLayout.tsx) 的 `menuItems` 和 `PAGE_NAMES` 中添加
4. 页面根容器使用 `<FadeIn>` 包裹，内容列表用 `<StaggerContainer>` + `<StaggerItem>`
5. 不要在根容器加 `bg-gray-50` 或 `min-h-screen`，背景色由 Layout 统一管理

### 修改动画效果

- **某个动画太慢/太快**：在 [src/components/common/Motion.tsx](src/components/common/Motion.tsx) 中修改对应组件的 `duration`
- **某个动画不想要了**：把 `<SlideInUp>` 替换为普通 `<div>`
- **添加新的动画效果**：在 `Motion.tsx` 末尾添加新的导出组件

### 修改侧边栏菜单分组

在 [src/layouts/BasicLayout.tsx](src/layouts/BasicLayout.tsx) 的 `menuItems` 数组中，每个 `type: 'group'` 对象代表一个分组：

```typescript
{
  type: 'group' as const,
  label: !collapsed ? 'YOUR GROUP' : '',  // 折叠时隐藏 label
  children: [/* 菜单项 */],
},
```

### 修改 Chat 右侧参数面板

Chat 页面右侧的参数项由 [src/config/ragConfig.ts](src/config/ragConfig.ts) 中的 `searchConfig` 驱动，每个 RAG 策略有独立的配置：

```typescript
// src/config/ragConfig.ts
{
  key: 'topK',           // Form.Item name
  label: 'Top K',        // 显示标签
  type: 'slider',        // 类型：slider | select | switch | model_select
  min: 1, max: 20, step: 1,
  defaultValue: 5,
  description: '检索文档数量',  // Tooltip 说明
}
```

修改参数项：直接修改 `ragConfig.ts` 即可，Chat 页面会自动根据当前知识库策略渲染对应参数。

---

## 快速参考

| 需要改什么 | 改哪个文件 |
|-----------|-----------|
| 主色 / 全局颜色 | `src/App.tsx` (lightToken/darkToken) |
| 组件样式（圆角、高度等） | `src/App.tsx` (componentTokens) |
| CSS 变量（Markdown、滚动条等） | `src/index.css` |
| Tailwind 颜色/字体 | `tailwind.config.js` |
| 字体引入 | `index.html` |
| 动画组件 | `src/components/common/Motion.tsx` |
| 侧边栏菜单 | `src/layouts/BasicLayout.tsx` |
| 页面路由 | `src/App.tsx` |
| RAG 参数配置项 | `src/config/ragConfig.ts` |
| 全局状态（用户、主题、知识库） | `src/store/useAppStore.ts` |
| 认证页样式 | `src/pages/auth/login.css` |
