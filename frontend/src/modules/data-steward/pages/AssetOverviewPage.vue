<template>
  <section class="mvp-page asset-page">
    <div class="mvp-page__header">
      <div>
        <h1>资产总览</h1>
        <p>默认聚焦真实 NAS 项目，样例、测试和归档项目需主动切换查看</p>
      </div>
      <div class="mvp-page__actions">
        <el-segmented v-model="sourceFilter" :options="sourceOptions" @change="loadPage" />
        <el-input
          v-model="keyword"
          class="asset-search"
          clearable
          placeholder="项目编码、名称"
          :prefix-icon="Search"
          @keyup.enter="loadPage"
          @clear="loadPage"
        />
        <el-select v-model="projectSortOrder" class="asset-sort" aria-label="项目ID排序">
          <el-option label="项目ID升序" value="ASC" />
          <el-option label="项目ID降序" value="DESC" />
        </el-select>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </div>

    <section class="asset-governance-hero">
      <div class="asset-governance-hero__intro">
        <span>G2 真实项目治理入口</span>
        <h2>从真实 NAS 资产进入数字化交付</h2>
        <p>
          这里先识别真实项目，再按资产目录、接入评估、主数据草案和交付治理助手推进。页面只展示目录级治理线索，不读取文件正文，也不触碰 NAS 文件。
        </p>
      </div>

      <div class="asset-governance-layer">
        <header>
          <strong>平台目标</strong>
          <span>把既有项目从“文件已接管”推进到“可解释、可复核、可交付”。</span>
        </header>
        <div class="asset-objective-track">
          <article v-for="item in objectiveSteps" :key="item.label">
            <strong>{{ item.label }}</strong>
            <span>{{ item.description }}</span>
          </article>
        </div>
      </div>

      <div class="asset-governance-layer">
        <header>
          <strong>项目状态</strong>
          <span>按当前已接管真实 NAS 项目统计，不把 smoke / 测试项目混入默认视图。</span>
        </header>
        <div class="asset-status-grid">
          <article v-for="item in statusCards" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <em>{{ item.unit }}</em>
          </article>
        </div>
      </div>

      <div class="asset-governance-layer">
        <header>
          <strong>下一步动作</strong>
          <span>选择一个真实项目后，按治理路径继续处理。</span>
        </header>
        <div class="asset-action-grid">
          <button
            v-for="item in actionItems"
            :key="item.key"
            class="asset-action-tile"
            type="button"
            :disabled="!primaryActionProject"
            @click="openHeroAction(item.key)"
          >
            <strong>{{ item.label }}</strong>
            <span>{{ item.description }}</span>
          </button>
        </div>
      </div>

      <div class="asset-governance-layer">
        <header>
          <strong>风险提醒</strong>
          <span>这些问题会影响后续接入评估、交付缺失解释和人工挂接。</span>
        </header>
        <div class="asset-risk-strip">
          <article v-for="risk in riskSummaryCards" :key="risk.label" :class="{ 'is-warning': risk.count > 0 }">
            <span>{{ risk.label }}</span>
            <strong>{{ formatCount(risk.count) }}</strong>
            <em>{{ risk.helper }}</em>
          </article>
        </div>
      </div>
    </section>

    <el-table
      v-loading="loading"
      :data="sortedProjects"
      class="master-table"
      empty-text="暂无资产项目"
      @row-dblclick="openDetail"
    >
      <el-table-column label="项目" min-width="260">
        <template #default="{ row }">
          <div class="asset-title-cell">
            <strong>{{ row.code }}</strong>
            <span>{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="项目来源" width="130">
        <template #default="{ row }">
          <el-tag size="small" :type="sourceTagType(row.projectSource, row.projectCategory)">
            {{ projectSourceText(row.projectSource, row.projectCategory) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="接入状态" width="150">
        <template #default="{ row }">
          <el-tag size="small" :type="onboardingTagType(row.onboardingStatus)">
            {{ onboardingStatusText(row.onboardingStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="projectStage" label="阶段" width="120" show-overflow-tooltip />
      <el-table-column prop="projectManagerName" label="负责人" width="130" show-overflow-tooltip />
      <el-table-column label="治理路径" min-width="300">
        <template #default="{ row }">
          <div class="asset-governance-cell">
            <div class="asset-governance-trail">
              <span
                v-for="step in governanceTrail(row)"
                :key="`${row.projectId}-${step.key}`"
                :class="{ 'is-current': step.current, 'is-done': step.done }"
              >
                {{ step.label }}
              </span>
            </div>
            <small>{{ governanceStage(row).hint }}</small>
            <el-button text :icon="ArrowRight" @click.stop="openGovernanceNext(row)">
              {{ governanceStage(row).actionLabel }}
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="文件数" width="100" align="right">
        <template #default="{ row }">{{ formatCount(row.fileCount) }}</template>
      </el-table-column>
      <el-table-column label="模型数" width="110" align="right">
        <template #default="{ row }">{{ formatCount(row.modelCount) }}</template>
      </el-table-column>
      <el-table-column label="主要类型" min-width="160">
        <template #default="{ row }">
          <div class="asset-kind-tags">
            <el-tag
              v-for="kind in row.dominantFileKinds || []"
              :key="`${row.projectId}-${kind}`"
              size="small"
              type="info"
            >
              {{ kind }}
            </el-tag>
            <span v-if="!(row.dominantFileKinds || []).length">-</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="模型容量" width="130" align="right">
        <template #default="{ row }">{{ formatBytes(row.totalSizeBytes) }}</template>
      </el-table-column>
      <el-table-column label="最近扫描/更新" width="170">
        <template #default="{ row }">{{ formatDate(row.lastScanAt || row.lastModelUpdatedAt) }}</template>
      </el-table-column>
      <el-table-column label="底座" width="160">
        <template #default="{ row }">
          <div class="asset-foundation-tags">
            <el-tag size="small" :type="row.hasMasterData ? 'success' : 'info'">主数据</el-tag>
            <el-tag size="small" :type="row.hasDeliveryStandard ? 'success' : 'info'">交付标准</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.assetStatus === 'ACTIVE' ? 'success' : 'info'">{{ row.assetStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text :icon="ArrowRight" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowRight, Refresh, Search } from '@element-plus/icons-vue';

import {
  fetchAssetProjects,
  fetchAssetQualityOverview,
  type AssetProject,
  type AssetQualityOverview
} from '@/modules/data-steward/api/dataSteward';

const router = useRouter();
const loading = ref(false);
const keyword = ref('');
const sourceFilter = ref('REAL_NAS');
const projectSortOrder = ref<'ASC' | 'DESC'>('ASC');
const projects = ref<AssetProject[]>([]);
const qualityOverview = ref<AssetQualityOverview | null>(null);
const sourceOptions = [
  { label: '真实项目', value: 'REAL_NAS' },
  { label: '未完成接入', value: 'UNFINISHED_ONBOARDING' },
  { label: '样例/模板', value: 'SAMPLE_TEMPLATE' },
  { label: '测试', value: 'TEST_PROJECT' },
  { label: '归档', value: 'ARCHIVED_HISTORY' },
  { label: '全部', value: 'ALL' }
];

const objectiveSteps = [
  { label: '真实项目接入', description: '先把已接管 NAS 项目识别清楚，区分测试、样例和归档。' },
  { label: '工程主数据准备', description: '用目录和文件元数据形成接入评估，再生成待确认草案。' },
  { label: '交付治理闭环', description: '进入助手解释缺失项，由人工确认后再挂接文件。' }
];

const realNasProjects = computed(() => projects.value.filter(isRealNasProject));

const primaryActionProject = computed(() => {
  return visibleProjects.value.find(isRealNasProject) ?? realNasProjects.value[0] ?? visibleProjects.value[0] ?? null;
});

const statusCards = computed(() => {
  const rows = realNasProjects.value;
  const registered = rows.filter((item) => Number(item.fileCount ?? 0) > 0).length;
  const masterData = rows.filter((item) => Boolean(item.hasMasterData)).length;
  const governance = rows.filter((item) => Boolean(item.governanceReady) || item.onboardingStatus === 'GOVERNANCE_READY').length;
  const pending = rows.filter((item) => !Boolean(item.hasMasterData) || !Boolean(item.hasDeliveryStandard)).length;
  return [
    { label: '真实 NAS 项目', value: formatCount(rows.length), unit: '个' },
    { label: '已登记资产', value: formatCount(registered), unit: '个项目' },
    { label: '已初始化主数据', value: formatCount(masterData), unit: '个项目' },
    { label: '已进入交付治理', value: formatCount(governance), unit: '个项目' },
    { label: '待接入 / 待治理', value: formatCount(pending), unit: '需处理' }
  ];
});

const actionItems = [
  { key: 'enter', label: '进入真实项目', description: '打开项目工作台，先看资产目录、容量和治理风险。' },
  { key: 'assessment', label: '查看接入评估', description: '查看目录线索和主数据缺口，确认模板只是草案。' },
  { key: 'master-data', label: '完善工程主数据', description: '补齐部位树、节点类型、交付定义和目录模板。' },
  { key: 'governance', label: '进入交付治理助手', description: '解释缺失项，生成候选文件，等待人工确认挂接。' }
] as const;

const riskSummaryCards = computed(() => {
  const rows = realNasProjects.value;
  return [
    { label: '缺工程主数据', count: rows.filter((item) => !Boolean(item.hasMasterData)).length, helper: '影响接入评估' },
    { label: '缺交付标准', count: rows.filter((item) => !Boolean(item.hasDeliveryStandard)).length, helper: '影响缺失项计算' },
    { label: '待审核', count: Number(qualityOverview.value?.pendingReviewCount ?? 0), helper: '需人工确认' },
    { label: '缺 checksum', count: Number(qualityOverview.value?.missingChecksumCount ?? 0), helper: '影响重复识别' },
    { label: '低置信度', count: Number(qualityOverview.value?.missingConfidenceCount ?? 0), helper: '需补元数据' }
  ];
});

const sortedProjects = computed(() => {
  return [...visibleProjects.value].sort((left, right) => {
    return projectSortOrder.value === 'ASC'
      ? left.projectId - right.projectId
      : right.projectId - left.projectId;
  });
});

const visibleProjects = computed(() => {
  if (sourceFilter.value === 'ALL') {
    return projects.value;
  }
  if (sourceFilter.value === 'REAL_NAS') {
    return projects.value.filter((item) => item.projectCategory === 'REAL_NAS_PROJECT' || item.projectSource === 'REAL_NAS');
  }
  return projects.value.filter((item) => item.projectCategory === sourceFilter.value || item.projectSource === sourceFilter.value);
});

loadPage();

async function loadPage() {
  loading.value = true;
  try {
    const assetSource = sourceFilter.value === 'REAL_NAS' ? 'NAS_REAL*' : undefined;
    const [nextProjects, nextQualityOverview] = await Promise.all([
      fetchAssetProjects(keyword.value.trim() || undefined, assetSource),
      fetchAssetQualityOverview(undefined, assetSource)
    ]);
    projects.value = nextProjects;
    qualityOverview.value = nextQualityOverview;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资产总览加载失败');
  } finally {
    loading.value = false;
  }
}

function openDetail(row: AssetProject) {
  router.push({ name: 'data-steward-asset-detail', params: { projectId: row.projectId } });
}

function openHeroAction(action: typeof actionItems[number]['key']) {
  const project = primaryActionProject.value;
  if (!project) return;
  if (action === 'enter') {
    openDetail(project);
    return;
  }
  if (action === 'assessment') {
    router.push({ name: 'project-master-data-initialization', params: { projectId: project.projectId } });
    return;
  }
  if (action === 'master-data') {
    router.push({ name: 'project-master-data-deliverable-standard', params: { projectId: project.projectId } });
    return;
  }
  router.push({ name: 'project-work-agent-governance', params: { projectId: project.projectId } });
}

function openGovernanceNext(row: AssetProject) {
  const stage = governanceStage(row);
  if (stage.routeName === 'data-steward-asset-detail') {
    openDetail(row);
    return;
  }
  router.push({ name: stage.routeName, params: { projectId: row.projectId } });
}

function governanceStage(row: AssetProject) {
  const status = row.onboardingStatus;
  if (status === 'GOVERNANCE_READY') {
    return {
      index: 4,
      actionLabel: '看缺失解释',
      hint: '底座已具备，下一步进入交付治理助手解释缺项并人工确认挂接。',
      routeName: 'project-work-agent-governance'
    } as const;
  }
  if (status === 'MASTERDATA_INITIALIZED') {
    return {
      index: 3,
      actionLabel: '进入治理助手',
      hint: '主数据已初始化，下一步用交付治理助手检查文档和图纸缺口。',
      routeName: 'project-work-agent-governance'
    } as const;
  }
  if (status === 'ASSETS_REGISTERED') {
    return {
      index: 1,
      actionLabel: '查看接入评估',
      hint: '资产目录已登记，下一步查看接入评估并确认主数据草案。',
      routeName: 'project-master-data-initialization'
    } as const;
  }
  if (status === 'PATH_MAPPED') {
    return {
      index: 1,
      actionLabel: '查看接入评估',
      hint: '路径已映射但资产或主数据仍需补齐，先看接入评估。',
      routeName: 'project-master-data-initialization'
    } as const;
  }
  return {
    index: 0,
    actionLabel: '进入项目',
    hint: '先进入项目工作台，确认资产目录和接入前置条件。',
    routeName: 'data-steward-asset-detail'
  } as const;
}

function governanceTrail(row: AssetProject) {
  const stage = governanceStage(row);
  const labels = [
    ['catalog', '资产目录'],
    ['assessment', '接入评估'],
    ['draft', '主数据草案'],
    ['governance', '交付治理'],
    ['missing', '缺失解释'],
    ['binding', '人工挂接']
  ] as const;
  return labels.map(([key, label], index) => ({
    key,
    label,
    done: index < stage.index,
    current: index === stage.index
  }));
}

function isRealNasProject(row: AssetProject) {
  return row.projectCategory === 'REAL_NAS_PROJECT' || row.projectSource === 'REAL_NAS';
}

function formatCount(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('zh-CN');
}

function formatBytes(value: number | null | undefined) {
  const size = Number(value ?? 0);
  if (size <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let next = size;
  let unit = 0;
  while (next >= 1024 && unit < units.length - 1) {
    next /= 1024;
    unit += 1;
  }
  return `${next >= 100 || unit === 0 ? next.toFixed(0) : next.toFixed(2)} ${units[unit]}`;
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

function projectSourceText(value?: string | null, category?: string | null) {
  if (category === 'TEST_PROJECT') return '测试';
  if (category === 'SAMPLE_TEMPLATE') return '样例/模板';
  if (category === 'ARCHIVED_HISTORY') return '归档';
  if (category === 'REAL_NAS_PROJECT') return '真实 NAS';
  const labels: Record<string, string> = {
    REAL_NAS: '真实 NAS',
    SAMPLE_TEMPLATE: '样例/模板',
    TEST: '测试',
    ARCHIVED_HISTORY: '归档',
    MANUAL: '手工/API'
  };
  return labels[value || ''] ?? '未分类';
}

function sourceTagType(value?: string | null, category?: string | null) {
  if (category === 'REAL_NAS_PROJECT' || value === 'REAL_NAS') return 'success';
  if (category === 'TEST_PROJECT' || value === 'TEST') return 'warning';
  if (category === 'SAMPLE_TEMPLATE' || value === 'SAMPLE_TEMPLATE') return 'info';
  return 'info';
}

function onboardingStatusText(value?: string | null) {
  const labels: Record<string, string> = {
    GOVERNANCE_READY: '治理就绪',
    MASTERDATA_INITIALIZED: '主数据已初始化',
    ASSETS_REGISTERED: '资产已登记',
    PATH_MAPPED: '路径已映射',
    NOT_ONBOARDED: '待接入'
  };
  return labels[value || ''] ?? '待评估';
}

function onboardingTagType(value?: string | null) {
  if (value === 'GOVERNANCE_READY') return 'success';
  if (value === 'MASTERDATA_INITIALIZED' || value === 'ASSETS_REGISTERED') return 'warning';
  return 'info';
}

</script>

<style scoped>
.asset-page {
  min-width: 0;
}

.asset-search {
  width: 260px;
}

.asset-sort {
  width: 130px;
}

.asset-governance-hero {
  background: #fbfdfa;
  border: 1px solid rgba(20, 184, 166, 0.2);
  border-radius: 8px;
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 16px;
}

.asset-governance-hero__intro {
  display: grid;
  gap: 6px;
  max-width: 860px;
}

.asset-governance-hero__intro span {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.asset-governance-hero__intro h2 {
  color: #0f172a;
  font-size: 24px;
  line-height: 1.25;
  margin: 0;
}

.asset-governance-hero__intro p {
  color: #475569;
  line-height: 1.7;
  margin: 0;
}

.asset-governance-layer {
  border-top: 1px solid rgba(15, 118, 110, 0.12);
  display: grid;
  gap: 10px;
  min-width: 0;
  padding-top: 12px;
}

.asset-governance-layer header {
  align-items: baseline;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  min-width: 0;
}

.asset-governance-layer header strong {
  color: #0f172a;
  font-size: 15px;
}

.asset-governance-layer header span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.5;
}

.asset-objective-track,
.asset-status-grid,
.asset-action-grid,
.asset-risk-strip {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.asset-objective-track {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.asset-status-grid,
.asset-risk-strip {
  grid-template-columns: repeat(auto-fit, minmax(136px, 1fr));
}

.asset-action-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.asset-objective-track article,
.asset-status-grid article,
.asset-risk-strip article,
.asset-action-tile {
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 12px;
  text-align: left;
}

.asset-objective-track article {
  background: #f0fdfa;
}

.asset-action-tile {
  color: inherit;
  cursor: pointer;
}

.asset-action-tile:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.asset-objective-track strong,
.asset-action-tile strong {
  color: #0f172a;
  font-size: 14px;
}

.asset-objective-track span,
.asset-action-tile span,
.asset-risk-strip em,
.asset-status-grid em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  line-height: 1.5;
}

.asset-status-grid span,
.asset-risk-strip span {
  color: #64748b;
  font-size: 12px;
}

.asset-status-grid strong,
.asset-risk-strip strong {
  color: #0f172a;
  font-size: 21px;
}

.asset-risk-strip article.is-warning {
  background: #fff7ed;
  border-color: rgba(251, 146, 60, 0.28);
}

.asset-title-cell {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.asset-title-cell strong,
.asset-title-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-title-cell span {
  color: #64748b;
}

.asset-kind-tags,
.asset-foundation-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.asset-governance-cell {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.asset-governance-cell small {
  color: #64748b;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.asset-governance-trail {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  min-width: 0;
}

.asset-governance-trail span {
  background: #f1f5f9;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1;
  padding: 5px 6px;
}

.asset-governance-trail span.is-done {
  background: #ecfdf5;
  border-color: rgba(16, 185, 129, 0.22);
  color: #047857;
}

.asset-governance-trail span.is-current {
  background: #eff6ff;
  border-color: rgba(59, 130, 246, 0.26);
  color: #1d4ed8;
  font-weight: 700;
}

@media (max-width: 960px) {
  .asset-search,
  .asset-sort {
    width: 100%;
  }

  .asset-objective-track {
    grid-template-columns: 1fr;
  }
}
</style>
