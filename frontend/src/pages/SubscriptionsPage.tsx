import { Button } from '../components/ui/Button.tsx';
import { Users } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { VideoCard } from '../components/video/VideoCard.tsx';
import { Card } from '../components/ui/Card.tsx';
import { subscriptionApi } from '../services/api/subscriptionApi.ts';
import { videoApi } from '../services/api/videoApi.ts';
import { Bell, Play, Clock } from 'lucide-react';
import { Card as CardComp } from '../components/ui/Card.tsx';

export function SubscriptionsPage() {
  const { data: feed, isLoading } = useQuery({
    queryKey: ['subscriptionFeed'],
    queryFn: () => subscriptionApi.getFeed({ page: 0, size: 20 }),
  });

  const { data: subscriptions, isLoading: subsLoading } = useQuery({
    queryKey: ['mySubscriptions'],
    queryFn: () => subscriptionApi.getMySubscriptions({ page: 0, size: 50 }),
  });

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Subscriptions</h1>
          <p className="text-[var(--color-text-secondary)]">Latest videos from channels you subscribe to</p>
        </div>
        <Link to="/channels" className="text-sm text-[var(--color-primary)] hover:underline">
          Manage subscriptions
        </Link>
      </div>

      {/* Subscribed Channels Bar */}
      {!subsLoading && subscriptions?.content?.length > 0 && (
        <div className="mb-6">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">Subscribed Channels</h2>
            <Link to="/channels" className="text-sm text-[var(--color-primary)] hover:underline">
              View all
            </Link>
          </div>
          <div className="flex gap-3 overflow-x-auto pb-4">
            {subscriptions.content.slice(0, 10).map((sub: any) => (
              <Link key={sub.channelId} to={`/channel/${sub.channelSlug}`} className="flex flex-col items-center gap-2 shrink-0 w-32">
                <div className="w-24 h-24 rounded-full bg-[var(--color-bg-tertiary)] overflow-hidden border-2 border-[var(--color-border)]">
                  {sub.channelAvatarUrl ? (
                    <img src={sub.channelAvatarUrl} alt="" className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-secondary)]/20 flex items-center justify-center">
                      <Users className="w-8 h-8 text-[var(--color-text-tertiary)]" />
                    </div>
                  )}
                </div>
                <span className="text-xs font-medium text-[var(--color-text-primary)] truncate w-full text-center">
                  {sub.channelName}
                </span>
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Feed */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)] flex items-center gap-2">
            <Bell className="w-5 h-5" />
            Subscription Feed
          </h2>
        </div>

        {isLoading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {[...Array(8)].map((_, i) => <VideoCardSkeleton key={i} />)}
          </div>
        ) : feed?.content?.length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {feed.content.map((video: any) => (
              <VideoCard key={video.id} video={video} />
            ))}
          </div>
        ) : (
          <div className="text-center py-16">
            <Bell className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4 opacity-50" />
            <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No subscription feed yet</h3>
            <p className="text-[var(--color-text-secondary)] mb-6">Subscribe to channels to see their latest videos here.</p>
            <Link to="/search">
              <Button>Explore Channels</Button>
            </Link>
          </div>
        )}
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