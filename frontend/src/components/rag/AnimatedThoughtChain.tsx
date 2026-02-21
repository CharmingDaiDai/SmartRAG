import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
    CheckCircleOutlined, 
    LoadingOutlined, 
    CaretRightOutlined, 
    CaretDownOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';
import { theme, Typography, Space } from 'antd';
import { ThoughtItem } from '../../types';
import { XMarkdown } from '@ant-design/x-markdown';

interface AnimatedThoughtChainProps {
    items: ThoughtItem[];
    title?: string;
}

const AnimatedThoughtChain: React.FC<AnimatedThoughtChainProps> = ({ items, title = "思考过程" }) => {
    const { token } = theme.useToken();
    const [isExpanded, setIsExpanded] = useState(true);
    
    // 自动展开：当有新的正在处理的项时，自动展开
    useEffect(() => {
        if (items.some(item => item.status === 'processing')) {
            setIsExpanded(true);
        }
    }, [items]);

    if (!items || items.length === 0) return null;

    const processingItem = items.find(item => item.status === 'processing');
    const isAllSuccess = items.every(item => item.status === 'success');

    return (
        <div style={{ 
            marginBottom: 16, 
            border: `1px solid ${token.colorBorderSecondary}`, 
            borderRadius: token.borderRadiusLG,
            overflow: 'hidden',
            background: token.colorFillAlter
        }}>
            {/* Header */}
            <div 
                onClick={() => setIsExpanded(!isExpanded)}
                style={{
                    padding: '8px 12px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    background: isExpanded ? token.colorFillQuaternary : 'transparent',
                    transition: 'background 0.3s'
                }}
            >
                <Space>
                    {isAllSuccess ? (
                        <CheckCircleOutlined style={{ color: token.colorSuccess }} />
                    ) : (
                        <LoadingOutlined spin style={{ color: token.colorPrimary }} />
                    )}
                    <Typography.Text strong style={{ fontSize: 13 }}>
                        {title}
                        {processingItem && (
                            <Typography.Text type="secondary" style={{ fontWeight: 'normal', marginLeft: 8 }}>
                                · {processingItem.title}
                            </Typography.Text>
                        )}
                    </Typography.Text>
                </Space>
                {isExpanded ? <CaretDownOutlined style={{ fontSize: 10 }} /> : <CaretRightOutlined style={{ fontSize: 10 }} />}
            </div>

            {/* Content List */}
            <AnimatePresence initial={false}>
                {isExpanded && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.3, ease: "easeInOut" }}
                    >
                        <div style={{ padding: '8px 16px 16px 16px' }}>
                            <div style={{ 
                                position: 'relative', 
                                paddingLeft: 12, 
                                borderLeft: `2px solid ${token.colorBorderSecondary}`,
                                marginLeft: 6
                            }}>
                                {items.map((item, index) => (
                                    <ThoughtNode key={index} item={item} isLast={index === items.length - 1} />
                                ))}
                            </div>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
};

const ThoughtNode: React.FC<{ item: ThoughtItem; isLast: boolean }> = ({ item, isLast }) => {
    const { token } = theme.useToken();
    const isProcessing = item.status === 'processing';
    const isSuccess = item.status === 'success';

    return (
        <motion.div
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.3 }}
            style={{ 
                marginBottom: isLast ? 0 : 16,
                position: 'relative'
            }}
        >
            {/* Dot */}
            <div style={{
                position: 'absolute',
                left: -19,
                top: 4,
                width: 12,
                height: 12,
                borderRadius: '50%',
                background: isProcessing ? token.colorPrimary : (isSuccess ? token.colorSuccess : token.colorTextDisabled),
                border: `2px solid ${token.colorBgContainer}`,
                boxShadow: isProcessing ? `0 0 0 2px ${token.colorPrimaryBg}` : 'none',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 2
            }}>
                {isProcessing && (
                    <motion.div
                        animate={{ scale: [1, 1.5, 1], opacity: [0.5, 0, 0.5] }}
                        transition={{ duration: 1.5, repeat: Infinity }}
                        style={{
                            width: '100%',
                            height: '100%',
                            borderRadius: '50%',
                            background: token.colorPrimary
                        }}
                    />
                )}
            </div>

            {/* Content */}
            <div style={{ paddingLeft: 8 }}>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: 4 }}>
                    <span style={{ marginRight: 8 }}>{item.icon || <InfoCircleOutlined />}</span>
                    <Typography.Text strong>{item.title}</Typography.Text>
                    {item.duration && (
                        <Typography.Text type="secondary" style={{ fontSize: 12, marginLeft: 8 }}>
                            {item.duration}ms
                        </Typography.Text>
                    )}
                </div>
                
                {item.content && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ duration: 0.5 }}
                    >
                        <Typography.Text type="secondary" style={{ fontSize: 13 }}>
                            <XMarkdown>{item.content}</XMarkdown>
                        </Typography.Text>
                    </motion.div>
                )}
            </div>
        </motion.div>
    );
};

export default AnimatedThoughtChain;
