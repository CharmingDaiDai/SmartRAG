import { useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, theme } from 'antd';
import BasicLayout from './layouts/BasicLayout';
import DocumentsPage from './pages/documents';
import KnowledgeBasePage from './pages/KnowledgeBase';
import KnowledgeBaseDetail from './pages/KnowledgeBase/KnowledgeBaseDetail';
import ChatPage from './pages/Chat';
import UserProfile from './pages/User';
import Dashboard from './pages/Dashboard';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import GitHubCallback from './pages/auth/callback/GitHubCallback';
import ProtectedRoute from './components/ProtectedRoute';
import { useAppStore } from './store/useAppStore';

const FONT_FAMILY = "'LXGW WenKai Screen', 'PingFang SC', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif";
const FONT_FAMILY_CODE = "'JetBrains Mono', 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace";

const lightToken = {
  colorPrimary: '#6366f1',
  colorSuccess: '#10b981',
  colorWarning: '#f59e0b',
  colorError: '#ef4444',
  colorInfo: '#6366f1',

  colorBgBase: '#ffffff',
  colorBgContainer: '#ffffff',
  colorBgElevated: '#ffffff',
  colorBgLayout: '#fafaf9',
  colorBgSpotlight: '#f5f5f4',
  colorBgMask: 'rgba(28,25,23,0.45)',

  colorBorder: '#e7e5e4',
  colorBorderSecondary: '#f5f5f4',

  colorText: '#1c1917',
  colorTextSecondary: '#57534e',
  colorTextTertiary: '#a8a29e',
  colorTextQuaternary: '#d6d3d1',
  colorTextDescription: '#78716c',
  colorTextDisabled: '#d6d3d1',
  colorTextHeading: '#1c1917',
  colorTextLabel: '#44403c',
  colorTextPlaceholder: '#a8a29e',

  colorFill: '#f5f5f4',
  colorFillSecondary: '#fafaf9',
  colorFillTertiary: '#fafaf9',
  colorFillAlter: '#fafaf9',
  colorFillQuaternary: 'rgba(245,245,244,0.6)',

  colorSplit: '#e7e5e4',

  borderRadius: 8,
  borderRadiusXS: 4,
  borderRadiusSM: 6,
  borderRadiusLG: 12,
  borderRadiusOuter: 16,

  fontFamily: FONT_FAMILY,
  fontFamilyCode: FONT_FAMILY_CODE,
  fontSize: 14,
  fontSizeSM: 12,
  fontSizeLG: 16,
  fontSizeXL: 20,
  fontSizeHeading1: 32,
  fontSizeHeading2: 24,
  fontSizeHeading3: 20,
  fontSizeHeading4: 16,
  fontSizeHeading5: 14,

  lineHeight: 1.75,
  lineHeightSM: 1.5,
  lineHeightLG: 1.8,
  lineHeightHeading1: 1.3,
  lineHeightHeading2: 1.35,
  lineHeightHeading3: 1.4,
  lineHeightHeading4: 1.5,
  lineHeightHeading5: 1.6,

  padding: 16,
  paddingSM: 12,
  paddingXS: 8,
  paddingXXS: 4,
  paddingLG: 24,
  paddingXL: 32,

  margin: 16,
  marginSM: 12,
  marginXS: 8,
  marginXXS: 4,
  marginLG: 24,
  marginXL: 32,

  controlHeight: 36,
  controlHeightSM: 28,
  controlHeightLG: 44,

  boxShadow: '0 4px 12px rgba(28,25,23,0.08), 0 2px 4px rgba(28,25,23,0.04)',
  boxShadowSecondary: '0 1px 3px rgba(28,25,23,0.08)',
  boxShadowTertiary: '0 8px 24px rgba(28,25,23,0.10)',

  motionDurationFast: '0.15s',
  motionDurationMid: '0.25s',
  motionDurationSlow: '0.35s',
  motionEaseInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
  motionEaseOut: 'cubic-bezier(0, 0, 0.2, 1)',
  motionEaseIn: 'cubic-bezier(0.4, 0, 1, 1)',
};

const darkToken = {
  ...lightToken,
  colorBgBase: '#141210',
  colorBgContainer: '#1c1917',
  colorBgElevated: '#292524',
  colorBgLayout: '#141210',
  colorBgSpotlight: '#292524',
  colorBgMask: 'rgba(0,0,0,0.65)',

  colorBorder: 'rgba(255,255,255,0.08)',
  colorBorderSecondary: 'rgba(255,255,255,0.04)',

  colorText: '#f5f5f4',
  colorTextSecondary: '#a8a29e',
  colorTextTertiary: '#78716c',
  colorTextQuaternary: '#57534e',
  colorTextDescription: '#a8a29e',
  colorTextDisabled: '#44403c',
  colorTextHeading: '#fafaf9',
  colorTextLabel: '#d6d3d1',
  colorTextPlaceholder: '#78716c',

  colorFill: 'rgba(255,255,255,0.06)',
  colorFillSecondary: 'rgba(255,255,255,0.04)',
  colorFillTertiary: 'rgba(255,255,255,0.02)',
  colorFillAlter: 'rgba(255,255,255,0.04)',
  colorFillQuaternary: 'rgba(255,255,255,0.02)',

  colorSplit: 'rgba(255,255,255,0.08)',

  boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
  boxShadowSecondary: '0 1px 3px rgba(0,0,0,0.3)',
  boxShadowTertiary: '0 8px 24px rgba(0,0,0,0.5)',
};

const getComponentTokens = (isDark: boolean) => ({
  Menu: {
    itemBorderRadius: 8,
    itemHeight: 40,
    itemSelectedBg: 'rgba(99,102,241,0.10)',
    itemSelectedColor: '#6366f1',
    itemHoverBg: 'rgba(99,102,241,0.06)',
    itemHoverColor: isDark ? '#f5f5f4' : '#1c1917',
    itemActiveBg: 'rgba(99,102,241,0.12)',
    iconSize: 16,
    iconMarginInlineEnd: 10,
    collapsedIconSize: 18,
    itemPaddingInline: 14,
  },
  Card: {
    borderRadius: 12,
    borderRadiusLG: 16,
    paddingLG: 20,
  },
  Button: {
    borderRadius: 8,
    borderRadiusSM: 6,
    controlHeight: 36,
    controlHeightSM: 28,
    controlHeightLG: 44,
    fontWeight: 500,
    paddingInline: 16,
    paddingInlineSM: 12,
    paddingInlineLG: 20,
  },
  Input: {
    borderRadius: 8,
    controlHeight: 36,
    paddingInline: 12,
    activeShadow: '0 0 0 3px rgba(99,102,241,0.12)',
  },
  Select: {
    borderRadius: 8,
    controlHeight: 36,
    optionSelectedBg: 'rgba(99,102,241,0.08)',
    optionSelectedColor: '#6366f1',
  },
  Table: {
    borderRadius: 12,
    cellPaddingBlock: 12,
    cellPaddingInline: 16,
  },
  Modal: {
    borderRadius: 16,
    paddingContentHorizontalLG: 28,
    paddingMD: 24,
    titleFontSize: 16,
  },
  Tag: {
    borderRadius: 6,
    paddingInline: 8,
  },
  Slider: {
    railBg: '#e7e5e4',
    railHoverBg: '#d6d3d1',
    trackBg: '#6366f1',
    handleColor: '#6366f1',
    railSize: 4,
    handleSize: 14,
    handleSizeHover: 16,
  },
  Switch: {
    colorPrimary: '#6366f1',
    colorPrimaryHover: '#4f46e5',
    handleSize: 18,
    trackHeight: 22,
    trackMinWidth: 44,
  },
  Progress: {
    defaultColor: '#6366f1',
    remainingColor: '#e7e5e4',
    lineBorderRadius: 4,
  },
  Form: {
    labelFontSize: 13,
    labelColor: isDark ? '#d6d3d1' : '#44403c',
    verticalLabelPadding: '0 0 6px',
  },
  Dropdown: {
    borderRadius: 10,
    paddingBlock: 5,
    controlPaddingHorizontal: 14,
  },
  Tooltip: {
    borderRadius: 8,
    colorBgDefault: isDark ? '#292524' : '#ffffff',
    colorTextLightSolid: isDark ? '#f5f5f4' : '#1c1917',
  },
  Divider: {
    marginLG: 20,
  },
  Statistic: {
    titleFontSize: 13,
    contentFontSize: 28,
    fontFamily: FONT_FAMILY,
  },
  Alert: {
    borderRadius: 10,
  },
});

function App() {
  const { initUser, themeMode } = useAppStore();

  useEffect(() => {
    initUser();
  }, [initUser]);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', themeMode);
  }, [themeMode]);

  return (
    <ConfigProvider
      theme={{
        algorithm: themeMode === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm,
        token: themeMode === 'dark' ? darkToken : lightToken,
        components: getComponentTokens(themeMode === 'dark'),
      }}
    >
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/auth/callback/github" element={<GitHubCallback />} />

        <Route path="/" element={
          <ProtectedRoute>
            <BasicLayout />
          </ProtectedRoute>
        }>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="documents" element={<DocumentsPage />} />
          <Route path="kb" element={<KnowledgeBasePage />} />
          <Route path="kb/:id" element={<KnowledgeBaseDetail />} />
          <Route path="chat" element={<ChatPage />} />
          <Route path="profile" element={<UserProfile />} />
        </Route>
      </Routes>
    </ConfigProvider>
  );
}

export default App;
