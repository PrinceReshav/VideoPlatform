import { http } from '../http';
export interface UploadSession { id:string; videoId?:string; status:string; uploadedChunks:number; totalChunks:number; }
export const uploadApi = {
  createSession: (data:any) => http.post<UploadSession>('/uploads/sessions', data),
  getSession: (id:string) => http.get<UploadSession>(`/uploads/sessions/${id}`),
  uploadChunk: (id:string, partNumber:number, data:any, config?:any) => http.put<UploadSession>(`/uploads/sessions/${id}/chunks/${partNumber}`, data, config),
  completeSession: (id:string, parts:any[] = []) => http.post<UploadSession>(`/uploads/sessions/${id}/complete`, { parts }),
  cancelSession: (id:string) => http.delete(`/uploads/sessions/${id}`),
};
