<template>
  <section class="mvp-page file-service-page">
    <div class="mvp-page__header">
      <div>
        <h1>文件服务与对象存储</h1>
        <p>{{ projectLabel }}，集中管理受控文件访问和对象存储镜像任务。</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" :loading="loading" @click="refresh">刷新</el-button>
      </div>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="service-notice"
      title="当前只做对象存储镜像"
      description="NAS 原文件保留；任务中心不会生成语义证据，不代表 Hermes 已理解文件正文，也不会展示底层路径、bucket 或 object key。"
    />

    <el-tabs v-model="activeTab" class="service-tabs">
      <el-tab-pane label="对象存储迁移" name="migration">
        <section class="migration-summary">
          <div class="migration-metric">
            <span>文件总数</span>
            <strong>{{ formatCount(summary?.totalFileCount) }}</strong>
          </div>
          <div class="migration-metric">
            <span>已对象化</span>
            <strong>{{ formatCount(summary?.objectStoredCount) }}</strong>
          </div>
          <div class="migration-metric">
            <span>仍在 NAS</span>
            <strong>{{ formatCount(summary?.nasOnlyCount) }}</strong>
          </div>
          <div class="migration-metric">
            <span>异常任务</span>
            <strong>{{ formatCount(summary?.failedTaskCount) }}</strong>
          </div>
          <div class="migration-progress">
            <span>对象化覆盖率 {{ objectStoredPercent }}%</span>
            <el-progress :percentage="objectStoredPercent" :stroke-width="8" :show-text="false" />
          </div>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>创建迁移任务</h2>
              <span>只能显式选择文件；单次最多 {{ summary?.maxFilesPerTask ?? 10 }} 个文件，单文件上限 {{ formatBytes(summary?.maxFileSizeBytes) }}。</span>
            </div>
            <el-tag type="warning" effect="plain">受控镜像</el-tag>
          </div>

          <div class="migration-create">
            <el-form label-position="top">
              <el-form-item label="目标对象存储">
                <el-segmented v-model="createForm.targetProvider" :options="targetProviderOptions" />
              </el-form-item>
              <el-form-item label="待迁移内部文件 ID">
                <el-input
                  v-model="createForm.fileIdsText"
                  type="textarea"
                  :rows="3"
                  placeholder="输入内部文件 ID，用逗号、空格或换行分隔。也可以从下方文件选择器加入。"
                />
              </el-form-item>
            </el-form>
            <div class="migration-create__side">
              <strong>已选择 {{ selectedFileIds.length }} 个文件</strong>
              <span>任务创建后会逐个校验项目归属、生命周期、大小限制和对象版本幂等。</span>
              <el-button
                type="primary"
                :icon="Plus"
                :loading="creating"
                :disabled="selectedFileIds.length === 0"
                @click="createTask"
              >
                创建迁移任务
              </el-button>
            </div>
          </div>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>选择项目文件</h2>
              <span>按文件名或平台资产 ID 搜索，仅登记文件可加入迁移任务。</span>
            </div>
            <el-button :icon="Search" :loading="candidateLoading" @click="loadCandidates">查询</el-button>
          </div>

          <div class="candidate-toolbar">
            <el-input
              v-model="candidateFilters.keyword"
              clearable
              placeholder="搜索文件名或平台资产ID"
              @keyup.enter="loadCandidates"
            />
            <el-select v-model="candidateFilters.fileKind" placeholder="文件类型">
              <el-option label="全部" value="" />
              <el-option label="文档" value="DOCUMENT" />
              <el-option label="图纸" value="DRAWING" />
              <el-option label="模型" value="MODEL" />
            </el-select>
            <el-button @click="appendCandidateSelection">加入迁移清单</el-button>
          </div>

          <el-table
            v-loading="candidateLoading"
            :data="candidateRows"
            row-key="fileId"
            empty-text="暂无可选文件"
            @selection-change="handleCandidateSelection"
          >
            <el-table-column type="selection" width="48" :selectable="isCandidateSelectable" />
            <el-table-column label="平台资产ID" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">{{ row.assetUuid || '-' }}</template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
            <el-table-column prop="fileKind" label="类型" width="90" />
            <el-table-column label="大小" width="120" align="right">
              <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.registered === false ? 'warning' : 'success'" size="small">
                  {{ row.registered === false ? '未登记' : '已登记' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>迁移任务</h2>
              <span>任务只返回业务状态和平台资产 ID，不展示底层对象定位。</span>
            </div>
          </div>

          <el-table v-loading="taskLoading" :data="tasks" row-key="taskId" empty-text="暂无迁移任务" @row-click="openTask">
            <el-table-column prop="taskId" label="任务ID" width="90" />
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusType(row.taskStatus)" size="small">{{ taskStatusLabel(row.taskStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="targetProvider" label="目标" width="120" />
            <el-table-column label="数量" width="150">
              <template #default="{ row }">
                {{ row.totalCount }} / 成功 {{ row.successCount }} / 跳过 {{ row.skippedCount }} / 失败 {{ row.failureCount }}
              </template>
            </el-table-column>
            <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column label="更新时间" width="170">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click.stop="openTask(row)">详情</el-button>
                <el-button text :disabled="row.failureCount === 0" @click.stop="retryTask(row)">重试</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="文件访问安全" name="access">
        <section class="service-grid">
          <article v-for="item in enabledServices" :key="item.title" class="service-card">
            <el-tag type="success" effect="plain">已开放</el-tag>
            <h2>{{ item.title }}</h2>
            <p>{{ item.description }}</p>
            <el-button text type="primary" @click="openService(item.target)">进入</el-button>
          </article>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>未开放写操作</h2>
              <span>这些能力会直接改变 NAS 文件，当前只作为受控灰度能力保留。</span>
            </div>
            <el-tag type="warning" effect="plain">需要审批和回滚方案</el-tag>
          </div>
          <div class="disabled-action-grid">
            <article v-for="item in disabledActions" :key="item.title" class="disabled-action">
              <strong>{{ item.title }}</strong>
              <span>{{ item.reason }}</span>
              <el-button disabled size="small">受控开放</el-button>
            </article>
          </div>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>安全口径</h2>
              <span>当前文件服务遵循文件访问安全闭环和对象存储证据链边界。</span>
            </div>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="真实路径">普通项目用户不可见，使用平台逻辑路径和受控访问入口。</el-descriptions-item>
            <el-descriptions-item label="预览与下载">通过短时票据访问，预览权限和下载权限分离。</el-descriptions-item>
            <el-descriptions-item label="对象存储">只展示对象化状态，不展示 bucket、object key 或底层 URI。</el-descriptions-item>
            <el-descriptions-item label="Hermes">只读辅助，不能执行写库、NAS 操作或自动审批。</el-descriptions-item>
          </el-descriptions>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="detailVisible" title="迁移任务详情" size="760px">
      <template v-if="selectedTask">
        <section class="task-detail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="任务ID">{{ selectedTask.taskId }}</el-descriptions-item>
            <el-descriptions-item label="目标">{{ selectedTask.targetProvider }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ taskStatusLabel(selectedTask.taskStatus) }}</el-descriptions-item>
            <el-descriptions-item label="存储状态">{{ storageStateLabel(selectedTask.storageState) }}</el-descriptions-item>
            <el-descriptions-item label="数量">
              {{ selectedTask.totalCount }} / 成功 {{ selectedTask.successCount }} / 跳过 {{ selectedTask.skippedCount }} / 失败 {{ selectedTask.failureCount }}
            </el-descriptions-item>
            <el-descriptions-item label="说明">{{ selectedTask.message }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="selectedTask.rows" row-key="rowId" class="task-detail__rows">
            <el-table-column label="平台资产ID" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">{{ row.assetUuid || '-' }}</template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
            <el-table-column prop="fileKind" label="类型" width="90" />
            <el-table-column label="迁移状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusType(row.migrationStatus)" size="small">{{ migrationStatusLabel(row.migrationStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="存储状态" width="120">
              <template #default="{ row }">{{ storageStateLabel(row.storageState) }}</template>
            </el-table-column>
            <el-table-column prop="resultCode" label="结果码" width="150" show-overflow-tooltip />
            <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column label="完成时间" width="170">
              <template #default="{ row }">{{ formatDate(row.completedAt) }}</template>
            </el-table-column>
          </el-table>
        </section>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import type { RouteRecordName } from 'vue-router';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh, Search } from '@element-plus/icons-vue';

import {
  createStorageMigrationTask,
  fetchCatalogFiles,
  fetchStorageMigrationSummary,
  fetchStorageMigrationTask,
  fetchStorageMigrationTasks,
  retryStorageMigrationTask,
  type CatalogFile,
  type StorageMigrationSummary,
  type StorageMigrationTaskDetail,
  type StorageMigrationTaskListItem
} from '@/modules/data-steward/api/dataSteward';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const activeTab = ref(route.name === 'project-data-steward-file-service' ? 'access' : 'migration');
const loading = ref(false);
const taskLoading = ref(false);
const candidateLoading = ref(false);
const creating = ref(false);
const detailVisible = ref(false);
const summary = ref<StorageMigrationSummary | null>(null);
const tasks = ref<StorageMigrationTaskListItem[]>([]);
const selectedTask = ref<StorageMigrationTaskDetail | null>(null);
const candidateRows = ref<CatalogFile[]>([]);
const candidateSelection = ref<CatalogFile[]>([]);

const createForm = reactive({
  targetProvider: 'MINIO',
  fileIdsText: ''
});

const candidateFilters = reactive({
  keyword: '',
  fileKind: ''
});

const targetProviderOptions = [
  { label: 'MinIO', value: 'MINIO' },
  { label: 'S3-compatible', value: 'S3_COMPATIBLE' }
];

const projectId = computed(() => {
  const routeId = Number(route.params.projectId);
  return Number.isFinite(routeId) && routeId > 0 ? routeId : authStore.currentProjectId;
});

const projectLabel = computed(() => {
  const current = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return current ? `${current.code} ${current.name}` : '等待项目上下文';
});

const selectedFileIds = computed(() => {
  const typedIds = parseFileIds(createForm.fileIdsText);
  const selectedIds = candidateSelection.value
    .map((row) => Number(row.fileId))
    .filter((fileId) => Number.isFinite(fileId) && fileId > 0);
  return Array.from(new Set([...typedIds, ...selectedIds]));
});

const objectStoredPercent = computed(() => {
  const total = Number(summary.value?.totalFileCount ?? 0);
  if (!total) return 0;
  return Math.round((Number(summary.value?.objectStoredCount ?? 0) / total) * 100);
});

const enabledServices: Array<{ title: string; description: string; target: RouteRecordName }> = [
  { title: '文件预览', description: '查看预览状态，并通过短时票据打开可预览文件。', target: 'data-steward-asset-detail' },
  { title: '下载权限', description: '下载和预览分开判断，普通查看者不能下载。', target: 'data-steward-asset-detail' },
  { title: '权限证明', description: '验证当前用户对指定文件的访问权限和原因。', target: 'data-steward-agent-preview' },
  { title: 'Hermes 只读辅助', description: '围绕资产目录回答问题，不读取正文也不执行写操作。', target: 'data-steward-asset-detail' }
];

const disabledActions = [
  { title: '目录全量迁移', reason: '当前只允许显式选择文件，不开放目录或项目一键迁移。' },
  { title: '生成语义证据', reason: 'documents / chunks 和索引能力在 M4 后置。' },
  { title: '移动文件', reason: '会影响路径追溯和交付绑定，必须具备回滚方案。' },
  { title: '重命名文件', reason: '会影响文件查找、版本链和审计证据。' },
  { title: '真实删除', reason: '必须走申请、审批、隔离、恢复和到期永久删除。' },
  { title: '批量打包下载', reason: '涉及大容量、权限聚合和审计，后续单独开放。' }
];

watch(
  () => route.name,
  () => {
    activeTab.value = route.name === 'project-data-steward-file-service' ? 'access' : 'migration';
  }
);

watch(
  () => projectId.value,
  () => {
    void refresh();
  }
);

onMounted(() => {
  void refresh();
});

async function refresh() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    await Promise.all([loadSummary(), loadTasks(), loadCandidates()]);
  } finally {
    loading.value = false;
  }
}

async function loadSummary() {
  if (!projectId.value) return;
  summary.value = await fetchStorageMigrationSummary(projectId.value);
}

async function loadTasks() {
  if (!projectId.value) return;
  taskLoading.value = true;
  try {
    tasks.value = await fetchStorageMigrationTasks(projectId.value);
  } finally {
    taskLoading.value = false;
  }
}

async function loadCandidates() {
  if (!projectId.value) return;
  candidateLoading.value = true;
  try {
    const result = await fetchCatalogFiles({
      projectId: projectId.value,
      keyword: candidateFilters.keyword || undefined,
      fileKind: candidateFilters.fileKind || undefined,
      page: 1,
      pageSize: 20
    });
    candidateRows.value = result.rows;
  } finally {
    candidateLoading.value = false;
  }
}

function handleCandidateSelection(rows: CatalogFile[]) {
  candidateSelection.value = rows;
}

function appendCandidateSelection() {
  const ids = selectedFileIds.value;
  createForm.fileIdsText = ids.join(', ');
  ElMessage.success(`已加入 ${ids.length} 个文件`);
}

function isCandidateSelectable(row: CatalogFile) {
  return row.registered !== false && Number.isFinite(Number(row.fileId));
}

async function createTask() {
  if (!projectId.value || selectedFileIds.value.length === 0) return;
  const confirmed = await confirmAction('将创建对象存储镜像任务。NAS 原文件会保留，平台不会读取文件正文，也不会生成语义证据。');
  if (!confirmed) return;
  creating.value = true;
  try {
    const detail = await createStorageMigrationTask(projectId.value, {
      fileIds: selectedFileIds.value,
      targetProvider: createForm.targetProvider
    });
    selectedTask.value = detail;
    detailVisible.value = true;
    createForm.fileIdsText = '';
    candidateSelection.value = [];
    ElMessage.success('迁移任务已创建');
    await Promise.all([loadSummary(), loadTasks()]);
  } finally {
    creating.value = false;
  }
}

async function openTask(row: StorageMigrationTaskListItem) {
  selectedTask.value = await fetchStorageMigrationTask(row.taskId);
  detailVisible.value = true;
}

async function retryTask(row: StorageMigrationTaskListItem) {
  const confirmed = await confirmAction('将只重试失败或可重试文件；已对象化文件会按幂等策略跳过。');
  if (!confirmed) return;
  const detail = await retryStorageMigrationTask(row.taskId);
  selectedTask.value = detail;
  detailVisible.value = true;
  ElMessage.success('重试任务已创建');
  await Promise.all([loadSummary(), loadTasks()]);
}

async function confirmAction(message: string) {
  try {
    await ElMessageBox.confirm(message, '确认迁移任务', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    });
    return true;
  } catch {
    return false;
  }
}

function openService(name: RouteRecordName) {
  if (name === 'data-steward-agent-preview') {
    void router.push({ name });
    return;
  }
  if (projectId.value) {
    void router.push({ name, params: { projectId: projectId.value } });
  }
}

function parseFileIds(value: string) {
  if (!value.trim()) return [];
  return Array.from(new Set(value
    .split(/[\s,，;；]+/)
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0)));
}

function statusType(status: string) {
  const value = (status || '').toUpperCase();
  if (['COMPLETED', 'OBJECT_STORED', 'SKIPPED'].includes(value)) return 'success';
  if (['FAILED', 'PARTIAL_FAILED', 'MIGRATION_FAILED'].includes(value)) return 'danger';
  if (['RUNNING', 'MIGRATION_PENDING', 'PENDING'].includes(value)) return 'warning';
  return 'info';
}

function taskStatusLabel(status: string) {
  return ({
    COMPLETED: '已完成',
    FAILED: '失败',
    PARTIAL_FAILED: '部分失败',
    RUNNING: '执行中'
  } as Record<string, string>)[status] ?? status;
}

function migrationStatusLabel(status: string) {
  return ({
    COMPLETED: '已完成',
    FAILED: '失败',
    SKIPPED: '已跳过',
    RUNNING: '执行中',
    PENDING: '待执行'
  } as Record<string, string>)[status] ?? status;
}

function storageStateLabel(status: string) {
  return ({
    NAS_ONLY: '仅 NAS',
    MIGRATION_PENDING: '迁移待完成',
    OBJECT_STORED: '对象已存储',
    MIGRATION_FAILED: '迁移失败',
    MIGRATION_PARTIAL: '部分完成'
  } as Record<string, string>)[status] ?? status;
}

function formatCount(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('zh-CN');
}

function formatBytes(value: number | null | undefined) {
  const size = Number(value ?? 0);
  if (!Number.isFinite(size) || size <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let current = size;
  let index = 0;
  while (current >= 1024 && index < units.length - 1) {
    current /= 1024;
    index += 1;
  }
  return `${current >= 10 || index === 0 ? current.toFixed(0) : current.toFixed(1)} ${units[index]}`;
}

function formatDate(value: string | null | undefined) {
  if (!value) return '-';
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}
</script>

<style scoped>
.file-service-page {
  min-width: 0;
}

.service-notice,
.service-tabs,
.service-section {
  margin-top: 14px;
}

.migration-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) minmax(220px, 1.2fr);
  gap: 10px;
  align-items: stretch;
}

.migration-metric,
.migration-progress,
.service-card,
.service-section,
.disabled-action {
  min-width: 0;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-surface);
}

.migration-metric {
  display: grid;
  gap: 6px;
  padding: 14px;
}

.migration-metric span,
.migration-progress span,
.service-card p,
.disabled-action span,
.service-section__header span,
.migration-create__side span {
  margin: 0;
  color: var(--zy-muted);
  font-size: 13px;
}

.migration-metric strong {
  color: var(--zy-ink);
  font-size: 24px;
  line-height: 1.1;
}

.migration-progress {
  display: grid;
  align-content: center;
  gap: 10px;
  padding: 14px;
}

.migration-create {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 16px;
  align-items: start;
}

.migration-create__side {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px dashed var(--zy-line);
  border-radius: 8px;
  background: var(--zy-bg);
}

.candidate-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 160px auto;
  gap: 10px;
  margin-bottom: 12px;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.service-card {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: 14px;
}

.service-card h2 {
  margin: 0;
  color: var(--zy-ink);
  font-size: 16px;
}

.service-section {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.service-section__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.service-section__header h2 {
  margin: 0;
  font-size: 16px;
}

.disabled-action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.disabled-action {
  display: grid;
  gap: 8px;
  padding: 12px;
  background: var(--zy-bg);
}

.disabled-action strong {
  color: var(--zy-ink);
  font-size: 14px;
}

.task-detail {
  display: grid;
  gap: 16px;
}

.task-detail__rows {
  width: 100%;
}

@media (max-width: 1180px) {
  .migration-summary,
  .service-grid,
  .disabled-action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .migration-progress {
    grid-column: span 2;
  }
}

@media (max-width: 760px) {
  .migration-summary,
  .migration-create,
  .candidate-toolbar,
  .service-grid,
  .disabled-action-grid {
    grid-template-columns: 1fr;
  }

  .migration-progress {
    grid-column: auto;
  }
}
</style>
