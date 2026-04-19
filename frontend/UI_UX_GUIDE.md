# SmartRAG 前端 UI/UX 与个性化主题开发指南

> 面向后续开发者的说明文档，涵盖设计系统、动画、组件库的使用规范、统一视觉风格和主题个性化的实现方式。

---

## 目录

1. [技术栈总览](#1-技术栈总览)
2. [设计系统：颜色与主题核心](#2-设计系统颜色与主题核心)
3. [字体系统与动态加载](#3-字体系统与动态加载)
4. [个性化主题配置引擎 (ThemePopover)](#4-个性化主题配置引擎-themepopover)
5. [全局样式 CSS 变量引擎](#5-全局样式-css-变量引擎)
6. [Ant Design 主题配置 (CSS-in-JS)](#6-ant-design-主题配置-css-in-js)
7. [Tailwind CSS 配套约束](#7-tailwind-css-配套约束)
8. [动画系统 (Framer Motion)](#8-动画系统-framer-motion)
9. [高定玻璃态特效 (LiquidGlass)](#9-高定玻璃态特效-liquidglass)

---

## 1. 技术栈总览

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | React | 18.2 | 核心响应式框架 |
| 语言 | TypeScript | 5.x | 类型安全与接口保证 |
| 构建 | Vite | 7.2.4 | 开发构建与秒级热更新 |
| 组件库 | Ant Design | 6.0 | 基础表单、弹窗与反馈组件 |
| AI 场景 | @ant-design/x | 2.2.2 | 提供对话气泡、请求条、模型选择器 |
| Markdown | @ant-design/x-markdown | 2.x | 提供代码高亮、KaTeX 数学公式、Mermaid 图表支持 |
| 动画 | Framer Motion | 12.x | 页面路由过渡、元素连锁进场动画 |
| 原子样式 | Tailwind CSS | 4.x | 高效的工具类样式堆叠 |
| 状态管理 | Zustand | 5.x | 管理深色模式、用户设置及鉴权状态 |
| 视觉层 | tsParticles | 3.x | `ThemeBackground.tsx` 中的 Canvas 动态粒子特效 |

---

## 2. 设计系统：颜色与主题核心

### 设计理念

本系统默认出厂风格为 **科技蓝/极简质感**，但由于引入了完整的**个性化引擎（ThemePopover）**，所有的基础颜色由用户和运营方自由调配。
系统内置支持的基础色板包括：
- **科技蓝 (Tech Blue)**：默认主色系，沉稳专业。
- **极简黑白 (Monochrome)**：专注内容的无彩色调。
- **薄荷绿 (Mint Green)**：清新自然的色调。
- **樱花紫 (Sakura Purple)**：温柔活泼的色调。
- **日落橙 (Sunset Orange)**：温暖热情的色调。

### 状态色 (语义色锁死)

为了保证报错与警告的识别度，语义状态颜色全局受保护，不随用户定义的品牌色（colorPrimary）变化而变化：
```
success: #10b981 / #52c41a
warning: #f59e0b / #faad14
error:   #ef4444 / #ff4d4f
info:    主题色衍生 (同 colorPrimary)
```

### 颜色如何被修改

**工程级修改主色**：如果需要增加一种主题色，需要在 `src/config/themeConfig.ts` 中的 `COLOR_THEMES` 增加对应配置（含主题色 HEX 值及名称）。无需在各个组件中做任何处理，顶层 `ConfigProvider` 和 Zustand 的联动机制会自动把颜色下发。

---

## 3. 字体系统与动态加载

### 字体配置库 (`src/config/themeConfig.ts`)

为了满足阅读复杂 RAG 文档的不同需求，字体从硬编码改造为配置驱动。

| 类别 | 预设字体 | fallback 策略 |
|------|------|------|
| 无衬线 (现代) | Inter / Roboto | `system-ui, -apple-system, sans-serif` |
| 衬线 (传统阅读) | Merriweather / Noto Serif SC | `Georgia, serif` |
| 手写排版 (文艺) | 霞鹜文楷 (LXGW) | `cursive` |
| 代码/日志 | JetBrains Mono | `ui-monospace, Consolas, monospace` |

### 动态加载机制
由于中文字体极大（如霞鹜文楷包体积过大），前端系统不再将字体静态打入产物中。
替代方案为通过 `themeConfig` 的配置项联动 `<link>` 标签，或者在用户首次在控制面板选择新字体时，由浏览器动态获取外部 Web Font CDN (如 Google Fonts, JSDelivr)。

---

## 4. 个性化主题配置引擎 (ThemePopover)

本项目的一大特性即允许终端用户重塑 UI 感官。控制台代码详见：`src/components/ThemePopover.tsx`。

### 核心管理项
- **深浅模式 (ThemeMode)**：切换亮色或暗色。
- **UI 外观 (UIStyle)**：
  - `default`：拟物化、微立体阴影。
  - `flat`：扁平化极简，去除大多数投影与圆角。
  - `glass`：**独特毛玻璃特效**，对话区域和抽屉呈现半透磨砂材质。
- **排版系统 (Typography)**：调节 `fontFamily` 与 `fontSize`。
- **品牌色 (ColorTheme)**：改变按钮、光标、对话气泡的底色。

### 持久化
设置改变时会经由 `useAppStore` 自动将当前对象的序列化结果写入到 `localStorage` 的 `SmartRAG_personalization` 键中，进行用户侧的云端隔离。

---

## 5. 全局样式 CSS 变量引擎

**入口**：`src/index.css` 及 `src/App.css`。

系统使用了 CSS-in-JS + CSS Variables 混编的策略：
对于标准组件使用 Antd Token，对于自定义的高定组件（比如边栏的特殊呼吸灯效果）则绑定到了 `<html>` 或 `body` 上的 CSS 变量。

### 亮暗主题 CSS 隔离示例
```css
:root, [data-theme='light'] {
  --color-bg-base: #fafaf9;        /* 页面底色 */
  --color-border: #e7e5e4;         /* 主边框 */
  --md-code-bg: #f5f5f4;           /* Markdown 内部代码块背景 */
}

[data-theme='dark'] {
  --color-bg-base: #101014;
  --color-border: #333333;
  --md-code-bg: #1c1c1e;
}
```

---

## 6. Ant Design 主题配置 (CSS-in-JS)

**文件**：[src/App.tsx](src/App.tsx)
通过 `ConfigProvider` 利用了最新的 Token 计算体系。

### 覆盖最佳实践 
**禁止！**直接使用类名在 css 中重写：
```css
/* 错误示范 */
.ant-btn { background-color: red !important; }
```

**推荐！**在 `theme.components` 节点覆写：
```tsx
<ConfigProvider 
  theme={{ 
    token: { colorPrimary: '#...', borderRadius: 8 }, 
    components: { Button: { controlHeight: 40 } } 
  }}
...
```

---

## 7. 高定玻璃态特效 (LiquidGlass)

为了贴合 AI 工具的前沿调性，项目中特地开发了高耗能的炫酷材质——玻璃流态（Liquid Glass），通常用于 Dashboard 统计卡片和登录弹窗背景。

### 相关组件 
- `src/components/common/LiquidGlassCard.tsx`
- `src/components/common/LiquidGlassButton.tsx`

### 视觉特点与修改方式
该特点融合了 `backdrop-filter: blur()` 与多层渐变 border。
如果在低端机型上卡顿，或者在白主题下显示突兀，可以在 `useAppStore` 中把 `uiStyle` 强制切回 `default`。如果要修改其反光色泽，可编辑对应的 `tailwind` class 组合 `bg-white/10 border-white/20 shadow-[...]`。

Ant Design v6 采用了 **CSS-in-JS** 架构，会在运行时动态生成带有 hash 的类名。
**强烈不建议**在 `App.css` 中使用全局 CSS 类名（如 `.ant-card`）去强行覆盖样式，因为：
1. 容易被 Ant Design 自身的高优先级样式覆盖。
2. 破坏了主题切换的动态性。
3. DOM 结构嵌套深时难以生效。

**最佳实践：Token 穿透 + 独立 Canvas 背景层**
如果需要深度定制主题（例如添加粒子特效等背景）：
1. **修改 Design Token**：通过 `ConfigProvider` 将底层的 `colorBgLayout` 设为 `transparent`，将 `colorBgContainer` 设为半透明的 `rgba`，让底层透出来。
2. **关闭原生动画**：对于需要复杂动画的主题，可以通过 Token 关闭原生过渡动画（`motion: false`）。
3. **引入专业特效库**：在最底层放置一个 `z-index: 0` 且 `pointer-events: none` 的 Canvas 特效层（如 `tsparticles`）。

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

### 复杂背景与特效动画 (Canvas / tsParticles)

**文件**：[src/components/ThemeBackground.tsx](src/components/ThemeBackground.tsx)

对于需要视觉特效的主题（如粒子背景），我们使用 `@tsparticles/react` 渲染高性能的 Canvas 粒子动画。

- **层级控制**：Canvas 必须设置 `position: fixed`, `z-index: 0`, `pointer-events: none`，确保不阻挡用户交互。
- **Ant Design 配合**：在 `App.tsx` 的 `ConfigProvider` 中，将对应主题的 `colorBgLayout` 设为 `transparent`，`colorBgContainer` 设为半透明（如 `rgba(20, 20, 20, 0.6)`），让底层的 Canvas 特效能够透视出来。
- **动画冲突**：如果外部动画库（如 Framer Motion 或 tsParticles）与 Ant Design 的原生动画冲突，可以在 `ConfigProvider` 的 `token` 中设置 `motion: false` 关闭原生动画。

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

### 为什么我写的 CSS 覆盖没有生效？

1. **CSS-in-JS 优先级**：Ant Design v6 使用动态 hash 类名，优先级极高。不要在 `App.css` 中写 `.ant-card { background: red !important; }`。
2. **正确做法**：在 `src/App.tsx` 的 `componentTokens` 中找到对应的组件（如 `Card`），修改其 `colorBgContainer` 或 `borderRadius` 等 Token。
3. **透明背景**：如果你想让组件透明以显示底层 Canvas，必须在 Token 中设置 `rgba(..., 0.8)` 或 `transparent`，而不是用 CSS 强行覆盖。

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
