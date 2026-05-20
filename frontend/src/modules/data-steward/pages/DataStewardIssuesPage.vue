<template>
  <section class="mvp-page issue-page">
    <div class="mvp-page__header">
      <div>
        <h1>事项列表</h1>
        <p>{{ projectLabel }}，把质量缺口和失败任务整理成可处理事项。</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </div>

    <section class="issue-summary-grid">
      <article v-for="item in summaryCards" :key="item.label" class="issue-summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.helper }}</em>
      </article>
    </section>

    <el-table v-loading="loading" :data="issueRows" class="master-table" empty-text="暂无治理事项">
      <el-table-column prop="typeLabel" label="事项类型" width="150" />
      <el-table-column prop="severity" label="严重程度" width="110">
        <template #default="{ row }">
          <el-tag :type="severityTag(row.severity)">{{ severityLabel(row.severity) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetLabel" label="关联对象" min-width="220" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="suggestion" label="建议动作" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openIssue(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import {
  fetchAssetQualityOverview,
  fetchAssetScanTasks,
  fetchFileAssetsPage,
  type AssetQualityOverview,
  type AssetScanTask,
  type FileAsset
} from '@/modules/data-steward/api/dataSteward';
import { useAuthStore } from '@/stores/auth';

interface IssueRow {
  key: string;
  type: string;
  typeLabel: string;
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
  targetLabel: string;
  status: string;
  suggestion: string;
  qualityIssue?: string;
  fileId?: number;
  taskId?: number;
}

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const quality = ref<AssetQualityOverview | null>(null);
const sampleFiles = ref<Record<string, FileAsset[]>>({});
const failedScans = ref<AssetScanTask[]>([]);

const qualityTypes = [
  { code: 'MISSING_CHECKSUM', label: '缺 checksum', severity: 'MEDIUM' as const, suggestion: '进入文件管理创建 checksum 补算任务' },
  { code: 'MISSING_DISCIPLINE', label: '缺专业', severity: 'MEDIUM' as const, suggestion: '进入人工治理补齐专业字段' },
  { code: 'MISSING_CONFIDENCE', label: '低置信/缺置信', severity: 'LOW' as const, suggestion: '进入人工治理确认文件归属和类型' },
  { code: 'MISSING_STORAGE_PATH', label: '路径缺失', severity: 'HIGH' as const, suggestion: '检查路径映射或重新扫描' }
];

const projectId = computed(() => {
  const routeId = Number(route.params.projectId);
  return Number.isFinite(routeId) && routeId > 0 ? routeId : authStore.currentProjectId;
});
const projectLabel = computed(() => {
  const current = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return current ? `${current.code} ${current.name}` : '等待项目上下文';
});
const issueRows = computed<IssueRow[]>(() => {
  const rows: IssueRow[] = [];
  for (const type of qualityTypes) {
    const files = sampleFiles.value[type.code] ?? [];
    for (const file of files.slice(0, 5)) {
      rows.push({
        key: `${type.code}-${file.fileId}`,
        type: type.code,
        typeLabel: type.label,
        severity: type.severity,
        targetLabel: `${file.fileName} / 文件ID ${file.fileId}`,
        status: file.reviewStatus || file.processStatus || '待处理',
        suggestion: type.suggestion,
        qualityIssue: type.code,
        fileId: file.fileId
      });
    }
  }
  for (const task of failedScans.value.slice(0, 10)) {
    rows.push({
      key: `FAILED_SCAN-${task.id}`,
      type: 'FAILED_SCAN',
      typeLabel: '扫描失败',
      severity: 'HIGH',
      targetLabel: `扫描任务 ${task.id} / ${task.rootCode}`,
      status: task.status,
      suggestion: task.failureReason || '进入任务列表查看失败原因',
      taskId: task.id
    });
  }
  if ((quality.value?.pendingReviewCount ?? 0) > 0) {
    rows.push({
      key: 'PENDING_REVIEW',
      type: 'PENDING_REVIEW',
      typeLabel: '待审核',
      severity: 'MEDIUM',
      targetLabel: `${quality.value?.pendingReviewCount ?? 0} 条扫描候选`,
      status: '待人工确认',
      suggestion: '进入扫描任务或治理入口处理待审核候选'
    });
  }
  return rows;
});
const summaryCards = computed(() => [
  { label: '风险信号', value: Number(quality.value?.riskSignalCount ?? 0).toLocaleString('zh-CN'), helper: '项目内质量问题' },
  { label: '待审核', value: Number(quality.value?.pendingReviewCount ?? 0).toLocaleString('zh-CN'), helper: '扫描候选' },
  { label: '扫描失败', value: failedScans.value.length.toLocaleString('zh-CN'), helper: '需要排查' },
  { label: '事项样本', value: issueRows.value.length.toLocaleString('zh-CN'), helper: '当前页面展示' }
]);

watch(projectId, () => loadPage(), { immediate: true });

async function loadPage() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [nextQuality, scans, ...filePages] = await Promise.all([
      fetchAssetQualityOverview(projectId.value),
      fetchAssetScanTasks(),
      ...qualityTypes.map((item) =>
        fetchFileAssetsPage({ projectId: projectId.value ?? undefined, qualityIssue: item.code, pageNo: 1, pageSize: 5 })
      )
    ]);
    quality.value = nextQuality;
    failedScans.value = scans.filter((item) => item.projectId === projectId.value && item.status === 'FAILED');
    sampleFiles.value = Object.fromEntries(qualityTypes.map((item, index) => [item.code, filePages[index].rows]));
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '事项列表加载失败');
  } finally {
    loading.value = false;
  }
}

function openIssue(row: IssueRow) {
  if (!projectId.value) return;
  if (row.fileId || row.qualityIssue) {
    void router.push({
      name: 'data-steward-asset-detail',
      params: { projectId: projectId.value },
      query: row.qualityIssue ? { qualityIssue: row.qualityIssue } : undefined
    });
    return;
  }
  void router.push({ name: 'project-data-steward-tasks', params: { projectId: projectId.value } });
}

function severityTag(value: IssueRow['severity']) {
  if (value === 'HIGH') return 'danger';
  if (value === 'MEDIUM') return 'warning';
  return 'info';
}

function severityLabel(value: IssueRow['severity']) {
  const labels = { HIGH: '高', MEDIUM: '中', LOW: '低' };
  return labels[value];
}
</script>

<style scoped>
.issue-page {
  min-width: 0;
}

.issue-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.issue-summary-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #ffffff;
}

.issue-summary-card span,
.issue-summary-card em {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.issue-summary-card strong {
  display: block;
  margin: 6px 0 4px;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.15;
}
</style>
