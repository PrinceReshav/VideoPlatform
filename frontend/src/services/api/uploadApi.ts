import { http } from '../http';

export interface UploadSession {
  id: string;
  fileName: string;
  contentType: string;
  totalSize: number;
  chunkSize: number;
  totalChunks: number;
  expiresAt: string;
  status: 'PENDING' | 'UPLOADING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';
  uploadedParts: number[];
  videoId?: string;
}

export interface CreateSessionRequest {
  fileName: string;
  contentType: string;
  totalSize: number;
}

export interface UploadChunkResponse {
  partNumber: number;
  etag: string;
}

export interface CompleteSessionRequest {
  parts: { partNumber: number; etag: string }[];
}

export const uploadApi = {
  createSession: (data: CreateSessionRequest) =>
    http.post<UploadSession>('/uploads/sessions', data),

  uploadChunk: (sessionId: string, partNumber: number, chunk: Blob) => {
    const formData = new FormData();
    formData.append('file', chunk);
    return http.put<UploadChunkResponse>(`/uploads/sessions/${sessionId}/chunks/${partNumber}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  completeSession: (sessionId: string, data: CompleteSessionRequest) =>
    http.post<UploadSession>(`/uploads/sessions/${sessionId}/complete`, data),

  getSession: (sessionId: string) =>
    http.get<UploadSession>(`/uploads/sessions/${sessionId}`),

  abortSession: (sessionId: string) =>
    http.delete(`/uploads/sessions/${sessionId}`),
};