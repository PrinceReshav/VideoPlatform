import { Select } from '../components/ui/Select.tsx';
import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Plus, List, MoreHorizontal, Trash2, Edit, Play } from 'lucide-react';
import { VideoCard } from '../components/video/VideoCard.tsx';
import { playlistApi } from '../services/api/playlistApi.ts';
import { Button } from '../components/ui/Button.tsx';
import { Card } from '../components/ui/Card.tsx';
import { Input } from '../components/ui/Input.tsx';
import { Dialog } from '../components/ui/Dialog.tsx';

export function PlaylistsPage() {
  const queryClient = useQueryClient();
  const [showCreateDialog, setShowCreateDialog] = React.useState(false);
  const [newPlaylistName, setNewPlaylistName] = React.useState('');
  const [newPlaylistDescription, setNewPlaylistDescription] = React.useState('');
  const [newPlaylistVisibility, setNewPlaylistVisibility] = React.useState<'PUBLIC' | 'UNLISTED' | 'PRIVATE'>('PRIVATE');
  const [page, setPage] = React.useState(0);

  const { data: playlists, isLoading } = useQuery({
    queryKey: ['myPlaylists', page],
    queryFn: () => playlistApi.getMyPlaylists({ page, size: 20 }),
  });

  const createPlaylistMutation = useMutation({
    mutationFn: (data: { name: string; description?: string; visibility: 'PUBLIC' | 'UNLISTED' | 'PRIVATE' }) =>
      playlistApi.createPlaylist(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myPlaylists'] });
      setShowCreateDialog(false);
      setNewPlaylistName('');
      setNewPlaylistDescription('');
    },
  });

  const deletePlaylistMutation = useMutation({
    mutationFn: (playlistId: string) => playlistApi.deletePlaylist(playlistId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myPlaylists'] });
    },
  });

  const handleCreatePlaylist = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPlaylistName.trim()) return;
    createPlaylistMutation.mutate({
      name: newPlaylistName.trim(),
      description: newPlaylistDescription.trim(),
      visibility: newPlaylistVisibility,
    });
  };

  return (
    <div className="max-w-4xl mx-auto p-4 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-[var(--color-text-primary)] flex items-center gap-3">
            <List className="w-8 h-8 text-[var(--color-primary)]" />
            Playlists
          </h1>
          <p className="text-[var(--color-text-secondary)]">Your video collections</p>
        </div>
        <Button onClick={() => setShowCreateDialog(true)}>
          <Plus className="w-4 h-4" />
          New Playlist
        </Button>
      </div>

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {[...Array(8)].map((_, i) => <PlaylistCardSkeleton key={i} />)}
        </div>
      ) : playlists?.content?.length > 0 ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {playlists.content.map((playlist: any) => (
              <PlaylistCard
                key={playlist.id}
                playlist={playlist}
                onDelete={() => deletePlaylistMutation.mutate(playlist.id)}
              />
            ))}
          </div>

          {/* Pagination */}
          {playlists.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-8">
              <Button
                variant="outline"
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
              >
                Previous
              </Button>
              <span className="px-4 text-[var(--color-text-secondary)]">
                Page {page + 1} of {playlists.totalPages}
              </span>
              <Button
                variant="outline"
                disabled={page >= playlists.totalPages - 1}
                onClick={() => setPage(p => p + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </>
      ) : (
        <div className="text-center py-16">
          <List className="w-16 h-16 mx-auto text-[var(--color-text-tertiary)] mb-4 opacity-50" />
          <h3 className="text-lg font-medium text-[var(--color-text-primary)] mb-2">No playlists yet</h3>
          <p className="text-[var(--color-text-secondary)] mb-6">Create your first playlist to organize videos.</p>
          <Button onClick={() => setShowCreateDialog(true)}>
            <Plus className="w-4 h-4" />
            Create Playlist
          </Button>
        </div>
      )}

      {/* Create Playlist Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <Dialog.Trigger asChild>
          <Button onClick={() => setShowCreateDialog(true)}>
            <Plus className="w-4 h-4" />
            New Playlist
          </Button>
        </Dialog.Trigger>
        <Dialog.Content>
          <Dialog.Header>
            <Dialog.Title>Create New Playlist</Dialog.Title>
            <Dialog.Description>Give your playlist a name and description</Dialog.Description>
          </Dialog.Header>
          <form onSubmit={handleCreatePlaylist} className="space-y-4">
            <Input
              label="Playlist Name"
              value={newPlaylistName}
              onChange={(e) => setNewPlaylistName(e.target.value)}
              placeholder="My Awesome Playlist"
              required
              maxLength={200}
            />
            <Input
              label="Description (optional)"
              value={newPlaylistDescription}
              onChange={(e) => setNewPlaylistDescription(e.target.value)}
              placeholder="What's this playlist about?"
              as="textarea"
              rows={3}
            />
            <div>
              <label className="block text-sm font-medium text-[var(--color-text-primary)] mb-2">
                Visibility
              </label>
              <Select value={newPlaylistVisibility} onValueChange={setNewPlaylistVisibility}>
                <Select.Trigger>
                  <Select.Value />
                </Select.Trigger>
                <Select.Content>
                  <Select.Item value="PRIVATE">Private - Only you</Select.Item>
                  <Select.Item value="UNLISTED">Unlisted - Anyone with link</Select.Item>
                  <Select.Item value="PUBLIC">Public - Everyone</Select.Item>
                </Select.Content>
              </Select>
            </div>
            <Dialog.Footer>
              <Button type="button" variant="outline" onClick={() => setShowCreateDialog(false)}>
                Cancel
              </Button>
              <Button type="submit" loading={createPlaylistMutation.isPending}>
                Create Playlist
              </Button>
            </Dialog.Footer>
          </form>
        </Dialog.Content>
      </Dialog>
    </div>
  );
}

function PlaylistCard({ playlist, onDelete }: { playlist: any; onDelete: () => void }) {
  return (
    <Link to={`/playlists/${playlist.id}`} className="block group">
      <Card className="h-full flex flex-col hover:border-[var(--color-primary)] transition-colors">
        <div className="aspect-video w-full overflow-hidden rounded-t-xl bg-[var(--color-bg-tertiary)] relative">
          {playlist.thumbnailUrl ? (
            <img src={playlist.thumbnailUrl} alt="" className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105" />
          ) : (
            <div className="w-full h-full bg-gradient-to-br from-[var(--color-primary)]/20 to-[var(--color-secondary)]/20 flex items-center justify-center">
              <Play className="w-12 h-12 text-[var(--color-text-tertiary)]" />
            </div>
          )}
          <div className="absolute bottom-2 right-2 flex gap-1">
            <Button variant="ghost" size="icon" className="bg-white/90">
              <Play className="w-4 h-4" />
            </Button>
            <Button variant="ghost" size="icon" className="bg-white/90" onClick={(e) => { e.preventDefault(); onDelete(); }}>
              <Trash2 className="w-4 h-4 text-[var(--color-error)]" />
            </Button>
          </div>
        </div>
        <div className="p-4 flex-1 flex flex-col">
          <h3 className="font-semibold text-[var(--color-text-primary)] line-clamp-1 group-hover:text-[var(--color-primary)] transition-colors">
            {playlist.name}
          </h3>
          <p className="text-sm text-[var(--color-text-secondary)] line-clamp-2 mt-1 flex-1">
            {playlist.description || 'No description'}
          </p>
          <div className="mt-auto flex items-center gap-2 text-xs text-[var(--color-text-tertiary)] pt-2 border-t border-[var(--color-border)]">
            <span>{playlist.videoCount} videos</span>
            <span>•</span>
            <span className="capitalize">{playlist.visibility.toLowerCase()}</span>
          </div>
        </div>
      </Card>
    </Link>
  );
}

function PlaylistCardSkeleton() {
  return (
    <div className="group block">
      <Card className="h-full flex flex-col animate-pulse">
        <div className="aspect-video w-full overflow-hidden rounded-t-xl bg-[var(--color-bg-tertiary)]" />
        <div className="p-4 flex-1 flex flex-col">
          <div className="h-5 w-3/4 bg-[var(--color-bg-tertiary)] rounded mb-2" />
          <div className="h-4 w-1/2 bg-[var(--color-bg-tertiary)] rounded mb-1" />
          <div className="h-3 w-1/3 bg-[var(--color-bg-tertiary)] rounded mt-auto" />
        </div>
      </Card>
    </div>
  );
}