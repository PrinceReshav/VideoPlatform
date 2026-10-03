import { http } from '../http';

export interface VideoListResponse {
  content: Video[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export interface Video {
  id: string;
  title: string;
  slug: string;
  description?: string;
  thumbnailUrl?: string;
  durationSeconds?: number;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  publishedAt: string;
  visibility: string;
  processingState: string;
  publicationState: string;
  channel: {
    id: string;
    name: string;
    slug: string;
    avatarUrl?: string;
  };
  channelId: string;
  channelName: string;
  channelSlug: string;
  channelAvatarUrl?: string;
  genreId?: string;
  genreName?: string;
  categoryId?: string;
  categoryName?: string;
  availableResolutions?: string[];
  hlsManifestUrl?: string;
  tags?: string;
  subscriberCount?: number;
  languageId?: string;
  allowComments?: boolean;
  allowEmbedding?: boolean;
  ageRestricted?: boolean;
}

export interface SearchResponse {
  content: Video[];
  totalElements: number;
  nextCursor?: string;
  hasNext: boolean;
}

export interface SearchRequest {
  q?: string;
  genreId?: string;
  categoryId?: string;
  creatorId?: string;
  channelId?: string;
  languageId?: string;
  sort?: string;
  page?: number;
  size?: number;
  cursor?: string;
}

export interface SuggestionsResponse {
  suggestions: string[];
}

export const videoApi = {
  getPublished: (params: { page?: number; size?: number; sort?: string }) =>
    http.get<VideoListResponse>('/videos', { params }),

  getDetail: (videoId: string) =>
    http.get<Video>(`/videos/${videoId}`),

  getBySlug: (slug: string) =>
    http.get<Video>(`/videos/slug/${slug}`),

  search: (params: SearchRequest) =>
    http.post<any>('/search/videos', params).then((response: any) => ({
      content: (response.results || response.content || []).map((item: any) => ({
        ...item,
        id: item.id || item.videoId,
        slug: item.slug || item.videoId || item.id,
        title: item.title || 'Untitled video',
        description: item.description || '',
        thumbnailUrl: item.thumbnailUrl || '',
        viewCount: item.viewCount ?? 0,
        likeCount: item.likeCount ?? 0,
        commentCount: item.commentCount ?? 0,
        publishedAt: item.publishedAt || new Date(0).toISOString(),
        visibility: item.visibility || 'PUBLIC',
        processingState: item.processingState || 'READY',
        publicationState: item.isPublished === false ? 'UNPUBLISHED' : 'PUBLISHED',
        channelId: item.channelId || '',
        channelName: item.channelName || 'Creator',
        channelSlug: item.channelSlug || item.channelId || '',
        channelAvatarUrl: item.channelAvatarUrl || item.avatarUrl,
        channel: item.channel || { id: item.channelId || '', name: item.channelName || 'Creator', slug: item.channelSlug || item.channelId || '', avatarUrl: item.channelAvatarUrl },
      })),
      totalElements: response.totalHits ?? response.totalElements ?? 0,
      nextCursor: response.nextCursor, hasNext: !!response.hasNext,
    } as SearchResponse)),

  getSuggestions: (query: string) =>
    http.get<SuggestionsResponse>('/search/suggest', { params: { q: query } }),

  getHlsManifest: (videoId: string) =>
    http.get<string>(`/videos/${videoId}/hls`),

  createVideo: (data: any) =>
    http.post<Video>('/videos', data),

  updateVideo: (videoId: string, data: any) =>
    http.put<Video>(`/videos/${videoId}`, data),

  deleteVideo: (videoId: string) =>
    http.delete(`/videos/${videoId}`),

  publishVideo: (videoId: string, data?: any) =>
    http.post(`/videos/${videoId}/publish`, data || {}),

  unpublishVideo: (videoId: string) =>
    http.post(`/videos/${videoId}/unpublish`),

  getMyVideos: (params: { page?: number; size?: number; filter?: string }) =>
    http.get<VideoListResponse>('/videos/my', { params }),

  getChannel: (slug: string) => http.get<any>(`/channels/slug/${slug}`),

  getChannelVideos: (channelId: string, params: { page?: number; size?: number }) =>
    http.get<VideoListResponse>(`/videos/channel/${channelId}`, { params }),

  incrementViewCount: (videoId: string) =>
    http.post(`/engagement/views/${videoId}`),
};