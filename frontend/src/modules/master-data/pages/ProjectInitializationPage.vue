<template>
  <section class="initialization-page">
    <div class="initialization-page__header">
      <div>
        <h1>真实项目接入向导</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadPage">刷新</el-button>
    </div>

    <section class="initialization-hero">
      <div>
        <span class="initialization-hero__eyebrow">G2 真实项目接入</span>
        <h2>先评估真实项目目录，再生成待确认主数据草案</h2>
        <p>
          接入向导只读取项目目录元数据和受控路径映射状态，不读取文件正文，不访问或复制 NAS 文件。模板内容只是草案，应用前需要人工确认。
        </p>
      </div>
      <el-tag size="large" :type="status?.ready ? 'success' : 'warning'">
        {{ status?.ready ? '标准已就绪' : '待初始化' }}
      </el-tag>
    </section>

    <div class="initialization-status">
      <article
        v-for="item in statusCards"
        :key="item.key"
        class="initialization-status__card"
        :class="{ 'is-ready': item.ready }"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.ready ? '已具备' : '待补齐' }}</small>
      </article>
    </div>

    <el-alert
      v-if="status?.blockers?.length"
      class="initialization-alert"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #title>当前项目还缺少标准前置条件</template>
      <ul>
        <li v-for="blocker in status.blockers" :key="blocker">{{ blocker }}</li>
      </ul>
    </el-alert>

    <section class="initialization-panel">
      <div class="initialization-panel__header">
        <div>
          <h2>接入评估</h2>
          <p>基于 catalog-only 证据判断真实项目是否已有路径映射、资产目录和主数据底座。</p>
        </div>
        <el-tag size="large" type="info">{{ onboardingStatusText(assessment?.onboardingStatus) }}</el-tag>
      </div>

      <div class="onboarding-summary">
        <article v-for="item in onboardingCards" :key="item.label" class="onboarding-summary__card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.unit }}</small>
        </article>
      </div>

      <div class="onboarding-columns">
        <section>
          <h3>目录线索</h3>
          <el-empty v-if="!assessment?.evidenceClues?.length" description="暂无目录线索" />
          <ul v-else class="onboarding-list">
            <li v-for="clue in assessment.evidenceClues" :key="clue.clueType">
              <strong>{{ clue.label }}</strong>
              <span>{{ clue.description }}</span>
            </li>
          </ul>
        </section>
        <section>
          <h3>缺口与边界</h3>
          <el-empty v-if="!assessment?.gaps?.length" description="暂无明显缺口" />
          <ul v-else class="onboarding-list">
            <li v-for="gap in assessment.gaps" :key="gap.code">
              <strong>{{ gap.description }}</strong>
              <span>{{ gap.missingEvidenceReason }}</span>
            </li>
          </ul>
        </section>
      </div>
    </section>

    <div class="initialization-grid">
      <section class="initialization-panel">
        <div class="initialization-panel__header">
          <div>
            <h2>接入草案模板</h2>
            <p>先用稳定的建筑机电/BIM草案补齐标准底座，再由项目负责人复核。</p>
          </div>
        </div>
        <div class="template-list" v-loading="loadingTemplates">
          <button
            v-for="template in templates"
            :key="template.templateCode"
            class="template-card"
            :class="{ 'is-active': selectedTemplateCode === template.templateCode }"
            type="button"
            @click="selectTemplate(template.templateCode)"
          >
            <span>{{ template.industryType }}</span>
            <strong>{{ template.templateName }}</strong>
            <small>{{ template.description }}</small>
            <em>{{ totalTemplateItems(template.counts) }} 个标准项</em>
          </button>
        </div>
      </section>

      <section class="initialization-panel">
        <div class="initialization-panel__header">
          <div>
            <h2>草案内容</h2>
            <p>预览将要补齐的部位、节点类型、交付物和目录；所有项默认待人工确认。</p>
          </div>
          <el-button :disabled="!selectedTemplateCode" :loading="previewing" @click="() => handlePreview()">
            预览草案
          </el-button>
        </div>

        <el-table
          :data="templateDetail?.items ?? []"
          class="initialization-table"
          max-height="360"
          empty-text="请选择模板"
        >
          <el-table-column prop="category" label="类别" width="170" />
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="code" label="编码" min-width="160" />
          <el-table-column prop="fileKind" label="文件类型" width="110">
            <template #default="{ row }">{{ row.fileKind || '-' }}</template>
          </el-table-column>
        </el-table>
      </section>
    </div>

    <section class="initialization-panel">
      <div class="initialization-panel__header">
        <div>
          <h2>草案预览</h2>
          <p>这是只读预检查，不生成交付结论，不读取文件正文，不触碰 NAS。确认无阻塞后再应用草案。</p>
        </div>
        <el-button
          type="primary"
          :disabled="!preview || preview.blocked"
          :loading="applying"
          @click="confirmApply"
        >
          确认应用草案
        </el-button>
      </div>

      <div v-if="preview" class="preview-summary">
        <el-alert
          v-if="onboardingPreview"
          type="info"
          :closable="false"
          show-icon
          title="草案基于 catalog-only 目录证据生成：不会访问、复制、移动、改名或删除 NAS 文件，也不代表真实项目接入已经完成。"
        />
        <div class="preview-summary__counts">
          <span>将创建 {{ totalTemplateItems(preview.willCreate) }} 项</span>
          <span>将跳过 {{ totalTemplateItems(preview.willSkip) }} 项</span>
          <span :class="{ danger: preview.blocked }">{{ preview.blocked ? '存在阻塞' : '可应用' }}</span>
        </div>
        <div v-if="onboardingPreview?.warnings?.length" class="preview-warnings">
          <el-tag v-for="warning in onboardingPreview.warnings" :key="warning" type="info">{{ warning }}</el-tag>
        </div>
        <el-alert v-if="preview.blockReasons.length" type="error" :closable="false" show-icon>
          <template #title>模板暂不能应用</template>
          <ul>
            <li v-for="reason in preview.blockReasons" :key="reason">{{ reason }}</li>
          </ul>
        </el-alert>
        <el-alert v-if="preview.conflicts.length" type="error" :closable="false" show-icon>
          <template #title>发现编码或名称冲突</template>
          <ul>
            <li v-for="conflict in preview.conflicts" :key="conflict">{{ conflict }}</li>
          </ul>
        </el-alert>
        <el-table :data="preview.items" class="initialization-table" max-height="320" empty-text="暂无预览结果">
          <el-table-column prop="category" label="类别" width="170" />
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="code" label="编码" min-width="160" />
          <el-table-column prop="action" label="动作" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="actionType(row.action)">{{ actionText(row.action) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="说明" min-width="180" />
        </el-table>
      </div>
      <el-empty v-else description="请选择模板并点击预览草案" />
    </section>

    <section v-if="applyResult" class="initialization-result">
      <div>
        <h2>接入草案已应用</h2>
        <p>本次创建 {{ totalTemplateItems(applyResult.created) }} 项，跳过 {{ totalTemplateItems(applyResult.skipped) }} 项。</p>
      </div>
      <div class="initialization-result__actions">
        <el-button @click="router.push({ name: 'project-master-data-sections', params: { projectId } })">查看部位树</el-button>
        <el-button @click="router.push({ name: 'project-master-data-deliverable-standard', params: { projectId } })">
          查看交付物标准
        </el-button>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import {
  applyOnboardingDraft,
  fetchInitializationStatus,
  fetchOnboardingAssessment,
  fetchOnboardingPreview,
  fetchStandardTemplateDetail,
  fetchStandardTemplates,
  type InitializationStatus,
  type OnboardingAssessment,
  type OnboardingDraftPreview,
  type StandardTemplateDetail,
  type StandardTemplateSummary,
  type TemplateApplyResult,
  type TemplateCounts,
  type TemplatePreview
} from '@/modules/master-data/api/masterData';
import { useProjectWorkspaceContext } from '@/modules/core/composables/useProjectWorkspaceContext';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();
const { workspaceProjectId } = useProjectWorkspaceContext();

const loading = ref(false);
const loadingTemplates = ref(false);
const previewing = ref(false);
const applying = ref(false);
const status = ref<InitializationStatus | null>(null);
const assessment = ref<OnboardingAssessment | null>(null);
const templates = ref<StandardTemplateSummary[]>([]);
const templateDetail = ref<StandardTemplateDetail | null>(null);
const selectedTemplateCode = ref('');
const preview = ref<TemplatePreview | null>(null);
const onboardingPreview = ref<OnboardingDraftPreview | null>(null);
const applyResult = ref<TemplateApplyResult | null>(null);

const projectId = computed(() => workspaceProjectId.value ?? 0);
const projectLabel = computed(() => {
  const project = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return project ? `${project.code} | ${project.name}` : `项目 ${projectId.value || '-'}`;
});

const statusCards = computed(() => {
  const standardStatus = status.value?.standardStatus;
  return [
    { key: 'section', label: '部位节点', value: standardStatus?.sectionNodeCount ?? 0, ready: Boolean(standardStatus?.hasSectionTree) },
    { key: 'nodeType', label: '节点类型', value: standardStatus?.nodeTypeCount ?? 0, ready: Boolean(standardStatus?.hasNodeTypes) },
    { key: 'locked', label: '节点锁定', value: standardStatus?.nodeTypesLocked ? '已锁定' : '未锁定', ready: Boolean(standardStatus?.nodeTypesLocked) },
    { key: 'definition', label: '交付定义', value: standardStatus?.deliverableDefinitionCount ?? 0, ready: Boolean(standardStatus?.hasDeliverableDefinitions) },
    { key: 'type', label: '交付类型', value: standardStatus?.deliverableTypeCount ?? 0, ready: Boolean(standardStatus?.hasDeliverableTypes) },
    { key: 'attribute', label: '交付属性', value: standardStatus?.deliverableAttributeCount ?? 0, ready: Boolean(standardStatus?.hasDeliverableAttributes) },
    { key: 'template', label: '目录模板', value: standardStatus?.directoryTemplateCount ?? 0, ready: Boolean(standardStatus?.hasDirectoryTemplates) }
  ];
});

const onboardingCards = computed(() => {
  const summary = assessment.value?.assetSummary;
  return [
    { label: '目录文件', value: formatCount(summary?.fileCount), unit: '已登记' },
    { label: '模型文件', value: formatCount(summary?.modelFileCount), unit: 'catalog-only' },
    { label: '图纸文件', value: formatCount(summary?.drawingFileCount), unit: 'catalog-only' },
    { label: '文档文件', value: formatCount(summary?.documentFileCount), unit: 'catalog-only' },
    { label: '路径映射', value: formatCount(summary?.pathMappingCount), unit: '不展示原始路径' },
    { label: '最近扫描', value: formatDate(summary?.lastScanAt || summary?.lastAssetSeenAt), unit: '只读记录' }
  ];
});

watch(
  projectId,
  () => {
    loadPage();
  },
  { immediate: true }
);

async function loadPage() {
  if (!projectId.value) return;
  loading.value = true;
  loadingTemplates.value = true;
  applyResult.value = null;
  try {
    const [nextStatus, nextAssessment, nextTemplates] = await Promise.all([
      fetchInitializationStatus(projectId.value),
      fetchOnboardingAssessment(projectId.value),
      fetchStandardTemplates()
    ]);
    status.value = nextStatus;
    assessment.value = nextAssessment;
    templates.value = nextTemplates;
    const nextTemplate = selectedTemplateCode.value || nextTemplates[0]?.templateCode || '';
    if (nextTemplate) {
      await selectTemplate(nextTemplate);
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '初始化信息加载失败');
  } finally {
    loading.value = false;
    loadingTemplates.value = false;
  }
}

async function selectTemplate(templateCode: string) {
  selectedTemplateCode.value = templateCode;
  preview.value = null;
  onboardingPreview.value = null;
  try {
    templateDetail.value = await fetchStandardTemplateDetail(templateCode);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '模板详情加载失败');
  }
}

async function handlePreview(resetApplyResult = true) {
  if (!projectId.value || !selectedTemplateCode.value) return;
  previewing.value = true;
  if (resetApplyResult) {
    applyResult.value = null;
  }
  try {
    onboardingPreview.value = await fetchOnboardingPreview(projectId.value, selectedTemplateCode.value);
    preview.value = onboardingPreview.value.templatePreview;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '接入草案预览失败');
  } finally {
    previewing.value = false;
  }
}

async function confirmApply() {
  if (!projectId.value || !selectedTemplateCode.value) return;
  try {
    await ElMessageBox.confirm(
      '确认应用当前接入草案？本操作仅基于目录元数据和模板草案补齐主数据，不读取文件正文，不触碰 NAS 文件，生成内容仍需人工复核。',
      '确认应用接入草案',
      {
        type: 'warning',
        confirmButtonText: '确认应用',
        cancelButtonText: '取消'
      }
    );
    applying.value = true;
    const result = await applyOnboardingDraft(projectId.value, selectedTemplateCode.value);
    applyResult.value = result.templateResult;
    ElMessage.success('接入草案已应用，请继续人工复核');
    await Promise.all([loadStatus(), loadAssessment(), handlePreview(false)]);
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '接入草案应用失败');
    }
  } finally {
    applying.value = false;
  }
}

async function loadStatus() {
  if (!projectId.value) return;
  status.value = await fetchInitializationStatus(projectId.value);
}

async function loadAssessment() {
  if (!projectId.value) return;
  assessment.value = await fetchOnboardingAssessment(projectId.value);
}

function totalTemplateItems(counts?: TemplateCounts | null) {
  if (!counts) return 0;
  return counts.sectionNodes
    + counts.nodeTypes
    + counts.deliverableDefinitions
    + counts.deliverableTypes
    + counts.deliverableAttributes
    + counts.directoryTemplates;
}

function actionType(action: string) {
  if (action === 'CREATE') return 'success';
  if (action === 'CONFLICT') return 'danger';
  return 'info';
}

function actionText(action: string) {
  if (action === 'CREATE') return '创建';
  if (action === 'CONFLICT') return '冲突';
  return '跳过';
}

function onboardingStatusText(value?: string | null) {
  const labels: Record<string, string> = {
    GOVERNANCE_READY: '治理底座已就绪',
    MASTERDATA_INITIALIZED: '主数据已初始化',
    ASSETS_REGISTERED: '资产目录已登记',
    PATH_MAPPED: '路径已映射',
    NOT_ONBOARDED: '待接入'
  };
  return labels[value || ''] ?? '待评估';
}

function formatCount(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('zh-CN');
}

function formatDate(value: string | null | undefined) {
  if (!value) return '-';
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}
</script>

<style scoped>
.initialization-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.initialization-page__header,
.initialization-panel__header,
.initialization-result {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
}

.initialization-page__header h1,
.initialization-panel__header h2,
.initialization-result h2,
.initialization-hero h2 {
  margin: 0;
  color: #1f2937;
}

.initialization-page__header p,
.initialization-panel__header p,
.initialization-result p,
.initialization-hero p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.initialization-hero,
.initialization-panel,
.initialization-result {
  min-width: 0;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 8px;
  background: #fff;
}

.initialization-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.initialization-hero__eyebrow {
  display: inline-block;
  margin-bottom: 6px;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.initialization-status {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(128px, 1fr));
  gap: 10px;
  min-width: 0;
}

.initialization-status__card {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(251, 146, 60, 0.24);
  border-radius: 8px;
  background: #fff7ed;
}

.initialization-status__card.is-ready {
  border-color: rgba(20, 184, 166, 0.24);
  background: #f0fdfa;
}

.initialization-status__card span,
.initialization-status__card small {
  color: #64748b;
  font-size: 12px;
}

.initialization-status__card strong {
  color: #111827;
  font-size: 20px;
}

.onboarding-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
  margin-top: 14px;
  min-width: 0;
}

.onboarding-summary__card {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: #f8fafc;
}

.onboarding-summary__card span,
.onboarding-summary__card small {
  color: #64748b;
  font-size: 12px;
}

.onboarding-summary__card strong {
  color: #0f172a;
  font-size: 18px;
}

.onboarding-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 14px;
  min-width: 0;
}

.onboarding-columns h3 {
  margin: 0 0 8px;
  color: #1f2937;
  font-size: 14px;
}

.onboarding-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.onboarding-list li {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 8px;
  background: #fff;
}

.onboarding-list strong {
  color: #111827;
  font-size: 13px;
}

.onboarding-list span {
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.initialization-alert ul,
.preview-summary ul {
  margin: 6px 0 0;
  padding-left: 18px;
}

.initialization-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.75fr) minmax(0, 1.25fr);
  gap: 16px;
  min-width: 0;
}

.template-list {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.template-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  text-align: left;
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.template-card.is-active {
  border-color: #409eff;
  background: #ecf5ff;
}

.template-card span,
.template-card small,
.template-card em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  line-height: 1.5;
}

.template-card strong {
  color: #1f2937;
}

.initialization-table {
  width: 100%;
  min-width: 0;
}

.preview-summary {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.preview-summary__counts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-summary__counts span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f1f5f9;
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.preview-summary__counts .danger {
  background: #fef2f2;
  color: #b91c1c;
}

.preview-warnings {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-warnings :deep(.el-tag) {
  height: auto;
  line-height: 1.4;
  max-width: 100%;
  padding: 5px 8px;
  white-space: normal;
}

.initialization-result__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1120px) {
  .initialization-grid,
  .onboarding-columns {
    grid-template-columns: 1fr;
  }
}
</style>
