import React, { useState, useMemo } from 'react';
import {
  Form,
  Input,
  Button,
  Card,
  message,
  Typography,
  Divider,
  Row,
  Col,
  Checkbox,
  Space,
} from 'antd';
import {
  UserOutlined,
  LockOutlined,
  GithubOutlined,
  EyeTwoTone,
  EyeInvisibleOutlined,
} from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../../services/authService';
import { useAppStore } from '../../store/useAppStore';
import { FadeIn, SlideInUp } from '../../components/common/Motion';
import './login.css';

const { Title, Text } = Typography;

const PasswordStrength: React.FC<{ value?: string }> = ({ value }) => {
  const score = useMemo(() => {
    if (!value) return 0;
    let s = 0;
    if (value.length >= 8) s += 1;
    if (/[A-Z]/.test(value)) s += 1;
    if (/[0-9]/.test(value)) s += 1;
    if (/[^A-Za-z0-9]/.test(value)) s += 1;
    return s;
  }, [value]);

  const labels = ['太弱', '弱', '中等', '强'];
  return (
    <div className="pwd-strength">
      <div className={`bar bar-${score}`} />
      <Text type="secondary">
        {score > 0 ? labels[Math.max(0, score - 1)] : '未设置密码'}
      </Text>
    </div>
  );
};

const Login: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [password, setPassword] = useState('');

  const onFinish = async (values: any) => {
    setLoading(true);
    try {
      const res: any = await authService.login(values);
      if (res && res.code === 200) {
        message.success('登录成功');
        const token = res.data?.accessToken || res.data?.token || '';
        if (token) localStorage.setItem('token', token);
        if (useAppStore.getState().initUser) {
          await useAppStore.getState().initUser();
        }
        navigate('/dashboard');
      } else {
        message.error(res?.message || '登录失败，请检查凭证');
      }
    } catch (error) {
      console.error(error);
      message.error('登录过程中发生错误');
    } finally {
      setLoading(false);
    }
  };

  const handleGithubLogin = async () => {
    try {
      const res: any = await authService.githubLogin();
      if (res?.code === 200 && res.data) {
        window.location.href = res.data;
      } else {
        message.error('无法获取 GitHub 授权链接');
      }
    } catch (error) {
      message.error('获取 GitHub 授权链接失败');
    }
  };

  return (
    <FadeIn>
      <div className="auth-page">
        <SlideInUp>
          <Card className="auth-card">
            <Row gutter={24} align="middle">
              <Col xs={24} sm={10} className="auth-illustration">
                <div className="illus-box">
                  <div className="logo">SD</div>
                  <Title level={4}>欢迎使用 SmartDoc</Title>
                  <Text type="secondary">安全 · 智能 · 高效的知识检索与问答平台</Text>
                </div>
              </Col>

              <Col xs={24} sm={14}>
                <div className="auth-form">
                  <Title level={3}>登录到您的账户</Title>

                  <Form name="login" initialValues={{ remember: true }} onFinish={onFinish} layout="vertical">
                    <Form.Item
                      name="username"
                      label="用户名或邮箱"
                      rules={[{ required: true, message: '请输入用户名或邮箱' }]}
                    >
                      <Input prefix={<UserOutlined />} placeholder="用户名或邮箱" size="large" />
                    </Form.Item>

                    <Form.Item
                      name="password"
                      label="密码"
                      rules={[{ required: true, message: '请输入密码' }]}
                    >
                      <Input.Password
                        prefix={<LockOutlined />}
                        placeholder="密码"
                        size="large"
                        iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
                        onChange={(e) => setPassword(e.target.value)}
                      />
                    </Form.Item>

                    <PasswordStrength value={password} />

                    <Form.Item>
                      <div className="form-row">
                        <Checkbox defaultChecked>记住我</Checkbox>
                        <Link to="/auth/forgot" className="forgot-link">
                          忘记密码？
                        </Link>
                      </div>
                    </Form.Item>

                    <Form.Item>
                      <Button type="primary" htmlType="submit" block size="large" loading={loading}>
                        登录
                      </Button>
                    </Form.Item>

                    <div style={{ textAlign: 'center' }}>
                      <Space>
                        <Text type="secondary">还没有账号？</Text>
                        <Link to="/register">立即注册</Link>
                      </Space>
                    </div>

                    <Divider>其他登录方式</Divider>

                    <div className="social-row">
                      <Button icon={<GithubOutlined />} onClick={handleGithubLogin}>
                        使用 GitHub 登录
                      </Button>
                    </div>
                  </Form>
                </div>
              </Col>
            </Row>
          </Card>
        </SlideInUp>
      </div>
    </FadeIn>
  );
};

export default Login;
