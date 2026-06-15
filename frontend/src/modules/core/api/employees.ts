import { http } from '@/app/http';
import type {
  ApiResponse,
  AssignableProject,
  EmployeeCreatePayload,
  EmployeeDetail,
  EmployeeProjectRoleUpdatePayload,
  EmployeeSummary,
  ProjectRoleOption
} from '@/modules/core/api/types';

export interface EmployeeListParams {
  keyword?: string;
  status?: 'ACTIVE' | 'DISABLED' | '';
}

export async function fetchEmployees(params: EmployeeListParams) {
  const { data } = await http.get<ApiResponse<EmployeeSummary[]>>('/api/core/users', { params });
  return data.data;
}

export async function fetchEmployeeDetail(userId: number) {
  const { data } = await http.get<ApiResponse<EmployeeDetail>>(`/api/core/users/${userId}`);
  return data.data;
}

export async function createEmployee(payload: EmployeeCreatePayload) {
  const { data } = await http.post<ApiResponse<EmployeeDetail>>('/api/core/users', payload);
  return data.data;
}

export async function updateEmployeeStatus(userId: number, status: 'ACTIVE' | 'DISABLED') {
  const { data } = await http.patch<ApiResponse<EmployeeDetail>>(`/api/core/users/${userId}/status`, { status });
  return data.data;
}

export async function deleteEmployee(userId: number) {
  await http.delete<ApiResponse<null>>(`/api/core/users/${userId}`);
}

export async function updateEmployeeProjectRoles(userId: number, payload: EmployeeProjectRoleUpdatePayload) {
  const { data } = await http.put<ApiResponse<EmployeeDetail>>(`/api/core/users/${userId}/project-roles`, payload);
  return data.data;
}

export async function fetchAssignableProjects() {
  const { data } = await http.get<ApiResponse<AssignableProject[]>>('/api/core/projects/assignable');
  return data.data;
}

export async function fetchProjectRoleOptions() {
  const { data } = await http.get<ApiResponse<ProjectRoleOption[]>>('/api/core/roles/project-assignable');
  return data.data;
}
