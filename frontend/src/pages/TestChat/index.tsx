import { useMemo, useRef, useEffect, useState } from 'react';
import {
  Bubble,
  Sender,
  XProvider,
  Prompts,
} from '@ant-design/x';
import { useXChat } from '@ant-design/x-sdk';
import {
  UserOutlined,
  RobotOutlined,
} from '@ant-design/icons';
import { Layout, Space, Avatar, Typography, Divider, GetProp, theme } from 'antd';
import { XMarkdown, type ComponentProps } from '@ant-design/x-markdown';
import HighlightCode from '@ant-design/x-markdown/plugins/HighlightCode';
import Latex from '@ant-design/x-markdown/plugins/Latex';
import Mermaid from '@ant-design/x-markdown/plugins/Mermaid';
import '@ant-design/x-markdown/themes/light.css';
import '@ant-design/x-markdown/themes/dark.css';
import { useAppStore } from '../../store/useAppStore';
import { SmartRAGChatProvider } from '../../utils/SmartRagChatProvider';
import ReferenceViewer from '../../components/ReferenceViewer';
import AnimatedThoughtChain from '../../components/rag/AnimatedThoughtChain';
import { ThoughtItem, ReferenceItem } from '../../types';

const { Content, Sider } = Layout;

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

const MD_COMPONENTS = {
    code: Code,
};

const TestChatPage = () => {
    const { token } = theme.useToken();
    const { userInfo, token: authToken, currentKbId, themeMode } = useAppStore();
    const scrollRef = useRef<HTMLDivElement>(null);
    const [input, setInput] = useState('');

    // 1. Initialize Provider
    const provider = useMemo(() => {
        return new SmartRAGChatProvider(authToken || undefined);
    }, [authToken]);

    // 2. Use XChat Hook
    const { messages, onRequest, isRequesting } = useXChat({
        provider,
    });

    // Auto scroll to bottom
    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [messages]);

    const handleRequest = (content: string) => {
        onRequest({
            messages: [
                { role: 'user', content: content }
            ] as any,
            // Pass context if needed, though TestChat might not have full context controls
            kbId: currentKbId || undefined, 
            ragMethod: 'naive', // Default for test
            ragParams: {}
        });
        setInput('');
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
                variant: extendedMsg.role === 'user' ? 'shadow' : 'outlined',
                avatar: extendedMsg.role === 'user' ? (
                    <Avatar src={userInfo?.avatarUrl || userInfo?.avatar} icon={<UserOutlined />} />
                ) : (
                    <Avatar icon={<RobotOutlined />} style={{ backgroundColor: token.colorPrimary }} />
                ),
                header: extendedMsg.thoughts && extendedMsg.thoughts.length > 0 ? (
                    <AnimatedThoughtChain items={extendedMsg.thoughts} />
                ) : undefined,
                footer: extendedMsg.references && extendedMsg.references.length > 0 ? (
                    <ReferenceViewer references={extendedMsg.references} />
                ) : undefined,
            };
        });
    }, [messages, userInfo]);

    return (
        <XProvider theme={{ token: { colorPrimary: token.colorPrimary } }}>
            <Layout style={{ height: '100%', background: token.colorBgContainer }}>
                <Content style={{ display: 'flex', flexDirection: 'column', height: '100%', minWidth: 0 }}>
                    <div ref={scrollRef} className="chat-messages-scroll" style={{ flex: 1, overflowY: 'auto', padding: 24, minHeight: 0, overscrollBehavior: 'contain' as const }}>
                        {messages.length === 0 ? (
                            <div style={{
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                                justifyContent: 'center',
                                minHeight: '60%',
                                gap: 12,
                                paddingTop: 60,
                                textAlign: 'center',
                            }}>
                                <div style={{
                                    width: 48,
                                    height: 48,
                                    borderRadius: 14,
                                    background: 'rgba(99, 102, 241, 0.10)',
                                    border: '1px solid rgba(99, 102, 241, 0.20)',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    marginBottom: 4,
                                }}>
                                    <RobotOutlined style={{ fontSize: 22, color: token.colorPrimary }} />
                                </div>
                                <Typography.Text style={{ fontSize: 14, color: token.colorTextSecondary }}>
                                    开始一个新的测试对话吧
                                </Typography.Text>
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
                                                        animationConfig: { fadeDuration: 800 },
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
                    <div className="chat-input-bar" style={{ padding: '12px 20px 16px', borderTop: `1px solid ${token.colorBorderSecondary}`, flexShrink: 0, background: token.colorBgContainer }}>
                        <Sender
                            value={input}
                            onChange={setInput}
                            loading={isRequesting}
                            onSubmit={handleRequest}
                            placeholder="输入问题..."
                        />
                    </div>
                </Content>
                <Sider
                    width={300}
                    className="chat-sidebar-right"
                    theme={themeMode === 'dark' ? 'dark' : 'light'}
                    style={{
                        borderLeft: `1px solid ${token.colorBorderSecondary}`,
                        padding: 20,
                        background: token.colorBgContainer
                    }}
                >
                    <Typography.Title level={5} style={{ marginBottom: 6 }}>测试控制台</Typography.Title>
                    <Typography.Paragraph type="secondary">
                        这是一个前端模拟的问答界面，用于测试 UI 交互和流式响应效果。
                    </Typography.Paragraph>
                    <Divider />
                    <Typography.Text strong>预设问题：</Typography.Text>
                    {/* @ts-ignore */}
                    <Space orientation="vertical" style={{ width: '100%', marginTop: 16 }}>
                        <Prompts
                            items={[
                                { key: '1', label: '什么是 RAG 技术？' },
                                { key: '2', label: 'SmartRAG 有什么功能？' },
                            ]}
                            onItemClick={(item) => {
                                handleRequest(item.data.label as string);
                            }}
                        />
                    </Space>
                </Sider>
            </Layout>
        </XProvider>
    );
};

export default TestChatPage;


