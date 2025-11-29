import React, { useState } from 'react';
import { Layout, Menu, Dropdown, Avatar, theme, Button, Tooltip } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { 
  DashboardOutlined, 
  FileTextOutlined, 
  ReadOutlined, 
  RobotOutlined, 
  UserOutlined, 
  LogoutOutlined, 
  MenuUnfoldOutlined, 
  MenuFoldOutlined, 
  ExperimentOutlined,
  SunOutlined,
  MoonOutlined
} from '@ant-design/icons';
import { useAppStore } from '../store/useAppStore';

const { Header, Sider, Content } = Layout;

export default function BasicLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { userInfo, themeMode, toggleTheme } = useAppStore();
  const [collapsed, setCollapsed] = useState(false);
  const {
    token: { colorBgContainer, borderRadiusLG },
  } = theme.useToken();

  const menuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘',
      onClick: () => navigate('/dashboard'),
    },
    {
      key: '/documents',
      icon: <FileTextOutlined />,
      label: '文档管理',
      onClick: () => navigate('/documents'),
    },
    {
      key: '/kb',
      icon: <ReadOutlined />,
      label: '知识库管理',
      onClick: () => navigate('/kb'),
    },
    {
      key: '/chat',
      icon: <RobotOutlined />,
      label: '知识库问答',
      onClick: () => navigate('/chat'),
    },
    {
      key: '/test-chat',
      icon: <ExperimentOutlined />,
      label: '测试问答',
      onClick: () => navigate('/test-chat'),
    },
  ];

  const userMenu = {
    items: [
        {
            key: 'profile',
            icon: <UserOutlined />,
            label: '个人资料',
            onClick: () => navigate('/profile'),
        },
        {
            key: 'logout',
            icon: <LogoutOutlined />,
            label: '退出登录',
            onClick: () => {
                // TODO: Clear token
                navigate('/login');
            },
        },
    ],
  };

  return (
    <Layout style={{ height: '100vh', overflow: 'hidden' }}>
      <Sider 
        trigger={null} 
        collapsible 
        collapsed={collapsed} 
        theme={themeMode === 'dark' ? 'dark' : 'light'} 
        style={{ 
            borderRight: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0' 
        }}
      >
        <div className="demo-logo-vertical" style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', borderBottom: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0' }}>
            <img src="/logo.png" alt="logo" style={{ height: 32 }} />
            {!collapsed && <span style={{ marginLeft: 8, fontWeight: 'bold', fontSize: 18, color: themeMode === 'dark' ? '#fff' : 'inherit' }}>SmartRAG</span>}
        </div>
        <Menu
          theme={themeMode === 'dark' ? 'dark' : 'light'}
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          style={{ borderRight: 0 }}
        />
      </Sider>
      <Layout style={{ height: '100%', overflow: 'hidden' }}>
        <Header style={{ padding: '0 24px', background: colorBgContainer, display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: themeMode === 'dark' ? '1px solid #303030' : '1px solid #f0f0f0' }}>
          {React.createElement(collapsed ? MenuUnfoldOutlined : MenuFoldOutlined, {
            className: 'trigger',
            onClick: () => setCollapsed(!collapsed),
            style: { fontSize: '18px', cursor: 'pointer' }
          })}
          
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
             <Tooltip title={themeMode === 'light' ? '切换暗色模式' : '切换亮色模式'}>
                <Button 
                    type="text" 
                    icon={themeMode === 'light' ? <MoonOutlined /> : <SunOutlined />} 
                    onClick={toggleTheme}
                    style={{ fontSize: 16 }}
                />
             </Tooltip>
             <Dropdown menu={userMenu}>
                <div style={{ cursor: 'pointer', display: 'flex', alignItems: 'center' }}>
                    <Avatar size="small" src={userInfo?.avatarUrl || userInfo?.avatar} icon={<UserOutlined />} />
                    <span style={{ marginLeft: 8 }}>{userInfo?.username || 'User'}</span>
                </div>
             </Dropdown>
          </div>
        </Header>
        <Content
          style={{
            margin: (location.pathname === '/chat' || location.pathname === '/test-chat') ? 0 : '24px 16px',
            padding: (location.pathname === '/chat' || location.pathname === '/test-chat') ? 0 : 24,
            minHeight: 280,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
            overflow: (location.pathname === '/chat' || location.pathname === '/test-chat') ? 'hidden' : 'auto',
            height: (location.pathname === '/chat' || location.pathname === '/test-chat') ? 'calc(100vh - 64px)' : undefined,
            flex: (location.pathname === '/chat' || location.pathname === '/test-chat') ? 'none' : 1,
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
