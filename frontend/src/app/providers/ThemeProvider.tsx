import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { useThemeStore, Theme, Palette } from '../../store/themeStore';
interface ThemeContextType { theme: Theme; palette: Palette; resolvedTheme: 'light' | 'dark'; setTheme: (theme: Theme) => void; setPalette: (palette: Palette) => void; toggleTheme: () => void; }
const ThemeContext = createContext<ThemeContextType | undefined>(undefined);
const darkThemes = new Set<Theme>(['dark','midnight','oled','graphite','forest']);
export function ThemeProvider({ children }: { children: ReactNode }) {
 const { theme, palette, setTheme, setPalette } = useThemeStore();
 const [resolvedTheme,setResolvedTheme]=useState<'light'|'dark'>('light');
 useEffect(()=>{ const root=document.documentElement; root.setAttribute('data-palette',palette); root.setAttribute('data-theme',theme); const systemDark=window.matchMedia('(prefers-color-scheme: dark)').matches; const resolved: 'light'|'dark'=theme==='system'?(systemDark?'dark':'light'):(darkThemes.has(theme)?'dark':'light'); setResolvedTheme(resolved); root.setAttribute('data-theme', theme === 'system' ? resolved : theme); root.setAttribute('data-resolved-theme',resolved); },[theme,palette]);
 useEffect(()=>{ const media=window.matchMedia('(prefers-color-scheme: dark)'); const change=()=>{ if(theme!=='system') return; const resolved=media.matches?'dark':'light'; setResolvedTheme(resolved); document.documentElement.setAttribute('data-theme', resolved); document.documentElement.setAttribute('data-resolved-theme',resolved); }; media.addEventListener('change',change); return()=>media.removeEventListener('change',change); },[theme]);
 const toggleTheme=()=>setTheme(theme==='system'?'dark':theme==='dark'?'light':'system');
 return <ThemeContext.Provider value={{theme,palette,resolvedTheme,setTheme,setPalette,toggleTheme}}>{children}</ThemeContext.Provider>;
}
export function useTheme(){ const context=useContext(ThemeContext); if(!context) throw new Error('useTheme must be used within a ThemeProvider'); return context; }
