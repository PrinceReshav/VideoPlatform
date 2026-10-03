import { create } from 'zustand';
import { persist } from 'zustand/middleware';

type Theme = 'light' | 'dark' | 'system' | 'midnight' | 'oled' | 'graphite' | 'arctic' | 'warm' | 'forest' | 'lavender';
type Palette = 'default' | 'violet' | 'red' | 'rose' | 'ocean' | 'cyan' | 'emerald' | 'green' | 'amber' | 'orange' | 'pink' | 'slate';
interface ThemeState { theme: Theme; palette: Palette; setTheme: (theme: Theme) => void; setPalette: (palette: Palette) => void; }
export const useThemeStore = create<ThemeState>()(persist((set) => ({ theme: 'system', palette: 'red', setTheme: (theme) => set({ theme }), setPalette: (palette) => set({ palette }) }), { name: 'theme-storage' }));
export type { Theme, Palette };
