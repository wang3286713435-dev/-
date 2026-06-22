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
      <SidebarMenu :menus="primaryMenus" />
      <div class="app-layout__sidebar-foot">
        <SidebarMenu :menus="supportMenus" class="app-layout__sidebar-support-menu" />
        <div class="app-layout__build-info">
          <strong>BUILD · UX4-A</strong>
          <span>项目壳层 / SaaS shell</span>
        </div>
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
            <template v-if="showShellProjectBreadcrumb && shellProject">
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
          <div ref="globalSearchShellRef" class="global-search-shell">
            <button class="app-layout__search-entry" type="button" @click="openGlobalSearch">
              <el-icon><Search /></el-icon>
              <span>搜索项目、文件、交付事项</span>
              <kbd>⌘K</kbd>
            </button>
            <section v-if="globalSearchVisible" class="global-search-panel" aria-label="全局搜索">
              <div class="global-search-panel__field">
                <el-icon><Search /></el-icon>
                <input
                  ref="globalSearchInputRef"
                  v-model="globalSearchKeyword"
                  type="search"
                  placeholder="搜索项目、文件、模型、交付事项..."
                  @keydown.enter.prevent="openFirstGlobalSearchResult"
                  @keydown.esc.prevent="closeGlobalSearch"
                />
              </div>
              <div class="global-search-panel__meta">
                <span>{{ globalSearchScopeLabel }}</span>
                <small>按 Enter 打开第一条结果</small>
              </div>

              <div v-if="globalSearchLoading" class="global-search-panel__state">
                正在搜索...
              </div>
              <div v-else-if="globalSearchError" class="global-search-panel__state is-error">
                {{ globalSearchError }}
              </div>
              <div
                v-else-if="globalSearchKeyword.trim() && !globalSearchResult?.totalCount"
                class="global-search-panel__state"
              >
                没有找到相关项目或文件
              </div>
              <div v-else-if="!globalSearchKeyword.trim()" class="global-search-panel__state">
                输入关键词后，会在你有权限的项目、文件、模型和交付事项中搜索。
              </div>
              <div v-else class="global-search-panel__groups">
                <section
                  v-for="group in visibleGlobalSearchGroups"
                  :key="group.type"
                  class="global-search-group"
                >
                  <header>
                    <strong>{{ group.label }}</strong>
                    <span>{{ group.count }}</span>
                  </header>
                  <button
                    v-for="item in group.items"
                    :key="`${item.type}-${item.id}`"
                    class="global-search-result"
                    type="button"
                    @click="openGlobalSearchItem(item)"
                  >
                    <span class="global-search-result__type" :class="`is-${item.type.toLowerCase()}`">
                      {{ globalSearchTypeLabel(item.type) }}
                    </span>
                    <span class="global-search-result__main">
                      <strong>{{ item.title }}</strong>
                      <small>{{ item.projectName }} · {{ item.subtitle }}</small>
                    </span>
                    <span class="global-search-result__status">{{ item.status || '可打开' }}</span>
                  </button>
                </section>
              </div>
            </section>
          </div>
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
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
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
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowDown, Back, Bell, ChatDotRound, QuestionFilled, Search } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

import DataStewardPanel from '@/modules/data-steward/components/DataStewardPanel.vue';
import HermesWorkspaceDrawer from '@/modules/data-steward/components/HermesWorkspaceDrawer.vue';
import ProjectWorkspaceNav from '@/modules/core/components/ProjectWorkspaceNav.vue';
import SidebarMenu from '@/modules/core/components/SidebarMenu.vue';
import { useProjectWorkspaceContext } from '@/modules/core/composables/useProjectWorkspaceContext';
import type { MenuItem } from '@/modules/core/api/types';
import { fetchGlobalSearch, type GlobalSearchGroup, type GlobalSearchItem } from '@/modules/core/api/search';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const hermesDrawerVisible = ref(false);
const globalSearchVisible = ref(false);
const globalSearchKeyword = ref('');
const globalSearchLoading = ref(false);
const globalSearchError = ref('');
const globalSearchResult = ref<Awaited<ReturnType<typeof fetchGlobalSearch>> | null>(null);
const globalSearchShellRef = ref<HTMLElement | null>(null);
const globalSearchInputRef = ref<HTMLInputElement | null>(null);
let globalSearchTimer: ReturnType<typeof window.setTimeout> | null = null;
let globalSearchRequestSeq = 0;

const { assetProjectContext, routeProjectId } = useProjectWorkspaceContext();

const hermesEnabledRouteNames = new Set([
  'data-steward-assets',
  'data-steward-asset-detail',
  'project-master-data-initialization',
  'project-work-agent-governance'
]);

const primaryMenus = computed(() => {
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
      icon: 'DataBoard',
      children: [
        {
          key: 'asset-catalog',
          label: '资产目录',
          path: '/data-steward/catalog',
          icon: 'FolderOpened'
        },
        {
          key: 'asset-quality',
          label: '数据质量',
          path: '/data-steward/quality',
          icon: 'Warning'
        },
        {
          key: 'asset-object-storage',
          label: '对象存储',
          path: '/data-steward/file-service',
          icon: 'Box'
        }
      ]
    },
    {
      key: 'bim-collaboration',
      label: 'BIM 协同',
      path: '/bim-collaboration',
      icon: 'Monitor'
    }
  ];
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
  if (hasEmployeeManagementPermission()) {
    globalMenus.push({
      key: 'admin-center',
      label: '管理中心',
      path: '/admin/employees',
      icon: 'User'
    });
  }
  return globalMenus;
});

const supportMenus = computed<MenuItem[]>(() => [
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
]);

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
  'data-steward-file-service',
  'data-steward-quality',
  'data-steward-scans',
  'data-steward-agent-preview',
  'admin-employees',
  'user-profile',
  'home',
  'access-pending'
]);

const platformProjectQueryRouteNames = new Set([
  'bim-collaboration',
  'glandar-model-preview'
]);

const routeName = computed(() => String(route.name ?? ''));

const routeQueryFileName = computed(() => {
  const raw = route.query.fileName;
  const value = Array.isArray(raw) ? raw[0] : raw;
  return typeof value === 'string' ? value.trim() : '';
});

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
    'data-steward-file-service': '对象存储',
    'bim-collaboration': 'BIM协同管理',
    'admin-employees': '员工权限管理',
    'user-profile': '个人中心',
    'access-pending': '等待项目授权'
  };
  return labels[String(route.name ?? '')] ?? '卓羽智能数据中台';
});

const shellCurrentLabel = computed(() => {
  if (routeName.value === 'glandar-model-preview') {
    return routeQueryFileName.value || '模型预览';
  }
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
  if (routeName.value === 'bim-collaboration' || routeName.value === 'glandar-model-preview') return 'BIM 协同';
  return '项目启动台';
});

const breadcrumbRootRouteName = computed(() => {
  if (routeName.value === 'bim-collaboration' || routeName.value === 'glandar-model-preview') return 'bim-collaboration';
  return 'data-steward-assets';
});

const breadcrumbRootQuery = computed(() => {
  if (breadcrumbRootRouteName.value !== 'bim-collaboration') return {};
  const projectId = routeQueryProjectId.value ?? shellProjectId.value;
  return projectId ? { projectId } : {};
});

const showShellProjectBreadcrumb = computed(() => routeName.value !== 'glandar-model-preview');

const isBreadcrumbRootOnly = computed(() =>
  platformRootRouteNames.has(routeName.value) && !shellProject.value
);

const showBreadcrumbBack = computed(() => !isBreadcrumbRootOnly.value);
const showHeaderProjectSelect = computed(() =>
  Boolean(authorizedProjects.value.length && !platformRootRouteNames.has(routeName.value))
);

const globalSearchScopeLabel = computed(() => {
  if (shellProject.value) return `搜索全部授权项目 · 当前在 ${shellProject.value.name}`;
  return '搜索全部授权项目';
});

const visibleGlobalSearchGroups = computed<GlobalSearchGroup[]>(() =>
  (globalSearchResult.value?.groups ?? []).filter((group) => group.count > 0)
);

async function handleLogout() {
  await authStore.signOut();
  router.replace({ name: 'login' });
}

async function handleUserCommand(command: string) {
  if (command === 'profile') {
    router.push({ name: 'user-profile' });
    return;
  }
  if (command === 'logout') {
    await handleLogout();
  }
}

function goBreadcrumbRoot() {
  router.push({ name: breadcrumbRootRouteName.value, query: breadcrumbRootQuery.value });
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

function openGlobalSearch() {
  globalSearchVisible.value = true;
  nextTick(() => globalSearchInputRef.value?.focus());
}

function closeGlobalSearch() {
  globalSearchVisible.value = false;
}

async function runGlobalSearch() {
  const keyword = globalSearchKeyword.value.trim();
  globalSearchError.value = '';
  if (!keyword) {
    globalSearchResult.value = null;
    globalSearchLoading.value = false;
    return;
  }
  const seq = ++globalSearchRequestSeq;
  globalSearchLoading.value = true;
  try {
    const result = await fetchGlobalSearch({ keyword, limit: 5 });
    if (seq === globalSearchRequestSeq) {
      globalSearchResult.value = result;
    }
  } catch (error) {
    if (seq === globalSearchRequestSeq) {
      globalSearchResult.value = null;
      globalSearchError.value = error instanceof Error ? error.message : '搜索失败，请稍后重试';
    }
  } finally {
    if (seq === globalSearchRequestSeq) {
      globalSearchLoading.value = false;
    }
  }
}

function scheduleGlobalSearch() {
  if (globalSearchTimer) window.clearTimeout(globalSearchTimer);
  globalSearchTimer = window.setTimeout(() => {
    void runGlobalSearch();
  }, 260);
}

function openFirstGlobalSearchResult() {
  const item = visibleGlobalSearchGroups.value.flatMap((group) => group.items)[0];
  if (item) openGlobalSearchItem(item);
}

function openGlobalSearchItem(item: GlobalSearchItem) {
  closeGlobalSearch();
  router.push({
    name: item.routeName,
    params: item.routeParams ?? {},
    query: item.routeQuery ?? {}
  });
}

function globalSearchTypeLabel(type: GlobalSearchItem['type']) {
  const labels: Record<GlobalSearchItem['type'], string> = {
    PROJECT: '项目',
    FILE: '文件',
    MODEL: '模型',
    DELIVERY: '交付',
    RECTIFICATION: '整改'
  };
  return labels[type] ?? '结果';
}

function handleGlobalSearchShortcut(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault();
    openGlobalSearch();
    return;
  }
  if (event.key === 'Escape' && globalSearchVisible.value) {
    closeGlobalSearch();
  }
}

function handleGlobalSearchOutside(event: MouseEvent) {
  if (!globalSearchVisible.value) return;
  const target = event.target as Node | null;
  if (target && globalSearchShellRef.value?.contains(target)) return;
  closeGlobalSearch();
}

watch(globalSearchKeyword, scheduleGlobalSearch);

onMounted(() => {
  window.addEventListener('keydown', handleGlobalSearchShortcut);
  window.addEventListener('mousedown', handleGlobalSearchOutside);
});

onBeforeUnmount(() => {
  if (globalSearchTimer) window.clearTimeout(globalSearchTimer);
  window.removeEventListener('keydown', handleGlobalSearchShortcut);
  window.removeEventListener('mousedown', handleGlobalSearchOutside);
});

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
.global-search-shell {
  position: relative;
}

.global-search-panel {
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 22px;
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.16);
  left: 50%;
  overflow: hidden;
  position: absolute;
  top: calc(100% + 12px);
  transform: translateX(-50%);
  width: min(620px, calc(100vw - 380px));
  z-index: 80;
}

.global-search-panel__field {
  align-items: center;
  border-bottom: 1px solid rgba(226, 232, 240, 0.92);
  display: flex;
  gap: 10px;
  padding: 14px 16px;
}

.global-search-panel__field .el-icon {
  color: #2563eb;
  font-size: 18px;
}

.global-search-panel__field input {
  background: transparent;
  border: 0;
  color: #0f172a;
  flex: 1;
  font-size: 15px;
  line-height: 24px;
  min-width: 0;
  outline: none;
}

.global-search-panel__field input::placeholder {
  color: #94a3b8;
}

.global-search-panel__meta {
  align-items: center;
  color: #64748b;
  display: flex;
  font-size: 12px;
  justify-content: space-between;
  padding: 9px 16px 0;
}

.global-search-panel__meta small {
  color: #94a3b8;
}

.global-search-panel__state {
  color: #64748b;
  font-size: 14px;
  padding: 28px 16px 30px;
  text-align: center;
}

.global-search-panel__state.is-error {
  color: #dc2626;
}

.global-search-panel__groups {
  max-height: min(60vh, 560px);
  overflow: auto;
  padding: 10px 10px 12px;
}

.global-search-group + .global-search-group {
  border-top: 1px solid rgba(226, 232, 240, 0.82);
  margin-top: 8px;
  padding-top: 8px;
}

.global-search-group header {
  align-items: center;
  color: #64748b;
  display: flex;
  font-size: 12px;
  justify-content: space-between;
  padding: 6px 8px;
}

.global-search-group header strong {
  color: #334155;
  font-size: 12px;
}

.global-search-group header span {
  background: #eff6ff;
  border-radius: 999px;
  color: #2563eb;
  font-weight: 700;
  min-width: 24px;
  padding: 2px 8px;
  text-align: center;
}

.global-search-result {
  align-items: center;
  background: transparent;
  border: 0;
  border-radius: 14px;
  color: inherit;
  cursor: pointer;
  display: grid;
  gap: 12px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  padding: 10px 8px;
  text-align: left;
  width: 100%;
}

.global-search-result:hover,
.global-search-result:focus-visible {
  background: #f8fafc;
  outline: none;
}

.global-search-result__type {
  align-items: center;
  border-radius: 12px;
  display: inline-flex;
  font-size: 12px;
  font-weight: 700;
  height: 32px;
  justify-content: center;
  width: 44px;
}

.global-search-result__type.is-project {
  background: #dbeafe;
  color: #1d4ed8;
}

.global-search-result__type.is-file {
  background: #ecfdf5;
  color: #047857;
}

.global-search-result__type.is-model {
  background: #eef2ff;
  color: #4f46e5;
}

.global-search-result__type.is-delivery {
  background: #fff7ed;
  color: #c2410c;
}

.global-search-result__type.is-rectification {
  background: #fef2f2;
  color: #b91c1c;
}

.global-search-result__main {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.global-search-result__main strong,
.global-search-result__main small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.global-search-result__main strong {
  color: #0f172a;
  font-size: 14px;
}

.global-search-result__main small {
  color: #64748b;
  font-size: 12px;
}

.global-search-result__status {
  color: #64748b;
  font-size: 12px;
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

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
  .global-search-panel {
    left: auto;
    right: 0;
    transform: none;
    width: min(92vw, 520px);
  }

  .global-search-panel__meta small {
    display: none;
  }

  .hermes-global-entry {
    bottom: 14px;
    right: 14px;
  }

  .hermes-global-entry small {
    display: none;
  }
}
</style>
