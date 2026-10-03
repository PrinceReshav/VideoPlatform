import { http } from '../http';

export interface Channel {
  id: string;
  name: string;
  slug: string;
  description?: string;
  avatarUrl?: string;
  bannerUrl?: string;
  verified: boolean;
  subscriberCount: number;
  videoCount: number;
  totalViews: number;
  customUrl?: string;
}

export interface ChannelResponse {
  content: Channel[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export const channelApi = {
  getMyChannel: () =>
    http.get<Channel>('/channels/me'),

  getChannel: (slug: string) =>
    http.get<Channel>(`/channels/slug/${slug}`),

  getChannelById: (id: string) =>
    http.get<Channel>(`/channels/${id}`),

  updateChannel: (id: string, data: any) =>
    http.put<Channel>(`/channels/${id}`, data),

  createChannel: (data: any) =>
    http.post<Channel>('/channels', data),
};