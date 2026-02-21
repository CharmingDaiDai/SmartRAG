import { create } from 'zustand';
import { User } from '../types';
import { userService } from '../services/userService';
import { modelService } from '../services/modelService';

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
  
  setUserInfo: (user: User | null) => void;
  setCurrentKbId: (id: string | null) => void;
  setToken: (token: string | null) => void;
  login: (token: string, user: User) => void;
  logout: () => void;
  initUser: () => Promise<void>;
  fetchModelLists: () => Promise<void>;
  updateLocalSettings: (settings: LocalSettings) => void;
  toggleTheme: () => void;
}

export const useAppStore = create<AppState>((set, get) => ({
  userInfo: null,
  currentKbId: null,
  token: localStorage.getItem('token'),
  llmModels: [],
  embeddingModels: [],
  rerankModels: [],
  localSettings: JSON.parse(localStorage.getItem('SmartRAG_localSettings') || '{}'),
  themeMode: (localStorage.getItem('SmartRAG_theme') as 'light' | 'dark') || 'light',
  
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
                  // Token invalid
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
  }
}));
