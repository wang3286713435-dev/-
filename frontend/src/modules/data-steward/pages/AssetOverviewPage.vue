<template>
  <section class="mvp-page asset-page">
    <div class="mvp-page__header">
      <div>
        <h1>项目启动台</h1>
        <p>先选项目，再进入文件管理、项目可视化或交付状态</p>
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

    <section ref="heroRef" class="asset-launchpad" aria-label="项目启动台">
      <div class="zy-hero-lightfield" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
      </div>
      <div class="zy-spotlight" aria-hidden="true"></div>
      <ParticleField :count="14" :speed="0.2" :link-distance="120" />
      <div class="asset-launchpad__copy">
        <span class="zy-code-chip">PROJECT LAUNCHPAD</span>
        <h2>选择项目，直接开始工作</h2>
        <p>默认聚焦真实项目。搜索或选择推荐项目后，直接进入文件管理、项目可视化或交付状态。</p>
      </div>

      <article class="asset-launchpad__recommend">
        <span>推荐项目</span>
        <template v-if="recommendedProject">
          <strong>{{ recommendedProject.code }} {{ recommendedProject.name }}</strong>
          <div class="asset-launchpad__tags">
            <el-tag size="small" :type="sourceTagType(recommendedProject.projectSource, recommendedProject.projectCategory)">
              {{ projectSourceText(recommendedProject.projectSource, recommendedProject.projectCategory) }}
            </el-tag>
            <el-tag size="small" :type="onboardingTagType(recommendedProject.onboardingStatus)">
              {{ onboardingStatusText(recommendedProject.onboardingStatus) }}
            </el-tag>
          </div>
          <div class="asset-launchpad__actions">
            <el-button type="primary" @click="openDetail(recommendedProject)">进入项目</el-button>
            <el-button @click="openProjectFiles(recommendedProject)">文件管理</el-button>
            <el-button @click="openProjectVisualization(recommendedProject)">项目可视化</el-button>
          </div>
        </template>
        <template v-else>
          <strong>暂无推荐项目</strong>
          <p>当前筛选下没有可进入项目，可以切换筛选或联系管理员确认权限。</p>
        </template>
      </article>
    </section>

    <section class="asset-entry-lanes" aria-label="项目入口台">
      <article class="asset-entry-card">
        <span class="asset-entry-card__eyebrow">待处理</span>
        <strong>{{ pendingProjects.length ? '需要确认规则' : '暂无明显阻塞' }}</strong>
        <div v-if="pendingProjects.length" class="asset-entry-list">
          <button v-for="row in pendingProjects" :key="`pending-${row.projectId}`" type="button" @click="openGovernanceNext(row)">
            <span>{{ row.name }}</span>
            <em>{{ governanceStage(row).actionLabel }}</em>
          </button>
        </div>
        <p v-else>真实项目暂无主数据或交付标准阻塞，可继续查看交付状态。</p>
      </article>

      <article class="asset-entry-card">
        <span class="asset-entry-card__eyebrow">最近项目</span>
        <strong>{{ recentProjects.length ? '继续上次项目' : '还没有最近项目' }}</strong>
        <div v-if="recentProjects.length" class="asset-entry-list">
          <button v-for="row in recentProjects" :key="`recent-${row.projectId}`" type="button" @click="openDetail(row)">
            <span>{{ row.name }}</span>
            <em>进入工作台</em>
          </button>
        </div>
        <p v-else>进入项目工作台后，这里会记录最近打开的项目。</p>
      </article>

      <article class="asset-entry-card asset-entry-card--summary">
        <span class="asset-entry-card__eyebrow">项目概况</span>
        <strong>{{ formatCount(visibleProjects.length) }} 个可见项目</strong>
        <div class="asset-summary-row">
          <span>已登记文件</span>
          <em>{{ statusCards[1]?.value ?? '0' }} 份</em>
        </div>
        <div class="asset-summary-row">
          <span>待补规则</span>
          <em>{{ statusCards[6]?.value ?? '0' }} 个</em>
        </div>
        <el-button text @click="overviewMoreActive = overviewMoreActive.length ? [] : ['summary']">
          查看统计和风险
        </el-button>
      </article>
    </section>

    <el-collapse v-model="overviewMoreActive" class="asset-overview-more">
      <el-collapse-item name="summary">
        <template #title>
          <span class="asset-overview-more__title">统计和风险摘要</span>
          <small>低频信息已收起，不影响选项目</small>
        </template>
        <div class="asset-status-grid">
          <article v-for="item in statusCards" :key="item.label">
            <span class="asset-status-grid__label">{{ item.label }}</span>
            <strong class="asset-status-grid__value">{{ item.value }}</strong>
            <em class="asset-status-grid__unit">{{ item.unit }}</em>
          </article>
        </div>
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
      </el-collapse-item>
    </el-collapse>

    <section class="asset-table-section" aria-label="项目列表">
      <div class="asset-table-section__header">
        <div>
          <span class="zy-code-chip">PROJECTS</span>
          <strong>项目列表</strong>
          <p>选择项目后可直接进入文件管理、项目可视化或交付状态。</p>
        </div>
        <el-switch
          v-model="detailColumnsVisible"
          active-text="显示详细字段"
          inactive-text="收起详细字段"
        />
      </div>

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
      <el-table-column v-if="detailColumnsVisible" prop="projectStage" label="阶段" width="120" show-overflow-tooltip />
      <el-table-column prop="projectManagerName" label="负责人" width="130" show-overflow-tooltip />
      <el-table-column label="下一步" min-width="260">
        <template #default="{ row }">
          <div class="asset-governance-cell">
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
      <el-table-column v-if="detailColumnsVisible" label="模型数" width="110" align="right">
        <template #default="{ row }">{{ formatCount(row.modelCount) }}</template>
      </el-table-column>
      <el-table-column v-if="detailColumnsVisible" label="主要类型" min-width="160">
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
      <el-table-column v-if="detailColumnsVisible" label="模型容量" width="130" align="right">
        <template #default="{ row }">{{ formatBytes(row.totalSizeBytes) }}</template>
      </el-table-column>
      <el-table-column label="最近扫描/更新" width="170">
        <template #default="{ row }">{{ formatDate(row.lastScanAt || row.lastModelUpdatedAt) }}</template>
      </el-table-column>
      <el-table-column v-if="detailColumnsVisible" label="底座" width="160">
        <template #default="{ row }">
          <div class="asset-foundation-tags">
            <el-tag size="small" :type="row.hasMasterData ? 'success' : 'info'">主数据</el-tag>
            <el-tag size="small" :type="row.hasDeliveryStandard ? 'success' : 'info'">交付标准</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="detailColumnsVisible" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.assetStatus === 'ACTIVE' ? 'success' : 'info'">{{ row.assetStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <div class="asset-row-actions">
            <el-button text :icon="ArrowRight" @click="openDetail(row)">工作台</el-button>
            <el-button text @click="openProjectFiles(row)">文件</el-button>
            <el-button text @click="openProjectVisualization(row)">可视化</el-button>
          </div>
        </template>
      </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowRight, Refresh, Search } from '@element-plus/icons-vue';

import ParticleField from '@/modules/core/components/ParticleField.vue';
import { useSpotlight } from '@/modules/core/composables/useSpotlight';
import {
  fetchAssetProjects,
  fetchAssetQualityOverview,
  fetchAssetStatistics,
  type AssetProject,
  type AssetQualityOverview,
  type AssetStatistics
} from '@/modules/data-steward/api/dataSteward';

const heroRef = ref<HTMLElement | null>(null);
useSpotlight(heroRef);

const router = useRouter();
const loading = ref(false);
const keyword = ref('');
const sourceFilter = ref('REAL_NAS');
const projectSortOrder = ref<'ASC' | 'DESC'>('ASC');
const detailColumnsVisible = ref(false);
const overviewMoreActive = ref<string[]>([]);
const projects = ref<AssetProject[]>([]);
const statistics = ref<AssetStatistics | null>(null);
const qualityOverview = ref<AssetQualityOverview | null>(null);
const recentProjectIds = ref<number[]>(readRecentProjectIds());
const sourceOptions = [
  { label: '真实项目', value: 'REAL_NAS' },
  { label: '未完成接入', value: 'UNFINISHED_ONBOARDING' },
  { label: '样例/模板', value: 'SAMPLE_TEMPLATE' },
  { label: '测试', value: 'TEST_PROJECT' },
  { label: '归档', value: 'ARCHIVED_HISTORY' },
  { label: '全部', value: 'ALL' }
];

const realNasProjects = computed(() => projects.value.filter(isRealNasProject));

const primaryActionProject = computed(() => {
  return visibleProjects.value.find(isRealNasProject) ?? realNasProjects.value[0] ?? visibleProjects.value[0] ?? null;
});

const pendingProjects = computed(() => sortedProjects.value
  .filter((item) => isRealNasProject(item) && (!item.hasMasterData || !item.hasDeliveryStandard || item.onboardingStatus !== 'GOVERNANCE_READY'))
  .slice(0, 3));

const recentProjects = computed(() => recentProjectIds.value
  .map((id) => projects.value.find((item) => item.projectId === id))
  .filter((item): item is AssetProject => Boolean(item))
  .slice(0, 3));

const recommendedProject = computed(() =>
  pendingProjects.value[0] ?? recentProjects.value[0] ?? primaryActionProject.value
);

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
  rememberProject(row.projectId);
  router.push({ name: 'data-steward-asset-detail', params: { projectId: row.projectId } });
}

function openProjectFiles(row: AssetProject) {
  rememberProject(row.projectId);
  router.push({ name: 'data-steward-asset-detail', params: { projectId: row.projectId }, query: { tab: 'files' } });
}

function openProjectVisualization(row: AssetProject) {
  rememberProject(row.projectId);
  router.push({ name: 'data-steward-asset-detail', params: { projectId: row.projectId }, query: { tab: 'dashboard' } });
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

function isRealNasProject(row: AssetProject) {
  return row.projectCategory === 'REAL_NAS_PROJECT' || row.projectSource === 'REAL_NAS';
}

function readRecentProjectIds() {
  try {
    const raw = window.localStorage.getItem('delivery.recentProjectIds');
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed)
      ? parsed.map((item) => Number(item)).filter((item) => Number.isFinite(item)).slice(0, 5)
      : [];
  } catch {
    return [];
  }
}

function rememberProject(projectId: number) {
  const next = [projectId, ...recentProjectIds.value.filter((item) => item !== projectId)].slice(0, 5);
  recentProjectIds.value = next;
  try {
    window.localStorage.setItem('delivery.recentProjectIds', JSON.stringify(next));
  } catch {
    // Browser storage can be unavailable in private mode; recent projects are only a convenience.
  }
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

/* ---- Project launchpad ---- */
.asset-launchpad {
  background: var(--zy-panel-tint);
  -webkit-backdrop-filter: blur(14px) saturate(1.04);
  backdrop-filter: blur(14px) saturate(1.04);
  border: 1px solid color-mix(in srgb, var(--zy-line) 72%, transparent);
  border-radius: var(--zy-radius-lg);
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 0.72fr);
  gap: var(--zy-sp-5);
  align-items: center;
  min-width: 0;
  padding: var(--zy-sp-5);
  position: relative;
  overflow: hidden;
  box-shadow: var(--zy-shadow-soft);
  isolation: isolate;
}

@supports not ((-webkit-backdrop-filter: blur(1px)) or (backdrop-filter: blur(1px))) {
  .asset-launchpad {
    background: var(--zy-surface);
  }
}

.asset-launchpad::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image:
    linear-gradient(to right, rgba(37, 99, 235, 0.04) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(37, 99, 235, 0.04) 1px, transparent 1px);
  background-size: 32px 32px;
  pointer-events: none;
  mask-image: linear-gradient(to bottom, black 0%, transparent 70%);
}

.asset-launchpad__copy,
.asset-launchpad__recommend {
  position: relative;
  z-index: 2;
}

.asset-launchpad__copy {
  display: grid;
  gap: var(--zy-sp-2);
  max-width: 700px;
}

.asset-launchpad__copy h2 {
  margin: 0;
  color: var(--zy-ink);
  font-size: var(--zy-fs-3xl);
  font-weight: var(--zy-fw-semi);
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.asset-launchpad__copy p,
.asset-launchpad__recommend p {
  margin: 0;
  color: var(--zy-text-soft);
  font-size: var(--zy-fs-sm);
  line-height: 1.7;
}

.asset-launchpad__recommend {
  display: grid;
  gap: var(--zy-sp-3);
  min-width: 0;
  padding: var(--zy-sp-4);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: var(--zy-shadow-xs);
}

.asset-launchpad__recommend > span {
  color: var(--zy-blue-700);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.asset-launchpad__recommend strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-xl);
  font-weight: var(--zy-fw-semi);
  line-height: 1.35;
}

.asset-launchpad__tags,
.asset-launchpad__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  min-width: 0;
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

/* ---- 项目入口台 ---- */
.asset-entry-lanes {
  display: grid;
  grid-template-columns: minmax(280px, 1.2fr) repeat(2, minmax(220px, 1fr));
  gap: var(--zy-sp-4);
  min-width: 0;
}

.asset-entry-card {
  display: grid;
  align-content: start;
  gap: var(--zy-sp-3);
  min-width: 0;
  padding: var(--zy-sp-4);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
  position: relative;
  overflow: hidden;
}

.asset-entry-card::before {
  content: "";
  position: absolute;
  left: 0;
  top: var(--zy-sp-4);
  bottom: var(--zy-sp-4);
  width: 3px;
  border-radius: 0 2px 2px 0;
  background: var(--zy-blue-500);
}

.asset-entry-card--primary {
  background:
    linear-gradient(135deg, rgba(239, 246, 255, 0.78), rgba(255, 255, 255, 0.94)),
    var(--zy-surface);
}

.asset-entry-card__eyebrow {
  color: var(--zy-blue-700);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.asset-entry-card strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-lg);
  font-weight: var(--zy-fw-semi);
  line-height: 1.35;
}

.asset-entry-card p {
  margin: 0;
  color: var(--zy-text-soft);
  font-size: var(--zy-fs-sm);
  line-height: 1.7;
}

.asset-entry-card__meta,
.asset-entry-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.asset-summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zy-sp-3);
  min-width: 0;
  padding: 6px 0;
  border-top: var(--zy-border-soft);
}

.asset-summary-row span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.asset-summary-row em {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-style: normal;
  font-weight: var(--zy-fw-semi);
}

.asset-overview-more {
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
  overflow: hidden;
}

.asset-overview-more :deep(.el-collapse),
.asset-overview-more :deep(.el-collapse-item__wrap) {
  border: 0;
}

.asset-overview-more :deep(.el-collapse-item__header) {
  min-height: 48px;
  padding: 0 var(--zy-sp-4);
  border: 0;
}

.asset-overview-more :deep(.el-collapse-item__content) {
  display: grid;
  gap: var(--zy-sp-3);
  padding: 0 var(--zy-sp-4) var(--zy-sp-4);
}

.asset-overview-more__title {
  margin-right: var(--zy-sp-2);
  color: var(--zy-ink);
  font-weight: var(--zy-fw-semi);
}

.asset-overview-more small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-regular);
}

.asset-entry-list {
  display: grid;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.asset-entry-list button {
  appearance: none;
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
  color: inherit;
  cursor: pointer;
  display: grid;
  gap: 3px;
  min-width: 0;
  padding: var(--zy-sp-2) var(--zy-sp-3);
  text-align: left;
  transition:
    border-color var(--zy-duration-2) var(--zy-ease),
    background var(--zy-duration-2) var(--zy-ease);
}

.asset-entry-list button:hover {
  background: var(--zy-blue-50);
  border-color: rgba(37, 99, 235, 0.24);
}

.asset-entry-list span {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-medium);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-entry-list em {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
}

.asset-table-section {
  display: grid;
  gap: var(--zy-sp-3);
  min-width: 0;
}

.asset-table-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zy-sp-4);
  min-width: 0;
  padding: var(--zy-sp-4);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
}

.asset-table-section__header > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.asset-table-section__header strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-lg);
  font-weight: var(--zy-fw-semi);
}

.asset-table-section__header p {
  margin: 0;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.6;
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

.asset-row-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
}

.asset-row-actions :deep(.el-button) {
  margin-left: 0;
}

@media (max-width: 960px) {
  .asset-launchpad {
    grid-template-columns: 1fr;
    padding: var(--zy-sp-4);
  }

  .asset-entry-lanes {
    grid-template-columns: 1fr;
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

  .asset-table-section__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
