import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import * as SecureStore from 'expo-secure-store';

type Theme = 'light' | 'dark' | 'system';
type Palette = 'default' | 'ocean' | 'forest' | 'sunset' | 'monochrome';

interface ThemeState {
  theme: Theme;
  palette: Palette;
  setTheme: (theme: Theme) => void;
  setPalette: (palette: Palette) => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      theme: 'system',
      palette: 'default',
      setTheme: (theme) => set({ theme }),
      setPalette: (palette) => set({ palette }),
    }),
    {
      name: 'theme-storage',
      storage: createJSONStorage(() => SecureStore),
    }
  )
);