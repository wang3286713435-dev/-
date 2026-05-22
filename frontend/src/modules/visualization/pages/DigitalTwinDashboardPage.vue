<template>
  <section class="bim-collab-page" :class="{ 'is-dark': themeMode === 'dark' }">
    <header class="bim-collab-hero">
      <div class="bim-collab-hero__title">
        <span>BIM COLLABORATION · PROJECT DATA</span>
        <h1>BIM协同管理平台</h1>
        <p>{{ dashboard?.project.name ?? projectLabel }}</p>
      </div>
      <div class="bim-collab-hero__actions">
        <el-tag effect="plain" round>{{ dashboard ? `项目 ${dashboard.project.projectId}` : '等待项目上下文' }}</el-tag>
        <el-switch
          v-model="darkThemeEnabled"
          active-text="深色"
          inactive-text="浅色"
          inline-prompt
          @change="persistTheme"
        />
        <el-button :icon="Refresh" :loading="loading" @click="loadPage">刷新平台数据</el-button>
      </div>
    </header>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    />

    <el-empty v-if="!loading && !dashboard && !errorMessage" description="请选择项目后查看 BIM 协同窗口" />

    <template v-if="dashboard && bimData">
      <section class="bim-collab-summary" aria-label="平台项目数据摘要">
        <article v-for="item in kpiCards" :key="item.label">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </article>
      </section>

      <main class="bim-collab-window-card" v-loading="loading" aria-label="平台内 BIM 协同窗口">
        <div class="bim-collab-window-card__header">
          <div>
            <span>平台窗口 · sc-datav BIM 前端组件</span>
            <strong>{{ dashboard.project.code }} / {{ dashboard.project.name }}</strong>
          </div>
          <div>
            <el-tag size="small" type="success" effect="plain">真实项目数据</el-tag>
            <el-tag size="small" effect="plain">{{ engineModeLabel(dashboard.modelSummary.engineMode) }}</el-tag>
            <el-tag v-if="!dashboard.modelSummary.viewerAvailable" size="small" type="warning" effect="plain">元数据视图</el-tag>
          </div>
        </div>

        <div ref="bimHostRef" class="bim-collab-react-host" />
      </main>

      <section class="bim-collab-footnote">
        <div>
          <span>下一步建议</span>
          <strong>{{ dashboard.deliverySummary.nextActionText }}</strong>
        </div>
        <ul>
          <li v-for="item in dashboard.safetyBoundary.guarantees.slice(0, 3)" :key="item">{{ item }}</li>
        </ul>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { useRoute } from 'vue-router';
import { createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';

import BimCollaborationIsland from '@/modules/visualization/bim-collab/BimCollaborationIsland';
import { mapDashboardToBimCollab } from '@/modules/visualization/bim-collab/mapDashboardToBimCollab';
import type { BimThemeMode } from '@/modules/visualization/bim-collab/types';
import {
  fetchDigitalTwinDashboard,
  type DigitalTwinDashboard
} from '@/modules/visualization/api/visualization';
import { useAuthStore } from '@/stores/auth';

const THEME_STORAGE_KEY = 'delivery:bim-collab-theme';

const authStore = useAuthStore();
const route = useRoute();
const loading = ref(false);
const dashboard = ref<DigitalTwinDashboard | null>(null);
const errorMessage = ref('');
const bimHostRef = ref<HTMLElement | null>(null);
const darkThemeEnabled = ref(readInitialTheme() === 'dark');
let bimRoot: Root | null = null;

const routeProjectId = computed(() => Number(route.params.projectId));
const projectId = computed(() => {
  const routeId = routeProjectId.value;
  if (Number.isFinite(routeId) && routeId > 0) return routeId;
  return authStore.currentProjectId;
});

const themeMode = computed<BimThemeMode>(() => (darkThemeEnabled.value ? 'dark' : 'light'));
const projectLabel = computed(() => authStore.currentUser?.currentProject?.name ?? '等待项目上下文');
const bimData = computed(() => (dashboard.value ? mapDashboardToBimCollab(dashboard.value) : null));

const kpiCards = computed(() => {
  const item = dashboard.value;
  if (!item) return [];
  return [
    { label: '资产文件', value: formatCount(item.assetSummary.fileCount), hint: formatBytes(item.assetSummary.totalSizeBytes) },
    { label: '模型文件', value: formatCount(item.assetSummary.modelFileCount), hint: `${formatCount(item.modelSummary.publishedModelCount)} 已发布` },
    { label: '图纸文件', value: formatCount(item.assetSummary.drawingFileCount), hint: '平台真实资产' },
    { label: '管理对象', value: formatCount(item.modelSummary.managedObjectCount), hint: engineModeLabel(item.modelSummary.engineMode) },
    { label: '质量风险', value: formatCount(item.qualitySummary.riskSignalCount), hint: `${formatCount(item.qualitySummary.pendingReviewCount)} 待审核` },
    { label: '交付完成', value: formatPercent(item.deliverySummary.completionRate), hint: `${formatCount(item.deliverySummary.missingCount)} 缺失` }
  ];
});

watch(projectId, () => loadPage(), { immediate: true });
watch([bimData, themeMode], () => renderBimWindow());

onMounted(() => renderBimWindow());
onBeforeUnmount(() => {
  bimRoot?.unmount();
  bimRoot = null;
});

async function loadPage() {
  if (!projectId.value) {
    dashboard.value = null;
    renderBimWindow();
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    dashboard.value = await fetchDigitalTwinDashboard(projectId.value);
  } catch (error) {
    dashboard.value = null;
    errorMessage.value = error instanceof Error ? error.message : 'BIM 协同窗口数据加载失败';
  } finally {
    loading.value = false;
  }
}

function renderBimWindow() {
  void nextTick(() => {
    if (!bimHostRef.value) return;
    if (!bimRoot) {
      bimRoot = createRoot(bimHostRef.value);
    }
    const data = bimData.value;
    bimRoot.render(data ? createElement(BimCollaborationIsland, { data, theme: themeMode.value }) : null);
  });
}

function persistTheme() {
  localStorage.setItem(THEME_STORAGE_KEY, themeMode.value);
  renderBimWindow();
}

function readInitialTheme(): BimThemeMode {
  const saved = localStorage.getItem(THEME_STORAGE_KEY);
  return saved === 'light' ? 'light' : 'dark';
}

function formatCount(value: number | null | undefined) {
  return new Intl.NumberFormat('zh-CN').format(value ?? 0);
}

function formatPercent(value: number | null | undefined) {
  return `${Math.round((value ?? 0) * 100)}%`;
}

function formatBytes(value: number | null | undefined) {
  const size = value ?? 0;
  if (size >= 1024 ** 4) return `${(size / 1024 ** 4).toFixed(1)} TB`;
  if (size >= 1024 ** 3) return `${(size / 1024 ** 3).toFixed(1)} GB`;
  if (size >= 1024 ** 2) return `${(size / 1024 ** 2).toFixed(1)} MB`;
  if (size >= 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${size} B`;
}

function engineModeLabel(value: string | null | undefined) {
  if (!value || value === 'MOCK' || value === 'METADATA_ADAPTER') return '元数据适配';
  return value;
}
</script>

<style scoped>
.bim-collab-page {
  --bim-bg: var(--zy-surface);
  --bim-panel: rgba(255, 255, 255, 0.94);
  --bim-line: rgba(37, 99, 235, 0.16);
  --bim-text: var(--zy-ink);
  --bim-muted: var(--zy-muted);
  --bim-accent: var(--zy-blue-600);
  background:
    linear-gradient(90deg, rgba(37, 99, 235, 0.05) 1px, transparent 1px),
    linear-gradient(180deg, rgba(37, 99, 235, 0.04) 1px, transparent 1px),
    var(--bim-bg);
  background-size: 42px 42px;
  border: 1px solid var(--bim-line);
  border-radius: var(--zy-radius-base);
  color: var(--bim-text);
  display: grid;
  gap: var(--zy-sp-4);
  min-width: 0;
  padding: var(--zy-sp-4);
}

.bim-collab-page.is-dark {
  --bim-bg: #07111f;
  --bim-panel: rgba(10, 27, 50, 0.9);
  --bim-line: rgba(91, 180, 255, 0.22);
  --bim-text: #e8f3ff;
  --bim-muted: #9fb7d1;
  --bim-accent: #66d8ff;
}

.bim-collab-hero {
  align-items: flex-start;
  display: flex;
  gap: var(--zy-sp-4);
  justify-content: space-between;
  min-width: 0;
}

.bim-collab-hero__title {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.bim-collab-hero__title span,
.bim-collab-footnote span {
  color: var(--bim-accent);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

.bim-collab-hero h1 {
  color: var(--bim-text);
  font-size: var(--zy-fs-3xl);
  line-height: 1.2;
  margin: 0;
}

.bim-collab-hero p {
  color: var(--bim-muted);
  margin: 0;
}

.bim-collab-hero__actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  justify-content: flex-end;
}

.bim-collab-summary {
  display: grid;
  gap: var(--zy-sp-3);
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.bim-collab-summary article,
.bim-collab-window-card,
.bim-collab-footnote {
  background: var(--bim-panel);
  border: 1px solid var(--bim-line);
  border-radius: var(--zy-radius-base);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.bim-collab-summary article {
  display: grid;
  gap: 4px;
  min-height: 82px;
  min-width: 0;
  padding: var(--zy-sp-3);
}

.bim-collab-summary span,
.bim-collab-summary em,
.bim-collab-window-card__header span,
.bim-collab-footnote li {
  color: var(--bim-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
}

.bim-collab-summary strong {
  color: var(--bim-text);
  font-size: var(--zy-fs-2xl);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.bim-collab-window-card {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: clamp(980px, 92dvh, 1160px);
  min-width: 0;
  overflow: hidden;
}

.bim-collab-window-card__header {
  align-items: center;
  border-bottom: 1px solid var(--bim-line);
  display: flex;
  gap: var(--zy-sp-3);
  justify-content: space-between;
  min-width: 0;
  padding: var(--zy-sp-3) var(--zy-sp-4);
}

.bim-collab-window-card__header > div {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.bim-collab-window-card__header > div:first-child {
  display: grid;
  gap: 2px;
}

.bim-collab-window-card__header strong,
.bim-collab-footnote strong {
  color: var(--bim-text);
  font-weight: var(--zy-fw-semi);
}

.bim-collab-react-host {
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.bim-collab-footnote {
  align-items: center;
  display: grid;
  gap: var(--zy-sp-4);
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.9fr);
  padding: var(--zy-sp-4);
}

.bim-collab-footnote > div {
  display: grid;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.bim-collab-footnote ul {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  justify-content: flex-end;
  list-style: none;
  margin: 0;
  padding: 0;
}

.bim-collab-footnote li {
  border: 1px solid var(--bim-line);
  border-radius: 999px;
  padding: 4px 10px;
}

@media (max-width: 1280px) {
  .bim-collab-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .bim-collab-window-card {
    min-height: 960px;
  }
}

@media (max-width: 860px) {
  .bim-collab-hero,
  .bim-collab-window-card__header,
  .bim-collab-footnote {
    display: grid;
  }

  .bim-collab-hero__actions,
  .bim-collab-footnote ul {
    justify-content: flex-start;
  }

  .bim-collab-summary {
    grid-template-columns: 1fr;
  }
}
</style>
