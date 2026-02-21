/**
 * ========================================
 * SmartDoc RAG 对话界面
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
 * - 数据流：用户输入 → SmartDocChatProvider → SSE 流式响应 → 实时渲染
 * - 状态管理：使用 Zustand 全局状态（useAppStore）管理用户信息、主题、知识库 ID
 * 
 * 【技术栈】
 * - UI 框架：Ant Design + Ant Design X（对话组件）
 * - 流式渲染：useXChat Hook + SSE（Server-Sent Events）
 * - Markdown 渲染：XMarkdown + KaTeX（数学公式）+ Mermaid（图表）
 * - 动画：Framer Motion（页面过渡动画）
 */

import React, { useState, useEffect, useMemo, useRef } from 'react';
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
} from '@ant-design/icons';
import { Layout, Select, Button, Space, Typography, theme, Form, Slider, Switch, Avatar, message, GetProp, Tooltip, Popconfirm } from 'antd';
import { useSearchParams } from 'react-router-dom';
import { kbService } from '../../services/kbService';
import { modelService } from '../../services/modelService';
import { KnowledgeBaseItem, ThoughtItem, ReferenceItem } from '../../types';
import { useAppStore } from '../../store/useAppStore';
import { SmartDocChatProvider } from '../../utils/SmartRagChatProvider';
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
  // 从 className 中提取语言类型（如 language-javascript）
  const lang = className?.match(/language-(\w+)/)?.[1] || '';

  if (typeof children !== 'string') return null;
  // Mermaid 图表特殊处理
  if (lang === 'mermaid') {
    return <Mermaid>{children}</Mermaid>;
  }
  // 其他语言使用高亮插件
  return <HighlightCode lang={lang}>{children}</HighlightCode>;
};

/**
 * Markdown 组件映射
 * 用于自定义 Markdown 元素的渲染方式
 */
const MD_COMPONENTS = {
  code: Code,  // 代码块使用自定义的 Code 组件
};

/**
 * 模拟历史对话数据
 * TODO: 后续需要从后端 API 获取真实的历史对话记录
 * 包括：对话 ID、标题（首句摘要）、创建时间等
 */
const MOCK_HISTORY = [
    { id: '1', title: '关于 RAG 的原理', date: '2023-11-29' },
    { id: '2', title: '如何优化检索效果', date: '2023-11-28' },
    { id: '3', title: '向量数据库对比', date: '2023-11-27' },
    { id: '4', title: 'LangChain 实践', date: '2023-11-26' },
    { id: '5', title: '大模型微调指南', date: '2023-11-25' },
];

const ChatPage: React.FC = () => {
  // ========== Ant Design 主题 Token ==========
  const { token } = theme.useToken();
  
  // ========== 状态管理 ==========
  // 知识库列表
  const [kbs, setKbs] = useState<KnowledgeBaseItem[]>([]);
  
  // 全局状态：当前知识库 ID、用户信息、主题模式、用户设置等
  const { currentKbId, setCurrentKbId, token: authToken, userInfo, themeMode, localSettings } = useAppStore();
  
  // URL 参数：支持通过 ?kbId=xxx 直接打开指定知识库的对话
  const [searchParams] = useSearchParams();
  const kbIdParam = searchParams.get('kbId');
  
  // 历史对话列表和当前激活的历史对话
  const [historyList, setHistoryList] = useState(MOCK_HISTORY);
  const [activeHistoryId, setActiveHistoryId] = useState<string | null>(null);
  
  // 消息列表滚动容器的引用（用于自动滚动到底部）
  const scrollRef = useRef<HTMLDivElement>(null);

  /**
   * 处理 URL 参数中的知识库 ID
   * 允许通过链接直接跳转到指定知识库的对话页面
   */
  useEffect(() => {
      if (kbIdParam) {
          setCurrentKbId(kbIdParam);
      }
  }, [kbIdParam, setCurrentKbId]);

  /**
   * 当前选中的知识库对象
   * 从知识库列表中找到与 currentKbId 匹配的知识库
   */
  const currentKb = useMemo(() => kbs.find(kb => String(kb.id) === String(currentKbId)), [kbs, currentKbId]);

  /**
   * 根据知识库的索引策略，动态确定 RAG 策略和后端接口地址
   * - NAIVE_RAG → /api/chat/rag/naive
   * - HISEM_RAG → /api/chat/rag/hisem
   * - HISEM_RAG_FAST → /api/chat/rag/hisem-fast
   */
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

  // ========== 参数配置表单 ==========
  const [form] = Form.useForm();  // RAG 参数配置表单实例
  const [input, setInput] = useState('');  // 用户输入框内容
  
  // ========== 模型列表 ==========
  // 从后端获取的可用模型列表
  const [llmModels, setLlmModels] = useState<string[]>([]);      // 大语言模型列表
  const [rerankModels, setRerankModels] = useState<string[]>([]); // 重排序模型列表
  const [modelsLoaded, setModelsLoaded] = useState(false);        // 模型是否加载完成
  
  // 监听表单中的 enable_rerank 字段，用于动态显示/隐藏 rerank 模型选择器
  const enableRerank = Form.useWatch('enable_rerank', form);

  /**
   * 组件挂载时加载可用的模型列表
   * 包括：LLM 模型（用于生成回答）和 Rerank 模型（用于重排序检索结果）
   */
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

  /**
   * 验证和自动填充表单中的模型选择
   * 1. 如果当前选择的模型不在可用列表中，尝试使用默认模型或清空
   * 2. 如果表单为空，自动填充用户设置中的默认模型
   */
  useEffect(() => {
      if (modelsLoaded) {
          const currentValues = form.getFieldsValue();
          
          // Validate LLM Model
          if (currentValues.llmModelId && llmModels.length > 0 && !llmModels.includes(currentValues.llmModelId)) {
              if (localSettings?.defaultModel && llmModels.includes(localSettings.defaultModel)) {
                  form.setFieldValue('llmModelId', localSettings.defaultModel);
              } else {
                  form.setFieldValue('llmModelId', undefined);
              }
          } else if (!currentValues.llmModelId && localSettings?.defaultModel && llmModels.includes(localSettings.defaultModel)) {
              form.setFieldValue('llmModelId', localSettings.defaultModel);
          }

          // Validate Rerank Model
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

  /**
   * 当 RAG 策略变更时，重置参数配置表单
   * 不同策略有不同的参数要求（如 Naive RAG 有 topK，HiSem RAG 有 maxTopK）
   * 重置后自动填充用户的默认模型设置
   */
  useEffect(() => {
      if (strategy) {
          form.resetFields();
          // 设置默认模型值
          if (localSettings?.defaultModel && llmModels.includes(localSettings.defaultModel)) {
              form.setFieldValue('llmModelId', localSettings.defaultModel);
          }
          if (localSettings?.defaultRerank && rerankModels.includes(localSettings.defaultRerank)) {
              form.setFieldValue('rerankModelId', localSettings.defaultRerank);
          }
      }
  }, [strategy, form, localSettings, llmModels, rerankModels]);


  /**
   * 初始化 SmartDocChatProvider
   * 负责与后端建立 SSE 连接，处理流式响应
   * - authToken: 用户认证令牌
   * - endpoint: 根据 RAG 策略动态确定的接口地址
   */
  const provider = useMemo(() => {
    return new SmartDocChatProvider(authToken || undefined, endpoint);
  }, [authToken, endpoint]);

  /**
   * 使用 Ant Design X 的 useXChat Hook
   * 提供：
   * - messages: 消息列表（包含用户消息和 AI 回复）
   * - onRequest: 发送消息到后端
   * - isRequesting: 是否正在请求中
   * - setMessages: 手动设置消息列表（用于清空或加载历史）
   * - abort: 终止当前请求
   */
  const { messages, onRequest, isRequesting, setMessages, abort } = useXChat({
    provider,
  });

  /**
   * 自动滚动到消息列表底部
   * 当新消息到达时，确保用户能看到最新的内容
   */
  useEffect(() => {
      if (scrollRef.current) {
          scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
      }
  }, [messages]);

  /**
   * 获取用户的知识库列表
   * 如果用户还没有选择知识库，自动选中第一个
   */
  useEffect(() => {
      const fetchKbs = async () => {
          try {
            const res: any = await kbService.list({});
            if (res.code === 200) {
                setKbs(res.data);
                // 自动选择第一个知识库
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

  /**
   * 处理用户发送消息
   * 
   * 数据流程：
   * 1. 验证是否选择了知识库
   * 2. 获取表单中的 RAG 参数配置
   * 3. 构造历史消息列表（用于上下文对话）
   * 4. 根据不同的 RAG 策略构造请求体
   * 5. 调用 onRequest 发送到后端
   * 6. 清空输入框
   * 
   * 后端接口：
   * - /api/chat/rag/naive (NaiveRagChatRequest)
   * - /api/chat/rag/hisem (HisemRagChatRequest)
   * - /api/chat/rag/hisem-fast (HisemRagChatRequest)
   */
  const handleRequest = (text: string) => {
      if (!currentKbId || !currentKb) {
          message.warning('请先选择知识库');
          return;
      }

      // 获取用户配置的 RAG 参数
      const ragParams = form.getFieldsValue();
      
      // 构造历史消息（用于上下文对话）
      // useXChat 的 messages 包含了之前的所有消息
      const historyMessages = messages.map(m => ({ 
          role: m.message.role === 'user' ? 'user' : 'ai', 
          content: m.message.content, 
          id: m.id 
      }));

      // 构造请求体（基础字段）
      const requestBody: any = {
          kbId: currentKbId,
          question: text,
          embeddingModelId: currentKb.embeddingModelId,
          llmModelId: ragParams.llmModelId,
          rerankModelId: ragParams.rerankModelId,
          // 通用的布尔开关
          enableQueryRewrite: ragParams.enableQueryRewrite,           // 查询重写
          enableQueryDecomposition: ragParams.enableQueryDecomposition, // 查询分解
          enableIntentRecognition: ragParams.enableIntentRecognition,  // 意图识别
          enableHyde: ragParams.enableHyde,                           // Hyde 模式
      };

      // 根据不同的 RAG 策略添加特定参数
      if (strategy === RAG_STRATEGIES.NAIVE_RAG) {
          requestBody.topK = ragParams.topK;           // 检索前 K 个结果
          requestBody.threshold = ragParams.threshold;  // 相似度阈值
      } else if (strategy === RAG_STRATEGIES.HISEM_RAG || strategy === RAG_STRATEGIES.HISEM_RAG_FAST) {
          requestBody.maxTopK = ragParams.maxTopK;     // 最大检索结果数
      }

      // 发送请求到后端（通过 SmartDocChatProvider）
      onRequest({
          messages: [
              ...historyMessages,
              { role: 'user', content: text, id: Date.now().toString() }
          ] as any,
          kbId: currentKbId,
          ragMethod: strategy,
          ragParams: requestBody
      });
      
      // 清空输入框
      setInput('');
  };

  /**
   * 开始新的对话
   * 清空当前消息列表和激活的历史对话
   */
  const handleNewChat = () => {
      setMessages([]);
      setActiveHistoryId(null);
      message.success('已开启新对话');
  };

  /**
   * 点击历史对话项
   * TODO: 当前是模拟数据，后续需要从后端加载真实的对话历史
   */
  const handleHistoryClick = (id: string) => {
      setActiveHistoryId(id);
      // 模拟加载历史
      setMessages([]);
      message.info('加载历史对话...');
      setTimeout(() => {
          // 模拟消息
          setMessages([
              { id: '1', status: 'success', message: { role: 'user', content: '你好，什么是 RAG？' } as any },
              { id: '2', status: 'success', message: { role: 'assistant', content: 'RAG (Retrieval-Augmented Generation) 是一种结合了检索和生成的 AI 技术...' } as any }
          ]);
      }, 500);
  };

  /**
   * 将内部消息格式转换为 Bubble.List 组件所需的数据格式
   * 
   * 关键设计：
   * 1. 过滤空内容的 assistant 消息（只有思考过程还没有生成内容的不显示气泡）
   * 2. 为 user 和 assistant 配置不同的头像、位置、样式
   * 3. 支持思考过程（header）和参考文档（footer）的显示
   */
  const items: GetProp<typeof Bubble.List, 'items'> = useMemo(() => {
    return messages
        .filter((msg) => {
            const extendedMsg = msg.message as unknown as ExtendedMessageContent;
            // 只有当 assistant 消息有实际内容时才显示气泡
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
            };
        });
  }, [messages, token.colorPrimary, userInfo]);

  /**
   * 获取正在处理中的 assistant 消息
   * 
   * 场景：
   * 当 AI 正在执行思考过程（检索、重排序等）但还没有开始生成文本时，
   * 需要单独显示一个“思考过程”卡片，而不是放在消息气泡里。
   * 当生成开始后，思考过程会自动移动到气泡的 header 位置。
   */
  const pendingAssistantMessage = useMemo(() => {
    const lastMsg = messages[messages.length - 1];
    if (!lastMsg) return null;

    const extendedMsg = lastMsg.message as unknown as ExtendedMessageContent;
    // 如果是 assistant 消息，没有内容但有思考过程，则显示独立的思考过程
    if (extendedMsg.role !== 'user' &&
        (!extendedMsg.content || extendedMsg.content.trim() === '') &&
        extendedMsg.thoughts && extendedMsg.thoughts.length > 0) {
      return extendedMsg;
    }
    return null;
  }, [messages]);

  return (
    <XProvider theme={{ token: { colorPrimary: token.colorPrimary } }}>
        {/* ========================================
            三栏布局：左侧历史 | 中间对话 | 右侧配置
            ======================================== */}
        <Layout style={{ height: '100%', background: token.colorBgContainer }}>
        
        {/* ==================== 左侧边栏：历史对话列表 ==================== */}
        <Sider 
            width={260} 
            theme={themeMode === 'dark' ? 'dark' : 'light'} 
            style={{ 
                borderRight: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0', 
                background: token.colorBgContainer,
                height: '100%',
                display: 'flex',
                flexDirection: 'column'
            }}
        >
            <div style={{ padding: 16, borderBottom: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0' }}>
                <Button type="primary" block icon={<PlusOutlined />} onClick={handleNewChat} size="large">
                    新建对话
                </Button>
            </div>
            <div style={{ flex: 1, overflowY: 'auto', padding: '12px 0' }}>
                <div style={{ padding: '0 16px 8px', color: token.colorTextDescription, fontSize: 12 }}>
                    历史对话
                </div>
                <StaggerContainer>
                    {historyList.map((item) => (
                        <StaggerItem key={item.id}>
                            <div
                                className={`cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors duration-200`}
                                style={{
                                    padding: '10px 16px',
                                    background: activeHistoryId === item.id ? (themeMode === 'dark' ? '#1f1f1f' : '#e6f4ff') : 'transparent',
                                    borderLeft: activeHistoryId === item.id ? `3px solid ${token.colorPrimary}` : '3px solid transparent'
                                }}
                                onClick={() => handleHistoryClick(item.id)}
                            >
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', overflow: 'hidden' }}>
                                        <MessageOutlined style={{ marginRight: 8, color: token.colorTextSecondary }} />
                                        <Text ellipsis style={{ maxWidth: 140, color: token.colorText }}>{item.title}</Text>
                                    </div>
                                    {activeHistoryId === item.id && (
                                        <div onClick={(e) => e.stopPropagation()}>
                                            <Popconfirm
                                                title="确定删除该对话吗？"
                                                onConfirm={() => {
                                                    message.success('删除成功');
                                                    setHistoryList(prev => prev.filter(h => h.id !== item.id));
                                                }}
                                                okText="确定"
                                                cancelText="取消"
                                            >
                                                <DeleteOutlined
                                                    className="text-gray-400 hover:text-red-500"
                                                />
                                            </Popconfirm>
                                        </div>
                                    )}
                                    </div>
                                    <div style={{ fontSize: 12, color: token.colorTextDescription, marginLeft: 24, marginTop: 4 }}>
                                        {item.date}
                                    </div>
                                </div>
                            </StaggerItem>
                        ))}
                </StaggerContainer>
            </div>
        </Sider>

        {/* ==================== 中间内容区：对话消息列表 + 输入框 ==================== */}
        <Content style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <FadeIn style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            {/* 消息滚动区域 */}
            <div ref={scrollRef} style={{ flex: 1, overflowY: 'auto', padding: 24 }}>
                {messages.length === 0 ? (
                    /* 空状态：没有消息时显示欢迎界面 */
                    <div style={{ textAlign: 'center', marginTop: 100, color: '#999' }}>
                        <RobotOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                        <p>开始一个新的对话吧</p>
                    </div>
                ) : (
                    <>
                        {/* 消息气泡列表 */}
                        <Bubble.List
                            items={items}
                            styles={{
                                bubble: { maxWidth: '95%' }
                            }}
                            role={{
                                user: {
                                    /* 用户消息的 Markdown 渲染（支持数学公式） */
                                    contentRender: (content: string) => (
                                        <XMarkdown
                                            className={themeMode === 'dark' ? 'x-markdown-dark' : 'x-markdown-light'}
                                            // @ts-ignore
                                            config={{
                                                extensions: [
                                                    ...Latex({
                                                        katexOptions: {
                                                            output: 'html',
                                                            throwOnError: false,
                                                        }
                                                    }),
                                                ]
                                            }}
                                            components={MD_COMPONENTS}
                                        >
                                            {content}
                                        </XMarkdown>
                                    )
                                },
                                assistant: {
                                    /**
                                     * AI 消息的流式 Markdown 渲染
                                     * - 支持数学公式、代码高亮、Mermaid 图表
                                     * - 启用打字机效果（streaming 模式）
                                     */
                                    contentRender: (content: string, { status }: any) => {
                                        if (!content) return null;
                                        return (
                                        <XMarkdown
                                            className={themeMode === 'dark' ? 'x-markdown-dark' : 'x-markdown-light'}
                                            // @ts-ignore
                                            config={{
                                                extensions: [
                                                    ...Latex({
                                                        katexOptions: {
                                                            output: 'html',
                                                            throwOnError: false,
                                                        }
                                                    }),
                                                ]
                                            }}
                                            components={MD_COMPONENTS}
                                            streaming={{
                                                hasNextChunk: status === 'updating' || status === 'loading',
                                                enableAnimation: true,
                                                // 在这里调整流式输出的速度（动画时长），单位毫秒
                                                animationConfig: { fadeDuration: 400 },
                                            }}
                                        >
                                            {content}
                                        </XMarkdown>
                                        );
                                    },
                                },
                            }}
                        />
                        {/* 
                            独立渲染正在处理中的思考过程
                            当 AI 还没有开始生成文本内容时，单独显示思考过程
                            避免显示空白的消息气泡
                        */}
                        {pendingAssistantMessage && (
                            <div style={{ display: 'flex', gap: 12, marginTop: 16 }}>
                                <Avatar icon={<RobotOutlined />} style={{ background: token.colorPrimary, flexShrink: 0 }} />
                                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                                    <AnimatedThoughtChain items={pendingAssistantMessage.thoughts!} />
                                    {/* 当思考过程完成后，显示等待大模型回答的动画 */}
                                    {pendingAssistantMessage.thoughts!.length > 0 &&
                                     pendingAssistantMessage.thoughts![pendingAssistantMessage.thoughts!.length - 1].status === 'success' && (
                                        <div style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: 8,
                                            color: token.colorTextSecondary,
                                            fontSize: 14,
                                            marginTop: 4
                                        }}>
                                            <LoadingOutlined spin style={{ color: token.colorPrimary }} />
                                            <span>思考中...</span>
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}
                    </>
                )}
            </div>
            {/* 输入框区域 */}
            <div style={{ padding: '16px 24px', borderTop: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0' }}>
                {/* 
                    Ant Design X Sender 组件
                    - onSubmit: 发送消息
                    - onCancel: 终止当前请求（点击停止按钮）
                    - loading: 显示加载状态和停止按钮
                */}
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

        <Sider 
            width={350} 
            theme={themeMode === 'dark' ? 'dark' : 'light'} 
            style={{ 
                borderLeft: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0', 
                background: token.colorBgContainer,
                height: '100%'
            }}
        >
            <SlideInUp style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                <div style={{ flex: 1, overflowY: 'auto', padding: 16, paddingRight: 20 }}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
                        <div>
                            {/* 知识库选择器 */}
                            <Title level={5}>当前知识库</Title>
                            <Select
                                style={{ width: '100%' }}
                                value={currentKbId}
                                onChange={setCurrentKbId}
                                options={kbs.map(kb => ({ label: kb.name, value: kb.id }))}
                                placeholder="选择知识库"
                            />
                        </div>
                        
                        <div>
                            {/* 
                                RAG 参数配置表单
                                - 根据知识库的索引策略动态显示不同的参数
                                - key={strategy}: 策略变更时重新挂载表单
                                - 配置项从 ragConfig.ts 中加载
                            */}
                            <Title level={5}>RAG 参数配置</Title>
                            <Form
                                layout="vertical"
                                form={form}
                                key={strategy}
                                initialValues={{
                                    topK: 5,
                                    threshold: 0.0,
                                    maxTopK: 10
                                }}
                            >
                                {/* 根据当前 RAG 策略动态渲染配置项 */}
                                {strategy && getMethodConfig(strategy).searchConfig.map((item: any) => {
                                    if (item.dependency) {
                                        const depValue = item.dependency.field === 'enable_rerank' ? enableRerank : form.getFieldValue(item.dependency.field);
                                        if (depValue !== item.dependency.value) return null;
                                    }

                                    let inputNode = <div />;
                                    // Form initialValues 中已定义的字段不再设置 initialValue
                                    const formInitialKeys = ['topK', 'threshold', 'maxTopK'];
                                    let initialValue = formInitialKeys.includes(item.key) ? undefined : item.defaultValue;

                                    if (item.type === 'select') {
                                        inputNode = (
                                            <Select>
                                                {item.options?.map((opt: any) => (
                                                    <Select.Option key={opt.value} value={opt.value}>{opt.label}</Select.Option>
                                                ))}
                                            </Select>
                                        );
                                    } else if (item.type === 'model_select') {
                                        const models = item.modelType === 'llm' ? llmModels : rerankModels;
                                        // Only set initial value if models are not loaded yet (to avoid flash) or if we are sure it exists
                                        // But useEffect will handle validation.
                                        if (item.modelType === 'llm' && localSettings?.defaultModel) {
                                            initialValue = localSettings.defaultModel;
                                        } else if (item.modelType === 'rerank' && localSettings?.defaultRerank) {
                                            initialValue = localSettings.defaultRerank;
                                        }

                                        inputNode = (
                                            <Select placeholder={`请选择 ${item.label}`}>
                                                {models.map(model => (
                                                    <Select.Option key={model} value={model}>{model}</Select.Option>
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
                                    } else if (item.type === 'switch') {
                                        inputNode = <Switch checkedChildren="开启" unCheckedChildren="关闭" />;
                                    }

                                    return (
                                        <Form.Item 
                                            key={item.key} 
                                            name={item.key} 
                                            label={
                                                <Space>
                                                    {item.label}
                                                    {item.description && (
                                                        <Tooltip title={item.description}>
                                                            <QuestionCircleOutlined />
                                                        </Tooltip>
                                                    )}
                                                </Space>
                                            }
                                            valuePropName={item.type === 'switch' ? 'checked' : 'value'}
                                            initialValue={initialValue}
                                        >
                                            {inputNode}
                                        </Form.Item>
                                    );
                                })}
                            </Form>
                        </div>
                    </div>
                </div>
                
                <div style={{ padding: 16, borderTop: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0' }}>
                    <Button block danger icon={<ClearOutlined />} onClick={() => setMessages([])}>
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
 * - SmartDocChatProvider 处理 SSE 事件流
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
