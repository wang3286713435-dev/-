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
        <el-button plain @click="toggleTheme">
          {{ darkThemeEnabled ? '切换浅色' : '切换深色' }}
        </el-button>
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
import type { BimLightweightSummary, BimMetric, BimThemeMode } from '@/modules/visualization/bim-collab/types';
import {
  fetchDigitalTwinDashboard,
  fetchGlandarModelFiles,
  type GlandarModelFile,
  type DigitalTwinDashboard
} from '@/modules/visualization/api/visualization';
import type { BimEmbeddedPreviewModel } from '@/modules/visualization/bim-collab/types';
import {
  fetchFileOwnershipCoverage,
  fetchFileOwnershipTree,
  type FileOwnershipCoverage,
  type FileOwnershipTree
} from '@/modules/data-steward/api/dataSteward';
import {
  fetchSectionTree,
  fetchStandardStatus,
  type SectionNode,
  type StandardStatus
} from '@/modules/master-data/api/masterData';
import { useAuthStore } from '@/stores/auth';

const THEME_STORAGE_KEY = 'delivery:bim-collab-theme';

const authStore = useAuthStore();
const route = useRoute();
const loading = ref(false);
const dashboard = ref<DigitalTwinDashboard | null>(null);
const modelFiles = ref<GlandarModelFile[]>([]);
const standardStatus = ref<StandardStatus | null>(null);
const sectionTree = ref<SectionNode[]>([]);
const ownershipCoverage = ref<FileOwnershipCoverage | null>(null);
const ownershipTree = ref<FileOwnershipTree | null>(null);
const errorMessage = ref('');
const bimHostRef = ref<HTMLElement | null>(null);
const darkThemeEnabled = ref(readInitialTheme() === 'dark');
let bimRoot: Root | null = null;

const routeProjectId = computed(() => Number(route.params.projectId));
const routeQueryProjectId = computed(() => Number(route.query.projectId));
const projectId = computed(() => {
  const routeId = routeProjectId.value;
  if (Number.isFinite(routeId) && routeId > 0) return routeId;
  const queryId = routeQueryProjectId.value;
  if (Number.isFinite(queryId) && queryId > 0) return queryId;
  return authStore.currentProjectId;
});

const themeMode = computed<BimThemeMode>(() => (darkThemeEnabled.value ? 'dark' : 'light'));
const projectLabel = computed(() => {
  const selectedProject = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return selectedProject?.name ?? authStore.currentUser?.currentProject?.name ?? '等待项目上下文';
});
const bimData = computed(() => (dashboard.value ? mapDashboardToBimCollab(dashboard.value) : null));
const sortedModelFiles = computed(() => (
  [...modelFiles.value].sort((left, right) => {
    const rankDiff = lightweightSortRank(left) - lightweightSortRank(right);
    if (rankDiff !== 0) return rankDiff;
    const scoreDiff = scoreModelCompleteness(right) - scoreModelCompleteness(left);
    if (scoreDiff !== 0) return scoreDiff;
    return (left.fileId ?? 0) - (right.fileId ?? 0);
  })
));
const embeddedPreviewModels = computed<BimEmbeddedPreviewModel[]>(() => {
  const currentProjectId = projectId.value;
  if (!currentProjectId) return [];
  return sortedModelFiles.value
    .filter((item) => item.viewerAvailable && item.latestJobId)
    .sort((left, right) => scoreModelCompleteness(right) - scoreModelCompleteness(left))
    .map((item) => ({
      id: `glandar-${item.fileId}`,
      modelFileId: item.fileId,
      label: item.fileName,
      meta: `文件 ${item.fileId} · ${formatBytes(item.sizeBytes)}`,
      modelFormat: item.extension || 'RVT',
      versionNo: item.versionNo || 'V1',
      integrationStatus: 'GLANDAR_READY',
      status: 'normal',
      weight: Math.max(item.sizeBytes ?? 1, 1),
      previewStatus: 'AVAILABLE',
      previewMode: 'GLANDAR_VIEWER',
      conversionStatus: item.taskStatus || 'READY',
      viewerAvailable: true,
      statusLabel: normalizedLightweightStatusLabel(item),
      actionHint: '已通过葛兰岱尔轻量化，可在当前 BIM 协同窗口内直接预览。',
      fileManagerUrl: `/data-steward/assets/${currentProjectId}?tab=files&fileKeyword=${encodeURIComponent(item.fileName)}&lastFileId=${item.fileId}`,
      sizeLabel: formatBytes(item.sizeBytes),
      frameUrl: `/visualization/glandar-viewer-embed?projectId=${currentProjectId}&jobId=${encodeURIComponent(item.latestJobId || '')}&fileName=${encodeURIComponent(item.fileName)}&modelFileId=${item.fileId}&embedded=1&theme=${themeMode.value}`
    }));
});
const lightweightSummary = computed<BimLightweightSummary>(() => {
  const totalModelFiles = modelFiles.value.length || dashboard.value?.assetSummary.modelFileCount || 0;
  const readyModels = embeddedPreviewModels.value;
  const failedCount = modelFiles.value.filter((item) => item.taskStatus === 'FAILED').length;
  return {
    totalModelFiles,
    readyCount: readyModels.length,
    pendingCount: Math.max(totalModelFiles - readyModels.length, 0),
    failedCount,
    allModels: sortedModelFiles.value.map((item) => ({
      id: `glandar-${item.fileId}`,
      fileId: item.fileId,
      assetUuid: item.assetUuid,
      fileName: item.fileName,
      extension: item.extension || 'UNKNOWN',
      sizeLabel: formatBytes(item.sizeBytes),
      versionNo: item.versionNo || 'V1',
      statusLabel: normalizedLightweightStatusLabel(item),
      actionHint: item.actionHint || item.unsupportedReason || '可在文件管理中查看模型轻量化状态。',
      lightweightStatus: item.lightweightStatus,
      viewerAvailable: item.viewerAvailable,
      supported: item.supported,
      fileManagerUrl: `/data-steward/assets/${projectId.value}?tab=files&fileKeyword=${encodeURIComponent(item.fileName)}&lastFileId=${item.fileId}`
    })),
    readyModels
  };
});

const kpiCards = computed<BimMetric[]>(() => {
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
watch([bimData, themeMode, embeddedPreviewModels, lightweightSummary, kpiCards], () => renderBimWindow());

onMounted(() => renderBimWindow());
onBeforeUnmount(() => {
  bimRoot?.unmount();
  bimRoot = null;
});

async function loadPage() {
  const targetProjectId = projectId.value;
  if (!targetProjectId) {
    dashboard.value = null;
    renderBimWindow();
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    if (!(await ensureProjectContext(targetProjectId))) {
      dashboard.value = null;
      modelFiles.value = [];
      renderBimWindow();
      return;
    }
    dashboard.value = await fetchDigitalTwinDashboard(targetProjectId);
    await Promise.all([
      loadModelFiles(),
      loadMasterDataContext(targetProjectId)
    ]);
  } catch (error) {
    dashboard.value = null;
    standardStatus.value = null;
    sectionTree.value = [];
    ownershipCoverage.value = null;
    ownershipTree.value = null;
    errorMessage.value = error instanceof Error ? error.message : 'BIM 协同窗口数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function ensureProjectContext(targetProjectId: number) {
  if (authStore.currentProjectId === targetProjectId) return true;
  try {
    await authStore.changeProject(targetProjectId);
    return true;
  } catch {
    errorMessage.value = '当前账号无法切换到该项目，请确认项目授权后重试。';
    return false;
  }
}

async function loadModelFiles() {
  if (!projectId.value) {
    modelFiles.value = [];
    return;
  }
  try {
    modelFiles.value = await fetchGlandarModelFiles(projectId.value);
    renderBimWindow();
  } catch {
    modelFiles.value = [];
    renderBimWindow();
  }
}

async function loadMasterDataContext(targetProjectId: number) {
  const [standardResult, sectionResult, coverageResult, treeResult] = await Promise.allSettled([
    fetchStandardStatus(targetProjectId),
    fetchSectionTree(targetProjectId),
    fetchFileOwnershipCoverage(targetProjectId),
    fetchFileOwnershipTree(targetProjectId)
  ]);
  standardStatus.value = standardResult.status === 'fulfilled' ? standardResult.value : null;
  sectionTree.value = sectionResult.status === 'fulfilled' ? sectionResult.value : [];
  ownershipCoverage.value = coverageResult.status === 'fulfilled' ? coverageResult.value : null;
  ownershipTree.value = treeResult.status === 'fulfilled' ? treeResult.value : null;
}

function renderBimWindow() {
  void nextTick(() => {
    if (!bimHostRef.value) return;
    if (!bimRoot) {
      bimRoot = createRoot(bimHostRef.value);
    }
    const data = bimData.value;
    bimRoot.render(data ? createElement(BimCollaborationIsland, {
      data,
      embeddedPreviewModels: embeddedPreviewModels.value,
      lightweightSummary: lightweightSummary.value,
      heroMetrics: kpiCards.value,
      standardStatus: standardStatus.value,
      sectionTree: sectionTree.value,
      ownershipCoverage: ownershipCoverage.value,
      ownershipTree: ownershipTree.value,
      theme: themeMode.value
    }) : null);
  });
}

function persistTheme() {
  localStorage.setItem(THEME_STORAGE_KEY, themeMode.value);
  renderBimWindow();
}

function toggleTheme() {
  darkThemeEnabled.value = !darkThemeEnabled.value;
  persistTheme();
}

function scoreModelCompleteness(item: GlandarModelFile) {
  const name = item.fileName || '';
  let score = item.sizeBytes ?? 0;
  if (name.includes('全楼层')) score += 10_000_000_000;
  if (name.includes('全楼') || name.includes('全栋')) score += 8_000_000_000;
  if (/\d+\s*栋/.test(name)) score += 2_000_000_000;
  if (item.viewerAvailable && item.latestJobId) score += 1_000_000_000;
  return score;
}

function lightweightSortRank(item: GlandarModelFile) {
  const status = item.taskStatus || item.lightweightStatus;
  if (item.viewerAvailable && item.latestJobId) return 0;
  if (status === 'READY') return 0;
  if (status === 'RUNNING' || status === 'SUBMITTED' || status === 'UPLOADED') return 1;
  if (status === 'FAILED') return 2;
  if (!item.supported || status === 'UNSUPPORTED') return 4;
  return 3;
}

function normalizedLightweightStatusLabel(item: GlandarModelFile) {
  const status = item.taskStatus || item.lightweightStatus;
  if (item.viewerAvailable && item.latestJobId) return '已轻量化';
  if (!item.supported || status === 'UNSUPPORTED') return '暂不支持';
  return lightweightStatusLabel(status);
}

function lightweightStatusLabel(value: string | null | undefined) {
  if (value === 'READY') return '已轻量化';
  if (value === 'RUNNING') return '处理中';
  if (value === 'FAILED') return '轻量化失败';
  if (value === 'UNSUPPORTED') return '暂不支持';
  return '未轻量化';
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

.bim-collab-hero__title span {
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

.bim-collab-window-card {
  background: var(--bim-panel);
  border: 1px solid var(--bim-line);
  border-radius: var(--zy-radius-base);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.bim-collab-window-card__header span {
  color: var(--bim-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
}

.bim-collab-inline-viewer {
  border: 1px solid var(--bim-line);
  border-radius: 12px;
  display: grid;
  gap: var(--zy-sp-3);
  min-width: 0;
  padding: var(--zy-sp-3);
}

.bim-collab-inline-viewer__head {
  align-items: flex-start;
  display: flex;
  gap: var(--zy-sp-3);
  justify-content: space-between;
  min-width: 0;
}

.bim-collab-inline-viewer__head > div:first-child {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.bim-collab-inline-viewer__head span {
  color: var(--bim-accent);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

.bim-collab-inline-viewer__head h3 {
  color: var(--bim-text);
  font-size: var(--zy-fs-lg);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bim-collab-inline-viewer__head p {
  color: var(--bim-muted);
  font-size: var(--zy-fs-xs);
  margin: 0;
}

.bim-collab-inline-viewer__head > div:last-child {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  justify-content: flex-end;
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

.bim-collab-window-card__header strong {
  color: var(--bim-text);
  font-weight: var(--zy-fw-semi);
}

.bim-collab-react-host {
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

@media (max-width: 1280px) {
  .bim-collab-window-card {
    min-height: 960px;
  }
}

@media (max-width: 860px) {
  .bim-collab-hero,
  .bim-collab-window-card__header {
    display: grid;
  }

  .bim-collab-hero__actions {
    justify-content: flex-start;
  }

}
</style>
