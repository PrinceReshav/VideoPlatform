import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { engagementApi } from '../services/api/engagementApi.ts';
import { Video, Clock, Play } from 'lucide-react';
import { Select, SelectTrigger, SelectValue, SelectContent, SelectItem } from '../components/ui/Select.tsx';
import { Button } from '../components/ui/Button.tsx';

export function WatchHistoryPage() {
  const [filter, setFilter] = useState<'all' | 'completed' | 'in-progress'>('all');

  const { data: historyResponse, isLoading } = useQuery({
    queryKey: ['watchHistory', filter],
    queryFn: () => engagementApi.getWatchHistory({ page: 0, size: 20 }),
  });

  const { data: continueWatchingResponse } = useQuery({
    queryKey: ['continueWatching'],
    queryFn: () => engagementApi.getContinueWatching(),
  });

  const history = historyResponse?.data;
  const continueWatching = continueWatchingResponse?.data;

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">Watch History</h1>
          <p className="text-[var(--color-text-secondary)]">Videos you've watched</p>
        </div>
        <div className="flex items-center gap-2">
          <Select value={filter} onValueChange={(value: string) => setFilter(value as 'all' | 'completed' | 'in-progress')} className="w-48">
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All</SelectItem>
              <SelectItem value="in-progress">In Progress</SelectItem>
              <SelectItem value="completed">Completed</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Continue Watching */}
      {continueWatching && continueWatching.length > 0 && (
        <div className="mb-6">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)] flex items-center gap-2 mb-4">
            <Clock className="w-5 h-5" />
            Continue Watching
          </h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {continueWatching.map((item: any) => (
              <Link key={item.videoId} to={`/watch/${item.videoSlug}`} className="group block">
                <div className="relative aspect-video overflow-hidden rounded-xl bg-[var(--color-bg-tertiary)]">
                  {item.videoThumbnailUrl ? (
                    <img src={item.videoThumbnailUrl} alt="" className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105" />
                  ) : (
                    <div className="w-full h-full bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-secondary)]/20 flex items-center justify-center">
                      <Play className="w-12 h-12 text-[var(--color-text-tertiary)]" />
                    </div>
                  )}
                  <div className="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <Play className="w-12 h-12 text-white" />
                  </div>
                  {item.progressPercent > 0 && (
                    <div className="absolute bottom-0 left-0 right-0 h-1 bg-[var(--color-primary)]">
                      <div
                        className="h-full bg-[var(--color-primary)] transition-all duration-300"
                        style={{ width: `${item.progressPercent}%` }}
                      />
                    </div>
                  )}
                </div>
                <div className="mt-2 flex gap-2">
                  <Video className="w-5 h-5 text-[var(--color-text-secondary)]" />
                  <span className="text-sm text-[var(--color-text-secondary)]">{item.progressPercent}% watched</span>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {/* Full History */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-semibold text-[var(--color-text-primary)] flex items-center gap-2">
            <Video className="w-5 h-5" />
            History
          </h2>
        </div>

        {isLoading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {[...Array(12)].map((_, i) => <VideoCardSkeleton key={i} />)}
          </div>
        ) : history && history.content && history.content.length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {history.content.map((item: any) => (
              <Link key={item.id} to={`/watch/${item.videoSlug}`} className="group block">
                <div className="relative aspect-video overflow-hidden rounded-xl bg-[var(--color-bg-tertiary)]">
                  {item.videoThumbnailUrl ? (
                    <img src={item.videoThumbnailUrl} alt="" className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105" />
                  ) : (
                    <div className="w-full h-full bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-secondary)]/20 flex items-center justify-center">
                      <Play className="w-12 h-12 text-[var(--color-text-tertiary)]" />
                    </div>
                  )}
                  <div className="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                    <Play className="w-12 h-12 text-white" />
                  </div>
                  {item.progressPercent > 0 && (
                    <div className="absolute bottom-0 left-0 right-0 h-1 bg-[var(--color-primary)]">
                      <div
                        className="h-full bg-[var(--color-primary)] transition-all duration-300"
                        style={{ width: `${item.progressPercent}%` }}
                      />
                    </div>
                  )}
                </div>
                <div className="mt-2 flex gap-2">
                  <Video className="w-5 h-5 text-[var(--color-text-secondary)]" />
                  <span className="text-sm text-[var(--color-text-secondary)]">{item.progressPercent}% watched</span>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <Video className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4 opacity-50" />
            <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No watch history yet</h3>
            <p className="text-[var(--color-text-secondary)] mb-6">Videos you watch will appear here.</p>
            <Link to="/">
              <Button>Start Watching</Button>
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