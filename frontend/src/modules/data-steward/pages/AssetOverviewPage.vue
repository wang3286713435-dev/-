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

    <section class="asset-hero">
      <div class="asset-hero__intro">
        <span class="asset-hero__eyebrow">
          <span class="zy-code-chip">ASSETS · OVERVIEW</span>
          <small>真实 NAS 项目优先 · 不读取文件正文 · 不触碰 NAS 文件</small>
        </span>
        <h2>先选真实项目，再进入数字化交付工作区</h2>
        <p>
          默认只看已经接管的真实 NAS 项目。先确认资产已登记，再进入接入评估、工程主数据、文档/图纸交付与整改闭环。
        </p>
      </div>

      <div class="asset-hero__workflow">
        <header class="asset-hero__section-head">
          <span class="zy-code-chip">FLOW</span>
          <strong>三步工作链</strong>
          <small>每一步都对应下方表格的「下一步动作」</small>
        </header>
        <ol class="asset-flow">
          <li
            v-for="(item, index) in objectiveSteps"
            :key="item.label"
            class="asset-flow__step"
            :data-index="String(index + 1).padStart(2, '0')"
          >
            <div class="asset-flow__head">
              <span class="asset-flow__num">{{ String(index + 1).padStart(2, '0') }}</span>
              <strong>{{ item.label }}</strong>
            </div>
            <p>{{ item.description }}</p>
            <button
              v-if="actionForStep(index)"
              class="asset-flow__cta"
              type="button"
              :disabled="!primaryActionProject"
              @click="openHeroAction(actionForStep(index)!.key)"
            >
              {{ actionForStep(index)!.label }}
              <span aria-hidden="true">→</span>
            </button>
          </li>
        </ol>
      </div>

      <div class="asset-hero__status">
        <header class="asset-hero__section-head">
          <span class="zy-code-chip">STATE</span>
          <strong>当前视图统计</strong>
          <small>{{ statusScopeText }}</small>
        </header>
        <div class="asset-status-grid">
          <article v-for="item in statusCards" :key="item.label">
            <span class="asset-status-grid__label">{{ item.label }}</span>
            <strong class="asset-status-grid__value">{{ item.value }}</strong>
            <em class="asset-status-grid__unit">{{ item.unit }}</em>
          </article>
        </div>
      </div>

      <div class="asset-hero__risk">
        <header class="asset-hero__section-head">
          <span class="zy-code-chip">ALERT</span>
          <strong>风险提醒</strong>
          <small>这些问题会影响后续接入评估、交付缺失解释和人工挂接</small>
        </header>
        <div class="asset-risk-strip">
          <article v-for="risk in riskSummaryCards" :key="risk.label" :class="{ 'is-warning': risk.count > 0 }">
            <span class="asset-risk-strip__label">
              <span class="zy-status-dot" :class="risk.count > 0 ? 'zy-status-dot--warning' : 'zy-status-dot--success'"></span>
              {{ risk.label }}
            </span>
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
      <el-table-column label="下一步动作" min-width="320">
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
  fetchAssetStatistics,
  type AssetProject,
  type AssetQualityOverview,
  type AssetStatistics
} from '@/modules/data-steward/api/dataSteward';

const router = useRouter();
const loading = ref(false);
const keyword = ref('');
const sourceFilter = ref('REAL_NAS');
const projectSortOrder = ref<'ASC' | 'DESC'>('ASC');
const projects = ref<AssetProject[]>([]);
const statistics = ref<AssetStatistics | null>(null);
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
  { label: '1. 看资产', description: '确认项目、目录、文件数量和最近扫描状态。' },
  { label: '2. 补底座', description: '维护部位树、节点类型、交付标准和目录模板。' },
  { label: '3. 做交付', description: '进入文档/图纸交付，处理审核、整改和预检查。' }
];

const realNasProjects = computed(() => projects.value.filter(isRealNasProject));

const primaryActionProject = computed(() => {
  return visibleProjects.value.find(isRealNasProject) ?? realNasProjects.value[0] ?? visibleProjects.value[0] ?? null;
});

const statusCards = computed(() => {
  const rows = visibleProjects.value;
  const stats = sourceFilter.value === 'REAL_NAS' ? statistics.value : null;
  const registered = rows.filter((item) => Number(item.fileCount ?? 0) > 0).length;
  const masterData = rows.filter((item) => Boolean(item.hasMasterData)).length;
  const pending = rows.filter((item) => !Boolean(item.hasMasterData) || !Boolean(item.hasDeliveryStandard)).length;
  const fileCount = stats?.fileCount ?? rows.reduce((sum, item) => sum + Number(item.fileCount ?? 0), 0);
  const modelCount = stats?.modelFileCount ?? rows.reduce((sum, item) => sum + Number(item.modelCount ?? 0), 0);
  const drawingCount = stats?.drawingFileCount ?? 0;
  const totalSize = stats?.totalSizeBytes ?? rows.reduce((sum, item) => sum + Number(item.totalSizeBytes ?? 0), 0);
  return [
    { label: '项目', value: formatCount(stats?.projectCount ?? rows.length), unit: '个' },
    { label: '已登记文件', value: formatCount(fileCount), unit: '份' },
    { label: '模型文件', value: formatCount(modelCount), unit: '份' },
    { label: '图纸文件', value: formatCount(drawingCount), unit: '份' },
    { label: '登记容量', value: formatBytes(totalSize), unit: '目录级' },
    { label: '主数据已建', value: formatCount(masterData), unit: `/${formatCount(rows.length)} 个项目` },
    { label: '待补底座', value: formatCount(pending), unit: '需处理' },
    { label: '有文件项目', value: formatCount(registered), unit: '个项目' }
  ];
});

const statusScopeText = computed(() => {
  if (sourceFilter.value === 'REAL_NAS') {
    return '来自真实 NAS 项目统计接口，不混入 smoke、测试或样例项目。';
  }
  if (sourceFilter.value === 'ALL') {
    return '当前显示全部可访问项目，包含样例、测试和历史项目。';
  }
  return '当前统计按列表筛选结果计算，用于快速判断这一类项目的接入情况。';
});

const actionItems = [
  { key: 'enter', label: '进入真实项目', description: '打开项目工作台，先看资产目录、容量和治理风险。' },
  { key: 'assessment', label: '查看接入评估', description: '查看目录线索和主数据缺口，确认模板只是草案。' },
  { key: 'master-data', label: '完善工程主数据', description: '补齐部位树、节点类型、交付定义和目录模板。' },
  { key: 'delivery', label: '进入文档交付', description: '查看应交项、已挂接文件、审核状态和导出预检查。' }
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
    const [nextProjects, nextStatistics, nextQualityOverview] = await Promise.all([
      fetchAssetProjects(keyword.value.trim() || undefined, assetSource),
      fetchAssetStatistics(undefined, assetSource),
      fetchAssetQualityOverview(undefined, assetSource)
    ]);
    projects.value = nextProjects;
    statistics.value = nextStatistics;
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
  router.push({ name: 'project-work-document-delivery', params: { projectId: project.projectId } });
}

function actionForStep(index: number) {
  const map: Array<typeof actionItems[number]['key']> = ['enter', 'master-data', 'delivery'];
  const key = map[index];
  return actionItems.find((item) => item.key === key) ?? null;
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
      actionLabel: '看交付状态',
      hint: '底座已具备，下一步在文档/图纸交付页查看缺项、审核和预检查。',
      routeName: 'project-work-document-delivery'
    } as const;
  }
  if (status === 'MASTERDATA_INITIALIZED') {
    return {
      index: 3,
      actionLabel: '进入文档交付',
      hint: '主数据已初始化，下一步查看文档/图纸应交项和缺失项。',
      routeName: 'project-work-document-delivery'
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
    ['master-data', '工程主数据'],
    ['standard', '交付标准'],
    ['delivery', '文档/图纸交付'],
    ['review', '审核整改']
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

/* ---- Hero ---- */
.asset-hero {
  background: var(--zy-surface);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  display: grid;
  gap: var(--zy-sp-5);
  min-width: 0;
  padding: var(--zy-sp-6);
  position: relative;
  overflow: hidden;
  box-shadow: var(--zy-shadow-xs);
}

.asset-hero::before {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(37, 99, 235, 0.04) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(37, 99, 235, 0.04) 1px, transparent 1px);
  background-size: 32px 32px;
  pointer-events: none;
  mask-image: linear-gradient(to bottom, black 0%, transparent 70%);
}

.asset-hero > * {
  position: relative;
  z-index: 1;
}

.asset-hero__intro {
  display: grid;
  gap: var(--zy-sp-2);
  max-width: 880px;
}

.asset-hero__eyebrow {
  display: inline-flex;
  align-items: center;
  gap: var(--zy-sp-2);
  flex-wrap: wrap;
}

.asset-hero__eyebrow small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  letter-spacing: 0;
}

.asset-hero__intro h2 {
  margin: 0;
  color: var(--zy-ink);
  font-size: var(--zy-fs-3xl);
  font-weight: var(--zy-fw-semi);
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.asset-hero__intro p {
  margin: 0;
  color: var(--zy-text-soft);
  font-size: var(--zy-fs-sm);
  line-height: 1.7;
  max-width: 720px;
}

.asset-hero__section-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--zy-sp-2) var(--zy-sp-3);
  min-width: 0;
}

.asset-hero__section-head strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-base);
  font-weight: var(--zy-fw-semi);
}

.asset-hero__section-head small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.5;
}

/* ---- Flow / 三步工作链 ---- */
.asset-hero__workflow,
.asset-hero__status,
.asset-hero__risk {
  display: grid;
  gap: var(--zy-sp-3);
}

.asset-flow {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--zy-sp-3);
  margin: 0;
  padding: 0;
  list-style: none;
  position: relative;
}

.asset-flow__step {
  position: relative;
  background: var(--zy-surface-soft);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  padding: var(--zy-sp-4);
  display: grid;
  gap: var(--zy-sp-2);
  align-content: start;
  transition:
    border-color var(--zy-duration-2) var(--zy-ease),
    transform var(--zy-duration-2) var(--zy-ease);
}

.asset-flow__step::after {
  content: "→";
  position: absolute;
  right: -10px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  background: var(--zy-bg);
  border: var(--zy-border-soft);
  border-radius: 999px;
  color: var(--zy-subtle);
  font-size: 11px;
  z-index: 2;
}

.asset-flow__step:last-child::after {
  display: none;
}

.asset-flow__step:hover {
  border-color: rgba(37, 99, 235, 0.28);
}

.asset-flow__head {
  display: flex;
  align-items: center;
  gap: var(--zy-sp-2);
}

.asset-flow__num {
  display: inline-grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: var(--zy-radius-sm);
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
  font-family: var(--zy-font-mono);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0;
  line-height: 1;
}

.asset-flow__head strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-base);
  font-weight: var(--zy-fw-semi);
}

.asset-flow__step p {
  margin: 0;
  color: var(--zy-text-soft);
  font-size: var(--zy-fs-xs);
  line-height: 1.7;
}

.asset-flow__cta {
  appearance: none;
  background: transparent;
  border: none;
  color: var(--zy-blue-700);
  font-family: inherit;
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
  padding: 6px 0 0;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  justify-self: start;
  letter-spacing: 0.02em;
  transition: gap var(--zy-duration-2) var(--zy-ease);
}

.asset-flow__cta:hover:not(:disabled) {
  gap: 8px;
}

.asset-flow__cta:disabled {
  color: var(--zy-subtle);
  cursor: not-allowed;
}

.asset-flow__cta span {
  font-family: var(--zy-font-mono);
}

/* ---- 当前视图统计 ---- */
.asset-status-grid,
.asset-risk-strip {
  display: grid;
  gap: var(--zy-sp-2);
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  min-width: 0;
}

.asset-status-grid article,
.asset-risk-strip article {
  background: var(--zy-surface);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  padding: var(--zy-sp-3) var(--zy-sp-4);
  display: grid;
  gap: 2px;
  min-width: 0;
  position: relative;
  transition: border-color var(--zy-duration-2) var(--zy-ease);
}

.asset-status-grid article:hover,
.asset-risk-strip article:hover {
  border-color: var(--zy-line);
}

.asset-status-grid__label,
.asset-risk-strip__label {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  letter-spacing: 0;
  display: flex;
  align-items: center;
}

.asset-status-grid__value,
.asset-risk-strip strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-3xl);
  font-weight: var(--zy-fw-bold);
  letter-spacing: -0.02em;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
}

.asset-status-grid__unit,
.asset-risk-strip em {
  color: var(--zy-muted);
  font-size: 11px;
  font-style: normal;
  line-height: 1.4;
}

.asset-risk-strip article.is-warning {
  background: var(--zy-amber-50);
  border-color: rgba(245, 158, 11, 0.28);
}

.asset-risk-strip article.is-warning strong {
  color: #b45309;
}

.asset-risk-strip article.is-warning .asset-risk-strip__label {
  color: #92400e;
}

/* ---- 表格内文案 ---- */
.asset-title-cell {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.asset-title-cell strong {
  color: var(--zy-ink);
  font-weight: var(--zy-fw-semi);
  font-family: var(--zy-font-mono);
  font-size: var(--zy-fs-xs);
  letter-spacing: 0;
}

.asset-title-cell span {
  color: var(--zy-text-soft);
  font-size: var(--zy-fs-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-kind-tags,
.asset-foundation-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  min-width: 0;
}

.asset-governance-cell {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.asset-governance-cell small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.asset-governance-trail {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  min-width: 0;
}

.asset-governance-trail span {
  background: var(--zy-bg);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-sm);
  color: var(--zy-muted);
  font-size: 11px;
  font-weight: var(--zy-fw-medium);
  line-height: 1;
  padding: 4px 6px;
}

.asset-governance-trail span.is-done {
  background: var(--zy-green-50);
  border-color: rgba(34, 197, 94, 0.22);
  color: #047857;
}

.asset-governance-trail span.is-current {
  background: var(--zy-blue-50);
  border-color: rgba(59, 130, 246, 0.26);
  color: var(--zy-blue-700);
  font-weight: var(--zy-fw-bold);
}

@media (max-width: 960px) {
  .asset-hero {
    padding: var(--zy-sp-4);
  }

  .asset-search,
  .asset-sort {
    width: 100%;
  }

  .asset-flow {
    grid-template-columns: 1fr;
  }

  .asset-flow__step::after {
    display: none;
  }
}
</style>
