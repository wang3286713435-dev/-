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

    <section class="m3g-readiness">
      <article class="readiness-card">
        <span>对象存储就绪</span>
        <strong>{{ readinessStatusLabel(readiness?.readinessStatus) }}</strong>
        <el-tag :type="readinessTagType(readiness?.readinessStatus)" effect="plain">
          {{ endpointTypeLabel(readiness?.endpointType) }}
        </el-tag>
        <p>{{ readiness?.message || '正在读取对象存储 readiness。' }}</p>
      </article>
      <article class="readiness-card">
        <span>读写探测</span>
        <strong>{{ readiness?.readable && readiness?.writable ? '可读写' : '未完全通过' }}</strong>
        <p>只做专用 smoke 探测，不返回 endpoint、bucket、object key 或密钥。</p>
      </article>
      <article class="readiness-card readiness-card--wide">
        <span>全项目对象化覆盖率</span>
        <strong>{{ formatPercent(inventory?.objectificationCoverageRate) }}%</strong>
        <p>
          {{ formatCount(inventory?.totalProjects) }} 个项目，
          {{ formatCount(inventory?.totalFiles) }} 个文件，
          仍在 NAS {{ formatCount(inventory?.nasOnlyFiles) }} 个。
        </p>
      </article>
    </section>

    <el-alert
      v-if="readiness?.readinessStatus === 'LOCAL_DEV_ONLY'"
      type="warning"
      :closable="false"
      show-icon
      class="service-notice"
      title="当前对象存储仍是本机开发环境"
      description="尚未确认 NAS 侧 MinIO，不能启动真实全项目对象化；本页 dry-run 只生成计划，不复制文件。"
    />

    <el-tabs v-model="activeTab" class="service-tabs">
      <el-tab-pane label="对象存储迁移" name="migration">
        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>项目对象化盘点</h2>
              <span>只基于 MySQL 台账和对象版本统计，不递归扫描真实 NAS。</span>
            </div>
            <el-tag type="info" effect="plain">M3G-1 dry-run</el-tag>
          </div>

          <el-table :data="inventoryRows" row-key="projectId" empty-text="暂无项目对象化盘点">
            <el-table-column label="项目" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
            </el-table-column>
            <el-table-column label="文件" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.totalFiles) }}</template>
            </el-table-column>
            <el-table-column label="已对象化" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.objectStoredFiles) }}</template>
            </el-table-column>
            <el-table-column label="仍在 NAS" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.nasOnlyFiles) }}</template>
            </el-table-column>
            <el-table-column label="覆盖率" width="150">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.objectificationCoverageRate || 0)" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column label="checksum" width="130">
              <template #default="{ row }">{{ formatPercent(row.checksumCoverageRate) }}%</template>
            </el-table-column>
            <el-table-column label="风险" width="90">
              <template #default="{ row }">
                <el-tag :type="riskTagType(row.riskLevel)" size="small">{{ riskLabel(row.riskLevel) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>单项目对象化 dry-run</h2>
              <span>生成筛选计划，不创建迁移任务、不复制文件、不修改 NAS。</span>
            </div>
            <el-button type="primary" :loading="dryRunLoading" @click="runDryRun">生成 dry-run 计划</el-button>
          </div>

          <div class="dry-run-form">
            <el-input v-model="dryRunForm.directoryPath" clearable placeholder="逻辑目录，可留空，例如 01_文件收发" />
            <el-select v-model="dryRunForm.storageState" placeholder="存储状态">
              <el-option label="全部" value="ANY" />
              <el-option label="仅 NAS" value="NAS_ONLY" />
              <el-option label="迁移失败" value="MIGRATION_FAILED" />
            </el-select>
            <el-select v-model="dryRunForm.checksumState" placeholder="checksum">
              <el-option label="全部" value="ANY" />
              <el-option label="已有 checksum" value="HAS_CHECKSUM" />
              <el-option label="缺少 checksum" value="MISSING_CHECKSUM" />
            </el-select>
            <el-input v-model="dryRunForm.extensionsText" clearable placeholder="扩展名：pdf,dwg,rvt，可留空" />
            <el-input-number v-model="dryRunForm.limit" :min="1" :max="5000" controls-position="right" />
          </div>

          <template v-if="dryRunResult">
            <div class="dry-run-summary">
              <div>
                <span>选中文件</span>
                <strong>{{ formatCount(dryRunResult.selectedFileCount) }}</strong>
              </div>
              <div>
                <span>预估容量</span>
                <strong>{{ formatBytes(dryRunResult.selectedTotalBytes) }}</strong>
              </div>
              <div>
                <span>预估批次</span>
                <strong>{{ formatCount(dryRunResult.estimatedBatches) }}</strong>
              </div>
              <div>
                <span>已对象化跳过</span>
                <strong>{{ formatCount(dryRunResult.objectStoredSkipCount) }}</strong>
              </div>
            </div>
            <div class="dry-run-actions">
              <div>
                <strong>105 小批灰度对象化</strong>
                <span>{{ dryRunGrayHint }}</span>
              </div>
              <div class="dry-run-actions__buttons">
                <el-button :disabled="dryRunExecutableFileIds.length === 0" @click="appendDryRunSelection">
                  加入小批清单
                </el-button>
                <el-button
                  type="primary"
                  :loading="creating"
                  :disabled="!canRunDryRunGrayTask"
                  @click="createTaskFromDryRun"
                >
                  执行小批灰度
                </el-button>
              </div>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="service-notice"
              :title="dryRunResult.dryRun && !dryRunResult.migrationStarted ? 'dry-run 已生成，未启动迁移' : '请检查 dry-run 状态'"
              :description="dryRunResult.riskMessages.join(' ')"
            />
            <el-table :data="dryRunResult.sampleItems" row-key="fileId" empty-text="暂无样本文件">
              <el-table-column prop="assetUuid" label="平台资产ID" min-width="230" show-overflow-tooltip />
              <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
              <el-table-column prop="fileKind" label="类型" width="90" />
              <el-table-column prop="extension" label="扩展名" width="90" />
              <el-table-column label="大小" width="110" align="right">
                <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
              </el-table-column>
              <el-table-column prop="checksumStatus" label="checksum" width="150" />
              <el-table-column prop="storageStatus" label="存储状态" width="150" />
              <el-table-column prop="reason" label="计划原因" min-width="180" show-overflow-tooltip />
            </el-table>
          </template>
        </section>

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
  dryRunStorageObjectificationPlan,
  fetchCatalogFiles,
  fetchStorageObjectificationInventory,
  fetchStorageMigrationSummary,
  fetchStorageMigrationTask,
  fetchStorageMigrationTasks,
  fetchStorageProviderReadiness,
  retryStorageMigrationTask,
  type CatalogFile,
  type ProjectStorageObjectificationInventory,
  type StorageObjectificationDryRun,
  type StorageObjectificationInventory,
  type StorageMigrationSummary,
  type StorageMigrationTaskDetail,
  type StorageMigrationTaskListItem,
  type StorageProviderReadiness
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
const dryRunLoading = ref(false);
const detailVisible = ref(false);
const summary = ref<StorageMigrationSummary | null>(null);
const readiness = ref<StorageProviderReadiness | null>(null);
const inventory = ref<StorageObjectificationInventory | null>(null);
const dryRunResult = ref<StorageObjectificationDryRun | null>(null);
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

const dryRunForm = reactive({
  directoryPath: '',
  storageState: 'NAS_ONLY',
  checksumState: 'ANY',
  extensionsText: '',
  limit: 200
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

const readinessReady = computed(() => (
  readiness.value?.endpointType === 'NAS_SIDE_MINIO'
  && readiness.value?.readinessStatus === 'READY'
  && readiness.value?.writable === true
));

const dryRunExecutableItems = computed(() => {
  const maxFiles = Number(summary.value?.maxFilesPerTask ?? 10);
  const maxSize = Number(summary.value?.maxFileSizeBytes ?? 10 * 1024 * 1024);
  return (dryRunResult.value?.sampleItems ?? [])
    .filter((item) => item.storageStatus === 'NAS_ONLY')
    .filter((item) => ['ELIGIBLE_DRY_RUN', 'MISSING_CHECKSUM'].includes(item.reason))
    .filter((item) => Number(item.sizeBytes ?? 0) <= maxSize)
    .slice(0, maxFiles);
});

const dryRunExecutableFileIds = computed(() => dryRunExecutableItems.value.map((item) => item.fileId));

const canRunDryRunGrayTask = computed(() => readinessReady.value && dryRunExecutableFileIds.value.length > 0);

const dryRunGrayHint = computed(() => {
  if (!readinessReady.value) {
    return '需要 NAS 侧 MinIO READY 后才能执行；dry-run 本身仍是只读计划。';
  }
  if (!dryRunResult.value) {
    return '先生成 dry-run 计划，再从其中选择安全小样本。';
  }
  if (dryRunExecutableFileIds.value.length === 0) {
    return '当前 dry-run 样本没有符合小批灰度条件的 NAS_ONLY 文件。';
  }
  return `将使用 dry-run 样本中的 ${dryRunExecutableFileIds.value.length} 个文件；NAS 原文件保留，只复制副本到对象存储。`;
});

const inventoryRows = computed<ProjectStorageObjectificationInventory[]>(() => {
  const rows = inventory.value?.projects ?? [];
  if (projectId.value) {
    const current = rows.find((row) => Number(row.projectId) === Number(projectId.value));
    return current ? [current, ...rows.filter((row) => Number(row.projectId) !== Number(projectId.value)).slice(0, 5)] : rows.slice(0, 6);
  }
  return rows.slice(0, 8);
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
    await Promise.all([loadReadiness(), loadInventory(), loadSummary(), loadTasks(), loadCandidates()]);
  } finally {
    loading.value = false;
  }
}

async function loadReadiness() {
  readiness.value = await fetchStorageProviderReadiness();
}

async function loadInventory() {
  inventory.value = await fetchStorageObjectificationInventory();
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

function appendDryRunSelection() {
  if (dryRunExecutableFileIds.value.length === 0) return;
  createForm.fileIdsText = dryRunExecutableFileIds.value.join(', ');
  ElMessage.success(`已加入 ${dryRunExecutableFileIds.value.length} 个 dry-run 小样本`);
}

function isCandidateSelectable(row: CatalogFile) {
  return row.registered !== false && Number.isFinite(Number(row.fileId));
}

async function createTask() {
  if (!projectId.value || selectedFileIds.value.length === 0) return;
  const confirmed = await confirmAction('将创建对象存储镜像任务。NAS 原文件会保留，平台不会读取文件正文，也不会生成语义证据。');
  if (!confirmed) return;
  await submitMigrationTask(selectedFileIds.value);
}

async function createTaskFromDryRun() {
  if (!projectId.value || dryRunExecutableFileIds.value.length === 0) return;
  if (!readinessReady.value) {
    ElMessage.warning('NAS 侧 MinIO 尚未 READY，不能执行真实对象化灰度。');
    return;
  }
  const confirmed = await confirmAction('将按 dry-run 小样本执行 105 对象化灰度。NAS 原文件保留，只复制副本到对象存储；不会读取正文，也不会写语义索引。');
  if (!confirmed) return;
  await submitMigrationTask(dryRunExecutableFileIds.value);
}

async function submitMigrationTask(fileIds: number[]) {
  if (!projectId.value || fileIds.length === 0) return;
  creating.value = true;
  try {
    const detail = await createStorageMigrationTask(projectId.value, {
      fileIds,
      targetProvider: createForm.targetProvider
    });
    selectedTask.value = detail;
    detailVisible.value = true;
    createForm.fileIdsText = '';
    candidateSelection.value = [];
    ElMessage.success('迁移任务已创建');
    await Promise.all([loadInventory(), loadSummary(), loadTasks()]);
  } finally {
    creating.value = false;
  }
}

async function runDryRun() {
  if (!projectId.value) return;
  dryRunLoading.value = true;
  try {
    dryRunResult.value = await dryRunStorageObjectificationPlan(projectId.value, {
      directoryPath: dryRunForm.directoryPath || undefined,
      storageState: dryRunForm.storageState as 'ANY' | 'NAS_ONLY' | 'MIGRATION_FAILED',
      checksumState: dryRunForm.checksumState as 'ANY' | 'HAS_CHECKSUM' | 'MISSING_CHECKSUM',
      extensions: parseExtensions(dryRunForm.extensionsText),
      limit: dryRunForm.limit
    });
    ElMessage.success('dry-run 计划已生成，未启动真实迁移');
  } finally {
    dryRunLoading.value = false;
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

function parseExtensions(value: string) {
  if (!value.trim()) return undefined;
  const items = Array.from(new Set(value
    .split(/[\s,，;；]+/)
    .map((item) => item.trim().replace(/^\./, '').toLowerCase())
    .filter(Boolean)));
  return items.length ? items : undefined;
}

function endpointTypeLabel(value: string | null | undefined) {
  return ({
    NAS_SIDE_MINIO: 'NAS 侧 MinIO',
    LOCAL_DEV_MINIO: '本机开发 MinIO',
    UNKNOWN: '待确认 endpoint'
  } as Record<string, string>)[value || ''] ?? value ?? '读取中';
}

function readinessStatusLabel(value: string | null | undefined) {
  return ({
    READY: '已就绪',
    NOT_CONFIGURED: '未配置',
    UNREACHABLE: '不可达',
    LOCAL_DEV_ONLY: '仅本机开发',
    WRITE_UNAVAILABLE: '写入不可用'
  } as Record<string, string>)[value || ''] ?? value ?? '读取中';
}

function readinessTagType(value: string | null | undefined) {
  if (value === 'READY') return 'success';
  if (value === 'LOCAL_DEV_ONLY' || value === 'WRITE_UNAVAILABLE') return 'warning';
  if (value === 'NOT_CONFIGURED' || value === 'UNREACHABLE') return 'danger';
  return 'info';
}

function riskLabel(value: string) {
  return ({
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高'
  } as Record<string, string>)[value] ?? value;
}

function riskTagType(value: string) {
  if (value === 'LOW') return 'success';
  if (value === 'MEDIUM') return 'warning';
  if (value === 'HIGH') return 'danger';
  return 'info';
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

function formatPercent(value: number | null | undefined) {
  const numeric = Number(value ?? 0);
  if (!Number.isFinite(numeric)) return '0.00';
  return numeric.toFixed(2);
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

.m3g-readiness {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(260px, 1.4fr);
  gap: 10px;
  margin-top: 14px;
}

.readiness-card {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-surface);
}

.readiness-card strong {
  color: var(--zy-ink);
  font-size: 22px;
  line-height: 1.1;
}

.readiness-card span,
.readiness-card p {
  margin: 0;
  color: var(--zy-muted);
  font-size: 13px;
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

.dry-run-form {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 150px 160px minmax(180px, 1fr) 130px;
  gap: 10px;
  margin-bottom: 14px;
}

.dry-run-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.dry-run-summary > div {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-soft);
}

.dry-run-summary span {
  color: var(--zy-muted);
  font-size: 12px;
}

.dry-run-summary strong {
  color: var(--zy-ink);
  font-size: 20px;
}

.dry-run-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-bg);
}

.dry-run-actions > div:first-child {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.dry-run-actions strong {
  color: var(--zy-ink);
  font-size: 14px;
}

.dry-run-actions span {
  color: var(--zy-muted);
  font-size: 13px;
}

.dry-run-actions__buttons {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
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
  .disabled-action-grid,
  .m3g-readiness,
  .dry-run-summary {
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
  .m3g-readiness,
  .dry-run-form,
  .dry-run-summary,
  .service-grid,
  .disabled-action-grid {
    grid-template-columns: 1fr;
  }

  .migration-progress {
    grid-column: auto;
  }
}
</style>
