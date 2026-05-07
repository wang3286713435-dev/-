import axios from 'axios';

import { readSession } from './session';
import type { ApiResponse } from '@/modules/core/api/types';

export const http = axios.create({
  baseURL: '/',
  timeout: 10000
});

http.interceptors.request.use((config) => {
  const session = readSession();
  if (session?.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const data = error.response?.data as ApiResponse<unknown> | undefined;
    const message = data?.message ?? error.message ?? '请求失败';
    return Promise.reject(new Error(message));
  }
);
