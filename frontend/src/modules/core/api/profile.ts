import { http } from '@/app/http';
import type { ApiResponse, CurrentUser } from '@/modules/core/api/types';

export interface UserProfileUpdatePayload {
  displayName: string;
  departmentName?: string;
}

export interface UserPasswordChangePayload {
  currentPassword: string;
  newPassword: string;
}

export async function updateCurrentUserProfile(payload: UserProfileUpdatePayload) {
  const { data } = await http.patch<ApiResponse<CurrentUser>>('/api/core/users/me/profile', payload);
  return data.data;
}

export async function changeCurrentUserPassword(payload: UserPasswordChangePayload) {
  await http.patch<ApiResponse<null>>('/api/core/users/me/password', payload);
}
