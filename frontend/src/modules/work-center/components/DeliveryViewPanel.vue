<template>
  <section class="mvp-page">
    <div class="mvp-page__header">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="mvp-page__actions">
        <el-button @click="handleExportCompleteness">导出完整率 CSV</el-button>
        <el-button @click="handleExportReviewSummary">导出审核汇总 CSV</el-button>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Link" @click="openCreateDialog">选择文件补交</el-button>
      </div>
    </div>

    <section class="workflow-guide">
      <div class="workflow-guide__main">
        <span class="workflow-guide__step">交付执行</span>
        <h2>{{ title }}用于检查标准要求是否已经补齐文件</h2>
        <p>
          平台会按当前项目的部位树和交付物标准生成应交项。已挂接表示已经选了文件，缺失项表示标准要求存在，但还没有绑定到对应文件。
        </p>
      </div>
      <ol class="workflow-guide__steps">
        <li>先确认交付标准已就绪。</li>
        <li>在缺失项中选择需要补交的条目。</li>
        <li>选择已处理完成的{{ viewLabel }}文件并保存，随后提交审核。</li>
      </ol>
    </section>

    <!-- Standard readiness alert -->
    <el-alert
      v-if="completeness && !completeness.standardReady"
      class="mb"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #title>
        <strong>交付标准尚未就绪</strong>
      </template>
      <ul class="issue-list">
        <li v-for="issue in completeness.readinessIssues" :key="issue">{{ issue }}</li>
      </ul>
      <template v-if="!completeness.readinessIssues?.length">
        请先在主数据管理中完善部位树、节点类型、交付物定义、交付物类型和目录模板。
      </template>
      <div class="readiness-help">
        <p>{{ readinessFixText }}</p>
        <div class="readiness-help__actions">
          <el-button size="small" type="primary" @click="goToDeliverableStandard">去配置交付物标准</el-button>
          <el-button size="small" @click="goToNodeTypes">检查节点类型</el-button>
        </div>
      </div>
    </el-alert>

    <!-- Completeness summary -->
    <section v-if="completeness && completeness.standardReady" class="completeness-card">
      <div class="completeness-card__summary">
        <span>本项目按当前交付标准</span>
        <strong>应交 {{ completeness.totalRequired }} 项</strong>
        <span>，</span>
        <strong class="text-success">已完成 {{ completeness.completedCount }} 项</strong>
        <span>，</span>
        <strong class="text-danger">缺失 {{ completeness.missingCount }} 项</strong>
      </div>
      <el-progress
        :percentage="Math.round((completeness.completionRate ?? 0) * 100)"
        :color="progressColor"
        :stroke-width="16"
      />
    </section>

    <section v-else-if="completeness" class="completeness-card completeness-card--empty">
      <p>当前{{ title }}还没有可计算的交付标准。请先在交付物标准中补齐{{ viewLabel }}类交付物类型。</p>
    </section>

    <!-- Tabs: bounded / missing -->
    <el-tabs v-if="completeness" v-model="activeTab" class="mvp-tabs">
      <el-tab-pane label="已挂接" name="bound">
        <section class="mvp-stat-row">
          <article class="mvp-stat">
            <span>视图类型</span>
            <strong>{{ view?.viewType ?? viewType }}</strong>
          </article>
          <article class="mvp-stat">
            <span>挂接总数</span>
            <strong>{{ view?.totalCount ?? 0 }}</strong>
          </article>
          <article class="mvp-stat">
            <span>已绑定</span>
            <strong>{{ view?.boundCount ?? 0 }}</strong>
          </article>
        </section>

        <el-table v-loading="loading" :data="view?.rows ?? []" class="master-table" empty-text="暂无交付挂接">
          <el-table-column prop="deliverableDefinitionName" label="交付定义" min-width="170" />
          <el-table-column prop="deliverableTypeName" label="交付类型" min-width="150" />
          <el-table-column prop="fileName" label="文件" min-width="220" />
          <el-table-column prop="sectionNodeName" label="部位" min-width="140" />
          <el-table-column prop="managedObjectName" label="对象" min-width="140" />
          <el-table-column prop="versionNo" label="版本" width="90" />
          <el-table-column prop="reviewStatus" label="审核" width="100">
            <template #default="{ row }">
              <el-tag :type="reviewTagType(row.reviewStatus)" size="small">{{ reviewLabel(row.reviewStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核操作" width="240" fixed="right">
            <template #default="{ row }">
              <template v-if="row.bindingStatus === 'BOUND'">
                <el-button v-if="row.reviewStatus === 'DRAFT'" size="small" type="primary" :loading="reviewLoading[`submit-${row.id}`]" @click="handleSubmitReview(row)">
                  提交审核
                </el-button>
                <el-button v-if="row.reviewStatus === 'PENDING' || row.reviewStatus === 'REJECTED'" size="small" type="success" :loading="reviewLoading[`approve-${row.id}`]" @click="handleApprove(row)">
                  通过
                </el-button>
                <el-button v-if="row.reviewStatus === 'PENDING' || row.reviewStatus === 'REJECTED'" size="small" type="danger" @click="openRejectDialog(row)">
                  驳回
                </el-button>
                <el-button size="small" @click="openReviewRecords(row)">记录</el-button>
              </template>
            </template>
          </el-table-column>
          <el-table-column prop="bindingStatus" label="状态" width="100" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="缺失项" name="missing">
        <p class="tab-helper">缺失项表示“当前标准要求存在，但还没有选择文件完成交付”的条目。可以从这里直接选择文件补交。</p>
        <el-table v-loading="completenessLoading" :data="missingRows" class="master-table" empty-text="暂无缺失项">
          <el-table-column prop="targetName" label="目标" min-width="140">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.targetType === 'SECTION' ? '部位' : '对象' }}</el-tag>
              {{ row.targetName }}
            </template>
          </el-table-column>
          <el-table-column prop="deliverableDefinitionName" label="交付定义" min-width="170" />
          <el-table-column prop="deliverableTypeName" label="交付类型" min-width="150" />
          <el-table-column prop="fileKind" label="文件类型" width="100" />
          <el-table-column prop="missingReason" label="缺失原因" min-width="140" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="openBindFromMissing(row)">
                选择文件补交
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- Fallback when completeness API hasn't been loaded yet -->
    <template v-if="!completeness">
      <section class="mvp-stat-row">
        <article class="mvp-stat">
          <span>视图类型</span>
          <strong>{{ view?.viewType ?? viewType }}</strong>
        </article>
        <article class="mvp-stat">
          <span>挂接总数</span>
          <strong>{{ view?.totalCount ?? 0 }}</strong>
        </article>
        <article class="mvp-stat">
          <span>已绑定</span>
          <strong>{{ view?.boundCount ?? 0 }}</strong>
        </article>
      </section>

      <el-table v-loading="loading" :data="view?.rows ?? []" class="master-table" empty-text="暂无交付挂接">
        <el-table-column prop="deliverableDefinitionName" label="交付定义" min-width="170" />
        <el-table-column prop="deliverableTypeName" label="交付类型" min-width="150" />
        <el-table-column prop="fileName" label="文件" min-width="220" />
        <el-table-column prop="sectionNodeName" label="部位" min-width="140" />
        <el-table-column prop="managedObjectName" label="对象" min-width="140" />
        <el-table-column prop="versionNo" label="版本" width="90" />
        <el-table-column prop="reviewStatus" label="审核" width="100">
          <template #default="{ row }">
            <el-tag :type="reviewTagType(row.reviewStatus)" size="small">{{ reviewLabel(row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核操作" width="240" fixed="right">
          <template #default="{ row }">
            <template v-if="row.bindingStatus === 'BOUND'">
              <el-button v-if="row.reviewStatus === 'DRAFT'" size="small" type="primary" :loading="reviewLoading[`submit-${row.id}`]" @click="handleSubmitReview(row)">
                提交审核
              </el-button>
              <el-button v-if="row.reviewStatus === 'PENDING' || row.reviewStatus === 'REJECTED'" size="small" type="success" :loading="reviewLoading[`approve-${row.id}`]" @click="handleApprove(row)">
                通过
              </el-button>
              <el-button v-if="row.reviewStatus === 'PENDING' || row.reviewStatus === 'REJECTED'" size="small" type="danger" @click="openRejectDialog(row)">
                驳回
              </el-button>
              <el-button size="small" @click="openReviewRecords(row)">记录</el-button>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="bindingStatus" label="状态" width="100" />
      </el-table>
    </template>

    <!-- Reject dialog -->
    <el-dialog v-model="rejectDialogVisible" title="驳回交付资料" width="480px">
      <el-form label-position="top">
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectReason" type="textarea" :rows="3" maxlength="1024" placeholder="请填写驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" :disabled="!rejectReason.trim()" @click="handleReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>

    <!-- Review records drawer -->
    <el-drawer v-model="recordsDrawerVisible" title="审核记录" size="420px">
      <el-timeline v-if="reviewRecords.length">
        <el-timeline-item
          v-for="rec in reviewRecords"
          :key="rec.id"
          :timestamp="rec.createdAt"
          :color="recordColor(rec.action)"
        >
          <strong>{{ recordActionLabel(rec.action) }}</strong>
          <p v-if="rec.comment" style="margin-top: 4px; color: var(--el-text-color-secondary)">{{ rec.comment }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审核记录" />
    </el-drawer>

    <!-- Create binding dialog -->
    <el-dialog v-model="dialogVisible" :title="`选择文件补交到${title}`" width="620px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="交付物类型">
          <el-select v-model="form.deliverableTypeId" filterable>
            <el-option v-for="item in deliverableTypes" :key="item.id" :label="`${item.name} | ${item.code}`" :value="item.id" />
          </el-select>
          <div class="field-hint">这表示当前要补交哪一类资料，通常已由缺失项自动带入。</div>
        </el-form-item>
        <el-form-item label="文件资源">
          <el-select
            v-model="form.fileResourceId"
            filterable
            remote
            :remote-method="searchFiles"
            :loading="filesLoading"
            placeholder="输入关键词搜索文件"
            @focus="searchFiles('')"
          >
            <el-option
              v-for="file in files"
              :key="file.id"
              :label="`${file.originalName} | ${file.processStatus}`"
              :value="file.id"
            />
          </el-select>
          <div class="field-hint">选择本次要提交的文件。列表只显示当前项目已处理完成的{{ viewLabel }}文件。</div>
        </el-form-item>
        <el-form-item label="挂接目标">
          <el-segmented v-model="form.targetMode" :options="targetModeOptions" />
          <div class="field-hint">挂接目标用于说明这份文件对应哪个工程部位或管理对象。</div>
        </el-form-item>
        <el-form-item v-if="form.targetMode === 'SECTION'" label="工程部位">
          <el-select v-model="form.sectionNodeId" filterable>
            <el-option v-for="node in sectionOptions" :key="node.id" :label="node.label" :value="node.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.targetMode === 'OBJECT'" label="管理对象">
          <el-select v-model="form.managedObjectId" filterable>
            <el-option v-for="object in objects" :key="object.id" :label="`${object.name} | ${object.code}`" :value="object.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="512" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Link, Refresh } from '@element-plus/icons-vue';

import { fetchFileResourcesPage, fetchManagedObjects, type FileResource, type ManagedObject } from '@/modules/data-steward/api/dataSteward';
import { fetchDeliverableTypes, fetchSectionTree, type DeliverableType, type SectionNode } from '@/modules/master-data/api/masterData';
import {
  createDeliveryBinding, fetchDeliveryCompleteness, fetchDeliveryView,
  submitForReview, approveBinding, rejectBinding, fetchReviewRecords,
  exportDeliveryCompletenessCsv, exportReviewSummaryCsv,
  type DeliveryBinding, type DeliveryCompletenessRow, type DeliveryView, type ReviewRecordItem
} from '@/modules/work-center/api/delivery';
import { useAuthStore } from '@/stores/auth';

const props = defineProps<{
  viewType: 'DOCUMENT' | 'DRAWING';
  title: string;
}>();

const authStore = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const view = ref<DeliveryView | null>(null);
const files = ref<FileResource[]>([]);
const filesLoading = ref(false);
const objects = ref<ManagedObject[]>([]);
const allTypes = ref<DeliverableType[]>([]);
const sections = ref<SectionNode[]>([]);
const completeness = ref<Awaited<ReturnType<typeof fetchDeliveryCompleteness>> | null>(null);
const completenessLoading = ref(false);
const activeTab = ref('bound');
let fileAbortController: AbortController | null = null;

const form = reactive({
  deliverableTypeId: null as number | null,
  fileResourceId: null as number | null,
  targetMode: 'SECTION',
  sectionNodeId: null as number | null,
  managedObjectId: null as number | null,
  remark: ''
});

const targetModeOptions = [
  { label: '部位', value: 'SECTION' },
  { label: '对象', value: 'OBJECT' }
];

const projectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => authStore.currentUser?.currentProject.name ?? '等待项目上下文');
const viewLabel = computed(() => (props.viewType === 'DOCUMENT' ? '文档' : '图纸'));
const deliverableTypes = computed(() => allTypes.value.filter((type) => type.fileKind === props.viewType));
const sectionOptions = computed(() => flattenSections(sections.value));
const missingRows = computed(() => completeness.value?.rows?.filter((r) => !r.completed) ?? []);
const readinessFixText = computed(() => {
  const issues = completeness.value?.readinessIssues ?? [];
  if (issues.some((issue) => issue.includes('当前视图类型') || issue.includes('交付物类型'))) {
    return `当前缺少${viewLabel.value}类交付物类型。请到交付物标准中为这个视图补一条文件类型为 ${props.viewType} 的交付物类型。`;
  }
  if (issues.some((issue) => issue.includes('节点类型'))) {
    return '请先到节点类型页面确认并锁定节点类型，再回到交付物标准配置资料要求。';
  }
  if (issues.some((issue) => issue.includes('部位'))) {
    return '请先建立工程部位树，平台会按部位树生成后续交付缺失项。';
  }
  return '请按工程部位树、节点类型、交付物标准和目录模板的顺序补齐前置条件。';
});
const progressColor = computed(() => {
  const rate = completeness.value?.completionRate ?? 0;
  if (rate >= 0.8) return '#67c23a';
  if (rate >= 0.5) return '#e6a23c';
  return '#f56c6c';
});

watch(projectId, () => loadPage(), { immediate: true });

async function loadPage() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [nextView, nextObjects, nextTypes, nextSections] = await Promise.all([
      fetchDeliveryView(projectId.value, props.viewType),
      fetchManagedObjects(projectId.value),
      fetchDeliverableTypes(projectId.value),
      fetchSectionTree(projectId.value)
    ]);
    view.value = nextView;
    objects.value = nextObjects;
    allTypes.value = nextTypes;
    sections.value = nextSections;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '交付视图加载失败');
  } finally {
    loading.value = false;
  }
  loadCompleteness();
}

// Review state
const reviewLoading = reactive<Record<string, boolean>>({});
const rejectDialogVisible = ref(false);
const rejectReason = ref('');
const rejecting = ref(false);
const rejectTarget = ref<DeliveryBinding | null>(null);
const recordsDrawerVisible = ref(false);
const reviewRecords = ref<ReviewRecordItem[]>([]);

function reviewTagType(status: string) {
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'danger';
  if (status === 'PENDING') return 'warning';
  return 'info';
}

function reviewLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' };
  return map[status] ?? status;
}

function recordColor(action: string) {
  if (action === 'APPROVED') return '#67c23a';
  if (action === 'REJECTED') return '#f56c6c';
  if (action === 'SUBMITTED') return '#409eff';
  return '#909399';
}

function recordActionLabel(action: string) {
  const map: Record<string, string> = { SUBMITTED: '提交审核', APPROVED: '审核通过', REJECTED: '驳回' };
  return map[action] ?? action;
}

async function handleSubmitReview(row: DeliveryBinding) {
  if (!projectId.value) return;
  const key = `submit-${row.id}`;
  reviewLoading[key] = true;
  try {
    await submitForReview(projectId.value, row.id);
    ElMessage.success('已提交审核');
    await loadPage();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '提交审核失败');
  } finally {
    reviewLoading[key] = false;
  }
}

async function handleApprove(row: DeliveryBinding) {
  if (!projectId.value) return;
  const key = `approve-${row.id}`;
  reviewLoading[key] = true;
  try {
    await approveBinding(projectId.value, row.id);
    ElMessage.success('审核已通过');
    await loadPage();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '审核通过失败');
  } finally {
    reviewLoading[key] = false;
  }
}

function openRejectDialog(row: DeliveryBinding) {
  rejectTarget.value = row;
  rejectReason.value = '';
  rejectDialogVisible.value = true;
}

async function handleReject() {
  if (!projectId.value || !rejectTarget.value || !rejectReason.value.trim()) return;
  rejecting.value = true;
  try {
    await rejectBinding(projectId.value, rejectTarget.value.id, rejectReason.value.trim());
    ElMessage.success('已驳回并生成整改项');
    rejectDialogVisible.value = false;
    await loadPage();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '驳回失败');
  } finally {
    rejecting.value = false;
  }
}

async function openReviewRecords(row: DeliveryBinding) {
  if (!projectId.value) return;
  try {
    reviewRecords.value = await fetchReviewRecords(projectId.value, row.id) ?? [];
  } catch {
    reviewRecords.value = [];
  }
  recordsDrawerVisible.value = true;
}

async function loadCompleteness() {
  if (!projectId.value) return;
  completenessLoading.value = true;
  try {
    completeness.value = await fetchDeliveryCompleteness(projectId.value, props.viewType);
  } catch {
    completeness.value = null;
  } finally {
    completenessLoading.value = false;
  }
}

async function searchFiles(keyword: string) {
  if (!projectId.value) return;
  if (fileAbortController) fileAbortController.abort();
  fileAbortController = new AbortController();
  filesLoading.value = true;
  try {
    const result = await fetchFileResourcesPage(
      projectId.value,
      {
        fileKind: props.viewType,
        keyword: keyword || undefined,
        processStatus: 'PROCESSED',
        pageNo: 1,
        pageSize: 20
      },
      fileAbortController.signal
    );
    files.value = result.rows;
  } catch {
    // aborted or error - ignore
  } finally {
    filesLoading.value = false;
  }
}

function openCreateDialog() {
  Object.assign(form, {
    deliverableTypeId: deliverableTypes.value[0]?.id ?? null,
    fileResourceId: null,
    targetMode: 'SECTION',
    sectionNodeId: sectionOptions.value[0]?.id ?? null,
    managedObjectId: objects.value[0]?.id ?? null,
    remark: ''
  });
  files.value = [];
  dialogVisible.value = true;
}

function openBindFromMissing(row: DeliveryCompletenessRow) {
  Object.assign(form, {
    deliverableTypeId: row.deliverableTypeId,
    fileResourceId: null,
    targetMode: row.targetType === 'OBJECT' ? 'OBJECT' : 'SECTION',
    sectionNodeId: row.targetType === 'SECTION' ? row.targetId : sectionOptions.value[0]?.id ?? null,
    managedObjectId: row.targetType === 'OBJECT' ? row.targetId : objects.value[0]?.id ?? null,
    remark: ''
  });
  files.value = [];
  dialogVisible.value = true;
}

async function handleSave() {
  if (!projectId.value || !form.deliverableTypeId || !form.fileResourceId) return;
  saving.value = true;
  try {
    await createDeliveryBinding(projectId.value, {
      viewType: props.viewType,
      deliverableTypeId: form.deliverableTypeId,
      fileResourceId: form.fileResourceId,
      sectionNodeId: form.targetMode === 'SECTION' ? form.sectionNodeId : null,
      managedObjectId: form.targetMode === 'OBJECT' ? form.managedObjectId : null,
      bindingStatus: 'BOUND',
      reviewStatus: 'PENDING',
      remark: form.remark
    });
    ElMessage.success('交付挂接已保存');
    dialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

function handleExportCompleteness() {
  if (!projectId.value) return;
  exportDeliveryCompletenessCsv(projectId.value);
  ElMessage.success('交付完整率 CSV 导出已触发下载');
}

function handleExportReviewSummary() {
  if (!projectId.value) return;
  exportReviewSummaryCsv(projectId.value);
  ElMessage.success('审核汇总 CSV 导出已触发下载');
}

function goToDeliverableStandard() {
  router.push('/master-data/deliverable-standard');
}

function goToNodeTypes() {
  router.push('/master-data/node-types');
}

function flattenSections(nodes: SectionNode[], prefix = ''): Array<{ id: number; label: string }> {
  return nodes.flatMap((node) => {
    const label = `${prefix}${node.name}`;
    return [{ id: node.id, label }, ...flattenSections(node.children ?? [], `${prefix} / `)];
  });
}
</script>

<style scoped>
.completeness-card {
  background: var(--el-fill-color-lighter, #f5f7fa);
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}
.completeness-card--empty {
  text-align: center;
  color: var(--el-text-color-secondary);
}
.completeness-card__summary {
  margin-bottom: 12px;
  font-size: 15px;
  line-height: 1.6;
}
.text-success {
  color: var(--el-color-success, #67c23a);
}
.text-danger {
  color: var(--el-color-danger, #f56c6c);
}
.issue-list {
  margin: 4px 0 0;
  padding-left: 20px;
}
.issue-list li {
  margin-bottom: 2px;
}
.readiness-help {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}
.readiness-help p,
.tab-helper {
  margin: 0;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
.readiness-help__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tab-helper {
  margin-bottom: 12px;
}
.mb {
  margin-bottom: 16px;
}
.mvp-tabs {
  margin-top: 12px;
}
</style>
