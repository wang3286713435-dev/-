<template>
  <div class="app-layout">
    <aside class="app-layout__sidebar">
      <div class="app-layout__brand">
        <strong>数字化交付平台</strong>
        <span>一期基础工程</span>
      </div>
      <SidebarMenu :menus="menus" />
    </aside>

    <div class="app-layout__main">
      <header class="app-layout__header">
        <div class="app-layout__header-left" />
        <div class="app-layout__actions">
          <div class="app-layout__user">
            <strong>{{ authStore.currentUser?.displayName }}</strong>
            <span>{{ authStore.currentUser?.username }}</span>
          </div>
          <el-button text @click="handleLogout">退出</el-button>
        </div>
      </header>

      <main class="app-layout__content">
        <ProjectWorkspaceNav
          v-if="routeProjectId && !assetProjectContext"
          :project-id="routeProjectId"
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
      <span>Hermes</span>
      <small>{{ globalHermesHint }}</small>
    </el-button>

    <el-drawer v-model="hermesDrawerVisible" title="Hermes 数据管家" size="560px">
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
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ChatDotRound } from '@element-plus/icons-vue';

import DataStewardPanel from '@/modules/data-steward/components/DataStewardPanel.vue';
import ProjectWorkspaceNav from '@/modules/core/components/ProjectWorkspaceNav.vue';
import SidebarMenu from '@/modules/core/components/SidebarMenu.vue';
import { useProjectWorkspaceContext } from '@/modules/core/composables/useProjectWorkspaceContext';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const hermesDrawerVisible = ref(false);

const { assetProjectContext, routeProjectId, workspaceProjectId } = useProjectWorkspaceContext();

const hiddenTopLevelKeys = new Set(['home', 'master-data', 'work-center']);
const hiddenTopLevelPathPrefixes = ['/home', '/master-data', '/work'];
const hermesEnabledRouteNames = new Set([
  'data-steward-assets',
  'data-steward-asset-detail',
  'project-master-data-initialization',
  'project-work-agent-governance'
]);

const menus = computed(() => {
  const all = authStore.currentUser?.menus ?? [];
  return all.filter((m) => {
    if (hiddenTopLevelKeys.has(m.key)) return false;
    return !hiddenTopLevelPathPrefixes.some((prefix) => m.path === prefix || m.path.startsWith(`${prefix}/`));
  });
});

const globalHermesProjectId = computed(() => {
  const id = workspaceProjectId.value;
  return typeof id === 'number' && Number.isFinite(id) ? id : null;
});

const globalHermesProject = computed(() => {
  const projectId = globalHermesProjectId.value;
  if (!projectId || !authStore.currentUser) return null;
  return authStore.currentUser.projects.find((item) => item.id === projectId)
    ?? (authStore.currentUser.currentProject.id === projectId ? authStore.currentUser.currentProject : null);
});

const showHermesEntry = computed(() => {
  const routeName = String(route.name ?? '');
  return Boolean(globalHermesProjectId.value && hermesEnabledRouteNames.has(routeName));
});

const globalHermesPageType = computed(() => {
  const routeName = String(route.name ?? '');
  const labels: Record<string, string> = {
    'data-steward-assets': 'assets_overview',
    'data-steward-asset-detail': 'project_detail',
    'project-master-data-initialization': 'real_project_onboarding',
    'project-work-agent-governance': 'agent_governance'
  };
  return labels[routeName] ?? 'data_steward_workspace';
});

const globalHermesPageTitle = computed(() => {
  const routeName = String(route.name ?? '');
  const labels: Record<string, string> = {
    'data-steward-assets': '资产总览',
    'data-steward-asset-detail': '项目工作台',
    'project-master-data-initialization': '真实项目接入向导',
    'project-work-agent-governance': '交付治理助手'
  };
  return labels[routeName] ?? '数据管家工作区';
});

const globalHermesHint = computed(() => globalHermesPageTitle.value);

async function handleLogout() {
  await authStore.signOut();
  router.replace({ name: 'login' });
}
</script>

<style scoped>
.hermes-global-entry {
  align-items: center;
  bottom: 22px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.22);
  display: inline-flex;
  gap: 8px;
  max-width: calc(100vw - 32px);
  position: fixed;
  right: 24px;
  z-index: 40;
}

.hermes-global-entry small {
  border-left: 1px solid rgba(255, 255, 255, 0.36);
  font-size: 12px;
  line-height: 1;
  max-width: 120px;
  overflow: hidden;
  padding-left: 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
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
