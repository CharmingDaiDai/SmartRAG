import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Typography, message } from 'antd';
import { FileTextOutlined, DatabaseOutlined, MessageOutlined } from '@ant-design/icons';
import { Tiny, WordCloud } from '@ant-design/plots';
import { dashboardService } from '../../services/dashboardService';
import { StaggerContainer, StaggerItem, SlideInUp, HoverCard } from '../../components/common/Motion';
const { Title } = Typography;

interface DailyStat {
    date: string;
    count: number;
}

interface WordCloudItem {
    text: string;
    name: string;
    value: number;
}

interface DashboardData {
    knowledgeBases: number;
    documents: number;
    conversationStats: {
        total: number;
        last7Days: DailyStat[];
    };
    wordCloud: WordCloudItem[];
}

// 图表与布局尺寸常量（集中管理，便于调整）
// - 在需要像素级稳定渲染时，建议对图表使用固定像素高度（例如 CHART_HEIGHT）。
// - 使用百分比（height: '100%'）可以让子元素撑满父容器，但前提是父容器有明确高度。
// - 使用视口单位（vh）或 calc(100vh - offset) 可使组件随屏幕高度自适应。
// 注意事项：
//  - height: '100%' 需要父容器有明确高度，否则会塌为内容高度（或 0）。
//  - 图表使用固定 px 高度更可靠，有助于图表库正确测量容器尺寸。
//  - overflow: hidden 会裁切超出内容，若希望显示滚动条请使用 overflowY: 'auto'。
const CHART_HEIGHT = 300;

const Dashboard: React.FC = () => {
    const [data, setData] = useState<DashboardData>({
        knowledgeBases: 0,
        documents: 0,
        conversationStats: {
            total: 0,
            last7Days: []
        },
        wordCloud: []
    });
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const fetchStats = async () => {
            setLoading(true);
            try {
                const res: any = await dashboardService.getStatistics();
                if (res.code === 200) {
                    setData(res.data);
                }
            } catch (error) {
                console.error(error);
                message.error('获取统计数据失败');
            } finally {
                setLoading(false);
            }
        };
        fetchStats();
    }, []);

    // Tiny Area Chart Config
    const areaConfig = {
        data: data.conversationStats.last7Days.map((item, index) => ({
            value: item.count,
            index: index,
            date: item.date
        })),
        autoFit: true,
        padding: 8,
        shapeField: 'smooth',
        xField: 'index',
        yField: 'value',
        style: {
            fill: 'linear-gradient(-90deg, white 0%, #1677ff 100%)',
            fillOpacity: 0.6,
        },
        tooltip: {
            title: (d: any) => d.date,
            items: [{ channel: 'y', name: '对话次数' }]
        },
        annotations: [
            {
                type: 'lineY',
                data: [data.conversationStats.last7Days.reduce((acc, cur) => acc + cur.count, 0) / (data.conversationStats.last7Days.length || 1)],
                label: {
                    text: '平均值',
                    position: 'left',
                    dx: -10,
                    style: { textBaseline: 'bottom' },
                },
                style: { stroke: 'rgba(0, 0, 0, 0.45)', lineDash: [4, 4] },
            },
        ],
    };

    // Word Cloud Config
    const wordCloudConfig = {
        data: data.wordCloud,
        layout: { spiral: 'rectangular' },
        colorField: 'text',
        autoFit: true,
    };

    return (
        <div
            className="p-[15px] bg-gray-50"
            style={{
                // 容器尝试填满父容器高度；如果父容器没有显式高度，height: '100%' 等同于 'auto'。
                height: '100%',
                // overflowY: 'hidden' 会隐藏垂直滚动；如内容可能超出，建议使用 'auto' 显示滚动条，避免内容被裁剪。
                // 一般页面主容器使用 'auto' 更友好，只有配合设计或固定布局时才使用 'hidden'。
                overflowY: 'auto',
                overflowX: 'auto'
            }}
        >
            <SlideInUp>
                <Title level={4} style={{ marginBottom: 24 }}>数据仪表盘</Title>
            </SlideInUp>
            {/* StaggerContainer：用于在子项挂载时错开出现动画。 */}
            {/* 注意：StaggerContainer 不会改变布局尺寸；若需要行/卡片撑满高度，请结合 flex 布局和显式高度（或 minHeight）。 */}
            <StaggerContainer>
                {/* 顶部统计行 - 当父容器（Row/Col）有显式高度或 Card 设置了 minHeight 时，卡片的 height: '100%' 才会生效； */}
                {/* 否则卡片根据内容高度自动伸缩。若需卡片等高，建议在父容器或 Card 上使用 minHeight 或固定高度。 */}
                <Row gutter={24} style={{ marginBottom: 32 }}>
                    <Col span={8}>
                        <StaggerItem>
                            <HoverCard style={{ height: '100%' }}>
                                {/*
                                    HoverCard 的 height: '100%':
                                    - 当父容器具有明确高度时，'100%' 会让卡片撑满父容器高度；
                                    - 如果父容器没有高度，'100%' 不起作用，卡片将按内容高度自动伸缩；
                                    - 建议：如需统一卡片高度，使用父容器或 Card 的 minHeight 或固定高度。
                                */}
                                <Card hoverable>
                                    <Statistic
                                        title="知识库数量"
                                        value={data.knowledgeBases}
                                        prefix={<DatabaseOutlined className="text-blue-500" />}
                                        loading={loading}
                                    />
                                </Card>
                            </HoverCard>
                        </StaggerItem>
                    </Col>
                    <Col span={8}>
                        <StaggerItem>
                            <HoverCard style={{ height: '100%' }}>
                                <Card hoverable>
                                    <Statistic
                                        title="文档总数"
                                        value={data.documents}
                                        prefix={<FileTextOutlined className="text-green-500" />}
                                        loading={loading}
                                    />
                                </Card>
                            </HoverCard>
                        </StaggerItem>
                    </Col>
                    <Col span={8}>
                        <StaggerItem>
                            <HoverCard style={{ height: '100%' }}>
                                <Card hoverable>
                                    <Statistic
                                        title="对话总次数"
                                        value={data.conversationStats.total}
                                        prefix={<MessageOutlined className="text-purple-500" />}
                                        loading={loading}
                                    />
                                </Card>
                            </HoverCard>
                        </StaggerItem>
                    </Col>
                </Row>

                <Row gutter={24}>
                    <Col span={12}>
                        <StaggerItem>
                            <HoverCard style={{ height: '100%' }}>
                                <Card title="近 7 天对话趋势" hoverable style={{ height: '100%' }}>
                                    {
                                                                    /*
                                                                        图表容器高度：
                                                                        - 我们使用固定像素高度（CHART_HEIGHT）以保证图表渲染的稳定性。
                                                                        - 若将 div 的高度设为百分比（height: '100%'），则父 Card 必须有显式高度，否则图表可能高度为 0。
                                                                        - 可选方案：
                                                                            * 在 Card 上使用 minHeight（如 minHeight: 200）来允许图表有最小高度并能伸缩。
                                                                            * 使用视口单位或 calc（例如 calc(100vh - 400px））使图表随屏幕高度自适应。
                                                                    */}
                                    <div style={{ height: CHART_HEIGHT }}>
                                        {/* @ts-ignore */}
                                        <Tiny.Area {...areaConfig} />
                                    </div>
                                </Card>
                            </HoverCard>
                        </StaggerItem>
                    </Col>
                    <Col span={12}>
                        <StaggerItem>
                            <HoverCard style={{ height: '100%' }}>
                                <Card title="热门关键词" hoverable style={{ height: '100%' }}>
                                    {/* 图表固定高度：
                                                                    - CHART_HEIGHT px 确保图表渲染器有稳定的区域可绘制。
                                                                    - 值越小图表越短，值越大图表占用的垂直空间越多。
                                                                    - 若希望图表高度自适应，可将父容器设为显式高度（例如 Card: height: '100%' 且父容器有 minHeight）。
                                                                */}
                                    <div style={{ height: CHART_HEIGHT }}>
                                        {/* @ts-ignore */}
                                        <WordCloud {...wordCloudConfig} />
                                    </div>
                                </Card>
                            </HoverCard>
                        </StaggerItem>
                    </Col>
                </Row>
            </StaggerContainer>
        </div>
    );
};

export default Dashboard;
