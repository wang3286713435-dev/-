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
  currentProject: ProjectSummary;
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
