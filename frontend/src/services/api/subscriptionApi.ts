import { http } from '../http';

export interface SubscriptionResponse {
  id: string;
  channelId: string;
  channelName: string;
  channelAvatar?: string;
  subscribedAt: string;
}

export const subscriptionApi = {
  getMySubscriptions: (params?: { page: number; size: number }) => http.get<{ content: SubscriptionResponse[]; totalElements: number; totalPages: number }>('/subscriptions/my', { params }),
  getFeed: (params?: { page: number; size: number }) =>
    http.get<{ content: SubscriptionResponse[]; totalElements: number; totalPages: number }>('/subscriptions/feed', { params }),

  subscribe: (channelId: string) =>
    http.post('/subscriptions', { channelId }),

  unsubscribe: (channelId: string) =>
    http.delete(`/subscriptions/${channelId}`),

  isSubscribed: (channelId: string) =>
    http.get<boolean>(`/subscriptions/${channelId}`),

  getSubscriptions: (params?: { page: number; size: number }) =>
    http.get<{ content: SubscriptionResponse[]; totalElements: number; totalPages: number }>('/subscriptions', { params }),

  getSubscribers: (channelId: string, params?: { page: number; size: number }) =>
    http.get<{ content: SubscriptionResponse[]; totalElements: number; totalPages: number }>(`/subscriptions/channel/${channelId}/subscribers`, { params }),
};