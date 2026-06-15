<template>
  <nav class="project-workspace-nav" aria-label="项目内导航">
    <div class="project-workspace-nav__summary">
      <el-button
        text
        size="small"
        :icon="Back"
        class="project-workspace-nav__back"
        @click="router.push({ name: 'data-steward-assets' })"
      >
        项目启动台
      </el-button>
      <div class="project-workspace-nav__project">
        <strong :title="projectName">{{ projectName }}</strong>
        <span v-if="projectCode">{{ projectCode }}</span>
      </div>
      <el-tag size="small" :type="masterDataReady ? 'success' : 'warning'" effect="plain">
        {{ masterDataReady ? '主数据已就绪' : '待确认主数据' }}
      </el-tag>
    </div>

    <div class="project-workspace-nav__tabs" role="list">
      <button
        v-for="item in tabs"
        :key="item.key"
        type="button"
        role="listitem"
        :class="{ 'is-active': item.key === activeTabKey }"
        @click="openTab(item)"
      >
        {{ item.label }}
      </button>
    </div>

    <div v-if="secondaryTabs.length" class="project-workspace-nav__subtabs" role="list" aria-label="项目内二级导航">
      <button
        v-for="item in secondaryTabs"
        :key="item.key"
        type="button"
        role="listitem"
        :class="{ 'is-active': item.key === activeSecondaryTabKey }"
        @click="openSecondaryTab(item)"
      >
        {{ item.label }}
      </button>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { RouteRecordName } from 'vue-router';
import { useRoute, useRouter } from 'vue-router';
import { Back } from '@element-plus/icons-vue';

import { fetchInitializationStatus, type InitializationStatus } from '@/modules/master-data/api/masterData';
import { useAuthStore } from '@/stores/auth';

type WorkspaceTab = {
  key: string;
  label: string;
  name?: RouteRecordName;
  tab?: string;
  query?: Record<string, string | number>;
};

type SecondaryWorkspaceTab = {
  key: string;
  label: string;
  name: RouteRecordName;
  tab?: string;
};

const props = defineProps<{
  projectId: number;
}>();

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const initializationStatus = ref<InitializationStatus | null>(null);

const tabs: WorkspaceTab[] = [
  { key: 'dashboard', label: '概览', name: 'data-steward-asset-detail', tab: 'dashboard' },
  { key: 'files', label: '文件管理', name: 'data-steward-asset-detail', tab: 'files' },
  { key: 'ownership', label: '工程主数据', name: 'data-steward-asset-detail', tab: 'ownership' },
  { key: 'delivery', label: '交付闭环', name: 'project-work-document-delivery' },
  { key: 'bim', label: 'BIM 协同', name: 'bim-collaboration', query: { projectId: props.projectId } },
  { key: 'archive', label: '档案目录', name: 'project-work-delivery-package' }
];

const masterDataSecondaryTabs: SecondaryWorkspaceTab[] = [
  { key: 'tree', label: '工程树', name: 'data-steward-asset-detail', tab: 'master-data' },
  { key: 'onboarding', label: '接入向导', name: 'project-master-data-initialization' },
  { key: 'sections', label: '部位树', name: 'project-master-data-sections' },
  { key: 'node-types', label: '节点类型', name: 'project-master-data-node-types' },
  { key: 'standard', label: '交付物标准', name: 'project-master-data-deliverable-standard' }
];

const project = computed(() =>
  authStore.currentUser?.projects.find((item) => item.id === props.projectId)
);
const projectName = computed(() => project.value?.name ?? `项目 ${props.projectId}`);
const projectCode = computed(() => project.value?.code ?? '');
const masterDataReady = computed(() =>
  Boolean(initializationStatus.value?.ready || initializationStatus.value?.standardStatus?.deliverableStandardReady)
);

const activeTabKey = computed(() => {
  const routeName = String(route.name ?? '');
  if (routeName === 'data-steward-asset-detail') {
    const current = typeof route.query.tab === 'string' ? route.query.tab : 'dashboard';
    if (current === 'files') return 'files';
    if (current === 'ownership') return 'ownership';
    return 'dashboard';
  }
  if (routeName === 'bim-collaboration') return 'bim';
  if (routeName === 'project-work-delivery-package') return 'archive';
  if (routeName.startsWith('project-work-')) return 'delivery';
  if (routeName.startsWith('project-master-data-')) return 'ownership';
  return '';
});

const secondaryTabs = computed(() => (activeTabKey.value === 'ownership' ? masterDataSecondaryTabs : []));

const activeSecondaryTabKey = computed(() => {
  const routeName = String(route.name ?? '');
  if (routeName === 'project-master-data-initialization') return 'onboarding';
  if (routeName === 'project-master-data-sections') return 'sections';
  if (routeName === 'project-master-data-node-types') return 'node-types';
  if (routeName === 'project-master-data-deliverable-standard') return 'standard';
  if (routeName === 'data-steward-asset-detail') {
    const current = typeof route.query.tab === 'string' ? route.query.tab : 'dashboard';
    if (current === 'master-data' || current === 'ownership') return 'tree';
  }
  return '';
});

watch(
  () => props.projectId,
  () => {
    void loadInitializationStatus();
  },
  { immediate: true }
);

async function loadInitializationStatus() {
  if (!Number.isFinite(props.projectId)) return;
  try {
    initializationStatus.value = await fetchInitializationStatus(props.projectId);
  } catch {
    initializationStatus.value = null;
  }
}

function openTab(item: WorkspaceTab) {
  if (item.name === 'bim-collaboration') {
    router.push({ name: 'data-steward-asset-detail', params: { projectId: props.projectId }, query: { tab: 'bim' } });
    return;
  }
  if (item.name === 'data-steward-asset-detail') {
    router.push({
      name: item.name,
      params: { projectId: props.projectId },
      query: { tab: item.tab ?? 'dashboard' }
    });
    return;
  }
  if (item.name) {
    router.push({ name: item.name, params: { projectId: props.projectId }, query: item.query });
  }
}

function openSecondaryTab(item: SecondaryWorkspaceTab) {
  if (item.name === 'data-steward-asset-detail') {
    router.push({
      name: item.name,
      params: { projectId: props.projectId },
      query: { tab: item.tab ?? 'master-data' }
    });
    return;
  }
  router.push({ name: item.name, params: { projectId: props.projectId } });
}
</script>

<style scoped>
.project-workspace-nav {
  display: grid;
  gap: var(--zy-sp-3);
  min-width: 0;
  margin-bottom: var(--zy-sp-4);
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
}

.project-workspace-nav__summary {
  display: flex;
  align-items: center;
  gap: var(--zy-sp-3);
  min-width: 0;
}

.project-workspace-nav__back.el-button {
  flex-shrink: 0;
  color: var(--zy-blue-700);
  font-weight: var(--zy-fw-semi);
}

.project-workspace-nav__project {
  display: flex;
  align-items: baseline;
  gap: var(--zy-sp-2);
  min-width: 0;
  flex: 1 1 auto;
}

.project-workspace-nav__project strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-base);
  font-weight: var(--zy-fw-semi);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-workspace-nav__project span {
  color: var(--zy-muted);
  font-family: var(--zy-font-mono);
  font-size: var(--zy-fs-xs);
}

.project-workspace-nav__tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  overflow-x: auto;
  padding: 4px;
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
}

.project-workspace-nav__tabs button {
  appearance: none;
  border: 0;
  border-radius: var(--zy-radius-sm);
  background: transparent;
  color: var(--zy-muted);
  cursor: pointer;
  flex: 0 0 auto;
  font-family: inherit;
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
  min-height: 32px;
  padding: 0 var(--zy-sp-3);
  transition:
    background-color var(--zy-duration-2) var(--zy-ease),
    color var(--zy-duration-2) var(--zy-ease),
    box-shadow var(--zy-duration-2) var(--zy-ease);
}

.project-workspace-nav__tabs button:hover {
  background: var(--zy-surface);
  color: var(--zy-blue-700);
}

.project-workspace-nav__tabs button.is-active {
  background: var(--zy-surface);
  color: var(--zy-blue-700);
  box-shadow: var(--zy-shadow-xs);
}

.project-workspace-nav__subtabs {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  overflow-x: auto;
  padding: 0 2px;
}

.project-workspace-nav__subtabs button {
  appearance: none;
  border: var(--zy-border);
  border-radius: var(--zy-radius-sm);
  background: var(--zy-surface);
  color: var(--zy-muted);
  cursor: pointer;
  flex: 0 0 auto;
  font-family: inherit;
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
  min-height: 28px;
  padding: 0 var(--zy-sp-3);
  transition:
    border-color var(--zy-duration-2) var(--zy-ease),
    color var(--zy-duration-2) var(--zy-ease),
    background-color var(--zy-duration-2) var(--zy-ease);
}

.project-workspace-nav__subtabs button:hover {
  border-color: var(--zy-blue-300);
  color: var(--zy-blue-700);
}

.project-workspace-nav__subtabs button.is-active {
  border-color: var(--zy-blue-300);
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
}

@media (max-width: 720px) {
  .project-workspace-nav__summary {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--zy-sp-2);
  }
}
</style>
