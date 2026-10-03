import { http } from '../http';

export interface LikeResponse {
  liked: boolean;
  likeCount: number;
}

export interface Comment {
  id: string;
  videoId: string;
  userId: string;
  username: string;
  userAvatarUrl?: string;
  parentId?: string;
  content: string;
  hidden: boolean;
  likeCount: number;
  replyCount: number;
  pinned: boolean;
  createdAt: string;
  updatedAt: string;
  replies?: Comment[];
  isLiked?: boolean;
  isOwn?: boolean;
}

export interface CommentThreadResponse {
  content: Comment[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export interface WatchHistoryItem {
  id: string;
  videoId: string;
  videoTitle: string;
  videoThumbnailUrl?: string;
  channelName: string;
  channelSlug: string;
  positionSeconds: number;
  durationSeconds: number;
  progressPercent: number;
  completed: boolean;
  lastWatchedAt: string;
  watchCount: number;
}

export interface WatchHistoryResponse {
  content: WatchHistoryItem[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export const engagementApi = {
  getLikedVideos: (params: { page?: number; size?: number }) => http.get<WatchHistoryResponse>('/engagement/likes/videos', { params }),
  toggleLike: (videoId: string) =>
    http.post<LikeResponse>('/engagement/likes', { videoId }),

  checkLike: (videoId: string) =>
    http.get<boolean>(`/engagement/likes/${videoId}`),

  getLikeCount: (videoId: string) =>
    http.get<number>(`/engagement/likes/${videoId}/count`),

  addComment: (videoId: string, content: string, parentId?: string) =>
    http.post<Comment>('/engagement/comments', { videoId, content, parentId }),

  updateComment: (commentId: string, content: string) =>
    http.put<Comment>(`/engagement/comments/${commentId}`, { content }),

  deleteComment: (commentId: string) =>
    http.delete(`/engagement/comments/${commentId}`),

  getComments: (videoId: string, params: { page?: number; size?: number }) =>
    http.get<CommentThreadResponse>(`/engagement/comments/${videoId}`, { params }),

  getCommentThread: (videoId: string) =>
    http.get<Comment[]>(`/engagement/comments/${videoId}/thread`),

  recordView: (videoId: string, position?: number, duration?: number, sessionId?: string) =>
    http.post(`/engagement/views/${videoId}`, undefined, { params: { position, duration, sessionId } }),

  getWatchHistory: (params: { page?: number; size?: number }) =>
    http.get<WatchHistoryResponse>('/engagement/watch-history', { params }),

  getContinueWatching: () =>
    http.get<WatchHistoryItem[]>('/engagement/watch-history/continue'),
};