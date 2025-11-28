import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Typography, message } from 'antd';
import { FileTextOutlined, DatabaseOutlined, MessageOutlined } from '@ant-design/icons';
import { Tiny, WordCloud } from '@ant-design/plots';
import { dashboardService } from '../../services/dashboardService';
import { StaggerContainer, StaggerItem } from '../../components/common/Motion';

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
        <div className="p-6 bg-gray-50 min-h-screen">
            <StaggerContainer>
                <Row gutter={16} style={{ marginBottom: 24 }}>
                    <Col span={8}>
                        <StaggerItem>
                            <Card hoverable>
                                <Statistic 
                                    title="知识库数量" 
                                    value={data.knowledgeBases} 
                                    prefix={<DatabaseOutlined className="text-blue-500" />} 
                                    loading={loading}
                                />
                            </Card>
                        </StaggerItem>
                    </Col>
                    <Col span={8}>
                        <StaggerItem>
                            <Card hoverable>
                                <Statistic 
                                    title="文档总数" 
                                    value={data.documents} 
                                    prefix={<FileTextOutlined className="text-green-500" />} 
                                    loading={loading}
                                />
                            </Card>
                        </StaggerItem>
                    </Col>
                    <Col span={8}>
                        <StaggerItem>
                            <Card hoverable>
                                <Statistic 
                                    title="对话总次数" 
                                    value={data.conversationStats.total} 
                                    prefix={<MessageOutlined className="text-purple-500" />} 
                                    loading={loading}
                                />
                            </Card>
                        </StaggerItem>
                    </Col>
                </Row>

                <Row gutter={16}>
                    <Col span={12}>
                        <StaggerItem>
                            <Card title="近 7 天对话趋势" hoverable style={{ height: '100%' }}>
                                <div style={{ height: 300 }}>
                                    {/* @ts-ignore */}
                                    <Tiny.Area {...areaConfig} />
                                </div>
                            </Card>
                        </StaggerItem>
                    </Col>
                    <Col span={12}>
                        <StaggerItem>
                            <Card title="热门关键词" hoverable style={{ height: '100%' }}>
                                <div style={{ height: 300 }}>
                                    {/* @ts-ignore */}
                                    <WordCloud {...wordCloudConfig} />
                                </div>
                            </Card>
                        </StaggerItem>
                    </Col>
                </Row>
            </StaggerContainer>
        </div>
    );
};

export default Dashboard;
