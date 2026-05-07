import { createRouter, createWebHistory } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

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
      path: '/',
      component: () => import('@/modules/core/layout/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/home'
        },
        {
          path: 'home',
          name: 'home',
          component: () => import('@/modules/work-center/pages/HomePage.vue'),
          meta: { requiresAuth: true }
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
          path: 'master-data/deliverable-standard',
          name: 'master-data-deliverable-standard',
          component: () => import('@/modules/master-data/pages/DeliverableStandardPage.vue'),
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
      redirect: '/home'
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

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: 'home' };
  }

  return true;
});

export default router;
