import { Outlet } from 'react-router-dom';
import { Header } from '../components/layout/Header';
import { CreatorSidebar } from '../components/layout/CreatorSidebar';
import { useUIStore } from '../store/uiStore';

export function CreatorLayout() {
  const { sidebarOpen, sidebarCollapsed, setSidebarOpen } = useUIStore();

  return (
    <div className="min-h-screen bg-[var(--color-bg-primary)] flex flex-col">
      <Header onMenuClick={() => setSidebarOpen(!sidebarOpen)} />
      <div className="flex flex-1 overflow-hidden">
        <CreatorSidebar
          isOpen={sidebarOpen}
          isCollapsed={sidebarCollapsed}
          onClose={() => setSidebarOpen(false)}
        />
        <main className="flex-1 min-w-0 min-h-0 overflow-y-auto transition-colors duration-300">
          <div className="page-shell"><Outlet /></div>
        </main>
      </div>
    </div>
  );
}