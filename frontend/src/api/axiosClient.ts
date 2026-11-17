import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiResponse, AuthResponse } from '../types';

let inMemoryToken: string | null = (typeof window !== 'undefined' ? localStorage.getItem('eshopping_token') : null);
let onAuthFailure: (() => void) | null = null;
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

export const setAccessToken = (token: string | null) => {
  inMemoryToken = token;
  try {
    if (token) {
      localStorage.setItem('eshopping_token', token);
    } else {
      localStorage.removeItem('eshopping_token');
    }
  } catch {}
};

export const getAccessToken = () => inMemoryToken;

export const setAuthFailureHandler = (handler: () => void) => {
  onAuthFailure = handler;
};

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: {
    'Content-Type': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
  },
  withCredentials: true, // Sends and receives HttpOnly refresh cookies
});

// Request Interceptor: Attach in-memory access token if present
axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (inMemoryToken && config.headers) {
      config.headers.Authorization = `Bearer ${inMemoryToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Auto-refresh access token on 401 using HttpOnly cookie
axiosClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (!error.response || error.response.status !== 401 || !originalRequest) {
      return Promise.reject(error);
    }

    // Do not attempt refresh on login, refresh, or register endpoints
    const requestUrl = originalRequest.url || '';
    if (
      requestUrl.includes('/api/auth/login') ||
      requestUrl.includes('/api/auth/refresh') ||
      requestUrl.includes('/api/auth/register')
    ) {
      return Promise.reject(error);
    }

    if (originalRequest._retry) {
      if (onAuthFailure) onAuthFailure();
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      })
        .then((token) => {
          if (originalRequest.headers && token) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          return axiosClient(originalRequest);
        })
        .catch((err) => Promise.reject(err));
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const response = await axios.post<ApiResponse<AuthResponse>>(
        '/api/auth/refresh',
        {},
        {
          headers: {
            'X-Requested-With': 'XMLHttpRequest',
          },
          withCredentials: true,
        }
      );

      const newAccessToken = response.data.data.accessToken;
      setAccessToken(newAccessToken);

      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
      }

      processQueue(null, newAccessToken);
      return axiosClient(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      setAccessToken(null);
      if (onAuthFailure) {
        onAuthFailure();
      }
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export { axiosClient };
export default axiosClient;

