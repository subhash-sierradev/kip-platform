import axios, { AxiosResponse, InternalAxiosRequestConfig } from 'axios';

import { env } from '@/config/env';
import { getToken, refreshToken } from '@/config/keycloak';

export const api = axios.create({
  baseURL: env.apiBaseUrl,
  timeout: 10000,
});

api.interceptors.request.use((cfg: InternalAxiosRequestConfig) => {
  const token = getToken();
  if (token) {
    cfg.headers = cfg.headers || {};
    cfg.headers.Authorization = `Bearer ${token}`;
  }
  return cfg;
});

api.interceptors.response.use(
  (resp: AxiosResponse) => resp,
  async (err: any) => {
    // Attempt silent refresh on 401 once
    if (err.response?.status === 401) {
      await refreshToken();
    }
    return Promise.reject(err);
  }
);
