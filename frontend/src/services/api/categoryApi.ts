import { http } from '../http';

export interface Category {
  id: string;
  name: string;
  slug: string;
  description?: string;
  icon?: string;
  color?: string;
  parentId?: string;
  sortOrder: number;
  active: boolean;
  isSystem: boolean;
  children?: Category[];
}

export interface CategoryListResponse {
  content: Category[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
  pageSize: number;
}

export const categoryApi = {
  getAllCategories: () =>
    http.get<Category[]>('/categories'),

  getActiveRootCategories: () =>
    http.get<Category[]>('/categories/active'),

  getCategoryById: (id: string) =>
    http.get<Category>(`/categories/${id}`),

  getCategoryBySlug: (slug: string) =>
    http.get<Category>(`/categories/slug/${slug}`),

  createCategory: (data: { name: string; description?: string; icon?: string; color?: string; parentId?: string; sortOrder?: number; system?: boolean }) =>
    http.post<Category>('/categories', data),

  updateCategory: (id: string, data: { name?: string; description?: string; icon?: string; color?: string; parentId?: string; sortOrder?: number; active?: boolean }) =>
    http.put<Category>(`/categories/${id}`, data),

  deleteCategory: (id: string) =>
    http.delete(`/categories/${id}`),
};

export interface Genre {
  id: string;
  name: string;
  slug: string;
  description?: string;
  parentId?: string;
  sortOrder: number;
  active: boolean;
  isSystem: boolean;
  children?: Genre[];
}

export const genreApi = {
  getAllGenres: () =>
    http.get<Genre[]>('/genres'),

  getActiveRootGenres: () =>
    http.get<Genre[]>('/genres/active'),

  getGenreById: (id: string) =>
    http.get<Genre>(`/genres/${id}`),

  getGenreBySlug: (slug: string) =>
    http.get<Genre>(`/genres/slug/${slug}`),

  createGenre: (data: { name: string; description?: string; parentId?: string; sortOrder?: number; system?: boolean }) =>
    http.post<Genre>('/genres', data),

  updateGenre: (id: string, data: { name?: string; description?: string; parentId?: string; sortOrder?: number; active?: boolean }) =>
    http.put<Genre>(`/genres/${id}`, data),

  deleteGenre: (id: string) =>
    http.delete(`/genres/${id}`),
};

export interface Language {
  id: string;
  code: string;
  name: string;
  nativeName?: string;
  active: boolean;
  isSystem: boolean;
}

export const languageApi = {
  getActiveLanguages: () =>
    http.get<Language[]>('/languages'),

  getLanguageById: (id: string) =>
    http.get<Language>(`/languages/${id}`),

  getLanguageByCode: (code: string) =>
    http.get<Language>(`/languages/code/${code}`),
};