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
import './App.css';
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

import ThemeBackground from './components/ThemeBackground';

// ==================== Token 构建函数 ====================

function hexToRgb(hex: string): string {
  const h = hex.replace('#', '');
  const r = parseInt(h.substring(0, 2), 16);
  const g = parseInt(h.substring(2, 4), 16);
  const b = parseInt(h.substring(4, 6), 16);
  return `${r},${g},${b}`;
}

type NavigatorWithDeviceMemory = Navigator & {
  deviceMemory?: number;
};

function detectLitePerformance(): boolean {
  if (typeof navigator === 'undefined') {
    return false;
  }

  const nav = navigator as NavigatorWithDeviceMemory;
  const memory = typeof nav.deviceMemory === 'number' ? nav.deviceMemory : 8;
  const cores = navigator.hardwareConcurrency || 8;

  return memory <= 4 || cores <= 4;
}

function buildToken(isDark: boolean, ct: ColorThemeDef, ff: FontFamilyDef, fs: FontSizeDef, us: UIStyleDef) {
  const base = {
    colorPrimary: ct.primary,
    colorSuccess: '#16a34a',
    colorWarning: '#d97706',
    colorError: '#dc2626',
    colorInfo: ct.primary,

    colorBgBase: '#f4f6fb',
    colorBgContainer: '#ffffff',
    colorBgElevated: '#ffffff',
    colorBgLayout: '#edf1f7',
    colorBgSpotlight: '#e6edf9',
    colorBgMask: 'rgba(15, 23, 42, 0.45)',

    colorBorder: '#dce3ee',
    colorBorderSecondary: '#e9eef6',

    colorText: '#0f172a',
    colorTextSecondary: '#475569',
    colorTextTertiary: '#94a3b8',
    colorTextQuaternary: '#cbd5e1',
    colorTextDescription: '#64748b',
    colorTextDisabled: '#cbd5e1',
    colorTextHeading: '#0b1220',
    colorTextLabel: '#334155',
    colorTextPlaceholder: '#94a3b8',

    colorFill: '#eef3fa',
    colorFillSecondary: '#f5f8fd',
    colorFillTertiary: '#f8fafe',
    colorFillAlter: '#f5f8fd',
    colorFillQuaternary: 'rgba(226, 232, 240, 0.6)',

    colorSplit: '#dde5f0',

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

    lineHeight: 1.7,
    lineHeightSM: 1.5,
    lineHeightLG: 1.75,
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

    boxShadow: 'none',
    boxShadowSecondary: 'none',
    boxShadowTertiary: 'none',

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
      colorBgBase: '#0b1220',
      colorBgContainer: '#111a2b',
      colorBgElevated: '#162033',
      colorBgLayout: '#090f1b',
      colorBgSpotlight: '#1c2840',
      colorBgMask: 'rgba(2, 6, 23, 0.75)',

      colorBorder: 'rgba(148, 163, 184, 0.24)',
      colorBorderSecondary: 'rgba(148, 163, 184, 0.14)',

      colorText: '#e2e8f0',
      colorTextSecondary: '#94a3b8',
      colorTextTertiary: '#64748b',
      colorTextQuaternary: '#475569',
      colorTextDescription: '#94a3b8',
      colorTextDisabled: '#475569',
      colorTextHeading: '#f8fafc',
      colorTextLabel: '#cbd5e1',
      colorTextPlaceholder: '#64748b',

      colorFill: 'rgba(148, 163, 184, 0.14)',
      colorFillSecondary: 'rgba(148, 163, 184, 0.10)',
      colorFillTertiary: 'rgba(148, 163, 184, 0.06)',
      colorFillAlter: 'rgba(148, 163, 184, 0.12)',
      colorFillQuaternary: 'rgba(148, 163, 184, 0.08)',

      colorSplit: 'rgba(148, 163, 184, 0.2)',

      boxShadow: 'none',
      boxShadowSecondary: 'none',
      boxShadowTertiary: 'none',
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
      itemHeight: 42,
      itemSelectedBg: primarySoft,
      itemSelectedColor: primary,
      itemHoverBg: isDark ? 'rgba(148, 163, 184, 0.12)' : `${primary}14`,
      itemHoverColor: isDark ? '#f8fafc' : '#0f172a',
      itemActiveBg: isDark ? 'rgba(148, 163, 184, 0.18)' : `${primary}22`,
      iconSize: 16,
      iconMarginInlineEnd: 10,
      collapsedIconSize: 18,
      itemPaddingInline: 14,
      itemBg: 'transparent',
      subMenuItemBg: 'transparent',
    },
    Card: {
      borderRadius: us.cardRadius,
      borderRadiusLG: us.cardRadius + 4,
      paddingLG: 22,
    },
    Button: {
      borderRadius: us.buttonRadius,
      borderRadiusSM: Math.max(us.buttonRadius - 2, 2),
      controlHeight: fs.controlHeight,
      controlHeightSM: fs.controlHeightSM,
      controlHeightLG: fs.controlHeightLG,
      fontWeight: 600,
      paddingInline: 18,
      paddingInlineSM: 12,
      paddingInlineLG: 22,
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
    Layout: {
      headerBg: isDark ? '#111a2b' : '#ffffff',
      siderBg: isDark ? '#111a2b' : '#ffffff',
      bodyBg: isDark ? '#090f1b' : '#edf1f7',
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
  const primaryRgb = hexToRgb(primary);
  const primaryHoverRgb = hexToRgb(primaryHover);

  const glassBg = isDark ? 'rgba(15, 23, 42, 0.62)' : 'rgba(255, 255, 255, 0.82)';
  const glassBgStrong = isDark ? 'rgba(17, 26, 43, 0.82)' : 'rgba(255, 255, 255, 0.96)';
  const glassBorder = isDark ? 'rgba(148, 163, 184, 0.38)' : 'rgba(136, 160, 195, 0.7)';
  const glassHighlight = isDark ? 'rgba(255, 242, 209, 0.22)' : 'rgba(255, 255, 255, 0.9)';
  const glassTint = isDark ? 'rgba(236, 188, 110, 0.2)' : 'rgba(246, 208, 132, 0.2)';
  const glassWarmRgb = isDark ? '236, 188, 110' : '246, 208, 132';
  const glassPrismA = isDark ? 'rgba(255, 246, 224, 0.24)' : 'rgba(255, 255, 255, 0.58)';
  const glassPrismB = isDark ? 'rgba(236, 190, 118, 0.3)' : 'rgba(255, 236, 190, 0.28)';
  const glassPrismC = isDark ? 'rgba(225, 165, 78, 0.24)' : 'rgba(246, 200, 118, 0.22)';
  const glassFilmOpacity = isDark ? '0.6' : '0.62';
  const glassCausticOpacity = isDark ? '0.18' : '0.2';
  const glassEdgeStrong = isDark ? 'rgba(226, 232, 240, 0.54)' : 'rgba(255, 255, 255, 0.96)';
  const glassEdgeSoft = isDark ? 'rgba(148, 163, 184, 0.38)' : 'rgba(177, 191, 214, 0.56)';
  const glassBorderWidth = isDark ? '1.25px' : '1.5px';
  const glassEdgeWidth = isDark ? '1.05px' : '1.2px';
  const glassAmbientLine = isDark ? 'rgba(148, 163, 184, 0.15)' : 'rgba(148, 163, 184, 0.16)';
  const glassShadow = 'none';
  const glassShadowHover = 'none';

  root.style.setProperty('--color-primary', primary);
  root.style.setProperty('--color-primary-hover', primaryHover);
  root.style.setProperty('--color-primary-soft', primarySoft);
  root.style.setProperty('--color-primary-border', primaryBorder);
  root.style.setProperty('--color-primary-rgb', primaryRgb);
  root.style.setProperty('--color-primary-hover-rgb', primaryHoverRgb);

  root.style.setProperty('--glass-bg', glassBg);
  root.style.setProperty('--glass-bg-strong', glassBgStrong);
  root.style.setProperty('--glass-border', glassBorder);
  root.style.setProperty('--glass-highlight', glassHighlight);
  root.style.setProperty('--glass-tint', glassTint);
  root.style.setProperty('--glass-warm-rgb', glassWarmRgb);
  root.style.setProperty('--glass-prism-a', glassPrismA);
  root.style.setProperty('--glass-prism-b', glassPrismB);
  root.style.setProperty('--glass-prism-c', glassPrismC);
  root.style.setProperty('--glass-shadow', glassShadow);
  root.style.setProperty('--glass-shadow-hover', glassShadowHover);
  root.style.setProperty('--glass-blur', isDark ? '16px' : '14px');
  root.style.setProperty('--glass-blur-strong', isDark ? '26px' : '22px');
  root.style.setProperty('--glass-noise-opacity', isDark ? '0.05' : '0.035');
  root.style.setProperty('--glass-film-opacity', glassFilmOpacity);
  root.style.setProperty('--glass-caustic-opacity', glassCausticOpacity);
  root.style.setProperty('--glass-edge-strong', glassEdgeStrong);
  root.style.setProperty('--glass-edge-soft', glassEdgeSoft);
  root.style.setProperty('--glass-border-width', glassBorderWidth);
  root.style.setProperty('--glass-edge-width', glassEdgeWidth);
  root.style.setProperty('--glass-ambient-line', glassAmbientLine);

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
  root.setAttribute('data-ui-style', 'minimal');
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

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) {
      return;
    }

    const root = document.documentElement;
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');

    const applyRuntimeFlags = () => {
      root.setAttribute('data-motion', mediaQuery.matches ? 'reduced' : 'normal');
      root.setAttribute('data-performance', detectLitePerformance() ? 'lite' : 'full');
    };

    applyRuntimeFlags();

    if (typeof mediaQuery.addEventListener === 'function') {
      mediaQuery.addEventListener('change', applyRuntimeFlags);
      return () => mediaQuery.removeEventListener('change', applyRuntimeFlags);
    }

    mediaQuery.addListener(applyRuntimeFlags);
    return () => mediaQuery.removeListener(applyRuntimeFlags);
  }, []);

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
        token: {
          ...tokenObj,
          motion: true,
        },
        components: componentTokens,
      }}
    >
      <ThemeBackground />
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
