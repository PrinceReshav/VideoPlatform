import { http } from '../http';
export interface Genre { id:string; name:string; slug:string; }
export const genreApi = {
  getActiveRootGenres: () => http.get<Genre[]>('/genres/active'),
  getGenres: () => http.get<Genre[]>('/genres'),
};
