import { http } from '../http';
export interface Language { id:string; code:string; name:string; nativeName?:string; }
export const languageApi = {
  getActiveLanguages: () => http.get<Language[]>('/languages'),
  getLanguages: () => http.get<Language[]>('/languages'),
};
