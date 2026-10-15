import { Outlet, useLocation } from 'react-router-dom';
import { Header } from '../components/layout/Header';
import { Sidebar } from '../components/layout/Sidebar';
import { useUIStore } from '../store/uiStore';

export function MainLayout() {
  const { sidebarOpen, sidebarCollapsed, setSidebarOpen } = useUIStore();
  const location = useLocation();
  const isLiveTv = location.pathname === '/live' || location.pathname.startsWith('/live/');

  if (isLiveTv) {
    return (
      <div className="min-h-screen bg-[#07090d] text-white">
        <Outlet />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[var(--color-bg-primary)] flex flex-col">
      <Header onMenuClick={() => setSidebarOpen(!sidebarOpen)} />
      <div className="flex flex-1 min-h-0">
        <Sidebar isOpen={sidebarOpen} isCollapsed={sidebarCollapsed} onClose={() => setSidebarOpen(false)} />
        <main className="flex-1 min-w-0 min-h-0 overflow-y-auto transition-colors duration-300">
          <div className="page-shell">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
