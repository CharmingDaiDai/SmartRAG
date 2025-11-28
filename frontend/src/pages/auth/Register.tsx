import React, { useState, useMemo } from 'react';
import {
  Form,
  Input,
  Button,
  Card,
  message,
  Typography,
  Row,
  Col,
  Space,
} from 'antd';
import {
  UserOutlined,
  LockOutlined,
  MailOutlined,
  EyeTwoTone,
  EyeInvisibleOutlined,
} from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../../services/authService';
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

const Register: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [password, setPassword] = useState('');

  const onFinish = async (values: any) => {
    setLoading(true);
    try {
      const res: any = await authService.register(values);
      if (res.code === 200) {
        message.success('注册成功，请登录');
        navigate('/login');
      } else {
        message.error(res.message || '注册失败');
      }
    } catch (error) {
      console.error(error);
      message.error('注册过程中发生错误');
    } finally {
      setLoading(false);
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
                  <img src="/logo.png" alt="SmartRAG" style={{ width: 64, height: 64, marginBottom: 12 }} />
                  <Title level={4}>加入 SmartDoc</Title>
                  <Text type="secondary">开启您的智能知识库之旅</Text>
                </div>
              </Col>

              <Col xs={24} sm={14}>
                <div className="auth-form">
                  <Title level={3}>创建新账户</Title>

                  <Form
                    name="register"
                    onFinish={onFinish}
                    layout="vertical"
                    size="large"
                    scrollToFirstError
                  >
                    <Form.Item
                      name="username"
                      label="用户名"
                      rules={[
                        { required: true, message: '请输入用户名!' },
                        { min: 3, message: '用户名至少3个字符' }
                      ]}
                    >
                      <Input prefix={<UserOutlined />} placeholder="用户名" />
                    </Form.Item>

                    <Form.Item
                      name="email"
                      label="邮箱"
                      rules={[
                        { type: 'email', message: '请输入有效的邮箱地址!' },
                        { required: true, message: '请输入邮箱!' },
                      ]}
                    >
                      <Input prefix={<MailOutlined />} placeholder="邮箱" />
                    </Form.Item>

                    <Form.Item
                      name="password"
                      label="密码"
                      rules={[
                        { required: true, message: '请输入密码!' },
                        { min: 6, message: '密码至少6个字符' }
                      ]}
                      hasFeedback
                    >
                      <Input.Password
                        prefix={<LockOutlined />}
                        placeholder="密码"
                        iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
                        onChange={(e) => setPassword(e.target.value)}
                      />
                    </Form.Item>

                    <PasswordStrength value={password} />

                    <Form.Item
                      name="confirm"
                      label="确认密码"
                      dependencies={['password']}
                      hasFeedback
                      rules={[
                        { required: true, message: '请确认密码!' },
                        ({ getFieldValue }) => ({
                          validator(_, value) {
                            if (!value || getFieldValue('password') === value) {
                              return Promise.resolve();
                            }
                            return Promise.reject(new Error('两次输入的密码不一致!'));
                          },
                        }),
                      ]}
                    >
                      <Input.Password prefix={<LockOutlined />} placeholder="确认密码" />
                    </Form.Item>

                    <Form.Item>
                      <Button type="primary" htmlType="submit" block loading={loading}>
                        注册
                      </Button>
                    </Form.Item>

                    <div style={{ textAlign: 'center' }}>
                      <Space>
                        <Text type="secondary">已有账户？</Text>
                        <Link to="/login">立即登录</Link>
                      </Space>
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

export default Register;
