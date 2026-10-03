import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Play, Clock, Eye, Users, MoreVertical, Search, Filter, Flame, Video } from 'lucide-react';
import { clsx } from 'clsx';
import { formatDistanceToNow } from 'date-fns';
import { VideoCard } from '../components/video/VideoCard.tsx';
import { videoApi } from '../services/api/videoApi.ts';
import { Button } from '../components/ui/Button.tsx';
import { Input } from '../components/ui/Input.tsx';
import { Skeleton } from '../components/ui/Skeleton.tsx';

export function HomePage() {
  const { data: videos, isLoading } = useQuery({
    queryKey: ['videos', 'home'],
    queryFn: () => videoApi.getPublished({ page: 0, size: 20 }),
  });

  const { data: trending, isLoading: trendingLoading } = useQuery({
    queryKey: ['videos', 'trending'],
    queryFn: () => videoApi.getPublished({ page: 0, size: 10, sort: 'viewCount' }),
  });

  const categories = ['All', 'Live TV', 'Music', 'Gaming', 'Technology', 'News', 'Education', 'Podcasts', 'Entertainment', 'Sports'];

  return (
    <div className="space-y-7">
      <div className="sticky top-16 z-30 -mx-4 md:-mx-6 lg:-mx-8 px-4 md:px-6 lg:px-8 py-3 bg-[var(--color-bg-primary)]/95 backdrop-blur-xl border-b border-[var(--color-border)] overflow-x-auto">
        <div className="flex items-center gap-2 min-w-max">
          {categories.map((category, index) => (
            <Link key={category} to={index === 0 ? '/' : `/search?q=${encodeURIComponent(category)}`} className={`category-chip ${index === 0 ? 'active' : ''}`}>
              {category}
            </Link>
          ))}
        </div>
      </div>
      {/* Hero Section */}
      <section className="relative overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--color-shadow-sm)]">
        <div className="absolute inset-0 opacity-80" style={{ background: `radial-gradient(circle at 15% 20%, color-mix(in srgb, var(--color-primary) 18%, transparent), transparent 38%), radial-gradient(circle at 85% 15%, color-mix(in srgb, var(--color-secondary) 16%, transparent), transparent 34%)` }} />
        <div className="relative px-4 py-16 md:py-24 max-w-7xl mx-auto">
          <h1 className="text-4xl md:text-5xl font-bold text-[var(--color-text-primary)] mb-4">
            Your world of video, built around you
          </h1>
          <p className="text-lg text-[var(--color-text-secondary)] mb-8 max-w-2xl">
            Explore trending videos, follow creators, build playlists, and discover your next favorite channel — with a personalized experience that feels fast, familiar, and yours.
          </p>
          <div className="flex flex-wrap gap-4">
            <Link to="/search">
              <Button size="lg" className="gap-2">
                <Search className="w-5 h-5" />
                Explore Videos
              </Button>
            </Link>
            <Link to="/creator">
              <Button variant="outline" size="lg">
                Start creating
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Trending Now */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[var(--color-warning)]/10 flex items-center justify-center">
              <Flame className="w-5 h-5 text-[var(--color-warning)]" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-[var(--color-text-primary)]">Trending Now</h2>
              <p className="text-sm text-[var(--color-text-secondary)]">Most viewed this week</p>
            </div>
          </div>
          <Link to="/search?sort=viewCount" className="text-sm text-[var(--color-primary)] hover:underline">
            View all
          </Link>
        </div>

        {trendingLoading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {[...Array(8)].map((_, i) => <VideoSkeleton key={i} />)}
          </div>
        ) : trending?.content?.length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {trending.content.map((video: any) => (
              <VideoCard key={video.id} video={video} />
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-[var(--color-text-secondary)]">
            No trending videos yet
          </div>
        )}
      </section>

      {/* Latest Videos */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[var(--color-info)]/10 flex items-center justify-center">
              <Clock className="w-5 h-5 text-[var(--color-info)]" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-[var(--color-text-primary)]">Latest Videos</h2>
              <p className="text-sm text-[var(--color-text-secondary)]">Just uploaded</p>
            </div>
          </div>
          <Link to="/search?sort=publishedAt" className="text-sm text-[var(--color-primary)] hover:underline">
            View all
          </Link>
        </div>

        {isLoading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {[...Array(12)].map((_, i) => <VideoSkeleton key={i} />)}
          </div>
        ) : videos?.content?.length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {videos.content.map((video: any) => (
              <VideoCard key={video.id} video={video} />
            ))}
          </div>
        ) : (
          <div className="text-center py-12 text-[var(--color-text-secondary)]">
            <Video className="w-12 h-12 mx-auto text-[var(--color-text-tertiary)] mb-4" />
            <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No videos yet</h3>
            <p className="text-[var(--color-text-secondary)]">Be the first to upload!</p>
          </div>
        )}
      </section>
    </div>
  );
}

function VideoSkeleton() {
  return (
    <Link className="block" to="#">
      <div className="aspect-video w-full overflow-hidden rounded-xl bg-[var(--color-bg-tertiary)] animate-pulse" />
      <div className="mt-3 flex gap-3">
        <div className="w-10 h-10 rounded-full bg-[var(--color-bg-tertiary)] animate-pulse" />
        <div className="flex-1">
          <div className="h-4 w-3/4 bg-[var(--color-bg-tertiary)] rounded animate-pulse mb-2" />
          <div className="h-3 w-1/2 bg-[var(--color-bg-tertiary)] rounded animate-pulse" />
        </div>
      </div>
    </Link>
  );
}
