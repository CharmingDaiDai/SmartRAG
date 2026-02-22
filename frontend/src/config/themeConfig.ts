// ==================== 个性化设置类型定义 ====================

export type ColorTheme = 'indigo' | 'teal' | 'amber' | 'rose' | 'pink';
export type FontFamily = 'system' | 'noto' | 'lxgw' | 'yozai' | 'xiaolai' | 'qxs';
export type FontSize = 'small' | 'medium' | 'large';
export type UIStyle = 'minimal' | 'tech' | 'fancy';

export interface PersonalizationSettings {
  colorTheme: ColorTheme;
  fontFamily: FontFamily;
  fontSize: FontSize;
  uiStyle: UIStyle;
}

// ==================== 配色方案 ====================

export interface ColorThemeDef {
  key: ColorTheme;
  label: string;
  primary: string;
  primaryHover: string;
  primarySoft: string;
  primaryBorder: string;
  // 暗色模式变体
  primaryLight: string;
  primaryLightHover: string;
  primaryLightSoft: string;
  primaryLightBorder: string;
}

export const COLOR_THEMES: Record<ColorTheme, ColorThemeDef> = {
  indigo: {
    key: 'indigo',
    label: '靛紫',
    primary: '#6366f1',
    primaryHover: '#4f46e5',
    primarySoft: 'rgba(99,102,241,0.08)',
    primaryBorder: 'rgba(99,102,241,0.20)',
    primaryLight: '#818cf8',
    primaryLightHover: '#6366f1',
    primaryLightSoft: 'rgba(129,140,248,0.10)',
    primaryLightBorder: 'rgba(129,140,248,0.20)',
  },
  teal: {
    key: 'teal',
    label: '翠绿',
    primary: '#14b8a6',
    primaryHover: '#0d9488',
    primarySoft: 'rgba(20,184,166,0.08)',
    primaryBorder: 'rgba(20,184,166,0.20)',
    primaryLight: '#2dd4bf',
    primaryLightHover: '#14b8a6',
    primaryLightSoft: 'rgba(45,212,191,0.10)',
    primaryLightBorder: 'rgba(45,212,191,0.20)',
  },
  amber: {
    key: 'amber',
    label: '琥珀',
    primary: '#f59e0b',
    primaryHover: '#d97706',
    primarySoft: 'rgba(245,158,11,0.08)',
    primaryBorder: 'rgba(245,158,11,0.20)',
    primaryLight: '#fbbf24',
    primaryLightHover: '#f59e0b',
    primaryLightSoft: 'rgba(251,191,36,0.10)',
    primaryLightBorder: 'rgba(251,191,36,0.20)',
  },
  rose: {
    key: 'rose',
    label: '玫红',
    primary: '#f43f5e',
    primaryHover: '#e11d48',
    primarySoft: 'rgba(244,63,94,0.08)',
    primaryBorder: 'rgba(244,63,94,0.20)',
    primaryLight: '#fb7185',
    primaryLightHover: '#f43f5e',
    primaryLightSoft: 'rgba(251,113,133,0.10)',
    primaryLightBorder: 'rgba(251,113,133,0.20)',
  },
  pink: {
    key: 'pink',
    label: '粉红',
    primary: '#ec4899',
    primaryHover: '#db2777',
    primarySoft: 'rgba(236,72,153,0.08)',
    primaryBorder: 'rgba(236,72,153,0.20)',
    primaryLight: '#f472b6',
    primaryLightHover: '#ec4899',
    primaryLightSoft: 'rgba(244,114,182,0.10)',
    primaryLightBorder: 'rgba(244,114,182,0.20)',
  },
};

// ==================== 字体方案 ====================

export interface FontFamilyDef {
  key: FontFamily;
  label: string;
  value: string;
  /** 分类：normal=常规, artistic=艺术 */
  category: 'normal' | 'artistic';
}

export const FONT_FAMILIES: Record<FontFamily, FontFamilyDef> = {
  system: {
    key: 'system',
    label: '系统默认',
    value: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif",
    category: 'normal',
  },
  noto: {
    key: 'noto',
    label: '思源黑体',
    value: "'Noto Sans SC', 'PingFang SC', sans-serif",
    category: 'normal',
  },
  lxgw: {
    key: 'lxgw',
    label: '霞鹜文楷',
    value: "'LXGW WenKai Screen', 'PingFang SC', sans-serif",
    category: 'normal',
  },
  yozai: {
    key: 'yozai',
    label: '悠哉手写',
    value: "'Yozai', 'PingFang SC', cursive",
    category: 'artistic',
  },
  xiaolai: {
    key: 'xiaolai',
    label: '小赖字体',
    value: "'Xiaolai', 'PingFang SC', cursive",
    category: 'artistic',
  },
  qxs: {
    key: 'qxs',
    label: '全小素像素',
    value: "'quan', 'PingFang SC', cursive",
    category: 'artistic',
  },
};

export const FONT_FAMILY_CODE = "'JetBrains Mono', 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace";

// ==================== 字号方案 ====================

export interface FontSizeDef {
  key: FontSize;
  label: string;
  base: number;
  sm: number;
  lg: number;
  xl: number;
  controlHeight: number;
  controlHeightSM: number;
  controlHeightLG: number;
}

export const FONT_SIZES: Record<FontSize, FontSizeDef> = {
  small: {
    key: 'small',
    label: '紧凑',
    base: 13,
    sm: 11,
    lg: 15,
    xl: 18,
    controlHeight: 32,
    controlHeightSM: 24,
    controlHeightLG: 40,
  },
  medium: {
    key: 'medium',
    label: '标准',
    base: 14,
    sm: 12,
    lg: 16,
    xl: 20,
    controlHeight: 36,
    controlHeightSM: 28,
    controlHeightLG: 44,
  },
  large: {
    key: 'large',
    label: '舒适',
    base: 15,
    sm: 13,
    lg: 17,
    xl: 22,
    controlHeight: 40,
    controlHeightSM: 32,
    controlHeightLG: 48,
  },
};

// ==================== UI 风格方案 ====================

export interface UIStyleDef {
  key: UIStyle;
  label: string;
  description: string;
  // 圆角
  borderRadius: number;
  borderRadiusXS: number;
  borderRadiusSM: number;
  borderRadiusLG: number;
  borderRadiusOuter: number;
  cardRadius: number;
  buttonRadius: number;
  modalRadius: number;
  // 阴影
  shadowScale: number;
  // 侧边栏
  sidebarBorderStyle: 'solid' | 'glow' | 'none';
  // 顶部栏
  headerBorderStyle: 'solid' | 'glow' | 'none';
  // 卡片
  cardBorderWidth: number;
  cardHoverLift: boolean;
  // 按钮
  buttonStyle: 'default' | 'gradient' | 'glow';
  buttonLetterSpacing: string;
  buttonTextTransform: 'none' | 'uppercase';
  // 菜单
  menuIndicator: boolean;
  // 动画速度
  motionSpeed: 'normal' | 'fast';
  // 输入框风格
  inputStyle: 'default' | 'frosted' | 'thick';
  // 点击反馈
  clickFeedback: 'default' | 'glow-ripple' | 'elastic-ripple';
  // 骨架屏风格
  skeletonStyle: 'default' | 'gradient-flow' | 'rainbow';
}

export const UI_STYLES: Record<UIStyle, UIStyleDef> = {
  minimal: {
    key: 'minimal',
    label: '简约',
    description: '干净利落',
    borderRadius: 8,
    borderRadiusXS: 4,
    borderRadiusSM: 6,
    borderRadiusLG: 12,
    borderRadiusOuter: 16,
    cardRadius: 12,
    buttonRadius: 8,
    modalRadius: 16,
    shadowScale: 1,
    sidebarBorderStyle: 'solid',
    headerBorderStyle: 'solid',
    cardBorderWidth: 1,
    cardHoverLift: false,
    buttonStyle: 'gradient',
    buttonLetterSpacing: '0',
    buttonTextTransform: 'none',
    menuIndicator: true,
    motionSpeed: 'normal',
    inputStyle: 'default',
    clickFeedback: 'default',
    skeletonStyle: 'default',
  },
  tech: {
    key: 'tech',
    label: '科技',
    description: '硬朗棱角',
    borderRadius: 2,
    borderRadiusXS: 0,
    borderRadiusSM: 1,
    borderRadiusLG: 4,
    borderRadiusOuter: 4,
    cardRadius: 4,
    buttonRadius: 2,
    modalRadius: 6,
    shadowScale: 0.4,
    sidebarBorderStyle: 'glow',
    headerBorderStyle: 'glow',
    cardBorderWidth: 1,
    cardHoverLift: false,
    buttonStyle: 'default',
    buttonLetterSpacing: '0.08em',
    buttonTextTransform: 'uppercase',
    menuIndicator: true,
    motionSpeed: 'fast',
    inputStyle: 'frosted',
    clickFeedback: 'glow-ripple',
    skeletonStyle: 'gradient-flow',
  },
  fancy: {
    key: 'fancy',
    label: '花哨',
    description: '圆润趣味',
    borderRadius: 16,
    borderRadiusXS: 8,
    borderRadiusSM: 12,
    borderRadiusLG: 20,
    borderRadiusOuter: 24,
    cardRadius: 20,
    buttonRadius: 999,
    modalRadius: 24,
    shadowScale: 1.8,
    sidebarBorderStyle: 'none',
    headerBorderStyle: 'none',
    cardBorderWidth: 2,
    cardHoverLift: true,
    buttonStyle: 'glow',
    buttonLetterSpacing: '0.02em',
    buttonTextTransform: 'none',
    menuIndicator: false,
    motionSpeed: 'normal',
    inputStyle: 'thick',
    clickFeedback: 'elastic-ripple',
    skeletonStyle: 'rainbow',
  },
};

// ==================== 默认值 ====================

export const DEFAULT_PERSONALIZATION: PersonalizationSettings = {
  colorTheme: 'indigo',
  fontFamily: 'noto',
  fontSize: 'medium',
  uiStyle: 'minimal',
};
