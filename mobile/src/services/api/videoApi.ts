import { http } from '../http';

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
  availableResolutions?: string[];
  hlsManifestUrl?: string;
}

export interface VideoListResponse {
  content: Video[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
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
}

export interface SearchResponse {
  content: Video[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
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

  search: async (params: SearchRequest): Promise<SearchResponse> => {
    const response: any = await http.post<any>('/search/videos', params);
    const content = (response?.results ?? response?.content ?? []).map((item: any) => ({
      ...item,
      id: item.id ?? item.videoId,
      slug: item.slug ?? item.videoId,
      viewCount: item.viewCount ?? 0,
      likeCount: item.likeCount ?? 0,
      commentCount: item.commentCount ?? 0,
      channelName: item.channelName ?? 'Creator',
      channelSlug: item.channelSlug ?? item.channelId ?? '',
      channel: item.channel ?? { id: item.channelId ?? '', name: item.channelName ?? 'Creator', slug: item.channelSlug ?? item.channelId ?? '' },
    }));
    return { content, totalElements: response?.totalHits ?? content.length, totalPages: response?.hasNext ? 2 : 1, pageNumber: params.page ?? 0, pageSize: params.size ?? 20 };
  },

  getSuggestions: (query: string) =>
    http.get<SuggestionsResponse>('/search/suggest', { params: { q: query } }),

  getHlsManifest: (videoId: string) =>
    http.get<string>(`/videos/${videoId}/hls`),

  getMyVideos: (params: { page?: number; size?: number; filter?: string }) =>
    http.get<VideoListResponse>('/videos/my', { params }),
  createVideo: (data: any) => http.post<Video>('/videos', data),
  updateVideo: (videoId: string, data: any) => http.put<Video>(`/videos/${videoId}`, data),
  deleteVideo: (videoId: string) => http.delete(`/videos/${videoId}`),
  publishVideo: (videoId: string) => http.post(`/videos/${videoId}/publish`, {}),
  unpublishVideo: (videoId: string) => http.post(`/videos/${videoId}/unpublish`),

  getChannelVideos: (channelId: string, params: { page?: number; size?: number }) =>
    http.get<VideoListResponse>(`/videos/channel/${channelId}`, { params }),

  incrementViewCount: (videoId: string) =>
    http.post(`/engagement/views/${videoId}`),
};