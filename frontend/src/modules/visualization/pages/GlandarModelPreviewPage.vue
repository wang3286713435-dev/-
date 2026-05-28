<template>
  <section class="glandar-preview-page">
    <header class="glandar-preview-page__head">
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

    <GlandarViewerCanvas
      ref="viewerCanvasRef"
      :key="`${projectId}-${jobId}`"
      :project-id="projectId"
      :job-id="jobId"
      :file-name="fileName"
      @ready-change="viewerReady = $event"
      @ticket-change="ticket = $event"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import GlandarViewerCanvas from '@/modules/visualization/components/GlandarViewerCanvas.vue';
import type { LightweightViewerTicketResponse } from '@/modules/visualization/api/visualization';

const route = useRoute();
const router = useRouter();
const viewerReady = ref(false);
const ticket = ref<LightweightViewerTicketResponse | null>(null);
const viewerCanvasRef = ref<InstanceType<typeof GlandarViewerCanvas> | null>(null);

const projectId = computed(() => Number(route.query.projectId));
const jobId = computed(() => String(route.query.jobId || ''));
const fileName = computed(() => String(route.query.fileName || ''));
const statusText = computed(() => {
  if (viewerReady.value) return '模型已在平台内通过葛兰岱尔 Viewer 加载。';
  if (ticket.value?.blockedReason) return ticket.value.blockedReason;
  return '平台只读取轻量化产物入口，不暴露真实 NAS 路径或引擎 token。';
});

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.push({ name: 'bim-collaboration' });
}

function refreshViewer() {
  void viewerCanvasRef.value?.loadViewer();
}
</script>

<style scoped>
.glandar-preview-page {
  display: grid;
  gap: var(--zy-sp-4);
  min-width: 0;
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
