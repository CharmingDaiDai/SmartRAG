import React from 'react';
import {Button, Card, Col, Divider, Row, Typography} from 'antd';
import {BulbOutlined, FileSearchOutlined, SafetyCertificateOutlined, ScanOutlined} from '@ant-design/icons';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../context/AuthContext';

const { Title, Text, Paragraph } = Typography;

const HomePage = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const features = [
    {
      icon: <FileSearchOutlined />,
      title: "智能文档检索",
      description: "基于多种 RAG 检索增强方法，实现精准文档智能搜索"
    },
    {
      icon: <ScanOutlined />,
      title: "关键词与摘要提取",
      description: "快速识别文档核心内容，提取关键词与摘要，把握文档要点"
    },
    {
      icon: <BulbOutlined />,
      title: "智能文档润色",
      description: "自动优化文档表述和格式，提升专业度和可读性"
    },
    {
      icon: <SafetyCertificateOutlined />,
      title: "敏感信息检测",
      description: "自动识别文档中的敏感信息，保障内容安全与合规"
    }
  ];

  return (
    <div className="home-container">
      {/* 主区域 - 简洁现代风格 */}
      <div className="hero-section">
        <div className="hero-content">
          <Title className="main-title">
            SmartRAG
          </Title>
          <div className="sub-title">智能文档检索与分析的新境界</div>
          
          <div className="features-section">
            <div className="features-heading">
              <Title level={3}>核心能力</Title>
              <div className="features-subtitle">基于大语言模型的智能文档处理系统</div>
            </div>
            
            <Row gutter={[24, 24]} className="features-cards">
              {features.map((feature, index) => (
                <Col xs={24} sm={12} key={index}>
                  <Card 
                    className="feature-card" 
                    onClick={() => isAuthenticated ? navigate('/dashboard') : navigate('/login')}
                    hoverable
                  >
                    <div className="card-icon-area">
                      {feature.icon}
                    </div>
                    <div className="card-title">
                      {feature.title}
                    </div>
                    <div className="card-description">
                      {feature.description}
                    </div>
                  </Card>
                </Col>
              ))}
            </Row>
            
            <div className="cta-section">
              <Button 
                type="primary"
                size="large"
                onClick={() => isAuthenticated ? navigate('/dashboard') : navigate('/login')}
              >
                {isAuthenticated ? '进入系统' : '立即体验'}
              </Button>
              
              {!isAuthenticated && (
                <Button 
                  size="large"
                  style={{ marginLeft: '16px' }}
                  onClick={() => navigate('/register')}
                >
                  注册账号
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 页脚部分 */}
      <footer className="home-footer">
        <div className="footer-container">
          <div className="footer-row">
            <div className="footer-section">
              <Title level={5}>产品功能</Title>
              <ul className="footer-links">
                <li>文档检索</li>
                <li>关键词提取</li>
                <li>文档润色</li>
                <li>敏感信息检测</li>
              </ul>
            </div>
            
            <div className="footer-section">
              <Title level={5}>解决方案</Title>
              <ul className="footer-links">
                <li>企业文档管理</li>
                <li>学术研究助手</li>
                <li>内容创作平台</li>
                <li>知识库构建</li>
              </ul>
            </div>
            
            <div className="footer-section">
              <Title level={5}>资源与支持</Title>
              <ul className="footer-links">
                <li>用户手册</li>
                <li>视频教程</li>
                <li>常见问题</li>
                <li>技术支持</li>
              </ul>
            </div>
            
            <div className="footer-section">
              <Title level={5}>关于我们</Title>
              <ul className="footer-links">
                <li>简介</li>
                <li>联系我们</li>
                <li>加入团队</li>
                <li>合作伙伴</li>
              </ul>
            </div>
          </div>
          
          <Divider style={{ margin: '24px 0', borderColor: '#e8e8e8' }} />
          
          <div className="copyright">
            <span>© 2025 智能RAG系统 V1.0 版权所有</span>
          </div>
        </div>
      </footer>
      
      {/* 添加CSS样式 */}
      <style jsx="true">{`
        .home-container {
          width: 100%;
          background: #fff;
          overflow: hidden;
          min-height: 100vh;
        }
        
        .hero-section {
          background: linear-gradient(160deg, #f0f8ff 0%, #e6f7ff 100%);
          padding: 80px 0;
          display: flex;
          justify-content: center;
          min-height: calc(100vh - 250px);
        }
        
        .hero-content {
          max-width: 1200px;
          width: 100%;
          padding: 0 24px;
          text-align: center;
        }
        
        .main-title {
          font-size: 64px !important;
          font-weight: 600 !important;
          color: #1890ff !important;
          margin-bottom: 16px !important;
          letter-spacing: 1px;
        }
        
        .sub-title {
          font-size: 24px;
          color: #666;
          margin-bottom: 64px;
        }
        
        .features-section {
          max-width: 1000px;
          margin: 0 auto;
        }
        
        .features-heading {
          margin-bottom: 40px;
        }
        
        .features-subtitle {
          font-size: 16px;
          color: #666;
          margin-top: 8px;
        }
        
        .features-cards {
          margin-bottom: 40px;
        }
        
        .feature-card {
          height: 100%;
          border-radius: 12px;
          background: white;
          box-shadow: 0 4px 12px rgba(0,0,0,0.08);
          cursor: pointer;
          transition: all 0.3s ease;
          text-align: center;
          padding: 30px 20px;
        }
        
        .feature-card:hover {
          transform: translateY(-5px);
          box-shadow: 0 8px 24px rgba(0,0,0,0.12);
        }
        
        .card-icon-area {
          width: 80px;
          height: 80px;
          margin: 0 auto 24px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: linear-gradient(140deg, #1890ff 0%, #096dd9 100%);
          color: white;
          font-size: 32px;
          border-radius: 12px;
        }
        
        .card-title {
          font-size: 20px;
          font-weight: 500;
          margin-bottom: 16px;
          color: #333;
        }
        
        .card-description {
          font-size: 14px;
          color: #666;
          line-height: 1.6;
        }
        
        .cta-section {
          margin-top: 40px;
        }
        
        .home-footer {
          background: #f7f7f7;
          padding: 48px 0 24px;
        }
        
        .footer-container {
          max-width: 1200px;
          margin: 0 auto;
          padding: 0 24px;
        }
        
        .footer-row {
          display: flex;
          flex-wrap: wrap;
          justify-content: space-between;
        }
        
        .footer-section {
          min-width: 160px;
          margin-bottom: 24px;
        }
        
        .footer-links {
          list-style: none;
          padding: 0;
          margin: 12px 0 0;
        }
        
        .footer-links li {
          margin-bottom: 8px;
          color: #666;
          cursor: pointer;
          font-size: 14px;
        }
        
        .footer-links li:hover {
          color: #1890ff;
        }
        
        .copyright {
          text-align: center;
          color: #999;
          font-size: 14px;
          padding: 12px 0;
        }
        
        /* 响应式调整 */
        @media (max-width: 768px) {
          .hero-section {
            padding: 48px 0;
          }
          
          .main-title {
            font-size: 48px !important;
          }
          
          .sub-title {
            font-size: 20px;
            margin-bottom: 40px;
          }
          
          .footer-row {
            flex-direction: column;
          }
          
          .footer-section {
            margin-bottom: 32px;
            width: 100%;
          }
        }
      `}</style>
    </div>
  );
};

export default HomePage;