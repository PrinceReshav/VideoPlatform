import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Users, Video, FileText, Shield, TrendingUp, Clock, ArrowRight } from 'lucide-react';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { adminApi } from '../services/api/adminApi.ts';
import { videoApi } from '../services/api/videoApi.ts';
import { engagementApi } from '../services/api/engagementApi.ts';
import { authApi } from '../services/api/authApi.ts';
import { formatDistanceToNow } from 'date-fns';

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  trend?: string;
  href?: string;
}

function StatCard({ title, value, icon, trend, href }: StatCardProps) {
  return (
    <Card className="p-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-[var(--color-text-secondary)]">{title}</p>
          <p className="text-3xl font-bold text-[var(--color-text-primary)] mt-1">{value}</p>
          {trend && <p className="text-sm text-[var(--color-success)] mt-1">{trend}</p>}
        </div>
        <div className="w-12 h-12 rounded-xl bg-[var(--color-primary)]/10 flex items-center justify-center">
          {icon}
        </div>
      </div>
      {href && (
        <Link to={href} className="mt-4 inline-flex items-center gap-1 text-sm text-[var(--color-primary)] hover:underline">
          View details <ArrowRight className="w-3 h-3" />
        </Link>
      )}
    </Card>
  );
}

export function AdminDashboardPage() {
  const { data: users } = useQuery({
    queryKey: ['adminUsers', '', 'all', 'all', 0],
    queryFn: () => adminApi.getUsers({ search: '', role: 'all', status: 'all', page: 0, size: 1 }),
  });

  const { data: videos } = useQuery({
    queryKey: ['adminVideos', '', 'all', 'all', 0],
    queryFn: () => adminApi.getVideos({ search: '', status: 'all', visibility: 'all', page: 0, size: 1 }),
  });

  const { data: reports } = useQuery({
    queryKey: ['adminReports', '', 'all', 'all', 0],
    queryFn: () => adminApi.getReports({ search: '', status: 'all', type: 'all', page: 0, size: 1 }),
  });

  const { data: publishedVideos } = useQuery({
    queryKey: ['videos', 'home'],
    queryFn: () => videoApi.getPublished({ page: 0, size: 1 }),
  });

  return (
    <div className="max-w-7xl mx-auto p-4 space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Admin Dashboard</h1>
        <p className="text-[var(--color-text-secondary)]">Platform overview and quick actions</p>
      </div>

      {/* Stats Grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          title="Total Users"
          value={users?.totalElements?.toLocaleString() || '—'}
          icon={<Users className="w-6 h-6 text-[var(--color-primary)]" />}
          href="/admin/users"
        />
        <StatCard
          title="Total Videos"
          value={videos?.totalElements?.toLocaleString() || '—'}
          icon={<Video className="w-6 h-6 text-[var(--color-primary)]" />}
          href="/admin/videos"
        />
        <StatCard
          title="Pending Reports"
          value={reports?.totalElements?.toLocaleString() || '—'}
          icon={<FileText className="w-6 h-6 text-[var(--color-warning)]" />}
          href="/admin/reports"
        />
        <StatCard
          title="Categories"
          value="—"
          icon={<Shield className="w-6 h-6 text-[var(--color-primary)]" />}
          href="/admin/categories"
        />
      </div>

      {/* Quick Actions */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Link to="/admin/users" className="block">
          <Card className="p-5 hover:bg-[var(--color-bg-hover)] transition-colors h-full">
            <div className="w-12 h-12 rounded-xl bg-[var(--color-primary)]/10 flex items-center justify-center mb-3">
              <Users className="w-6 h-6 text-[var(--color-primary)]" />
            </div>
            <h3 className="font-semibold text-[var(--color-text-primary)]">Manage Users</h3>
            <p className="text-sm text-[var(--color-text-secondary)] mt-1">View, ban, or change roles</p>
          </Card>
        </Link>
        <Link to="/admin/videos" className="block">
          <Card className="p-5 hover:bg-[var(--color-bg-hover)] transition-colors h-full">
            <div className="w-12 h-12 rounded-xl bg-[var(--color-primary)]/10 flex items-center justify-center mb-3">
              <Video className="w-6 h-6 text-[var(--color-primary)]" />
            </div>
            <h3 className="font-semibold text-[var(--color-text-primary)]">Moderate Videos</h3>
            <p className="text-sm text-[var(--color-text-secondary)] mt-1">Block, unblock, or delete</p>
          </Card>
        </Link>
        <Link to="/admin/reports" className="block">
          <Card className="p-5 hover:bg-[var(--color-bg-hover)] transition-colors h-full">
            <div className="w-12 h-12 rounded-xl bg-[var(--color-warning)]/10 flex items-center justify-center mb-3">
              <FileText className="w-6 h-6 text-[var(--color-warning)]" />
            </div>
            <h3 className="font-semibold text-[var(--color-text-primary)]">Review Reports</h3>
            <p className="text-sm text-[var(--color-text-secondary)] mt-1">Resolve or dismiss reports</p>
          </Card>
        </Link>
        <Link to="/admin/categories" className="block">
          <Card className="p-5 hover:bg-[var(--color-bg-hover)] transition-colors h-full">
            <div className="w-12 h-12 rounded-xl bg-[var(--color-success)]/10 flex items-center justify-center mb-3">
              <Shield className="w-6 h-6 text-[var(--color-success)]" />
            </div>
            <h3 className="font-semibold text-[var(--color-text-primary)]">Manage Taxonomy</h3>
            <p className="text-sm text-[var(--color-text-secondary)] mt-1">Categories, genres, languages</p>
          </Card>
        </Link>
      </div>

      {/* Recent Activity */}
      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <div className="p-4 border-b border-[var(--color-border)]">
            <h2 className="text-lg font-semibold text-[var(--color-text-primary)] flex items-center gap-2">
              <TrendingUp className="w-5 h-5" />
              Platform Stats
            </h2>
          </div>
          <div className="p-4 space-y-4">
            <div className="flex items-center justify-between py-2 border-b border-[var(--color-border)]">
              <span className="text-[var(--color-text-secondary)]">Published Videos</span>
              <span className="font-medium text-[var(--color-text-primary)]">{publishedVideos?.totalElements?.toLocaleString() || '—'}</span>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-[var(--color-border)]">
              <span className="text-[var(--color-text-secondary)]">Registered Users</span>
              <span className="font-medium text-[var(--color-text-primary)]">{users?.totalElements?.toLocaleString() || '—'}</span>
            </div>
            <div className="flex items-center justify-between py-2 border-b border-[var(--color-border)]">
              <span className="text-[var(--color-text-secondary)]">Total Videos</span>
              <span className="font-medium text-[var(--color-text-primary)]">{videos?.totalElements?.toLocaleString() || '—'}</span>
            </div>
            <div className="flex items-center justify-between py-2">
              <span className="text-[var(--color-text-secondary)]">Open Reports</span>
              <span className="font-medium text-[var(--color-text-primary)]">{reports?.totalElements?.toLocaleString() || '—'}</span>
            </div>
          </div>
        </Card>

        <Card>
          <div className="p-4 border-b border-[var(--color-border)]">
            <h2 className="text-lg font-semibold text-[var(--color-text-primary)] flex items-center gap-2">
              <Clock className="w-5 h-5" />
              Quick Links
            </h2>
          </div>
          <div className="p-4 space-y-3">
            <Link to="/admin/users" className="block p-3 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-[var(--color-primary)]/10 flex items-center justify-center">
                <Users className="w-5 h-5 text-[var(--color-primary)]" />
              </div>
              <span className="text-[var(--color-text-primary)]">User Management</span>
            </Link>
            <Link to="/admin/videos" className="block p-3 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-[var(--color-primary)]/10 flex items-center justify-center">
                <Video className="w-5 h-5 text-[var(--color-primary)]" />
              </div>
              <span className="text-[var(--color-text-primary)]">Video Moderation</span>
            </Link>
            <Link to="/admin/reports" className="block p-3 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-[var(--color-warning)]/10 flex items-center justify-center">
                <FileText className="w-5 h-5 text-[var(--color-warning)]" />
              </div>
              <span className="text-[var(--color-text-primary)]">Content Reports</span>
            </Link>
            <Link to="/admin/categories" className="block p-3 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors flex items-center gap-3">
              <div className="w-10 h-10 rounded-lg bg-[var(--color-success)]/10 flex items-center justify-center">
                <Shield className="w-5 h-5 text-[var(--color-success)]" />
              </div>
              <span className="text-[var(--color-text-primary)]">Taxonomy Management</span>
            </Link>
          </div>
        </Card>
      </div>
    </div>
  );
}