import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, Button, Card, Col, Empty, Row, Skeleton, Statistic, Typography, message, theme } from 'antd';
import { FileTextOutlined, DatabaseOutlined, MessageOutlined } from '@ant-design/icons';
import { Line, WordCloud } from '@ant-design/plots';
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

const getWordCloudSemanticColor = (item: WordCloudItem): string => {
    const source = `${item.text ?? ''} ${item.name ?? ''}`.toLowerCase();

    if (/chunk|chunking|切分/.test(source)) return '#1677ff';
    if (/parse|parser|文档|解析/.test(source)) return '#13c2c2';
    if (/prompt|提示词|hisem|sadp/.test(source)) return '#722ed1';
    if (/index|索引|embedding|向量/.test(source)) return '#00b42a';
    if (/conversation|chat|对话/.test(source)) return '#ff7d00';
    return '#86909c';
};

// 统计卡片的强调色配置（第一张卡片使用主题色，在组件内构建）
const getStatCards = (primaryColor: string) => [
    {
        title: '知识库数量',
        key: 'knowledgeBases' as const,
        icon: <DatabaseOutlined />,
        accentColor: primaryColor,
        bgColor: `${primaryColor}14`,
    },
    {
        title: '文档总数',
        key: 'documents' as const,
        icon: <FileTextOutlined />,
        accentColor: '#00b42a',
        bgColor: 'rgba(0, 180, 42, 0.1)',
    },
    {
        title: '对话总次数',
        key: 'conversationStats.total' as const,
        icon: <MessageOutlined />,
        accentColor: '#ff7d00',
        bgColor: 'rgba(255, 125, 0, 0.1)',
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
    const [loadError, setLoadError] = useState<string | null>(null);
    const { token } = theme.useToken();
    const STAT_CARDS = useMemo(() => getStatCards(token.colorPrimary), [token.colorPrimary]);

    const fetchStats = useCallback(async () => {
        setLoading(true);
        setLoadError(null);
        try {
            const res: any = await dashboardService.getStatistics();
            if (res.code === 200) {
                setData(res.data);
                return;
            }

            setLoadError(res.message || '获取统计数据失败');
            message.error(res.message || '获取统计数据失败');
        } catch (error) {
            console.error(error);
            setLoadError('获取统计数据失败，请稍后重试');
            message.error('获取统计数据失败');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchStats();
    }, [fetchStats]);

    const statValues = useMemo<Record<string, number>>(() => ({
        knowledgeBases: data.knowledgeBases,
        documents: data.documents,
        'conversationStats.total': data.conversationStats.total,
    }), [data]);

    const trendConfig = useMemo(() => {
        const trendData = data.conversationStats.last7Days.map((item) => ({
            value: item.count,
            date: item.date,
        }));
        const averageValue = trendData.reduce((acc, cur) => acc + cur.value, 0) / (trendData.length || 1);

        return {
            data: trendData,
            autoFit: true,
            padding: [10, 16, 18, 10] as [number, number, number, number],
            xField: 'date',
            yField: 'value',
            smooth: true,
            color: '#1677ff',
            point: {
                size: 3,
                shape: 'circle',
                style: {
                    fill: '#ffffff',
                    lineWidth: 1,
                },
            },
            style: {
                lineWidth: 2,
            },
            area: {
                style: {
                    fill: 'l(270) 0:rgba(22, 119, 255, 0.1) 1:rgba(22, 119, 255, 0.01)',
                },
            },
            xAxis: {
                line: {
                    style: {
                        stroke: '#e2e8f0',
                    },
                },
                tickLine: {
                    style: {
                        stroke: '#e2e8f0',
                    },
                },
                label: {
                    style: {
                        fill: '#94a3b8',
                        fontSize: 11,
                    },
                },
            },
            yAxis: {
                grid: {
                    line: {
                        style: {
                            stroke: '#e2e8f0',
                            lineDash: [3, 3],
                        },
                    },
                },
                label: {
                    style: {
                        fill: '#94a3b8',
                        fontSize: 11,
                    },
                },
            },
            tooltip: {
                title: (d: any) => d.date,
                items: [{ channel: 'y', name: '对话次数' }]
            },
            annotations: [
                {
                    type: 'lineY',
                    data: [averageValue],
                    label: {
                        text: '平均值',
                        position: 'left',
                        dx: -10,
                        style: { textBaseline: 'bottom', fill: '#94a3b8', fontSize: 11 },
                    },
                    style: { stroke: '#94a3b8', lineDash: [4, 4] },
                },
            ],
        };
    }, [data.conversationStats.last7Days]);

    const wordCloudData = useMemo(() => (
        [...data.wordCloud]
            .sort((a, b) => b.value - a.value)
            .slice(0, 80)
    ), [data.wordCloud]);

    const wordCloudConfig = useMemo(() => ({
        data: wordCloudData,
        layout: { spiral: 'rectangular' as const },
        colorField: 'text',
        autoFit: true,
        random: () => 0.5,
        color: (datum: WordCloudItem) => getWordCloudSemanticColor(datum),
    }), [wordCloudData]);

    const hasTrendData = data.conversationStats.last7Days.length > 0;
    const hasWordCloudData = data.wordCloud.length > 0;

    return (
        <div className="dashboard-page" style={{ height: '100%', overflowY: 'auto', overflowX: 'hidden' }}>
            <SlideInUp>
                <div style={{ marginBottom: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Title level={4} style={{ marginBottom: 0, fontSize: 16, fontWeight: 600, color: '#1e293b' }}>数据仪表盘</Title>
                    <Button type="primary" onClick={fetchStats} loading={loading}>刷新数据</Button>
                </div>
            </SlideInUp>

            {loadError && (
                <SlideInUp transition={{ delay: 0.05 }}>
                    <Alert
                        showIcon
                        type="warning"
                        message="数据加载存在异常"
                        description={loadError}
                        action={<Button size="small" onClick={fetchStats}>重试</Button>}
                        style={{ marginBottom: 16, borderRadius: 10 }}
                    />
                </SlideInUp>
            )}

            <StaggerContainer>
                {/* 顶部统计行 */}
                <Row gutter={[24, 24]} style={{ marginBottom: 24 }}>
                    {STAT_CARDS.map((card) => (
                        <Col xs={24} sm={12} xl={8} key={card.key}>
                            <StaggerItem>
                                <HoverCard>
                                    <Card
                                        className="dashboard-stat-card"
                                        hoverable
                                        style={{ borderRadius: 8, overflow: 'hidden' }}
                                        styles={{ body: { padding: 20 } }}
                                    >
                                        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                                            <div style={{
                                                width: 40,
                                                height: 40,
                                                borderRadius: 8,
                                                background: card.bgColor,
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                fontSize: 18,
                                                color: card.accentColor,
                                                flexShrink: 0,
                                            }}>
                                                {card.icon}
                                            </div>
                                            <Statistic
                                                title={(
                                                    <span className="dashboard-stat-title" style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                                                        <span style={{ width: 8, height: 8, borderRadius: '50%', background: card.accentColor, display: 'inline-block' }} />
                                                        <span>{card.title}</span>
                                                    </span>
                                                )}
                                                value={statValues[card.key]}
                                                loading={loading}
                                                styles={{
                                                    title: { fontSize: 12, fontWeight: 400, color: '#64748b' },
                                                    content: {
                                                        fontFamily: "'JetBrains Mono', ui-monospace, Consolas, monospace",
                                                        fontVariantNumeric: 'tabular-nums',
                                                        fontSize: 26,
                                                        fontWeight: 600,
                                                        color: '#1e293b',
                                                    }
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
                <Row gutter={[24, 24]}>
                    <Col xs={24} xl={12}>
                        <StaggerItem>
                            <HoverCard>
                                <Card
                                    className="dashboard-chart-card"
                                    title="近 7 天对话趋势"
                                    hoverable
                                    style={{ borderRadius: 8 }}
                                    styles={{
                                        body: {
                                            padding: 20,
                                        },
                                        header: {
                                            fontSize: 13,
                                            fontWeight: 600,
                                            borderBottom: `1px solid ${token.colorBorderSecondary}`,
                                        }
                                    }}
                                >
                                    <div style={{ height: CHART_HEIGHT }}>
                                        {loading ? (
                                            <Skeleton active paragraph={{ rows: 8 }} title={false} />
                                        ) : hasTrendData ? (
                                            // @ts-ignore
                                            <Line {...trendConfig} />
                                        ) : (
                                            <div className="ui-empty-panel" style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无趋势数据" />
                                            </div>
                                        )}
                                    </div>
                                </Card>
                            </HoverCard>
                        </StaggerItem>
                    </Col>
                    <Col xs={24} xl={12}>
                        <StaggerItem>
                            <HoverCard>
                                <Card
                                    className="dashboard-chart-card"
                                    title="热门关键词"
                                    hoverable
                                    style={{ borderRadius: 8 }}
                                    styles={{
                                        body: {
                                            padding: 20,
                                        },
                                        header: {
                                            fontSize: 13,
                                            fontWeight: 600,
                                            borderBottom: `1px solid ${token.colorBorderSecondary}`,
                                        }
                                    }}
                                >
                                    <div style={{ height: CHART_HEIGHT }}>
                                        {loading ? (
                                            <Skeleton active paragraph={{ rows: 8 }} title={false} />
                                        ) : hasWordCloudData ? (
                                            // @ts-ignore
                                            <WordCloud {...wordCloudConfig} />
                                        ) : (
                                            <div className="ui-empty-panel" style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无关键词数据" />
                                            </div>
                                        )}
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
