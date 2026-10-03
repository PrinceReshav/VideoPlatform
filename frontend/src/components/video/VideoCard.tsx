import { Play } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Clock, Eye, User, MoreVertical, Menu, X } from 'lucide-react';
import { clsx } from 'clsx';
import { formatDistanceToNow } from 'date-fns';
import { Card } from '../ui/Card';
import { Avatar } from '../ui/Avatar';
import { Button } from '../ui/Button';

interface VideoCardProps {
  video: {
    id: string;
    title: string;
    slug: string;
    thumbnailUrl?: string;
    durationSeconds?: number;
    viewCount: number;
    publishedAt: string;
    channel: {
      id: string;
      name: string;
      slug: string;
      avatarUrl?: string;
    };
    isLive?: boolean;
  };
  variant?: 'default' | 'compact' | 'large';
  showChannel?: boolean;
}

export function VideoCard({ video, variant = 'default', showChannel = true }: VideoCardProps) {
  const formatDuration = (seconds?: number) => {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const formatViews = (count: number) => {
    if (count >= 1000000) return `${(count / 1000000).toFixed(1)}M`;
    if (count >= 1000) return `${(count / 1000).toFixed(1)}K`;
    return count.toString();
  };

  const isCompact = variant === 'compact';
  const isLarge = variant === 'large';

  return (
    <Link to={`/watch/${video.slug || video.id}`} className="group block video-card">
      <div className="relative">
        <div className="video-card-thumbnail aspect-video w-full overflow-hidden rounded-xl bg-[var(--color-bg-tertiary)] relative">
          {video.thumbnailUrl ? (
            <img
              src={video.thumbnailUrl}
              alt=""
              className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
              loading="lazy"
            />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-secondary)]/20 flex items-center justify-center">
              <Play className="w-12 h-12 text-[var(--color-text-tertiary)]" />
            </div>
          )}

          {/* Duration badge */}
          {video.durationSeconds && (
            <span className="absolute bottom-2 right-2 bg-black/90 text-white text-xs font-medium px-1.5 py-0.5 rounded">
              {formatDuration(video.durationSeconds)}
            </span>
          )}

          {/* Live badge */}
          {video.isLive && (
            <span className="absolute top-2 left-2 bg-[var(--color-error)] text-white text-xs font-bold px-2 py-0.5 rounded animate-pulse flex items-center gap-1">
              <span className="w-1.5 h-1.5 bg-white rounded-full" />
              LIVE
            </span>
          )}
        </div>
      </div>

      <div className="mt-3 flex gap-3 px-0.5">
        {showChannel && video.channel?.avatarUrl && (
          <Avatar src={video.channel.avatarUrl} alt={video.channel.name} size="sm" />
        )}

        <div className="flex-1 min-w-0">
          <h3 className={clsx(
            'video-card-title font-semibold text-[var(--color-text-primary)] line-clamp-2 transition-colors',
            'group-hover:text-[var(--color-primary)]',
            isCompact ? 'text-sm' : isLarge ? 'text-lg' : 'text-base'
          )}>
            {video.title}
          </h3>

          <div className="mt-1 flex flex-wrap items-center gap-2 text-[var(--color-text-tertiary)] text-sm">
            {showChannel && video.channel?.name && (
              <Link to={`/channel/${video.channel.slug}`} className="hover:text-[var(--color-primary)] transition-colors font-medium">
                {video.channel.name}
              </Link>
            )}

            <span>•</span>
            <span>{formatViews(video.viewCount)} views</span>
            <span>•</span>
            <span>{formatDistanceToNow(new Date(video.publishedAt), { addSuffix: true })}</span>
          </div>
        </div>

        {!isCompact && (
          <Button variant="ghost" size="icon" className="opacity-0 group-hover:opacity-100 transition-opacity">
            <MoreVertical className="w-5 h-5" />
          </Button>
        )}
      </div>
    </Link>
  );
}