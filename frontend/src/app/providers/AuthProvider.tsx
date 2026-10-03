import React, { createContext, useContext, useEffect, ReactNode } from 'react';

import { useAuthStore } from '../../store/authStore';
import { authApi } from '../../services/api/authApi';

interface AuthContextType {
  user: any;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string, rememberMe?: boolean) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
  refreshToken: () => Promise<void>;
}

interface RegisterData {
  email: string;
  username: string;
  password: string;
  displayName?: string;
}

function normalizeUser(data: any) {
  return {
    id: data.id ?? data.userId,
    email: data.email ?? '',
    username: data.username ?? '',
    displayName: data.displayName ?? data.username ?? '',
    avatarUrl: data.avatarUrl,
    roles: Array.isArray(data.roles)
      ? data.roles.map((role: any) => typeof role === 'string' ? role : role?.authority?.replace(/^ROLE_/, '') ?? role?.name).filter(Boolean)
      : [],
  };
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const {
    user,
    setAuth,
    clearAuth,
    setLoading,
  } = useAuthStore();

  const handleRefreshToken = async () => {
    const token = useAuthStore.getState().refreshToken;
    if (!token) {
      clearAuth();
      throw new Error('No refresh token');
    }

    try {
      const response = await authApi.refreshToken(token);
      const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response;
      localStorage.setItem('accessToken', newAccessToken);
      localStorage.setItem('refreshToken', newRefreshToken);
      setAuth({ accessToken: newAccessToken, refreshToken: newRefreshToken });
    } catch (error) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      clearAuth();
      throw error;
    }
  };

  // Authentication is restored in the background. The application shell must
  // never be blocked by an unavailable API during startup.
  useEffect(() => {
    let cancelled = false;

    const initAuth = async () => {
      const storedRefreshToken = localStorage.getItem('refreshToken');
      const storedAccessToken = localStorage.getItem('accessToken');

      if (!storedRefreshToken || !storedAccessToken) {
        setLoading(false);
        return;
      }

      try {
        setAuth({
          accessToken: storedAccessToken,
          refreshToken: storedRefreshToken,
        });

        const response = await authApi.refreshToken(storedRefreshToken);
        if (cancelled) return;

        const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response;
        localStorage.setItem('accessToken', newAccessToken);
        localStorage.setItem('refreshToken', newRefreshToken);

        const userResponse = await authApi.getCurrentUser();
        if (cancelled) return;

        setAuth({
          accessToken: newAccessToken,
          refreshToken: newRefreshToken,
          user: normalizeUser(userResponse),
        });
      } catch (error) {
        console.warn('Auth session could not be restored; continuing as signed out.', error);
        if (!cancelled) {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          clearAuth();
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    initAuth();
    return () => { cancelled = true; };
  }, [setAuth, clearAuth, setLoading]);

  const login = async (
    email: string,
    password: string,
    rememberMe = false
  ) => {
    const response = await authApi.login({
      emailOrUsername: email,
      password,
      rememberMe,
    });

    const {
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
    } = response;

    localStorage.setItem('accessToken', newAccessToken);
    localStorage.setItem('refreshToken', newRefreshToken);

    setAuth({
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
      user: normalizeUser(response),
    });
  };

  const register = async (data: RegisterData) => {
    const response = await authApi.register(data);

    const {
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
    } = response;

    localStorage.setItem('accessToken', newAccessToken);
    localStorage.setItem('refreshToken', newRefreshToken);

    setAuth({
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
      user: normalizeUser(response),
    });
  };

  const logout = async () => {
    try {
      const { refreshToken: token } = useAuthStore.getState();

      if (token) {
        await authApi.logout(token);
      }
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      clearAuth();
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading: false,
        login,
        register,
        logout,
        refreshToken: handleRefreshToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }

  return context;
}