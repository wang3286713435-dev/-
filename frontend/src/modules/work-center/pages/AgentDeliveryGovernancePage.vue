<template>
  <section class="mvp-page agent-governance-page">
    <div class="mvp-page__header">
      <div>
        <h1>交付治理助手</h1>
        <p>{{ projectLabel }}，只做体检、推荐和人工确认挂接，不自动替你改数据。</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="ChatDotRound" @click="hermesDrawerVisible = true">问 Hermes</el-button>
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <el-alert
      title="助手不会读取 PDF、Office、DWG、RVT 或 IFC 正文，不移动 NAS 文件，也不会自动挂接。只有人工勾选并确认后，才调用平台已有批量挂接能力。"
      type="info"
      show-icon
      :closable="false"
    />

    <section v-loading="loading" class="governance-layout">
      <section class="health-band">
        <article class="health-summary">
          <span>Agent 总结</span>
          <strong>{{ overview?.summaryText ?? '正在读取项目交付体检结果。' }}</strong>
          <ul>
            <li v-for="item in overview?.nextActions ?? []" :key="item">{{ item }}</li>
          </ul>
        </article>
        <article class="standard-panel">
          <h2>工程主数据</h2>
          <div class="standard-list">
            <div>
              <span>部位树</span>
              <el-tag :type="overview?.standardStatus.hasSectionTree ? 'success' : 'warning'">
                {{ overview?.standardStatus.hasSectionTree ? '已建立' : '未建立' }}
              </el-tag>
            </div>
            <div>
              <span>节点类型</span>
              <el-tag :type="overview?.standardStatus.nodeTypesLocked ? 'success' : 'warning'">
                {{ overview?.standardStatus.nodeTypesLocked ? '已锁定' : '未锁定' }}
              </el-tag>
            </div>
            <div>
              <span>交付标准</span>
              <el-tag :type="overview?.standardStatus.deliverableStandardReady ? 'success' : 'warning'">
                {{ overview?.standardStatus.deliverableStandardReady ? '已就绪' : '待补齐' }}
              </el-tag>
            </div>
          </div>
          <p v-if="overview && !overview.standardStatus.hasSectionTree">部位树还没建好，平台还不知道资料应该交到哪个楼层或系统。</p>
          <p v-else-if="overview && !overview.standardStatus.nodeTypesLocked">节点类型尚未锁定，说明交付规则还可能变化，建议先锁定后再批量挂接。</p>
          <p v-else>主数据状态会影响缺失项计算和推荐是否可靠。</p>
        </article>
      </section>

      <section class="metric-grid">
        <article class="metric-panel">
          <span>文档交付</span>
          <strong>{{ deliveryRate(overview?.documentDelivery) }}</strong>
          <em>缺 {{ overview?.documentDelivery.missingCount ?? 0 }} 项，待审 {{ overview?.documentDelivery.pendingReviewCount ?? 0 }} 项</em>
        </article>
        <article class="metric-panel">
          <span>图纸交付</span>
          <strong>{{ deliveryRate(overview?.drawingDelivery) }}</strong>
          <em>缺 {{ overview?.drawingDelivery.missingCount ?? 0 }} 项，待审 {{ overview?.drawingDelivery.pendingReviewCount ?? 0 }} 项</em>
        </article>
        <article class="metric-panel">
          <span>整改待处理</span>
          <strong>{{ overview?.rectificationPendingCount ?? 0 }}</strong>
          <em>未关闭整改会阻塞交付准备</em>
        </article>
        <article class="metric-panel">
          <span>交付包预检查</span>
          <strong>{{ overview?.exportPrecheckSummary.blockedCount ?? 0 }}</strong>
          <em>阻塞项，状态 {{ packageStatusLabel(overview?.packageStatus) }}</em>
        </article>
      </section>

      <section class="action-strip">
        <el-button size="small" @click="go('project-master-data-sections')">去部位树</el-button>
        <el-button size="small" @click="go('project-master-data-node-types')">去节点类型</el-button>
        <el-button size="small" @click="go('project-master-data-deliverable-standard')">去交付物标准</el-button>
        <el-button size="small" @click="goAssetFiles">去文件管理</el-button>
        <el-button size="small" @click="go('project-work-document-delivery')">去文档交付</el-button>
        <el-button size="small" @click="go('project-work-drawing-delivery')">去图纸交付</el-button>
        <el-button size="small" @click="go('project-work-rectifications')">去整改闭环</el-button>
      </section>

      <section class="missing-panel">
        <div class="section-head">
          <div>
            <h2>缺失项解释</h2>
            <span>这里解释为什么当前被算作缺资料。</span>
          </div>
          <el-segmented v-model="activeViewType" :options="viewOptions" @change="handleViewChange" />
        </div>

        <el-table :data="activeMissingRows" class="master-table" max-height="320" empty-text="暂无缺失项">
          <el-table-column prop="targetName" label="目标" min-width="150" />
          <el-table-column prop="deliverableDefinitionName" label="交付定义" min-width="170" />
          <el-table-column prop="deliverableTypeName" label="应补文件类型" min-width="160" />
          <el-table-column prop="explanation" label="解释" min-width="300" show-overflow-tooltip />
        </el-table>
      </section>

      <section class="recommend-panel">
        <div class="section-head">
          <div>
            <h2>推荐挂接方案</h2>
            <span>推荐只看当前项目元数据。低置信或元数据不完整时，请先治理文件信息。</span>
          </div>
          <el-button type="primary" :icon="Search" :loading="recommendLoading" @click="generateRecommendations">
            生成{{ activeViewTypeLabel }}推荐
          </el-button>
        </div>

        <el-table
          :data="recommendations"
          class="master-table"
          row-key="recommendationId"
          max-height="360"
          empty-text="暂无推荐，请先生成推荐方案"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="44" />
          <el-table-column prop="confidence" label="置信度" width="95">
            <template #default="{ row }">
              <el-tag :type="confidenceTag(row.confidence)" size="small">{{ confidenceLabel(row.confidence) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetName" label="挂接目标" min-width="140" />
          <el-table-column prop="deliverableTypeName" label="缺失类型" min-width="150" />
          <el-table-column prop="fileName" label="推荐文件" min-width="220" show-overflow-tooltip />
          <el-table-column prop="versionNo" label="版本" width="90" />
          <el-table-column prop="statusLabel" label="预览状态" width="120" />
          <el-table-column prop="recommendationReason" label="推荐原因" min-width="210" show-overflow-tooltip />
          <el-table-column label="风险提示" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.riskWarnings.length ? row.riskWarnings.join('；') : '无明显风险' }}
            </template>
          </el-table-column>
        </el-table>

        <div class="confirm-panel">
          <el-checkbox v-model="confirmed">
            我已人工核对这些推荐，确认由平台批量挂接到对应目标。
          </el-checkbox>
          <div>
            <span>已选 {{ selectedRecommendations.length }} 条</span>
            <el-button
              type="success"
              :icon="Link"
              :loading="applyLoading"
              :disabled="!confirmed || selectedRecommendations.length === 0"
              @click="applySelectedRecommendations"
            >
              确认挂接
            </el-button>
          </div>
        </div>
      </section>

      <section v-if="applyResult" class="result-panel">
        <div class="section-head">
          <div>
            <h2>执行结果</h2>
            <span>挂接完成后已刷新体检、缺失项和完整率。</span>
          </div>
          <div class="result-tags">
            <el-tag type="success">创建 {{ applyResult.createdCount }}</el-tag>
            <el-tag type="warning">跳过 {{ applyResult.skippedCount }}</el-tag>
            <el-tag type="danger">失败 {{ applyResult.failedCount }}</el-tag>
          </div>
        </div>
        <el-table :data="applyResult.results" class="master-table" max-height="260">
          <el-table-column prop="fileResourceId" label="文件ID" width="90" />
          <el-table-column prop="status" label="结果" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'CREATED' ? 'success' : row.status === 'SKIPPED' ? 'warning' : 'danger'" size="small">
                {{ row.status === 'CREATED' ? '已创建' : row.status === 'SKIPPED' ? '已跳过' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bindingId" label="绑定ID" width="90" />
          <el-table-column prop="message" label="说明" min-width="240" />
        </el-table>
      </section>
    </section>

    <el-drawer v-model="hermesDrawerVisible" title="Hermes 只读辅助" size="520px">
      <DataStewardPanel
        v-if="projectId"
        :project-id="projectId"
        page-type="agent_governance"
        source-view="ProjectAssetView"
        :current-route="route.fullPath"
        :project-code="currentProject?.code"
        :project-name="currentProject?.name"
        page-title="交付治理助手"
      />
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ChatDotRound, Link, Refresh, Search } from '@element-plus/icons-vue';

import DataStewardPanel from '@/modules/data-steward/components/DataStewardPanel.vue';
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
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(false);
const recommendLoading = ref(false);
const applyLoading = ref(false);
const hermesDrawerVisible = ref(false);
const overview = ref<AgentGovernanceOverview | null>(null);
const missingItems = ref<AgentGovernanceMissingItem[]>([]);
const recommendations = ref<AgentBindingRecommendation[]>([]);
const selectedRecommendations = ref<AgentBindingRecommendation[]>([]);
const applyResult = ref<ApplyAgentRecommendationsResponse | null>(null);
const confirmed = ref(false);
const activeViewType = ref<'DOCUMENT' | 'DRAWING'>('DOCUMENT');

const projectId = computed(() => {
  const routeId = Number(route.params.projectId);
  return Number.isFinite(routeId) && routeId > 0 ? routeId : authStore.currentProjectId;
});
const currentProject = computed(() => authStore.currentUser?.projects.find((item) => item.id === projectId.value) ?? null);
const projectLabel = computed(() => {
  const current = currentProject.value;
  return current ? `${current.code} ${current.name}` : '等待项目上下文';
});
const activeViewTypeLabel = computed(() => activeViewType.value === 'DRAWING' ? '图纸' : '文档');
const activeMissingRows = computed(() => missingItems.value.filter((row) => row.viewType === activeViewType.value));
const viewOptions = [
  { label: '文档缺失', value: 'DOCUMENT' },
  { label: '图纸缺失', value: 'DRAWING' }
];

watch(projectId, () => {
  void loadAll();
}, { immediate: true });

async function loadAll(autoRecommend = true) {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [overviewResult, missingResult] = await Promise.all([
      fetchAgentGovernanceOverview(projectId.value),
      fetchAgentGovernanceMissingItems(projectId.value, undefined, 'SECTION')
    ]);
    overview.value = overviewResult;
    missingItems.value = missingResult.rows;
    if (autoRecommend && recommendations.value.length === 0 && missingResult.rows.some((row) => row.viewType === activeViewType.value)) {
      await generateRecommendations();
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '交付治理助手加载失败');
  } finally {
    loading.value = false;
  }
}

async function generateRecommendations() {
  if (!projectId.value) return;
  recommendLoading.value = true;
  selectedRecommendations.value = [];
  confirmed.value = false;
  applyResult.value = null;
  try {
    const result = await recommendAgentGovernanceBindings(projectId.value, {
      viewType: activeViewType.value,
      targetType: 'SECTION',
      limitPerMissingItem: 3
    });
    recommendations.value = result.rows;
    if (result.rows.length === 0) {
      ElMessage.info('当前没有可推荐的候选文件，请先在文件管理中补齐当前项目文件元数据。');
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '推荐方案生成失败');
  } finally {
    recommendLoading.value = false;
  }
}

async function applySelectedRecommendations() {
  if (!projectId.value || !confirmed.value || selectedRecommendations.value.length === 0) return;
  applyLoading.value = true;
  try {
    applyResult.value = await applyAgentGovernanceRecommendations(projectId.value, {
      confirmed: true,
      viewType: activeViewType.value,
      targetType: 'SECTION',
      items: selectedRecommendations.value.map((item) => ({
        recommendationId: item.recommendationId,
        missingItemKey: item.missingItemKey,
        targetType: item.targetType,
        targetId: item.targetId,
        deliverableTypeId: item.deliverableTypeId,
        fileResourceId: item.fileResourceId
      }))
    });
    ElMessage.success('已按人工确认结果调用批量挂接');
    recommendations.value = [];
    selectedRecommendations.value = [];
    confirmed.value = false;
    await loadAll(false);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '确认挂接失败');
  } finally {
    applyLoading.value = false;
  }
}

function handleSelectionChange(rows: AgentBindingRecommendation[]) {
  selectedRecommendations.value = rows;
}

function handleViewChange() {
  recommendations.value = [];
  selectedRecommendations.value = [];
  confirmed.value = false;
  applyResult.value = null;
}

function deliveryRate(item: AgentGovernanceDeliveryStatus | null | undefined) {
  if (!item) return '0%';
  return `${Math.round((item.completionRate ?? 0) * 100)}%`;
}

function packageStatusLabel(value: string | undefined) {
  return value === 'READY' ? '已就绪' : '需处理';
}

function confidenceLabel(value: string) {
  const labels: Record<string, string> = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低'
  };
  return labels[value] ?? value;
}

function confidenceTag(value: string) {
  if (value === 'HIGH') return 'success';
  if (value === 'MEDIUM') return 'warning';
  return 'info';
}

function go(name: string) {
  if (!projectId.value) return;
  void router.push({ name, params: { projectId: projectId.value } });
}

function goAssetFiles() {
  if (!projectId.value) return;
  void router.push({ name: 'data-steward-asset-detail', params: { projectId: projectId.value }, query: { tab: 'files' } });
}
</script>

<style scoped>
.agent-governance-page {
  min-width: 0;
}

.governance-layout {
  display: grid;
  gap: 14px;
}

.health-band {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.6fr);
  gap: 14px;
}

.health-summary,
.standard-panel,
.metric-panel,
.missing-panel,
.recommend-panel,
.result-panel {
  min-width: 0;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #ffffff;
}

.health-summary span,
.metric-panel span,
.section-head span {
  color: #64748b;
  font-size: 12px;
}

.health-summary strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.4;
}

.health-summary ul,
.standard-panel p {
  margin: 10px 0 0;
  color: #334155;
  line-height: 1.7;
}

.standard-panel h2,
.section-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
}

.standard-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.standard-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-panel strong {
  display: block;
  margin: 6px 0 4px;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.15;
}

.metric-panel em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.action-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.confirm-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  padding: 12px;
  border-radius: 8px;
  background: #f8fafc;
}

.confirm-panel > div {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1024px) {
  .health-band,
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .health-band,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .section-head,
  .confirm-panel {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
