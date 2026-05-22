<template>
  <section class="mvp-page package-archive-page">
    <ProjectWorkspaceNav v-if="Number.isFinite(projectId)" :project-id="projectId" />

    <header class="package-archive-hero">
      <div>
        <span class="zy-code-chip">M2C</span>
        <h1>交付包 / 档案目录</h1>
        <p>
          基于交付预检查生成清单草案和语义档案目录。这里只保存目录级快照，不生成真实压缩包，
          不访问、不复制、不移动 NAS 文件。
        </p>
      </div>
      <div class="package-archive-hero__actions">
        <el-button :loading="loading" @click="loadPage">刷新</el-button>
        <el-button type="primary" :loading="creating" @click="createDraft">生成草案</el-button>
      </div>
    </header>

    <el-alert
      class="package-archive-notice"
      type="info"
      show-icon
      :closable="false"
      title="这是只读清单草案，不代表正式导出已完成。"
      description="平台不会生成真实交付包，不会复制 NAS 文件，也不会读取 PDF、Office、DWG、RVT 或模型正文。"
    />

    <section class="package-archive-toolbar">
      <div>
        <span>视图</span>
        <el-segmented v-model="viewType" :options="viewTypeOptions" />
      </div>
      <div>
        <span>目标</span>
        <el-segmented v-model="targetType" :options="targetTypeOptions" />
      </div>
      <el-button :loading="loading" @click="loadPrepare">重新预检查</el-button>
    </section>

    <section class="package-archive-grid" v-loading="loading">
      <article v-for="item in summaryCards" :key="item.label" class="package-archive-kpi">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.helper }}</em>
      </article>
    </section>

    <section class="package-archive-panel">
      <header>
        <div>
          <h2>档案目录预览</h2>
          <p>目录由视图、目标、交付定义、交付物类型和文件名组合而成，只是语义归档建议。</p>
        </div>
        <el-tag effect="plain">{{ prepareRows.length }} 条</el-tag>
      </header>
      <el-table :data="previewRows" border>
        <el-table-column label="档案目录" min-width="280">
          <template #default="{ row }">
            <span class="package-archive-path">{{ row.archiveDirectoryPath }}</span>
          </template>
        </el-table-column>
        <el-table-column label="目标" min-width="140">
          <template #default="{ row }">
            {{ targetTypeLabel(row.targetType) }} · {{ row.targetName || '未命名目标' }}
          </template>
        </el-table-column>
        <el-table-column label="交付物" min-width="180">
          <template #default="{ row }">
            <strong>{{ row.deliverableDefinitionName || '未命名定义' }}</strong>
            <span>{{ row.deliverableTypeName || '未命名类型' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="文件" min-width="180">
          <template #default="{ row }">
            <strong>{{ row.fileName || '未挂接文件' }}</strong>
            <span>{{ row.fileId ? `ID ${row.fileId}` : '缺失' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="160">
          <template #default="{ row }">
            <el-tag :type="exportStatusType(row.exportStatus)" effect="plain">
              {{ exportStatusLabel(row.exportStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="阻塞原因" min-width="180">
          <template #default="{ row }">
            {{ row.blockReason || '无' }}
          </template>
        </el-table-column>
      </el-table>
      <p v-if="prepareRows.length > previewRows.length" class="package-archive-more">
        当前仅预览前 {{ previewRows.length }} 条，保存草案会记录完整清单。
      </p>
      <el-empty v-if="!loading && prepareRows.length === 0" description="暂无可生成的交付清单" :image-size="72" />
    </section>

    <section class="package-archive-panel">
      <header>
        <div>
          <h2>草案历史</h2>
          <p>草案是当时交付预检查结果的快照，可用于沟通档案目录和导出清单。</p>
        </div>
        <el-tag effect="plain">{{ drafts.length }} 份</el-tag>
      </header>
      <el-table :data="drafts" border>
        <el-table-column label="草案" width="100">
          <template #default="{ row }">#{{ row.draftId }}</template>
        </el-table-column>
        <el-table-column label="范围" min-width="150">
          <template #default="{ row }">
            {{ viewTypeLabel(row.viewType) }} / {{ targetTypeLabel(row.targetType) }}
          </template>
        </el-table-column>
        <el-table-column label="统计" min-width="220">
          <template #default="{ row }">
            共 {{ row.totalCount }}，可归档 {{ row.readyCount }}，阻塞 {{ row.blockedCount }}，
            缺失 {{ row.missingCount }}
          </template>
        </el-table-column>
        <el-table-column label="安全标记" min-width="220">
          <template #default="{ row }">
            dry-run={{ row.dryRun ? 'true' : 'false' }}，
            真实包={{ row.physicalPackageGenerated ? '已生成' : '未生成' }}，
            NAS复制={{ row.nasFileCopied ? '已复制' : '未复制' }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openDraft(row.draftId)">查看</el-button>
            <el-button text @click="downloadManifest(row.draftId)">导出清单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && drafts.length === 0" description="还没有草案，可先生成一份只读清单" :image-size="72" />
    </section>

    <el-drawer v-model="detailVisible" size="70%" title="交付包草案详情">
      <template v-if="selectedDraft">
        <section class="package-archive-detail">
          <div class="package-archive-detail__summary">
            <strong>草案 #{{ selectedDraft.draftId }}</strong>
            <span>{{ viewTypeLabel(selectedDraft.viewType) }} / {{ targetTypeLabel(selectedDraft.targetType) }}</span>
            <el-tag type="info" effect="plain">dry-run</el-tag>
            <el-tag type="success" effect="plain">未生成真实包</el-tag>
            <el-tag type="success" effect="plain">未复制 NAS 文件</el-tag>
          </div>
          <el-table :data="selectedDraft.rows" border height="520">
            <el-table-column label="档案目录" min-width="320" prop="archiveDirectoryPath" />
            <el-table-column label="文件ID" width="100">
              <template #default="{ row }">{{ row.fileId || '-' }}</template>
            </el-table-column>
            <el-table-column label="文件名" min-width="180">
              <template #default="{ row }">{{ row.fileName || '未挂接文件' }}</template>
            </el-table-column>
            <el-table-column label="审核" width="120">
              <template #default="{ row }">{{ reviewStatusLabel(row.reviewStatus) }}</template>
            </el-table-column>
            <el-table-column label="预览" width="130">
              <template #default="{ row }">{{ previewStatusLabel(row.previewStatus) }}</template>
            </el-table-column>
            <el-table-column label="导出状态" width="130">
              <template #default="{ row }">
                <el-tag :type="exportStatusType(row.exportStatus)" effect="plain">
                  {{ exportStatusLabel(row.exportStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="阻塞原因" min-width="180">
              <template #default="{ row }">{{ row.blockReason || '无' }}</template>
            </el-table-column>
          </el-table>
        </section>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';

import ProjectWorkspaceNav from '@/modules/core/components/ProjectWorkspaceNav.vue';
import {
  createDeliveryPackageDraft,
  exportDeliveryPackageManifest,
  fetchDeliveryPackageDraft,
  fetchDeliveryPackageDrafts,
  fetchDeliveryPackagePrepare,
  type DeliveryPackageArchiveItem,
  type DeliveryPackageDraftDetail,
  type DeliveryPackageDraftSummary,
  type DeliveryPackagePrepareResponse
} from '@/modules/work-center/api/delivery';

const route = useRoute();
const router = useRouter();

const projectId = computed(() => Number(route.params.projectId));
const viewType = ref<'ALL' | 'DOCUMENT' | 'DRAWING'>('ALL');
const targetType = ref<'SECTION' | 'OBJECT'>('SECTION');
const loading = ref(false);
const creating = ref(false);
const detailVisible = ref(false);
const prepare = ref<DeliveryPackagePrepareResponse | null>(null);
const drafts = ref<DeliveryPackageDraftSummary[]>([]);
const selectedDraft = ref<DeliveryPackageDraftDetail | null>(null);

const viewTypeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '文档', value: 'DOCUMENT' },
  { label: '图纸', value: 'DRAWING' }
];
const targetTypeOptions = [
  { label: '部位', value: 'SECTION' },
  { label: '对象', value: 'OBJECT' }
];

const prepareRows = computed(() => prepare.value?.rows ?? []);
const previewRows = computed(() => prepareRows.value.slice(0, 30));
const requestViewType = computed(() => viewType.value === 'ALL' ? undefined : viewType.value);
const summaryCards = computed(() => [
  { label: '应交项', value: prepare.value?.totalCount ?? 0, helper: '来自交付预检查' },
  { label: '可归档', value: prepare.value?.readyCount ?? 0, helper: '审核通过且可列入清单' },
  { label: '阻塞', value: prepare.value?.blockedCount ?? 0, helper: '缺失、待审或被驳回' },
  { label: '需转换', value: prepare.value?.conversionRequiredCount ?? 0, helper: '仅标记，不执行转换' },
  { label: '不支持预览', value: prepare.value?.unsupportedPreviewCount ?? 0, helper: '仍不读取文件正文' }
]);

onMounted(() => {
  if (!Number.isFinite(projectId.value)) {
    void router.push({ name: 'data-steward-assets' });
    return;
  }
  void loadPage();
});

watch([viewType, targetType], () => {
  void loadPrepare();
});

async function loadPage() {
  loading.value = true;
  try {
    await Promise.all([loadPrepare(), loadDrafts()]);
  } finally {
    loading.value = false;
  }
}

async function loadPrepare() {
  if (!Number.isFinite(projectId.value)) return;
  prepare.value = await fetchDeliveryPackagePrepare(projectId.value, requestViewType.value, targetType.value);
}

async function loadDrafts() {
  if (!Number.isFinite(projectId.value)) return;
  drafts.value = await fetchDeliveryPackageDrafts(projectId.value);
}

async function createDraft() {
  if (!Number.isFinite(projectId.value)) return;
  creating.value = true;
  try {
    const detail = await createDeliveryPackageDraft(projectId.value, {
      viewType: requestViewType.value,
      targetType: targetType.value
    });
    selectedDraft.value = detail;
    detailVisible.value = true;
    await loadDrafts();
    ElMessage.success('已生成只读草案；未生成真实交付包，未复制 NAS 文件。');
  } finally {
    creating.value = false;
  }
}

async function openDraft(draftId: number) {
  selectedDraft.value = await fetchDeliveryPackageDraft(projectId.value, draftId);
  detailVisible.value = true;
}

async function downloadManifest(draftId: number) {
  await exportDeliveryPackageManifest(projectId.value, draftId);
  ElMessage.success('正在导出清单 CSV；这不是正式交付包。');
}

function viewTypeLabel(value: string) {
  if (value === 'DOCUMENT') return '文档';
  if (value === 'DRAWING') return '图纸';
  return '全部';
}

function targetTypeLabel(value: string) {
  return value === 'OBJECT' ? '对象' : '部位';
}

function exportStatusLabel(value: string) {
  const labels: Record<string, string> = {
    READY: '可归档',
    MISSING: '缺失',
    REVIEW_REQUIRED: '待审核',
    REJECTED: '已驳回',
    BLOCKED: '阻塞'
  };
  return labels[value] ?? value;
}

function exportStatusType(value: string) {
  if (value === 'READY') return 'success';
  if (value === 'MISSING' || value === 'REJECTED') return 'danger';
  if (value === 'REVIEW_REQUIRED') return 'warning';
  return 'info';
}

function reviewStatusLabel(value: string | null) {
  const labels: Record<string, string> = {
    APPROVED: '已通过',
    PENDING: '待审核',
    DRAFT: '草稿',
    REJECTED: '已驳回'
  };
  return value ? labels[value] ?? value : '未挂接';
}

function previewStatusLabel(value: string | null) {
  const labels: Record<string, string> = {
    AVAILABLE: '可预览',
    NEEDS_CONVERSION: '需转换',
    UNSUPPORTED: '不支持'
  };
  return value ? labels[value] ?? value : '未判断';
}

function formatDateTime(value: string | null) {
  if (!value) return '-';
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}
</script>

<style scoped>
.package-archive-page {
  display: grid;
  gap: var(--zy-sp-4);
}

.package-archive-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--zy-sp-4);
  padding: var(--zy-sp-6);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: linear-gradient(180deg, var(--zy-surface) 0%, var(--zy-surface-soft) 100%);
  box-shadow: var(--zy-shadow-xs);
}

.package-archive-hero h1 {
  margin: var(--zy-sp-2) 0;
  color: var(--zy-ink);
  font-size: var(--zy-fs-3xl);
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0;
}

.package-archive-hero p {
  max-width: 720px;
  margin: 0;
  color: var(--zy-muted);
  font-size: var(--zy-fs-base);
  line-height: 1.7;
}

.package-archive-hero__actions {
  display: flex;
  gap: var(--zy-sp-2);
  flex: 0 0 auto;
}

.package-archive-notice {
  border-radius: var(--zy-radius-base);
}

.package-archive-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--zy-sp-3);
  padding: var(--zy-sp-4);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
}

.package-archive-toolbar > div {
  display: flex;
  align-items: center;
  gap: var(--zy-sp-2);
}

.package-archive-toolbar span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
}

.package-archive-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--zy-sp-3);
}

.package-archive-kpi,
.package-archive-panel {
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
}

.package-archive-kpi {
  display: grid;
  gap: var(--zy-sp-1);
  padding: var(--zy-sp-4);
}

.package-archive-kpi span,
.package-archive-kpi em {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
}

.package-archive-kpi strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-2xl);
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0;
}

.package-archive-panel {
  display: grid;
  gap: var(--zy-sp-3);
  padding: var(--zy-sp-4);
}

.package-archive-panel > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--zy-sp-3);
}

.package-archive-panel h2 {
  margin: 0 0 var(--zy-sp-1);
  color: var(--zy-ink);
  font-size: var(--zy-fs-xl);
  font-weight: var(--zy-fw-semi);
  letter-spacing: 0;
}

.package-archive-panel p {
  margin: 0;
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
}

.package-archive-path {
  color: var(--zy-ink);
  font-family: var(--zy-font-mono);
  font-size: var(--zy-fs-xs);
  word-break: break-all;
}

.package-archive-panel :deep(.el-table strong) {
  display: block;
  color: var(--zy-ink);
  font-weight: var(--zy-fw-semi);
}

.package-archive-panel :deep(.el-table span) {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.package-archive-more {
  padding: var(--zy-sp-2) var(--zy-sp-3);
  border-radius: var(--zy-radius-base);
  background: var(--zy-blue-50);
  color: var(--zy-blue-700) !important;
}

.package-archive-detail {
  display: grid;
  gap: var(--zy-sp-3);
}

.package-archive-detail__summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  padding: var(--zy-sp-3);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
}

.package-archive-detail__summary strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-lg);
}

.package-archive-detail__summary span {
  color: var(--zy-muted);
}

@media (max-width: 1100px) {
  .package-archive-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .package-archive-hero {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .package-archive-grid {
    grid-template-columns: 1fr;
  }

  .package-archive-hero__actions {
    width: 100%;
  }

  .package-archive-hero__actions :deep(.el-button) {
    flex: 1 1 0;
  }
}
</style>
