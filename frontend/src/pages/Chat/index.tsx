import React, { useState, useEffect, useMemo } from 'react';
import {
  Bubble,
  Sender,
  XProvider,
} from '@ant-design/x';
import { useXChat } from '@ant-design/x-sdk';
import { XMarkdown, type ComponentProps } from '@ant-design/x-markdown';
import HighlightCode from '@ant-design/x-markdown/plugins/HighlightCode';
import Latex from '@ant-design/x-markdown/plugins/latex';
import Mermaid from '@ant-design/x-markdown/plugins/mermaid';
import '@ant-design/x-markdown/themes/light.css';
import '@ant-design/x-markdown/themes/dark.css';
import {
  UserOutlined,
  RobotOutlined,
  ClearOutlined,
} from '@ant-design/icons';
import { Layout, Select, Button, Space, Typography, theme, Form, Slider, Switch, Avatar, message, GetProp } from 'antd';
import { useSearchParams } from 'react-router-dom';
import { kbService } from '../../services/kbService';
import { KnowledgeBaseItem, ThoughtItem, ReferenceItem } from '../../types';
import { useAppStore } from '../../store/useAppStore';
import { SmartDocChatProvider } from '../../utils/SmartRagChatProvider';
import ReferenceViewer from '../../components/ReferenceViewer';
import AnimatedThoughtChain from '../../components/rag/AnimatedThoughtChain';

const { Sider, Content } = Layout;
const { Title } = Typography;

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
  return <HighlightCode lang={lang}>{children}</HighlightCode>;
};

// Define plugins list (static constant to avoid recreation)
const MD_PLUGINS = [Latex, Mermaid];

// Define component mapping (placeholder for custom components)
const MD_COMPONENTS = {
  code: Code,
};

const ChatPage: React.FC = () => {
  const { token } = theme.useToken();
  const [kbs, setKbs] = useState<KnowledgeBaseItem[]>([]);
  const { currentKbId, setCurrentKbId, token: authToken, userInfo, themeMode } = useAppStore();
  const [searchParams] = useSearchParams();
  const kbIdParam = searchParams.get('kbId');

  useEffect(() => {
      if (kbIdParam) {
          setCurrentKbId(kbIdParam);
      }
  }, [kbIdParam, setCurrentKbId]);

  const [input, setInput] = useState('');
  
  // RAG Config Form
  const [form] = Form.useForm();

  // Initialize Provider
  const provider = useMemo(() => {
    return new SmartDocChatProvider(authToken || undefined);
  }, [authToken]);

  // Use XChat Hook
  const { messages, onRequest, isRequesting, setMessages } = useXChat({
    provider,
  });

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
      if (!currentKbId) {
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

      onRequest({
          messages: [
              ...historyMessages,
              { role: 'user', content: text, id: Date.now().toString() }
          ] as any,
          kbId: currentKbId,
          ragMethod: ragParams.method,
          ragParams: ragParams
      });
      
      setInput('');
  };

  const items: GetProp<typeof Bubble.List, 'items'> = useMemo(() => {
    return messages.map((msg) => {
        const { message, status, id } = msg;
        const extendedMsg = message as unknown as ExtendedMessageContent;
        
        return {
            key: id,
            loading: status === 'loading' || status === 'updating',
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
        <Content style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
            <div style={{ flex: 1, overflowY: 'auto', padding: 24 }}>
                {messages.length === 0 ? (
                    <div style={{ textAlign: 'center', marginTop: 100, color: '#999' }}>
                        <RobotOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                        <p>开始一个新的对话吧</p>
                    </div>
                ) : (
                    <Bubble.List 
                        items={items} 
                        role={{
                            user: {
                                contentRender: (content: string) => (
                                    <XMarkdown 
                                        className={themeMode === 'dark' ? 'x-markdown-dark' : 'x-markdown-light'}
                                        // @ts-ignore
                                        plugins={MD_PLUGINS}
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
                                            plugins={MD_PLUGINS}
                                            components={MD_COMPONENTS}
                                            streaming={{
                                                hasNextChunk: status === 'updating' || status === 'loading',
                                                enableAnimation: true,
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
                    placeholder="输入问题，Shift + Enter 换行"
                />
            </div>
        </Content>

        <Sider 
            width={320} 
            theme={themeMode === 'dark' ? 'dark' : 'light'} 
            style={{ 
                borderLeft: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0', 
                padding: 16, 
                display: 'flex', 
                flexDirection: 'column',
                background: token.colorBgContainer
            }}
        >
            <div style={{ flex: 1, overflowY: 'auto' }}>
                <Space direction="vertical" style={{ width: '100%' }} size="large">
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
                            initialValues={{ 
                                topK: 5, 
                                threshold: 0.7, 
                                method: 'naive',
                                rerank: false 
                            }}
                        >
                            <Form.Item name="method" label="RAG 方法">
                                <Select options={[
                                    { label: 'Naive RAG', value: 'naive' }, 
                                    { label: 'HiSem RAG', value: 'hisem' },
                                    { label: 'Graph RAG', value: 'graph' }
                                ]} />
                            </Form.Item>
                            <Form.Item name="topK" label="Top K (检索数量)">
                                <Slider min={1} max={20} marks={{ 1: '1', 10: '10', 20: '20' }} />
                            </Form.Item>
                            <Form.Item name="threshold" label="相似度阈值">
                                <Slider min={0} max={1} step={0.1} marks={{ 0: '0', 0.5: '0.5', 1: '1' }} />
                            </Form.Item>
                            <Form.Item name="rerank" label="开启重排序" valuePropName="checked">
                                <Switch checkedChildren="开启" unCheckedChildren="关闭" />
                            </Form.Item>
                        </Form>
                    </div>
                </Space>
            </div>
            
            <div style={{ marginTop: 16 }}>
                <Button block danger icon={<ClearOutlined />} onClick={() => setMessages([])}>
                    清空对话
                </Button>
            </div>
        </Sider>
        </Layout>
    </XProvider>
  );
};

export default ChatPage;
