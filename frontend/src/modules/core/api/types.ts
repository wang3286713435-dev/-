export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId: string;
  timestamp: string;
}

export interface ProjectSummary {
  id: number;
  code: string;
  name: string;
  industryType: string;
  status: string;
  projectManagerName: string | null;
  roleCode: string;
  roleName: string;
}

export interface MenuItem {
  key: string;
  label: string;
  path: string;
  icon: string;
  children?: MenuItem[];
}

export interface CurrentUser {
  userId: number;
  username: string;
  displayName: string;
  currentProject: ProjectSummary | null;
  projects: ProjectSummary[];
  permissions: string[];
  menus: MenuItem[];
}

export interface SessionTokenResponse {
  tokenType: string;
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  currentProjectId: number | null;
}

export interface HomeMetric {
  label: string;
  value: number;
  unit: string;
}

export interface HomeOverview {
  projectId: number;
  projectCode: string;
  projectName: string;
  metrics: HomeMetric[];
  notices: string[];
}

export interface RegisterResponse {
  userId: number;
  username: string;
  phoneNumber: string;
  displayName: string;
  departmentName: string | null;
  status: 'ACTIVE' | 'DISABLED';
  projectAuthorized: boolean;
}

export interface EmployeeSummary {
  userId: number;
  username: string;
  phoneNumber: string | null;
  displayName: string;
  departmentName: string | null;
  status: 'ACTIVE' | 'DISABLED';
  projectCount: number;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeProjectRoleAssignment {
  projectId: number;
  projectCode: string;
  projectName: string;
  roleCode: 'PROJECT_VIEWER' | 'DELIVERY_ENGINEER' | 'PROJECT_ADMIN';
  roleName: string;
}

export interface EmployeeDetail {
  userId: number;
  username: string;
  phoneNumber: string | null;
  displayName: string;
  departmentName: string | null;
  status: 'ACTIVE' | 'DISABLED';
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
  projectRoles: EmployeeProjectRoleAssignment[];
}

export interface EmployeeProjectRoleUpdatePayload {
  assignments: Array<{
    projectId: number;
    roleCode: 'PROJECT_VIEWER' | 'DELIVERY_ENGINEER' | 'PROJECT_ADMIN';
  }>;
}

export interface AssignableProject {
  id: number;
  code: string;
  name: string;
  industryType: string;
  status: string;
}

export interface ProjectRoleOption {
  code: 'PROJECT_VIEWER' | 'DELIVERY_ENGINEER' | 'PROJECT_ADMIN';
  name: string;
  description: string;
}
