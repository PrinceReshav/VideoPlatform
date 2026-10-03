import { Button } from '../components/ui/Button.tsx';
import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { VideoCard } from '../components/video/VideoCard.tsx';
import { engagementApi } from '../services/api/engagementApi.ts';
import { Heart, Trash2 } from 'lucide-react';

export function LikedVideosPage() {
  const [page, setPage] = React.useState(0);

  const { data: likedVideos, isLoading } = useQuery({
    queryKey: ['likedVideos', page],
    queryFn: () => engagementApi.getLikedVideos({ page, size: 20 }),
  });

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)] flex items-center gap-3">
            <Heart className="w-8 h-8 text-[var(--color-error)]" />
            Liked Videos
          </h1>
          <p className="text-[var(--color-text-secondary)]">Videos you've liked</p>
        </div>
      </div>

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {[...Array(12)].map((_, i) => <VideoCardSkeleton key={i} />)}
        </div>
      ) : likedVideos?.content?.length > 0 ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {likedVideos.content.map((video: any) => (
              <VideoCard key={video.id} video={video} />
            ))}
          </div>

          {/* Pagination */}
          {likedVideos.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-8">
              <Button
                variant="outline"
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
              >
                Previous
              </Button>
              <span className="px-4 text-[var(--color-text-secondary)]">
                Page {page + 1} of {likedVideos.totalPages}
              </span>
              <Button
                variant="outline"
                disabled={page >= likedVideos.totalPages - 1}
                onClick={() => setPage(p => p + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </>
      ) : (
        <div className="text-center py-16">
          <Heart className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4 opacity-50" />
          <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No liked videos yet</h3>
          <p className="text-[var(--color-text-secondary)] mb-6">Like videos to save them here.</p>
          <Link to="/">
            <Button>Explore Videos</Button>
          </Link>
        </div>
      )}
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