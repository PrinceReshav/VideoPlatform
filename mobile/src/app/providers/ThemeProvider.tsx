import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { useThemeStore } from '../../store/themeStore';
import { useColorScheme } from 'react-native';

type Theme = 'light' | 'dark' | 'system';
type Palette = 'default' | 'ocean' | 'forest' | 'sunset' | 'monochrome';

interface ThemeContextType {
  theme: Theme;
  palette: Palette;
  resolvedTheme: 'light' | 'dark';
  setTheme: (theme: Theme) => void;
  setPalette: (palette: Palette) => void;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const { theme, palette, setTheme, setPalette } = useThemeStore();
  const systemTheme = useColorScheme();
  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>('light');

  useEffect(() => {
    let resolved: 'light' | 'dark';
    if (theme === 'system') {
      resolved = systemTheme === 'dark' ? 'dark' : 'light';
    } else {
      resolved = theme;
    }
    setResolvedTheme(resolved);
  }, [theme, systemTheme]);

  const toggleTheme = () => {
    const themes: Theme[] = ['light', 'dark', 'system'];
    const currentIndex = themes.indexOf(theme);
    const nextIndex = (currentIndex + 1) % themes.length;
    setTheme(themes[nextIndex]);
  };

  return (
    <ThemeContext.Provider value={{ theme, palette, resolvedTheme, setTheme, setPalette, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
}