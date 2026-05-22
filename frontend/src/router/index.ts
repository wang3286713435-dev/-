import { createRouter, createWebHistory } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const legacyProjectRouteMap: Record<string, string> = {
  'data-steward-models': 'project-data-steward-models',
  'data-steward-objects': 'project-data-steward-objects',
  'master-data-sections': 'project-master-data-sections',
  'master-data-node-types': 'project-master-data-node-types',
  'master-data-initialization': 'project-master-data-initialization',
  'master-data-deliverable-standard': 'project-master-data-deliverable-standard',
  'work-document-delivery': 'project-work-document-delivery',
  'work-drawing-delivery': 'project-work-drawing-delivery',
  'work-delivery-package': 'project-work-delivery-package',
  'work-rectifications': 'project-work-rectifications',
  'work-agent-governance': 'project-work-agent-governance',
  'work-dashboard': 'project-work-dashboard'
};

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/modules/auth/pages/LoginPage.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/modules/auth/pages/RegisterPage.vue'),
      meta: { guestOnly: true }
    },
    {
      path: '/',
      component: () => import('@/modules/core/layout/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/data-steward/assets'
        },
        {
          path: 'home',
          name: 'home',
          component: () => import('@/modules/work-center/pages/HomePage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'access-pending',
          name: 'access-pending',
          component: () => import('@/modules/core/pages/AccessPendingPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'admin/employees',
          name: 'admin-employees',
          component: () => import('@/modules/core/pages/AdminEmployeesPage.vue'),
          meta: { requiresAuth: true, adminOnly: true }
        },
        {
          path: 'master-data/sections',
          name: 'master-data-sections',
          component: () => import('@/modules/master-data/pages/SectionNodesPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'master-data/node-types',
          name: 'master-data-node-types',
          component: () => import('@/modules/master-data/pages/NodeTypesPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'master-data/initialization',
          name: 'master-data-initialization',
          component: () => import('@/modules/master-data/pages/ProjectInitializationPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'master-data/deliverable-standard',
          name: 'master-data-deliverable-standard',
          component: () => import('@/modules/master-data/pages/DeliverableStandardPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets',
          name: 'data-steward-assets',
          component: () => import('@/modules/data-steward/pages/AssetOverviewPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/scans',
          name: 'data-steward-scans',
          component: () => import('@/modules/data-steward/pages/AssetScanOperationsPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/quality',
          name: 'data-steward-quality',
          component: () => import('@/modules/data-steward/pages/AssetQualityOverviewPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/nonstandard-directories',
          name: 'data-steward-nonstandard-directories',
          component: () => import('@/modules/data-steward/pages/NonstandardDirectoryGovernancePage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/catalog',
          name: 'data-steward-catalog',
          component: () => import('@/modules/data-steward/pages/AssetCatalogPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/agent-preview',
          name: 'data-steward-agent-preview',
          component: () => import('@/modules/data-steward/pages/AgentPreviewPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'bim-collaboration',
          name: 'bim-collaboration',
          component: () => import('@/modules/visualization/pages/DigitalTwinPortalPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'digital-twin',
          redirect: '/bim-collaboration',
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId',
          name: 'data-steward-asset-detail',
          component: () => import('@/modules/data-steward/pages/AssetProjectDetailPage.vue'),
          meta: { requiresAuth: true, assetProjectContext: true }
        },
        // Project-internal routes: data-steward
        {
          path: 'data-steward/assets/:projectId/data-steward/models',
          name: 'project-data-steward-models',
          component: () => import('@/modules/data-steward/pages/ModelIntegrationsPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/data-steward/objects',
          name: 'project-data-steward-objects',
          component: () => import('@/modules/data-steward/pages/ManagedObjectsPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/data-steward/issues',
          name: 'project-data-steward-issues',
          component: () => import('@/modules/data-steward/pages/DataStewardIssuesPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/data-steward/tasks',
          name: 'project-data-steward-tasks',
          component: () => import('@/modules/data-steward/pages/DataStewardTasksPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/data-steward/exports',
          name: 'project-data-steward-exports',
          component: () => import('@/modules/data-steward/pages/DataStewardExportsPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/data-steward/file-service',
          name: 'project-data-steward-file-service',
          component: () => import('@/modules/data-steward/pages/DataStewardFileServicePage.vue'),
          meta: { requiresAuth: true }
        },
        // Project-internal routes: master-data
        {
          path: 'data-steward/assets/:projectId/master-data/sections',
          name: 'project-master-data-sections',
          component: () => import('@/modules/master-data/pages/SectionNodesPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/master-data/node-types',
          name: 'project-master-data-node-types',
          component: () => import('@/modules/master-data/pages/NodeTypesPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/master-data/initialization',
          name: 'project-master-data-initialization',
          component: () => import('@/modules/master-data/pages/ProjectInitializationPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/master-data/deliverable-standard',
          name: 'project-master-data-deliverable-standard',
          component: () => import('@/modules/master-data/pages/DeliverableStandardPage.vue'),
          meta: { requiresAuth: true }
        },
        // Project-internal routes: work-center
        {
          path: 'data-steward/assets/:projectId/work/document-delivery',
          name: 'project-work-document-delivery',
          component: () => import('@/modules/work-center/pages/DocumentDeliveryPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/work/drawing-delivery',
          name: 'project-work-drawing-delivery',
          component: () => import('@/modules/work-center/pages/DrawingDeliveryPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/work/delivery-package',
          name: 'project-work-delivery-package',
          component: () => import('@/modules/work-center/pages/DeliveryPackageArchivePage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/work/rectifications',
          name: 'project-work-rectifications',
          component: () => import('@/modules/work-center/pages/RectificationsPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/work/agent-governance',
          name: 'project-work-agent-governance',
          component: () => import('@/modules/work-center/pages/AgentDeliveryGovernancePage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/assets/:projectId/work/dashboard',
          name: 'project-work-dashboard',
          component: () => import('@/modules/work-center/pages/DashboardPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/files',
          name: 'data-steward-files',
          component: () => import('@/modules/data-steward/pages/FileResourcesPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/models',
          name: 'data-steward-models',
          component: () => import('@/modules/data-steward/pages/ModelIntegrationsPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'data-steward/objects',
          name: 'data-steward-objects',
          component: () => import('@/modules/data-steward/pages/ManagedObjectsPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'work/document-delivery',
          name: 'work-document-delivery',
          component: () => import('@/modules/work-center/pages/DocumentDeliveryPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'work/drawing-delivery',
          name: 'work-drawing-delivery',
          component: () => import('@/modules/work-center/pages/DrawingDeliveryPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'work/delivery-package',
          name: 'work-delivery-package',
          component: () => import('@/modules/work-center/pages/DeliveryPackageArchivePage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'work/rectifications',
          name: 'work-rectifications',
          component: () => import('@/modules/work-center/pages/RectificationsPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'work/agent-governance',
          name: 'work-agent-governance',
          component: () => import('@/modules/work-center/pages/AgentDeliveryGovernancePage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'work/dashboard',
          name: 'work-dashboard',
          component: () => import('@/modules/work-center/pages/DashboardPage.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'visualization/workbench',
          name: 'visualization-workbench',
          component: () => import('@/modules/visualization/pages/ThreeDWorkbenchPage.vue'),
          meta: { requiresAuth: true }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/data-steward/assets'
    }
  ]
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  authStore.hydrate();

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login' };
  }

  if (authStore.isAuthenticated && !authStore.currentUser) {
    try {
      await authStore.loadCurrentUser();
    } catch {
      authStore.reset();
      return { name: 'login' };
    }
  }

  const noProjectUser = authStore.isAuthenticated && authStore.currentUser?.projects.length === 0;

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: noProjectUser ? 'access-pending' : 'data-steward-assets' };
  }

  if (to.meta.requiresAuth && noProjectUser && to.name !== 'access-pending') {
    return { name: 'access-pending' };
  }

  if (to.name === 'access-pending' && authStore.isAuthenticated && !noProjectUser) {
    return { name: 'data-steward-assets' };
  }

  if (to.meta.adminOnly && !hasEmployeeManagementPermission()) {
    return { name: 'data-steward-assets' };
  }

  const legacyTargetName = legacyProjectRouteMap[String(to.name ?? '')];
  if (legacyTargetName) {
    const projectId = authStore.currentProjectId;
    if (projectId) {
      return { name: legacyTargetName, params: { projectId } };
    }
    return { name: 'data-steward-assets' };
  }

  const routeProjectId = Number(to.params.projectId);
  if (
    to.meta.requiresAuth &&
    !to.meta.assetProjectContext &&
    Number.isFinite(routeProjectId) &&
    routeProjectId > 0 &&
    routeProjectId !== authStore.currentProjectId
  ) {
    try {
      await authStore.changeProject(routeProjectId);
    } catch {
      return { name: 'data-steward-assets' };
    }
  }

  return true;
});

function hasEmployeeManagementPermission() {
  const authStore = useAuthStore();
  const permissions = authStore.currentUser?.permissions ?? [];
  return permissions.includes('CORE_USER_MANAGE') || permissions.includes('CORE_PROJECT_ROLE_MANAGE');
}

export default router;
