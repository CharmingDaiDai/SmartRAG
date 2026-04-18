import { useState, useEffect } from 'react';
import { Layout, Menu, Dropdown, Avatar, theme, Button, Typography, Drawer } from 'antd';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  DashboardOutlined,
  FileTextOutlined,
  ReadOutlined,
  RobotOutlined,
  UserOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MenuOutlined,
} from '@ant-design/icons';
import { motion } from 'framer-motion';
import { useAppStore } from '../store/useAppStore';
import ThemePopover from '../components/ThemePopover';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const PAGE_NAMES: Record<string, string> = {
  '/dashboard': '仪表盘',
  '/documents': '文档管理',
  '/kb': '知识库管理',
  '/chat': '知识库问答',
  '/profile': '个人资料',
};

// Breakpoint: treat ≤768px as mobile
const MOBILE_BREAKPOINT = 768;

function useIsMobile() {
  const [isMobile, setIsMobile] = useState(() => window.innerWidth <= MOBILE_BREAKPOINT);
  useEffect(() => {
    const handler = () => setIsMobile(window.innerWidth <= MOBILE_BREAKPOINT);
    window.addEventListener('resize', handler);
    return () => window.removeEventListener('resize', handler);
  }, []);
  return isMobile;
}

// Shared sidebar nav content — used by both Sider (desktop) and Drawer (mobile)
function SidebarContent({
  collapsed,
  token,
  menuItems,
  selectedKey,
}: {
  collapsed: boolean;
  token: ReturnType<typeof theme.useToken>['token'];
  menuItems: any[];
  selectedKey: string;
}) {
  return (
    <>
      {/* Logo 区域 */}
      <div style={{
        height: 68,
        display: 'flex',
        alignItems: 'center',
        justifyContent: collapsed ? 'center' : 'flex-start',
        padding: collapsed ? '0' : '0 18px',
        borderBottom: `1px solid ${token.colorBorderSecondary}`,
        gap: 10,
        overflow: 'hidden',
        flexShrink: 0,
      }}>
        <img
          src="/logo.png"
          alt="SmartRAG logo"
          style={{ height: 28, width: 28, flexShrink: 0, borderRadius: 6 }}
          onError={(e) => {
            (e.target as HTMLImageElement).style.display = 'none';
          }}
        />
        {!collapsed && (
          <motion.span
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -8 }}
            transition={{ duration: 0.2 }}
            style={{
              fontWeight: 600,
              fontSize: 16,
              letterSpacing: '0.02em',
              color: token.colorText,
              whiteSpace: 'nowrap',
            }}
          >
            SmartRAG
          </motion.span>
        )}
      </div>

      {/* 导航菜单 */}
      <Menu
        mode="inline"
        selectedKeys={[selectedKey]}
        items={menuItems}
        style={{
          borderRight: 0,
          paddingTop: 8,
          flex: 1,
          overflow: 'auto',
          background: 'transparent',
        }}
      />
    </>
  );
}

export default function BasicLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { userInfo, logout } = useAppStore();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const { token } = theme.useToken();
  const isMobile = useIsMobile();

  // Close drawer on route change
  useEffect(() => {
    setMobileDrawerOpen(false);
  }, [location.pathname]);

  // Auto-collapse sidebar on resize to small
  useEffect(() => {
    if (isMobile) {
      setCollapsed(true);
    }
  }, [isMobile]);

  const isFullPage = location.pathname === '/chat';

  const currentPageName = Object.entries(PAGE_NAMES).find(([key]) =>
    location.pathname === key || location.pathname.startsWith(key + '/')
  )?.[1] || '';

  const makeMenuItems = (isDrawer = false) => [
    {
      type: 'group' as const,
      label: (!collapsed || isDrawer) ? '工作台' : '',
      children: [
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
      ],
    },
    {
      type: 'group' as const,
      label: (!collapsed || isDrawer) ? '知识库' : '',
      children: [
        {
          key: '/kb',
          icon: <ReadOutlined />,
          label: '知识库管理',
          onClick: () => navigate('/kb'),
        },
      ],
    },
    {
      type: 'group' as const,
      label: (!collapsed || isDrawer) ? 'AI 工具' : '',
      children: [
        {
          key: '/chat',
          icon: <RobotOutlined />,
          label: '知识库问答',
          onClick: () => navigate('/chat'),
        },
      ],
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
        type: 'divider' as const,
      },
      {
        key: 'logout',
        icon: <LogoutOutlined />,
        label: '退出登录',
        danger: true,
        onClick: () => {
          logout();
          navigate('/login');
        },
      },
    ],
  };

  const headerHeight = 56;

  return (
    <Layout style={{ height: '100dvh', overflow: 'hidden', background: token.colorBgLayout }}>
      {/* ── Desktop Sider ── */}
      {!isMobile && (
        <Sider
          trigger={null}
          collapsible
          collapsed={collapsed}
          width={232}
          collapsedWidth={72}
          style={{
            borderRight: '1px solid var(--color-border)',
            overflow: 'hidden',
            background: 'var(--glass-surface-nav)',
            backdropFilter: 'blur(var(--glass-blur-nav)) saturate(135%)',
            WebkitBackdropFilter: 'blur(var(--glass-blur-nav)) saturate(135%)',
          }}
        >
          <SidebarContent
            collapsed={collapsed}
            token={token}
            menuItems={makeMenuItems(false)}
            selectedKey={location.pathname}
          />
        </Sider>
      )}

      {/* ── Mobile Drawer ── */}
      {isMobile && (
        <Drawer
          open={mobileDrawerOpen}
          onClose={() => setMobileDrawerOpen(false)}
          placement="left"
          width={240}
          closeIcon={null}
          styles={{
            body: {
              padding: 0,
              display: 'flex',
              flexDirection: 'column',
              background: 'var(--glass-surface-nav)',
              backdropFilter: 'blur(var(--glass-blur-nav)) saturate(135%)',
              WebkitBackdropFilter: 'blur(var(--glass-blur-nav)) saturate(135%)',
            },
            header: { display: 'none' },
          }}
          style={{ zIndex: 1001 }}
        >
          <SidebarContent
            collapsed={false}
            token={token}
            menuItems={makeMenuItems(true)}
            selectedKey={location.pathname}
          />
        </Drawer>
      )}

      <Layout style={{ height: '100%', overflow: 'hidden' }}>
        {/* ── 顶部栏 ── */}
        <Header style={{
          padding: isMobile ? '0 12px' : '0 20px',
          background: 'var(--glass-surface-nav)',
          backdropFilter: 'blur(var(--glass-blur-nav)) saturate(135%)',
          WebkitBackdropFilter: 'blur(var(--glass-blur-nav)) saturate(135%)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid var(--color-border)',
          height: headerHeight,
          flexShrink: 0,
        }}>
          {/* 左侧：折叠/菜单按钮 + 页面标题 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: isMobile ? 8 : 12 }}>
            <Button
              type="text"
              aria-label={
                isMobile
                  ? '打开导航菜单'
                  : (collapsed ? '展开侧边栏' : '收起侧边栏')
              }
              icon={
                isMobile
                  ? <MenuOutlined />
                  : (collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />)
              }
              onClick={() => {
                if (isMobile) {
                  setMobileDrawerOpen(true);
                } else {
                  setCollapsed(!collapsed);
                }
              }}
              style={{
                fontSize: 16,
                color: token.colorTextSecondary,
                width: 36,
                height: 36,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            />
            {currentPageName && (
              <Text style={{
                fontSize: isMobile ? 14 : 15,
                fontWeight: 600,
                color: token.colorText,
                letterSpacing: '0.015em',
              }}>
                {currentPageName}
              </Text>
            )}
          </div>

          {/* 右侧：主题切换 + 用户菜单 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            {/* 个性化设置 */}
            <ThemePopover />

            {/* 用户下拉菜单 */}
            <Dropdown menu={userMenu} placement="bottomRight" arrow>
              <div
                role="button"
                tabIndex={0}
                aria-label="打开用户菜单"
                aria-haspopup="menu"
                style={{
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: isMobile ? '4px 6px' : '4px 8px',
                  borderRadius: 8,
                  transition: 'background 0.15s ease',
                }}
                onMouseEnter={(e) => {
                  (e.currentTarget as HTMLElement).style.background = token.colorFill;
                }}
                onMouseLeave={(e) => {
                  (e.currentTarget as HTMLElement).style.background = 'transparent';
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    (e.currentTarget as HTMLElement).click();
                  }
                }}
              >
                <Avatar
                  size={28}
                  src={userInfo?.avatarUrl || userInfo?.avatar}
                  icon={<UserOutlined />}
                  style={{ flexShrink: 0 }}
                />
                {!isMobile && (
                  <Text style={{
                    fontSize: 13,
                    fontWeight: 500,
                    color: token.colorTextSecondary,
                    maxWidth: 80,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap',
                  }}>
                    {userInfo?.username || 'User'}
                  </Text>
                )}
              </div>
            </Dropdown>
          </div>
        </Header>

        {/* ── 内容区 ── */}
        <Content
          style={{
            margin: isFullPage ? 0 : (isMobile ? '16px 12px' : '24px 32px'),
            padding: 0,
            background: isFullPage ? token.colorBgLayout : 'transparent',
            backdropFilter: 'none',
            WebkitBackdropFilter: 'none',
            borderRadius: 0,
            border: 'none',
            overflow: isFullPage ? 'hidden' : 'auto',
            height: isFullPage ? `calc(100dvh - ${headerHeight}px)` : undefined,
            flex: isFullPage ? 'none' : 1,
            minHeight: 0,
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
