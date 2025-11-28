import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic } from 'antd';
import { FileTextOutlined, DatabaseOutlined, MessageOutlined } from '@ant-design/icons';
import { dashboardService } from '../../services/dashboardService';
import { StaggerContainer, StaggerItem } from '../../components/common/Motion';

const Dashboard: React.FC = () => {
    const [stats, setStats] = useState<any>({
        documents: 0,
        kbs: 0,
        chats: 0,
    });
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const fetchStats = async () => {
            setLoading(true);
            try {
                const res: any = await dashboardService.getStatistics();
                if (res.code === 200) {
                    setStats(res.data);
                }
            } catch (error) {
                // message.error('获取统计数据失败');
            } finally {
                setLoading(false);
            }
        };
        fetchStats();
    }, []);

    return (
        <div className="p-6 bg-gray-50 min-h-screen">
            <StaggerContainer>
                <Row gutter={16}>
                    <Col span={8}>
                        <StaggerItem>
                            <Card>
                                <Statistic 
                                    title="文档总数" 
                                    value={stats.documents} 
                                    prefix={<FileTextOutlined />} 
                                    loading={loading}
                                />
                            </Card>
                        </StaggerItem>
                    </Col>
                    <Col span={8}>
                        <StaggerItem>
                            <Card>
                                <Statistic 
                                    title="知识库数量" 
                                    value={stats.kbs} 
                                    prefix={<DatabaseOutlined />} 
                                    loading={loading}
                                />
                            </Card>
                        </StaggerItem>
                    </Col>
                    <Col span={8}>
                        <StaggerItem>
                            <Card>
                                <Statistic 
                                    title="对话次数" 
                                    value={stats.chats} 
                                    prefix={<MessageOutlined />} 
                                    loading={loading}
                                />
                            </Card>
                        </StaggerItem>
                    </Col>
                </Row>
            </StaggerContainer>
        </div>
    );
};

export default Dashboard;
