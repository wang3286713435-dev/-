import { http } from '@/app/http';
import type { ApiResponse, CurrentUser, RegisterResponse, SessionTokenResponse } from '@/modules/core/api/types';

export interface LoginPayload {
  username: string;
  password: string;
}

export interface RegisterPayload {
  phoneNumber: string;
  displayName: string;
  departmentName?: string;
  password: string;
}

export async function login(payload: LoginPayload) {
  const { data } = await http.post<ApiResponse<SessionTokenResponse>>('/api/core/auth/login', payload);
  return data.data;
}

export async function registerEmployee(payload: RegisterPayload) {
  const { data } = await http.post<ApiResponse<RegisterResponse>>('/api/core/auth/register', payload);
  return data.data;
}

export async function logout() {
  await http.post<ApiResponse<null>>('/api/core/auth/logout');
}

export async function fetchCurrentUser() {
  const { data } = await http.get<ApiResponse<CurrentUser>>('/api/core/users/me');
  return data.data;
}

export async function switchProject(projectId: number) {
  const { data } = await http.post<ApiResponse<SessionTokenResponse>>(`/api/core/projects/${projectId}:switch`);
  return data.data;
}
