import React, {useEffect, useState} from 'react';
import {Avatar, Badge, Button, Divider, Dropdown, Layout, Menu, Space, theme, Tooltip, Typography} from 'antd';
import {
    AppstoreOutlined,
    BellOutlined,
    BookOutlined,
    CrownOutlined,
    DashboardOutlined,
    FileTextOutlined,
    LogoutOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    QuestionCircleOutlined,
    SettingOutlined,
    UserOutlined
} from '@ant-design/icons';
import {Outlet, useLocation, useNavigate} from 'react-router-dom';
import {useAuth} from '../../context/AuthContext';
import '../../styles/components/layout.css'; // 我们将创建一个新的CSS文件用于布局样式

const { Header, Sider, Content } = Layout;
const { Title, Text } = Typography;

const AppLayout = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [notifications, setNotifications] = useState(2); // 模拟未读通知数量
  const { token } = theme.useToken();
  const { currentUser, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  
  // 窗口宽度监听，实现响应式侧边栏
  const [windowWidth, setWindowWidth] = useState(window.innerWidth);
  
  useEffect(() => {
    const handleResize = () => setWindowWidth(window.innerWidth);
    window.addEventListener('resize', handleResize);
    
    // 在移动设备上默认折叠侧边栏
    if (windowWidth < 768 && !collapsed) {
      setCollapsed(true);
    }
    
    return () => window.removeEventListener('resize', handleResize);
  }, [windowWidth]);
  
  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  // 用户菜单项配置 - 更好的视觉分组
  const userMenuItems = [
    {
      key: 'user-info',
      label: (
        <div style={{ padding: '8px 0' }}>
          <div style={{ display: 'flex', alignItems: 'center', padding: '0 8px 8px' }}>
            <Avatar 
              size={48}
              icon={!currentUser?.avatarUrl && <UserOutlined />} 
              src={currentUser?.avatarUrl}
              style={{ marginRight: 12 }}
            />
            <div>
              <Text strong style={{ fontSize: '16px', display: 'block' }}>{currentUser?.username || '用户'}</Text>
              <Text type="secondary" style={{ fontSize: '12px' }}>{currentUser?.email || '未设置邮箱'}</Text>
            </div>
          </div>
          <Divider style={{ margin: '0 0 8px' }} />
        </div>
      ),
      type: 'group'
    },
    {
      key: 'profile',
      label: '个人资料',
      icon: <UserOutlined />,
      onClick: () => navigate('/profile')
    },
    {
      key: 'vip',
      label: (
        <span>
          VIP会员中心
          {currentUser?.isVip && <Badge dot style={{ marginLeft: 4 }} />}
        </span>
      ),
      icon: <CrownOutlined style={{ color: currentUser?.isVip ? '#faad14' : undefined }} />,
      onClick: () => navigate('/vip/membership')
    },
    {
      key: 'settings',
      label: '账户设置',
      icon: <SettingOutlined />,
      onClick: () => navigate('/profile/settings')
    },
    {
      type: 'divider'
    },
    {
      key: 'help',
      label: '帮助中心',
      icon: <QuestionCircleOutlined />,
      onClick: () => window.open('/docs/help', '_blank')
    },
    {
      type: 'divider'
    },
    {
      key: 'logout',
      label: '退出登录',
      icon: <LogoutOutlined />,
      danger: true,
      onClick: handleLogout
    }
  ];

  // 通知菜单
  const notificationItems = [
    {
      key: 'notification-header',
      label: (
        <div style={{ padding: '8px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Text strong>通知中心</Text>
            <Button type="link" size="small">全部标为已读</Button>
          </div>
        </div>
      ),
      type: 'group'
    },
    {
      key: 'notification-1',
      label: (
        <div>
          <Text strong>系统更新</Text>
          <div><Text type="secondary">智能RAG系统已更新到最新版本</Text></div>
          <div><Text type="secondary" style={{ fontSize: '12px' }}>30分钟前</Text></div>
        </div>
      )
    },
    // {
    //   key: 'notification-2',
    //   label: (
    //     <div>
    //       <Text strong>文档分析完成</Text>
    //       <div><Text type="secondary">您的文档"项目计划书"分析已完成</Text></div>
    //       <div><Text type="secondary" style={{ fontSize: '12px' }}>2小时前</Text></div>
    //     </div>
    //   )
    // },
    {
      key: 'notification-2',
      label: (
        <div>
          <Text strong>新功能上线</Text>
          <div><Text type="secondary">批量处理功能已上线，立即体验</Text></div>
          <div><Text type="secondary" style={{ fontSize: '12px' }}>1天前</Text></div>
        </div>
      )
    },
    {
      key: 'view-all',
      label: <div style={{ textAlign: 'center' }}>查看所有通知</div>,
    }
  ];

  // 主侧边栏菜单项配置
  const sideMenuItems = [
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: '仪表盘',
    },
    {
      key: '/documents',
      icon: <FileTextOutlined />,
      label: '文档管理',
    },
    {
      key: 'knowledge_base',
      icon: <BookOutlined />,
      label: '知识库',
      children: [
        {
          key: '/knowledge_base',
          label: '知识库管理',
        },
        // {
        //   key: '/knowledge_base/rag',
        //   label: '知识库问答 (基础版)',
        // },
        {
          key: '/knowledge_base/rag-x',
          label: '知识库问答',
        },
      ],
    },
    {
      key: 'analysis',
      icon: <AppstoreOutlined />,
      label: '文档分析',
      children: [
        {
          key: '/analysis/summary',
          label: '文档摘要',
        },
        {
          key: '/analysis/keywords',
          label: '关键词提取',
        },
        {
          key: '/analysis/polish',
          label: '文档润色',
        },
        {
          key: '/analysis/security',
          label: '敏感信息检测',
        },
        {
          key: '/analysis/',
          label: '文档查重',
        }
      ],
    }
  ];

  return (
    <Layout className="app-layout">
      <Sider 
        trigger={null} 
        collapsible 
        collapsed={collapsed}
        theme="light"
        className="app-sider"
        width={250}
        breakpoint="lg"
      >
        <div className={`logo-container ${collapsed ? 'collapsed' : ''}`}>
          {collapsed ? (
            <div className="logo-small">SD</div>
          ) : (
            <div className="logo-full">
              <Title level={3} style={{ margin: 0, color: '#1890ff' }}>SmartRAG</Title>
              <div className="logo-subtitle">智能RAG系统</div>
            </div>
          )}
        </div>
        
        <div className="sider-menu-container">
          <Menu
            theme="light"
            mode="inline"
            selectedKeys={[location.pathname]}
            defaultOpenKeys={['analysis', 'knowledge_base', 'vip']}
            items={sideMenuItems}
            onClick={({key}) => navigate(key)}
            className="sider-menu"
          />
        </div>
        
        {/* {!collapsed && (
          <div className="sider-bottom">
            {currentUser?.isVip ? (
              <div className="vip-badge">
                <CrownOutlined /> VIP会员
                <div className="vip-badge-subtitle">享受所有高级功能</div>
              </div>
            ) : (
              <Button 
                type="primary" 
                block 
                icon={<CrownOutlined />}
                onClick={() => navigate('/vip/membership')}
                className="upgrade-button"
              >
                升级到VIP
              </Button>
            )}
          </div>
        )} */}
      </Sider>
      
      <Layout>
        <Header className="app-header">
          <div className="header-left">
            <Tooltip title={collapsed ? '展开菜单' : '收起菜单'}>
              <Button
                type="text"
                icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
                onClick={() => setCollapsed(!collapsed)}
                className="menu-trigger"
              />
            </Tooltip>
            
            <div className="breadcrumb-placeholder">
              {/* 这里可以放置面包屑导航 */}
            </div>
          </div>
          
          <Space size={16} className="header-actions">
            <Tooltip title="帮助中心">
              <Button 
                type="text" 
                icon={<QuestionCircleOutlined />} 
                shape="circle"
                onClick={() => window.open('/docs/help', '_blank')}
              />
            </Tooltip>
            
            <Dropdown
              menu={{ items: notificationItems }}
              placement="bottomRight"
              trigger={['click']}
              arrow
              overlayClassName="notification-dropdown"
            >
              <Badge count={notifications} overflowCount={99} size="small">
                <Button 
                  type="text" 
                  icon={<BellOutlined />} 
                  shape="circle"
                  className="notification-button"
                />
              </Badge>
            </Dropdown>
            
            <Dropdown
              menu={{ items: userMenuItems }}
              placement="bottomRight"
              trigger={['click']}
              arrow
              overlayClassName="user-dropdown"
            >
              <div className="user-menu-trigger">
                {currentUser?.isVip ? (
                  <Badge count={<CrownOutlined style={{ color: '#faad14' }} />} offset={[-5, 5]}>
                    <Avatar 
                      icon={!currentUser?.avatarUrl && <UserOutlined />} 
                      src={currentUser?.avatarUrl}
                      className="user-avatar"
                    />
                  </Badge>
                ) : (
                  <Avatar 
                    icon={!currentUser?.avatarUrl && <UserOutlined />} 
                    src={currentUser?.avatarUrl}
                    className="user-avatar"
                  />
                )}
                <span className="username">{currentUser?.username || '用户'}</span>
              </div>
            </Dropdown>
          </Space>
        </Header>
        
        <Content className="app-content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default AppLayout;