import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './app/providers/AuthProvider';
import { MainLayout } from './layouts/MainLayout';
import { AuthLayout } from './layouts/AuthLayout';
import { CreatorLayout } from './layouts/CreatorLayout';
import { AdminLayout } from './layouts/AdminLayout';

// Pages
import { HomePage } from './pages/HomePage';
import { SearchPage } from './pages/SearchPage';
import { VideoWatchPage } from './pages/VideoWatchPage';
import { ChannelPage } from './pages/ChannelPage';
import { WatchHistoryPage } from './pages/WatchHistoryPage';
import { SubscriptionsPage } from './pages/SubscriptionsPage';
import { LikedVideosPage } from './pages/LikedVideosPage';
import { PlaylistsPage } from './pages/PlaylistsPage';
import { ProfilePage } from './pages/ProfilePage';
import { SettingsPage } from './pages/SettingsPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { CreatorDashboardPage } from './pages/CreatorDashboardPage';
import { UploadPage } from './pages/UploadPage';
import { MyVideosPage } from './pages/MyVideosPage';
import { VideoEditPage } from './pages/VideoEditPage';
import { AdminDashboardPage } from './pages/AdminDashboardPage';
import { AdminUsersPage } from './pages/AdminUsersPage';
import { AdminVideosPage } from './pages/AdminVideosPage';
import { AdminReportsPage } from './pages/AdminReportsPage';
import { AdminCategoriesPage } from './pages/AdminCategoriesPage';
import { LiveTvPage } from './pages/LiveTvPage';
import { NotFoundPage } from './pages/NotFoundPage';

function PrivateRoute({ children, requiredRole }: { children: React.ReactNode; requiredRole?: string }) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: window.location.pathname }} />;
  }

  if (requiredRole && user?.roles && !user.roles.includes(requiredRole)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

function CreatorRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: window.location.pathname }} />;
  }

  if (!user?.roles?.includes('CREATOR')) {
    return <Navigate to="/creator" replace />;
  }

  return <>{children}</>;
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: window.location.pathname }} />;
  }

  if (!user?.roles?.includes('ADMIN')) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

export default function App() {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/" element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="watch/:videoId" element={<VideoWatchPage />} />
        <Route path="channel/:channelSlug" element={<ChannelPage />} />
        <Route path="category/:categorySlug" element={<SearchPage />} />
        <Route path="genre/:genreSlug" element={<SearchPage />} />
        <Route path="live" element={<LiveTvPage />} />
      </Route>

      {/* Auth Routes */}
      <Route element={<AuthLayout />}>
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />
      </Route>

      {/* Protected User Routes */}
      <Route element={<PrivateRoute><MainLayout /></PrivateRoute>}>
        <Route path="history" element={<WatchHistoryPage />} />
        <Route path="subscriptions" element={<SubscriptionsPage />} />
        <Route path="liked" element={<LikedVideosPage />} />
        <Route path="playlists" element={<PlaylistsPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>

      {/* Creator Routes */}
      <Route element={<PrivateRoute requiredRole="CREATOR"><CreatorLayout /></PrivateRoute>}>
        <Route path="creator" element={<CreatorDashboardPage />} />
        <Route path="creator/upload" element={<UploadPage />} />
        <Route path="creator/videos" element={<MyVideosPage />} />
        <Route path="creator/videos/:videoId/edit" element={<VideoEditPage />} />
      </Route>

      {/* Admin Routes */}
      <Route element={<PrivateRoute requiredRole="ADMIN"><AdminLayout /></PrivateRoute>}>
        <Route path="admin" element={<AdminDashboardPage />} />
        <Route path="admin/users" element={<AdminUsersPage />} />
        <Route path="admin/videos" element={<AdminVideosPage />} />
        <Route path="admin/reports" element={<AdminReportsPage />} />
        <Route path="admin/categories" element={<AdminCategoriesPage />} />
      </Route>

      {/* 404 */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}