<template>
  <section class="mvp-page model-page">
    <div class="mvp-page__header">
      <div>
        <h1>模型集成</h1>
        <p>{{ projectLabel }}，登记项目模型组合与发布状态，暂不执行真实轻量化转换。</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建集成</el-button>
      </div>
    </div>

    <section class="model-summary-grid">
      <article v-for="item in summaryCards" :key="item.label" class="model-summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.helper }}</em>
      </article>
    </section>

    <el-alert
      title="轻量化状态入口仅做只读准备检查。当前为 Mock 适配，不执行真实转换，不读取模型正文，不生成在线三维预览。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-table v-loading="loading" :data="models" class="master-table" empty-text="暂无模型集成">
      <el-table-column prop="name" label="集成名称" min-width="180" show-overflow-tooltip />
      <el-table-column label="关联模型文件" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ modelFileLabel(row.modelFileId) }}
        </template>
      </el-table-column>
      <el-table-column prop="versionNo" label="版本" width="90" />
      <el-table-column prop="componentCount" label="构件数" width="100" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="轻量化状态" width="150">
        <template #default="{ row }">
          <el-tag :type="lightweightTagType(row.id)">{{ lightweightLabel(row.id) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="适配模式 / 引擎" min-width="170">
        <template #default="{ row }">
          <div class="adapter-status">
            <strong>{{ lightweightStatus(row.id)?.engineMode ?? 'MOCK' }}</strong>
            <span>{{ lightweightStatus(row.id)?.engineConnected ? '引擎已连接' : '引擎未连接' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" min-width="170">
        <template #default="{ row }">{{ formatDate(row.publishedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="310" fixed="right">
        <template #default="{ row }">
          <el-button text :icon="UploadFilled" :disabled="row.status === 'PUBLISHED'" @click="handlePublish(row.id)">
            发布
          </el-button>
          <el-button text :icon="View" :loading="lightweightLoadingId === row.id" @click="openLightweightPlan(row)">
            查看轻量化准备
          </el-button>
          <el-button text :icon="View" @click="openViewerPlaceholder(row)">
            打开 3D 预览入口
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建模型集成" width="620px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="集成名称">
          <el-input v-model="form.name" maxlength="128" placeholder="例如：机电综合模型 V1" />
        </el-form-item>
        <el-form-item label="模型文件">
          <el-select
            v-model="form.modelFileId"
            filterable
            remote
            reserve-keyword
            :remote-method="searchModelFiles"
            :loading="modelFilesLoading"
            placeholder="搜索当前项目模型文件"
          >
            <el-option
              v-for="file in modelFiles"
              :key="file.id"
              :label="`${file.originalName} | ${file.versionNo || 'V1'} | ${file.processStatus}`"
              :value="file.id"
            />
          </el-select>
          <div class="field-hint">只从当前项目模型文件中选择，列表按需检索，不全量加载大项目文件。</div>
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.versionNo" maxlength="32" />
        </el-form-item>
        <el-form-item label="构件数">
          <el-input-number v-model="form.componentCount" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="适配载荷 JSON">
          <el-input v-model="form.adapterPayloadJson" type="textarea" :rows="3" />
          <div class="field-hint">保留给后续 BIM 引擎适配。本批不会调用真实转换服务。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="lightweightDialogVisible" title="轻量化准备检查" width="720px" class="lightweight-dialog">
      <el-alert
        title="当前为 Mock 适配，未执行真实轻量化转换；本检查不访问、不复制 NAS 文件，不代表 3D 预览已完成。"
        type="warning"
        show-icon
        :closable="false"
      />
      <section v-if="selectedLightweightStatus" class="lightweight-panel">
        <div class="lightweight-head">
          <div>
            <span>集成名称</span>
            <strong>{{ selectedModel?.name ?? '-' }}</strong>
          </div>
          <div>
            <span>模型文件</span>
            <strong>{{ selectedLightweightStatus.modelName }}</strong>
          </div>
          <el-tag type="warning">{{ selectedLightweightStatus.statusLabel }}</el-tag>
        </div>
        <dl class="lightweight-facts">
          <div>
            <dt>集成状态</dt>
            <dd>{{ statusLabel(selectedLightweightStatus.integrationStatus) }}</dd>
          </div>
          <div>
            <dt>模型格式</dt>
            <dd>{{ selectedLightweightStatus.modelFormat }}</dd>
          </div>
          <div>
            <dt>预览模式</dt>
            <dd>{{ selectedLightweightStatus.previewMode }}</dd>
          </div>
          <div>
            <dt>轻量化状态</dt>
            <dd>{{ selectedLightweightStatus.lightweightStatus }}</dd>
          </div>
          <div>
            <dt>适配模式</dt>
            <dd>{{ selectedLightweightStatus.engineMode }}</dd>
          </div>
          <div>
            <dt>引擎连接</dt>
            <dd>{{ selectedLightweightStatus.engineConnected ? '已连接' : '未连接' }}</dd>
          </div>
          <div>
            <dt>转换任务</dt>
            <dd>{{ selectedLightweightStatus.taskStatus }}</dd>
          </div>
          <div>
            <dt>预览入口</dt>
            <dd>{{ selectedLightweightStatus.viewerAvailable ? '可打开' : '不可打开' }}</dd>
          </div>
        </dl>
        <p class="blocked-reason">{{ selectedLightweightStatus.blockedReason }}</p>
      </section>

      <section v-if="selectedLightweightPlan" class="lightweight-plan">
        <div class="plan-flags">
          <el-tag :type="selectedLightweightPlan.dryRun ? 'success' : 'danger'">dryRun=true</el-tag>
          <el-tag :type="!selectedLightweightPlan.taskCreated ? 'success' : 'danger'">taskCreated=false</el-tag>
          <el-tag :type="selectedLightweightPlan.engineBindingRequired ? 'warning' : 'info'">需要引擎绑定</el-tag>
          <el-tag :type="!selectedLightweightPlan.realConversionExecuted ? 'success' : 'danger'">未执行真实转换</el-tag>
          <el-tag :type="!selectedLightweightPlan.nasFileTouched ? 'success' : 'danger'">未触碰 NAS 文件</el-tag>
        </div>
        <div class="plan-columns">
          <article>
            <h3>接入条件</h3>
            <ul>
              <li v-for="item in selectedLightweightPlan.requiredConditions" :key="item">{{ item }}</li>
            </ul>
          </article>
          <article>
            <h3>后续步骤</h3>
            <ul>
              <li v-for="item in selectedLightweightPlan.futureSteps" :key="item">{{ item }}</li>
            </ul>
          </article>
        </div>
        <article class="risk-list">
          <h3>风险提示</h3>
          <ul>
            <li v-for="item in selectedLightweightPlan.riskWarnings" :key="item">{{ item }}</li>
          </ul>
        </article>
      </section>
      <template #footer>
        <el-button @click="lightweightDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Plus, Refresh, UploadFilled, View } from '@element-plus/icons-vue';

import {
  createModelIntegration,
  fetchBimLightweightPlan,
  fetchBimLightweightStatus,
  fetchFileResourcesPage,
  fetchModelIntegrations,
  publishModelIntegration,
  type BimLightweightPlan,
  type BimLightweightStatus,
  type FileResource,
  type ModelIntegration
} from '@/modules/data-steward/api/dataSteward';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const authStore = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const modelFilesLoading = ref(false);
const lightweightDialogVisible = ref(false);
const lightweightLoadingId = ref<number | null>(null);
const models = ref<ModelIntegration[]>([]);
const modelFiles = ref<FileResource[]>([]);
const lightweightStatuses = ref<Record<number, BimLightweightStatus>>({});
const selectedModel = ref<ModelIntegration | null>(null);
const selectedLightweightStatus = ref<BimLightweightStatus | null>(null);
const selectedLightweightPlan = ref<BimLightweightPlan | null>(null);

const form = reactive({
  name: '',
  modelFileId: null as number | null,
  versionNo: 'V1',
  componentCount: 0,
  adapterPayloadJson: '{"adapter":"mock-bim","lightweightPreview":false}'
});

const projectId = computed(() => {
  const routeId = Number(route.params.projectId);
  return Number.isFinite(routeId) && routeId > 0 ? routeId : authStore.currentProjectId;
});
const projectLabel = computed(() => {
  const current = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return current ? `${current.code} ${current.name}` : '等待项目上下文';
});
const publishedCount = computed(() => models.value.filter((item) => item.status === 'PUBLISHED').length);
const summaryCards = computed(() => [
  { label: '模型集成', value: models.value.length.toLocaleString('zh-CN'), helper: '已登记组合' },
  { label: '已发布', value: publishedCount.value.toLocaleString('zh-CN'), helper: '可供对象引用' },
  { label: '待发布', value: (models.value.length - publishedCount.value).toLocaleString('zh-CN'), helper: '仅平台元数据' },
  {
    label: 'Mock 引擎',
    value: mockEngineCount.value.toLocaleString('zh-CN'),
    helper: '未连接真实引擎'
  }
]);
const mockEngineCount = computed(
  () => models.value.filter((item) => lightweightStatuses.value[item.id]?.engineMode === 'MOCK').length
);

watch(projectId, () => loadPage(), { immediate: true });

async function loadPage() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [nextModels] = await Promise.all([
      fetchModelIntegrations(projectId.value),
      loadModelFiles('')
    ]);
    models.value = nextModels;
    await loadLightweightStatuses(nextModels);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '模型集成加载失败');
  } finally {
    loading.value = false;
  }
}

async function loadLightweightStatuses(rows: ModelIntegration[]) {
  if (!projectId.value || rows.length === 0) {
    lightweightStatuses.value = {};
    return;
  }
  const entries = await Promise.all(
    rows.map(async (row) => {
      try {
        return [row.id, await fetchBimLightweightStatus(projectId.value!, row.id)] as const;
      } catch {
        return [row.id, null] as const;
      }
    })
  );
  lightweightStatuses.value = Object.fromEntries(entries.filter(([, status]) => status !== null)) as Record<
    number,
    BimLightweightStatus
  >;
}

async function loadModelFiles(keyword: string) {
  if (!projectId.value) return;
  modelFilesLoading.value = true;
  try {
    const page = await fetchFileResourcesPage(projectId.value, {
      fileKind: 'MODEL',
      keyword,
      pageNo: 1,
      pageSize: 50
    });
    modelFiles.value = page.rows;
  } finally {
    modelFilesLoading.value = false;
  }
}

function searchModelFiles(keyword: string) {
  void loadModelFiles(keyword);
}

function openCreateDialog() {
  Object.assign(form, {
    name: '',
    modelFileId: modelFiles.value.find((file) => file.processStatus === 'PROCESSED')?.id ?? modelFiles.value[0]?.id ?? null,
    versionNo: 'V1',
    componentCount: 0,
    adapterPayloadJson: '{"adapter":"mock-bim","lightweightPreview":false}'
  });
  dialogVisible.value = true;
}

async function handleSave() {
  if (!projectId.value || !form.modelFileId) return;
  saving.value = true;
  try {
    await createModelIntegration(projectId.value, {
      name: form.name.trim(),
      modelFileId: form.modelFileId,
      versionNo: form.versionNo.trim() || 'V1',
      componentCount: form.componentCount,
      adapterPayloadJson: form.adapterPayloadJson
    });
    ElMessage.success('模型集成已创建');
    dialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handlePublish(integrationId: number) {
  if (!projectId.value) return;
  try {
    await publishModelIntegration(projectId.value, integrationId);
    ElMessage.success('模型集成已发布，当前仅更新平台元数据状态');
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败');
  }
}

async function openLightweightPlan(row: ModelIntegration) {
  if (!projectId.value) return;
  lightweightLoadingId.value = row.id;
  selectedModel.value = row;
  try {
    const [status, plan] = await Promise.all([
      fetchBimLightweightStatus(projectId.value, row.id),
      fetchBimLightweightPlan(projectId.value, row.id)
    ]);
    lightweightStatuses.value = {
      ...lightweightStatuses.value,
      [row.id]: status
    };
    selectedLightweightStatus.value = status;
    selectedLightweightPlan.value = plan;
    lightweightDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '轻量化准备检查失败');
  } finally {
    lightweightLoadingId.value = null;
  }
}

function openViewerPlaceholder(row: ModelIntegration) {
  const status = lightweightStatus(row.id);
  ElMessage.warning(status?.actionHint ?? '当前为 Mock 适配，未执行真实轻量化转换，暂不能打开 3D 预览。');
}

function modelFileLabel(fileId: number) {
  return modelFiles.value.find((file) => file.id === fileId)?.originalName ?? `文件ID ${fileId}`;
}

function lightweightStatus(integrationId: number) {
  return lightweightStatuses.value[integrationId];
}

function lightweightLabel(integrationId: number) {
  return lightweightStatus(integrationId)?.statusLabel ?? 'Mock 待检查';
}

function lightweightTagType(integrationId: number) {
  return lightweightStatus(integrationId)?.viewerAvailable ? 'success' : 'warning';
}

function statusLabel(value: string) {
  const labels: Record<string, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布'
  };
  return labels[value] ?? value;
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
.model-page {
  min-width: 0;
}

.model-summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.model-summary-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #ffffff;
}

.model-summary-card span,
.model-summary-card em {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.model-summary-card strong {
  display: block;
  margin: 6px 0 4px;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.15;
}

.adapter-status {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.adapter-status strong {
  color: #0f172a;
  font-size: 13px;
  line-height: 1.2;
}

.adapter-status span {
  color: #64748b;
  font-size: 12px;
}

.lightweight-dialog :deep(.el-dialog__body) {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.lightweight-panel,
.lightweight-plan article {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #ffffff;
}

.lightweight-panel {
  padding: 14px;
}

.lightweight-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
}

.lightweight-head span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.lightweight-head strong {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lightweight-facts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 14px 0 0;
}

.lightweight-facts div {
  min-width: 0;
  padding: 10px;
  border-radius: 8px;
  background: #f8fafc;
}

.lightweight-facts dt {
  color: #64748b;
  font-size: 12px;
}

.lightweight-facts dd {
  margin: 4px 0 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.blocked-reason {
  margin: 12px 0 0;
  color: #b45309;
  font-size: 13px;
}

.lightweight-plan {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.plan-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.lightweight-plan article {
  padding: 12px;
}

.lightweight-plan h3 {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 13px;
}

.lightweight-plan ul {
  margin: 0;
  padding-left: 18px;
  color: #334155;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .model-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .lightweight-head,
  .lightweight-facts,
  .plan-columns {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .model-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
