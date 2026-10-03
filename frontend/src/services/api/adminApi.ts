import { http } from '../http';

export interface User {
  id: string;
  email: string;
  username: string;
  displayName: string;
  avatarUrl?: string;
  roles: string[];
  blocked: boolean;
  lockedUntil?: string;
  emailVerified: boolean;
  createdAt: string;
}

export interface UserListResponse {
  content: User[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export const adminApi = {
  banUser: (userId: string, reason?: string) => http.post(`/admin/users/${userId}/ban`, { reason }),
  unbanUser: (userId: string) => http.post(`/admin/users/${userId}/unban`),
  updateUserRole: (userId: string, role: string) => http.put(`/admin/users/${userId}/role`, { role }),
  getUsers: (params: { search?: string; role?: string; status?: string; page?: number; size?: number }) =>
    http.get<UserListResponse>('/admin/users', { params }),

  getVideos: (params: { search?: string; status?: string; visibility?: string; page?: number; size?: number }) =>
    http.get<any>('/admin/videos', { params }),

  blockVideo: (videoId: string, reason: string) =>
    http.post(`/admin/videos/${videoId}/block`, { reason }),

  unblockVideo: (videoId: string) =>
    http.post(`/admin/videos/${videoId}/unblock`),

  deleteVideo: (videoId: string) =>
    http.delete(`/admin/videos/${videoId}`),

  getReports: (params: { search?: string; status?: string; type?: string; page?: number; size?: number }) =>
    http.get<any>('/admin/reports', { params }),

  resolveReport: (reportId: string, action: string, notes: string) =>
    http.post(`/admin/reports/${reportId}/resolve`, { action, notes }),
};