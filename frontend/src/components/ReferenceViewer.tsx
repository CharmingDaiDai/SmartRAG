/**
 * RAG 参考文档引用查看器 (ReferenceViewer)
 * 
 * 功能逻辑：
 * 1. 接收大模型执行完毕后返回的 `references` (参考列表数据，含标题、相似度打分和原文块)。
 * 2. 渲染一个折叠面板区域。折叠时展示数量，展开后展现前 5 条引用卡片。
 * 3. 点击单条引用卡片时，弹出一个宽 Modal，内部使用 XMarkdown 能够渲染原汁原味的代码、图表。
 * 4. Modal 底部提供「上一篇/下一篇」的分页轮播功能，方便用户核对大模型回答的准确性与事实来源。
 * 
 * 组件设计：
 * - 引入 memo 避免对话流式输入阶段（每秒多次 render）导致本组件无意义重绘。
 * - 动态注册的 Markdown 解析器，支持从后端抛出的包含 KaTeX 或 Mermaid 格式的文档块。
 */
import React, { useState, memo } from 'react';
import { Modal, Space, Button, Typography, theme } from 'antd';
import { FileTextOutlined, LeftOutlined, RightOutlined, EyeOutlined, CaretDownOutlined, CaretRightOutlined } from '@ant-design/icons';
import { CodeHighlighter, Mermaid } from '@ant-design/x';
import { XMarkdown, type ComponentProps } from '@ant-design/x-markdown';
import Latex from '@ant-design/x-markdown/plugins/Latex';
import { useAppStore } from '../store/useAppStore';

/** 后端反传出引用块的数据结构定义 */
interface Reference {
    title: string;          // 被引用的源文档名称
    score: number;          // 向量距离评分 (0-1)
    content?: string;       // Chunk 片段的原始文本内容
    id?: string | number;   // 溯源 ID
}

interface ReferenceViewerProps {
    references: Reference[];
}

/** 
 * 自定义的代码高亮插槽，当来源文档中包含程序代码时截获渲染
 * 如果命中 mermaid 标记，就转交为流程图绘制器
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

// 预先初始化 Markdown 的插件依赖防止组件内高频初始化
const MD_CONFIG = { extensions: Latex({ katexOptions: { output: 'html' as const, throwOnError: false } }) };
const MD_COMPONENTS = { code: Code };

const ReferenceViewer: React.FC<ReferenceViewerProps> = memo(({ references }) => {
    const [isModalOpen, setIsModalOpen] = useState(false); // 控制详细正文弹窗是否可见
    const [currentIndex, setCurrentIndex] = useState(0);    // 当前在弹窗中正在阅读的引文下标
    const [expanded, setExpanded] = useState(false);        // 控制卡片列表本身是合起还是展开
    const { themeMode } = useAppStore();
    const { token } = theme.useToken();

    // 防御性拦截，如果没有资料提供支撑则直接隐形
    if (!references || references.length === 0) return null;

    const currentRef = references[currentIndex];

    /** 通过点击小卡片唤出全屏弹层，并定位到对应的序列位置 */
    const handleItemClick = (idx: number) => {
        setCurrentIndex(idx);
        setIsModalOpen(true);
    };

    // 翻页循环机制 (取模运转，无缝切换)
    const handleNext = () => setCurrentIndex((prev) => (prev + 1) % references.length);
    const handlePrev = () => setCurrentIndex((prev) => (prev - 1 + references.length) % references.length);

    return (
        <div style={{ marginTop: 14, paddingTop: 12, borderTop: `1px solid ${token.colorBorderSecondary}` }}>
            {/* 折叠/展开控制头（包含三角号动画） */}
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

            {/* 当处于展开状态时绘制条目队列 */}
            {expanded && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                    {/* UI 保护：超过 5 条不渲染以防刷爆版面，用户可通过分页在弹窗里读 */}
                    {references.slice(0, 5).map((ref, idx) => (
                        <div
                            key={idx}
                            onClick={() => handleItemClick(idx)}
                            style={{
                                padding: '7px 10px',
                                borderRadius: 7,
                                cursor: 'pointer',
                                transition: 'background-color 0.18s ease, border-color 0.18s ease',
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

            {/* 包含分页器与详细文本高亮的阅读弹层 Modal */}
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
