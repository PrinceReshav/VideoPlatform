import React, { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Menu, X, Home, Search, History, Users, Bell, Settings, ChevronLeft, ChevronRight, LogOut, User, PlusCircle, Video, BarChart2, Crown, Sun, Moon, Palette, Heart, List, Clock, Play, Tv, Radio, Globe } from 'lucide-react';
import { clsx } from 'clsx';
import { useAuth } from '../../app/providers/AuthProvider';
import { useUIStore } from '../../store/uiStore';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Avatar } from '../ui/Avatar';
import { AppearanceMenu } from './AppearanceMenu';

interface SidebarProps {
  isOpen: boolean;
  isCollapsed: boolean;
  onClose: () => void;
}

const navigation = [
  { name: 'Home', href: '/', icon: Home },
  { name: 'Live TV', href: '/live', icon: Tv },
  { name: 'Subscriptions', href: '/subscriptions', icon: Users },
  { name: 'History', href: '/history', icon: History },
  { name: 'Liked Videos', href: '/liked', icon: Heart },
  { name: 'Playlists', href: '/playlists', icon: List },
  { name: 'Watch Later', href: '/watch-later', icon: Clock },
];

const bottomNavigation = [
  { name: 'Settings', href: '/settings', icon: Settings },
];

export function Sidebar({ isOpen, isCollapsed, onClose }: SidebarProps) {
  const location = useLocation();
  const { user, logout } = useAuth();
  const [appearanceOpen, setAppearanceOpen] = React.useState(false);

  if (!isOpen && !isCollapsed) return null;

  return (
    <>
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}
      <aside
        className={clsx(
          'fixed lg:static inset-y-0 left-0 z-50 bg-[var(--color-surface)] border-r border-[var(--color-border)] transition-all duration-300 ease-in-out flex flex-col',
          isCollapsed ? 'w-[72px]' : 'w-72',
          isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        )}
        aria-label="Main navigation"
      >
        <div className="flex h-16 items-center justify-between px-4 border-b border-[var(--color-border)]">
          {!isCollapsed && (
            <Link to="/" className="flex items-center gap-2" aria-label="Video Platform Home">
              <div className="w-8 h-8 rounded-lg bg-[var(--color-primary)] flex items-center justify-center">
                <Play className="w-5 h-5 text-white ml-1" />
              </div>
              <span className="font-bold text-lg text-[var(--color-text-primary)]">VideoPlatform</span>
            </Link>
          )}
          <button
            onClick={onClose}
            className={clsx(
              'p-1.5 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors lg:hidden',
              isCollapsed && 'hidden'
            )}
            aria-label="Close sidebar"
          >
            <X className="w-5 h-5 text-[var(--color-text-secondary)]" />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto py-4 px-2" aria-label="Main navigation">
          <ul className="space-y-1" role="list">
            {navigation.map((item) => (
              <li key={item.name}>
                <Link
                  to={item.href}
                  className={clsx(
                    'relative flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200',
                    'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]',
                    'hover:bg-[var(--color-bg-hover)]',
                    location.pathname === item.href
                      ? 'sidebar-active'
                      : '',
                    isCollapsed && 'justify-center px-2'
                  )}
                  aria-current={location.pathname === item.href ? 'page' : undefined}
                >
                  <item.icon className="w-5 h-5 flex-shrink-0" aria-hidden="true" />
                  {!isCollapsed && <span className="font-medium">{item.name}</span>}
                </Link>
              </li>
            ))}
          </ul>

          {!isCollapsed && (
            <>
              <div className="mt-8 mb-4 px-3">
                <h3 className="text-xs font-semibold text-[var(--color-text-tertiary)] uppercase tracking-wider">
                  More
                </h3>
              </div>
              <ul className="space-y-1" role="list">
                {bottomNavigation.map((item) => (
                  <li key={item.name}>
                    <Link
                      to={item.href}
                      className={clsx(
                        'relative flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200',
                        'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]',
                        'hover:bg-[var(--color-bg-hover)]'
                      )}
                    >
                      <item.icon className="w-5 h-5 flex-shrink-0" aria-hidden="true" />
                      <span className="font-medium">{item.name}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </>
          )}
        </nav>

        <div className="p-4 border-t border-[var(--color-border)]">
          {!isCollapsed && user && (
            <Link
              to="/profile"
              className="flex items-center gap-3 px-3 py-2.5 rounded-xl hover:bg-[var(--color-bg-hover)] transition-colors"
            >
              <Avatar src={user.avatarUrl} alt={user.displayName} size="sm" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-[var(--color-text-primary)] truncate">
                  {user.displayName}
                </p>
                <p className="text-xs text-[var(--color-text-tertiary)] truncate">@{user.username}</p>
              </div>
            </Link>
          )}
          {!isCollapsed && (
            <div className="mt-3 space-y-2">
              <div className="relative">
                <button
                  onClick={() => setAppearanceOpen((open) => !open)}
                  className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-bg-hover)] transition-colors"
                  aria-expanded={appearanceOpen}
                >
                  <Palette className="w-5 h-5" />
                  <span className="font-medium flex-1 text-left">Theme & Colors</span>
                  <span className="w-3 h-3 rounded-full bg-[var(--color-primary)] ring-2 ring-[var(--color-primary)]/20" />
                </button>
                {appearanceOpen && (
                  <div className="absolute left-0 bottom-12 z-[60]">
                    <AppearanceMenu />
                  </div>
                )}
              </div>
              <button
                onClick={logout}
                className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-[var(--color-text-secondary)] hover:text-[var(--color-error)] hover:bg-[var(--color-error)]/10 transition-colors"
              >
                <LogOut className="w-5 h-5" />
                <span className="font-medium">Sign Out</span>
              </button>
            </div>
          )}
          {isCollapsed && (
            <div className="flex flex-col items-center gap-2">
              <div className="relative">
                <button
                  onClick={() => setAppearanceOpen((open) => !open)}
                  className="p-2 rounded-lg text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-bg-hover)] transition-colors"
                  aria-label="Open theme and color palette"
                  aria-expanded={appearanceOpen}
                >
                  <Palette className="w-5 h-5" />
                </button>
                {appearanceOpen && (
                  <div className="absolute left-10 bottom-0 z-[60]">
                    <AppearanceMenu />
                  </div>
                )}
              </div>
              <button
                onClick={logout}
                className="p-2 rounded-lg text-[var(--color-text-secondary)] hover:text-[var(--color-error)] hover:bg-[var(--color-error)]/10 transition-colors"
                aria-label="Sign out"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          )}
        </div>
      </aside>
    </>
  );
}