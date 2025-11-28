import { useMemo, useRef, useEffect } from 'react';
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
import Latex from '@ant-design/x-markdown/plugins/latex';
import Mermaid from '@ant-design/x-markdown/plugins/mermaid';
import '@ant-design/x-markdown/themes/light.css';
import '@ant-design/x-markdown/themes/dark.css';
import { useAppStore } from '../../store/useAppStore';
import { SmartDocChatProvider } from '../../utils/SmartDocChatProvider';
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
  return <HighlightCode lang={lang}>{children}</HighlightCode>;
};

const MD_PLUGINS = [Latex, Mermaid];
const MD_COMPONENTS = {
    code: Code,
};

const TestChatPage = () => {
    const { token } = theme.useToken();
    const { userInfo, token: authToken, currentKbId, themeMode } = useAppStore();
    const scrollRef = useRef<HTMLDivElement>(null);

    // 1. Initialize Provider
    const provider = useMemo(() => {
        return new SmartDocChatProvider(authToken || undefined);
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
                    <Avatar icon={<RobotOutlined />} style={{ backgroundColor: '#1677ff' }} />
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
                <Content style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                    <div ref={scrollRef} style={{ flex: 1, overflowY: 'auto', padding: 24 }}>
                        {messages.length === 0 ? (
                            <div style={{ textAlign: 'center', marginTop: 100, color: '#999' }}>
                                <RobotOutlined style={{ fontSize: 48, marginBottom: 16 }} />
                                <p>开始一个新的测试对话吧</p>
                            </div>
                        ) : (
                            <Bubble.List 
                                items={items} 
                                role={{
                                    assistant: {
                                        contentRender: (content: string, { status }: any) => (
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
                                        ),
                                    },
                                }}
                            />
                        )}
                    </div>
                    <div style={{ padding: '16px 24px', borderTop: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0' }}>
                        <Sender
                            loading={isRequesting}
                            onSubmit={handleRequest}
                            placeholder="输入问题..."
                        />
                    </div>
                </Content>
                <Sider 
                    width={300} 
                    theme={themeMode === 'dark' ? 'dark' : 'light'} 
                    style={{ 
                        borderLeft: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0', 
                        padding: 24,
                        background: token.colorBgContainer
                    }}
                >
                    <Typography.Title level={5}>测试控制台</Typography.Title>
                    <Typography.Paragraph type="secondary">
                        这是一个前端模拟的问答界面，用于测试 UI 交互和流式响应效果。
                    </Typography.Paragraph>
                    <Divider />
                    <Typography.Text strong>预设问题：</Typography.Text>
                    <Space direction="vertical" style={{ width: '100%', marginTop: 16 }}>
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


