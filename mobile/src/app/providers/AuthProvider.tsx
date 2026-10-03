import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { View, ActivityIndicator } from 'react-native';
import * as SecureStore from 'expo-secure-store';
import { useAuthStore } from '../../store/authStore';
import { authApi } from '../../services/api/authApi';

interface AuthContextType {
  user: any;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
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

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const ACCESS_TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';

const normalizeUser = (data: any) => ({
  id: data.id ?? data.userId,
  email: data.email ?? '',
  username: data.username ?? '',
  displayName: data.displayName ?? data.username ?? '',
  avatarUrl: data.avatarUrl,
  roles: Array.isArray(data.roles) ? data.roles.map((r: any) => typeof r === 'string' ? r : r?.authority?.replace(/^ROLE_/, '') ?? r?.name).filter(Boolean) : [],
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const { user, setAuth, setLoading } = useAuthStore();
  const [isInitialized, setIsInitialized] = useState(false);

  useEffect(() => {
    const initAuth = async () => {
      try {
        const storedAccessToken = await SecureStore.getItemAsync(ACCESS_TOKEN_KEY);
        const storedRefreshToken = await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);

        if (storedAccessToken && storedRefreshToken) {
          setAuth({ accessToken: storedAccessToken, refreshToken: storedRefreshToken });
          await refreshToken();
        }
      } catch (error) {
        console.error('Auth initialization failed:', error);
        await clearAuth();
      } finally {
        setIsInitialized(true);
        setLoading(false);
      }
    };

    initAuth();
  }, [setAuth, setLoading]);

  const refreshToken = async () => {
    try {
      const { refreshToken: token } = useAuthStore.getState();
      if (!token) throw new Error('No refresh token');

      const response = await authApi.refreshToken(token);
      const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response;

      await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, newAccessToken);
      await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, newRefreshToken);
      setAuth({ accessToken: newAccessToken, refreshToken: newRefreshToken, user: normalizeUser(response) });
    } catch (error) {
      await clearAuth();
      throw error;
    }
  };

  const login = async (email: string, password: string) => {
    const response = await authApi.login({ emailOrUsername: email, password });
    const { accessToken, refreshToken } = response;

    await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, accessToken);
    await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken);
    setAuth({ accessToken, refreshToken, user: normalizeUser(response) });
  };

  const register = async (data: RegisterData) => {
    const response = await authApi.register(data);
    const { accessToken, refreshToken } = response;

    await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, accessToken);
    await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken);
    setAuth({ accessToken, refreshToken, user: normalizeUser(response) });
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
      await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY);
      await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
      clearAuth();
    }
  };

  const clearAuth = async () => {
    await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY);
    await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
    setAuth({ accessToken: null, refreshToken: null, user: null });
  };

  if (!isInitialized) {
    return (
      <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
        <ActivityIndicator size="large" color="#6366f1" />
      </View>
    );
  }

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading: false, login, register, logout, refreshToken }}>
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