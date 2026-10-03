import axios, { AxiosError, AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../store/authStore';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

class HttpService {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
      withCredentials: true,
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor - add auth token
    this.client.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const authStore = useAuthStore.getState();
        const { accessToken } = authStore;
        if (accessToken && config.headers) {
          config.headers.Authorization = `Bearer ${accessToken}`;
        }
        // Add correlation ID for tracing
        config.headers['X-Correlation-ID'] = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
          ? crypto.randomUUID()
          : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor - handle token refresh
    this.client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError) => {
        const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };

        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;

          try {
            const authStore = useAuthStore.getState();
            const { refreshToken } = authStore;
            if (refreshToken) {
              const response = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken });
              const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data;

              authStore.setAuth({ accessToken: newAccessToken, refreshToken: newRefreshToken });
              localStorage.setItem('accessToken', newAccessToken);
              localStorage.setItem('refreshToken', newRefreshToken);

              // Retry original request
              if (originalRequest.headers) {
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
              }
              return this.client.request(originalRequest);
            }
          } catch (refreshError) {
            // Refresh failed - logout
            const authStore = useAuthStore.getState();
            authStore.clearAuth();
            localStorage.removeItem('accessToken');
            localStorage.removeItem('refreshToken');
            window.location.href = '/login';
            return Promise.reject(refreshError);
          }
        }

        return Promise.reject(error);
      }
    );
  }

  get<T>(url: string, config?: AxiosRequestConfig): Promise<any> {
    return this.client.get<T>(url, config).then(r => r.data);
  }
  post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<any> {
    return this.client.post<T>(url, data, config).then(r => r.data);
  }
  put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<any> {
    return this.client.put<T>(url, data, config).then(r => r.data);
  }
  patch<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<any> {
    return this.client.patch<T>(url, data, config).then(r => r.data);
  }
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<any> {
    return this.client.delete<T>(url, config).then(r => r.data);
  }
}

export const http = new HttpService();