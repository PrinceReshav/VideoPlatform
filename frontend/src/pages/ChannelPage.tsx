import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useParams, Link } from 'react-router-dom';
import { VideoCard } from '../components/video/VideoCard.tsx';
import { Card } from '../components/ui/Card.tsx';
import { Avatar } from '../components/ui/Avatar.tsx';
import { Button } from '../components/ui/Button.tsx';
import { videoApi } from '../services/api/videoApi.ts';
import { subscriptionApi } from '../services/api/subscriptionApi.ts';
import { useAuth } from '../app/providers/AuthProvider.tsx';
import { formatDistanceToNow } from 'date-fns';
import { clsx } from 'clsx';
import { Video, Users, Bell, MoreHorizontal, Settings, Share } from 'lucide-react';

export function ChannelPage() {
  const { channelSlug } = useParams<{ channelSlug: string }>();
  const { isAuthenticated, user } = useAuth();
  const [isSubscribed, setIsSubscribed] = React.useState(false);
  const [subscriberCount, setSubscriberCount] = React.useState(0);

  const { data: channel, isLoading: channelLoading } = useQuery({
    queryKey: ['channel', channelSlug],
    queryFn: () => videoApi.getChannel(channelSlug!),
    enabled: !!channelSlug,
  });

  const { data: videos, isLoading: videosLoading } = useQuery({
    queryKey: ['channelVideos', channelSlug],
    queryFn: () => videoApi.getChannelVideos(channelSlug!, { page: 0, size: 20 }),
    enabled: !!channelSlug,
  });

  const handleSubscribe = async () => {
    if (!isAuthenticated) return;
    try {
      if (isSubscribed) {
        await subscriptionApi.unsubscribe(channel!.id);
        setSubscriberCount(prev => prev - 1);
      } else {
        await subscriptionApi.subscribe(channel!.id);
        setSubscriberCount(prev => prev + 1);
      }
      setIsSubscribed(!isSubscribed);
    } catch (error) {
      console.error('Subscribe failed:', error);
    }
  };

  if (channelLoading) {
    return <ChannelSkeleton />;
  }

  if (!channel) {
    return (
      <div className="max-w-4xl mx-auto p-4 text-center py-16">
        <Users className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4" />
        <h2 className="text-xl font-semibold text-[var(--color-text-primary)] mb-2">Channel not found</h2>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      {/* Channel Header */}
      <div className="space-y-4">
        <div className="flex flex-col md:flex-row md:items-center gap-6">
          <Avatar src={channel.avatarUrl} alt={channel.name} size="xl" />
          <div className="flex-1 min-w-0">
            <h1 className="text-3xl font-bold text-[var(--color-text-primary)] truncate">{channel.name}</h1>
            <p className="text-[var(--color-text-secondary)] mt-1">@{channel.slug}</p>
            {channel.description && (
              <p className="text-[var(--color-text-secondary)] mt-2 line-clamp-3">{channel.description}</p>
            )}
            <div className="flex flex-wrap items-center gap-4 mt-4 text-sm text-[var(--color-text-tertiary)]">
              <span>{channel.subscriberCount?.toLocaleString()} subscribers</span>
              <span>•</span>
              <span>{channel.videoCount} videos</span>
              <span>•</span>
              <span>{channel.totalViews?.toLocaleString()} views</span>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant={isSubscribed ? 'outline' : 'primary'} onClick={handleSubscribe} className="gap-2">
              <Users className="w-4 h-4" />
              {isSubscribed ? 'Subscribed' : 'Subscribe'}
            </Button>
            <Button variant="ghost" size="icon" aria-label="Notifications">
              <Bell className="w-4 h-4" />
            </Button>
            <Button variant="ghost" size="icon" aria-label="Share">
              <Share className="w-4 h-4" />
            </Button>
            <Button variant="ghost" size="icon" aria-label="More">
              <MoreHorizontal className="w-4 h-4" />
            </Button>
          </div>
        </div>

        {channel.bannerUrl && (
          <div className="aspect-video w-full overflow-hidden rounded-xl bg-[var(--color-bg-tertiary)]">
            <img src={channel.bannerUrl} alt="" className="w-full h-full object-cover" />
          </div>
        )}
      </div>

      {/* Channel Tabs */}
      <div className="border-b border-[var(--color-border)]">
        <nav className="flex gap-6" aria-label="Channel tabs">
          <button className="py-3 px-1 border-b-2 border-[var(--color-primary)] text-[var(--color-primary)] font-medium">
            Videos
          </button>
          <button className="py-3 px-1 border-b-2 border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] font-medium">
            Shorts
          </button>
          <button className="py-3 px-1 border-b-2 border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] font-medium">
            Live
          </button>
          <button className="py-3 px-1 border-b-2 border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] font-medium">
            Playlists
          </button>
          <button className="py-3 px-1 border-b-2 border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] font-medium">
            Community
          </button>
          <button className="py-3 px-1 border-b-2 border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] font-medium">
            Channels
          </button>
          <button className="py-3 px-1 border-b-2 border-transparent text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)] font-medium">
            About
          </button>
        </nav>
      </div>

      {/* Videos Grid */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)]">Uploads</h2>
          <select className="px-3 py-1.5 bg-[var(--color-surface)] border border-[var(--color-border)] rounded-lg text-[var(--color-text-primary)]">
            <option value="latest">Date added (newest)</option>
            <option value="popular">Most popular</option>
            <option value="oldest">Date added (oldest)</option>
          </select>
        </div>

        {videosLoading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {[...Array(8)].map((_, i) => <VideoCardSkeleton key={i} />)}
          </div>
        ) : videos?.content?.length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {videos.content.map((video: any) => (
              <VideoCard key={video.id} video={video} />
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <Video className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4" />
            <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No videos yet</h3>
            <p className="text-[var(--color-text-secondary)]">This channel doesn't have any videos yet.</p>
          </div>
        )}
      </div>
    </div>
  );
}

function ChannelSkeleton() {
  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      <div className="animate-pulse space-y-4">
        <div className="flex gap-6">
          <div className="w-24 h-24 rounded-full bg-[var(--color-bg-tertiary)]" />
          <div className="flex-1 space-y-3">
            <div className="h-8 w-3/4 bg-[var(--color-bg-tertiary)] rounded" />
            <div className="h-4 w-1/2 bg-[var(--color-bg-tertiary)] rounded" />
            <div className="h-4 w-1/3 bg-[var(--color-bg-tertiary)] rounded" />
          </div>
        </div>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {[...Array(8)].map((_, i) => <VideoCardSkeleton key={i} />)}
      </div>
    </div>
  );
}

function VideoCardSkeleton() {
  return (
    <div className="group block">
      <div className="aspect-video w-full overflow-hidden rounded-xl bg-[var(--color-bg-tertiary)] animate-pulse" />
      <div className="mt-3 flex gap-3">
        <div className="w-10 h-10 rounded-full bg-[var(--color-bg-tertiary)]" />
        <div className="flex-1">
          <div className="h-4 w-3/4 bg-[var(--color-bg-tertiary)] rounded mb-2" />
          <div className="h-3 w-1/2 bg-[var(--color-bg-tertiary)] rounded" />
        </div>
      </div>
    </div>
  );
}