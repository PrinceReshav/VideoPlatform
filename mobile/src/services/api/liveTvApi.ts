import { http } from '../http';

export interface LiveChannel {
  id: string;
  name: string;
  country: string;
  countryCode: string;
  language: string;
  category: string;
  logo: string;
  url: string;
  isNsfw: boolean;
  tvgId: string;
  tvgName: string;
  tvgLogo: string;
  groupTitle: string;
  hlsUrl: string;
  status: 'playable' | 'degraded' | 'no_source' | 'checking' | 'unknown';
  favorite?: boolean;
  viewCount?: number;
  accessType?: 'PUBLIC' | 'PROTECTED';
  lastChecked?: string;
}

export interface LiveTvResponse {
  content: LiveChannel[];
  pageable?: unknown;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface CountryInfo {
  code: string;
  name: string;
  flag?: string;
  channelCount: number;
}

export interface CategoryInfo {
  name: string;
  channelCount: number;
}

export interface EpgProgram {
  channelId: string;
  title: string;
  startTime: string;
  endTime: string;
  description?: string;
  category?: string;
  icon?: string;
}

export interface EpgResponse {
  programs: EpgProgram[];
  channelId: string;
  date: string;
}

export const liveTvApi = {
  getChannels: (params?: {
    country?: string;
    category?: string;
    search?: string;
    page?: number;
    size?: number;
    sort?: string;
  }) =>
    http.get<LiveTvResponse>('/live/channels', { params }),

  getCountries: (params?: { category?: string; search?: string }) =>
    http.get<CountryInfo[]>('/live/countries', { params }),

  getCategories: (params?: { country?: string; search?: string }) =>
    http.get<CategoryInfo[]>('/live/categories', { params }),

  getChannel: (channelId: string) =>
    http.get<LiveChannel>(`/live/channels/${channelId}`),

  getEpg: (channelId: string, date?: string) =>
    http.get<EpgResponse>(`/live/channels/${channelId}/epg`, {
      params: date ? { date } : {}
    }),

  checkChannelStatus: (channelId: string) =>
    http.post<{ status: LiveChannel['status'] }>(`/live/channels/${channelId}/check`),

  toggleFavorite: (channelId: string) =>
    http.post<LiveChannel>(`/live/channels/${channelId}/favorite`),

  recordView: (channelId: string) =>
    http.post<LiveChannel>(`/live/channels/${channelId}/view`),

  getFeatured: () =>
    http.get<LiveChannel[]>('/live/featured'),

};

export const IPTV_ORG_BASE = 'https://raw.githubusercontent.com/iptv-org/iptv/master';
export const IPTV_CHANNELS_URL = `${IPTV_ORG_BASE}/channels.json`;
export const IPTV_CATEGORIES_URL = `${IPTV_ORG_BASE}/categories.json`;
export const IPTV_COUNTRIES_URL = `${IPTV_ORG_BASE}/countries.json`;