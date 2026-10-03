import { http } from '../http';

export interface PlaylistResponse {
  id: string;
  title: string;
  description?: string;
  thumbnailUrl?: string;
  videoCount: number;
  totalDurationSeconds: number;
  visibility: 'PUBLIC' | 'UNLISTED' | 'PRIVATE';
  ownerId: string;
  ownerName: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlaylistRequest {
  name: string;
  description?: string;
  visibility?: 'PUBLIC' | 'UNLISTED' | 'PRIVATE';
}

export interface UpdatePlaylistRequest {
  title?: string;
  description?: string;
  visibility?: 'PUBLIC' | 'UNLISTED' | 'PRIVATE';
}

export interface AddVideoToPlaylistRequest {
  videoId: string;
}

export const playlistApi = {
  getMyPlaylists: (params?: { page: number; size: number }) => http.get<{ content: PlaylistResponse[]; totalElements: number; totalPages: number }>('/playlists/my', { params }),
  createPlaylist: (data: CreatePlaylistRequest) => http.post<PlaylistResponse>('/playlists', data),
  deletePlaylist: (id: string) => http.delete(`/playlists/${id}`),
  getAll: (params?: { page: number; size: number }) =>
    http.get<{ content: PlaylistResponse[]; totalElements: number; totalPages: number }>('/playlists', { params }),

  getById: (id: string) =>
    http.get<PlaylistResponse>(`/playlists/${id}`),

  getByUser: (userId: string, params?: { page: number; size: number }) =>
    http.get<{ content: PlaylistResponse[]; totalElements: number; totalPages: number }>(`/users/${userId}/playlists`, { params }),

  create: (data: CreatePlaylistRequest) =>
    http.post<PlaylistResponse>('/playlists', data),

  update: (id: string, data: UpdatePlaylistRequest) =>
    http.put<PlaylistResponse>(`/playlists/${id}`, data),

  delete: (id: string) =>
    http.delete(`/playlists/${id}`),

  addVideo: (playlistId: string, data: AddVideoToPlaylistRequest) =>
    http.post(`/playlists/${playlistId}/videos`, data),

  removeVideo: (playlistId: string, videoId: string) =>
    http.delete(`/playlists/${playlistId}/videos/${videoId}`),

  reorderVideos: (playlistId: string, videoIds: string[]) =>
    http.put(`/playlists/${playlistId}/reorder`, { videoIds }),
};