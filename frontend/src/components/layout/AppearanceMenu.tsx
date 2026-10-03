import { Check, Monitor, Moon, Palette, Sun, Sparkles, Contrast, Snowflake, Leaf, Gem, Circle } from 'lucide-react';
import { useTheme } from '../../app/providers/ThemeProvider';
import type { Theme } from '../../store/themeStore';

export const palettes = [
  { id: 'red', name: 'Crimson', primary: '#ff0033', secondary: '#ff5c77' },
  { id: 'violet', name: 'Violet', primary: '#7c3aed', secondary: '#a855f7' },
  { id: 'rose', name: 'Rose', primary: '#e11d48', secondary: '#fb7185' },
  { id: 'ocean', name: 'Ocean', primary: '#0284c7', secondary: '#06b6d4' },
  { id: 'cyan', name: 'Cyan', primary: '#0891b2', secondary: '#22d3ee' },
  { id: 'emerald', name: 'Emerald', primary: '#059669', secondary: '#10b981' },
  { id: 'green', name: 'Green', primary: '#16a34a', secondary: '#22c55e' },
  { id: 'amber', name: 'Amber', primary: '#d97706', secondary: '#f59e0b' },
  { id: 'orange', name: 'Orange', primary: '#ea580c', secondary: '#f97316' },
  { id: 'pink', name: 'Pink', primary: '#db2777', secondary: '#ec4899' },
  { id: 'slate', name: 'Slate', primary: '#475569', secondary: '#64748b' },
  { id: 'default', name: 'Classic Red', primary: '#ff0033', secondary: '#ff5c77' },
] as const;

export const themes: { id: Theme; label: string; description: string; icon: typeof Sun; preview: string }[] = [
  { id: 'light', label: 'Light', description: 'Bright & clean', icon: Sun, preview: 'linear-gradient(135deg,#ffffff,#e2e8f0)' },
  { id: 'dark', label: 'Dark', description: 'Balanced dark', icon: Moon, preview: 'linear-gradient(135deg,#181818,#303030)' },
  { id: 'system', label: 'System', description: 'Follow device', icon: Monitor, preview: 'linear-gradient(135deg,#fff 0 50%,#181818 50%)' },
  { id: 'midnight', label: 'Midnight', description: 'Deep blue', icon: Sparkles, preview: 'linear-gradient(135deg,#08111f,#111d35)' },
  { id: 'oled', label: 'OLED', description: 'Pure black', icon: Contrast, preview: 'linear-gradient(135deg,#000,#161616)' },
  { id: 'graphite', label: 'Graphite', description: 'Studio grey', icon: Circle, preview: 'linear-gradient(135deg,#17181b,#3a3d43)' },
  { id: 'arctic', label: 'Arctic', description: 'Cool & crisp', icon: Snowflake, preview: 'linear-gradient(135deg,#f8fbff,#dbeafe)' },
  { id: 'warm', label: 'Warm', description: 'Soft ivory', icon: Gem, preview: 'linear-gradient(135deg,#fffaf3,#f3e7d3)' },
  { id: 'forest', label: 'Forest', description: 'Deep green', icon: Leaf, preview: 'linear-gradient(135deg,#071611,#173a2c)' },
  { id: 'lavender', label: 'Lavender', description: 'Soft purple', icon: Sparkles, preview: 'linear-gradient(135deg,#fbf9ff,#e9ddff)' },
];

export function AppearanceMenu() {
  const { theme, palette, setTheme, setPalette } = useTheme();
  return (
    <div className="appearance-panel">
      <div className="appearance-panel-header">
        <div className="appearance-panel-icon"><Palette className="w-4 h-4" /></div>
        <div><p className="font-bold text-[var(--color-text-primary)]">Theme & colors</p><p className="text-xs text-[var(--color-text-tertiary)]">Make VideoPlatform yours</p></div>
      </div>

      <section className="appearance-panel-section">
        <div className="appearance-section-heading"><div><p className="appearance-panel-label">Visual theme</p><p className="appearance-help">Choose the overall surface and background style.</p></div><span className="appearance-current">{themes.find(t => t.id === theme)?.label}</span></div>
        <div className="appearance-theme-grid appearance-theme-grid-large">
          {themes.map((item) => { const Icon=item.icon; const selected=theme===item.id; return (
            <button key={item.id} type="button" onClick={()=>setTheme(item.id)} className={`appearance-theme-card ${selected?'selected':''}`} aria-pressed={selected}>
              <span className="appearance-theme-preview" style={{background:item.preview}}><Icon className="w-4 h-4" /></span>
              <span className="appearance-theme-copy"><strong>{item.label}</strong><small>{item.description}</small></span>
              {selected && <span className="appearance-selected-check"><Check className="w-3 h-3" strokeWidth={3}/></span>}
            </button>
          ); })}
        </div>
      </section>

      <section className="appearance-panel-section appearance-divider">
        <div className="appearance-section-heading"><div><p className="appearance-panel-label">Accent palette</p><p className="appearance-help">Applies to buttons, links, active states and highlights.</p></div><span className="appearance-current">{palettes.find(p=>p.id===palette)?.name}</span></div>
        <div className="appearance-palette-grid">
          {palettes.map(item=>{ const selected=palette===item.id; return <button key={item.id} type="button" title={item.name} aria-label={`Use ${item.name} accent palette`} aria-pressed={selected} onClick={()=>setPalette(item.id)} className={`appearance-palette-swatch ${selected?'selected':''}`} style={{background:`linear-gradient(135deg,${item.primary},${item.secondary})`}}>{selected&&<Check className="w-4 h-4" strokeWidth={3}/>}</button>; })}
        </div>
        <div className="appearance-palette-name"><span className="appearance-live-dot" style={{background:'var(--color-primary)'}} /> Using <strong>{palettes.find(p=>p.id===palette)?.name}</strong> accent</div>
      </section>
    </div>
  );
}
