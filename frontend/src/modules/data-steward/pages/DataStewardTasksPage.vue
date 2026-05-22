<template>
  <section class="mvp-page task-page">
    <div class="mvp-page__header">
      <div>
        <h1>任务列表</h1>
        <p>{{ projectLabel }}，追踪扫描、checksum 和后台治理任务。</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </div>

    <section class="task-toolbar">
      <el-select v-model="filters.type" clearable placeholder="任务类型">
        <el-option label="全部任务" value="" />
        <el-option label="扫描任务" value="SCAN" />
        <el-option label="后台任务" value="JOB" />
        <el-option label="checksum" value="CHECKSUM_CALC" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态">
        <el-option label="全部状态" value="" />
        <el-option label="等待执行" value="PENDING" />
        <el-option label="执行中" value="RUNNING" />
        <el-option label="成功" value="SUCCEEDED" />
        <el-option label="失败" value="FAILED" />
        <el-option label="取消" value="CANCELED" />
      </el-select>
    </section>

    <el-table v-loading="loading" :data="filteredRows" class="master-table" empty-text="暂无任务">
      <el-table-column prop="id" label="任务ID" width="90" />
      <el-table-column prop="typeLabel" label="类型" width="130" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="170">
        <template #default="{ row }">
          <el-progress :percentage="progressValue(row)" :stroke-width="8" />
        </template>
      </el-table-column>
      <el-table-column prop="message" label="进度说明" min-width="220" show-overflow-tooltip />
      <el-table-column prop="failureReason" label="失败原因" min-width="220" show-overflow-tooltip />
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.source === 'JOB' && row.status === 'FAILED'"
            text
            type="primary"
            :loading="retryingId === row.id"
            @click="retryJob(row.id)"
          >
            重试
          </el-button>
          <span v-else class="muted-text">只读</span>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import {
  fetchAssetJobs,
  fetchAssetScanTasks,
  retryAssetJob,
  type AssetJob,
  type AssetScanTask
} from '@/modules/data-steward/api/dataSteward';
import { useAuthStore } from '@/stores/auth';

interface TaskRow {
  id: number;
  source: 'SCAN' | 'JOB';
  type: string;
  typeLabel: string;
  status: string;
  progressPercent: number | null;
  progressCurrent: number | null;
  progressTotal: number | null;
  message: string;
  failureReason: string;
  updatedAt: string | null;
}

const route = useRoute();
const authStore = useAuthStore();
const loading = ref(false);
const retryingId = ref<number | null>(null);
const jobs = ref<AssetJob[]>([]);
const scans = ref<AssetScanTask[]>([]);
const filters = reactive({
  type: '',
  status: ''
});

const projectId = computed(() => {
  const routeId = Number(route.params.projectId);
  return Number.isFinite(routeId) && routeId > 0 ? routeId : authStore.currentProjectId;
});
const projectLabel = computed(() => {
  const current = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return current ? `${current.code} ${current.name}` : '等待项目上下文';
});
const rows = computed<TaskRow[]>(() => [
  ...scans.value.map((item) => ({
    id: item.id,
    source: 'SCAN' as const,
    type: 'SCAN',
    typeLabel: 'NAS 扫描',
    status: item.status,
    progressPercent: item.progressPercent,
    progressCurrent: item.progressCurrent,
    progressTotal: item.progressTotal,
    message: safeTaskText(item.progressMessage || item.lastScannedPath || '-'),
    failureReason: safeTaskText(item.failureReason || '-'),
    updatedAt: item.updatedAt
  })),
  ...jobs.value.map((item) => ({
    id: item.id,
    source: 'JOB' as const,
    type: item.jobType,
    typeLabel: jobTypeLabel(item.jobType),
    status: item.status,
    progressPercent: item.progressPercent,
    progressCurrent: item.progressCurrent,
    progressTotal: item.progressTotal,
    message: safeTaskText(item.progressMessage || '-'),
    failureReason: safeTaskText(item.failureReason || '-'),
    updatedAt: item.updatedAt
  }))
]);
const filteredRows = computed(() => rows.value.filter((item) => {
  if (filters.type && item.source !== filters.type && item.type !== filters.type) return false;
  if (filters.status && item.status !== filters.status) return false;
  return true;
}));

watch(projectId, () => loadPage(), { immediate: true });

async function loadPage() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [nextJobs, nextScans] = await Promise.all([
      fetchAssetJobs({ projectId: projectId.value, limit: 100 }),
      fetchAssetScanTasks()
    ]);
    jobs.value = nextJobs;
    scans.value = nextScans.filter((item) => item.projectId === projectId.value);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务列表加载失败');
  } finally {
    loading.value = false;
  }
}

async function retryJob(jobId: number) {
  retryingId.value = jobId;
  try {
    await retryAssetJob(jobId);
    ElMessage.success('任务已重新提交');
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务重试失败');
  } finally {
    retryingId.value = null;
  }
}

function progressValue(row: TaskRow) {
  if (row.status === 'SUCCEEDED') return 100;
  const next = Number(row.progressPercent ?? 0);
  if (!Number.isFinite(next)) return 0;
  return Math.max(0, Math.min(100, Math.round(next)));
}

function jobTypeLabel(value: string) {
  const labels: Record<string, string> = {
    NAS_SCAN: 'NAS 扫描',
    CHECKSUM_CALC: 'checksum',
    QUARANTINE_CLEANUP: '回收站清理',
    PERMANENT_DELETE: '受控删除'
  };
  return labels[value] ?? value;
}

function statusTag(value: string) {
  if (value === 'SUCCEEDED') return 'success';
  if (value === 'FAILED') return 'danger';
  if (value === 'RUNNING') return 'warning';
  return 'info';
}

function statusLabel(value: string) {
  const labels: Record<string, string> = {
    PENDING: '等待执行',
    RUNNING: '执行中',
    SUCCEEDED: '成功',
    FAILED: '失败',
    CANCELED: '取消'
  };
  return labels[value] ?? value;
}

function safeTaskText(value: string) {
  return value
    .replace(/nas:\/\/\/?[^\s，,;；。)）]+/g, '[受控存储路径]')
    .replace(/\/Volumes\/[^\s，,;；。)）]+/g, '[受控存储路径]');
}

function formatDate(value: string | null | undefined) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}
</script>

<style scoped>
.task-page {
  min-width: 0;
}

.task-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.task-toolbar .el-select {
  width: 180px;
}

.muted-text {
  color: var(--zy-subtle);
  font-size: 12px;
}
</style>
