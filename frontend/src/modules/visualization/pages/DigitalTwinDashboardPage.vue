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

      <section class="bim-collab-pilot" aria-label="105 RVT 轻量化试点模型">
        <div class="bim-collab-pilot__head">
          <div>
            <span>GLANDAR RVT PILOT</span>
            <h2>105 项目 10 个 RVT 试点模型</h2>
            <p>已轻量化的模型可直接进入葛兰岱尔 Viewer；未完成的模型可提交转换或刷新状态。</p>
          </div>
          <div>
            <el-button :loading="pilotLoading" @click="loadPilotFiles">刷新试点状态</el-button>
            <el-button type="primary" :loading="pilotSubmitting" @click="submitPilotFiles">提交 10 个试点转换</el-button>
          </div>
        </div>
        <div class="bim-collab-pilot__grid">
          <article v-for="item in pilotFiles" :key="item.fileId" class="bim-collab-pilot__item">
            <div>
              <span>#{{ item.pilotRank }} / 文件 {{ item.fileId }}</span>
              <strong>{{ item.fileName }}</strong>
              <em>{{ formatBytes(item.sizeBytes) }} · {{ item.actionHint }}</em>
            </div>
            <div class="bim-collab-pilot__status">
              <el-tag :type="pilotStatusTag(item.taskStatus)" effect="plain">{{ item.statusLabel }}</el-tag>
              <el-progress :percentage="item.progressPercent ?? 0" :show-text="false" />
            </div>
            <div class="bim-collab-pilot__actions">
              <el-button size="small" :disabled="!item.latestJobId" @click="refreshPilotJob(item)">查状态</el-button>
              <el-button
                size="small"
                :type="activePilotFile?.fileId === item.fileId ? 'success' : 'default'"
                :disabled="!item.viewerAvailable || !item.latestJobId"
                @click="selectPilotForPreview(item)"
              >
                下方预览
              </el-button>
              <el-button
                size="small"
                type="primary"
                :disabled="!item.viewerAvailable || !item.latestJobId"
                @click="openPilotViewer(item)"
              >
                大窗口
              </el-button>
            </div>
          </article>
          <el-empty v-if="!pilotLoading && pilotFiles.length === 0" description="当前项目没有 RVT 试点模型" :image-size="64" />
        </div>

        <section class="bim-collab-inline-viewer" aria-label="葛兰岱尔真实模型预览">
          <div class="bim-collab-inline-viewer__head">
            <div>
              <span>MODEL PREVIEW · GLANDAR</span>
              <h3>{{ activePilotFile?.fileName || '选择一个已轻量化模型' }}</h3>
              <p>这里复用平台预留的 Viewer ticket 接口，模型在 BIM 协同管理页内直接展示。</p>
            </div>
            <div>
              <el-tag v-if="activePilotFile" type="success" effect="plain">已轻量化</el-tag>
              <el-button
                :disabled="!activePilotFile"
                @click="activePilotFile && openPilotViewer(activePilotFile)"
              >
                打开大窗口
              </el-button>
            </div>
          </div>
          <GlandarViewerCanvas
            v-if="projectId && activePilotFile?.latestJobId"
            :key="`${projectId}-${activePilotFile.latestJobId}`"
            embedded
            :project-id="projectId"
            :job-id="activePilotFile.latestJobId"
            :file-name="activePilotFile.fileName"
          />
          <el-empty
            v-else
            description="请选择上方已轻量化模型后，在这里直接预览。"
            :image-size="76"
          />
        </section>
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
import { ElMessage } from 'element-plus';
import { useRoute } from 'vue-router';
import { createElement } from 'react';
import { createRoot, type Root } from 'react-dom/client';

import BimCollaborationIsland from '@/modules/visualization/bim-collab/BimCollaborationIsland';
import { mapDashboardToBimCollab } from '@/modules/visualization/bim-collab/mapDashboardToBimCollab';
import type { BimThemeMode } from '@/modules/visualization/bim-collab/types';
import {
  fetchDigitalTwinDashboard,
  fetchGlandarRvtPilotFiles,
  fetchLightweightJob,
  issueLightweightViewerTicket,
  submitGlandarRvtPilotFiles,
  type GlandarRvtPilotFile,
  type DigitalTwinDashboard
} from '@/modules/visualization/api/visualization';
import GlandarViewerCanvas from '@/modules/visualization/components/GlandarViewerCanvas.vue';
import { useAuthStore } from '@/stores/auth';

const THEME_STORAGE_KEY = 'delivery:bim-collab-theme';

const authStore = useAuthStore();
const route = useRoute();
const loading = ref(false);
const dashboard = ref<DigitalTwinDashboard | null>(null);
const pilotFiles = ref<GlandarRvtPilotFile[]>([]);
const activePilotFileId = ref<number | null>(null);
const errorMessage = ref('');
const pilotLoading = ref(false);
const pilotSubmitting = ref(false);
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
const activePilotFile = computed(() => {
  const currentId = activePilotFileId.value;
  const selected = currentId ? pilotFiles.value.find((item) => item.fileId === currentId) : null;
  return selected || pilotFiles.value.find((item) => item.viewerAvailable && item.latestJobId) || null;
});

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
    await loadPilotFiles();
  } catch (error) {
    dashboard.value = null;
    errorMessage.value = error instanceof Error ? error.message : 'BIM 协同窗口数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadPilotFiles() {
  if (!projectId.value) {
    pilotFiles.value = [];
    return;
  }
  pilotLoading.value = true;
  try {
    pilotFiles.value = await fetchGlandarRvtPilotFiles(projectId.value);
    if (!activePilotFile.value) {
      activePilotFileId.value = pilotFiles.value.find((item) => item.viewerAvailable && item.latestJobId)?.fileId ?? null;
    }
  } catch {
    pilotFiles.value = [];
  } finally {
    pilotLoading.value = false;
  }
}

function selectPilotForPreview(item: GlandarRvtPilotFile) {
  if (!item.viewerAvailable || !item.latestJobId) return;
  activePilotFileId.value = item.fileId;
}

async function submitPilotFiles() {
  if (!projectId.value || pilotSubmitting.value) return;
  pilotSubmitting.value = true;
  try {
    pilotFiles.value = await submitGlandarRvtPilotFiles(projectId.value, false);
    ElMessage.success('已提交或复用 10 个 RVT 试点轻量化任务');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '试点轻量化任务提交失败');
  } finally {
    pilotSubmitting.value = false;
  }
}

async function refreshPilotJob(item: GlandarRvtPilotFile) {
  if (!projectId.value || !item.latestJobId) return;
  try {
    const job = await fetchLightweightJob(projectId.value, item.latestJobId);
    pilotFiles.value = pilotFiles.value.map((row) => row.fileId === item.fileId
      ? {
          ...row,
          taskStatus: job.taskStatus,
          progressPercent: job.progressPercent,
          viewerAvailable: job.viewerAvailable,
          statusLabel: job.statusLabel,
          blockedReason: job.blockedReason,
          updatedAt: job.updatedAt
        }
      : row);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '轻量化状态刷新失败');
  }
}

async function openPilotViewer(item: GlandarRvtPilotFile) {
  if (!projectId.value || !item.latestJobId) return;
  try {
    const ticket = await issueLightweightViewerTicket(projectId.value, item.latestJobId);
    if (!ticket.viewerAvailable || !ticket.ticketIssued) {
      ElMessage.warning(ticket.blockedReason || 'Viewer 暂不可用');
      return;
    }
    const href = `/visualization/glandar-viewer?projectId=${projectId.value}&jobId=${encodeURIComponent(item.latestJobId)}&fileName=${encodeURIComponent(item.fileName)}`;
    window.open(href, '_blank', 'noopener,noreferrer');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '打开 Viewer 失败');
  }
}

function pilotStatusTag(status: string | null | undefined) {
  if (status === 'READY') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'RUNNING' || status === 'UPLOADED' || status === 'SUBMITTED') return 'warning';
  return 'info';
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
.bim-collab-pilot,
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

.bim-collab-pilot {
  display: grid;
  gap: var(--zy-sp-3);
  min-width: 0;
  padding: var(--zy-sp-4);
}

.bim-collab-pilot__head {
  align-items: flex-start;
  display: flex;
  gap: var(--zy-sp-3);
  justify-content: space-between;
  min-width: 0;
}

.bim-collab-pilot__head > div:first-child {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.bim-collab-pilot__head span,
.bim-collab-pilot__item span {
  color: var(--bim-accent);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

.bim-collab-pilot__head h2 {
  color: var(--bim-text);
  font-size: var(--zy-fs-xl);
  margin: 0;
}

.bim-collab-pilot__head p,
.bim-collab-pilot__item em {
  color: var(--bim-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
  margin: 0;
}

.bim-collab-pilot__head > div:last-child {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  justify-content: flex-end;
}

.bim-collab-pilot__grid {
  display: grid;
  gap: var(--zy-sp-2);
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.bim-collab-pilot__item {
  border: 1px solid var(--bim-line);
  border-radius: 10px;
  display: grid;
  gap: var(--zy-sp-2);
  grid-template-columns: minmax(0, 1fr) 150px auto;
  min-width: 0;
  padding: var(--zy-sp-3);
}

.bim-collab-pilot__item > div:first-child {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.bim-collab-pilot__item strong {
  color: var(--bim-text);
  font-size: var(--zy-fs-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bim-collab-pilot__status,
.bim-collab-pilot__actions {
  align-content: center;
  display: grid;
  gap: 6px;
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

  .bim-collab-pilot__grid,
  .bim-collab-pilot__item {
    grid-template-columns: 1fr;
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
