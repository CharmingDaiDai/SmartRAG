import React, {useState} from 'react';
import {Alert, Button, Card, Col, Divider, Form, Input, message, Row, Typography} from 'antd';
import {GithubOutlined, LockOutlined, QqOutlined, UserOutlined, WechatOutlined} from '@ant-design/icons';
import {Link, useNavigate} from 'react-router-dom';
import {useAuth} from '../../context/AuthContext';
import '../../styles/auth.css';
import {authAPI} from '../../services/api';

const { Title, Text, Paragraph } = Typography;

const Login = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const onFinish = async (values) => {
    setLoading(true);
    setError('');
    try {
      // 调用认证上下文中的login函数，该函数会调用authAPI
      const success = await login({
        username: values.username,
        password: values.password
      });
      
      if (success) {
        message.success('登录成功！');
        navigate('/dashboard');
      } else {
        setError('登录失败，请检查用户名和密码');
      }
    } catch (err) {
      console.error('登录错误:', err);
      setError('登录时发生错误，请稍后再试');
    } finally {
      setLoading(false);
    }
  };

  const handleSocialLogin = async (platform) => {
    if (platform === 'GitHub') {
      try {
        setLoading(true);
        // 使用authAPI调用GitHub登录接口
        const response = await authAPI.githubLogin();
        
        if (response && response.data) {
          // 跳转到GitHub授权页面
          window.location.href = response.data;
        } else {
          message.error('获取GitHub授权链接失败');
        }
      } catch (error) {
        console.error('GitHub登录请求失败:', error);
        message.error('GitHub登录请求失败，请稍后再试');
      } finally {
        setLoading(false);
      }
    } else {
      message.info(`正在使用${platform}登录，此功能正在开发中...`);
      // 实际调用第三方登录API的代码会在这里
    }
  };

  return (
    <div className="login-container">
      <Row className="login-row">
        <Col xs={24} md={12} className="login-left">
          <div className="login-left-content">
            <div className="brand-logo">
              {/* Use the image from the public folder */}
              <img src="/logo192.png" alt="SmartRAG Logo" className="brand-logo-img" />
              <Title level={3} className="brand-title">SmartRAG</Title>
            </div>
            <Title level={2} className="welcome-title">欢迎回来</Title>
            <Paragraph className="welcome-subtitle">
              登录您的账户以使用智能RAG系统的全部功能
            </Paragraph>
            <img
              src="/login-illustration.svg"
              alt="Login"
              className="login-illustration"
              onError={(e) => {
                e.target.onerror = null;
                e.target.style.display = 'none';
              }}
            />
          </div>
        </Col>

        <Col xs={24} md={12} className="login-right">
          <Card
            bordered={false}
            className="login-card"
          >
            <div className="login-header">
              <Title level={3}>用户登录</Title>
              <Text type="secondary">请输入您的账户信息</Text>
            </div>

            {error && (
              <Alert
                message={error}
                type="error"
                showIcon
                style={{ marginBottom: 24 }}
              />
            )}

            <Form
              name="login"
              initialValues={{ remember: true }}
              onFinish={onFinish}
              size="large"
              layout="vertical"
            >
              <Form.Item
                name="username"
                rules={[{ required: true, message: '请输入您的用户名!' }]}
              >
                <Input
                  prefix={<UserOutlined />}
                  placeholder="用户名"
                />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[{ required: true, message: '请输入您的密码!' }]}
              >
                <Input.Password
                  prefix={<LockOutlined />}
                  placeholder="密码"
                />
              </Form.Item>

              <div className="remember-forgot">
                <Link to="/forgot-password" className="forgot-link">
                  忘记密码?
                </Link>
              </div>

              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  className="login-button"
                  loading={loading}
                >
                  登录
                </Button>
              </Form.Item>

              <Divider className="login-divider">
                <span className="divider-text">或使用社交账号登录</span>
              </Divider>

              <div className="social-login">
                <Button
                  type="default"
                  icon={<GithubOutlined />}
                  size="large"
                  className="social-button github"
                  onClick={() => handleSocialLogin('GitHub')}
                >
                  GitHub
                </Button>
                <Button
                  type="default"
                  icon={<WechatOutlined />}
                  size="large"
                  className="social-button wechat"
                  onClick={() => handleSocialLogin('微信')}
                >
                  微信
                </Button>
                <Button
                  type="default"
                  icon={<QqOutlined />}
                  size="large"
                  className="social-button qq"
                  onClick={() => handleSocialLogin('QQ')}
                >
                  QQ
                </Button>
              </div>

              <div className="register-prompt">
                <Text type="secondary">还没有账户? </Text>
                <Link to="/register" className="register-link">
                  立即注册
                </Link>
              </div>
            </Form>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Login;