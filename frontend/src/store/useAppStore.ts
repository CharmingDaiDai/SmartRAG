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
  defaultModel?: string;
  defaultEmbedding?: string;
  defaultRerank?: string;
}

interface AppState {
  userInfo: User | null;
  currentKbId: string | null;
  token: string | null;
  llmModels: string[];
  embeddingModels: string[];
  rerankModels: string[];
  localSettings: LocalSettings;
  themeMode: 'light' | 'dark';
  colorTheme: ColorTheme;
  fontFamily: FontFamily;
  fontSize: FontSize;
  uiStyle: UIStyle;

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
      return parsed;
    }
  } catch { /* ignore */ }
  return DEFAULT_PERSONALIZATION;
};

const savePersonalization = (patch: Partial<PersonalizationSettings>) => {
  const current = loadPersonalization();
  localStorage.setItem('SmartRAG_personalization', JSON.stringify({ ...current, ...patch }));
};

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
      savePersonalization({ colorTheme });
      set({ colorTheme });
  },

  setFontFamily: (fontFamily) => {
      savePersonalization({ fontFamily });
      set({ fontFamily });
  },

  setFontSize: (fontSize) => {
      savePersonalization({ fontSize });
      set({ fontSize });
  },

  setUIStyle: (uiStyle) => {
      const updates: Partial<PersonalizationSettings> = { uiStyle };
      if (uiStyle === 'tech') {
          updates.fontFamily = 'system';
          updates.colorTheme = 'indigo';
      } else if (uiStyle === 'playful') {
          updates.fontFamily = 'lxgw';
          updates.colorTheme = 'pink';
      }
      savePersonalization(updates);
      set(updates);
  },
}));
