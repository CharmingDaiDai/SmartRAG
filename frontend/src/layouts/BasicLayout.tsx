/**
 * 核心基础嵌套布局组件
 * 
 * 功能逻辑：
 * 1. 提供全局的三大金刚区域（Header 顶导, Sider 侧边栏/Drawer, Content 主内容区），除了登录/注册页外所有的子页面路由 (`<Outlet />`) 都在这里展开。
 * 2. 处理适配响应式布局，提供了一个 `useIsMobile` 的自定义 Hook。如果在终端设备或小窗口上渲染，原侧边栏抽屉式收起，呈现为 Drawer 展开形态。
 * 3. 顶层 Header 承载当前路径名显示、全局主题引擎 (`ThemePopover`) 和当前登录人头像管理（注销功能）。
 * 4. 特化全屏模式：对 `/chat` (RAG功能界面) 进行特判 (`isFullPage`)，屏蔽传统业务页面的 Padding 与内阴影盒子，留出版心进行 Edge-to-Edge 的流式聊天设计。
 */
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

// 当前应用中所有的导航地址与顶层大标题的映射
const PAGE_NAMES: Record<string, string> = {
  '/dashboard': '仪表盘',
  '/documents': '文档管理',
  '/kb': '知识库管理',
  '/chat': '知识库问答',
  '/profile': '个人资料',
};

// 预设移动端/小屏幕的分界阈值 (Breakpoint)
const MOBILE_BREAKPOINT = 768;

/**
 * 监听视口 Resize 的钩子，判断当前是否为狭窄布局环境
 */
function useIsMobile() {
  const [isMobile, setIsMobile] = useState(() => window.innerWidth <= MOBILE_BREAKPOINT);
  useEffect(() => {
    const handler = () => setIsMobile(window.innerWidth <= MOBILE_BREAKPOINT);
    window.addEventListener('resize', handler);
    return () => window.removeEventListener('resize', handler);
  }, []);
  return isMobile;
}

/**
 * 提取后的边栏渲染物
 * 无论是在桌面浏览器的固定版位中，还是在移动端的抽屉环境里，保持统一外观逻辑。
 */
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
      {/* 网站 Logo 图标与文案展示 */}
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
          // 基于 framer-motion 实现的展开折叠文字位移动画，避免生硬地跳字
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

      {/* 使用 Ant Design Menu 进行导航项绑定 */}
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

  // 当页面的路由变化时，如果是在移动端则顺手关掉左侧导航抽屉
  useEffect(() => {
    setMobileDrawerOpen(false);
  }, [location.pathname]);

  // 当窗口收拢到指定范围触发 isMobile 响应，顺势收起固定菜单栏
  useEffect(() => {
    if (isMobile) {
      setCollapsed(true);
    }
  }, [isMobile]);

  // 针对 Chat 界面的特判标志，后续决定是否让内容框贴着边缘 (100% 充满视口)
  const isFullPage = location.pathname === '/chat';

  // 动态找出顶部的汉化名称
  const currentPageName = Object.entries(PAGE_NAMES).find(([key]) =>
    location.pathname === key || location.pathname.startsWith(key + '/')
  )?.[1] || '';

  // Antd Menu 支持的声明式导航条目数组，`onClick` 用来发起单页跳转
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
          size={240}
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
