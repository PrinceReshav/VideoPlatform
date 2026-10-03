import { http } from '../http';
export const engagementApi = {
  recordView: (videoId:string, data?:any) => http.post(`/engagement/views/${videoId}`, undefined, { params: data || {} }),
  toggleLike: (videoId:string) => http.post('/engagement/likes', { videoId }),
  isLiked: (videoId:string) => http.get<boolean>(`/engagement/likes/${videoId}`),
  getWatchHistory: (params?:any) => http.get<any>('/engagement/watch-history', { params }),
  getContinueWatching: () => http.get<any[]>('/engagement/watch-history/continue'),
  getLikedVideos: (params?:any) => http.get<any>('/engagement/likes/videos', { params }),
};
