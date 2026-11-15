import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';

export const api = axios.create({
  baseURL: '/api',
  withCredentials: false,
});

let accessToken: string | null = localStorage.getItem('apilens_token');

export function setAccessToken(token: string | null) {
  accessToken = token;
  if (token) localStorage.setItem('apilens_token', token);
  else localStorage.removeItem('apilens_token');
}

export function getAccessToken() {
  return accessToken;
}

// Request interceptor: attach Bearer + correlation id on every call
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken) {
    config.headers['Authorization'] = `Bearer ${accessToken}`;
  }
  const reqId = crypto.randomUUID();
  config.headers['X-Request-Id'] = reqId;
  config.headers['X-Correlation-Id'] = reqId;
  return config;
});

// Response interceptor: surface backend error messages
api.interceptors.response.use(
  (r) => r,
  (error: AxiosError<any>) => {
    const data = error.response?.data;
    if (data && typeof data === 'object' && 'error' in data) {
      (error as any).friendlyMessage = String(data.error);
    } else if (data && typeof data === 'object' && 'message' in data) {
      (error as any).friendlyMessage = String(data.message);
    } else if (error.response?.status === 401) {
      (error as any).friendlyMessage = 'Unauthorized - please log in again';
      setAccessToken(null);
    } else {
      (error as any).friendlyMessage = error.message;
    }
    return Promise.reject(error);
  }
);
