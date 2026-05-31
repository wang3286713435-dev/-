<template>
  <section class="glandar-preview-page" :class="{ 'is-embedded': embeddedMode }">
    <header v-if="!embeddedMode" class="glandar-preview-page__head">
      <div>
        <span>GLANDAR VIEWER · BIM LIGHTWEIGHT</span>
        <h1>{{ fileName || '葛兰岱尔模型预览' }}</h1>
        <p>{{ statusText }}</p>
      </div>
      <div class="glandar-preview-page__actions">
        <el-tag :type="viewerReady ? 'success' : 'warning'" effect="plain">
          {{ viewerReady ? '已轻量化' : '等待模型' }}
        </el-tag>
        <el-button @click="refreshViewer">刷新 Viewer</el-button>
        <el-button @click="goBack">返回</el-button>
      </div>
    </header>

    <el-alert
      v-if="contextError"
      :closable="false"
      type="error"
      show-icon
      :title="contextError"
    />

    <GlandarViewerCanvas
      v-else-if="contextReady"
      ref="viewerCanvasRef"
      :key="`${projectId}-${jobId}`"
      :project-id="projectId"
      :job-id="jobId"
      :file-name="fileName"
      :model-file-id="modelFileId"
      :embedded="embeddedMode"
      :auto-rotate="autoRotate"
      :theme="themeMode"
      :show-info="!embeddedMode"
      @ready-change="viewerReady = $event"
      @ticket-change="ticket = $event"
    />

    <el-skeleton v-else :rows="8" animated />
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import GlandarViewerCanvas from '@/modules/visualization/components/GlandarViewerCanvas.vue';
import type { LightweightViewerTicketResponse } from '@/modules/visualization/api/visualization';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const viewerReady = ref(false);
const ticket = ref<LightweightViewerTicketResponse | null>(null);
const viewerCanvasRef = ref<InstanceType<typeof GlandarViewerCanvas> | null>(null);
const contextReady = ref(false);
const contextError = ref('');

const projectId = computed(() => Number(route.query.projectId));
const jobId = computed(() => String(route.query.jobId || ''));
const fileName = computed(() => String(route.query.fileName || ''));
const modelFileId = computed(() => String(route.query.modelFileId || ''));
const embeddedMode = computed(() => route.query.embedded === '1' || route.query.embedded === 'true');
const autoRotate = computed(() => route.query.autoRotate === '1' || route.query.autoRotate === 'true');
const themeMode = computed(() => (route.query.theme === 'light' ? 'light' : 'dark'));
const statusText = computed(() => {
  if (viewerReady.value) return '模型已在平台内通过葛兰岱尔 Viewer 加载。';
  if (ticket.value?.blockedReason) return ticket.value.blockedReason;
  return '平台只读取轻量化产物入口，不暴露真实 NAS 路径或引擎 token。';
});

watch(projectId, () => {
  void ensureProjectContext();
}, { immediate: true });

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push({ name: 'bim-collaboration', query: Number.isFinite(projectId.value) ? { projectId: projectId.value } : undefined });
}

function refreshViewer() {
  void viewerCanvasRef.value?.loadViewer();
}

async function ensureProjectContext() {
  contextReady.value = false;
  contextError.value = '';
  const targetProjectId = projectId.value;
  if (!Number.isFinite(targetProjectId) || targetProjectId <= 0) {
    contextError.value = '缺少有效项目上下文，无法打开模型预览。';
    return;
  }
  try {
    if (authStore.currentProjectId !== targetProjectId) {
      await authStore.changeProject(targetProjectId);
    }
    contextReady.value = true;
  } catch {
    contextError.value = '当前账号无法切换到该项目，请确认项目授权后重试。';
  }
}
</script>

<style scoped>
.glandar-preview-page {
  display: grid;
  gap: var(--zy-sp-4);
  min-width: 0;
}

.glandar-preview-page.is-embedded {
  background: transparent;
  display: block;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  width: 100%;
}

.glandar-preview-page.is-embedded :deep(.glandar-viewer) {
  height: 100vh;
  min-height: 100vh;
}

.glandar-preview-page.is-embedded :deep(.glandar-viewer__canvas-card) {
  height: 100vh;
  min-height: 100vh;
}

.glandar-preview-page__head {
  align-items: flex-start;
  background: var(--zy-surface);
  border: 1px solid color-mix(in srgb, var(--zy-line) 72%, transparent);
  border-radius: var(--zy-radius-base);
  box-shadow: var(--zy-shadow-xs);
  display: flex;
  gap: var(--zy-sp-4);
  justify-content: space-between;
  padding: var(--zy-sp-4);
}

.glandar-preview-page__head span {
  color: var(--zy-blue-600);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

.glandar-preview-page__head h1 {
  color: var(--zy-ink);
  font-size: var(--zy-fs-2xl);
  margin: 4px 0;
}

.glandar-preview-page__head p {
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
  font-style: normal;
}

.glandar-preview-page__actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  justify-content: flex-end;
}
</style>
