import React, { memo } from 'react';
import { Think } from '@ant-design/x';
import { CheckCircleOutlined } from '@ant-design/icons';
import { theme, Typography, Space } from 'antd';
import { ThoughtItem } from '../../types';
import { XMarkdown } from '@ant-design/x-markdown';

interface AnimatedThoughtChainProps {
    items: ThoughtItem[];
    title?: string;
    expanded?: boolean;
    onExpand?: (expand: boolean) => void;
}

const ThoughtContent = memo(({ items }: { items: ThoughtItem[] }) => {
    const { token } = theme.useToken();

    return (
        <div style={{
            position: 'relative',
            paddingLeft: 12,
            borderLeft: `2px solid ${token.colorBorderSecondary}`,
            marginLeft: 6,
            paddingTop: 4,
            paddingBottom: 4,
        }}>
            {items.map((item, index) => (
                <div
                    key={index}
                    style={{
                        marginBottom: index === items.length - 1 ? 0 : 12,
                        position: 'relative',
                    }}
                >
                    {/* Timeline dot */}
                    <div style={{
                        position: 'absolute',
                        left: -19,
                        top: 4,
                        width: 10,
                        height: 10,
                        borderRadius: '50%',
                        background: item.status === 'processing'
                            ? 'rgba(99, 102, 241, 0.6)'
                            : (item.status === 'success' ? token.colorSuccess : token.colorTextDisabled),
                        border: `2px solid ${token.colorBgContainer}`,
                        zIndex: 2,
                    }} />

                    <div style={{ paddingLeft: 8 }}>
                        <Space size={4}>
                            {item.icon}
                            <Typography.Text strong style={{ fontSize: 13 }}>{item.title}</Typography.Text>
                            {item.duration != null && item.duration > 0 && (
                                <Typography.Text type="secondary" style={{ fontSize: 11 }}>
                                    {item.duration}ms
                                </Typography.Text>
                            )}
                        </Space>
                        {item.content && (
                            <div style={{ marginTop: 2 }}>
                                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                    <XMarkdown>{item.content}</XMarkdown>
                                </Typography.Text>
                            </div>
                        )}
                    </div>
                </div>
            ))}
        </div>
    );
});

const AnimatedThoughtChain: React.FC<AnimatedThoughtChainProps> = memo(({
    items,
    title = '执行流程',
    expanded,
    onExpand,
}) => {
    if (!items || items.length === 0) return null;

    const isLoading = items.some(item => item.status === 'processing');
    const isAllSuccess = items.every(item => item.status === 'success');
    const processingItem = items.find(item => item.status === 'processing');

    const statusTitle = (
        <span>
            {title}
            {processingItem && (
                <Typography.Text type="secondary" style={{ fontWeight: 'normal', marginLeft: 8, fontSize: 13 }}>
                    · {processingItem.title}
                </Typography.Text>
            )}
        </span>
    );

    return (
        <Think
            loading={isLoading}
            icon={isAllSuccess ? <CheckCircleOutlined style={{ color: '#10b981' }} /> : undefined}
            title={statusTitle}
            blink={isLoading}
            defaultExpanded={true}
            expanded={expanded}
            onExpand={onExpand}
            style={{ marginBottom: 12 }}
        >
            <ThoughtContent items={items} />
        </Think>
    );
});

export default AnimatedThoughtChain;
