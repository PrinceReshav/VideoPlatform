import { http } from '../http';

export interface LanguageResponse {
  id: string;
  name: string;
  code: string;
  nativeName: string;
  createdAt: string;
}

export interface CreateLanguageRequest {
  name: string;
  code: string;
  nativeName: string;
}

export interface UpdateLanguageRequest {
  name?: string;
  code?: string;
  nativeName?: string;
}

export const languageApi = {
  getAll: (params?: { page: number; size: number }) =>
    http.get<{ content: LanguageResponse[]; totalElements: number; totalPages: number }>('/languages', { params }),

  getActiveLanguages: () =>
    http.get<LanguageResponse[]>('/languages'),

  getById: (id: string) =>
    http.get<LanguageResponse>(`/languages/${id}`),

  getByCode: (code: string) =>
    http.get<LanguageResponse>(`/languages/code/${code}`),

  create: (data: CreateLanguageRequest) =>
    http.post<LanguageResponse>('/languages', data),

  update: (id: string, data: UpdateLanguageRequest) =>
    http.put<LanguageResponse>(`/languages/${id}`, data),

  delete: (id: string) =>
    http.delete(`/languages/${id}`),
};