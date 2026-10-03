import { http } from '../http';

export interface GenreResponse {
  id: string;
  name: string;
  slug: string;
  description?: string;
  createdAt: string;
}

export interface CreateGenreRequest {
  name: string;
  description?: string;
}

export interface UpdateGenreRequest {
  name?: string;
  description?: string;
}

export const genreApi = {
  getAllGenres: () => http.get<GenreResponse[]>('/genres'),
  getAll: (params?: { page: number; size: number }) =>
    http.get<{ content: GenreResponse[]; totalElements: number; totalPages: number }>('/genres', { params }),

  getActiveRootGenres: () =>
    http.get<GenreResponse[]>('/genres/active'),

  getById: (id: string) =>
    http.get<GenreResponse>(`/genres/${id}`),

  getBySlug: (slug: string) =>
    http.get<GenreResponse>(`/genres/slug/${slug}`),

  create: (data: CreateGenreRequest) =>
    http.post<GenreResponse>('/genres', data),

  update: (id: string, data: UpdateGenreRequest) =>
    http.put<GenreResponse>(`/genres/${id}`, data),

  delete: (id: string) =>
    http.delete(`/genres/${id}`),
};