import { clsx } from 'clsx';
import { Eye, Users } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Plus, Video, Clock, BarChart2, Settings, Search, Upload } from 'lucide-react';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { VideoCard } from '../components/video/VideoCard.tsx';

interface CreatorDashboardProps {
  stats?: {
    totalViews: number;
    totalWatchTime: number;
    subscribers: number;
    videos: number;
  };
  recentVideos?: any[];
}

export function CreatorDashboardPage({ stats, recentVideos }: CreatorDashboardProps) {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Creator Studio</h1>
          <p className="text-[var(--color-text-secondary)] mt-1">Manage your channel and content</p>
        </div>
        <Link to="/creator/upload" className="flex items-center gap-2">
          <Button>
            <Upload className="w-4 h-4" />
            Upload Video
          </Button>
        </Link>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard title="Views" value={stats?.totalViews?.toLocaleString() || '0'} icon={Eye} color="primary" />
        <StatCard title="Watch Time" value={`${(stats?.totalWatchTime || 0) / 3600}h`} icon={Clock} color="success" />
        <StatCard title="Subscribers" value={stats?.subscribers?.toLocaleString() || '0'} icon={Users} color="secondary" />
        <StatCard title="Videos" value={stats?.videos?.toLocaleString() || '0'} icon={Video} color="info" />
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Link to="/creator/upload">
          <ActionCard icon={Upload} title="Upload Video" description="Add new content to your channel" />
        </Link>
        <Link to="/creator/videos">
          <ActionCard icon={Video} title="My Videos" description="Manage your uploaded videos" />
        </Link>
        <Link to="/creator/analytics">
          <ActionCard icon={BarChart2} title="Analytics" description="View channel performance" />
        </Link>
        <Link to="/creator/settings">
          <ActionCard icon={Settings} title="Settings" description="Channel and account settings" />
        </Link>
      </div>

      {/* Recent Videos */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">Recent Videos</h2>
          <Link to="/creator/videos" className="text-sm text-[var(--color-primary)] hover:underline">
            View all
          </Link>
        </div>
        {recentVideos?.length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {recentVideos.map((video: any) => (
              <VideoCard key={video.id} video={video} variant="default" />
            ))}
          </div>
        ) : (
          <Card className="text-center py-12">
            <Video className="w-12 h-12 mx-auto text-[var(--color-text-tertiary)] mb-4" />
            <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No videos yet</h3>
            <p className="text-[var(--color-text-secondary)] mb-4">Start by uploading your first video</p>
            <Link to="/creator/upload">
              <Button>Upload Video</Button>
            </Link>
          </Card>
        )}
      </div>
    </div>
  );
}

function StatCard({ title, value, icon: Icon, color }: { title: string; value: string; icon: any; color: string }) {
  return (
    <Card className="p-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-[var(--color-text-secondary)]">{title}</p>
          <p className="text-2xl font-bold text-[var(--color-text-primary)] mt-1">{value}</p>
        </div>
        <div className={clsx('p-3 rounded-xl', `bg-[var(--color-${color})]/10 text-[var(--color-${color})]`)}>
          <Icon className="w-6 h-6" />
        </div>
      </div>
    </Card>
  );
}

function ActionCard({ icon: Icon, title, description }: { icon: any; title: string; description: string }) {
  return (
    <Card className="p-5 hover:bg-[var(--color-bg-hover)] transition-colors cursor-pointer group">
      <div className={clsx('p-3 rounded-xl mb-4', 'bg-[var(--color-primary)]/10 text-[var(--color-primary)] group-hover:bg-[var(--color-primary)] group-hover:text-white transition-colors')}>
        <Icon className="w-6 h-6" />
      </div>
      <h3 className="font-semibold text-[var(--color-text-primary)] group-hover:text-[var(--color-primary)] transition-colors">{title}</h3>
      <p className="text-sm text-[var(--color-text-secondary)]">{description}</p>
    </Card>
  );
}