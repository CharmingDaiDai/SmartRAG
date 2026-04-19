/**
 * ========================================
 * SmartRAG RAG 对话界面
 * ========================================
 *
 * 【核心功能】
 * 1. 与知识库进行智能对话（RAG 检索增强生成）
 * 2. 支持多种 RAG 策略（Naive RAG、HiSem RAG Fast、HiSem-SADP）
 * 3. 实时流式输出、执行流程可视化、参考文档展示
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
  CodeHighlighter,
  Mermaid,
} from '@ant-design/x';
import { useXChat } from '@ant-design/x-sdk';
import { XMarkdown, type ComponentProps } from '@ant-design/x-markdown';
import Latex from '@ant-design/x-markdown/plugins/Latex';
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
import { Layout, Select, Button, Space, Typography, theme, Form, Slider, Switch, Avatar, message, Modal, GetProp, Tooltip, Popconfirm, Collapse } from 'antd';
import { useSearchParams } from 'react-router-dom';
import { kbService } from '../../services/kbService';
import { documentService } from '../../services/documentService';
import { modelService } from '../../services/modelService';
import { conversationService } from '../../services/conversationService';
import { KnowledgeBaseItem, DocumentItem, ThoughtItem, ReferenceItem, RetrievalTreeNode, TokenUsageReport, ConversationSessionItem, ConversationSessionDetail } from '../../types';
import { useAppStore } from '../../store/useAppStore';
import { SmartRAGChatProvider } from '../../utils/SmartRagChatProvider';
import ReferenceViewer from '../../components/ReferenceViewer';
import RetrievalTreeViewer from '../../components/RetrievalTreeViewer';
import { TokenUsagePanel } from '../../components/TokenUsagePanel';
import AnimatedThoughtChain from '../../components/rag/AnimatedThoughtChain';
import { getMethodConfig, normalizeStrategyType, RAG_STRATEGIES } from '../../config/ragConfig';
import { FadeIn, SlideInUp, StaggerContainer, StaggerItem } from '../../components/common/Motion';

const { Sider, Content } = Layout;
const { Title, Text } = Typography;

/**
 * 扩展的消息内容接口
 * 除了基础的角色和内容，还包含 RAG 特有的执行流程和参考文档
 */
interface ExtendedMessageContent {
    role: string;              // 'user' | 'assistant'
    content: string;           // 消息文本内容
    thoughts?: ThoughtItem[];  // AI 的执行流程（检索、重排序等步骤）
    references?: ReferenceItem[]; // 参考的文档片段
    retrievalTree?: RetrievalTreeNode[]; // 检索路径树
    tokenUsage?: TokenUsageReport; // Token 用量明细
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
  return <CodeHighlighter lang={lang}>{children}</CodeHighlighter>;
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

const createSessionId = (): string => {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
        return crypto.randomUUID().replace(/-/g, '');
    }
    return `${Date.now()}${Math.random().toString(16).slice(2, 10)}`;
};

const getDateGroup = (isoDate?: string): string => {
    if (!isoDate) return '更早';

    const date = new Date(isoDate);
    if (isNaN(date.getTime())) return '更早';

    const now = new Date();
    const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const startOfYesterday = new Date(startOfToday);
    startOfYesterday.setDate(startOfYesterday.getDate() - 1);

    if (date >= startOfToday) return '今天';
    if (date >= startOfYesterday) return '昨天';
    return '更早';
};

/** 建议问题（空状态展示） */
const SUGGESTION_QUESTIONS = [
    { icon: <SearchOutlined />, text: '主变压器安装的工艺标准是什么？' },
    { icon: <BookOutlined />, text: '中性点系统设备安装的关键工序控制？' },
    { icon: <ThunderboltOutlined />, text: '油浸式站用变压器安装的关键工序控制？' },
];

const ChatPage: React.FC = () => {
  // ========== Ant Design 主题 Token ==========
  const { token } = theme.useToken();

  // ========== 状态管理 ==========
  const [kbs, setKbs] = useState<KnowledgeBaseItem[]>([]);
  const [kbsLoaded, setKbsLoaded] = useState(false);
  const [currentKbDocs, setCurrentKbDocs] = useState<DocumentItem[]>([]);

  const { currentKbId, setCurrentKbId, token: authToken, userInfo, themeMode, localSettings } = useAppStore();

  const [searchParams] = useSearchParams();
  const kbIdParam = searchParams.get('kbId');

    const [historyList, setHistoryList] = useState<ConversationSessionItem[]>([]);
  const [activeHistoryId, setActiveHistoryId] = useState<string | null>(null);
    const [historyLoading, setHistoryLoading] = useState(false);

  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
      if (kbIdParam) {
          setCurrentKbId(kbIdParam);
      }
  }, [kbIdParam, setCurrentKbId]);

  const currentKb = useMemo(() => kbs.find(kb => String(kb.id) === String(currentKbId)), [kbs, currentKbId]);

  const { strategy, endpoint } = useMemo(() => {
      if (!currentKb) return { strategy: RAG_STRATEGIES.NAIVE_RAG, endpoint: '/api/chat/rag/naive' };

      const type = normalizeStrategyType(currentKb.indexStrategyType);
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

  const fetchHistorySessions = useCallback(async (kbId: string, silent = false) => {
      if (!kbId) {
          setHistoryList([]);
          setActiveHistoryId(null);
          return;
      }

      if (!silent) {
          setHistoryLoading(true);
      }

      try {
          const res: any = await conversationService.listSessions(kbId, { page: 0, size: 50 });
          if (res.code === 200) {
              const sessions: ConversationSessionItem[] = Array.isArray(res.data?.content) ? res.data.content : [];
              setHistoryList(sessions);
              setActiveHistoryId(prev => {
                  if (!prev) return prev;
                  return sessions.some(item => item.sessionId === prev) ? prev : null;
              });
          }
      } catch (e) {
          if (!silent) {
              message.error('历史会话加载失败');
          }
      } finally {
          if (!silent) {
              setHistoryLoading(false);
          }
      }
    }, []);

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

  // Cancel pending RAF on unmount
  useEffect(() => {
    return () => {
      if (rafScrollRef.current !== null) {
        cancelAnimationFrame(rafScrollRef.current);
      }
    };
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
          } finally {
              setKbsLoaded(true);
          }
      };
      fetchKbs();
  }, []);

  useEffect(() => {
      if (!currentKbId) {
          setHistoryList([]);
          setActiveHistoryId(null);
          return;
      }
      fetchHistorySessions(String(currentKbId));
  }, [currentKbId, fetchHistorySessions]);

  const wasRequestingRef = useRef(false);
  useEffect(() => {
      if (wasRequestingRef.current && !isRequesting && currentKbId) {
          fetchHistorySessions(String(currentKbId), true);
      }
      wasRequestingRef.current = isRequesting;
  }, [isRequesting, currentKbId, fetchHistorySessions]);

  // 当知识库切换且策略为 HiSem-SADP 时，预加载文档列表用于文件类型校验
  useEffect(() => {
      if (!currentKbId || strategy !== RAG_STRATEGIES.HISEM_RAG) {
          setCurrentKbDocs([]);
          return;
      }
      documentService.listByKb(String(currentKbId), { page: 1, size: 100 })
          .then((res: any) => {
              if (res.code === 200) {
                  const data = Array.isArray(res.data) ? res.data
                      : (res.data?.records ?? res.data?.list ?? []);
                  setCurrentKbDocs(data);
              }
          })
          .catch(() => setCurrentKbDocs([]));
  }, [currentKbId, strategy]);

  const handleRequest = (text: string) => {
      if (!currentKbId || !currentKb) {
          message.warning('请先选择知识库');
          return;
      }

      // HiSem-SADP 仅支持 Markdown 文件，如有非 Markdown 文档则阻止请求
      // 注意：浏览器上传 .md 文件时 fileType 可能为 application/octet-stream，
      // 因此使用文件名后缀（而非 MIME 类型）来判断。
      if (strategy === RAG_STRATEGIES.HISEM_RAG && currentKbDocs.length > 0) {
          const hasNonMarkdown = currentKbDocs.some(
              d => !d.filename?.toLowerCase().endsWith('.md')
          );
          if (hasNonMarkdown) {
              Modal.warning({
                  title: 'HiSem-SADP 不支持当前文件类型',
                  content: 'HiSem-SADP 方法仅支持 Markdown (.md) 格式的文档。当前知识库包含不受支持的文件类型，请改用"普通 RAG"方法，或上传 Markdown 格式文档并重建索引。',
                  okText: '知道了',
              });
              return;
          }
      }

      const ragParams = form.getFieldsValue();

      const sessionId = activeHistoryId || createSessionId();
      if (!activeHistoryId) {
          setActiveHistoryId(sessionId);
      }

      const historyMessages = messages.map(m => ({
          role: m.message.role === 'user' ? 'user' : 'ai',
          content: m.message.content,
          id: m.id
      }));

      const requestBody: any = {
          kbId: currentKbId,
          question: text,
          sessionId,
          historyWindow: 8,
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
      if (isRequesting) {
          abort();
      }
      setMessages([]);
      setActiveHistoryId(createSessionId());
      message.success('已开启新对话');
  };

  const handleHistoryClick = async (sessionId: string) => {
      if (!currentKbId) {
          message.warning('知识库尚未加载完成，请稍后再试');
          return;
      }

      if (isRequesting) {
          abort();
      }
      setActiveHistoryId(sessionId);
      setMessages([]);
      try {
          const res: any = await conversationService.getSessionDetail(String(currentKbId), sessionId);
          if (res.code !== 200) {
              message.error(res.message || '加载历史对话失败');
              return;
          }

          const detail = res.data as ConversationSessionDetail;
          const restoredMessages: any[] = [];
          detail.messages?.forEach((msg, index) => {
              if (msg.query) {
                  restoredMessages.push({
                      id: `${msg.id || index}-u`,
                      status: 'success',
                      message: { role: 'user', content: msg.query }
                  });
              }
              if (msg.response) {
                  restoredMessages.push({
                      id: `${msg.id || index}-a`,
                      status: 'success',
                      message: { role: 'assistant', content: msg.response }
                  });
              }
          });

          setMessages(restoredMessages as any);
      } catch (e) {
          message.error('加载历史对话失败');
      }
  };

  const items: GetProp<typeof Bubble.List, 'items'> = useMemo(() => {
    // Only exclude the last message from Bubble.List when it is the actively
    // streaming *assistant* message (rendered separately below to avoid
    // rebuilding the entire list on every SSE chunk).
    // If the last message is still the user's own message (waiting for the
    // first assistant chunk), keep it in the list so it appears immediately.
    const lastMsg = messages.length > 0 ? messages[messages.length - 1] : null;
    const lastIsAssistantStreaming =
      isRequesting &&
      lastMsg !== null &&
      (lastMsg.message as unknown as ExtendedMessageContent).role !== 'user';

    const msgsToRender = lastIsAssistantStreaming
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
                footer: (extendedMsg.retrievalTree?.length || extendedMsg.references?.length || extendedMsg.tokenUsage) ? (
                    // Wrap in same maxWidth as the bubble so tree/refs don't overflow wider
                    <div style={{ maxWidth: '88%' }}>
                        {extendedMsg.retrievalTree && extendedMsg.retrievalTree.length > 0 && (
                            <RetrievalTreeViewer treeRoots={extendedMsg.retrievalTree} />
                        )}
                        {extendedMsg.references && extendedMsg.references.length > 0 && (
                            <ReferenceViewer references={extendedMsg.references} />
                        )}
                        {extendedMsg.tokenUsage && (
                            <TokenUsagePanel tokenUsage={extendedMsg.tokenUsage} />
                        )}
                    </div>
                ) : undefined,
                // 用户气泡：主题色调浅底
                ...(extendedMsg.role === 'user' ? {
                    styles: {
                        content: {
                            background: `${token.colorPrimary}14`,
                            border: `1px solid ${token.colorPrimary}2E`,
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

  // 按分组整理历史对话
  const groupedHistory = useMemo(() => {
        const groups: Record<string, ConversationSessionItem[]> = {};
    historyList.forEach(item => {
            const group = getDateGroup(item.lastActiveAt);
            if (!groups[group]) groups[group] = [];
            groups[group].push(item);
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
            config={MD_CONFIG}
            components={MD_COMPONENTS}
            // debug={import.meta.env.DEV}
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
                <Layout className="chat-page-layout" style={{ height: '100%', background: token.colorBgContainer }}>

        {/* ==================== 左侧边栏：历史对话列表 ==================== */}
        <Sider
            width={240}
            className="chat-sidebar-left chat-side-panel chat-side-panel--history"
            style={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column'
            }}
        >
            {/* 新建对话按钮（顶部） */}
            <div style={{ padding: '12px 12px 8px', borderBottom: `1px solid ${token.colorBorderSecondary}` }}>
                <Button
                    type="primary"
                    block
                    className="chat-new-conversation-btn"
                    aria-label="新建聊天会话"
                    icon={<PlusOutlined />}
                    onClick={handleNewChat}
                    style={{
                        height: 36,
                        borderRadius: 8,
                    }}
                >
                    新建对话
                </Button>
            </div>

            {/* 历史对话分组列表 */}
            <div style={{ flex: 1, overflowY: 'auto', padding: '8px 0' }}>
                {historyLoading && (
                    <div style={{ padding: '12px 14px', fontSize: 12, color: token.colorTextSecondary }}>
                        <LoadingOutlined style={{ marginRight: 6 }} />
                        加载历史会话中...
                    </div>
                )}
                {!historyLoading && historyList.length === 0 && (
                    <div style={{ padding: '12px 14px', fontSize: 12, color: token.colorTextSecondary }}>
                        <div style={{ marginBottom: 8 }}>暂无历史会话</div>
                        <Button type="link" size="small" style={{ padding: 0 }} onClick={handleNewChat}>
                            开始第一轮提问
                        </Button>
                    </div>
                )}
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
                                key={item.sessionId}
                                className={`chat-history-item ${activeHistoryId === item.sessionId ? 'chat-history-item--active' : ''}`}
                                style={{
                                    padding: '8px 12px',
                                    margin: '1px 6px',
                                    borderRadius: 8,
                                    background: activeHistoryId === item.sessionId
                                        ? '#e6f4ff'
                                        : 'transparent',
                                    borderLeft: activeHistoryId === item.sessionId
                                        ? `2px solid ${token.colorPrimary}`
                                        : '2px solid transparent',
                                    transition: 'background-color 0.2s ease, border-left-color 0.2s ease',
                                    position: 'relative',
                                }}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                                    <button
                                        type="button"
                                        className="chat-history-trigger"
                                        aria-label={`打开历史会话 ${item.title || item.lastQuestion || '新对话'}`}
                                        onClick={() => handleHistoryClick(item.sessionId)}
                                    >
                                        <MessageOutlined style={{ fontSize: 12, color: token.colorTextTertiary, flexShrink: 0 }} />
                                        <Text
                                            ellipsis
                                            style={{
                                                fontSize: 13,
                                                flex: 1,
                                                minWidth: 0,
                                                color: activeHistoryId === item.sessionId ? token.colorPrimary : token.colorText,
                                                fontWeight: activeHistoryId === item.sessionId ? 500 : 400,
                                            }}
                                        >
                                            {item.title || item.lastQuestion || '新对话'}
                                        </Text>
                                    </button>
                                    {/* hover 时显示删除按钮 */}
                                    <div
                                        className="chat-history-actions"
                                        style={{ flexShrink: 0, marginLeft: 4 }}
                                    >
                                        <Popconfirm
                                            title="确定删除该对话吗？"
                                            onConfirm={async () => {
                                                if (!currentKbId) return;
                                                try {
                                                    const res: any = await conversationService.deleteSession(String(currentKbId), item.sessionId);
                                                    if (res.code === 200) {
                                                        message.success('删除成功');
                                                        setHistoryList(prev => prev.filter(h => h.sessionId !== item.sessionId));
                                                        if (activeHistoryId === item.sessionId) {
                                                            setActiveHistoryId(null);
                                                            setMessages([]);
                                                        }
                                                    } else {
                                                        message.error(res.message || '删除失败');
                                                    }
                                                } catch (e) {
                                                    message.error('删除失败');
                                                }
                                            }}
                                            okText="确定"
                                            cancelText="取消"
                                        >
                                            <Button
                                                type="text"
                                                size="small"
                                                aria-label="删除历史会话"
                                                icon={<DeleteOutlined style={{ fontSize: 12 }} />}
                                            />
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
        <Content className="chat-main-content" style={{ display: 'flex', flexDirection: 'column', height: '100%', minWidth: 0, background: '#ffffff' }}>
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
                        minHeight: '68%',
                        gap: 28,
                        paddingTop: 16,
                    }}>
                        {/* Logo + 欢迎语 */}
                        <div style={{ textAlign: 'center' }}>
                            <div style={{
                                width: 56,
                                height: 56,
                                borderRadius: 16,
                                background: `${token.colorPrimary}1A`,
                                border: `1px solid ${token.colorPrimary}33`,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                margin: '0 auto 16px',
                            }}>
                                <RobotOutlined style={{ fontSize: 24, color: token.colorPrimary }} />
                            </div>
                            <Title level={3} style={{ margin: 0, fontWeight: 700, letterSpacing: '-0.01em' }}>
                                你好，我是 SmartRAG
                            </Title>
                            <Text style={{ color: token.colorTextSecondary, fontSize: 15, marginTop: 8, display: 'block' }}>
                                {currentKb ? `正在使用「${currentKb.name}」知识库` : '请在右侧选择一个知识库开始对话'}
                            </Text>
                        </div>

                        {/* 建议问题卡片 */}
                        {currentKb && (
                            <StaggerContainer style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12, width: '100%', maxWidth: 620 }}>
                                {SUGGESTION_QUESTIONS.map((q, i) => (
                                    <StaggerItem key={i}>
                                        <button
                                            type="button"
                                            className="suggestion-card"
                                            onClick={() => handleRequest(q.text)}
                                            disabled={isRequesting}
                                            aria-label={`使用建议问题：${q.text}`}
                                            style={{ width: '100%', textAlign: 'left' }}
                                        >
                                            <div className="suggestion-card-content" style={{ color: token.colorText }}>
                                                <span
                                                    className="suggestion-card-icon"
                                                    style={{
                                                        background: `${token.colorPrimary}14`,
                                                        color: token.colorPrimary,
                                                    }}
                                                >
                                                    {q.icon}
                                                </span>
                                                <span>{q.text}</span>
                                            </div>
                                        </button>
                                    </StaggerItem>
                                ))}
                            </StaggerContainer>
                        )}
                    </div>
                ) : (
                    <>
                        {/* 历史消息气泡列表 — 只在流式结束后更新，不在每个chunk时重建 */}
                        {/* autoScroll={false}: 禁用内部 column-reverse 自动滚动，由外层 scrollRef 统一管理滚动 */}
                        {/* style={{ maxHeight: 'none' }}: 移除默认 maxHeight:100% 约束，让列表自由增长，避免与流式消息div发生位置重叠 */}
                        <Bubble.List
                            items={items}
                            autoScroll={false}
                            style={{ maxHeight: 'none' }}
                            styles={{
                                bubble: { maxWidth: '88%' }
                            }}
                            role={bubbleRole}
                        />
                        {/* 正在流式输出的消息 — 用 Bubble 组件渲染，保持与历史消息一致的气泡样式 */}
                        {streamingMessage && (
                            <Bubble
                                placement="start"
                                variant="shadow"
                                style={{ maxWidth: '88%', marginTop: 8, marginBottom: 8 }}
                                avatar={<Avatar icon={<RobotOutlined />} style={{ background: token.colorPrimary }} />}
                                header={
                                    streamingMessage.extendedMsg.thoughts && streamingMessage.extendedMsg.thoughts.length > 0
                                        ? <AnimatedThoughtChain items={streamingMessage.extendedMsg.thoughts} />
                                        : undefined
                                }
                                content={streamingMessage.extendedMsg.content || ''}
                                contentRender={(content) => {
                                    // 无文本内容时显示加载指示器
                                    if (!content) {
                                        const thoughts = streamingMessage.extendedMsg.thoughts ?? [];
                                        const allThoughtsDone = thoughts.length > 0 && thoughts.every(t => t.status === 'success');
                                        return (
                                            <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: token.colorTextSecondary, fontSize: 13 }}>
                                                <LoadingOutlined spin style={{ color: token.colorPrimary }} />
                                                <span>{allThoughtsDone ? '生成回答中...' : '思考中...'}</span>
                                            </div>
                                        );
                                    }
                                    // 有内容时渲染流式 Markdown
                                    return (
                                        <XMarkdown
                                            className={themeMode === 'dark' ? 'x-markdown-dark' : 'x-markdown-light'}
                                            config={MD_CONFIG}
                                            components={MD_COMPONENTS}
                                            // debug={import.meta.env.DEV}
                                            streaming={{
                                                hasNextChunk: streamingMessage.status === 'updating' || streamingMessage.status === 'loading',
                                                enableAnimation: true,
                                                animationConfig: { fadeDuration: 80 },
                                            }}
                                        >
                                            {content}
                                        </XMarkdown>
                                    );
                                }}
                                footer={
                                    (streamingMessage.extendedMsg.retrievalTree?.length || streamingMessage.extendedMsg.references?.length || streamingMessage.extendedMsg.tokenUsage)
                                        ? (
                                            <div style={{ maxWidth: '88%' }}>
                                                {streamingMessage.extendedMsg.retrievalTree && streamingMessage.extendedMsg.retrievalTree.length > 0 && (
                                                    <RetrievalTreeViewer treeRoots={streamingMessage.extendedMsg.retrievalTree} />
                                                )}
                                                {streamingMessage.extendedMsg.references && streamingMessage.extendedMsg.references.length > 0 && (
                                                    <ReferenceViewer references={streamingMessage.extendedMsg.references} />
                                                )}
                                                {streamingMessage.extendedMsg.tokenUsage && (
                                                    <TokenUsagePanel tokenUsage={streamingMessage.extendedMsg.tokenUsage} />
                                                )}
                                            </div>
                                        )
                                        : undefined
                                }
                            />
                        )}
                    </>
                )}
            </div>
            {/* 输入框区域 */}
            <div className="chat-input-bar" style={{ padding: '12px 20px 16px', flexShrink: 0 }}>
                <Sender
                    className="chat-sender-root"
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
            className="chat-sidebar-right chat-side-panel chat-side-panel--config"
            style={{
                height: '100%'
            }}
        >
            <SlideInUp style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                <div style={{ flex: 1, overflowY: 'auto', padding: '16px 16px 0' }}>

                    {/* 知识库选择 */}
                    <div style={{ marginBottom: 20 }}>
                        <div style={{
                            fontSize: 11,
                            fontWeight: 700,
                            letterSpacing: '0.06em',
                            textTransform: 'uppercase',
                            color: token.colorTextSecondary,
                            marginBottom: 8,
                        }}>
                            当前知识库
                        </div>
                        <Select
                            style={{ width: '100%' }}
                            aria-label="选择用于问答的知识库"
                            value={kbsLoaded && kbs.some(kb => kb.id === currentKbId) ? currentKbId : undefined}
                            onChange={setCurrentKbId}
                            options={kbs.map(kb => ({ label: kb.name, value: kb.id }))}
                            placeholder={kbsLoaded ? '选择知识库' : '加载中...'}
                            loading={!kbsLoaded}
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
                                            fontSize: 11,
                                            fontWeight: 700,
                                            letterSpacing: '0.06em',
                                            textTransform: 'uppercase',
                                            color: token.colorTextSecondary,
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
                                            fontSize: 11,
                                            fontWeight: 700,
                                            letterSpacing: '0.06em',
                                            textTransform: 'uppercase',
                                            color: token.colorTextSecondary,
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
                                            fontSize: 11,
                                            fontWeight: 700,
                                            letterSpacing: '0.06em',
                                            textTransform: 'uppercase',
                                            color: token.colorTextSecondary,
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
                    <Button block type="default" className="chat-clear-btn" aria-label="清空当前对话消息" icon={<ClearOutlined />} onClick={() => setMessages([])} size="small">
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
 * - 已接入后端会话接口
 * - 支持：加载会话列表、按会话回放、删除会话
 * - 请求体已携带 sessionId，回答完成后由后端持久化
 *
 * 【流式渲染优化】
 * - SmartRAGChatProvider 处理 SSE 事件流
 * - 支持的事件类型：thought（执行流程）、ref（参考文档）、message（流式文本）、done（完成）
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
