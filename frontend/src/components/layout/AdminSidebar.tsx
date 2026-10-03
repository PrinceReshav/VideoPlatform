import { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Home, Users, Video, FileText, Shield, Settings, LogOut, Sun, Moon, X, LayoutDashboard } from 'lucide-react';
import { clsx } from 'clsx';
import { useAuth } from '../../app/providers/AuthProvider';
import { useTheme } from '../../app/providers/ThemeProvider';
import { useUIStore } from '../../store/uiStore';
import { Button } from '../ui/Button';
import { Avatar } from '../ui/Avatar';

interface AdminSidebarProps {
  isOpen: boolean;
  isCollapsed: boolean;
  onClose: () => void;
}

const adminNavigation = [
  { name: 'Dashboard', href: '/admin', icon: LayoutDashboard },
  { name: 'Users', href: '/admin/users', icon: Users },
  { name: 'Videos', href: '/admin/videos', icon: Video },
  { name: 'Reports', href: '/admin/reports', icon: FileText },
  { name: 'Categories', href: '/admin/categories', icon: Shield },
  { name: 'Settings', href: '/admin/settings', icon: Settings },
];

export function AdminSidebar({ isOpen, isCollapsed, onClose }: AdminSidebarProps) {
  const location = useLocation();
  const { toggleTheme, resolvedTheme } = useTheme();
  const { user, logout } = useAuth();

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
        aria-label="Admin navigation"
      >
        <div className="flex h-16 items-center justify-between px-4 border-b border-[var(--color-border)]">
          {!isCollapsed && (
            <Link to="/admin" className="flex items-center gap-2" aria-label="Admin Dashboard">
              <div className="w-8 h-8 rounded-lg bg-[var(--color-error)] flex items-center justify-center">
                <Shield className="w-5 h-5 text-white" />
              </div>
              <span className="font-bold text-lg text-[var(--color-text-primary)]">Admin Panel</span>
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

        <nav className="flex-1 overflow-y-auto py-4 px-2" aria-label="Admin navigation">
          <ul className="space-y-1" role="list">
            {adminNavigation.map((item) => (
              <li key={item.name}>
                <Link
                  to={item.href}
                  className={clsx(
                    'relative flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200',
                    'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]',
                    'hover:bg-[var(--color-bg-hover)]',
                    location.pathname.startsWith(item.href)
                      ? 'sidebar-active'
                      : '',
                    isCollapsed && 'justify-center px-2'
                  )}
                  aria-current={location.pathname.startsWith(item.href) ? 'page' : undefined}
                >
                  <item.icon className="w-5 h-5 flex-shrink-0" aria-hidden="true" />
                  {!isCollapsed && <span className="font-medium">{item.name}</span>}
                </Link>
              </li>
            ))}
          </ul>
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
              <button
                onClick={toggleTheme}
                className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-bg-hover)] transition-colors"
              >
                {resolvedTheme === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
                <span className="font-medium">
                  {resolvedTheme === 'dark' ? 'Light Mode' : 'Dark Mode'}
                </span>
              </button>
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
              <button
                onClick={toggleTheme}
                className="p-2 rounded-lg text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] hover:bg-[var(--color-bg-hover)] transition-colors"
                aria-label={resolvedTheme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
              >
                {resolvedTheme === 'dark' ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
              </button>
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