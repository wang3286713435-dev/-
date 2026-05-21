<template>
  <section class="data-steward-panel">
    <header class="data-steward-panel__head">
      <div>
        <h3>Hermes 数据管家</h3>
        <p>只基于资产目录、受控项目路径、权限证明和当前项目上下文回答。</p>
      </div>
      <div class="data-steward-panel__badges">
        <EvidenceModeBadge mode="catalog_only" />
        <el-tag :type="healthTagType(health)" size="small">{{ healthLabel(health) }}</el-tag>
      </div>
    </header>

    <el-alert
      type="info"
      title="当前不读取 PDF、Office、CAD、BIM 正文，不执行数据库或 NAS 写操作。"
      :closable="false"
      show-icon
    />

    <section class="data-steward-panel__context" aria-label="当前页面上下文">
      <div>
        <span>当前上下文</span>
        <strong>{{ contextTitle }}</strong>
      </div>
      <p>{{ contextDescription }}</p>
    </section>

    <div class="data-steward-panel__status">
      <div class="data-steward-panel__status-item">
        <span>当前模式</span>
        <strong>资产目录辅助</strong>
      </div>
      <div class="data-steward-panel__status-item">
        <span>正文问答</span>
        <strong>未开放</strong>
      </div>
      <div class="data-steward-panel__status-item">
        <span>项目路径</span>
        <strong>受控查询</strong>
      </div>
      <div class="data-steward-panel__status-item">
        <span>写操作</span>
        <strong>不会执行</strong>
      </div>
      <div class="data-steward-panel__status-item">
        <span>生产发布</span>
        <strong>未开放</strong>
      </div>
    </div>

    <el-descriptions v-if="capabilities" :column="1" border size="small">
      <el-descriptions-item label="能力名称">{{ capabilities.agentName }}</el-descriptions-item>
      <el-descriptions-item label="模式">{{ modeLabel(capabilities.mode) }}</el-descriptions-item>
      <el-descriptions-item label="合同">{{ capabilities.contractVersion }}</el-descriptions-item>
      <el-descriptions-item v-if="health" label="网关状态">
        {{ health.status === 'ok' ? '正常' : '降级运行' }}
      </el-descriptions-item>
      <el-descriptions-item v-if="health" label="Hermes连接">
        {{ healthLabel(health) }}
      </el-descriptions-item>
      <el-descriptions-item v-if="health" label="运行模式">{{ modeLabel(health.mode) }}</el-descriptions-item>
      <el-descriptions-item v-if="health" label="运行时写入">
        {{ health.runtimeWriteEnabled ? '已开放' : '未开放' }}
      </el-descriptions-item>
      <el-descriptions-item v-if="health" label="Hermes真实回答">
        {{ health.agentAnswerIntegrationEnabled ? '已接入' : '未接入' }}
      </el-descriptions-item>
      <el-descriptions-item label="目录辅助">
        {{ capabilities.supports.catalogQuery ? '可用' : '未开放' }}
      </el-descriptions-item>
      <el-descriptions-item label="正文问答">
        {{ capabilities.supports.documentContentAnswer ? '可用' : '未开放' }}
      </el-descriptions-item>
      <el-descriptions-item label="数据库 / NAS 写操作">
        {{ capabilities.supports.dbCrud || capabilities.supports.nasCrud ? '可用' : '不会执行' }}
      </el-descriptions-item>
      <el-descriptions-item label="生产发布">
        {{ capabilities.supports.productionRollout ? '可用' : '未开放' }}
      </el-descriptions-item>
      <el-descriptions-item v-if="health?.unavailableReason" label="不可用原因">
        {{ health.unavailableReason }}
      </el-descriptions-item>
    </el-descriptions>

    <section class="data-steward-panel__action-center" aria-label="Hermes Action Center">
      <header>
        <div>
          <strong>Hermes Action Center</strong>
          <span>通过平台受控接口生成计划，只有人工确认后才会执行挂接。</span>
        </div>
        <el-tag size="small" type="info">G3 read-only controlled MVP</el-tag>
      </header>

      <el-tabs v-model="actionCenterTab" class="data-steward-panel__tabs">
        <el-tab-pane label="回答" name="answer">
          <div class="data-steward-panel__answer-state">
            <strong>自然语言回答</strong>
            <span>下方输入问题后，Hermes 会按 catalog-only / Missing Evidence 边界回答。</span>
          </div>
        </el-tab-pane>

        <el-tab-pane label="操作草案" name="draft">
          <div class="data-steward-panel__tool-row">
            <el-button
              size="small"
              type="primary"
              plain
              :loading="masterPlanLoading"
              :disabled="actionBusy || !Number.isFinite(projectId)"
              @click="generateMasterDataPlan"
            >
              生成主数据补齐计划
            </el-button>
            <el-segmented v-model="actionViewType" :options="actionViewOptions" size="small" />
            <el-button
              size="small"
              type="primary"
              plain
              :loading="missingPlanLoading"
              :disabled="actionBusy || !Number.isFinite(projectId)"
              @click="generateMissingDeliveryPlan"
            >
              生成缺失交付方案
            </el-button>
          </div>

          <el-empty
            v-if="!masterDataPlan && !missingDeliveryPlan"
            description="可先生成主数据计划或缺失交付方案。这里只产生草案，不创建主数据，不读取文件正文。"
          />

          <article v-if="masterDataPlan" class="data-steward-panel__plan">
            <div class="data-steward-panel__plan-head">
              <h4>工程主数据补齐计划</h4>
              <el-tag size="small" type="success">操作草案</el-tag>
            </div>
            <div class="data-steward-panel__plan-grid">
              <div>
                <span>接入状态</span>
                <strong>{{ masterDataPlan.onboardingStatus }}</strong>
              </div>
              <div>
                <span>部位树</span>
                <strong>{{ masterDataPlan.sectionTreeStatus }}</strong>
              </div>
              <div>
                <span>节点类型</span>
                <strong>{{ masterDataPlan.nodeTypeStatus }}</strong>
              </div>
              <div>
                <span>交付标准</span>
                <strong>{{ masterDataPlan.deliverableStandardStatus }}</strong>
              </div>
              <div>
                <span>文档准备度</span>
                <strong>{{ masterDataPlan.documentReadiness }}</strong>
              </div>
              <div>
                <span>图纸准备度</span>
                <strong>{{ masterDataPlan.drawingReadiness }}</strong>
              </div>
            </div>
            <ul class="data-steward-panel__compact-list">
              <li v-for="item in masterDataPlan.nextSteps" :key="item">{{ item }}</li>
            </ul>
            <div class="data-steward-panel__entrances">
              <el-tag v-for="item in masterDataPlan.entrances" :key="item" size="small" type="info">{{ item }}</el-tag>
            </div>
          </article>

          <article v-if="missingDeliveryPlan" class="data-steward-panel__plan">
            <div class="data-steward-panel__plan-head">
              <h4>{{ viewTypeLabel(missingDeliveryPlan.viewType) }}缺失交付方案</h4>
              <el-tag size="small" type="warning">待人工确认</el-tag>
            </div>
            <p class="data-steward-panel__plan-copy">
              共 {{ missingDeliveryPlan.totalMissing }} 个缺失项，已生成 {{ missingDeliveryPlan.recommendationCount }}
              条候选推荐。推荐只依据当前项目元数据，低置信或元数据不完整时应先治理文件信息。
            </p>
            <ul class="data-steward-panel__compact-list">
              <li v-for="item in missingDeliveryPlan.previewRows" :key="item.missingItemKey">
                {{ item.targetName }} 缺少 {{ item.deliverableTypeName }}，{{ item.explanation }}
              </li>
            </ul>
          </article>
        </el-tab-pane>

        <el-tab-pane label="待人工确认" name="confirm">
          <el-alert
            type="warning"
            title="这里不会自动挂接。必须勾选推荐、勾选人工确认，再由平台后端二次校验后执行。"
            :closable="false"
            show-icon
          />

          <el-empty
            v-if="actionRecommendations.length === 0"
            description="暂无待确认推荐，请先在“操作草案”中生成缺失交付方案。"
          />

          <div v-else class="data-steward-panel__recommendations">
            <el-checkbox-group v-model="selectedRecommendationIds" class="data-steward-panel__recommendation-list">
              <el-checkbox
                v-for="item in actionRecommendations"
                :key="item.recommendationId"
                :label="item.recommendationId"
                class="data-steward-panel__recommendation"
              >
                <div>
                  <strong>{{ item.targetName }} · {{ item.deliverableTypeName }}</strong>
                  <span>{{ item.fileName }} · {{ item.versionNo || '无版本' }}</span>
                  <small>
                    {{ item.recommendationReason }}
                    <template v-if="item.riskWarnings.length">；{{ item.riskWarnings.join('；') }}</template>
                    <template v-if="item.metadataGovernanceRequired">；建议先治理元数据</template>
                  </small>
                </div>
                <el-tag :type="confidenceTag(item.confidence)" size="small">{{ confidenceLabel(item.confidence) }}</el-tag>
              </el-checkbox>
            </el-checkbox-group>

            <div class="data-steward-panel__confirm-bar">
              <el-checkbox v-model="actionConfirmed">
                我已人工核对所选推荐，确认调用平台挂接能力。
              </el-checkbox>
              <div>
                <span>已选 {{ selectedActionRecommendations.length }} 条</span>
                <el-button
                  type="success"
                  size="small"
                  :loading="actionApplyLoading"
                  :disabled="!actionConfirmed || selectedActionRecommendations.length === 0 || actionApplyLoading"
                  @click="applyActionRecommendations"
                >
                  确认执行
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="执行结果" name="result">
          <el-empty v-if="!actionApplyResult" description="尚无执行结果。未确认前不会产生任何挂接写入。" />
          <section v-else class="data-steward-panel__result">
            <div class="data-steward-panel__result-tags">
              <el-tag type="success">创建 {{ actionApplyResult.createdCount }}</el-tag>
              <el-tag type="warning">跳过 {{ actionApplyResult.skippedCount }}</el-tag>
              <el-tag type="danger">失败 {{ actionApplyResult.failedCount }}</el-tag>
            </div>
            <div class="data-steward-panel__result-list">
              <div v-for="item in actionApplyResult.results" :key="`${item.fileResourceId}-${item.recommendationId}`">
                <strong>{{ resultStatusLabel(item.status) }}</strong>
                <span>文件 {{ item.fileResourceId }}：{{ item.message }}</span>
              </div>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-input
      v-model="question"
      type="textarea"
      :rows="4"
      maxlength="500"
      show-word-limit
      :disabled="asking"
      placeholder="问 Hermes，例如：这个项目路径在哪里？这个项目有哪些已登记文件？哪些资料缺少可引用证据？"
    />

    <div class="data-steward-panel__quick">
      <el-button
        v-for="item in quickQuestions"
        :key="item"
        size="small"
        :disabled="asking || !Number.isFinite(projectId)"
        @click="askQuickQuestion(item)"
      >
        {{ item }}
      </el-button>
    </div>

    <div class="data-steward-panel__actions">
      <el-button type="primary" :loading="asking" :disabled="!canAsk || asking" @click="submitQuestion">
        问 Hermes
      </el-button>
      <el-button :loading="capabilityLoading" @click="loadCapabilities">刷新能力</el-button>
    </div>

    <el-alert
      v-if="asking"
      class="data-steward-panel__thinking"
      type="info"
      title="真实 Hermes 正在组织回答，可能需要 10-30 秒。平台未执行任何写操作。"
      :closable="false"
      show-icon
    />

    <DataStewardAnswerCard v-if="answer" :response="answer" />

    <section v-if="catalogLoading || catalogPreview" class="data-steward-panel__catalog">
      <header>
        <div>
          <strong>资产目录预览</strong>
          <span>仅展示目录元数据，不代表已读取文件正文。</span>
        </div>
        <el-tag size="small" type="info">asset_catalog_preview</el-tag>
      </header>

      <el-skeleton v-if="catalogLoading" :rows="3" animated />
      <el-empty
        v-else-if="catalogPreview && catalogPreview.results.length === 0"
        description="未找到匹配的目录资产"
      />
      <div v-else class="data-steward-panel__catalog-list">
        <article
          v-for="item in catalogPreview?.results"
          :key="item.assetRef"
          class="data-steward-panel__catalog-item"
        >
          <div>
            <strong>{{ item.fileName }}</strong>
            <span>{{ item.projectCode }} {{ item.projectName }} · {{ item.displayPath || item.pathHint }}</span>
          </div>
          <div class="data-steward-panel__catalog-tags">
            <el-tag size="small">{{ fileKindLabel(item.assetKind) }}</el-tag>
            <el-tag size="small" type="info">{{ item.fileExt || '未知格式' }}</el-tag>
            <el-tag size="small" type="success">{{ item.indexEligibility }}</el-tag>
            <el-tag v-if="!item.contentEvidenceAvailable" size="small" type="warning">无正文证据</el-tag>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  askHermes,
  fetchHermesCapabilities,
  fetchHermesHealth,
  searchCatalogPreview,
  type CatalogSearchResponse,
  type HermesCapabilities,
  type HermesChatResponse,
  type HermesHealth
} from '@/modules/data-steward/api/dataSteward';
import {
  fetchOnboardingAssessment,
  type OnboardingAssessment
} from '@/modules/master-data/api/masterData';
import {
  applyAgentGovernanceRecommendations,
  fetchAgentGovernanceMissingItems,
  fetchAgentGovernanceOverview,
  recommendAgentGovernanceBindings,
  type AgentBindingRecommendation,
  type AgentGovernanceDeliveryStatus,
  type AgentGovernanceMissingItem,
  type AgentGovernanceOverview,
  type ApplyAgentRecommendationsResponse
} from '@/modules/work-center/api/delivery';
import DataStewardAnswerCard from '@/modules/data-steward/components/DataStewardAnswerCard.vue';
import EvidenceModeBadge from '@/modules/data-steward/components/EvidenceModeBadge.vue';

const props = defineProps<{
  projectId: number;
  pageType: string;
  sourceView?: string;
  assetId?: number;
  currentRoute?: string;
  projectCode?: string;
  projectName?: string;
  pageTitle?: string;
}>();

const question = ref('');
const capabilities = ref<HermesCapabilities | null>(null);
const health = ref<HermesHealth | null>(null);
const answer = ref<HermesChatResponse | null>(null);
const catalogPreview = ref<CatalogSearchResponse | null>(null);
const asking = ref(false);
const catalogLoading = ref(false);
const capabilityLoading = ref(false);
type ActionCenterTab = 'answer' | 'draft' | 'confirm' | 'result';
type ActionViewType = 'DOCUMENT' | 'DRAWING';

interface MasterDataActionPlan {
  onboardingStatus: string;
  sectionTreeStatus: string;
  nodeTypeStatus: string;
  deliverableStandardStatus: string;
  documentReadiness: string;
  drawingReadiness: string;
  nextSteps: string[];
  entrances: string[];
}

interface MissingDeliveryActionPlan {
  viewType: ActionViewType;
  totalMissing: number;
  recommendationCount: number;
  previewRows: AgentGovernanceMissingItem[];
}

const actionCenterTab = ref<ActionCenterTab>('answer');
const actionViewType = ref<ActionViewType>('DOCUMENT');
const actionViewOptions = [
  { label: '文档', value: 'DOCUMENT' },
  { label: '图纸', value: 'DRAWING' }
];
const masterPlanLoading = ref(false);
const missingPlanLoading = ref(false);
const actionApplyLoading = ref(false);
const masterDataPlan = ref<MasterDataActionPlan | null>(null);
const missingDeliveryPlan = ref<MissingDeliveryActionPlan | null>(null);
const actionRecommendations = ref<AgentBindingRecommendation[]>([]);
const selectedRecommendationIds = ref<string[]>([]);
const actionConfirmed = ref(false);
const actionApplyResult = ref<ApplyAgentRecommendationsResponse | null>(null);
const contextTitle = computed(() => props.pageTitle || pageTypeLabel(props.pageType));
const projectLabel = computed(() => {
  if (props.projectCode || props.projectName) {
    return [props.projectCode, props.projectName].filter(Boolean).join(' ');
  }
  return `项目 ${props.projectId}`;
});
const contextDescription = computed(() => {
  const routeText = props.currentRoute ? ` · ${props.currentRoute}` : '';
  return `${projectLabel.value}${routeText}`;
});
const quickQuestions = computed(() => {
  const items = [
    '这个页面是干什么的？',
    '我下一步应该做什么？',
    '这个项目路径在哪里？',
    '这个项目有哪些已登记文件？',
    '哪些资料缺少可引用证据？'
  ];
  if (props.pageType.includes('governance')) {
    items.splice(2, 0, '交付治理助手能做什么？');
  }
  if (props.pageType.includes('initialization') || props.pageType.includes('onboarding')) {
    items.splice(2, 0, '为什么模板只是草案？');
  }
  return items;
});

const canAsk = computed(() => Number.isFinite(props.projectId) && question.value.trim().length > 0);
const actionBusy = computed(() => masterPlanLoading.value || missingPlanLoading.value || actionApplyLoading.value);
const selectedActionRecommendations = computed(() => {
  const selected = new Set(selectedRecommendationIds.value);
  return actionRecommendations.value.filter((item) => selected.has(item.recommendationId));
});

watch(
  () => [props.projectId, props.assetId, props.sourceView] as const,
  () => {
    answer.value = null;
    catalogPreview.value = null;
    resetActionCenter();
  }
);

onMounted(() => {
  void loadCapabilities();
});

async function loadCapabilities() {
  capabilityLoading.value = true;
  try {
    const [capabilityResult, healthResult] = await Promise.all([
      fetchHermesCapabilities(),
      fetchHermesHealth()
    ]);
    capabilities.value = capabilityResult;
    health.value = healthResult;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '数据管家能力加载失败');
  } finally {
    capabilityLoading.value = false;
  }
}

async function submitQuestion() {
  if (!canAsk.value || asking.value) return;
  asking.value = true;
  catalogLoading.value = true;
  const text = question.value.trim();
  answer.value = null;
  catalogPreview.value = null;
  try {
    const [answerResult, catalogResult] = await Promise.allSettled([
      askHermes({
        pageType: props.pageType,
        projectId: props.projectId,
        assetId: props.assetId,
        sourceView: props.sourceView,
        currentRoute: props.currentRoute,
        projectCode: props.projectCode,
        projectName: props.projectName,
        pageTitle: props.pageTitle,
        question: text
      }),
      searchCatalogPreview({
        query: text,
        projectFilters: [String(props.projectId)],
        filters: {
          assetKind: ['FILE'],
          indexEligibility: ['catalog_only']
        },
        page: {
          limit: 5,
          cursor: null
        }
      })
    ]);

    if (answerResult.status === 'fulfilled') {
      answer.value = answerResult.value;
    } else {
      throw answerResult.reason;
    }
    catalogPreview.value = catalogResult.status === 'fulfilled' ? catalogResult.value : null;
  } catch (error) {
    ElMessage.error(hermesErrorMessage(error));
  } finally {
    asking.value = false;
    catalogLoading.value = false;
  }
}

function hermesErrorMessage(error: unknown) {
  const message = error instanceof Error ? error.message : '';
  if (/timeout|exceeded|ECONNABORTED/i.test(message)) {
    return '真实 Hermes 回答超时，请稍后重试；平台未执行任何写操作。';
  }
  return message || '真实 Hermes 暂时无法回答，请稍后重试；平台未执行任何写操作。';
}

async function askQuickQuestion(value: string) {
  if (asking.value) return;
  question.value = value;
  await submitQuestion();
}

async function generateMasterDataPlan() {
  if (!Number.isFinite(props.projectId) || actionBusy.value) return;
  masterPlanLoading.value = true;
  try {
    const [assessment, overview] = await Promise.all([
      fetchOnboardingAssessment(props.projectId),
      fetchAgentGovernanceOverview(props.projectId)
    ]);
    masterDataPlan.value = buildMasterDataPlan(assessment, overview);
    actionCenterTab.value = 'draft';
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '工程主数据计划生成失败');
  } finally {
    masterPlanLoading.value = false;
  }
}

async function generateMissingDeliveryPlan() {
  if (!Number.isFinite(props.projectId) || actionBusy.value) return;
  missingPlanLoading.value = true;
  actionConfirmed.value = false;
  selectedRecommendationIds.value = [];
  actionApplyResult.value = null;
  try {
    const [missingResult, recommendationResult] = await Promise.all([
      fetchAgentGovernanceMissingItems(props.projectId, actionViewType.value, 'SECTION'),
      recommendAgentGovernanceBindings(props.projectId, {
        viewType: actionViewType.value,
        targetType: 'SECTION',
        limitPerMissingItem: 3
      })
    ]);
    missingDeliveryPlan.value = {
      viewType: actionViewType.value,
      totalMissing: missingResult.totalCount,
      recommendationCount: recommendationResult.totalCount,
      previewRows: missingResult.rows.slice(0, 5)
    };
    actionRecommendations.value = recommendationResult.rows;
    actionCenterTab.value = recommendationResult.rows.length > 0 ? 'confirm' : 'draft';
    if (recommendationResult.rows.length === 0) {
      ElMessage.info('当前没有可推荐的候选文件，请先补齐文件元数据。');
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '缺失交付方案生成失败');
  } finally {
    missingPlanLoading.value = false;
  }
}

async function applyActionRecommendations() {
  if (!Number.isFinite(props.projectId) || !actionConfirmed.value || selectedActionRecommendations.value.length === 0) return;
  actionApplyLoading.value = true;
  try {
    const selectedRows = selectedActionRecommendations.value;
    actionApplyResult.value = await applyAgentGovernanceRecommendations(props.projectId, {
      confirmed: true,
      viewType: selectedRows[0]?.viewType ?? actionViewType.value,
      targetType: 'SECTION',
      items: selectedRows.map((item) => ({
        recommendationId: item.recommendationId,
        missingItemKey: item.missingItemKey,
        targetType: item.targetType,
        targetId: item.targetId,
        deliverableTypeId: item.deliverableTypeId,
        fileResourceId: item.fileResourceId
      }))
    });
    actionCenterTab.value = 'result';
    actionRecommendations.value = [];
    selectedRecommendationIds.value = [];
    actionConfirmed.value = false;
    ElMessage.success('已按人工确认结果调用平台挂接能力');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '确认执行失败');
  } finally {
    actionApplyLoading.value = false;
  }
}

function buildMasterDataPlan(
  assessment: OnboardingAssessment,
  overview: AgentGovernanceOverview
): MasterDataActionPlan {
  const standard = overview.standardStatus;
  return {
    onboardingStatus: onboardingStatusLabel(assessment.onboardingStatus),
    sectionTreeStatus: standard.hasSectionTree ? `已建立 ${standard.sectionNodeCount} 个节点` : '未建立，需先补部位树',
    nodeTypeStatus: standard.hasNodeTypes
      ? `${standard.nodeTypeCount} 个节点类型，${standard.nodeTypesLocked ? '已锁定' : '未锁定'}`
      : '未建立，需先定义节点类型',
    deliverableStandardStatus: standard.deliverableStandardReady
      ? `已就绪，${standard.deliverableDefinitionCount} 个定义 / ${standard.deliverableTypeCount} 个类型`
      : '待补齐交付定义、交付类型或目录模板',
    documentReadiness: deliveryReadiness(overview.documentDelivery),
    drawingReadiness: deliveryReadiness(overview.drawingDelivery),
    nextSteps: uniqueStrings([...assessment.nextActions, ...overview.nextActions]).slice(0, 6),
    entrances: ['真实项目接入向导', '部位树', '节点类型', '交付物标准', '文档交付', '图纸交付']
  };
}

function resetActionCenter() {
  masterDataPlan.value = null;
  missingDeliveryPlan.value = null;
  actionRecommendations.value = [];
  selectedRecommendationIds.value = [];
  actionConfirmed.value = false;
  actionApplyResult.value = null;
  actionCenterTab.value = 'answer';
}

function fileKindLabel(value: string) {
  const labels: Record<string, string> = {
    MODEL: '模型',
    DRAWING: '图纸',
    DOCUMENT: '文档',
    SPREADSHEET: '表格',
    PRESENTATION: '汇报',
    ARCHIVE: '归档',
    FILE: '文件'
  };
  return labels[value] ?? value;
}

function viewTypeLabel(value: string) {
  return value === 'DRAWING' ? '图纸' : '文档';
}

function deliveryReadiness(item: AgentGovernanceDeliveryStatus) {
  return `${Math.round((item.completionRate ?? 0) * 100)}%，缺 ${item.missingCount} 项，待审 ${item.pendingReviewCount} 项`;
}

function onboardingStatusLabel(value: string) {
  const labels: Record<string, string> = {
    ASSET_READY: '资产目录已就绪',
    MASTER_DATA_READY: '工程主数据已初始化',
    GOVERNANCE_READY: '可进入交付治理',
    NEEDS_MASTER_DATA: '待补齐工程主数据',
    NEEDS_ASSET: '待接入资产目录'
  };
  return labels[value] ?? value;
}

function uniqueStrings(values: string[]) {
  return Array.from(new Set(values.filter((item) => item && item.trim().length > 0)));
}

function confidenceLabel(value: string) {
  const labels: Record<string, string> = {
    HIGH: '高置信',
    MEDIUM: '中置信',
    LOW: '低置信'
  };
  return labels[value] ?? value;
}

function confidenceTag(value: string) {
  if (value === 'HIGH') return 'success';
  if (value === 'MEDIUM') return 'warning';
  return 'info';
}

function resultStatusLabel(value: string) {
  const labels: Record<string, string> = {
    CREATED: '已创建',
    SKIPPED: '已跳过',
    FAILED: '失败'
  };
  return labels[value] ?? value;
}

function modeLabel(mode: string) {
  const labels: Record<string, string> = {
    catalog_only: '资产目录辅助',
    read_only_gateway: '只读网关'
  };
  return labels[mode] ?? mode;
}

function pageTypeLabel(value: string) {
  const labels: Record<string, string> = {
    assets_overview: '资产总览',
    project_detail: '项目工作台',
    real_project_onboarding: '真实项目接入向导',
    g2_real_project_onboarding: '真实项目接入向导',
    agent_governance: '交付治理助手',
    'agent-governance': '交付治理助手'
  };
  return labels[value] ?? value;
}

function healthLabel(value: HermesHealth | null) {
  if (!value) return '检测中';
  if (value.hermesAvailable) return 'Hermes 可用';
  if (!value.gatewayEnabled) return '本地目录兜底';
  return 'Hermes 暂不可用';
}

function healthTagType(value: HermesHealth | null) {
  if (!value) return 'info';
  if (value.hermesAvailable) return 'success';
  if (!value.gatewayEnabled) return 'info';
  return 'warning';
}
</script>

<style scoped>
.data-steward-panel {
  display: grid;
  gap: 14px;
}

.data-steward-panel__head {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
}

.data-steward-panel__head h3 {
  font-size: 18px;
  margin: 0 0 4px;
}

.data-steward-panel__head p {
  color: var(--el-text-color-secondary);
  margin: 0;
}

.data-steward-panel__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.data-steward-panel__context {
  background: var(--zy-bg);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 10px 12px;
}

.data-steward-panel__context div {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__context span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.data-steward-panel__context strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.data-steward-panel__context p {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
  margin: 0;
  overflow-wrap: anywhere;
}

.data-steward-panel__status {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.data-steward-panel__status-item {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
}

.data-steward-panel__status-item span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.data-steward-panel__status-item strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.data-steward-panel__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.data-steward-panel__action-center {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: 12px;
}

.data-steward-panel__action-center > header {
  align-items: flex-start;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  min-width: 0;
}

.data-steward-panel__action-center > header > div,
.data-steward-panel__answer-state,
.data-steward-panel__plan-head {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.data-steward-panel__action-center span,
.data-steward-panel__answer-state span,
.data-steward-panel__plan-copy {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.data-steward-panel__tabs {
  min-width: 0;
}

.data-steward-panel__tool-row {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__plan {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 10px;
  margin-top: 10px;
  min-width: 0;
  padding: 12px;
}

.data-steward-panel__plan-head {
  align-items: flex-start;
  display: flex;
  justify-content: space-between;
}

.data-steward-panel__plan-head h4 {
  color: var(--el-text-color-primary);
  font-size: 14px;
  margin: 0;
}

.data-steward-panel__plan-grid {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.data-steward-panel__plan-grid div {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  display: grid;
  gap: 3px;
  min-width: 0;
  padding: 8px;
}

.data-steward-panel__plan-grid span,
.data-steward-panel__compact-list,
.data-steward-panel__recommendation small,
.data-steward-panel__confirm-bar span,
.data-steward-panel__result-list span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.data-steward-panel__plan-grid strong,
.data-steward-panel__recommendation strong,
.data-steward-panel__result-list strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.data-steward-panel__compact-list {
  margin: 0;
  padding-left: 18px;
}

.data-steward-panel__compact-list li + li {
  margin-top: 4px;
}

.data-steward-panel__entrances,
.data-steward-panel__result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.data-steward-panel__recommendations,
.data-steward-panel__recommendation-list,
.data-steward-panel__result {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.data-steward-panel__recommendation {
  align-items: flex-start;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: flex;
  height: auto;
  margin-right: 0;
  padding: 10px;
  white-space: normal;
  width: 100%;
}

.data-steward-panel__recommendation :deep(.el-checkbox__label) {
  display: grid;
  flex: 1;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) auto;
  min-width: 0;
  padding-left: 8px;
  width: 100%;
}

.data-steward-panel__recommendation :deep(.el-checkbox__label > div) {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.data-steward-panel__recommendation span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-steward-panel__confirm-bar {
  align-items: center;
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  min-width: 0;
  padding: 10px;
}

.data-steward-panel__confirm-bar > div {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.data-steward-panel__result-list {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__result-list div {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 8px;
}

.data-steward-panel__thinking {
  min-width: 0;
}

.data-steward-panel__catalog {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  display: grid;
  gap: 12px;
  min-width: 0;
  padding: 12px;
}

.data-steward-panel__catalog header {
  align-items: flex-start;
  display: flex;
  gap: 10px;
  justify-content: space-between;
  min-width: 0;
}

.data-steward-panel__catalog header > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.data-steward-panel__catalog header span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.data-steward-panel__catalog-list {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.data-steward-panel__catalog-item {
  background: var(--el-fill-color-lighter);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px;
}

.data-steward-panel__catalog-item strong,
.data-steward-panel__catalog-item span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-steward-panel__catalog-item span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.data-steward-panel__catalog-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

@media (max-width: 640px) {
  .data-steward-panel__status,
  .data-steward-panel__plan-grid {
    grid-template-columns: 1fr;
  }

  .data-steward-panel__action-center > header,
  .data-steward-panel__confirm-bar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
