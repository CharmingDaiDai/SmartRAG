import React from 'react';
import {Badge, Button, Card, Col, Divider, List, Row, Space, Typography} from 'antd';
import {
    CheckCircleOutlined,
    CloudDownloadOutlined,
    CrownOutlined,
    RocketOutlined,
    ThunderboltOutlined,
    UploadOutlined
} from '@ant-design/icons';
import {useAuth} from '../../context/AuthContext';

const { Title, Paragraph, Text } = Typography;

const VipMembership = () => {
  const { currentUser } = useAuth();
  const isVip = currentUser?.isVip;

  // VIP特权列表
  const vipFeatures = [
    {
      title: '高级文档分析',
      description: '使用更先进的AI模型进行文档分析，获得更准确的分析结果',
      icon: <RocketOutlined />,
    },
    {
      title: '批量处理能力',
      description: '一次处理多个文档，提高工作效率',
      icon: <ThunderboltOutlined />,
    },
    {
      title: '无限上传',
      description: '突破普通用户的上传限制，享受无限上传文档的特权',
      icon: <UploadOutlined />,
    },
    {
      title: '优先云存储',
      description: '更大的云存储空间和更快的访问速度',
      icon: <CloudDownloadOutlined />,
    },
  ];

  // 会员套餐信息
  const membershipPlans = [
    {
      title: '月度会员',
      price: '￥6/月',
      features: [
        '所有VIP特权',
        '每月自动续费',
        '随时可取消',
      ],
      recommended: false,
      buttonText: '立即开通',
    },
    {
      title: '年度会员',
      price: '￥60/年',
      features: [
        '所有VIP特权',
        '相当于￥5/月',
        '节省23%',
        '赠送1个月',
      ],
      recommended: true,
      buttonText: '立即开通',
    },
    {
      title: '终身会员',
      price: '￥88',
      features: [
        '所有VIP特权',
        '一次付费，终身受益',
        '免费获取所有未来更新',
      ],
      recommended: false,
      buttonText: '立即开通',
    },
  ];

  return (
    <div style={{ padding: '20px' }}>
      <Row gutter={[24, 24]}>
        <Col span={24}>
          <Card>
            <Space align="center" style={{ marginBottom: 24 }}>
              <CrownOutlined style={{ fontSize: 32, color: '#faad14' }} />
              <Title level={2} style={{ margin: 0 }}>VIP会员中心</Title>
            </Space>
            
            <Paragraph>
              解锁全部高级功能，享受更好的服务体验。
            </Paragraph>
            
            <div style={{ background: '#f5f5f5', padding: 16, borderRadius: 8, marginBottom: 24 }}>
              <Space align="center">
                <Title level={4} style={{ margin: 0 }}>
                  您当前的会员状态: 
                </Title>
                {isVip ? (
                  <Badge status="success" text={<Text strong style={{ color: '#52c41a', fontSize: 16 }}>VIP会员</Text>} />
                ) : (
                  <Badge status="default" text={<Text style={{ fontSize: 16 }}>普通用户</Text>} />
                )}
              </Space>
              
              {isVip && (
                <Text type="secondary" style={{ display: 'block', marginTop: 8 }}>
                  有效期至: 2025年12月31日
                </Text>
              )}
            </div>
          </Card>
        </Col>
      </Row>
      
      <Divider orientation="left">
        <Space>
          <CrownOutlined style={{ color: '#faad14' }} />
          <span>会员特权</span>
        </Space>
      </Divider>
      
      <Row gutter={[24, 24]}>
        {vipFeatures.map((feature, index) => (
          <Col xs={24} sm={12} md={6} key={index}>
            <Card 
              hoverable 
              style={{ height: '100%' }}
              actions={[
                isVip ? (
                  <Text type="success">
                    <CheckCircleOutlined /> 已解锁
                  </Text>
                ) : (
                  <Text type="secondary">
                    <CrownOutlined /> 成为会员解锁
                  </Text>
                )
              ]}
            >
              <div style={{ textAlign: 'center', marginBottom: 16 }}>
                <div style={{ 
                  fontSize: 36, 
                  color: '#1890ff', 
                  marginBottom: 16,
                  background: '#f0f5ff',
                  width: 80,
                  height: 80,
                  lineHeight: '80px',
                  borderRadius: '50%',
                  margin: '0 auto'
                }}>
                  {feature.icon}
                </div>
                <Title level={4}>{feature.title}</Title>
                <Paragraph type="secondary">
                  {feature.description}
                </Paragraph>
              </div>
            </Card>
          </Col>
        ))}
      </Row>
      
      {!isVip && (
        <>
          <Divider orientation="left">
            <Space>
              <CrownOutlined style={{ color: '#faad14' }} />
              <span>会员套餐</span>
            </Space>
          </Divider>
          
          <Row gutter={[24, 24]}>
            {membershipPlans.map((plan, index) => (
              <Col xs={24} md={8} key={index}>
                <Badge.Ribbon text="推荐" color="gold" style={{ display: plan.recommended ? 'block' : 'none' }}>
                  <Card 
                    hoverable
                    style={{ 
                      height: '100%',
                      borderColor: plan.recommended ? '#faad14' : undefined,
                      transform: plan.recommended ? 'translateY(-10px)' : undefined
                    }}
                  >
                    <div style={{ textAlign: 'center' }}>
                      <Title level={3}>{plan.title}</Title>
                      <Title level={2} style={{ color: '#1890ff', margin: '16px 0' }}>
                        {plan.price}
                      </Title>
                      
                      <List
                        style={{ textAlign: 'left', marginBottom: 24 }}
                        itemLayout="horizontal"
                        dataSource={plan.features}
                        renderItem={item => (
                          <List.Item>
                            <Space>
                              <CheckCircleOutlined style={{ color: '#52c41a' }} />
                              <span>{item}</span>
                            </Space>
                          </List.Item>
                        )}
                      />
                      
                      <Button 
                        type={plan.recommended ? 'primary' : 'default'} 
                        size="large"
                        block
                        icon={<CrownOutlined />}
                      >
                        {plan.buttonText}
                      </Button>
                    </div>
                  </Card>
                </Badge.Ribbon>
              </Col>
            ))}
          </Row>
        </>
      )}
      
      <Divider />
      
      <Row>
        <Col span={24} style={{ textAlign: 'center' }}>
          <Paragraph type="secondary">
            如有任何疑问，请联系我们的客服团队
          </Paragraph>
          <Paragraph type="secondary">
            邮箱: support@smartrag.com | 电话: 400-123-4567
          </Paragraph>
        </Col>
      </Row>
    </div>
  );
};

export default VipMembership;