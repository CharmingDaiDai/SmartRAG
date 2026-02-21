/**
 * ========================================
 * SmartRAG RAG 对话界面
 * ========================================
 *
 * 【核心功能】
 * 1. 与知识库进行智能对话（RAG 检索增强生成）
 * 2. 支持多种 RAG 策略（Naive RAG、HiSem RAG、Graph RAG）
 * 3. 实时流式输出、思考过程可视化、参考文档展示
 * 4. 历史对话管理、参数配置、主题切换
 *
 * 【架构设计】
 * - 三栏布局：左侧历史对话列表 | 中间对话区域 | 右侧知识库选择和参数配置
 * - 数据流：用户输入 → SmartRAGChatProvider → SSE 流式响应 → 实时渲染
 * - 状态管理：使用 Zustand 全局状态（useAppStore）管理用户信息、主题、知识库 ID
 *
 * 【技术栈】
 * - UI 框架：Ant Design + Ant Design X（对话组件）
 * - 流式渲染：useXChat Hook + SSE（Server-Sent Events）
 * - Markdown 渲染：XMarkdown + KaTeX（数学公式）+ Mermaid（图表）
 * - 动画：Framer Motion（页面过渡动画）
 */

import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import {
  Bubble,
  Sender,
  XProvider,
} from '@ant-design/x';
import { useXChat } from '@ant-design/x-sdk';
import { XMarkdown, type ComponentProps } from '@ant-design/x-markdown';
import HighlightCode from '@ant-design/x-markdown/plugins/HighlightCode';
import Latex from '@ant-design/x-markdown/plugins/Latex';
import Mermaid from '@ant-design/x-markdown/plugins/Mermaid';
import '@ant-design/x-markdown/themes/light.css';
import '@ant-design/x-markdown/themes/dark.css';
import {
  UserOutlined,
  RobotOutlined,
  ClearOutlined,
  QuestionCircleOutlined,
  PlusOutlined,
  MessageOutlined,
  DeleteOutlined,
  LoadingOutlined,
  BookOutlined,
  ThunderboltOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { Layout, Select, Button, Space, Typography, theme, Form, Slider, Switch, Avatar, message, GetProp, Tooltip, Popconfirm, Collapse } from 'antd';
import { useSearchParams } from 'react-router-dom';
import { kbService } from '../../services/kbService';
import { modelService } from '../../services/modelService';
import { KnowledgeBaseItem, ThoughtItem, ReferenceItem } from '../../types';
import { useAppStore } from '../../store/useAppStore';
import { SmartRAGChatProvider } from '../../utils/SmartRagChatProvider';
import ReferenceViewer from '../../components/ReferenceViewer';
import AnimatedThoughtChain from '../../components/rag/AnimatedThoughtChain';
import { getMethodConfig, RAG_STRATEGIES } from '../../config/ragConfig';
import { FadeIn, SlideInUp, StaggerContainer, StaggerItem } from '../../components/common/Motion';

const { Sider, Content } = Layout;
const { Title, Text } = Typography;

/**
 * 扩展的消息内容接口
 * 除了基础的角色和内容，还包含 RAG 特有的思考过程和参考文档
 */
interface ExtendedMessageContent {
    role: string;              // 'user' | 'assistant'
    content: string;           // 消息文本内容
    thoughts?: ThoughtItem[];  // AI 的思考过程（检索、重排序等步骤）
    references?: ReferenceItem[]; // 参考的文档片段
}

/**
 * 自定义代码组件 - 用于 Markdown 中的代码块渲染
 * 支持语法高亮和 Mermaid 图表渲染
 */
const Code: React.FC<ComponentProps> = (props) => {
  const { className, children } = props;
  const lang = className?.match(/language-(\w+)/)?.[1] || '';

  if (typeof children !== 'string') return null;
  if (lang === 'mermaid') {
    return <Mermaid>{children}</Mermaid>;
  }
  return <HighlightCode lang={lang}>{children}</HighlightCode>;
};

/**
 * Markdown 组件映射（模块级常量，不在组件内创建）
 */
const MD_COMPONENTS = {
  code: Code,
};

// Module-level constant: Latex extensions config created once, not on every render.
// This avoids re-running the Latex plugin factory (which is expensive) on every SSE chunk.
const LATEX_EXTENSIONS = Latex({ katexOptions: { output: 'html', throwOnError: false } });
const MD_CONFIG = { extensions: LATEX_EXTENSIONS };

/**
 * 模拟历史对话数据（按日期分组）
 * TODO: 后续需要从后端 API 获取真实的历史对话记录
 */
const MOCK_HISTORY = [
    { id: '1', title: '关于 RAG 的原理', date: '2023-11-29', group: '今天' },
    { id: '2', title: '如何优化检索效果', date: '2023-11-28', group: '昨天' },
    { id: '3', title: '向量数据库对比', date: '2023-11-27', group: '更早' },
    { id: '4', title: 'LangChain 实践', date: '2023-11-26', group: '更早' },
    { id: '5', title: '大模型微调指南', date: '2023-11-25', group: '更早' },
];

/** 建议问题（空状态展示） */
const SUGGESTION_QUESTIONS = [
    { icon: <SearchOutlined />, text: '这个知识库里有哪些主要内容？' },
    { icon: <BookOutlined />, text: '帮我总结一下最重要的知识点' },
    { icon: <ThunderboltOutlined />, text: '有什么实际应用案例？' },
];

const ChatPage: React.FC = () => {
  // ========== Ant Design 主题 Token ==========
  const { token } = theme.useToken();

  // ========== 状态管理 ==========
  const [kbs, setKbs] = useState<KnowledgeBaseItem[]>([]);

  const { currentKbId, setCurrentKbId, token: authToken, userInfo, themeMode, localSettings } = useAppStore();

  const [searchParams] = useSearchParams();
  const kbIdParam = searchParams.get('kbId');

  const [historyList, setHistoryList] = useState(MOCK_HISTORY);
  const [activeHistoryId, setActiveHistoryId] = useState<string | null>(null);

  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
      if (kbIdParam) {
          setCurrentKbId(kbIdParam);
      }
  }, [kbIdParam, setCurrentKbId]);

  const currentKb = useMemo(() => kbs.find(kb => String(kb.id) === String(currentKbId)), [kbs, currentKbId]);

  const { strategy, endpoint } = useMemo(() => {
      if (!currentKb) return { strategy: RAG_STRATEGIES.NAIVE_RAG, endpoint: '/api/chat/rag/naive' };

      const type = currentKb.indexStrategyType || RAG_STRATEGIES.NAIVE_RAG;
      let ep = '/api/chat/rag/naive';

      if (type === RAG_STRATEGIES.HISEM_RAG) {
          ep = '/api/chat/rag/hisem';
      } else if (type === RAG_STRATEGIES.HISEM_RAG_FAST) {
          ep = '/api/chat/rag/hisem-fast';
      }

      return { strategy: type, endpoint: ep };
  }, [currentKb]);

  const [form] = Form.useForm();
  const [input, setInput] = useState('');

  const [llmModels, setLlmModels] = useState<string[]>([]);
  const [rerankModels, setRerankModels] = useState<string[]>([]);
  const [modelsLoaded, setModelsLoaded] = useState(false);

  const enableRerank = Form.useWatch('enable_rerank', form);

  useEffect(() => {
      const fetchModels = async () => {
          try {
              const llmRes: any = await modelService.getLLMs();
              if (llmRes.code === 200) setLlmModels(llmRes.data);

              const rerankRes: any = await modelService.getReranks();
              if (rerankRes.code === 200) setRerankModels(rerankRes.data);
          } catch (e) {
              console.error(e);
          } finally {
              setModelsLoaded(true);
          }
      };
      fetchModels();
  }, []);

  useEffect(() => {
      if (modelsLoaded) {
          const currentValues = form.getFieldsValue();

          if (currentValues.llmModelId && llmModels.length > 0 && !llmModels.includes(currentValues.llmModelId)) {
              if (localSettings?.defaultModel && llmModels.includes(localSettings.defaultModel)) {
                  form.setFieldValue('llmModelId', localSettings.defaultModel);
              } else {
                  form.setFieldValue('llmModelId', undefined);
              }
          } else if (!currentValues.llmModelId && localSettings?.defaultModel && llmModels.includes(localSettings.defaultModel)) {
              form.setFieldValue('llmModelId', localSettings.defaultModel);
          }

          if (currentValues.rerankModelId) {
             if (rerankModels.length > 0 && !rerankModels.includes(currentValues.rerankModelId)) {
                 form.setFieldValue('rerankModelId', undefined);
             } else if (rerankModels.length === 0) {
                 form.setFieldValue('rerankModelId', undefined);
             }
          } else if (!currentValues.rerankModelId && localSettings?.defaultRerank && rerankModels.includes(localSettings.defaultRerank)) {
              form.setFieldValue('rerankModelId', localSettings.defaultRerank);
          }
      }
  }, [modelsLoaded, llmModels, rerankModels, localSettings, form]);

  useEffect(() => {
      if (strategy) {
          form.resetFields();
          if (localSettings?.defaultModel && llmModels.includes(localSettings.defaultModel)) {
              form.setFieldValue('llmModelId', localSettings.defaultModel);
          }
          if (localSettings?.defaultRerank && rerankModels.includes(localSettings.defaultRerank)) {
              form.setFieldValue('rerankModelId', localSettings.defaultRerank);
          }
      }
  }, [strategy, form, localSettings, llmModels, rerankModels]);

  const provider = useMemo(() => {
    return new SmartRAGChatProvider(authToken || undefined, endpoint);
  }, [authToken, endpoint]);

  const { messages, onRequest, isRequesting, setMessages, abort } = useXChat({
    provider,
  });

  // ── Throttled scroll-to-bottom ──────────────────────────────────────────
  // Use RAF to avoid forcing layout reflow on every SSE chunk.
  const rafScrollRef = useRef<number | null>(null);
  const scrollToBottom = useCallback(() => {
    if (rafScrollRef.current !== null) return; // already scheduled
    rafScrollRef.current = requestAnimationFrame(() => {
      rafScrollRef.current = null;
      if (scrollRef.current) {
        scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
      }
    });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  useEffect(() => {
      const fetchKbs = async () => {
          try {
            const res: any = await kbService.list({});
            if (res.code === 200) {
                setKbs(res.data);
                if (res.data.length > 0 && !currentKbId) {
                    setCurrentKbId(res.data[0].id);
                }
            }
          } catch (e) {
              console.error(e);
          }
      };
      fetchKbs();
  }, []);

  const handleRequest = (text: string) => {
      if (!currentKbId || !currentKb) {
          message.warning('请先选择知识库');
          return;
      }

      const ragParams = form.getFieldsValue();

      const historyMessages = messages.map(m => ({
          role: m.message.role === 'user' ? 'user' : 'ai',
          content: m.message.content,
          id: m.id
      }));

      const requestBody: any = {
          kbId: currentKbId,
          question: text,
          embeddingModelId: currentKb.embeddingModelId,
          llmModelId: ragParams.llmModelId,
          rerankModelId: ragParams.rerankModelId,
          enableQueryRewrite: ragParams.enableQueryRewrite,
          enableQueryDecomposition: ragParams.enableQueryDecomposition,
          enableIntentRecognition: ragParams.enableIntentRecognition,
          enableHyde: ragParams.enableHyde,
      };

      if (strategy === RAG_STRATEGIES.NAIVE_RAG) {
          requestBody.topK = ragParams.topK;
          requestBody.threshold = ragParams.threshold;
      } else if (strategy === RAG_STRATEGIES.HISEM_RAG || strategy === RAG_STRATEGIES.HISEM_RAG_FAST) {
          requestBody.maxTopK = ragParams.maxTopK;
      }

      onRequest({
          messages: [
              ...historyMessages,
              { role: 'user', content: text, id: Date.now().toString() }
          ] as any,
          kbId: currentKbId,
          ragMethod: strategy,
          ragParams: requestBody
      });

      setInput('');
  };

  const handleNewChat = () => {
      setMessages([]);
      setActiveHistoryId(null);
      message.success('已开启新对话');
  };

  const handleHistoryClick = (id: string) => {
      setActiveHistoryId(id);
      setMessages([]);
      message.info('加载历史对话...');
      setTimeout(() => {
          setMessages([
              { id: '1', status: 'success', message: { role: 'user', content: '你好，什么是 RAG？' } as any },
              { id: '2', status: 'success', message: { role: 'assistant', content: 'RAG (Retrieval-Augmented Generation) 是一种结合了检索和生成的 AI 技术...' } as any }
          ]);
      }, 500);
  };

  const items: GetProp<typeof Bubble.List, 'items'> = useMemo(() => {
    // Exclude the last message if it's actively streaming — it's rendered separately
    // to avoid rebuilding the entire Bubble.List items array on every SSE chunk.
    const msgsToRender = (isRequesting && messages.length > 0)
        ? messages.slice(0, -1)
        : messages;

    return msgsToRender
        .filter((msg) => {
            const extendedMsg = msg.message as unknown as ExtendedMessageContent;
            if (extendedMsg.role !== 'user') {
                return extendedMsg.content && extendedMsg.content.trim() !== '';
            }
            return true;
        })
        .map((msg) => {
            const { message, status, id } = msg;
            const extendedMsg = message as unknown as ExtendedMessageContent;

            return {
                key: id,
                role: extendedMsg.role === 'user' ? 'user' : 'assistant',
                status: status,
                placement: extendedMsg.role === 'user' ? 'end' : 'start',
                content: extendedMsg.content,
                header: extendedMsg.thoughts && extendedMsg.thoughts.length > 0 ? (
                    <AnimatedThoughtChain
                        items={extendedMsg.thoughts}
                    />
                ) : undefined,
                avatar: extendedMsg.role === 'user' ? (
                    <Avatar src={userInfo?.avatarUrl || userInfo?.avatar} icon={<UserOutlined />} />
                ) : (
                    <Avatar icon={<RobotOutlined />} style={{ background: token.colorPrimary }} />
                ),
                variant: extendedMsg.role === 'user' ? 'outlined' : 'shadow',
                footer: extendedMsg.references && extendedMsg.references.length > 0 ? (
                    <ReferenceViewer references={extendedMsg.references} />
                ) : undefined,
                // 用户气泡：靛紫色调浅底
                ...(extendedMsg.role === 'user' ? {
                    styles: {
                        content: {
                            background: 'rgba(99, 102, 241, 0.08)',
                            border: '1px solid rgba(99, 102, 241, 0.18)',
                            borderRadius: 12,
                        }
                    }
                } : {}),
            };
        });
  }, [messages, isRequesting, token.colorPrimary, userInfo]);

  // The actively streaming assistant message — rendered separately to avoid
  // rebuilding Bubble.List items on every SSE chunk.
  const streamingMessage = useMemo(() => {
    if (!isRequesting || messages.length === 0) return null;
    const lastMsg = messages[messages.length - 1];
    const extendedMsg = lastMsg.message as unknown as ExtendedMessageContent;
    if (extendedMsg.role === 'user') return null;
    return { extendedMsg, status: lastMsg.status, id: lastMsg.id };
  }, [messages, isRequesting]);

  // Legacy: thought-only pending message (no content yet, has thoughts)
  const pendingAssistantMessage = useMemo(() => {
    if (!streamingMessage) return null;
    const { extendedMsg } = streamingMessage;
    if ((!extendedMsg.content || extendedMsg.content.trim() === '') &&
        extendedMsg.thoughts && extendedMsg.thoughts.length > 0) {
      return extendedMsg;
    }
    return null;
  }, [streamingMessage]);

  // 按分组整理历史对话
  const groupedHistory = useMemo(() => {
    const groups: Record<string, typeof MOCK_HISTORY> = {};
    historyList.forEach(item => {
      if (!groups[item.group]) groups[item.group] = [];
      groups[item.group].push(item);
    });
    return groups;
  }, [historyList]);

  // ── Stable role config for Bubble.List ──────────────────────────────────
  // Defined with useMemo keyed on themeMode only, so it doesn't change on
  // every SSE chunk. This prevents Bubble.List from re-rendering all historical
  // bubbles when only the last streaming bubble changes.
  const bubbleRole = useMemo(() => ({
    user: {
      contentRender: (content: string) => (
        <XMarkdown
          className={themeMode === 'dark' ? 'x-markdown-dark' : 'x-markdown-light'}
          // @ts-ignore
          config={MD_CONFIG}
          components={MD_COMPONENTS}
        >
          {content}
        </XMarkdown>
      ),
    },
    assistant: {
      contentRender: (content: string, { status }: any) => {
        if (!content) return null;
        return (
          <XMarkdown
            className={themeMode === 'dark' ? 'x-markdown-dark' : 'x-markdown-light'}
            // @ts-ignore
            config={MD_CONFIG}
            components={MD_COMPONENTS}
            streaming={{
              hasNextChunk: status === 'updating' || status === 'loading',
              enableAnimation: true,
              animationConfig: { fadeDuration: 80 },
            }}
          >
            {content}
          </XMarkdown>
        );
      },
    },
  }), [themeMode]);

  return (
    <XProvider theme={{ token: { colorPrimary: token.colorPrimary } }}>
        <Layout style={{ height: '100%', background: token.colorBgContainer }}>

        {/* ==================== 左侧边栏：历史对话列表 ==================== */}
        <Sider
            width={240}
            className="chat-sidebar-left"
            theme={themeMode === 'dark' ? 'dark' : 'light'}
            style={{
                borderRight: `1px solid ${token.colorBorderSecondary}`,
                background: token.colorBgContainer,
                height: '100%',
                display: 'flex',
                flexDirection: 'column'
            }}
        >
            {/* 新建对话按钮（顶部） */}
            <div style={{ padding: '12px 12px 8px', borderBottom: `1px solid ${token.colorBorderSecondary}` }}>
                <Button
                    type="dashed"
                    block
                    icon={<PlusOutlined />}
                    onClick={handleNewChat}
                    style={{
                        borderColor: token.colorBorderSecondary,
                        color: token.colorTextSecondary,
                        height: 36,
                        borderRadius: 8,
                    }}
                >
                    新建对话
                </Button>
            </div>

            {/* 历史对话分组列表 */}
            <div style={{ flex: 1, overflowY: 'auto', padding: '8px 0' }}>
                {Object.entries(groupedHistory).map(([group, items]) => (
                    <div key={group}>
                        {/* 分组标题 */}
                        <div style={{
                            padding: '8px 14px 4px',
                            fontSize: 10,
                            fontWeight: 600,
                            letterSpacing: '0.06em',
                            textTransform: 'uppercase',
                            color: token.colorTextQuaternary,
                        }}>
                            {group}
                        </div>
                        {items.map((item) => (
                            <div
                                key={item.id}
                                className="chat-history-item"
                                style={{
                                    padding: '8px 12px',
                                    margin: '1px 6px',
                                    borderRadius: 8,
                                    cursor: 'pointer',
                                    background: activeHistoryId === item.id
                                        ? 'rgba(99, 102, 241, 0.08)'
                                        : 'transparent',
                                    borderLeft: activeHistoryId === item.id
                                        ? `2px solid ${token.colorPrimary}`
                                        : '2px solid transparent',
                                    transition: 'all 0.15s ease',
                                    position: 'relative',
                                }}
                                onClick={() => handleHistoryClick(item.id)}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: 7, overflow: 'hidden', flex: 1 }}>
                                        <MessageOutlined style={{ fontSize: 12, color: token.colorTextTertiary, flexShrink: 0 }} />
                                        <Text
                                            ellipsis
                                            style={{
                                                fontSize: 13,
                                                color: activeHistoryId === item.id ? token.colorPrimary : token.colorText,
                                                fontWeight: activeHistoryId === item.id ? 500 : 400,
                                            }}
                                        >
                                            {item.title}
                                        </Text>
                                    </div>
                                    {/* hover 时显示删除按钮 */}
                                    <div
                                        className="chat-history-actions"
                                        onClick={(e) => e.stopPropagation()}
                                        style={{ flexShrink: 0, marginLeft: 4 }}
                                    >
                                        <Popconfirm
                                            title="确定删除该对话吗？"
                                            onConfirm={() => {
                                                message.success('删除成功');
                                                setHistoryList(prev => prev.filter(h => h.id !== item.id));
                                                if (activeHistoryId === item.id) setActiveHistoryId(null);
                                            }}
                                            okText="确定"
                                            cancelText="取消"
                                        >
                                            <DeleteOutlined style={{ fontSize: 12, color: token.colorTextTertiary }} />
                                        </Popconfirm>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                ))}
            </div>
        </Sider>

        {/* ==================== 中间内容区：对话消息列表 + 输入框 ==================== */}
        <Content style={{ display: 'flex', flexDirection: 'column', height: '100%', minWidth: 0 }}>
            <FadeIn style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
            {/* 消息滚动区域 */}
            <div ref={scrollRef} className="chat-messages-scroll" style={{ flex: 1, overflowY: 'auto', padding: 24, minHeight: 0, overscrollBehavior: 'contain' }}>
                {messages.length === 0 ? (
                    /* 空状态：欢迎界面 + 建议问题卡 */
                    <div style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        minHeight: '60%',
                        gap: 24,
                        paddingTop: 40,
                    }}>
                        {/* Logo + 欢迎语 */}
                        <div style={{ textAlign: 'center' }}>
                            <div style={{
                                width: 56,
                                height: 56,
                                borderRadius: 16,
                                background: 'rgba(99, 102, 241, 0.10)',
                                border: '1px solid rgba(99, 102, 241, 0.20)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                margin: '0 auto 16px',
                            }}>
                                <RobotOutlined style={{ fontSize: 24, color: token.colorPrimary }} />
                            </div>
                            <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                                你好，我是 SmartRAG
                            </Title>
                            <Text style={{ color: token.colorTextSecondary, fontSize: 14, marginTop: 6, display: 'block' }}>
                                {currentKb ? `正在使用「${currentKb.name}」知识库` : '请在右侧选择一个知识库开始对话'}
                            </Text>
                        </div>

                        {/* 建议问题卡片 */}
                        {currentKb && (
                            <StaggerContainer style={{ display: 'flex', flexDirection: 'column', gap: 10, width: '100%', maxWidth: 480 }}>
                                {SUGGESTION_QUESTIONS.map((q, i) => (
                                    <StaggerItem key={i}>
                                        <div
                                            className="suggestion-card"
                                            onClick={() => handleRequest(q.text)}
                                            style={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: 12,
                                                padding: '12px 16px',
                                                borderRadius: 10,
                                                border: `1px solid ${token.colorBorderSecondary}`,
                                                background: token.colorBgContainer,
                                                cursor: 'pointer',
                                                transition: 'all 0.18s ease',
                                                fontSize: 14,
                                                color: token.colorText,
                                            }}
                                        >
                                            <span style={{
                                                width: 28,
                                                height: 28,
                                                borderRadius: 7,
                                                background: 'rgba(99, 102, 241, 0.08)',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                color: token.colorPrimary,
                                                fontSize: 13,
                                                flexShrink: 0,
                                            }}>
                                                {q.icon}
                                            </span>
                                            {q.text}
                                        </div>
                                    </StaggerItem>
                                ))}
                            </StaggerContainer>
                        )}
                    </div>
                ) : (
                    <>
                        {/* 历史消息气泡列表 — 只在流式结束后更新，不在每个chunk时重建 */}
                        <Bubble.List
                            items={items}
                            styles={{
                                bubble: { maxWidth: '88%' }
                            }}
                            role={bubbleRole}
                        />
                        {/* 正在流式输出的消息 — 单独渲染，不触发Bubble.List重渲染 */}
                        {streamingMessage && (
                            <div style={{ display: 'flex', gap: 12, marginTop: 8, paddingBottom: 8 }}>
                                <Avatar icon={<RobotOutlined />} style={{ background: token.colorPrimary, flexShrink: 0 }} />
                                <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 8 }}>
                                    {/* 思考过程（thoughts-only phase） */}
                                    {pendingAssistantMessage && (
                                        <>
                                            <AnimatedThoughtChain items={pendingAssistantMessage.thoughts!} />
                                            {pendingAssistantMessage.thoughts!.length > 0 &&
                                             pendingAssistantMessage.thoughts![pendingAssistantMessage.thoughts!.length - 1].status === 'success' && (
                                                <div style={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    gap: 8,
                                                    color: token.colorTextSecondary,
                                                    fontSize: 13,
                                                    marginTop: 4
                                                }}>
                                                    <LoadingOutlined spin style={{ color: token.colorPrimary }} />
                                                    <span>思考中...</span>
                                                </div>
                                            )}
                                        </>
                                    )}
                                    {/* 流式文本内容 */}
                                    {streamingMessage.extendedMsg.content && (
                                        <>
                                            {streamingMessage.extendedMsg.thoughts && streamingMessage.extendedMsg.thoughts.length > 0 && (
                                                <AnimatedThoughtChain items={streamingMessage.extendedMsg.thoughts} />
                                            )}
                                            <XMarkdown
                                                className={themeMode === 'dark' ? 'x-markdown-dark' : 'x-markdown-light'}
                                                // @ts-ignore
                                                config={MD_CONFIG}
                                                components={MD_COMPONENTS}
                                                streaming={{
                                                    hasNextChunk: streamingMessage.status === 'updating' || streamingMessage.status === 'loading',
                                                    enableAnimation: true,
                                                    animationConfig: { fadeDuration: 80 },
                                                }}
                                            >
                                                {streamingMessage.extendedMsg.content}
                                            </XMarkdown>
                                        </>
                                    )}
                                </div>
                            </div>
                        )}
                    </>
                )}
            </div>
            {/* 输入框区域 */}
            <div className="chat-input-bar" style={{ padding: '12px 20px 16px', borderTop: `1px solid ${token.colorBorderSecondary}`, flexShrink: 0, background: token.colorBgContainer }}>
                <Sender
                    value={input}
                    onChange={setInput}
                    loading={isRequesting}
                    onSubmit={handleRequest}
                    onCancel={abort}
                    placeholder="输入问题，Shift + Enter 换行"
                />
            </div>
            </FadeIn>
        </Content>

        {/* ==================== 右侧边栏：知识库选择 + 参数配置 ==================== */}
        <Sider
            width={300}
            className="chat-sidebar-right"
            theme={themeMode === 'dark' ? 'dark' : 'light'}
            style={{
                borderLeft: `1px solid ${token.colorBorderSecondary}`,
                background: token.colorBgContainer,
                height: '100%'
            }}
        >
            <SlideInUp style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                <div style={{ flex: 1, overflowY: 'auto', padding: '16px 16px 0' }}>

                    {/* 知识库选择 */}
                    <div style={{ marginBottom: 20 }}>
                        <div style={{
                            fontSize: 10,
                            fontWeight: 600,
                            letterSpacing: '0.08em',
                            textTransform: 'uppercase',
                            color: token.colorTextQuaternary,
                            marginBottom: 8,
                        }}>
                            当前知识库
                        </div>
                        <Select
                            style={{ width: '100%' }}
                            value={currentKbId}
                            onChange={setCurrentKbId}
                            options={kbs.map(kb => ({ label: kb.name, value: kb.id }))}
                            placeholder="选择知识库"
                        />
                    </div>

                    {/* RAG 参数配置（折叠面板） */}
                    <Form
                        layout="vertical"
                        form={form}
                        key={strategy}
                        initialValues={{
                            topK: 5,
                            threshold: 0.0,
                            maxTopK: 10
                        }}
                        size="small"
                    >
                        <Collapse
                            defaultActiveKey={['model']}
                            ghost
                            size="small"
                            style={{ marginLeft: -8, marginRight: -8 }}
                            items={[
                                {
                                    key: 'model',
                                    label: (
                                        <span style={{
                                            fontSize: 10,
                                            fontWeight: 600,
                                            letterSpacing: '0.08em',
                                            textTransform: 'uppercase',
                                            color: token.colorTextQuaternary,
                                        }}>
                                            模型配置
                                        </span>
                                    ),
                                    children: (
                                        <div style={{ paddingTop: 4 }}>
                                            {strategy && getMethodConfig(strategy).searchConfig
                                                .filter((item: any) => item.type === 'model_select')
                                                .map((item: any) => {
                                                    if (item.dependency) {
                                                        const depValue = item.dependency.field === 'enable_rerank' ? enableRerank : form.getFieldValue(item.dependency.field);
                                                        if (depValue !== item.dependency.value) return null;
                                                    }
                                                    const models = item.modelType === 'llm' ? llmModels : rerankModels;
                                                    let initialValue;
                                                    if (item.modelType === 'llm' && localSettings?.defaultModel) {
                                                        initialValue = localSettings.defaultModel;
                                                    } else if (item.modelType === 'rerank' && localSettings?.defaultRerank) {
                                                        initialValue = localSettings.defaultRerank;
                                                    }
                                                    return (
                                                        <Form.Item
                                                            key={item.key}
                                                            name={item.key}
                                                            label={
                                                                <Space size={4}>
                                                                    <span style={{ fontSize: 12, color: token.colorTextSecondary }}>{item.label}</span>
                                                                    {item.description && (
                                                                        <Tooltip title={item.description}>
                                                                            <QuestionCircleOutlined style={{ fontSize: 11, color: token.colorTextTertiary }} />
                                                                        </Tooltip>
                                                                    )}
                                                                </Space>
                                                            }
                                                            valuePropName="value"
                                                            initialValue={initialValue}
                                                            style={{ marginBottom: 12 }}
                                                        >
                                                            <Select placeholder={`请选择 ${item.label}`}>
                                                                {models.map(model => (
                                                                    <Select.Option key={model} value={model}>{model}</Select.Option>
                                                                ))}
                                                            </Select>
                                                        </Form.Item>
                                                    );
                                                })
                                            }
                                        </div>
                                    ),
                                },
                                {
                                    key: 'search',
                                    label: (
                                        <span style={{
                                            fontSize: 10,
                                            fontWeight: 600,
                                            letterSpacing: '0.08em',
                                            textTransform: 'uppercase',
                                            color: token.colorTextQuaternary,
                                        }}>
                                            检索配置
                                        </span>
                                    ),
                                    children: (
                                        <div style={{ paddingTop: 4 }}>
                                            {strategy && getMethodConfig(strategy).searchConfig
                                                .filter((item: any) => item.type !== 'model_select' && item.type !== 'switch')
                                                .map((item: any) => {
                                                    if (item.dependency) {
                                                        const depValue = item.dependency.field === 'enable_rerank' ? enableRerank : form.getFieldValue(item.dependency.field);
                                                        if (depValue !== item.dependency.value) return null;
                                                    }
                                                    const formInitialKeys = ['topK', 'threshold', 'maxTopK'];
                                                    const initialValue = formInitialKeys.includes(item.key) ? undefined : item.defaultValue;
                                                    let inputNode = <div />;
                                                    if (item.type === 'select') {
                                                        inputNode = (
                                                            <Select>
                                                                {item.options?.map((opt: any) => (
                                                                    <Select.Option key={opt.value} value={opt.value}>{opt.label}</Select.Option>
                                                                ))}
                                                            </Select>
                                                        );
                                                    } else if (item.type === 'slider') {
                                                        inputNode = (
                                                            <Slider
                                                                min={item.min}
                                                                max={item.max}
                                                                step={item.step}
                                                                marks={{ [item.min]: item.min, [item.max]: item.max }}
                                                                style={{ marginLeft: 8, marginRight: 8 }}
                                                            />
                                                        );
                                                    }
                                                    return (
                                                        <Form.Item
                                                            key={item.key}
                                                            name={item.key}
                                                            label={
                                                                <Space size={4}>
                                                                    <span style={{ fontSize: 12, color: token.colorTextSecondary }}>{item.label}</span>
                                                                    {item.description && (
                                                                        <Tooltip title={item.description}>
                                                                            <QuestionCircleOutlined style={{ fontSize: 11, color: token.colorTextTertiary }} />
                                                                        </Tooltip>
                                                                    )}
                                                                </Space>
                                                            }
                                                            valuePropName="value"
                                                            initialValue={initialValue}
                                                            style={{ marginBottom: 12 }}
                                                        >
                                                            {inputNode}
                                                        </Form.Item>
                                                    );
                                                })
                                            }
                                        </div>
                                    ),
                                },
                                {
                                    key: 'advanced',
                                    label: (
                                        <span style={{
                                            fontSize: 10,
                                            fontWeight: 600,
                                            letterSpacing: '0.08em',
                                            textTransform: 'uppercase',
                                            color: token.colorTextQuaternary,
                                        }}>
                                            高级配置
                                        </span>
                                    ),
                                    children: (
                                        <div style={{ paddingTop: 4 }}>
                                            {strategy && getMethodConfig(strategy).searchConfig
                                                .filter((item: any) => item.type === 'switch')
                                                .map((item: any) => {
                                                    if (item.dependency) {
                                                        const depValue = item.dependency.field === 'enable_rerank' ? enableRerank : form.getFieldValue(item.dependency.field);
                                                        if (depValue !== item.dependency.value) return null;
                                                    }
                                                    return (
                                                        <Form.Item
                                                            key={item.key}
                                                            name={item.key}
                                                            label={
                                                                <Space size={4}>
                                                                    <span style={{ fontSize: 12, color: token.colorTextSecondary }}>{item.label}</span>
                                                                    {item.description && (
                                                                        <Tooltip title={item.description}>
                                                                            <QuestionCircleOutlined style={{ fontSize: 11, color: token.colorTextTertiary }} />
                                                                        </Tooltip>
                                                                    )}
                                                                </Space>
                                                            }
                                                            valuePropName="checked"
                                                            initialValue={item.defaultValue}
                                                            style={{ marginBottom: 12 }}
                                                        >
                                                            <Switch checkedChildren="开启" unCheckedChildren="关闭" />
                                                        </Form.Item>
                                                    );
                                                })
                                            }
                                        </div>
                                    ),
                                },
                            ]}
                        />
                    </Form>
                </div>

                {/* 清空对话按钮 */}
                <div style={{ padding: '12px 16px', borderTop: `1px solid ${token.colorBorderSecondary}` }}>
                    <Button block danger icon={<ClearOutlined />} onClick={() => setMessages([])} size="small">
                        清空对话
                    </Button>
                </div>
            </div>
            </SlideInUp>
        </Sider>
        </Layout>
    </XProvider>
  );
};

/**
 * ========================================
 * 关键扩展点和优化建议
 * ========================================
 *
 * 【历史对话持久化】
 * - 当前使用模拟数据（MOCK_HISTORY）
 * - 需要实现：保存对话到后端、从后端加载历史对话、对话分页加载
 * - 建议接口：GET /api/chat/conversations, GET /api/chat/conversations/:id
 *
 * 【流式渲染优化】
 * - SmartRAGChatProvider 处理 SSE 事件流
 * - 支持的事件类型：thought（思考过程）、ref（参考文档）、message（流式文本）、done（完成）
 * - 可扩展新的事件类型，如 image、file 等
 *
 * 【参数配置持久化】
 * - 当前参数配置在切换知识库或刷新页面后会重置
 * - 建议：将用户的参数偏好保存到 localStorage 或用户设置中
 *
 * 【性能优化】
 * - useMemo 用于缓存计算结果（currentKb、strategy、items 等）
 * - 大量消息时考虑虚拟滚动（react-window 或 react-virtualized）
 * - 图片/文件类消息考虑懒加载
 *
 * 【错误处理】
 * - 网络错误、模型错误、超时等异常情况需要更完善的错误提示
 * - 考虑添加重试机制和错误恢复策略
 *
 * 【多模态支持】
 * - 当前仅支持文本对话
 * - 可扩展：图片上传、文件上传、语音输入等
 * - Bubble 组件支持自定义渲染，可以展示富媒体内容
 */

export default ChatPage;
