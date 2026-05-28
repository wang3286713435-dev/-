<template>
  <div class="app-layout">
    <aside class="app-layout__sidebar">
      <div class="app-layout__brand">
        <strong>数字化交付平台</strong>
        <span>ZHUOYU · BIM DELIVERY</span>
      </div>
      <SidebarMenu :menus="menus" />
      <div class="app-layout__sidebar-foot">
        <strong>BUILD · UX2</strong>
        <span>使用路径重构 / Field relief</span>
      </div>
    </aside>

    <div class="app-layout__main">
      <header class="app-layout__header">
        <div class="app-layout__header-left">
          <span class="app-layout__route-eyebrow">{{ shellEyebrow }}</span>
          <div class="app-layout__route-context">
            <strong>{{ shellTitle }}</strong>
            <small>{{ shellSubtitle }}</small>
          </div>
        </div>
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
    ?? (authStore.currentUser.currentProject?.id === projectId ? authStore.currentUser.currentProject : null);
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

const shellEyebrow = computed(() => {
  if (routeProjectId.value) return '当前项目工作台';
  if (String(route.name ?? '').startsWith('admin-')) return '管理中心';
  if (String(route.name ?? '').startsWith('c-tower-')) return 'C塔定制化演示';
  if (String(route.name ?? '').startsWith('bim-submission')) return 'BIM报建';
  return '平台主入口';
});

const shellTitle = computed(() => {
  if (routeProjectId.value) {
    return globalHermesProject.value?.name ?? `项目 ${routeProjectId.value}`;
  }
  const labels: Record<string, string> = {
    'c-tower-demo': 'C塔数据中台能力验证',
    'bim-submission': 'BIM报建总览',
    'bim-submission-overview': 'BIM报建总览',
    'bim-submission-code-center': 'BIM报建编码标准中心',
    'bim-submission-plugin-contract': 'BIM报建插件契约中心',
    'bim-submission-data-center': 'BIM报建数据中心',
    'bim-submission-quality': 'BIM报建质量校验',
    'bim-submission-batches': 'BIM报建批次闭环',
    'bim-submission-work-orders': 'BIM报建整改工单',
    'bim-submission-archives': 'BIM报建归档摘要',
    'data-steward-assets': '项目资产总览',
    'data-steward-scans': '扫描任务',
    'data-steward-quality': '数据质量',
    'data-steward-catalog': '资产目录',
    'admin-employees': '员工权限管理',
    'access-pending': '等待项目授权'
  };
  return labels[String(route.name ?? '')] ?? '数字化交付平台';
});

const shellSubtitle = computed(() => {
  if (routeProjectId.value) {
    return '先看资产，再确认工程主数据，最后进入交付工作中心。';
  }
  if (String(route.name ?? '') === 'data-steward-assets') {
    return '从真实 NAS 项目进入工作台，按项目推进数字化交付。';
  }
  if (String(route.name ?? '') === 'c-tower-demo') {
    return '展示当前能力验证范围、对应文件需求和仍需专项建设的缺口。';
  }
  if (String(route.name ?? '').startsWith('bim-submission')) {
    return '平台管理上传编码记录，智能化标准包当前为草案，真实数据等待插件回传。';
  }
  if (String(route.name ?? '').startsWith('admin-')) {
    return '管理员工账号、项目授权和试运行访问范围。';
  }
  return '请选择项目或功能入口继续。';
});

async function handleLogout() {
  await authStore.signOut();
  router.replace({ name: 'login' });
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
