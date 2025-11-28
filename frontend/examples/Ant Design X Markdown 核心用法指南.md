# Part 1: Ant Design X Markdown (XMarkdown) 核心用法指南

此文档旨在指导编程工具正确使用 `@ant-design/x-markdown` 组件，特别是在结合 `Bubble.List` 进行流式输出（Streaming）时的配置。

## 1. 核心依赖与样式引入

必须显式引入样式文件，否则代码高亮和排版会失效。

```tsx
import { XMarkdown } from '@ant-design/x-markdown';
// 必须引入 CSS
import '@ant-design/x-markdown/themes/light.css'; 
// 如果支持暗色模式，也引入 dark.css
// import '@ant-design/x-markdown/themes/dark.css'; 
```

## 2. 关键属性：`streaming` (流式动画)

这是实现“打字机效果”和“光标闪烁”的核心。普通的 `ReactMarkdown` 在接收流式数据时会整段跳动，而 `XMarkdown` 会平滑过渡。

```tsx
<XMarkdown
  // 核心配置
  streaming={{
    enableAnimation: true,  // 开启平滑动画
    hasNextChunk: boolean,  // 关键：告诉组件是否还有数据在路上
  }}
>
  {content}
</XMarkdown>
```

* **`hasNextChunk` 逻辑**：
  * 当消息状态为 `loading` 或 `processing` 时，设为 `true`（此时末尾会有闪烁的光标）。
  * 当消息状态为 `success` 或 `done` 时，设为 `false`（光标消失）。

## 3. 在 `Bubble.List` 中的最佳实践

不要直接在 `items` 里写 `<XMarkdown>`，而是应该利用 `role` 属性的 `contentRender` 插槽。这样可以获得消息的 `status` 来自动控制动画。

```tsx
<Bubble.List
  items={messages}
  role={{
    // 配置 AI (assistant) 的渲染逻辑
    ai: {
      // content: 消息文本
      // status: 'loading' | 'success' | 'error' (由 useXChat 或后端状态决定)
      contentRender: (content, { status }) => (
        <XMarkdown
          streaming={{
            enableAnimation: true,
            // 只有在非最终状态时，才显示光标
            hasNextChunk: status === 'pending' || status === 'loading',
          }}
        >
          {content}
        </XMarkdown>
      ),
    },
    // 用户配置保持默认
    user: { placement: 'end', variant: 'shadow' },
  }}
/>
```

## 4. 自定义组件 (代码高亮增强)

如果需要自定义代码块渲染（例如添加“复制”按钮），可以使用 `components` 属性，但 AntD X 默认已经内置了较好的代码块样式，通常无需重写，除非有特殊需求。

这非常关键！你提供的这份官方文档补充了 `XMarkdown` 最强大的两个能力：**插件生态（Plugins）** 和 **自定义组件的流式状态管理（streamStatus）**。

这意味着我们可以让 RAG 的回复不仅支持普通的 Markdown，还能完美渲染 **数学公式（LaTeX）**、**代码高亮**、**Mermaid 流程图**，甚至可以自定义组件来感知“正在生成中”的状态。

以下是为你整理的**两部分内容**：

1. **Ant Design X Markdown 进阶开发手册**（喂给编程工具的知识库）。
2. **重构提示词**（让编程工具执行升级任务）。

---

# Part 2: Ant Design X Markdown 进阶开发手册

此文档汇总了 `@ant-design/x-markdown` 的高级特性，用于指导 RAG 系统前端的富文本渲染开发。

## 1. 核心架构与性能优化

`components` 属性允许替换标准 HTML 标签或渲染自定义标签。

### 最佳实践 (Performance)

* **禁止内联定义**：永远不要在 `render` 函数内部定义组件，否则会导致每次流式更新都重新挂载 DOM，输入框会闪烁，性能极差。
* **正确做法**：在组件外部定义，或使用 `React.memo`。

```tsx
// ✅ 正确：组件定义在外部
const CustomH1 = (props) => <h1 style={{ color: 'blue' }} {...props} />;

const App = () => (
  <XMarkdown 
    content="..." 
    components={{ h1: CustomH1 }} // 引用静态引用
  />
);
```

## 2. 流式状态感知 (`streamStatus`)

自定义组件会接收 `streamStatus` 属性，用于判断当前节点是否还在生成中。

* **`'loading'`**: 标签尚未闭合（流正在输出该部分）。
* **`'done'`**: 标签已闭合（生成完成）。

**应用场景**：

* **思维链折叠**：检测到 `<thinking>` 标签时，如果 `status === 'loading'` 自动展开，`'done'` 自动收起。
* **数据卡片**：检测到 `<user-card id="1">` 时，仅在 `'done'` 状态下才发起 API 请求获取详情，避免流传输过程中重复请求。

```tsx
const AsyncCard = ({ streamStatus, id }) => {
  // 只有当流走完这个标签后，才加载数据，防止闪烁
  useEffect(() => {
    if (streamStatus === 'done') loadData(id);
  }, [streamStatus, id]);
  
  if (streamStatus === 'loading') return <LoadingSpinner />;
  return <DataDisplay />;
};
```

## 3. 插件系统 (Plugins)

要支持代码高亮、公式和图表，必须引入对应插件。

### 必需插件清单

1. **`HighlightCode`**: 代码块高亮。
2. **`Latex`**: 数学公式支持 (基于 Katex)。
3. **`Mermaid`**: 流程图/时序图支持。

```tsx
import { XMarkdown } from '@ant-design/x-markdown';
import HighlightCode from '@ant-design/x-markdown/plugins/highlight-code';
import Latex from '@ant-design/x-markdown/plugins/latex';
import Mermaid from '@ant-design/x-markdown/plugins/mermaid';

const plugins = [HighlightCode, Latex, Mermaid];

// 使用
<XMarkdown plugins={plugins} content="..." />
```
