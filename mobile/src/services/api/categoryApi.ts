import { http } from '../http';
export interface Category { id:string; name:string; slug:string; description?:string; icon?:string; color?:string; }
export const categoryApi = {
  getActiveRootCategories: () => http.get<Category[]>('/categories/active'),
  getCategories: () => http.get<Category[]>('/categories'),
};
