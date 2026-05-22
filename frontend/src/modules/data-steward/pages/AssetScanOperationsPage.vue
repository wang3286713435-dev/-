<template>
  <section class="mvp-page scan-page">
    <div class="mvp-page__header">
      <div>
        <h1>扫描任务</h1>
        <p>NAS 资产入库任务运维</p>
      </div>
      <div class="mvp-page__actions">
        <el-select v-model="filters.projectId" clearable filterable placeholder="项目" @change="applyFilters">
          <el-option
            v-for="item in projects"
            :key="item.projectId"
            :label="`${item.code} | ${item.name}`"
            :value="item.projectId"
          />
        </el-select>
        <el-segmented v-model="filters.status" :options="statusOptions" @change="applyFilters" />
        <el-checkbox v-model="filters.hasPendingReview" @change="applyFilters">仅待审核</el-checkbox>
        <el-input
          v-model="filters.keyword"
          class="scan-search"
          clearable
          placeholder="任务编码、项目、路径"
          :prefix-icon="Search"
          @keyup.enter="applyFilters"
          @clear="applyFilters"
        />
        <el-button :icon="Plus" type="primary" @click="openCreateDialog">新建任务</el-button>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </div>

    <section class="mvp-dashboard">
      <article v-for="item in cards" :key="item.label" class="mvp-stat mvp-stat--large">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.unit }}</em>
      </article>
    </section>

    <el-table v-loading="loading" :data="filteredTasks" class="master-table" empty-text="暂无扫描任务">
      <el-table-column prop="id" label="任务ID" width="90" />
      <el-table-column label="项目" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="scan-project-cell">
            <strong>{{ scanProjectLabel(row) }}</strong>
            <span v-if="scanProjectHint(row)">{{ scanProjectHint(row) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="rootCode" label="根编码" width="130" show-overflow-tooltip />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="scanStatusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" width="190">
        <template #default="{ row }">
          <el-progress :percentage="progressValue(row)" :stroke-width="8" />
        </template>
      </el-table-column>
      <el-table-column label="扫描/入库/待审/失败" width="190">
        <template #default="{ row }">
          {{ formatCount(row.totalScanned) }} / {{ formatCount(row.autoIngested) }} /
          {{ formatCount(row.pendingReview) }} / {{ formatCount(row.failedCount) }}
        </template>
      </el-table-column>
      <el-table-column label="跳过" width="130">
        <template #default="{ row }">
          {{ formatCount(row.skippedDirectories) }} 目录
        </template>
      </el-table-column>
      <el-table-column prop="progressMessage" label="当前信息" min-width="180" show-overflow-tooltip />
      <el-table-column prop="failureReason" label="失败原因" min-width="180" show-overflow-tooltip />
      <el-table-column prop="rootPath" label="扫描路径" min-width="320" show-overflow-tooltip />
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="canRun(row)"
            text
            :icon="VideoPlay"
            :loading="actionTaskId === row.id"
            @click="runTask(row)"
          >
            运行
          </el-button>
          <el-button
            v-if="canCancel(row)"
            text
            :icon="CircleClose"
            :loading="actionTaskId === row.id"
            @click="cancelTask(row)"
          >
            取消
          </el-button>
          <el-button
            v-if="canResume(row)"
            text
            :icon="RefreshRight"
            :loading="actionTaskId === row.id"
            @click="resumeTask(row)"
          >
            续扫
          </el-button>
          <el-button text :icon="Document" @click="openReport(row)">报告</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createDialogVisible" title="新建扫描任务" width="680px">
      <el-form label-width="110px" :model="createForm">
        <el-form-item label="所属项目">
          <el-select v-model="createForm.projectId" clearable filterable placeholder="可选">
            <el-option
              v-for="item in projects"
              :key="item.projectId"
              :label="`${item.code} | ${item.name}`"
              :value="item.projectId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="根编码" required>
          <el-input v-model="createForm.rootCode" />
        </el-form-item>
        <el-form-item label="NAS 路径" required>
          <el-input v-model="createForm.rootPath" />
        </el-form-item>
        <el-form-item label="扩展名">
          <el-input v-model="createForm.extensionsText" />
        </el-form-item>
        <el-form-item label="跳过目录">
          <el-input v-model="createForm.skipKeywordsText" />
        </el-form-item>
        <el-form-item label="扫描选项">
          <el-checkbox v-model="createForm.recursive">递归</el-checkbox>
          <el-checkbox v-model="createForm.skipLowValueDirectories">跳过低价值目录</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createTask">创建</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="reportVisible" title="扫描报告" size="640px">
      <section v-if="selectedReport" class="scan-report">
        <div class="scan-report__summary">
          <article>
            <span>状态</span>
            <strong>{{ statusLabel(selectedReport.status) }}</strong>
          </article>
          <article>
            <span>扫描</span>
            <strong>{{ formatCount(selectedReport.totalScanned) }}</strong>
          </article>
          <article>
            <span>入库</span>
            <strong>{{ formatCount(selectedReport.autoIngested) }}</strong>
          </article>
          <article>
            <span>待审</span>
            <strong>{{ formatCount(selectedReport.pendingReview) }}</strong>
          </article>
          <article>
            <span>失败</span>
            <strong>{{ formatCount(selectedReport.failedCount) }}</strong>
          </article>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="根编码">{{ selectedReport.rootCode }}</el-descriptions-item>
          <el-descriptions-item label="扫描路径">{{ selectedReport.rootPath }}</el-descriptions-item>
          <el-descriptions-item label="最后路径">{{ selectedReport.lastScannedPath || '-' }}</el-descriptions-item>
          <el-descriptions-item label="失败原因">{{ selectedReport.failureReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatDate(selectedReport.startedAt) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatDate(selectedReport.completedAt) }}</el-descriptions-item>
        </el-descriptions>

        <pre class="scan-report__json">{{ formattedReportJson }}</pre>
      </section>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  CircleClose,
  Document,
  Plus,
  Refresh,
  RefreshRight,
  Search,
  VideoPlay
} from '@element-plus/icons-vue';

import {
  cancelAssetScan,
  createAssetScan,
  fetchAssetProjects,
  fetchAssetScanReport,
  fetchAssetScanTasks,
  resumeAssetScan,
  runAssetScan,
  type AssetProject,
  type AssetScanReport,
  type AssetScanTask
} from '@/modules/data-steward/api/dataSteward';

const route = useRoute();
const loading = ref(false);
const creating = ref(false);
const actionTaskId = ref<number | null>(null);
const reportVisible = ref(false);
const createDialogVisible = ref(false);
const selectedReport = ref<AssetScanReport | null>(null);
const projects = ref<AssetProject[]>([]);
const tasks = ref<AssetScanTask[]>([]);
const filters = reactive({
  projectId: undefined as number | undefined,
  status: queryString(route.query.status) ?? 'ALL',
  keyword: queryString(route.query.keyword) ?? '',
  hasPendingReview: queryString(route.query.hasPendingReview) === 'true'
});
const createForm = reactive({
  projectId: undefined as number | undefined,
  rootCode: 'NAS_REAL',
  rootPath: '',
  extensionsText: '.rvt,.dwg,.ifc,.nwd,.nwc,.dxf,.pdf',
  skipKeywordsText: 'Backup,Temp,转换,导出,缓存',
  recursive: true,
  skipLowValueDirectories: true
});

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '待执行', value: 'PENDING' },
  { label: '运行中', value: 'RUNNING' },
  { label: '成功', value: 'SUCCEEDED' },
  { label: '失败', value: 'FAILED' },
  { label: '已取消', value: 'CANCELED' }
];

const cards = computed(() => {
  const list = filteredTasks.value;
  return [
    { label: '任务总数', value: formatCount(list.length), unit: '条' },
    { label: '运行中', value: formatCount(countStatus(list, 'RUNNING')), unit: '条' },
    { label: '待执行', value: formatCount(countStatus(list, 'PENDING')), unit: '条' },
    { label: '待审核', value: formatCount(sumField(list, 'pendingReview')), unit: '份' },
    { label: '失败任务', value: formatCount(countStatus(list, 'FAILED')), unit: '条' },
    { label: '最近更新', value: formatDate(latestUpdatedAt(list)), unit: '时间' }
  ];
});

const filteredTasks = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase();
  return tasks.value.filter((item) => {
    if (filters.projectId && item.projectId !== filters.projectId) return false;
    if (filters.status !== 'ALL' && item.status !== filters.status) return false;
    if (filters.hasPendingReview && Number(item.pendingReview ?? 0) <= 0) return false;
    if (!keyword) return true;
    return [item.rootCode, item.rootPath, item.projectCode, projectName(item.projectId), scanProjectLabel(item), item.progressMessage]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword));
  });
});

const formattedReportJson = computed(() => {
  const raw = selectedReport.value?.scanReportJson;
  if (!raw) return '{}';
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
});

loadPage();

async function loadPage() {
  loading.value = true;
  try {
    const [nextProjects, nextTasks] = await Promise.all([fetchAssetProjects(), fetchAssetScanTasks()]);
    projects.value = nextProjects;
    tasks.value = nextTasks;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '扫描任务加载失败');
  } finally {
    loading.value = false;
  }
}

function applyFilters() {
  return undefined;
}

function openCreateDialog() {
  createDialogVisible.value = true;
}

async function createTask() {
  if (!createForm.rootCode.trim() || !createForm.rootPath.trim()) {
    ElMessage.warning('请填写根编码和 NAS 路径');
    return;
  }
  creating.value = true;
  try {
    const project = projects.value.find((item) => item.projectId === createForm.projectId);
    const task = await createAssetScan({
      rootCode: createForm.rootCode.trim(),
      rootPath: createForm.rootPath.trim(),
      projectId: createForm.projectId,
      projectCode: project?.code,
      recursive: createForm.recursive,
      extensions: splitList(createForm.extensionsText),
      skipLowValueDirectories: createForm.skipLowValueDirectories,
      skipDirectoryKeywords: splitList(createForm.skipKeywordsText)
    });
    tasks.value = [task, ...tasks.value.filter((item) => item.id !== task.id)];
    createDialogVisible.value = false;
    ElMessage.success('扫描任务已创建');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '扫描任务创建失败');
  } finally {
    creating.value = false;
  }
}

async function runTask(task: AssetScanTask) {
  await confirmAndApply(task, '确认运行该扫描任务？', runAssetScan);
}

async function cancelTask(task: AssetScanTask) {
  await confirmAndApply(task, '确认取消该扫描任务？', cancelAssetScan);
}

async function resumeTask(task: AssetScanTask) {
  await confirmAndApply(task, '确认续扫该扫描任务？', resumeAssetScan);
}

async function confirmAndApply(
  task: AssetScanTask,
  message: string,
  action: (scanTaskId: number) => Promise<AssetScanTask>
) {
  try {
    await ElMessageBox.confirm(message, `任务 ${task.id}`, { type: 'warning' });
  } catch {
    return;
  }
  actionTaskId.value = task.id;
  try {
    const next = await action(task.id);
    upsertTask(next);
    ElMessage.success('操作完成');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  } finally {
    actionTaskId.value = null;
  }
}

async function openReport(task: AssetScanTask) {
  try {
    selectedReport.value = await fetchAssetScanReport(task.id);
    reportVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '扫描报告加载失败');
  }
}

function upsertTask(next: AssetScanTask) {
  tasks.value = tasks.value.map((item) => (item.id === next.id ? next : item));
}

function projectName(projectId: number | null) {
  if (!projectId) return '';
  const project = projects.value.find((item) => item.projectId === projectId);
  return project ? `${project.code} ${project.name}` : '';
}

function scanProjectLabel(task: AssetScanTask) {
  const matched = projectName(task.projectId);
  if (matched) return matched;
  if (task.projectCode) return task.projectCode;
  return '全局扫描';
}

function scanProjectHint(task: AssetScanTask) {
  if (task.projectId && !projectName(task.projectId)) return `项目ID ${task.projectId}`;
  if (!task.projectId && !task.projectCode) return '未绑定具体项目';
  return '';
}

function splitList(value: string) {
  return value
    .split(/[,\n，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function canRun(task: AssetScanTask) {
  return task.status === 'PENDING';
}

function canCancel(task: AssetScanTask) {
  return ['PENDING', 'RUNNING'].includes(task.status) && !task.cancelRequested;
}

function canResume(task: AssetScanTask) {
  return ['FAILED', 'CANCELED'].includes(task.status);
}

function scanStatusTag(value: string) {
  if (value === 'SUCCEEDED') return 'success';
  if (value === 'FAILED') return 'danger';
  if (value === 'RUNNING') return 'warning';
  if (value === 'CANCELED') return 'info';
  return 'primary';
}

function statusLabel(value: string) {
  const found = statusOptions.find((item) => item.value === value);
  return found?.label ?? value;
}

function progressValue(task: AssetScanTask) {
  if (task.status === 'SUCCEEDED') return 100;
  const next = Number(task.progressPercent ?? 0);
  if (!Number.isFinite(next)) return 0;
  return Math.min(100, Math.max(0, Math.round(next)));
}

function countStatus(list: AssetScanTask[], status: string) {
  return list.filter((item) => item.status === status).length;
}

function sumField(list: AssetScanTask[], field: 'pendingReview') {
  return list.reduce((sum, item) => sum + Number(item[field] ?? 0), 0);
}

function latestUpdatedAt(list: AssetScanTask[]) {
  return list
    .map((item) => item.updatedAt)
    .filter(Boolean)
    .sort()
    .at(-1);
}

function formatCount(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('zh-CN');
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

function queryString(value: unknown) {
  if (Array.isArray(value)) return value[0] ? String(value[0]) : undefined;
  return value ? String(value) : undefined;
}
</script>

<style scoped>
.scan-page {
  min-width: 0;
}

.scan-search {
  width: 280px;
}

.scan-project-cell {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.scan-project-cell strong,
.scan-project-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scan-project-cell span {
  color: var(--zy-muted);
  font-size: 12px;
}

.scan-report {
  display: grid;
  gap: 18px;
}

.scan-report__summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.scan-report__summary article {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  background: var(--zy-surface);
  min-width: 0;
}

.scan-report__summary span {
  display: block;
  color: var(--zy-muted);
  font-size: 12px;
  margin-bottom: 6px;
}

.scan-report__summary strong {
  font-size: 18px;
  color: var(--zy-ink);
}

.scan-report__json {
  margin: 0;
  padding: 14px;
  border-radius: 8px;
  background: var(--zy-ink);
  color: #e2e8f0;
  overflow: auto;
  max-height: 360px;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .scan-search {
    width: 100%;
  }

  .scan-report__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
