/**
 * SmartRAG 对话流式服务提供者 (基于 @ant-design/x-sdk)
 * 
 * 功能逻辑：
 * 1. 继承并实现 `AbstractChatProvider`，专门对接具备生成阶段、思维链阶段（Thought Chain）和参考引用的 SSE 端点。
 * 2. 封装底层的 XRequest（负责管理 SSE 流的发起与流式数据读取）。
 * 3. 通过 `transformMessage` 方法解析每一个从服务端涌入的 SSE 数据块（Event & Data），并将零散的数据拼接、归类到本地的状态消息（ChatMessage）中。
 * 4. 支持各种中间态的解析：模型检索思考（thought_process）、片段拼接（chunk）、文档分片展现（references）。
 * 
 * 核心原理：
 * 每次获取到 server 端的流片段时，重组 originMessage 对象并返回最新拷贝，React 会响应式更新对话框内容。
 */
import { AbstractChatProvider, XRequest, XRequestOptions } from '@ant-design/x-sdk';
import { ChatMessage, ThoughtItem, RetrievalTreeNode, TokenUsageReport } from '../types';
import { 
    EditOutlined, 
    SearchOutlined, 
    BranchesOutlined, 
    SortAscendingOutlined, 
    CheckCircleOutlined, 
    LoadingOutlined,
    CloseCircleOutlined
} from '@ant-design/icons';
import { message } from 'antd';
import React from 'react';

/** 对话提供者输入的结构定义定义：消息历史，绑定的知识库，使用的检索策略及其参数 */
interface ChatInput {
  messages: ChatMessage[];
  kbId?: string;
  ragMethod?: string;
  ragParams?: any;
}

/** 对话流式输出（SSE）的结构定义：事件名和附带数据 */
interface ChatOutput {
  event: string;
  data: any;
}

/**
 * 根据服务端返回的底层步骤名映射对应图标
 * @param name 步骤名 (如 search, sort, check 等)
 * @returns 实例化的 React SVG 图标组件
 */
const getIcon = (name: string) => {
    switch(name) {
        case 'edit': return React.createElement(EditOutlined); // 预处理阶段
        case 'search': return React.createElement(SearchOutlined); // 检索阶段
        case 'merge': return React.createElement(BranchesOutlined); // 合并/组织阶段
        case 'sort': return React.createElement(SortAscendingOutlined); // 重排序阶段
        case 'check': return React.createElement(CheckCircleOutlined); // 检查/完成阶段
        case 'error': return React.createElement(CloseCircleOutlined, { style: { color: '#ff4d4f' } }); // 故障阶段
        default: return React.createElement(LoadingOutlined); // 默认加载状态
    }
};

export class SmartRAGChatProvider extends AbstractChatProvider<ChatMessage, ChatInput, ChatOutput> {
  /**
   * 初始化包含授权的 SSE 请求实例 
   * @param token 传入当前的 JWT 认证凭据，供内部的 fetch 挂载 Authorization header
   * @param endpoint 指向后端处理对话的接口地址
   */
  constructor(token?: string, endpoint: string = '/api/chat/test/full-rag-emitter') {
    super({
      request: XRequest(endpoint, {
        manual: true,
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}), // 将用户身份附着在连接上
        },
      })
    });
  }

  /**
   * 发起请求前将应用层传递进来的本地变量转化为 XRequest 的最终载荷 (Payload)
   */
  transformParams(
    requestParams: Partial<ChatInput>,
    _options: XRequestOptions<ChatInput, ChatOutput>
  ): ChatInput {
    const { ragParams } = requestParams;

    return {
        ...ragParams
    } as any;
  }

  /**
   * 用来确定用户发送的这一条本地消息对象长什么样
   */
  transformLocalMessage(requestParams: Partial<ChatInput>): ChatMessage {
    const { messages } = requestParams;
    return messages?.[messages.length - 1] || {
      id: Date.now().toString(),
      role: 'user',
      content: '', // 未获取到对应值时的回退
      createTime: Date.now()
    };
  }

  /**
   * 核心解析函数：每当服务端有数据冲入前端缓冲区时触发。
   * 控制这回合需要更新的 Message 片段，把最新的 AI 消息结构扔出去由 UI 层接管渲染。
   * @param info 包含前一条的消息拷贝、新到的块、历史拼接块及当前执行阶段。
   */
  transformMessage(info: {
    originMessage?: ChatMessage;
    chunk: any;
    chunks: any[];
    status: 'local' | 'loading' | 'updating' | 'success' | 'error' | 'abort';
  }): ChatMessage {
    const { originMessage, chunk, status } = info;
    
    // 如果是第一次接到响应，则创建一个干净的占位符；否则基于上一次的解析态继续补充内容
    const currentMessage: ChatMessage = originMessage || {
      id: Date.now().toString(),
      role: 'ai',
      content: '',
      thoughts: [],
      createTime: Date.now(),
      status: 'loading'
    };

      /**
       * 将处于 'processing' (加载中) 状态的思绪节点强行结束
       * 比如对话结束或流发生报错中断时被动调用该函数来收尾
       */
      const finalizeProcessingThoughts = (targetStatus: 'success' | 'error') =>
        currentMessage.thoughts?.map(t => {
          if (t.status === 'processing') {
            const startTime = (t as any).startTime || Date.now();
            return {
              ...t,
              status: targetStatus,
              duration: Date.now() - startTime
            };
          }
          return t;
        });

      /**
       * 创建系统容错或异常展示信息
       */
      const toErrorMessage = (errorText: string): ChatMessage => {
        const safeErrorText = errorText || '服务异常，请稍后重试。';
        const baseContent = currentMessage.content || '';
        const hasContent = baseContent.trim().length > 0;
        const tip = `[系统提示] ${safeErrorText}`;

        // 避免重复追加错误字样
        const nextContent = hasContent
          ? (baseContent.includes(tip) ? baseContent : `${baseContent}\n\n${tip}`)
          : safeErrorText;

        return {
          ...currentMessage,
          thoughts: finalizeProcessingThoughts('error'),
          content: nextContent,
          status: 'error'
        };
      };

    // 如果流最终圆满结束 (SSE 关闭)。把所有加载中状态洗为 finished
    if (status === 'success') {
        return {
            ...currentMessage,
            status: 'success',
          thoughts: finalizeProcessingThoughts('success')
        };
    }

      // 如果流中断或读取意外失败的后摇清理工作
      if (!chunk) {
        if (status === 'error' || status === 'abort') {
          const fallback = status === 'abort'
            ? '请求已中断，请重试。'
            : '服务异常，请稍后重试。';
          message.error(fallback);
          return toErrorMessage(fallback);
        }
        return currentMessage;
      }

    // 解析从 XRequest 发出的 SSE 事件簇
    let { event, data } = chunk;

    // 根据 JSON 序列化规则进行智能脱敏，提取真正的数据核心
    if (typeof data === 'string') {
        try {
            // Try to parse if it looks like JSON, otherwise treat as raw string
            if (data.trim().startsWith('{') || data.trim().startsWith('[')) {
                 const parsed = JSON.parse(data);
                 data = parsed;
            }
        } catch (e) {
            // ignore
        }
    }
    
    // 确保从 SSE 拿出来的数据变成普通字符串（部分框架为了防转义可能又嵌了一层结构如 {delta: 'xxx'}）
    const textData = typeof data === 'string' ? data : (data?.delta || JSON.stringify(data));

    // 如果流被强制打断或者因为 500 等 HTTP 错误被阻断时的回显处理
    if (status === 'error' || status === 'abort') {
      const fallback = status === 'abort'
        ? '请求已中断，请重试。'
        : '服务异常，请稍后重试。';
      // 提取服务端可能有幸回传的报错原文
      const errorText = (data && typeof data === 'object' && data.error)
        ? data.error
        : (typeof data === 'string' && data.trim() ? data : fallback);
      message.error(errorText);
      return toErrorMessage(errorText);
    }

    // 根据自定义协议的多段式事件 (event)，将不同阶段吐出的数据喂给不同图层的 UI 组件
    switch (event) {
      // 1. 系统思考过程（如正在执行向量检索、对内容进行总结）
      case 'thought':
        const thoughts = currentMessage.thoughts || [];
        const lastThought = thoughts[thoughts.length - 1];
        
        // 状态转接优化逻辑：如果在执行到此步时收到一个新的 Thought 事件，
        // 前置前提必然是上一个 Thought 已经完成了（不再处于 processing 态），并把运行时长计算并落盘。
        let updatedThoughts = [...thoughts];
        if (lastThought && lastThought.status === 'processing') {
             const endTime = Date.now();
             const startTime = (lastThought as any).startTime || (endTime - 1000);
             updatedThoughts[updatedThoughts.length - 1] = {
                 ...lastThought,
                 status: 'success', // 将前一步设定为绿色的已完成状态
                 duration: endTime - startTime // 记录消耗毫秒值
             };
        }

        const thoughtStatus = data.status || 'processing';
        
        if (thoughtStatus === 'error') {
            message.error(data.content || '处理过程中发生错误');
        }

        // 创建新的思维链节点并向其中压入时间戳和内容数据
        // Backend 发送类似: { status, content, icon }
        const newThought: ThoughtItem & { startTime: number } = {
            title: data.content || data.title || (thoughtStatus === 'error' ? '发生错误' : '思考中...'),
            status: thoughtStatus,
            content: '',
            icon: getIcon(data.icon || (thoughtStatus === 'error' ? 'error' : undefined)),
            duration: 0,
            startTime: Date.now()
        };
        
        // 更新 Message 到 Redux 等外部 Store (状态为 updating 证明流还没完)
        return { 
            ...currentMessage, 
            thoughts: [...updatedThoughts, newThought],
            status: thoughtStatus === 'error' ? 'error' : 'updating'
        };

      // 2. 接收结构化的文档打回结果 (往往在 HiSem 策略用到树形图回显)
      case 'retrieval_tree':
        return {
            ...currentMessage,
            retrievalTree: Array.isArray(data) ? (data as RetrievalTreeNode[]) : [],
            status: 'updating',
        };

      // 3. 将大模型消耗的 Token 以特定事件抛给前端并接住后进行图表渲染
      case 'token_usage':
        return {
            ...currentMessage,
            tokenUsage: data as TokenUsageReport,
            status: 'updating',
        };

      // 4. 接住了来自引擎在回答开始之前的具体参考切片列表
      case 'ref':
        // Handle references
        return {
            ...currentMessage,
            references: Array.isArray(data) ? data : [],
            status: 'updating'
        };

      // 5. 模型正处于逐字生成的文本输出阶段（与 OpenAI 规范齐平的 message payload）
      case 'message':
        // 如果系统开始正经说话了，要一刀切平前面所有未收尾的检索 thought，防止假死
        let currentThoughts = currentMessage.thoughts || [];
        const lastProcessingThought = currentThoughts[currentThoughts.length - 1];
        if (lastProcessingThought && lastProcessingThought.status === 'processing') {
             const endTime = Date.now();
             const startTime = (lastProcessingThought as any).startTime || (endTime - 1000);
             const finishedThought = { 
                 ...lastProcessingThought, 
                 status: 'success' as const,
                 duration: endTime - startTime
             };
             currentThoughts = [...currentThoughts.slice(0, -1), finishedThought];
        }

        return {
          ...currentMessage,
          thoughts: currentThoughts,
          content: (currentMessage.content || '') + textData,
          status: 'updating'
        };
      
      case 'done':
        return {
            ...currentMessage,
            status: 'success',
            // Finalize any remaining processing thoughts
          thoughts: finalizeProcessingThoughts('success')
        };

        case 'error': {
          const streamErrorText = (data && typeof data === 'object' && data.error)
              ? data.error
              : (typeof data === 'string' && data.trim() ? data : '服务异常，请稍后重试。');
          message.error(streamErrorText);
          // Throwing here lets x-sdk enter onError and reset isRequesting immediately.
          const streamError = new Error(streamErrorText);
          streamError.name = 'StreamEventError';
          throw streamError;
        }
        
      default:
        return currentMessage;
    }
  }
}
