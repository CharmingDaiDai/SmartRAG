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
  HistoryOutlined,
  MessageOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import { Layout, Select, Button, Space, Typography, theme, Form, Slider, Switch, Avatar, message, GetProp, InputNumber, Tooltip, Popconfirm } from 'antd';
import { useSearchParams } from 'react-router-dom';
import { kbService } from '../../services/kbService';
import { modelService } from '../../services/modelService';
import { KnowledgeBaseItem, ThoughtItem, ReferenceItem } from '../../types';
import { useAppStore } from '../../store/useAppStore';
import { SmartDocChatProvider } from '../../utils/SmartRagChatProvider';
import ReferenceViewer from '../../components/ReferenceViewer';
import AnimatedThoughtChain from '../../components/rag/AnimatedThoughtChain';
import { getMethodConfig, RAG_METHODS, RAG_STRATEGIES } from '../../config/ragConfig';
import { FadeIn, SlideInUp, StaggerContainer, StaggerItem } from '../../components/common/Motion';

const { Sider, Content } = Layout;
const { Title, Text } = Typography;

interface ExtendedMessageContent {
    role: string;
    content: string;
    thoughts?: ThoughtItem[];
    references?: ReferenceItem[];
}

// Define custom Code component for syntax highlighting
const Code: React.FC<ComponentProps> = (props) => {
  const { className, children } = props;
  const lang = className?.match(/language-(\w+)/)?.[1] || '';

  if (typeof children !== 'string') return null;
  if (lang === 'mermaid') {
    return <Mermaid>{children}</Mermaid>;
  }
  return <HighlightCode lang={lang}>{children}</HighlightCode>;
};

// Define plugins list (static constant to avoid recreation)
const MD_PLUGINS = [
    Latex({ 
        katexOptions: { 
            output: 'html',
            throwOnError: false,
        } 
    }), 
    Mermaid
];

// Define component mapping (placeholder for custom components)
const MD_COMPONENTS = {
  code: Code,
};

// Mock History Data
const MOCK_HISTORY = [
    { id: '1', title: '关于 RAG 的原理', date: '2023-11-29' },
    { id: '2', title: '如何优化检索效果', date: '2023-11-28' },
    { id: '3', title: '向量数据库对比', date: '2023-11-27' },
    { id: '4', title: 'LangChain 实践', date: '2023-11-26' },
    { id: '5', title: '大模型微调指南', date: '2023-11-25' },
];

const ChatPage: React.FC = () => {
  const { token } = theme.useToken();
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

  // Validate selected models against loaded lists
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

  // Reset form when strategy changes
  useEffect(() => {
      if (strategy) {
          form.resetFields();
          // Set default model values
          if (localSettings?.defaultModel && llmModels.includes(localSettings.defaultModel)) {
              form.setFieldValue('llmModelId', localSettings.defaultModel);
          }
          if (localSettings?.defaultRerank && rerankModels.includes(localSettings.defaultRerank)) {
              form.setFieldValue('rerankModelId', localSettings.defaultRerank);
          }
      }
  }, [strategy, form, localSettings, llmModels, rerankModels]);


  // Initialize Provider
  const provider = useMemo(() => {
    return new SmartDocChatProvider(authToken || undefined, endpoint);
  }, [authToken, endpoint]);

  // Use XChat Hook
  const { messages, onRequest, isRequesting, setMessages, abort } = useXChat({
    provider,
  });

  // Auto scroll to bottom
  useEffect(() => {
      if (scrollRef.current) {
          scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
      }
  }, [messages]);

  // Fetch KBs
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
      
      // Construct history for the backend
      // Note: messages from useXChat includes the previous state
      const historyMessages = messages.map(m => ({ 
          role: m.message.role === 'user' ? 'user' : 'ai', 
          content: m.message.content, 
          id: m.id 
      }));

      // Construct request body based on strategy
      const requestBody: any = {
          kbId: currentKbId,
          question: text,
          embeddingModelId: currentKb.embeddingModelId,
          llmModelId: ragParams.llmModelId,
          rerankModelId: ragParams.rerankModelId,
          // Common boolean flags
          enableQueryRewrite: ragParams.enableQueryRewrite,
          enableQueryDecomposition: ragParams.enableQueryDecomposition,
          enableIntentRecognition: ragParams.enableIntentRecognition,
          enableHyde: ragParams.enableHyde,
      };

      if (strategy === RAG_STRATEGIES.NAIVE_RAG) {
          requestBody.topK = ragParams.topK;
          requestBody.threshold = ragParams.threshold;
      } else if (strategy === RAG_STRATEGIES.HISEM_RAG || strategy === RAG_STRATEGIES.HISEM_FAST_RAG) {
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
      // Mock loading history
      setMessages([]);
      message.info('加载历史对话...');
      setTimeout(() => {
          // Mock messages
          setMessages([
              { id: '1', status: 'success', message: { role: 'user', content: '你好，什么是 RAG？' } as any },
              { id: '2', status: 'success', message: { role: 'assistant', content: 'RAG (Retrieval-Augmented Generation) 是一种结合了检索和生成的 AI 技术...' } as any }
          ]);
      }, 500);
  };

  const items: GetProp<typeof Bubble.List, 'items'> = useMemo(() => {
    return messages.map((msg) => {
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

  return (
    <XProvider theme={{ token: { colorPrimary: token.colorPrimary } }}>
        <Layout style={{ height: '100%', background: token.colorBgContainer }}>
        
        {/* Left Sidebar - History */}
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

        <Content style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <FadeIn style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div ref={scrollRef} style={{ flex: 1, overflowY: 'auto', padding: 24 }}>
                {messages.length === 0 ? (
                    <div style={{ textAlign: 'center', marginTop: 100, color: '#999' }}>
                        <RobotOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                        <p>开始一个新的对话吧</p>
                    </div>
                ) : (
                    <Bubble.List 
                        items={items} 
                        styles={{
                            bubble: { maxWidth: '95%' }
                        }}
                        role={{
                            user: {
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
                )}
            </div>
            <div style={{ padding: '16px 24px', borderTop: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0' }}>
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
                                {/* RAG Method is determined by KB strategy */}
                                
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

export default ChatPage;
