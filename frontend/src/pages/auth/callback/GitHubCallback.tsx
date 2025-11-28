import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { message, Spin } from 'antd';
import { authService } from '../../../services/authService';
import { useAppStore } from '../../../store/useAppStore';

const GitHubCallback: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const code = searchParams.get('code');
  const state = searchParams.get('state');

  useEffect(() => {
    const exchange = async () => {
      if (!code || !state) {
        message.error('无效的回调参数');
        navigate('/login');
        return;
      }

      try {
        const res: any = await authService.exchangeToken(code, state);
        if (res.code === 200) {
          const token = res.data.accessToken;
          useAppStore.getState().setToken(token);
          await useAppStore.getState().initUser();
          message.success('GitHub 登录成功');
          navigate('/dashboard');
        } else {
          message.error(res.message || 'GitHub 登录失败');
          navigate('/login');
        }
      } catch (error) {
        console.error(error);
        message.error('登录过程中发生错误');
        navigate('/login');
      }
    };

    exchange();
  }, [code, state, navigate]);

  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100vh' 
    }}>
      <Spin size="large" tip="正在处理 GitHub 登录..." />
    </div>
  );
};

export default GitHubCallback;
