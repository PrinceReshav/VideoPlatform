import { http } from '../http';
export const playlistApi = {
  getMyPlaylists: (params?:any) => http.get<any>('/playlists/my', { params }),
  getPlaylist: (id:string) => http.get<any>(`/playlists/${id}`),
  createPlaylist: (data:any) => http.post<any>('/playlists', data),
  updatePlaylist: (id:string,data:any) => http.put<any>(`/playlists/${id}`, data),
  deletePlaylist: (id:string) => http.delete(`/playlists/${id}`),
};
