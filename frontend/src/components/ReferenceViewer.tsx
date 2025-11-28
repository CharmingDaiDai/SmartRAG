import React, { useState } from 'react';
import { Modal, Space, Typography, Button } from 'antd';
import { FileTextOutlined, LeftOutlined, RightOutlined, EyeOutlined } from '@ant-design/icons';
import { XMarkdown, type ComponentProps } from '@ant-design/x-markdown';
import HighlightCode from '@ant-design/x-markdown/plugins/HighlightCode';
import Latex from '@ant-design/x-markdown/plugins/latex';
import Mermaid from '@ant-design/x-markdown/plugins/mermaid';
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

const ReferenceViewer: React.FC<ReferenceViewerProps> = ({ references }) => {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [currentIndex, setCurrentIndex] = useState(0);
    const { themeMode } = useAppStore();

    if (!references || references.length === 0) return null;

    const handleOpen = (index: number) => {
        setCurrentIndex(index);
        setIsModalOpen(true);
    };

    const handleNext = () => {
        setCurrentIndex((prev) => (prev + 1) % references.length);
    };

    const handlePrev = () => {
        setCurrentIndex((prev) => (prev - 1 + references.length) % references.length);
    };

    const currentRef = references[currentIndex];

    return (
        <div style={{ marginTop: 16, paddingTop: 16, borderTop: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0' }}>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>参考文档：</Typography.Text>
            <Space direction="vertical" size={4} style={{ width: '100%', marginTop: 8 }}>
                {references.slice(0, 5).map((ref, idx) => (
                    <div 
                        key={idx} 
                        style={{ 
                            padding: '8px 12px', 
                            background: themeMode === 'dark' ? '#1f1f1f' : '#f9f9f9', 
                            borderRadius: 4,
                            cursor: 'pointer',
                            transition: 'all 0.2s',
                            border: '1px solid transparent'
                        }}
                        className="reference-item"
                        onClick={() => handleOpen(idx)}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.background = themeMode === 'dark' ? '#141414' : '#e6f7ff';
                            e.currentTarget.style.borderColor = themeMode === 'dark' ? '#1677ff' : '#91caff';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.background = themeMode === 'dark' ? '#1f1f1f' : '#f9f9f9';
                            e.currentTarget.style.borderColor = 'transparent';
                        }}
                    >
                        <Space style={{ width: '100%', justifyContent: 'space-between' }}>
                            <Space>
                                <FileTextOutlined style={{ color: '#1677ff' }} />
                                <Typography.Text ellipsis style={{ maxWidth: 200, fontSize: 12, color: themeMode === 'dark' ? 'rgba(255,255,255,0.85)' : undefined }}>
                                    {ref.title}
                                </Typography.Text>
                            </Space>
                            <Space>
                                <Typography.Text type="success" style={{ fontSize: 12 }}>
                                    {(ref.score * 100).toFixed(0)}%
                                </Typography.Text>
                                <EyeOutlined style={{ color: '#999', fontSize: 12 }} />
                            </Space>
                        </Space>
                    </div>
                ))}
                {references.length > 5 && (
                    <Typography.Text type="secondary" style={{ fontSize: 12, paddingLeft: 8 }}>
                        ... 等共 {references.length} 个文档
                    </Typography.Text>
                )}
            </Space>

            <Modal
                title={
                    <Space>
                        <FileTextOutlined />
                        <span>{currentRef?.title}</span>
                        <span style={{ fontSize: 12, color: '#52c41a', fontWeight: 'normal' }}>
                            (相关度: {(currentRef?.score * 100).toFixed(0)}%)
                        </span>
                    </Space>
                }
                open={isModalOpen}
                onCancel={() => setIsModalOpen(false)}
                footer={[
                    <Button key="prev" icon={<LeftOutlined />} onClick={handlePrev} disabled={references.length <= 1}>
                        上一篇
                    </Button>,
                    <span key="indicator" style={{ margin: '0 16px' }}>
                        {currentIndex + 1} / {references.length}
                    </span>,
                    <Button key="next" icon={<RightOutlined />} onClick={handleNext} disabled={references.length <= 1}>
                        下一篇
                    </Button>,
                ]}
                width={800}
            >
                <div style={{ maxHeight: '60vh', overflowY: 'auto', padding: '0 16px' }}>
                    {currentRef?.content ? (
                        <XMarkdown 
                            className={themeMode === 'dark' ? 'x-markdown-dark' : 'x-markdown-light'}
                            // @ts-ignore
                            plugins={MD_PLUGINS}
                            components={MD_COMPONENTS}
                        >
                            {currentRef.content}
                        </XMarkdown>
                    ) : (
                        <div style={{ textAlign: 'center', padding: 40, color: '#999' }}>
                            暂无预览内容
                        </div>
                    )}
                </div>
            </Modal>
        </div>
    );
};

export default ReferenceViewer;
