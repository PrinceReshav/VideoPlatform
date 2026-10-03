import { http } from '../http';

export interface RegisterRequest {
  email: string;
  username: string;
  password: string;
  displayName?: string;
}

export interface LoginRequest {
  emailOrUsername: string;
  password: string;
  rememberMe?: boolean;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  userId: string;
  username: string;
  email: string;
  roles: string[];
}

export const authApi = {
  register: (data: RegisterRequest) =>
    http.post<TokenResponse>('/auth/register', data),

  login: (data: LoginRequest) =>
    http.post<TokenResponse>('/auth/login', data),

  refreshToken: (refreshToken: string) =>
    http.post<TokenResponse>('/auth/refresh', { refreshToken }),

  logout: (refreshToken: string) =>
    http.post('/auth/logout', { refreshToken }),

  forgotPassword: (email: string) =>
    http.post('/auth/forgot-password', { email }),

  resetPassword: (token: string, newPassword: string) =>
    http.post('/auth/reset-password', { token, newPassword }),

  verifyEmail: (token: string) =>
    http.get('/auth/verify-email', { params: { token } }),

  getCurrentUser: () =>
    http.get('/auth/me'),
};