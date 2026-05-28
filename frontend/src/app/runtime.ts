import type { CurrentUser, SessionTokenResponse } from '@/modules/core/api/types';

export const backendEnabled = import.meta.env.VITE_C_TOWER_BACKEND_ENABLED === 'true'
  || import.meta.env.VITE_BIM_SUBMISSION_BACKEND === 'true';

export const frontendMockOnly = !backendEnabled;

export const mockCurrentUser: CurrentUser = {
  userId: 1,
  username: 'platform.admin',
  displayName: 'C塔管理方',
  currentProject: {
    id: 1,
    code: 'CTOWER-BIM-001',
    name: 'C塔数字化交付平台',
    industryType: 'BUILDING_DIGITAL_DELIVERY',
    status: 'ACTIVE',
    projectManagerName: '管理方',
    roleCode: 'PROJECT_ADMIN',
    roleName: '项目管理员'
  },
  projects: [
    {
      id: 1,
      code: 'CTOWER-BIM-001',
      name: 'C塔数字化交付平台',
      industryType: 'BUILDING_DIGITAL_DELIVERY',
      status: 'ACTIVE',
      projectManagerName: '管理方',
      roleCode: 'PROJECT_ADMIN',
      roleName: '项目管理员'
    }
  ],
  permissions: [
    'BIM_SUBMISSION_READ',
    'BIM_SUBMISSION_MANAGE'
  ],
  menus: [
    {
      key: 'bim-submission',
      label: 'BIM报建',
      path: '/bim-submission/overview',
      icon: 'Tickets',
      children: [
        { key: 'bim-submission-overview', label: '报建总览', path: '/bim-submission/overview', icon: 'DataAnalysis' },
        { key: 'bim-submission-code-center', label: '编码标准中心', path: '/bim-submission/code-center', icon: 'Document' },
        { key: 'bim-submission-plugin-contract', label: '插件契约中心', path: '/bim-submission/plugin-contract', icon: 'SetUp' },
        { key: 'bim-submission-data-center', label: '数据中心', path: '/bim-submission/data-center', icon: 'DataBoard' },
        { key: 'bim-submission-quality', label: '质量校验', path: '/bim-submission/quality', icon: 'CircleCheck' },
        { key: 'bim-submission-batches', label: '批次闭环', path: '/bim-submission/batches', icon: 'Connection' },
        { key: 'bim-submission-work-orders', label: '整改工单', path: '/bim-submission/work-orders', icon: 'Warning' },
        { key: 'bim-submission-archives', label: '归档摘要', path: '/bim-submission/archives', icon: 'Files' }
      ]
    }
  ]
};

export const mockSessionToken: SessionTokenResponse = {
  tokenType: 'Mock',
  accessToken: 'ctower-frontend-mock-token',
  accessTokenExpiresAt: '2099-12-31T23:59:59Z',
  refreshToken: 'ctower-frontend-mock-refresh-token',
  refreshTokenExpiresAt: '2099-12-31T23:59:59Z',
  currentProjectId: 1
};
