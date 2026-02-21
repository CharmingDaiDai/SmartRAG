import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Typography, message, theme } from 'antd';
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

const CHART_HEIGHT = 300;

// 统计卡片的强调色配置
const STAT_CARDS = [
    {
        title: '知识库数量',
        key: 'knowledgeBases' as const,
        icon: <DatabaseOutlined />,
        accentColor: '#6366f1',
        bgColor: 'rgba(99, 102, 241, 0.08)',
    },
    {
        title: '文档总数',
        key: 'documents' as const,
        icon: <FileTextOutlined />,
        accentColor: '#10b981',
        bgColor: 'rgba(16, 185, 129, 0.08)',
    },
    {
        title: '对话总次数',
        key: 'conversationStats.total' as const,
        icon: <MessageOutlined />,
        accentColor: '#f59e0b',
        bgColor: 'rgba(245, 158, 11, 0.08)',
    },
];

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
    const { token } = theme.useToken();

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

    const statValues: Record<string, number> = {
        knowledgeBases: data.knowledgeBases,
        documents: data.documents,
        'conversationStats.total': data.conversationStats.total,
    };

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
            fill: 'linear-gradient(-90deg, rgba(99,102,241,0.02) 0%, rgba(99,102,241,0.25) 100%)',
            fillOpacity: 1,
            stroke: '#6366f1',
            lineWidth: 2,
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
                    style: { textBaseline: 'bottom', fill: token.colorTextTertiary },
                },
                style: { stroke: token.colorTextTertiary, lineDash: [4, 4] },
            },
        ],
    };

    const wordCloudConfig = {
        data: data.wordCloud,
        layout: { spiral: 'rectangular' },
        colorField: 'text',
        autoFit: true,
        color: ['#c7d2fe', '#a5b4fc', '#818cf8', '#6366f1', '#4f46e5', '#4338ca'],
    };

    return (
        <div style={{ height: '100%', overflowY: 'auto', overflowX: 'hidden' }}>
            <SlideInUp>
                <Title level={4} style={{ marginBottom: 24 }}>数据仪表盘</Title>
            </SlideInUp>

            <StaggerContainer>
                {/* 顶部统计行 */}
                <Row gutter={[20, 20]} style={{ marginBottom: 24 }}>
                    {STAT_CARDS.map((card) => (
                        <Col span={8} key={card.key}>
                            <StaggerItem>
                                <HoverCard>
                                    <Card hoverable style={{ borderRadius: 12, overflow: 'hidden' }}>
                                        {/* 顶部彩色强调条 */}
                                        <div style={{
                                            position: 'absolute',
                                            top: 0,
                                            left: 0,
                                            right: 0,
                                            height: 3,
                                            background: card.accentColor,
                                            borderRadius: '12px 12px 0 0',
                                        }} />
                                        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                                            <div style={{
                                                width: 44,
                                                height: 44,
                                                borderRadius: 10,
                                                background: card.bgColor,
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                fontSize: 20,
                                                color: card.accentColor,
                                                flexShrink: 0,
                                            }}>
                                                {card.icon}
                                            </div>
                                            <Statistic
                                                title={card.title}
                                                value={statValues[card.key]}
                                                loading={loading}
                                                valueStyle={{
                                                    fontFamily: "'JetBrains Mono', ui-monospace, Consolas, monospace",
                                                    fontVariantNumeric: 'tabular-nums',
                                                    fontSize: 28,
                                                    fontWeight: 600,
                                                    color: token.colorText,
                                                }}
                                            />
                                        </div>
                                    </Card>
                                </HoverCard>
                            </StaggerItem>
                        </Col>
                    ))}
                </Row>

                {/* 图表行 */}
                <Row gutter={[20, 20]}>
                    <Col span={12}>
                        <StaggerItem>
                            <HoverCard>
                                <Card
                                    title="近 7 天对话趋势"
                                    hoverable
                                    styles={{
                                        header: {
                                            fontSize: 14,
                                            fontWeight: 600,
                                            borderBottom: `1px solid ${token.colorBorderSecondary}`,
                                        }
                                    }}
                                >
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
                            <HoverCard>
                                <Card
                                    title="热门关键词"
                                    hoverable
                                    styles={{
                                        header: {
                                            fontSize: 14,
                                            fontWeight: 600,
                                            borderBottom: `1px solid ${token.colorBorderSecondary}`,
                                        }
                                    }}
                                >
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
