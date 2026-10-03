import { Link } from 'react-router-dom';
import { Dialog } from '../components/ui/Dialog.tsx';
import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Video, Search, Shield, MoreHorizontal, Ban, Eye, Trash2, Filter, Flag, Check, X } from 'lucide-react';
import { Card } from '../components/ui/Card.tsx';
import { Button } from '../components/ui/Button.tsx';
import { Input } from '../components/ui/Input.tsx';
import { Select } from '../components/ui/Select.tsx';
import { Badge } from '../components/ui/Badge.tsx';
import { videoApi } from '../services/api/videoApi.ts';
import { adminApi } from '../services/api/adminApi.ts';
import { formatDistanceToNow } from 'date-fns';

export function AdminVideosPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = React.useState('');
  const [statusFilter, setStatusFilter] = React.useState<string>('all');
  const [visibilityFilter, setVisibilityFilter] = React.useState<string>('all');
  const [page, setPage] = React.useState(0);
  const [selectedVideo, setSelectedVideo] = React.useState<any | null>(null);
  const [actionDialog, setActionDialog] = React.useState<'block' | 'unblock' | 'delete' | null>(null);

  const { data: videos, isLoading } = useQuery({
    queryKey: ['adminVideos', searchQuery, statusFilter, visibilityFilter, page],
    queryFn: () => adminApi.getVideos({ search: searchQuery, status: statusFilter, visibility: visibilityFilter, page, size: 20 }),
  });

  const blockMutation = useMutation({
    mutationFn: ({ videoId, reason }: { videoId: string; reason: string }) => adminApi.blockVideo(videoId, reason),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminVideos'] }),
  });

  const unblockMutation = useMutation({
    mutationFn: (videoId: string) => adminApi.unblockVideo(videoId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminVideos'] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (videoId: string) => adminApi.deleteVideo(videoId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['adminVideos'] }),
  });

  const handleAction = (action: 'block' | 'unblock' | 'delete', video: any) => {
    setSelectedVideo(video);
    setActionDialog(action);
  };

  return (
    <div className="max-w-7xl mx-auto p-4 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)] flex items-center gap-3">
            <Video className="w-8 h-8 text-[var(--color-primary)]" />
            Video Management
          </h1>
          <p className="text-[var(--color-text-secondary)]">Manage platform videos and content moderation</p>
        </div>
      </div>

      {/* Filters */}
      <Card className="p-4">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="flex-1 min-w-[250px]">
            <Input
              placeholder="Search videos..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              leftIcon={<Search className="w-4 h-4" />}
            />
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter} className="w-48">
            <Select.Trigger><Select.Value placeholder="All Status" /></Select.Trigger>
            <Select.Content>
              <Select.Item value="all">All Status</Select.Item>
              <Select.Item value="PUBLISHED">Published</Select.Item>
              <Select.Item value="DRAFT">Draft</Select.Item>
              <Select.Item value="PROCESSING">Processing</Select.Item>
              <Select.Item value="PROCESSING_FAILED">Failed</Select.Item>
              <Select.Item value="BLOCKED">Blocked</Select.Item>
            </Select.Content>
          </Select>
          <Select value={visibilityFilter} onValueChange={setVisibilityFilter} className="w-48">
            <Select.Trigger><Select.Value placeholder="All Visibility" /></Select.Trigger>
            <Select.Content>
              <Select.Item value="all">All Visibility</Select.Item>
              <Select.Item value="PUBLIC">Public</Select.Item>
              <Select.Item value="UNLISTED">Unlisted</Select.Item>
              <Select.Item value="PRIVATE">Private</Select.Item>
            </Select.Content>
          </Select>
        </div>
      </Card>

      {/* Videos Table */}
      <Card>
        {isLoading ? (
          <div className="p-8 space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="animate-pulse flex items-center gap-4">
                <div className="w-16 h-9 rounded-lg bg-[var(--color-bg-tertiary)]" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-1/3 bg-[var(--color-bg-tertiary)] rounded" />
                  <div className="h-3 w-1/4 bg-[var(--color-bg-tertiary)] rounded" />
                </div>
                <div className="w-24 h-8 bg-[var(--color-bg-tertiary)] rounded" />
                <div className="w-32 h-8 bg-[var(--color-bg-tertiary)] rounded" />
              </div>
            ))}
          </div>
        ) : videos?.content?.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-[var(--color-border)] text-left text-sm font-medium text-[var(--color-text-secondary)]">
                    <th className="pb-3 pr-4">Video</th>
                    <th className="pb-3 pr-4">Channel</th>
                    <th className="pb-3 pr-4">Status</th>
                    <th className="pb-3 pr-4">Visibility</th>
                    <th className="pb-3 pr-4">Views</th>
                    <th className="pb-3 pr-4">Published</th>
                    <th className="pb-3 pr-4">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {videos.content.map((video: any) => (
                    <tr key={video.id} className="border-b border-[var(--color-border)]/50 hover:bg-[var(--color-bg-hover)]/50">
                      <td className="py-4 pr-4">
                        <div className="flex items-center gap-3">
                          <div className="w-16 h-9 rounded-lg overflow-hidden bg-[var(--color-bg-tertiary)]">
                            {video.thumbnailUrl ? (
                              <img src={video.thumbnailUrl} alt="" className="w-full h-full object-cover" />
                            ) : (
                              <div className="w-full h-full bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-secondary)]/20" />
                            )}
                          </div>
                          <div className="min-w-0">
                            <p className="font-medium text-[var(--color-text-primary)] truncate max-w-xs">{video.title}</p>
                            <p className="text-xs text-[var(--color-text-secondary)]">ID: {video.id.slice(0, 8)}...</p>
                          </div>
                        </div>
                      </td>
                      <td className="py-4 pr-4">
                        <div>
                          <p className="font-medium text-[var(--color-text-primary)]">{video.channelName}</p>
                          <p className="text-xs text-[var(--color-text-secondary)]">@{video.channelSlug}</p>
                        </div>
                      </td>
                      <td className="py-4 pr-4">
                        <Badge variant={
                          video.processingState === 'PROCESSING' ? 'warning' :
                          video.processingState === 'PROCESSING_FAILED' ? 'error' :
                          video.blocked ? 'error' :
                          video.publicationState === 'PUBLISHED' ? 'success' :
                          'default'
                        }>
                          {video.blocked ? 'Blocked' : video.processingState === 'PROCESSING' ? 'Processing' :
                           video.processingState === 'PROCESSING_FAILED' ? 'Failed' :
                           video.publicationState}
                        </Badge>
                      </td>
                      <td className="py-4 pr-4">
                        <Badge variant={
                          video.visibility === 'PUBLIC' ? 'success' :
                          video.visibility === 'UNLISTED' ? 'warning' :
                          'default'
                        }>
                          {video.visibility}
                        </Badge>
                      </td>
                      <td className="py-4 pr-4 text-[var(--color-text-secondary)]">
                        {video.viewCount?.toLocaleString()}
                      </td>
                      <td className="py-4 pr-4 text-[var(--color-text-secondary)] text-sm">
                        {video.publishedAt ? formatDistanceToNow(new Date(video.publishedAt), { addSuffix: true }) : '—'}
                      </td>
                      <td className="py-4 pr-4">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleAction(video.blocked ? 'unblock' : 'block', video)}
                            className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-error)]"
                            title={video.blocked ? 'Unblock' : 'Block'}
                          >
                            {video.blocked ? <Shield className="w-4 h-4" /> : <Ban className="w-4 h-4" />}
                          </button>
                          <button
                            onClick={() => handleAction('delete', video)}
                            className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-error)]"
                            title="Delete"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                          <Link to={`/watch/${video.slug || video.id}`} target="_blank" className="p-2 rounded-lg hover:bg-[var(--color-bg-hover)] transition-colors text-[var(--color-text-secondary)] hover:text-[var(--color-primary)]" title="View">
                            <Eye className="w-4 h-4" />
                          </Link>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {videos.totalPages > 1 && (
              <div className="flex items-center justify-center gap-2 p-4 border-t border-[var(--color-border)]">
                <Button variant="outline" disabled={page === 0} onClick={() => setPage(p => p - 1)}>
                  Previous
                </Button>
                <span className="px-4 text-[var(--color-text-secondary)]">
                  Page {page + 1} of {videos.totalPages}
                </span>
                <Button variant="outline" disabled={page >= videos.totalPages - 1} onClick={() => setPage(p => p + 1)}>
                  Next
                </Button>
              </div>
            )}
          </>
        ) : (
          <div className="p-8 text-center">
            <Video className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4 opacity-50" />
            <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No videos found</h3>
            <p className="text-[var(--color-text-secondary)]">Try adjusting your search or filters</p>
          </div>
        )}
      </Card>

      {/* Action Dialogs */}
      {actionDialog === 'block' && selectedVideo && (
        <BlockDialog video={selectedVideo} onClose={() => { setActionDialog(null); setSelectedVideo(null); }} onConfirm={(reason) => blockMutation.mutate({ videoId: selectedVideo.id, reason })} />
      )}
      {actionDialog === 'unblock' && selectedVideo && (
        <ConfirmDialog
          title="Unblock Video"
          message={`Are you sure you want to unblock "${selectedVideo.title}"?`}
          onClose={() => { setActionDialog(null); setSelectedVideo(null); }}
          onConfirm={() => unblockMutation.mutate(selectedVideo.id)}
        />
      )}
      {actionDialog === 'delete' && selectedVideo && (
        <ConfirmDialog
          title="Delete Video"
          message={`Are you sure you want to permanently delete "${selectedVideo.title}"? This action cannot be undone.`}
          onClose={() => { setActionDialog(null); setSelectedVideo(null); }}
          onConfirm={() => deleteMutation.mutate(selectedVideo.id)}
        />
      )}
    </div>
  );
}

function BlockDialog({ video, onClose, onConfirm }: { video: any; onClose: () => void; onConfirm: (reason: string) => void }) {
  const [reason, setReason] = React.useState('');
  return (
    <Dialog open onOpenChange={onClose}>
      <Dialog.Content>
        <Dialog.Header>
          <Dialog.Title>Block Video</Dialog.Title>
          <Dialog.Description>Enter reason for blocking "{video.title}"</Dialog.Description>
        </Dialog.Header>
        <form onSubmit={(e) => { e.preventDefault(); onConfirm(reason); onClose(); }} className="space-y-4">
          <Input label="Reason" value={reason} onChange={(e) => setReason(e.target.value)} as="textarea" rows={3} required />
          <Dialog.Footer>
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button type="submit" variant="danger">Block Video</Button>
          </Dialog.Footer>
        </form>
      </Dialog.Content>
    </Dialog>
  );
}

function ConfirmDialog({ title, message, onClose, onConfirm }: { title: string; message: string; onClose: () => void; onConfirm: () => void }) {
  return (
    <Dialog open onOpenChange={onClose}>
      <Dialog.Content>
        <Dialog.Header>
          <Dialog.Title>{title}</Dialog.Title>
          <Dialog.Description>{message}</Dialog.Description>
        </Dialog.Header>
        <Dialog.Footer>
          <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
          <Button type="button" variant="danger" onClick={() => { onConfirm(); onClose(); }}>Confirm</Button>
        </Dialog.Footer>
      </Dialog.Content>
    </Dialog>
  );
}