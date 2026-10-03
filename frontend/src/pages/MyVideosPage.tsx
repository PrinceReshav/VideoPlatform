import { formatDistanceToNow } from 'date-fns';
import { Upload } from 'lucide-react';
import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Video, Trash2, Edit, Eye, MoreHorizontal, Check, X, Filter } from 'lucide-react';
import { VideoCard } from '../components/video/VideoCard.tsx';
import { videoApi } from '../services/api/videoApi.ts';
import { Button } from '../components/ui/Button.tsx';
import { Select } from '../components/ui/Select.tsx';
import { Dialog } from '../components/ui/Dialog.tsx';

export function MyVideosPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = React.useState<'all' | 'published' | 'draft' | 'processing' | 'failed'>('all');
  const [page, setPage] = React.useState(0);
  const [showDeleteDialog, setShowDeleteDialog] = React.useState<string | null>(null);
  const [showStatusDialog, setShowStatusDialog] = React.useState<{ videoId: string; action: 'publish' | 'unpublish' } | null>(null);

  const { data: videos, isLoading } = useQuery({
    queryKey: ['myVideos', filter, page],
    queryFn: () => videoApi.getMyVideos({ page, size: 20, filter }),
  });

  const deleteMutation = useMutation({
    mutationFn: (videoId: string) => videoApi.deleteVideo(videoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myVideos'] });
      setShowDeleteDialog(null);
    },
  });

  const publishMutation = useMutation({
    mutationFn: ({ videoId, action }: { videoId: string; action: 'publish' | 'unpublish' }) =>
      action === 'publish' ? videoApi.publishVideo(videoId) : videoApi.unpublishVideo(videoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myVideos'] });
      setShowStatusDialog(null);
    },
  });

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)]">My Videos</h1>
          <p className="text-[var(--color-text-secondary)]">Manage your uploaded videos</p>
        </div>
        <Link to="/creator/upload">
          <Button>
            <Upload className="w-4 h-4" />
            Upload Video
          </Button>
        </Link>
      </div>

      <div className="flex flex-wrap items-center gap-3 mb-6">
        <Select value={filter} onValueChange={setFilter} className="w-48">
          <Select.Trigger><Select.Value placeholder="All videos" /></Select.Trigger>
          <Select.Content>
            <Select.Item value="all">All</Select.Item>
            <Select.Item value="published">Published</Select.Item>
            <Select.Item value="draft">Draft</Select.Item>
            <Select.Item value="processing">Processing</Select.Item>
            <Select.Item value="failed">Failed</Select.Item>
          </Select.Content>
        </Select>
      </div>

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {[...Array(8)].map((_, i) => <VideoCardSkeleton key={i} />)}
        </div>
      ) : videos?.content?.length > 0 ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {videos.content.map((video: any) => (
              <MyVideoCard
                key={video.id}
                video={video}
                onDelete={() => deleteMutation.mutate(video.id)}
                onPublish={() => publishMutation.mutate({ videoId: video.id, action: 'publish' })}
                onUnpublish={() => publishMutation.mutate({ videoId: video.id, action: 'unpublish' })}
              />
            ))}
          </div>

          {videos.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-8">
              <Button variant="outline" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                Previous
              </Button>
              <span className="px-4 text-[var(--color-text-secondary)]">Page {page + 1} of {videos.totalPages}</span>
              <Button variant="outline" disabled={page >= videos.totalPages - 1} onClick={() => setPage(p => p + 1)}>
                Next
              </Button>
            </div>
          )}
        </>
      ) : (
        <div className="text-center py-16">
          <Video className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4 opacity-50" />
          <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No videos yet</h3>
          <p className="text-[var(--color-text-secondary)] mb-6">Upload your first video to get started</p>
          <Link to="/creator/upload">
            <Button>Upload Video</Button>
          </Link>
        </div>
      )}
    </div>
  );
}

function MyVideoCard({ video, onDelete, onPublish, onUnpublish }: { video: any; onDelete: () => void; onPublish: () => void; onUnpublish: () => void }) {
  const isPublished = video.publicationState === 'PUBLISHED';
  const isProcessing = video.processingState === 'PROCESSING' || video.processingState === 'UPLOADING' || video.processingState === 'UPLOADED';
  const isFailed = video.processingState === 'PROCESSING_FAILED';

  const getStatusBadge = () => {
    if (isPublished) return <span className="px-2 py-0.5 bg-[var(--color-success)]/10 text-[var(--color-success)] text-xs font-medium rounded-full">Published</span>;
    if (isProcessing) return <span className="px-2 py-0.5 bg-[var(--color-warning)]/10 text-[var(--color-warning)] text-xs font-medium rounded-full animate-pulse">Processing</span>;
    if (isFailed) return <span className="px-2 py-0.5 bg-[var(--color-error)]/10 text-[var(--color-error)] text-xs font-medium rounded-full">Failed</span>;
    return <span className="px-2 py-0.5 bg-[var(--color-border)] text-[var(--color-text-secondary)] text-xs font-medium rounded-full">Draft</span>;
  };

  return (
    <Link to={`/creator/videos/${video.id}/edit`} className="block group">
      <div className="relative aspect-video w-full overflow-hidden rounded-xl bg-[var(--color-bg-tertiary)]">
        {video.thumbnailUrl ? (
          <img src={video.thumbnailUrl} alt="" className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105" />
        ) : (
          <div className="w-full h-full bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-secondary)]/20 flex items-center justify-center">
            <Video className="w-12 h-12 text-[var(--color-text-tertiary)]" />
          </div>
        )}
        <div className="absolute top-2 right-2 flex gap-1">
          {getStatusBadge()}
        </div>
        <div className="absolute bottom-2 right-2">
          {video.durationSeconds && (
            <span className="bg-black/90 text-white text-xs font-medium px-1.5 py-0.5 rounded">
              {Math.floor(video.durationSeconds / 60)}:{String(video.durationSeconds % 60).padStart(2, '0')}
            </span>
          )}
        </div>
      </div>
      <div className="mt-3 flex gap-3">
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-[var(--color-text-primary)] line-clamp-2 group-hover:text-[var(--color-primary)] transition-colors">
            {video.title}
          </h3>
          <div className="mt-1 flex flex-wrap items-center gap-2 text-[var(--color-text-tertiary)] text-sm">
            <span>{video.viewCount?.toLocaleString()} views</span>
            <span>•</span>
            <span>{video.likeCount?.toLocaleString()} likes</span>
            <span>•</span>
            <span>{formatDistanceToNow(new Date(video.createdAt), { addSuffix: true })}</span>
          </div>
        </div>
        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <Link to={`/watch/${video.slug || video.id}`} className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors" title="View">
            <Eye className="w-4 h-4" />
          </Link>
          <Link to={`/creator/videos/${video.id}/edit`} className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors" title="Edit">
            <Edit className="w-4 h-4" />
          </Link>
          {video.publicationState === 'PUBLISHED' ? (
            <button onClick={onUnpublish} className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-error)]" title="Unpublish">
              <X className="w-4 h-4" />
            </button>
          ) : video.publicationState === 'DRAFT' && video.processingState === 'READY' ? (
            <button onClick={onPublish} className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-success)]" title="Publish">
              <Check className="w-4 h-4" />
            </button>
          ) : null}
          <button onClick={onDelete} className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-error)]" title="Delete">
            <Trash2 className="w-4 h-4" />
          </button>
        </div>
      </div>
    </Link>
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