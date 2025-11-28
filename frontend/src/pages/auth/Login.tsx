import React, { useState } from 'react';
import { Form, Input, Button, Card, message, Typography, Divider } from 'antd';
import { UserOutlined, LockOutlined, GithubOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../../services/authService';
import { useAppStore } from '../../store/useAppStore';
import { FadeIn, SlideInUp } from '../../components/common/Motion';

const { Title, Text } = Typography;

const Login: React.FC = () => {
  const navigate = useNavigate();
  const login = useAppStore((state) => state.login);
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: any) => {
    setLoading(true);
    try {
      const res: any = await authService.login(values);
      if (res.code === 200) {
        message.success('登录成功');
        // 假设后端返回的数据结构包含 token 和 userInfo
        // 根据 OpenAPI，login 返回 AuthenticationResponse (accessToken, refreshToken)
        // 我们可能需要再调用一次 getProfile 来获取用户信息，或者后端 login 接口直接返回了用户信息
        // 这里假设我们需要先存 token，然后获取用户信息
        
        const token = res.data.accessToken;
        // 先临时存一下 token，以便 getProfile 能带上
        localStorage.setItem('token', token);
        
        // 获取用户信息 (如果 login 接口不返回用户信息的话)
        // 这里的逻辑取决于后端实现，通常 login 会返回 token
        // 我们在 useAppStore 里有一个 initUser 方法，或者我们可以手动调用 userService.getProfile
        // 为了简单起见，我们假设 login 成功后，我们调用 initUser 或者手动设置
        
        // 重新利用 store 的 login 方法
        // 由于 login 方法需要 user 对象，我们先获取它
        // 但是 store.login 会设置 token 和 user。
        
        // 让我们修改一下策略：
        // 1. 存 token
        // 2. 调用 initUser (它会拉取 profile 并更新 store)
        
        useAppStore.getState().setToken(token);
        await useAppStore.getState().initUser();
        
        navigate('/dashboard');
      } else {
        message.error(res.message || '登录失败');
      }
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleGithubLogin = async () => {
      try {
          const res: any = await authService.githubLogin();
          if (res.code === 200 && res.data) {
              window.location.href = res.data;
          }
      } catch (error) {
          message.error('获取 GitHub 授权链接失败');
      }
  };

  return (
    <FadeIn>
        <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        minHeight: '100vh',
        background: '#f0f2f5' 
        }}>
        <SlideInUp>
            <Card style={{ width: 400, boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
                <div style={{ textAlign: 'center', marginBottom: 24 }}>
                <Title level={3}>Smart Doc</Title>
                <Text type="secondary">智能文档助手</Text>
                </div>
                
                <Form
                name="login"
                initialValues={{ remember: true }}
                onFinish={onFinish}
                size="large"
                >
                <Form.Item
                    name="username"
                    rules={[{ required: true, message: '请输入用户名!' }]}
                >
                    <Input prefix={<UserOutlined />} placeholder="用户名" />
                </Form.Item>

                <Form.Item
                    name="password"
                    rules={[{ required: true, message: '请输入密码!' }]}
                >
                    <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                </Form.Item>

                <Form.Item>
                    <Button type="primary" htmlType="submit" block loading={loading}>
                    登录
                    </Button>
                </Form.Item>
                
                <div style={{ textAlign: 'center' }}>
                    <Link to="/register">注册账户</Link>
                </div>

                <Divider plain>其他登录方式</Divider>
                
                <div style={{ display: 'flex', justifyContent: 'center' }}>
                    <Button icon={<GithubOutlined />} onClick={handleGithubLogin}>GitHub</Button>
                </div>
                </Form>
            </Card>
        </SlideInUp>
        </div>
    </FadeIn>
  );
};

export default Login;
