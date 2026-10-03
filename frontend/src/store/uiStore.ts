import { create } from 'zustand';

interface UIState {
  sidebarOpen: boolean;
  sidebarCollapsed: boolean;
  mobileMenuOpen: boolean;
  searchOpen: boolean;
  uploadModalOpen: boolean;
  videoPlayerFullscreen: boolean;
  setSidebarOpen: (open: boolean) => void;
  toggleSidebar: () => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  setMobileMenuOpen: (open: boolean) => void;
  setSearchOpen: (open: boolean) => void;
  setUploadModalOpen: (open: boolean) => void;
  setVideoPlayerFullscreen: (fullscreen: boolean) => void;
}

export const useUIStore = create<UIState>((set) => ({
  sidebarOpen: false,
  sidebarCollapsed: false,
  mobileMenuOpen: false,
  searchOpen: false,
  uploadModalOpen: false,
  videoPlayerFullscreen: false,
  setSidebarOpen: (sidebarOpen) => set({ sidebarOpen }),
  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  setSidebarCollapsed: (sidebarCollapsed) => set({ sidebarCollapsed }),
  setMobileMenuOpen: (mobileMenuOpen) => set({ mobileMenuOpen }),
  setSearchOpen: (searchOpen) => set({ searchOpen }),
  setUploadModalOpen: (uploadModalOpen) => set({ uploadModalOpen }),
  setVideoPlayerFullscreen: (videoPlayerFullscreen) => set({ videoPlayerFullscreen }),
}));