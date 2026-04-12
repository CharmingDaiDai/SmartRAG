import React, { useState, memo } from 'react';
import { Modal, Space, Button, Typography, theme } from 'antd';
import { FileTextOutlined, LeftOutlined, RightOutlined, EyeOutlined, CaretDownOutlined, CaretRightOutlined } from '@ant-design/icons';
import { CodeHighlighter, Mermaid } from '@ant-design/x';
import { XMarkdown, type ComponentProps } from '@ant-design/x-markdown';
import Latex from '@ant-design/x-markdown/plugins/Latex';
import { useAppStore } from '../store/useAppStore';

interface Reference {
    title: string;
    score: number;
    content?: string;
    id?: string | number;
}

interface ReferenceViewerProps {
    references: Reference[];
}

const Code: React.FC<ComponentProps> = (props) => {
    const { className, children } = props;
    const lang = className?.match(/language-(\w+)/)?.[1] || '';
    if (typeof children !== 'string') return null;
    if (lang === 'mermaid') {
        return <Mermaid>{children}</Mermaid>;
    }
    return <CodeHighlighter lang={lang}>{children}</CodeHighlighter>;
};

const MD_CONFIG = { extensions: Latex({ katexOptions: { output: 'html' as const, throwOnError: false } }) };
const MD_COMPONENTS = { code: Code };

const ReferenceViewer: React.FC<ReferenceViewerProps> = memo(({ references }) => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentIndex, setCurrentIndex] = useState(0);
    const [expanded, setExpanded] = useState(false);
    const { themeMode } = useAppStore();
    const { token } = theme.useToken();

    if (!references || references.length === 0) return null;

    const currentRef = references[currentIndex];

    const handleItemClick = (idx: number) => {
        setCurrentIndex(idx);
        setIsModalOpen(true);
    };

    const handleNext = () => setCurrentIndex((prev) => (prev + 1) % references.length);
    const handlePrev = () => setCurrentIndex((prev) => (prev - 1 + references.length) % references.length);

    return (
        <div style={{ marginTop: 14, paddingTop: 12, borderTop: `1px solid ${token.colorBorderSecondary}` }}>
            {/* 标题行（点击展开/收缩） */}
            <div
                onClick={() => setExpanded(!expanded)}
                style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 6,
                    cursor: 'pointer',
                    marginBottom: expanded ? 8 : 0,
                    userSelect: 'none',
                }}
            >
                {expanded
                    ? <CaretDownOutlined style={{ fontSize: 10, color: token.colorTextTertiary }} />
                    : <CaretRightOutlined style={{ fontSize: 10, color: token.colorTextTertiary }} />
                }
                <Typography.Text style={{
                    fontSize: 11,
                    color: token.colorTextTertiary,
                    letterSpacing: '0.04em',
                    textTransform: 'uppercase',
                    fontWeight: 600,
                }}>
                    参考文档 ({references.length})
                </Typography.Text>
            </div>

            {/* 文档列表（展开后显示） */}
            {expanded && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                    {references.slice(0, 5).map((ref, idx) => (
                        <div
                            key={idx}
                            onClick={() => handleItemClick(idx)}
                            style={{
                                padding: '7px 10px',
                                borderRadius: 7,
                                cursor: 'pointer',
                                transition: 'all 0.18s ease',
                                border: `1px solid ${token.colorBorderSecondary}`,
                                background: token.colorBgContainer,
                            }}
                            className="reference-item"
                        >
                            <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                                <Space>
                                    <FileTextOutlined style={{ color: token.colorPrimary, fontSize: 12 }} />
                                    <Typography.Text ellipsis style={{ maxWidth: 200, fontSize: 12 }}>
                                        {ref.title}
                                    </Typography.Text>
                                </Space>
                                <Space size={4}>
                                    <Typography.Text style={{ fontSize: 11, color: token.colorPrimary, fontWeight: 600 }}>
                                        {(ref.score * 100).toFixed(0)}%
                                    </Typography.Text>
                                    <EyeOutlined style={{ color: token.colorTextTertiary, fontSize: 11 }} />
                                </Space>
                            </Space>
                        </div>
                    ))}
                    {references.length > 5 && (
                        <Typography.Text type="secondary" style={{ fontSize: 12, paddingLeft: 8 }}>
                            ... 等共 {references.length} 个文档
                        </Typography.Text>
                    )}
                </div>
            )}

            {/* 详情 Modal */}
            <Modal
                title={
                    <Space>
                        <FileTextOutlined style={{ color: token.colorPrimary }} />
                        <span>{currentRef?.title}</span>
                        <span style={{ fontSize: 12, color: token.colorPrimary, fontWeight: 500 }}>
                            ({(currentRef?.score * 100).toFixed(0)}% 相关)
                        </span>
                    </Space>
                }
                open={isModalOpen}
                onCancel={() => setIsModalOpen(false)}
                footer={[
                    <Button key="prev" aria-label="查看上一条引用" icon={<LeftOutlined />} onClick={handlePrev} disabled={references.length <= 1}>
                        上一篇
                    </Button>,
                    <span key="indicator" style={{ margin: '0 16px', color: token.colorTextSecondary, fontSize: 13 }}>
                        {currentIndex + 1} / {references.length}
                    </span>,
                    <Button key="next" aria-label="查看下一条引用" icon={<RightOutlined />} onClick={handleNext} disabled={references.length <= 1}>
                        下一篇
                    </Button>,
                ]}
                width={800}
            >
                <div style={{ maxHeight: '60vh', overflowY: 'auto', padding: '0 16px' }}>
                    {currentRef?.content ? (
                        <XMarkdown
                            className={themeMode === 'dark' ? 'x-markdown-dark' : 'x-markdown-light'}
                            config={MD_CONFIG}
                            components={MD_COMPONENTS}
                        >
                            {currentRef.content}
                        </XMarkdown>
                    ) : (
                        <div style={{ textAlign: 'center', padding: 40, color: token.colorTextTertiary }}>
                            暂无预览内容
                        </div>
                    )}
                </div>
            </Modal>
        </div>
    );
});

export default ReferenceViewer;
