<template>
  <section class="initialization-page">
    <div class="initialization-page__header">
      <div>
        <span class="master-data-page__eyebrow">工程主数据</span>
        <h1>接入向导</h1>
        <p>{{ projectLabel }} · 从真实资产线索生成可复核的工程主数据草案。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadPage">刷新</el-button>
    </div>

    <MasterDataStepNav active="initialization" />

    <section class="initialization-hero">
      <div>
        <span class="initialization-hero__eyebrow">{{ heroEyebrow }}</span>
        <h2>{{ heroTitle }}</h2>
        <p>接入向导只根据资产目录、文件类型和项目线索生成草案。真正生效前，仍需要人工确认部位树、节点类型和交付物标准。</p>
      </div>
      <el-tag size="large" :type="status?.ready ? 'success' : 'warning'">
        {{ status?.ready ? '标准已就绪' : '待初始化' }}
      </el-tag>
    </section>

    <section class="master-workspace-callout">
      <div>
        <span>先定义规则</span>
        <strong>接入向导只生成草案，真正交付前还要复核三类规则</strong>
        <p>先确认部位树，再锁定节点类型，最后配置交付物标准。规则稳定后，文档 / 图纸交付页面才会准确计算应交和缺失。</p>
      </div>
      <div class="master-workspace-callout__actions">
        <el-button type="primary" @click="router.push({ name: 'project-master-data-sections', params: { projectId } })">查看部位树</el-button>
        <el-button @click="router.push({ name: 'project-master-data-node-types', params: { projectId } })">查看节点类型</el-button>
        <el-button @click="router.push({ name: 'project-master-data-deliverable-standard', params: { projectId } })">查看交付物标准</el-button>
      </div>
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
          <p>基于 catalog-only 证据判断真实项目是否已有路径映射、资产目录、扫描记录和主数据底座。</p>
        </div>
        <div class="initialization-panel__tags">
          <el-tag size="large" :type="assessment?.realNasProject ? 'success' : 'warning'">
            {{ assessment?.realNasProject ? '真实 NAS 项目' : '来源待确认' }}
          </el-tag>
          <el-tag size="large" type="info">{{ onboardingStatusText(assessment?.onboardingStatus) }}</el-tag>
        </div>
      </div>

      <div class="onboarding-summary">
        <article v-for="item in onboardingCards" :key="item.label" class="onboarding-summary__card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.unit }}</small>
        </article>
      </div>

      <div class="onboarding-clue-grid">
        <article v-for="group in assetClueGroups" :key="group.label">
          <span>{{ group.label }}</span>
          <div v-if="group.values.length" class="onboarding-tags">
            <el-tag v-for="value in group.values" :key="`${group.label}-${value}`" size="small" type="info">
              {{ value }}
            </el-tag>
          </div>
          <strong v-else>暂无</strong>
          <small>{{ group.helper }}</small>
        </article>
      </div>

      <div class="onboarding-distribution-grid">
        <article v-for="group in distributionGroups" :key="group.label">
          <h3>{{ group.label }}</h3>
          <el-empty v-if="!group.rows.length" description="暂无目录分布" />
          <ul v-else class="onboarding-list">
            <li v-for="row in group.rows" :key="`${group.label}-${row.code}`">
              <strong>{{ row.label }} · {{ formatCount(row.count) }}</strong>
              <span>{{ formatPercent(row.ratio) }}，仅代表目录级统计。</span>
            </li>
          </ul>
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
              <strong>
                <el-tag size="small" :type="gapSeverityType(gap.severity)">{{ gap.severity }}</el-tag>
                {{ gap.description }}
              </strong>
              <span>{{ gap.missingEvidenceReason }}</span>
            </li>
          </ul>
        </section>
      </div>

      <div class="onboarding-columns">
        <section>
          <h3>治理风险</h3>
          <el-empty v-if="!assessment?.assetSummary?.governanceRisks?.length" description="暂无明显治理风险" />
          <ul v-else class="onboarding-list">
            <li v-for="risk in assessment.assetSummary.governanceRisks" :key="risk.code">
              <strong>
                <el-tag size="small" :type="gapSeverityType(risk.severity)">{{ risk.severity }}</el-tag>
                {{ risk.description }}
              </strong>
              <span>{{ formatCount(risk.count) }} 项 · {{ risk.missingEvidenceReason }}</span>
            </li>
          </ul>
        </section>
        <section>
          <h3>Missing Evidence</h3>
          <el-empty v-if="!assessment?.missingEvidence?.length" description="暂无证据缺口" />
          <ul v-else class="onboarding-list">
            <li v-for="item in assessment.missingEvidence" :key="item.code">
              <strong>{{ item.code }}</strong>
              <span>{{ item.reason }} 需要：{{ item.requiredEvidence }}</span>
            </li>
          </ul>
        </section>
      </div>
    </section>

    <div class="initialization-grid">
      <section class="initialization-panel">
        <div class="initialization-panel__header">
          <div>
            <h2>参考草案模板</h2>
            <p>模板只作为行业参考骨架；真实 105 项目以已登记资产线索为主，不会直接一键写成正式标准。</p>
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
            <h2>参考骨架</h2>
            <p>这里展示模板包含的标准项。真实项目需要结合 DWG / PDF / RVT / Excel 线索人工取舍。</p>
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
          <p>这是只读预检查，不生成交付结论，不读取文件正文，不触碰 NAS。真实项目草案只用于人工配置，不会直接变成就绪标准。</p>
        </div>
        <el-button
          type="primary"
          :disabled="!preview || preview.blocked"
          :loading="applying"
          @click="confirmApply"
        >
          {{ realProjectManualMode ? '确认生成工程主数据' : '确认应用草案' }}
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
        <div v-if="onboardingPreview?.draftItems?.length" class="draft-evidence">
          <div class="draft-evidence__header">
            <h3>草案证据与风险</h3>
            <p>请勾选本次要采纳的草案项。后端只会处理所选项及必要依赖项，catalog-only 线索不能替代真实工程结构。</p>
          </div>
          <div v-if="realProjectManualMode" class="manual-confirm-options">
            <el-radio-group v-model="sectionStrategy" size="small">
              <el-radio-button label="DISCIPLINE_LEVEL">按专业生成</el-radio-button>
              <el-radio-button label="PROJECT_LEVEL">仅项目级</el-radio-button>
            </el-radio-group>
            <el-checkbox v-model="riskAccepted">
              我理解这些规则只基于目录元数据，后续仍需人工检查
            </el-checkbox>
          </div>
          <el-table
            :data="onboardingPreview.draftItems"
            class="initialization-table"
            max-height="320"
            :row-key="draftItemKey"
            @selection-change="handleDraftSelectionChange"
          >
            <el-table-column v-if="realProjectManualMode" type="selection" width="48" />
            <el-table-column label="类别" width="150">
              <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
            </el-table-column>
            <el-table-column prop="name" label="草案项" min-width="140" />
            <el-table-column label="来源" min-width="180">
              <template #default="{ row }">
                <div class="evidence-source-cell">
                  <el-tag v-if="row.fromTemplateSkeleton" size="small" type="info">模板骨架</el-tag>
                  <el-tag v-if="row.fromRealAssetClue" size="small" type="success">资产线索</el-tag>
                  <span>{{ evidenceSourceLabel(row.evidenceSource) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="证据模式" width="130">
              <template #default="{ row }">
                <el-tag size="small" type="warning">{{ row.evidenceMode }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="置信度" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="confidenceType(row.confidenceLevel)">
                  {{ confidenceLabel(row.confidenceLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="riskHint" label="风险提示" min-width="260" show-overflow-tooltip />
            <el-table-column label="确认" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.pendingConfirmation ? 'warning' : 'success'">
                  {{ row.pendingConfirmation ? '需复核' : '已确认' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
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

    <section v-if="confirmResult" class="initialization-result">
      <div>
        <h2>工程主数据已生成</h2>
        <p>
          本次创建 {{ totalTemplateItems(confirmResult.created) }} 项，跳过 {{ totalTemplateItems(confirmResult.skipped) }} 项。
          已生成可编辑的初始规则；系统没有读取正文、没有触碰 NAS 文件，也没有自动挂接或审核文件。
        </p>
        <ul class="initialization-result__followups">
          <li v-for="item in confirmResult.manualFollowUps" :key="item">{{ item }}</li>
        </ul>
      </div>
      <div class="initialization-result__actions">
        <el-button @click="router.push({ name: 'project-master-data-sections', params: { projectId } })">查看部位树</el-button>
        <el-button @click="router.push({ name: 'project-master-data-node-types', params: { projectId } })">查看节点类型</el-button>
        <el-button @click="router.push({ name: 'project-master-data-deliverable-standard', params: { projectId } })">
          查看交付物标准
        </el-button>
        <el-button @click="router.push({ name: 'project-work-document-delivery', params: { projectId } })">进入文档交付</el-button>
        <el-button @click="router.push({ name: 'project-work-drawing-delivery', params: { projectId } })">进入图纸交付</el-button>
      </div>
    </section>

    <section v-else-if="applyResult" class="initialization-result">
      <div>
        <h2>接入草案已应用</h2>
        <p>
          本次创建 {{ totalTemplateItems(applyResult.created) }} 项，跳过 {{ totalTemplateItems(applyResult.skipped) }} 项。
          草案已生成，但仍需项目负责人复核部位树、节点类型和交付物标准。
        </p>
      </div>
      <div class="initialization-result__actions">
        <el-button @click="router.push({ name: 'project-master-data-sections', params: { projectId } })">查看部位树</el-button>
        <el-button @click="router.push({ name: 'project-master-data-node-types', params: { projectId } })">查看节点类型</el-button>
        <el-button @click="router.push({ name: 'project-master-data-deliverable-standard', params: { projectId } })">
          查看交付物标准
        </el-button>
        <el-button @click="router.push({ name: 'project-work-document-delivery', params: { projectId } })">进入文档交付</el-button>
        <el-button @click="router.push({ name: 'project-work-drawing-delivery', params: { projectId } })">进入图纸交付</el-button>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import MasterDataStepNav from '@/modules/master-data/components/MasterDataStepNav.vue';
import {
  applyOnboardingDraft,
  confirmOnboardingDraft,
  fetchInitializationStatus,
  fetchOnboardingAssessment,
  fetchOnboardingPreview,
  fetchStandardTemplateDetail,
  fetchStandardTemplates,
  type InitializationStatus,
  type OnboardingAssessment,
  type OnboardingConfirmResult,
  type OnboardingDraftItem,
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
const confirmResult = ref<OnboardingConfirmResult | null>(null);
const selectedDraftItems = ref<OnboardingDraftItem[]>([]);
const sectionStrategy = ref<'DISCIPLINE_LEVEL' | 'PROJECT_LEVEL'>('DISCIPLINE_LEVEL');
const riskAccepted = ref(false);

const projectId = computed(() => workspaceProjectId.value ?? 0);
const realProjectManualMode = computed(() => {
  const source = assessment.value?.assetSource?.toUpperCase() ?? '';
  return source.startsWith('NAS_REAL') && !source.includes('SMOKE');
});
const projectLabel = computed(() => {
  const project = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return project ? `${project.code} | ${project.name}` : `项目 ${projectId.value || '-'}`;
});
const heroEyebrow = computed(() => status.value?.ready ? 'M2E 工程主数据人工确认' : 'M2E 真实项目主数据待确认');
const heroTitle = computed(() => {
  if (!assessment.value?.realNasProject) {
    return '用目录证据生成草案，再由项目负责人确认';
  }
  return status.value?.ready ? '工程主数据已生成，仍需人工复核和维护' : '真实资产已接入，工程主数据待确认';
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
    { label: '项目来源', value: assessment.value?.realNasProject ? '真实 NAS' : '待确认', unit: assessment.value?.assetSource || '-' },
    { label: '目录文件', value: formatCount(summary?.fileCount), unit: '已登记' },
    { label: '模型文件', value: formatCount(summary?.modelFileCount), unit: 'catalog-only' },
    { label: '图纸文件', value: formatCount(summary?.drawingFileCount), unit: 'catalog-only' },
    { label: '文档文件', value: formatCount(summary?.documentFileCount), unit: 'catalog-only' },
    { label: '清单表格', value: formatCount(summary?.spreadsheetFileCount), unit: 'catalog-only' },
    { label: '路径映射', value: formatCount(summary?.pathMappingCount), unit: '不展示原始路径' },
    { label: '扫描记录', value: formatCount(summary?.scanTaskCount), unit: '只读任务' },
    { label: '最近扫描', value: formatDate(summary?.lastScanAt || summary?.lastAssetSeenAt), unit: '只读记录' }
  ];
});

const assetClueGroups = computed(() => {
  const summary = assessment.value?.assetSummary;
  return [
    {
      label: '主要扩展名',
      values: summary?.dominantFileExtensions ?? [],
      helper: '来自文件名元数据，只能辅助判断资料类型。'
    },
    {
      label: '主要专业线索',
      values: summary?.dominantDisciplines ?? [],
      helper: '来自目录入库或人工治理字段，需要负责人复核。'
    },
    {
      label: '主要目录线索',
      values: summary?.directoryClues ?? [],
      helper: '仅展示脱敏后的项目内目录线索，不代表真实工程结构。'
    }
  ];
});

const distributionGroups = computed(() => {
  const summary = assessment.value?.assetSummary;
  return [
    {
      label: '文件类型分布',
      rows: summary?.fileKindDistribution ?? []
    },
    {
      label: '扩展名分布',
      rows: summary?.extensionDistribution ?? []
    },
    {
      label: '专业分布',
      rows: summary?.disciplineDistribution ?? []
    }
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
  confirmResult.value = null;
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
  selectedDraftItems.value = [];
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
    confirmResult.value = null;
  }
  try {
    onboardingPreview.value = await fetchOnboardingPreview(projectId.value, selectedTemplateCode.value);
    preview.value = onboardingPreview.value.templatePreview;
    selectedDraftItems.value = [];
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '接入草案预览失败');
  } finally {
    previewing.value = false;
  }
}

async function confirmApply() {
  if (!projectId.value || !selectedTemplateCode.value) return;
  if (realProjectManualMode.value) {
    if (!riskAccepted.value) {
      ElMessage.warning('请先确认已理解 catalog-only 证据边界和后续人工复核风险');
      return;
    }
    if (!selectedDraftItems.value.length) {
      ElMessage.warning('请至少选择一个要采纳的草案项');
      return;
    }
    try {
      await ElMessageBox.confirm(
        '确认生成工程主数据？后端只会处理你勾选的草案项及必要依赖项。本操作只基于资产目录和文件元数据生成可编辑初始规则，不读取正文，不触碰 NAS 文件，也不会自动挂接、审核或生成交付结论。',
        '确认生成工程主数据',
        {
          type: 'warning',
          confirmButtonText: '确认生成',
          cancelButtonText: '取消'
        }
      );
      applying.value = true;
      const result = await confirmOnboardingDraft(projectId.value, {
        templateCode: selectedTemplateCode.value,
        confirmed: true,
        confirmationMode: 'MANUAL_REVIEW',
        selectedDraftItemIds: selectedDraftItems.value.map(draftItemKey),
        sectionStrategy: sectionStrategy.value,
        nodeTypeStrategy: 'LOCK_CONFIRMED',
        deliverableStrategy: 'FILE_TYPE_MINIMAL',
        riskAccepted: true
      });
      confirmResult.value = result;
      ElMessage.success('工程主数据已生成，请继续人工复核');
      await Promise.all([loadStatus(), loadAssessment(), handlePreview(false)]);
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error(error instanceof Error ? error.message : '工程主数据生成失败');
      }
    } finally {
      applying.value = false;
    }
    return;
  }
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
    if (!result.templateResult) {
      applyResult.value = null;
      ElMessage.info('真实项目草案已确认待人工配置，未直接应用模板');
      await Promise.all([loadStatus(), loadAssessment(), handlePreview(false)]);
      return;
    }
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

function handleDraftSelectionChange(rows: OnboardingDraftItem[]) {
  selectedDraftItems.value = rows;
}

function draftItemKey(row: OnboardingDraftItem) {
  return `${row.category}:${row.name}:${row.evidenceSource}`;
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

function categoryLabel(value: string) {
  const labels: Record<string, string> = {
    SECTION_NODE: '部位节点',
    NODE_TYPE: '节点类型',
    DELIVERABLE_DEFINITION: '交付定义',
    DELIVERABLE_TYPE: '交付类型',
    DELIVERABLE_ATTRIBUTE: '交付属性',
    DIRECTORY_TEMPLATE: '目录模板',
    DISCIPLINE_CANDIDATE: '专业候选',
    DELIVERABLE_TYPE_CANDIDATE: '交付类型候选',
    TARGET_CANDIDATE: '交付对象候选'
  };
  return labels[value] ?? value;
}

function evidenceSourceLabel(value: string) {
  const labels: Record<string, string> = {
    EXISTING_PROJECT_MASTERDATA: '项目中已有同编码/同名项',
    CATALOG_DIRECTORY_CLUE: '目录线索支持',
    CATALOG_FILE_KIND_CLUE: '文件类型线索支持',
    CATALOG_DISCIPLINE_DISTRIBUTION: '专业分布线索',
    CATALOG_EXTENSION_DISTRIBUTION: '扩展名分布线索',
    CATALOG_PROJECT_ASSET_SUMMARY: '项目资产汇总线索',
    TEMPLATE_SKELETON: '模板默认骨架'
  };
  return labels[value] ?? value;
}

function confidenceLabel(value: string) {
  const labels: Record<string, string> = {
    EXISTING: '已有',
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低'
  };
  return labels[value] ?? value;
}

function confidenceType(value: string) {
  if (value === 'EXISTING' || value === 'HIGH') return 'success';
  if (value === 'MEDIUM') return 'warning';
  return 'info';
}

function gapSeverityType(value: string) {
  if (value === 'error') return 'danger';
  if (value === 'warning') return 'warning';
  return 'info';
}

function formatCount(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('zh-CN');
}

function formatPercent(value: number | null | undefined) {
  return `${Math.round(Number(value ?? 0) * 1000) / 10}%`;
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
  gap: var(--zy-sp-4);
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
}

.initialization-page__header,
.initialization-panel__header,
.initialization-result {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--zy-sp-4);
  min-width: 0;
  max-width: 100%;
}

.initialization-page__header > div,
.initialization-panel__header > div,
.initialization-result > div,
.initialization-hero > div,
.masterdata-next-action > div {
  min-width: 0;
}

.initialization-panel__tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  justify-content: flex-end;
}

.initialization-page__header h1,
.initialization-panel__header h2,
.initialization-result h2,
.initialization-hero h2 {
  margin: 0;
  color: var(--zy-ink);
  font-weight: var(--zy-fw-semi);
  letter-spacing: -0.01em;
}

.initialization-page__header p,
.initialization-panel__header p,
.initialization-result p,
.initialization-hero p {
  margin: 6px 0 0;
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
  line-height: 1.65;
}

.initialization-hero,
.initialization-panel,
.initialization-result {
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  padding: var(--zy-sp-5);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
}

.initialization-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--zy-sp-5);
  position: relative;
  overflow: hidden;
}

.initialization-hero::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--zy-blue-500);
}

.initialization-hero > * {
  position: relative;
}

.initialization-hero__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  padding: 2px 8px;
  border-radius: var(--zy-radius-sm);
  background: var(--zy-blue-50);
  border: 1px solid rgba(37, 99, 235, 0.18);
  color: var(--zy-blue-700);
  font-size: 11px;
  font-weight: var(--zy-fw-semi);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  line-height: 1.2;
}

.initialization-status {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(128px, 1fr));
  gap: var(--zy-sp-2);
  min-width: 0;
}

.initialization-status__card {
  display: grid;
  gap: 4px;
  min-width: 0;
  max-width: 100%;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border: 1px solid rgba(245, 158, 11, 0.28);
  border-left: 3px solid var(--zy-amber-500);
  border-radius: var(--zy-radius-base);
  background: var(--zy-amber-50);
}

.initialization-status__card.is-ready {
  border-color: rgba(34, 197, 94, 0.28);
  border-left-color: var(--zy-green-500);
  background: var(--zy-green-50);
}

.initialization-status__card span,
.initialization-status__card small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.initialization-status__card strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-2xl);
  font-weight: var(--zy-fw-bold);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
  overflow-wrap: anywhere;
}

.onboarding-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--zy-sp-2);
  margin-top: var(--zy-sp-3);
  min-width: 0;
}

.onboarding-summary__card {
  display: grid;
  gap: 4px;
  min-width: 0;
  max-width: 100%;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
}

.onboarding-summary__card span,
.onboarding-summary__card small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.onboarding-summary__card strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-xl);
  font-weight: var(--zy-fw-bold);
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}

.onboarding-clue-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--zy-sp-2);
  margin-top: var(--zy-sp-3);
  min-width: 0;
}

.onboarding-distribution-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--zy-sp-3);
  margin-top: var(--zy-sp-3);
  min-width: 0;
}

.onboarding-distribution-grid article,
.onboarding-columns section {
  min-width: 0;
  max-width: 100%;
}

.onboarding-clue-grid article {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
}

.onboarding-clue-grid span,
.onboarding-clue-grid small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.5;
}

.onboarding-clue-grid strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-base);
  font-weight: var(--zy-fw-semi);
}

.onboarding-tags,
.evidence-source-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  min-width: 0;
}

.evidence-source-cell span {
  flex: 1 1 100%;
  min-width: 0;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.5;
}

.onboarding-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--zy-sp-4);
  margin-top: var(--zy-sp-3);
  min-width: 0;
}

.onboarding-columns h3 {
  margin: 0 0 var(--zy-sp-2);
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.onboarding-distribution-grid h3 {
  margin: 0 0 var(--zy-sp-2);
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.onboarding-list {
  display: grid;
  gap: var(--zy-sp-2);
  margin: 0;
  padding: 0;
  list-style: none;
}

.onboarding-list li {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: var(--zy-sp-2) var(--zy-sp-3);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
}

.onboarding-list strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
  min-width: 0;
  overflow-wrap: anywhere;
}

.onboarding-list span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.55;
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
  gap: var(--zy-sp-4);
  min-width: 0;
  max-width: 100%;
}

.template-list {
  display: grid;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.template-card {
  display: grid;
  gap: 6px;
  min-width: 0;
  max-width: 100%;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  text-align: left;
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  cursor: pointer;
  font-family: inherit;
  transition:
    border-color var(--zy-duration-2) var(--zy-ease),
    background var(--zy-duration-2) var(--zy-ease);
}

.template-card:hover {
  border-color: rgba(37, 99, 235, 0.32);
  background: var(--zy-surface-soft);
}

.template-card.is-active {
  border-color: var(--zy-blue-500);
  background: var(--zy-blue-50);
  box-shadow: 0 0 0 1px var(--zy-blue-500);
}

.template-card span,
.template-card small,
.template-card em {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
  line-height: 1.55;
}

.template-card strong {
  color: var(--zy-ink);
  font-weight: var(--zy-fw-semi);
}

.initialization-table {
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.initialization-table :deep(.el-table__inner-wrapper),
.initialization-table :deep(.el-scrollbar),
.initialization-table :deep(.el-scrollbar__wrap) {
  min-width: 0;
  max-width: 100%;
}

.initialization-table :deep(.cell) {
  overflow-wrap: anywhere;
  word-break: break-word;
}

.preview-summary {
  display: grid;
  gap: var(--zy-sp-3);
  min-width: 0;
}

.preview-summary__counts {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
}

.preview-summary__counts span {
  padding: 5px 10px;
  border-radius: var(--zy-radius-sm);
  background: var(--zy-bg);
  border: var(--zy-border-soft);
  color: var(--zy-text-soft);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
  font-variant-numeric: tabular-nums;
}

.preview-summary__counts .danger {
  background: var(--zy-red-50);
  border-color: rgba(239, 68, 68, 0.28);
  color: #b91c1c;
}

.preview-warnings {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
}

.preview-warnings :deep(.el-tag) {
  height: auto;
  line-height: 1.45;
  max-width: 100%;
  padding: 5px 8px;
  white-space: normal;
}

.draft-evidence {
  display: grid;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.manual-confirm-options {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--zy-sp-2);
  min-width: 0;
  max-width: 100%;
  padding: var(--zy-sp-3);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-amber-50);
}

.draft-evidence__header h3 {
  margin: 0;
  color: var(--zy-ink);
  font-size: var(--zy-fs-md);
  font-weight: var(--zy-fw-semi);
}

.draft-evidence__header p {
  margin: 4px 0 0;
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
  line-height: 1.55;
}

.initialization-result__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
}

.initialization-result__followups {
  display: grid;
  gap: 4px;
  margin: var(--zy-sp-2) 0 0;
  padding-left: 18px;
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
  line-height: 1.55;
}

@media (max-width: 1120px) {
  .initialization-grid,
  .onboarding-columns,
  .onboarding-clue-grid,
  .onboarding-distribution-grid {
    grid-template-columns: 1fr;
  }

  .initialization-panel__header {
    flex-direction: column;
  }

  .initialization-panel__tags {
    justify-content: flex-start;
  }
}
</style>
