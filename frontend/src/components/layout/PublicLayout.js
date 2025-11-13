import React from 'react';
import {Avatar, Button, Dropdown, Layout, Space} from 'antd';
import {LoginOutlined, LogoutOutlined, UserOutlined} from '@ant-design/icons';
import {Outlet, useNavigate} from 'react-router-dom';
import {useAuth} from '../../context/AuthContext';
import './layout-styles.css';

const { Content, Footer } = Layout;

const PublicLayout = () => {
  const { isAuthenticated, currentUser, logout } = useAuth();
  const navigate = useNavigate();

  const userMenuItems = [
    {
      key: 'dashboard',
      label: '进入系统',
      onClick: () => navigate('/dashboard')
    },
    {
      key: 'profile',
      label: '个人资料',
      onClick: () => navigate('/profile')
    },
    {
      key: 'logout',
      label: '退出登录',
      danger: true,
      icon: <LogoutOutlined />,
      onClick: () => {
        logout();
        navigate('/');
      }
    }
  ];

  return (
    <Layout className="public-layout">
      {/* 移除了Header，改为放置一个右上角悬浮的登录按钮 */}
      <div className="floating-auth-actions">
        {isAuthenticated ? (
          <Dropdown
            menu={{ items: userMenuItems }}
            placement="bottomRight"
            trigger={['click']}
          >
            <Space className="user-dropdown-trigger">
              <Avatar 
                icon={<UserOutlined />}
                src={currentUser?.avatarUrl}
                alt={currentUser?.username}
              />
              <span>{currentUser?.username}</span>
            </Space>
          </Dropdown>
        ) : (
          <Space>
            <Button
              type="primary"
              ghost
              icon={<LoginOutlined />}
              onClick={() => navigate('/login')}
            >
              登录
            </Button>
            <Button 
              type="primary"
              onClick={() => navigate('/register')}
            >
              注册
            </Button>
          </Space>
        )}
      </div>
      
      <Content>
        <Outlet />
      </Content>
      
      <Footer style={{ textAlign: 'center' }}>
        智能RAG系统 V1.0 ©2025 Created by NCUT
      </Footer>
      
      <style jsx="true">{`
        .public-layout {
          min-height: 100vh;
          background: transparent;
        }
        
        .floating-auth-actions {
          position: fixed;
          top: 20px;
          right: 20px;
          z-index: 999;
          background: rgba(255, 255, 255, 0.9);
          padding: 8px 16px;
          border-radius: 24px;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
        
        .user-dropdown-trigger {
          cursor: pointer;
        }
      `}</style>
    </Layout>
  );
};

export default PublicLayout;