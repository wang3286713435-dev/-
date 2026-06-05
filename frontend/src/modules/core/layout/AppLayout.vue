<template>
  <div class="app-layout">
    <aside class="app-layout__sidebar">
      <div class="app-layout__brand">
        <span class="app-layout__brand-mark" aria-hidden="true"></span>
        <div>
          <strong>卓羽智能数据中台</strong>
          <span>ZHUOYU · DATA HUB</span>
        </div>
      </div>
      <SidebarMenu :menus="menus" />
      <div class="app-layout__sidebar-foot">
        <strong>BUILD · UX4-A</strong>
        <span>项目壳层 / SaaS shell</span>
      </div>
    </aside>

    <div class="app-layout__main">
      <header class="app-layout__header">
        <div class="app-layout__header-left">
          <button
            v-if="showBreadcrumbBack"
            class="app-layout__breadcrumb-back"
            type="button"
            :aria-label="`返回${breadcrumbRootLabel}`"
            @click="goBreadcrumbRoot"
          >
            <el-icon><Back /></el-icon>
          </button>
          <nav class="app-layout__breadcrumbs" aria-label="当前位置">
            <strong v-if="isBreadcrumbRootOnly">{{ breadcrumbRootLabel }}</strong>
            <button v-else type="button" @click="goBreadcrumbRoot">{{ breadcrumbRootLabel }}</button>
            <template v-if="shellProject">
              <span>/</span>
              <button type="button" @click="goProjectDashboard(shellProject.id)">{{ shellProject.name }}</button>
            </template>
            <template v-if="!isBreadcrumbRootOnly">
              <span>/</span>
              <strong>{{ shellCurrentLabel }}</strong>
            </template>
          </nav>
        </div>
        <div class="app-layout__toolbar">
          <el-select
            v-if="showHeaderProjectSelect"
            :model-value="shellProjectId"
            class="app-layout__project-select"
            filterable
            size="small"
            placeholder="选择项目"
            @change="handleProjectSelect"
          >
            <el-option
              v-for="item in authorizedProjects"
              :key="item.id"
              :label="`${item.code} · ${item.name}`"
              :value="item.id"
            />
          </el-select>
          <button class="app-layout__search-entry" type="button" @click="showSearchPlaceholder">
            <el-icon><Search /></el-icon>
            <span>搜索项目、文件、交付事项</span>
            <kbd>⌘K</kbd>
          </button>
          <el-tooltip content="通知中心占位">
            <el-button circle text :icon="Bell" aria-label="通知中心" />
          </el-tooltip>
          <el-tooltip content="帮助中心占位">
            <el-button circle text :icon="QuestionFilled" aria-label="帮助中心" @click="router.push({ name: 'home' })" />
          </el-tooltip>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <button class="app-layout__user-button" type="button">
              <span>{{ userInitial }}</span>
              <div>
                <strong>{{ authStore.currentUser?.displayName || '当前用户' }}</strong>
                <small>{{ authStore.currentUser?.username }}</small>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="app-layout__content">
        <ProjectWorkspaceNav
          v-if="projectWorkspaceNavProjectId && !assetProjectContext"
          :project-id="projectWorkspaceNavProjectId"
        />
        <RouterView />
      </main>
    </div>

    <el-button
      v-if="showHermesEntry"
      class="hermes-global-entry"
      type="primary"
      :icon="ChatDotRound"
      @click="hermesDrawerVisible = true"
    >
      <span>Hermes 助手</span>
      <small>{{ globalHermesHint }}</small>
    </el-button>

    <HermesWorkspaceDrawer v-model="hermesDrawerVisible">
      <DataStewardPanel
        v-if="globalHermesProjectId"
        :project-id="globalHermesProjectId"
        :page-type="globalHermesPageType"
        source-view="ProjectAssetView"
        :current-route="route.fullPath"
        :project-code="globalHermesProject?.code"
        :project-name="globalHermesProject?.name"
        :page-title="globalHermesPageTitle"
      />
      <el-empty v-else description="请先选择项目" :image-size="56" />
    </HermesWorkspaceDrawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowDown, Back, Bell, ChatDotRound, QuestionFilled, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

import DataStewardPanel from '@/modules/data-steward/components/DataStewardPanel.vue';
import HermesWorkspaceDrawer from '@/modules/data-steward/components/HermesWorkspaceDrawer.vue';
import ProjectWorkspaceNav from '@/modules/core/components/ProjectWorkspaceNav.vue';
import SidebarMenu from '@/modules/core/components/SidebarMenu.vue';
import { useProjectWorkspaceContext } from '@/modules/core/composables/useProjectWorkspaceContext';
import type { MenuItem } from '@/modules/core/api/types';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const hermesDrawerVisible = ref(false);

const { assetProjectContext, routeProjectId } = useProjectWorkspaceContext();

const hermesEnabledRouteNames = new Set([
  'data-steward-assets',
  'data-steward-asset-detail',
  'project-master-data-initialization',
  'project-work-agent-governance'
]);

const menus = computed(() => {
  const globalMenus: MenuItem[] = [
    {
      key: 'project-launchpad',
      label: '项目启动台',
      path: '/data-steward/assets',
      icon: 'House'
    },
    {
      key: 'asset-governance',
      label: '资产治理',
      path: '/data-steward/catalog',
      icon: 'DataBoard'
    },
    {
      key: 'bim-collaboration',
      label: 'BIM 协同',
      path: '/bim-collaboration',
      icon: 'Monitor'
    }
  ];
  if (hasEmployeeManagementPermission()) {
    globalMenus.push({
      key: 'admin-center',
      label: '管理中心',
      path: '/admin/employees',
      icon: 'User'
    });
  }
  globalMenus.push({
    key: 'my-projects',
    label: '我的项目',
    path: '/data-steward/assets',
    icon: 'OfficeBuilding',
    children: authorizedProjects.value.slice(0, 8).map((item) => ({
      key: `project-${item.id}`,
      label: item.name,
      path: `/data-steward/assets/${item.id}`,
      icon: 'FolderOpened'
    }))
  });
  globalMenus.push(
    {
      key: 'help-center',
      label: '帮助中心',
      path: '/home',
      icon: 'QuestionFilled'
    },
    {
      key: 'hermes-assistant',
      label: 'Hermes 助手',
      path: '/data-steward/agent-preview',
      icon: 'ChatDotRound'
    }
  );
  return globalMenus;
});

const authorizedProjects = computed(() => authStore.currentUser?.projects ?? []);

const routeQueryProjectId = computed(() => {
  const raw = route.query.projectId;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : null;
});

const platformRootRouteNames = new Set([
  'data-steward-assets',
  'bim-collaboration',
  'data-steward-catalog',
  'data-steward-quality',
  'data-steward-scans',
  'data-steward-agent-preview',
  'admin-employees',
  'home',
  'access-pending'
]);

const platformProjectQueryRouteNames = new Set([
  'bim-collaboration'
]);

const routeName = computed(() => String(route.name ?? ''));

const shellProjectId = computed(() => {
  if (routeProjectId.value) return routeProjectId.value;
  if (platformProjectQueryRouteNames.has(routeName.value)) return routeQueryProjectId.value;
  return null;
});

const shellProject = computed(() => {
  const projectId = shellProjectId.value;
  if (!projectId || !authStore.currentUser) return null;
  return authStore.currentUser.projects.find((item) => item.id === projectId)
    ?? (authStore.currentUser.currentProject?.id === projectId ? authStore.currentUser.currentProject : null);
});

const projectWorkspaceNavProjectId = computed(() => {
  const id = routeProjectId.value ?? (routeName.value === 'bim-collaboration' ? routeQueryProjectId.value : null);
  return typeof id === 'number' && Number.isFinite(id) ? id : null;
});

const globalHermesProjectId = computed(() => {
  const id = shellProjectId.value ?? routeProjectId.value;
  return typeof id === 'number' && Number.isFinite(id) ? id : null;
});

const globalHermesProject = computed(() => {
  const projectId = globalHermesProjectId.value;
  if (!projectId || !authStore.currentUser) return null;
  return authStore.currentUser.projects.find((item) => item.id === projectId)
    ?? (authStore.currentUser.currentProject?.id === projectId ? authStore.currentUser.currentProject : null);
});

const showHermesEntry = computed(() => {
  return Boolean(globalHermesProjectId.value && hermesEnabledRouteNames.has(routeName.value));
});

const globalHermesPageType = computed(() => {
  const labels: Record<string, string> = {
    'data-steward-assets': 'assets_overview',
    'data-steward-asset-detail': 'project_detail',
    'project-master-data-initialization': 'real_project_onboarding',
    'project-work-agent-governance': 'agent_governance'
  };
  return labels[routeName.value] ?? 'data_steward_workspace';
});

const globalHermesPageTitle = computed(() => {
  const labels: Record<string, string> = {
    'data-steward-assets': '资产总览',
    'data-steward-asset-detail': '项目工作台',
    'project-master-data-initialization': '真实项目接入向导',
    'project-work-agent-governance': '交付治理助手'
  };
  return labels[routeName.value] ?? '数据管家工作区';
});

const globalHermesHint = computed(() => globalHermesPageTitle.value);

const shellTitle = computed(() => {
  if (routeProjectId.value) {
    return globalHermesProject.value?.name ?? `项目 ${routeProjectId.value}`;
  }
  const labels: Record<string, string> = {
    'data-steward-assets': '项目资产总览',
    'data-steward-scans': '扫描任务',
    'data-steward-quality': '数据质量',
    'data-steward-catalog': '资产目录',
    'bim-collaboration': 'BIM协同管理',
    'admin-employees': '员工权限管理',
    'access-pending': '等待项目授权'
  };
  return labels[String(route.name ?? '')] ?? '卓羽智能数据中台';
});

const shellCurrentLabel = computed(() => {
  if (routeName.value === 'data-steward-asset-detail') {
    const tab = typeof route.query.tab === 'string' ? route.query.tab : 'dashboard';
    const labels: Record<string, string> = {
      dashboard: '概览',
      files: '文件管理',
      'master-data': '工程主数据',
      ownership: '工程主数据',
      delivery: '交付闭环',
      bim: 'BIM 协同',
      archive: '档案目录',
      scans: '扫描任务',
      mappings: '路径映射'
    };
    return labels[tab] ?? '概览';
  }
  if (routeName.value.startsWith('project-work-')) {
    if (routeName.value === 'project-work-delivery-package') return '档案目录';
    return '交付闭环';
  }
  if (routeName.value.startsWith('project-master-data-')) return '工程主数据';
  if (routeName.value === 'bim-collaboration' && shellProject.value) return '协同看板';
  if (routeName.value === 'bim-collaboration') return 'BIM 协同';
  return shellTitle.value;
});

const breadcrumbRootLabel = computed(() => {
  if (routeName.value === 'bim-collaboration') return 'BIM 协同';
  return '项目启动台';
});

const breadcrumbRootRouteName = computed(() => {
  if (routeName.value === 'bim-collaboration') return 'bim-collaboration';
  return 'data-steward-assets';
});

const isBreadcrumbRootOnly = computed(() =>
  platformRootRouteNames.has(routeName.value) && !shellProject.value
);

const showBreadcrumbBack = computed(() => !isBreadcrumbRootOnly.value);
const showHeaderProjectSelect = computed(() =>
  Boolean(authorizedProjects.value.length && !platformRootRouteNames.has(routeName.value))
);

async function handleLogout() {
  await authStore.signOut();
  router.replace({ name: 'login' });
}

async function handleUserCommand(command: string) {
  if (command === 'logout') {
    await handleLogout();
  }
}

function goBreadcrumbRoot() {
  router.push({ name: breadcrumbRootRouteName.value });
}

function goProjectDashboard(projectId: number) {
  router.push({ name: 'data-steward-asset-detail', params: { projectId }, query: { tab: 'dashboard' } });
}

async function handleProjectSelect(value: number | string) {
  const projectId = Number(value);
  if (!Number.isFinite(projectId) || projectId <= 0) return;
  try {
    if (projectId !== authStore.currentProjectId) {
      await authStore.changeProject(projectId);
    }
    if (routeName.value === 'bim-collaboration') {
      router.push({ name: 'bim-collaboration', query: { projectId } });
      return;
    }
    if (routeProjectId.value && route.name) {
      router.push({ name: route.name, params: { ...route.params, projectId }, query: route.query });
      return;
    }
    goProjectDashboard(projectId);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '项目切换失败');
  }
}

function showSearchPlaceholder() {
  ElMessage.info('全局搜索入口已预留，当前可先通过项目启动台和文件管理筛选。');
}

const userInitial = computed(() => {
  const name = authStore.currentUser?.displayName || authStore.currentUser?.username || 'U';
  return name.trim().slice(0, 1).toUpperCase();
});

function hasEmployeeManagementPermission() {
  const permissions = authStore.currentUser?.permissions ?? [];
  return permissions.includes('CORE_USER_MANAGE') || permissions.includes('CORE_PROJECT_ROLE_MANAGE');
}
</script>

<style scoped>
.hermes-global-entry {
  align-items: center;
  bottom: 24px;
  box-shadow: var(--zy-shadow-md);
  display: inline-flex;
  gap: 10px;
  max-width: calc(100vw - 32px);
  position: fixed;
  right: 24px;
  z-index: 40;
  height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  font-weight: var(--zy-fw-semi);
  letter-spacing: 0.02em;
  transition:
    transform var(--zy-duration-2) var(--zy-ease-out),
    box-shadow var(--zy-duration-2) var(--zy-ease-out);
}

.hermes-global-entry:hover {
  transform: translateY(-1px);
  box-shadow: var(--zy-shadow-lg);
}

.hermes-global-entry small {
  border-left: 1px solid rgba(255, 255, 255, 0.32);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-medium);
  letter-spacing: 0.04em;
  line-height: 1;
  max-width: 140px;
  overflow: hidden;
  padding-left: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
  opacity: 0.92;
}

@media (max-width: 720px) {
  .hermes-global-entry {
    bottom: 14px;
    right: 14px;
  }

  .hermes-global-entry small {
    display: none;
  }
}
</style>
