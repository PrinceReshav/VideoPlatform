import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Bell, ChevronDown, Menu, Palette, Plus, Search, Video, X, Command } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../app/providers/AuthProvider';
import { useTheme } from '../../app/providers/ThemeProvider';
import { Avatar } from '../ui/Avatar';
import { AppearanceMenu } from './AppearanceMenu';

export function Header({ onMenuClick }: { onMenuClick: () => void }) {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [appearanceOpen, setAppearanceOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchValue, setSearchValue] = useState(new URLSearchParams(location.search).get('q') || '');
  const appearanceRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setSearchValue(new URLSearchParams(location.search).get('q') || '');
  }, [location.search]);

  useEffect(() => {
    const close = (event: MouseEvent) => {
      if (appearanceRef.current && !appearanceRef.current.contains(event.target as Node)) setAppearanceOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        document.getElementById('global-search')?.focus();
      }
      if (event.key === 'Escape') setSearchOpen(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  const submitSearch = (event: FormEvent) => {
    event.preventDefault();
    const q = searchValue.trim();
    if (q) navigate(`/search?q=${encodeURIComponent(q)}`);
    else navigate('/search');
    setSearchOpen(false);
  };

  return (
    <header className="site-header">
      <div className="header-left">
        <button onClick={onMenuClick} className="header-icon-button mobile-menu" aria-label="Open navigation"><Menu className="w-5 h-5" /></button>
        <Link to="/" className="brand" aria-label="VideoPlatform home">
          <span className="brand-mark"><Video className="w-5 h-5" /></span>
          <span className="brand-name">VideoPlatform</span>
        </Link>
      </div>

      <form className="global-search" onSubmit={submitSearch}>
        <Search className="global-search-icon" />
        <input id="global-search" value={searchValue} onChange={e=>setSearchValue(e.target.value)} onFocus={()=>setSearchOpen(true)} placeholder="Search videos, creators, channels..." aria-label="Search" />
        {searchValue && <button type="button" className="global-search-clear" onClick={()=>setSearchValue('')} aria-label="Clear search"><X className="w-4 h-4" /></button>}
        <span className="search-shortcut"><Command className="w-3 h-3" /> K</span>
        <button className="global-search-submit" type="submit" aria-label="Search"><Search className="w-4 h-4" /></button>
        {searchOpen && searchValue.length > 1 && <div className="global-search-popover"><div className="global-search-popover-title">Search for <strong>“{searchValue}”</strong></div><button type="submit"><Search className="w-4 h-4"/>See all results</button></div>}
      </form>

      <div className="header-right">
        {isAuthenticated && <Link to="/creator/upload" className="create-button"><Plus className="w-4 h-4"/><span>Create</span></Link>}
        <div className="header-popover-anchor" ref={appearanceRef}>
          <button className={`header-icon-button ${appearanceOpen?'active':''}`} onClick={()=>{setAppearanceOpen(v=>!v);setMenuOpen(false)}} aria-label="Theme and colors" aria-expanded={appearanceOpen}><Palette className="w-5 h-5"/><span className="accent-dot"/></button>
          {appearanceOpen && <div className="appearance-popover"><AppearanceMenu/></div>}
        </div>
        {isAuthenticated && <button className="header-icon-button" aria-label="Notifications"><Bell className="w-5 h-5"/><span className="notification-dot"/></button>}
        {isAuthenticated && user ? <div className="header-popover-anchor">
          <button className="profile-trigger" onClick={()=>{setMenuOpen(v=>!v);setAppearanceOpen(false)}} aria-expanded={menuOpen}><Avatar src={user.avatarUrl} alt={user.displayName} size="sm"/><span className="profile-name">{user.displayName}</span><ChevronDown className={`w-4 h-4 ${menuOpen?'rotate-180':''}`}/></button>
          {menuOpen && <div className="user-popover"><div className="user-popover-head"><Avatar src={user.avatarUrl} alt={user.displayName} size="md"/><div><strong>{user.displayName}</strong><span>@{user.username}</span></div></div><Link to="/profile" onClick={()=>setMenuOpen(false)}>Your profile</Link><Link to="/settings" onClick={()=>setMenuOpen(false)}>Settings & appearance</Link><button onClick={()=>{setMenuOpen(false);logout()}}>Sign out</button></div>}
        </div> : <div className="auth-actions"><Link to="/login">Sign in</Link><Link className="get-started" to="/register">Get started</Link></div>}
      </div>
    </header>
  );
}
