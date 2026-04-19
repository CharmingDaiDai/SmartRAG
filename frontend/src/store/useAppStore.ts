/**
 * 全局状态管理器 (Zustand 驱动)
 * 
 * 功能逻辑：
 * 1. 集中管理用户的认证状态（userInfo, token）、当前选择的知识库（currentKbId）、以及系统配置（如大模型列表、Embedding模型列表）。
 * 2. 负责主题个性化与样式配置项的维护，如 `themeMode`（深浅色模式）、`colorTheme`、`fontFamily` 及 `uiStyle`，并将其同步持久化到 localStorage 中以便刷新后保留配置。
 * 3. 包含核心的基础 API 和交互方法，例如 `login`（登录挂载 Token），`logout`（清理认证状态），`initUser`（初始化用户信息），以及 `fetchModelLists`（异步获取模型配置）。
 * 
 * 影响范围：
 * 作为 React 应用状态的中枢，其属性变更会立刻触发所有订阅此 Store 的组件重新渲染并应用最新上下文（比如切换应用主题、变更个人配置或全局鉴权登出）。
 */
import { create } from 'zustand';
import { User } from '../types';
import { userService } from '../services/userService';
import { modelService } from '../services/modelService';
import {
  ColorTheme, FontFamily, FontSize, UIStyle,
  PersonalizationSettings, DEFAULT_PERSONALIZATION,
  FONT_FAMILIES, COLOR_THEMES, UI_STYLES, FONT_SIZES,
} from '../config/themeConfig';

interface LocalSettings {
  defaultModel?: string;      // 记录默认大语言模型
  defaultEmbedding?: string;  // 记录默认向量模型
  defaultRerank?: string;     // 记录默认重排序模型
}

interface AppState {
  userInfo: User | null;      // 用户信息
  currentKbId: string | null; // 当前上下文打开的知识库 ID
  token: string | null;       // 用户鉴权凭证 JWT
  llmModels: string[];        // 服务端获取的支持的 LLM 列表
  embeddingModels: string[];  // 服务端获取的支持的 Embedding 列表
  rerankModels: string[];     // 服务端获取的支持的 Rerank 列表
  localSettings: LocalSettings;   // 本地的选择偏好映射
  themeMode: 'light' | 'dark';    // 系统的暗黑模式配置
  colorTheme: ColorTheme;     // 主题调色盘风格（锁死默认避免碎片化）
  fontFamily: FontFamily;     // 字体风格
  fontSize: FontSize;         // 字号
  uiStyle: UIStyle;           // UI外观模式

  setUserInfo: (user: User | null) => void;
  setCurrentKbId: (id: string | null) => void;
  setToken: (token: string | null) => void;
  login: (token: string, user: User) => void;
  logout: () => void;
  initUser: () => Promise<void>;
  fetchModelLists: () => Promise<void>;
  updateLocalSettings: (settings: LocalSettings) => void;
  toggleTheme: () => void;
  setColorTheme: (theme: ColorTheme) => void;
  setFontFamily: (font: FontFamily) => void;
  setFontSize: (size: FontSize) => void;
  setUIStyle: (style: UIStyle) => void;
}

/**
 * 个性化配置归一化函数
 * 逻辑：在应用当前形态下，将视觉、色彩和字体进行基础回落处理以保持产品级纯净风格。
 */
const normalizePersonalization = (settings: PersonalizationSettings): PersonalizationSettings => {
    const normalized = { ...settings };

        // Lock style preset to keep a single visual language across the product.
        if (normalized.uiStyle !== DEFAULT_PERSONALIZATION.uiStyle) {
            normalized.uiStyle = DEFAULT_PERSONALIZATION.uiStyle;
    }

        // Keep one brand accent color for consistency.
        if (normalized.colorTheme !== DEFAULT_PERSONALIZATION.colorTheme) {
            normalized.colorTheme = DEFAULT_PERSONALIZATION.colorTheme;
        }

        // Lock typography to default family to avoid style fragmentation.
        if (
            normalized.fontFamily !== DEFAULT_PERSONALIZATION.fontFamily ||
            FONT_FAMILIES[normalized.fontFamily]?.category === 'artistic'
        ) {
            normalized.fontFamily = DEFAULT_PERSONALIZATION.fontFamily;
    }

    return normalized;
};

/**
 * 从 localStorage 中加载且校验个性化配置。
 * 遇到不合理的（已下架或弃用）配置自动回滚到默认设置 (`DEFAULT_PERSONALIZATION`)。
 */
const loadPersonalization = (): PersonalizationSettings => {
  try {
    const raw = localStorage.getItem('SmartRAG_personalization');
    if (raw) {
      const parsed = { ...DEFAULT_PERSONALIZATION, ...JSON.parse(raw) };
      // Validate saved keys still exist (handles removed fonts etc.)
      if (!(parsed.fontFamily in FONT_FAMILIES)) parsed.fontFamily = DEFAULT_PERSONALIZATION.fontFamily;
      if (!(parsed.colorTheme in COLOR_THEMES)) parsed.colorTheme = DEFAULT_PERSONALIZATION.colorTheme;
      if (!(parsed.uiStyle in UI_STYLES)) parsed.uiStyle = DEFAULT_PERSONALIZATION.uiStyle;
      if (!(parsed.fontSize in FONT_SIZES)) parsed.fontSize = DEFAULT_PERSONALIZATION.fontSize;
            return normalizePersonalization(parsed);
    }
  } catch { /* ignore */ }
  return DEFAULT_PERSONALIZATION;
};

/**
 * 局部更新视觉配置病持久化存储至 localStorage。
 */
const savePersonalization = (patch: Partial<PersonalizationSettings>) => {
  const current = loadPersonalization();
  localStorage.setItem('SmartRAG_personalization', JSON.stringify({ ...current, ...patch }));
};

// 立即读取已存放的视图配置为初始化数据
const persisted = loadPersonalization();

export const useAppStore = create<AppState>((set, get) => ({
  userInfo: null,
  currentKbId: null,
  token: localStorage.getItem('token'),
  llmModels: [],
  embeddingModels: [],
  rerankModels: [],
  localSettings: JSON.parse(localStorage.getItem('SmartRAG_localSettings') || '{}'),
  themeMode: (localStorage.getItem('SmartRAG_theme') as 'light' | 'dark') || 'light',
  colorTheme: persisted.colorTheme,
  fontFamily: persisted.fontFamily,
  fontSize: persisted.fontSize,
  uiStyle: persisted.uiStyle,

  setUserInfo: (user) => set({ userInfo: user }),
  setCurrentKbId: (id) => set({ currentKbId: id }),
  setToken: (token) => {
      if (token) {
          localStorage.setItem('token', token);
      } else {
          localStorage.removeItem('token');
      }
      set({ token });
  },

  login: (token, user) => {
      localStorage.setItem('token', token);
      set({ token, userInfo: user });
  },

  logout: () => {
      localStorage.removeItem('token');
      set({ token: null, userInfo: null, currentKbId: null });
  },

  initUser: async () => {
      const token = localStorage.getItem('token');
      if (token) {
          try {
              const res: any = await userService.getProfile();
              if (res.code === 200) {
                  set({ userInfo: res.data, token });
              } else {
                  get().logout();
              }
          } catch (e) {
              get().logout();
          }
      }
  },

  fetchModelLists: async () => {
      try {
          const [llmRes, embedRes, rerankRes] = await Promise.all([
              modelService.getLLMs(),
              modelService.getEmbeddings(),
              modelService.getReranks()
          ]);

          set({
              llmModels: (llmRes as any).data || [],
              embeddingModels: (embedRes as any).data || [],
              rerankModels: (rerankRes as any).data || []
          });
      } catch (error) {
          console.error('Failed to fetch model lists', error);
      }
  },

  updateLocalSettings: (settings) => {
      const newSettings = { ...get().localSettings, ...settings };
      localStorage.setItem('SmartRAG_localSettings', JSON.stringify(newSettings));
      set({ localSettings: newSettings });
  },

  toggleTheme: () => {
      const newTheme = get().themeMode === 'light' ? 'dark' : 'light';
      localStorage.setItem('SmartRAG_theme', newTheme);
      set({ themeMode: newTheme });
  },

  setColorTheme: (colorTheme) => {
            const stableTheme = DEFAULT_PERSONALIZATION.colorTheme;
            if (colorTheme !== stableTheme) {
                savePersonalization({ colorTheme: stableTheme });
                set({ colorTheme: stableTheme });
                return;
            }

            savePersonalization({ colorTheme });
            set({ colorTheme });
  },

  setFontFamily: (fontFamily) => {
            const stableFont = DEFAULT_PERSONALIZATION.fontFamily;
            if (fontFamily !== stableFont) {
                savePersonalization({ fontFamily: stableFont });
                set({ fontFamily: stableFont });
                return;
            }

            savePersonalization({ fontFamily });
            set({ fontFamily });
  },

  setFontSize: (fontSize) => {
      savePersonalization({ fontSize });
      set({ fontSize });
  },

  setUIStyle: (uiStyle) => {
            const stableStyle = DEFAULT_PERSONALIZATION.uiStyle;
            if (uiStyle !== stableStyle) {
                savePersonalization({ uiStyle: stableStyle });
                set({ uiStyle: stableStyle });
                return;
            }

            savePersonalization({ uiStyle });
            set({ uiStyle });
  },
}));
