/**
 * 动画思维链组件 (Animated Thought Chain)
 * 
 * 功能逻辑：
 * 1. 接收来自大模型或后端执行过程中的一系列 `ThoughtItem`（思考节点）。
 * 2. 使用 Ant Design X 的 `<Think>` 核心容器，渲染一个可折叠的执行过程面板。
 * 3. 内部通过定制化 CSS (borderLeft 模拟时间线, 绝对定位渲染时间线圆点) 实现了各个执行步骤 (如分词、检索、重排) 的可视化时间轴。
 * 4. 对于进行中的节点支持光标闪烁动画（由 `<Think blink={isLoading}>` 驱动），已完成的节点展示耗时。
 * 
 * 组件设计：
 * - 将内部列表渲染拆分为 `ThoughtContent` 并使用 `memo` 缓存，避免外层父组件由于微小状态更新带来的高频无意义重绘。
 * - 可以展现 Markdown 格式的具体思考日志。
 */
import React, { memo } from 'react';
import { Think } from '@ant-design/x';
import { CheckCircleOutlined } from '@ant-design/icons';
import { theme, Typography, Space } from 'antd';
import { ThoughtItem } from '../../types';
import { XMarkdown } from '@ant-design/x-markdown';

/** 思维链组件的属性定义 */
interface AnimatedThoughtChainProps {
    items: ThoughtItem[];               // 执行节点列表数据
    title?: string;                     // 外层容器显示的标题，默认为 "执行流程"
    expanded?: boolean;                 // 控制展开/折叠状态 (受控属性)
    onExpand?: (expand: boolean) => void; // 折叠状态改变时的回调函数
}

/**
 * 内部节点列表渲染组件 (提取出来独立渲染以提升性能)
 */
const ThoughtContent = memo(({ items }: { items: ThoughtItem[] }) => {
    const { token } = theme.useToken();

    return (
        <div style={{
            position: 'relative',
            paddingLeft: 12,
            borderLeft: `2px solid ${token.colorBorderSecondary}`, // 纵向时间线轨道
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
                    {/* 时间轴节点上的小圆点 (Timeline dot) */}
                    <div style={{
                        position: 'absolute',
                        left: -19, // 将圆点往左推移，精准盖在左侧 border 上
                        top: 4,
                        width: 10,
                        height: 10,
                        borderRadius: '50%',
                        // 根据当前子步骤状态变色
                        background: item.status === 'processing'
                            ? 'rgba(99, 102, 241, 0.6)' // 进行中展示带透明度的主色
                            : (item.status === 'success' ? token.colorSuccess : token.colorTextDisabled),
                        border: `2px solid ${token.colorBgContainer}`, // 利用背景色打底，模拟空心环效果
                        zIndex: 2,
                    }} />

                    {/* 具体内容描述区 */}
                    <div style={{ paddingLeft: 8 }}>
                        <Space size={4}>
                            {item.icon} {/* 步骤特定图标 */}
                            <Typography.Text strong style={{ fontSize: 13 }}>{item.title}</Typography.Text>
                            {/* 若包含耗时数据则展现出多少毫秒 */}
                            {item.duration != null && item.duration > 0 && (
                                <Typography.Text type="secondary" style={{ fontSize: 11 }}>
                                    {item.duration}ms
                                </Typography.Text>
                            )}
                        </Space>
                        {/* 若后端有返回具体执行的内容明细，用 Markdown 格式化后显示 */}
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

/**
 * 对外暴露的主视图外壳
 */
const AnimatedThoughtChain: React.FC<AnimatedThoughtChainProps> = memo(({
    items,
    title = '执行流程',
    expanded,
    onExpand,
}) => {
    // 容错：无列表数据时不渲染这个壳子
    if (!items || items.length === 0) return null;

    // 分析整体状态
    const isLoading = items.some(item => item.status === 'processing'); // 是否有进行中的步骤
    const isAllSuccess = items.every(item => item.status === 'success'); // 图标打钩条件：全体通过
    const processingItem = items.find(item => item.status === 'processing');

    // 动态标题组装：如果是进行中，标题后方动态缀上当前子步骤标题 (例如 "执行流程 · 检索文档中...")
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
            // 当这套组合操作整体都完毕后，最外面的兜底图标变成全绿的钩
            icon={isAllSuccess ? <CheckCircleOutlined style={{ color: '#10b981' }} /> : undefined}
            title={statusTitle}
            blink={isLoading} // 数据加载中，产生文字流水特效(闪烁)
            defaultExpanded={true} // 首次进来强制展开给用户看过程
            expanded={expanded}
            onExpand={onExpand}
            style={{ marginBottom: 12 }}
        >
            <ThoughtContent items={items} />
        </Think>
    );
});

export default AnimatedThoughtChain;
