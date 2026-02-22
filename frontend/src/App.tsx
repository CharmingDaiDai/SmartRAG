import { useEffect, useMemo } from 'react';
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
import {
  COLOR_THEMES,
  FONT_FAMILIES,
  FONT_SIZES,
  FONT_FAMILY_CODE,
  UI_STYLES,
  type ColorThemeDef,
  type FontFamilyDef,
  type FontSizeDef,
  type UIStyleDef,
} from './config/themeConfig';

// ==================== Token 构建函数 ====================

function hexToRgb(hex: string): string {
  const h = hex.replace('#', '');
  const r = parseInt(h.substring(0, 2), 16);
  const g = parseInt(h.substring(2, 4), 16);
  const b = parseInt(h.substring(4, 6), 16);
  return `${r},${g},${b}`;
}

function buildToken(isDark: boolean, ct: ColorThemeDef, ff: FontFamilyDef, fs: FontSizeDef, us: UIStyleDef) {
  const shadowMul = us.shadowScale;
  const base = {
    colorPrimary: ct.primary,
    colorSuccess: '#10b981',
    colorWarning: '#f59e0b',
    colorError: '#ef4444',
    colorInfo: ct.primary,

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

    borderRadius: us.borderRadius,
    borderRadiusXS: us.borderRadiusXS,
    borderRadiusSM: us.borderRadiusSM,
    borderRadiusLG: us.borderRadiusLG,
    borderRadiusOuter: us.borderRadiusOuter,

    fontFamily: ff.value,
    fontFamilyCode: FONT_FAMILY_CODE,
    fontSize: fs.base,
    fontSizeSM: fs.sm,
    fontSizeLG: fs.lg,
    fontSizeXL: fs.xl,
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

    controlHeight: fs.controlHeight,
    controlHeightSM: fs.controlHeightSM,
    controlHeightLG: fs.controlHeightLG,

    boxShadow: `0 ${4 * shadowMul}px ${12 * shadowMul}px rgba(28,25,23,${(0.08 * shadowMul).toFixed(2)}), 0 ${2 * shadowMul}px ${4 * shadowMul}px rgba(28,25,23,${(0.04 * shadowMul).toFixed(2)})`,
    boxShadowSecondary: `0 ${1 * shadowMul}px ${3 * shadowMul}px rgba(28,25,23,${(0.08 * shadowMul).toFixed(2)})`,
    boxShadowTertiary: `0 ${8 * shadowMul}px ${24 * shadowMul}px rgba(28,25,23,${(0.10 * shadowMul).toFixed(2)})`,

    motionDurationFast: us.motionSpeed === 'fast' ? '0.1s' : '0.15s',
    motionDurationMid: us.motionSpeed === 'fast' ? '0.15s' : '0.25s',
    motionDurationSlow: us.motionSpeed === 'fast' ? '0.2s' : '0.35s',
    motionEaseInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
    motionEaseOut: 'cubic-bezier(0, 0, 0.2, 1)',
    motionEaseIn: 'cubic-bezier(0.4, 0, 1, 1)',
  };

  if (isDark) {
    return {
      ...base,
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

      boxShadow: `0 ${4 * shadowMul}px ${12 * shadowMul}px rgba(0,0,0,${(0.4 * shadowMul).toFixed(2)})`,
      boxShadowSecondary: `0 ${1 * shadowMul}px ${3 * shadowMul}px rgba(0,0,0,${(0.3 * shadowMul).toFixed(2)})`,
      boxShadowTertiary: `0 ${8 * shadowMul}px ${24 * shadowMul}px rgba(0,0,0,${(0.5 * shadowMul).toFixed(2)})`,
    };
  }

  return base;
}

function buildComponentTokens(isDark: boolean, ct: ColorThemeDef, ff: FontFamilyDef, fs: FontSizeDef, us: UIStyleDef) {
  const primarySoft = isDark ? ct.primaryLightSoft : ct.primarySoft;
  const primary = ct.primary;

  return {
    Menu: {
      itemBorderRadius: us.borderRadius,
      itemHeight: 40,
      itemSelectedBg: primarySoft,
      itemSelectedColor: primary,
      itemHoverBg: isDark ? 'rgba(255,255,255,0.06)' : `${primary}0F`,
      itemHoverColor: isDark ? '#f5f5f4' : '#1c1917',
      itemActiveBg: isDark ? 'rgba(255,255,255,0.08)' : `${primary}1F`,
      iconSize: 16,
      iconMarginInlineEnd: 10,
      collapsedIconSize: 18,
      itemPaddingInline: 14,
    },
    Card: {
      borderRadius: us.cardRadius,
      borderRadiusLG: us.cardRadius + 4,
      paddingLG: 20,
    },
    Button: {
      borderRadius: us.buttonRadius,
      borderRadiusSM: Math.max(us.buttonRadius - 2, 2),
      controlHeight: fs.controlHeight,
      controlHeightSM: fs.controlHeightSM,
      controlHeightLG: fs.controlHeightLG,
      fontWeight: 500,
      paddingInline: 16,
      paddingInlineSM: 12,
      paddingInlineLG: 20,
    },
    Input: {
      borderRadius: us.borderRadius,
      controlHeight: fs.controlHeight,
      paddingInline: 12,
      activeShadow: `0 0 0 3px ${primarySoft}`,
    },
    Select: {
      borderRadius: us.borderRadius,
      controlHeight: fs.controlHeight,
      optionSelectedBg: primarySoft,
      optionSelectedColor: primary,
    },
    Table: {
      borderRadius: us.cardRadius,
      cellPaddingBlock: 12,
      cellPaddingInline: 16,
    },
    Modal: {
      borderRadius: us.modalRadius,
      paddingContentHorizontalLG: 28,
      paddingMD: 24,
      titleFontSize: 16,
    },
    Tag: {
      borderRadius: us.borderRadiusSM,
      paddingInline: 8,
    },
    Slider: {
      railBg: isDark ? 'rgba(255,255,255,0.12)' : '#e7e5e4',
      railHoverBg: isDark ? 'rgba(255,255,255,0.18)' : '#d6d3d1',
      trackBg: primary,
      handleColor: primary,
      railSize: 4,
      handleSize: 14,
      handleSizeHover: 16,
    },
    Switch: {
      colorPrimary: primary,
      colorPrimaryHover: ct.primaryHover,
      handleSize: 18,
      trackHeight: 22,
      trackMinWidth: 44,
    },
    Progress: {
      defaultColor: primary,
      remainingColor: isDark ? 'rgba(255,255,255,0.08)' : '#e7e5e4',
      lineBorderRadius: 4,
    },
    Form: {
      labelFontSize: 13,
      labelColor: isDark ? '#d6d3d1' : '#44403c',
      verticalLabelPadding: '0 0 6px',
    },
    Dropdown: {
      borderRadius: us.borderRadiusLG,
      paddingBlock: 5,
      controlPaddingHorizontal: 14,
    },
    Tooltip: {
      borderRadius: us.borderRadius,
      colorBgDefault: isDark ? '#292524' : '#ffffff',
      colorTextLightSolid: isDark ? '#f5f5f4' : '#1c1917',
    },
    Divider: {
      marginLG: 20,
    },
    Statistic: {
      titleFontSize: 13,
      contentFontSize: 28,
      fontFamily: ff.value,
    },
    Alert: {
      borderRadius: us.borderRadiusLG,
    },
  };
}

// ==================== 同步 CSS 变量 ====================

function syncCSSVariables(isDark: boolean, ct: ColorThemeDef, us: UIStyleDef) {
  const root = document.documentElement;
  const primary = isDark ? ct.primaryLight : ct.primary;
  const primaryHover = isDark ? ct.primaryLightHover : ct.primaryHover;
  const primarySoft = isDark ? ct.primaryLightSoft : ct.primarySoft;
  const primaryBorder = isDark ? ct.primaryLightBorder : ct.primaryBorder;

  root.style.setProperty('--color-primary', primary);
  root.style.setProperty('--color-primary-hover', primaryHover);
  root.style.setProperty('--color-primary-soft', primarySoft);
  root.style.setProperty('--color-primary-border', primaryBorder);
  root.style.setProperty('--color-primary-rgb', hexToRgb(primary));
  root.style.setProperty('--color-primary-hover-rgb', hexToRgb(primaryHover));

  // UI style CSS variables
  root.style.setProperty('--ui-border-radius', `${us.borderRadius}px`);
  root.style.setProperty('--ui-border-radius-lg', `${us.borderRadiusLG}px`);
  root.style.setProperty('--ui-card-radius', `${us.cardRadius}px`);
  root.style.setProperty('--ui-button-radius', `${us.buttonRadius}px`);
  root.style.setProperty('--ui-modal-radius', `${us.modalRadius}px`);
  root.style.setProperty('--ui-shadow-scale', `${us.shadowScale}`);
  root.style.setProperty('--ui-card-border-width', `${us.cardBorderWidth}px`);
  root.style.setProperty('--ui-btn-letter-spacing', us.buttonLetterSpacing);
  root.style.setProperty('--ui-btn-text-transform', us.buttonTextTransform);
  root.setAttribute('data-ui-style', us.key);
}

// ==================== App 组件 ====================

function App() {
  const { initUser, themeMode, colorTheme, fontFamily, fontSize, uiStyle } = useAppStore();

  useEffect(() => {
    initUser();
  }, [initUser]);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', themeMode);
  }, [themeMode]);

  const isDark = themeMode === 'dark';
  const ct = COLOR_THEMES[colorTheme];
  const ff = FONT_FAMILIES[fontFamily];
  const fs = FONT_SIZES[fontSize];
  const us = UI_STYLES[uiStyle];

  // Sync CSS custom properties for App.css / login.css / index.css
  useEffect(() => {
    syncCSSVariables(isDark, ct, us);
  }, [isDark, ct, us]);

  const tokenObj = useMemo(() => buildToken(isDark, ct, ff, fs, us), [isDark, ct, ff, fs, us]);
  const componentTokens = useMemo(() => buildComponentTokens(isDark, ct, ff, fs, us), [isDark, ct, ff, fs, us]);

  return (
    <ConfigProvider
      theme={{
        algorithm: isDark ? theme.darkAlgorithm : theme.defaultAlgorithm,
        token: tokenObj,
        components: componentTokens,
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
