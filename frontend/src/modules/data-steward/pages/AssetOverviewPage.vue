<template>
  <section class="mvp-page launchpad-page">
    <header class="launchpad-page__header">
      <div>
        <h1>项目启动台</h1>
        <p>选择项目，查看工作状态，快速进入项目工作台</p>
      </div>
      <div class="launchpad-page__tools">
        <el-input
          v-model="keyword"
          class="launchpad-search"
          clearable
          placeholder="搜索项目名称 / 编码 / 负责人"
          :prefix-icon="Search"
          @keyup.enter="loadPage"
          @clear="loadPage"
        />
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </header>

    <section class="launchpad-layout" aria-label="项目启动台">
      <main class="launchpad-main">
        <section class="launchpad-hero-grid" aria-label="推荐项目和待处理事项">
          <article class="launchpad-panel launchpad-recommend">
            <div class="launchpad-section-title">
              <strong>为你推荐</strong>
              <span>{{ recommendedProject ? '优先处理当前最相关项目' : '等待项目数据' }}</span>
            </div>
            <div v-if="recommendedProject" class="launchpad-recommend__card">
              <div
                class="launchpad-recommend__cover"
                :style="{ backgroundImage: `url(${projectCoverImage})` }"
              >
                <span>{{ recommendedBadge }}</span>
                <strong>{{ recommendedProject.name }}</strong>
              </div>
              <dl class="launchpad-recommend__meta">
                <div>
                  <dt>项目编码</dt>
                  <dd>{{ recommendedProject.code }}</dd>
                </div>
                <div>
                  <dt>你的角色</dt>
                  <dd>{{ currentRoleLabel }}</dd>
                </div>
                <div>
                  <dt>负责人</dt>
                  <dd>{{ recommendedProject.projectManagerName || '-' }}</dd>
                </div>
                <div>
                  <dt>最近访问</dt>
                  <dd>{{ projectRecentText(recommendedProject) }}</dd>
                </div>
              </dl>
              <div class="launchpad-recommend__actions">
                <el-button type="primary" :icon="ArrowRight" @click="openDetail(recommendedProject)">
                  进入项目工作台
                </el-button>
                <el-button @click="openProjectFiles(recommendedProject)">文件管理</el-button>
              </div>
            </div>
            <div v-else class="launchpad-empty">
              <strong>暂无推荐项目</strong>
              <p>当前筛选下没有可进入项目，可以切换筛选或联系管理员确认权限。</p>
            </div>
          </article>

          <article class="launchpad-panel launchpad-todo">
            <div class="launchpad-section-title">
              <strong>待处理事项</strong>
              <span>按当前项目数据自动汇总</span>
            </div>
            <div class="launchpad-todo__grid">
              <button
                v-for="item in todoCards"
                :key="item.key"
                class="launchpad-todo-card"
                :class="`is-${item.tone}`"
                type="button"
                @click="handleTodoClick(item)"
              >
                <span class="launchpad-todo-card__icon" aria-hidden="true">{{ item.initial }}</span>
                <span>{{ item.label }}</span>
                <strong>{{ formatCount(item.value) }}</strong>
                <em>{{ item.action }} <span>›</span></em>
              </button>
            </div>
          </article>
        </section>

        <section class="launchpad-filters" aria-label="项目筛选">
          <label>
            <span>项目状态</span>
            <el-select v-model="onboardingFilter" placeholder="全部">
              <el-option label="全部" value="ALL" />
              <el-option label="治理就绪" value="GOVERNANCE_READY" />
              <el-option label="主数据已初始化" value="MASTERDATA_INITIALIZED" />
              <el-option label="资产已登记" value="ASSETS_REGISTERED" />
              <el-option label="路径已映射" value="PATH_MAPPED" />
              <el-option label="待接入" value="NOT_ONBOARDED" />
            </el-select>
          </label>
          <label>
            <span>项目类型</span>
            <el-select v-model="sourceFilter" placeholder="全部">
              <el-option
                v-for="item in sourceOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </label>
          <label>
            <span>负责人</span>
            <el-select v-model="managerFilter" placeholder="全部" filterable>
              <el-option label="全部" value="ALL" />
              <el-option
                v-for="item in managerOptions"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </label>
          <label class="launchpad-filters__date">
            <span>最近更新</span>
            <el-date-picker
              v-model="updatedRange"
              type="daterange"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              unlink-panels
            />
          </label>
          <el-button @click="resetFilters">重置</el-button>
        </section>

        <section class="launchpad-panel launchpad-projects" aria-label="全部项目">
          <div class="launchpad-projects__header">
            <div>
              <strong>全部项目 <span>({{ formatCount(sortedProjects.length) }})</span></strong>
              <p>双击项目行或点击“进入”，即可打开真实项目工作台。</p>
            </div>
            <el-button type="primary" @click="showCreateProjectNotice">新建项目</el-button>
          </div>

          <el-table
            v-loading="loading"
            :data="pagedProjects"
            class="master-table launchpad-table"
            empty-text="暂无资产项目"
            @row-dblclick="openDetail"
          >
            <el-table-column label="项目名称" min-width="260">
              <template #default="{ row }">
                <div class="launchpad-project-title">
                  <strong>{{ row.name }}</strong>
                  <span>{{ projectSourceText(row.projectSource, row.projectCategory) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="项目编码" width="150">
              <template #default="{ row }">
                <span class="launchpad-code">{{ row.code }}</span>
              </template>
            </el-table-column>
            <el-table-column label="项目类型" width="120">
              <template #default="{ row }">{{ row.industryType || projectSourceText(row.projectSource, row.projectCategory) }}</template>
            </el-table-column>
            <el-table-column label="当前阶段" width="150">
              <template #default="{ row }">
                <span class="launchpad-phase">{{ governanceStage(row).index }} {{ governanceStage(row).shortLabel }}</span>
              </template>
            </el-table-column>
            <el-table-column label="整体进度" width="160">
              <template #default="{ row }">
                <div class="launchpad-progress">
                  <span :style="{ width: `${projectProgress(row)}%` }"></span>
                </div>
                <em class="launchpad-progress-text">{{ projectProgress(row) }}%</em>
              </template>
            </el-table-column>
            <el-table-column label="风险等级" width="110">
              <template #default="{ row }">
                <span class="launchpad-risk-pill" :class="`is-${projectRisk(row).level}`">
                  {{ projectRisk(row).label }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="负责人" width="110" show-overflow-tooltip>
              <template #default="{ row }">{{ row.projectManagerName || '-' }}</template>
            </el-table-column>
            <el-table-column label="最近访问" width="140">
              <template #default="{ row }">{{ projectRecentText(row) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <div class="launchpad-row-actions">
                  <el-button text @click="openDetail(row)">进入</el-button>
                  <el-button text :icon="MoreFilled" aria-label="更多操作" @click="openProjectFiles(row)" />
                </div>
              </template>
            </el-table-column>
          </el-table>

          <div class="launchpad-pagination">
            <span>共 {{ formatCount(sortedProjects.length) }} 条</span>
            <el-pagination
              v-model:current-page="pageNo"
              v-model:page-size="pageSize"
              background
              layout="prev, pager, next, sizes, jumper"
              :page-sizes="[8, 10, 20, 50]"
              :total="sortedProjects.length"
            />
          </div>
        </section>
      </main>

      <aside class="launchpad-sidebar" aria-label="项目状态侧栏">
        <section class="launchpad-panel launchpad-status">
          <div class="launchpad-section-title">
            <strong>项目总体状态</strong>
            <span>按当前项目列表统计</span>
          </div>
          <div class="launchpad-donut-wrap">
            <div class="launchpad-donut" :style="{ background: donutGradient }">
              <div class="launchpad-donut__content">
                <span>{{ formatCount(statusTotal) }}</span>
                <em>总项目数</em>
              </div>
            </div>
            <ul class="launchpad-status__legend">
              <li v-for="item in statusLegend" :key="item.key">
                <span :style="{ background: item.color }"></span>
                <strong>{{ item.label }}</strong>
                <em>{{ formatCount(item.value) }} ({{ item.percent }}%)</em>
              </li>
            </ul>
          </div>
        </section>

        <section class="launchpad-panel launchpad-risk-list">
          <div class="launchpad-section-title launchpad-section-title--row">
            <strong>风险项目</strong>
            <el-button text @click="router.push({ name: 'data-steward-quality' })">查看全部</el-button>
          </div>
          <div class="launchpad-side-list">
            <button
              v-for="item in riskProjects"
              :key="`risk-${item.projectId}`"
              type="button"
              @click="openDetailById(item.projectId)"
            >
              <span class="launchpad-side-list__alert" aria-hidden="true">!</span>
              <span>
                <strong>{{ item.projectName }}</strong>
                <em>{{ item.reason }}</em>
              </span>
              <b :class="`is-${item.level}`">{{ item.label }}</b>
            </button>
            <p v-if="!riskProjects.length" class="launchpad-side-empty">当前没有高风险项目。</p>
          </div>
        </section>

        <section class="launchpad-panel launchpad-notices">
          <div class="launchpad-section-title launchpad-section-title--row">
            <strong>公告通知</strong>
            <el-button text @click="router.push({ name: 'data-steward-quality' })">查看全部</el-button>
          </div>
          <ul>
            <li v-for="item in noticeItems" :key="item.key">
              <span></span>
              <strong>{{ item.title }}</strong>
              <time>{{ item.time }}</time>
            </li>
          </ul>
        </section>
      </aside>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowRight, MoreFilled, Refresh, Search } from '@element-plus/icons-vue';

import projectCoverImage from '@/assets/ux4/project-cover-reference.png';
import {
  fetchCatalogProjects,
  fetchAssetProjects,
  fetchAssetQualityOverview,
  fetchAssetStatistics,
  type AssetProject,
  type AssetQualityOverview,
  type AssetQualityProjectRisk,
  type AssetStatistics,
  type CatalogProject
} from '@/modules/data-steward/api/dataSteward';

type TodoCard = {
  key: string;
  label: string;
  initial: string;
  value: number;
  tone: 'red' | 'amber' | 'blue' | 'green';
  action: string;
  project?: AssetProject | null;
  routeName?: string;
};

type LaunchpadRisk = {
  projectId: number;
  projectName: string;
  reason: string;
  level: 'high' | 'medium' | 'low';
  label: string;
};

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const keyword = ref('');
const sourceFilter = ref('ALL');
const onboardingFilter = ref('ALL');
const managerFilter = ref('ALL');
const projectSortOrder = ref<'ASC' | 'DESC'>('ASC');
const updatedRange = ref<[string, string] | null>(null);
const projects = ref<AssetProject[]>([]);
const statistics = ref<AssetStatistics | null>(null);
const qualityOverview = ref<AssetQualityOverview | null>(null);
const recentProjectIds = ref<number[]>(readRecentProjectIds());
const pageNo = ref(1);
const pageSize = ref(10);

const sourceOptions = [
  { label: '全部', value: 'ALL' },
  { label: '真实项目', value: 'REAL_NAS' },
  { label: '样例/模板', value: 'SAMPLE_TEMPLATE' },
  { label: '测试', value: 'TEST_PROJECT' },
  { label: '归档', value: 'ARCHIVED_HISTORY' }
];

const baseProjects = computed(() => {
  if (sourceFilter.value === 'ALL') {
    return projects.value;
  }
  if (sourceFilter.value === 'REAL_NAS') {
    return projects.value.filter((item) => item.projectCategory === 'REAL_NAS_PROJECT' || item.projectSource === 'REAL_NAS');
  }
  return projects.value.filter((item) => item.projectCategory === sourceFilter.value || item.projectSource === sourceFilter.value);
});

const realNasProjects = computed(() => projects.value.filter(isRealNasProject));

const visibleProjects = computed(() => baseProjects.value.filter((item) => {
  if (onboardingFilter.value !== 'ALL' && (item.onboardingStatus || 'NOT_ONBOARDED') !== onboardingFilter.value) {
    return false;
  }
  if (managerFilter.value !== 'ALL' && (item.projectManagerName || '-') !== managerFilter.value) {
    return false;
  }
  if (updatedRange.value) {
    const timestamp = projectTimestamp(item);
    if (!timestamp) return false;
    const [start, end] = updatedRange.value;
    const startTime = new Date(`${start}T00:00:00`).getTime();
    const endTime = new Date(`${end}T23:59:59`).getTime();
    if (timestamp < startTime || timestamp > endTime) return false;
  }
  return true;
}));

const sortedProjects = computed(() => {
  return [...visibleProjects.value].sort((left, right) => {
    return projectSortOrder.value === 'ASC'
      ? left.projectId - right.projectId
      : right.projectId - left.projectId;
  });
});

const pagedProjects = computed(() => {
  const start = (pageNo.value - 1) * pageSize.value;
  return sortedProjects.value.slice(start, start + pageSize.value);
});

const managerOptions = computed(() => {
  const names = projects.value
    .map((item) => item.projectManagerName || '-')
    .filter((item) => item !== '-');
  return [...new Set(names)].sort((left, right) => left.localeCompare(right, 'zh-CN'));
});

const primaryActionProject = computed(() => {
  return visibleProjects.value.find(isRealNasProject) ?? realNasProjects.value[0] ?? visibleProjects.value[0] ?? projects.value[0] ?? null;
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

const recommendedBadge = computed(() => {
  if (!recommendedProject.value) return '';
  if (recentProjectIds.value.includes(recommendedProject.value.projectId)) return '继续上次';
  if (!recommendedProject.value.hasMasterData || !recommendedProject.value.hasDeliveryStandard) return '待处理';
  return '推荐';
});

const currentRoleLabel = computed(() => {
  return recommendedProject.value ? '项目管理员' : '-';
});

const todoCards = computed<TodoCard[]>(() => {
  const rows = visibleProjects.value.length ? visibleProjects.value : projects.value;
  const masterPendingProject = rows.find((item) => !item.hasMasterData);
  const deliveryPendingProject = rows.find((item) => !item.hasDeliveryStandard) ?? masterPendingProject;
  const objectProject = riskProjectForTodo.value ?? deliveryPendingProject;
  const bimProject = rows.find((item) => Number(item.modelCount ?? 0) > 0) ?? recommendedProject.value;
  return [
    {
      key: 'master-data',
      label: '主数据待确认',
      initial: 'M',
      value: rows.filter((item) => !item.hasMasterData).length,
      tone: 'red',
      action: '去处理',
      project: masterPendingProject,
      routeName: 'project-master-data-initialization'
    },
    {
      key: 'delivery',
      label: '交付缺失文件',
      initial: 'D',
      value: rows.filter((item) => !item.hasDeliveryStandard).length + Number(qualityOverview.value?.pendingReviewCount ?? 0),
      tone: 'amber',
      action: '去查看',
      project: deliveryPendingProject,
      routeName: 'project-work-document-delivery'
    },
    {
      key: 'object-storage',
      label: '对象存储风险',
      initial: 'O',
      value: Number(qualityOverview.value?.missingStoragePathCount ?? 0) + Number(qualityOverview.value?.zeroSizeFileCount ?? 0),
      tone: 'blue',
      action: '去处置',
      project: objectProject,
      routeName: objectProject ? 'project-data-steward-file-service' : 'data-steward-quality'
    },
    {
      key: 'bim',
      label: 'BIM 模型待轻量化',
      initial: 'B',
      value: rows.filter((item) => Number(item.modelCount ?? 0) > 0 && item.onboardingStatus !== 'GOVERNANCE_READY').length,
      tone: 'green',
      action: '去查看',
      project: bimProject,
      routeName: 'data-steward-asset-detail'
    }
  ];
});

const riskProjectForTodo = computed(() => {
  const firstRisk = qualityOverview.value?.topRiskProjects?.[0];
  if (!firstRisk) return null;
  return projects.value.find((item) => item.projectId === firstRisk.projectId) ?? null;
});

const statusTotal = computed(() => visibleProjects.value.length);

const statusLegend = computed(() => {
  const rows = visibleProjects.value;
  const completed = rows.filter((item) => item.onboardingStatus === 'GOVERNANCE_READY').length;
  const running = rows.filter((item) => ['MASTERDATA_INITIALIZED', 'ASSETS_REGISTERED'].includes(item.onboardingStatus || '')).length;
  const preparing = rows.filter((item) => !item.onboardingStatus || ['PATH_MAPPED', 'NOT_ONBOARDED'].includes(item.onboardingStatus)).length;
  const paused = rows.filter((item) => item.projectCategory === 'ARCHIVED_HISTORY' || item.assetStatus !== 'ACTIVE').length;
  return [
    { key: 'running', label: '进行中', value: running, color: 'oklch(0.52 0.19 257)' },
    { key: 'preparing', label: '准备中', value: preparing, color: 'oklch(0.63 0.16 162)' },
    { key: 'completed', label: '已完成', value: completed, color: 'oklch(0.64 0.16 273)' },
    { key: 'paused', label: '暂停中', value: paused, color: 'oklch(0.64 0.19 25)' }
  ].map((item) => ({
    ...item,
    percent: statusTotal.value ? Math.round((item.value / statusTotal.value) * 100) : 0
  }));
});

const donutGradient = computed(() => {
  if (!statusTotal.value) {
    return 'conic-gradient(oklch(0.9 0.015 255) 0deg 360deg)';
  }
  let start = 0;
  const segments = statusLegend.value.map((item) => {
    const end = start + (item.value / statusTotal.value) * 360;
    const segment = `${item.color} ${start}deg ${end}deg`;
    start = end;
    return segment;
  });
  return `conic-gradient(${segments.join(', ')})`;
});

const riskProjects = computed<LaunchpadRisk[]>(() => {
  const risks = qualityOverview.value?.topRiskProjects ?? [];
  const fromQuality = risks.slice(0, 3).map((item) => riskFromQualityProject(item));
  if (fromQuality.length) return fromQuality;
  return visibleProjects.value
    .filter((item) => !item.hasMasterData || !item.hasDeliveryStandard)
    .slice(0, 3)
    .map((item) => ({
      projectId: item.projectId,
      projectName: item.name,
      reason: !item.hasMasterData ? '主数据未确认' : '交付标准待配置',
      level: !item.hasMasterData ? 'high' : 'medium',
      label: !item.hasMasterData ? '高风险' : '中风险'
    }));
});

const noticeItems = computed(() => {
  const events = qualityOverview.value?.recentEvents ?? [];
  const rows = events.slice(0, 3).map((item) => ({
    key: `event-${item.id}`,
    title: noticeTitle(item.actionCode, item.summary),
    time: formatDate(item.createdAt)
  }));
  if (rows.length) return rows;
  return [
    {
      key: 'empty-notice',
      title: '当前暂无新的系统公告',
      time: '-'
    }
  ];
});

loadPage();

watch(
  () => route.query.selectProject,
  (value) => {
    if (value !== '1') return;
    ElMessage.info('请先选择项目');
    const { selectProject: _selectProject, ...nextQuery } = route.query;
    void router.replace({ name: 'data-steward-assets', query: nextQuery });
  },
  { immediate: true }
);

watch([sourceFilter, onboardingFilter, managerFilter, updatedRange, pageSize], () => {
  pageNo.value = 1;
});

async function loadPage() {
  loading.value = true;
  try {
    const nextProjects = await loadLaunchpadProjects();
    projects.value = nextProjects;
    pageNo.value = 1;

    const [statisticsResult, qualityResult] = await Promise.allSettled([
      fetchAssetStatistics(undefined, undefined),
      fetchAssetQualityOverview(undefined, undefined)
    ]);

    statistics.value = statisticsResult.status === 'fulfilled' ? statisticsResult.value : null;
    qualityOverview.value = qualityResult.status === 'fulfilled' ? qualityResult.value : null;

    if (statisticsResult.status === 'rejected' || qualityResult.status === 'rejected') {
      ElMessage.warning('项目列表已加载，部分统计信息稍后刷新。');
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资产总览加载失败');
    projects.value = [];
    statistics.value = null;
    qualityOverview.value = null;
  } finally {
    loading.value = false;
  }
}

async function loadLaunchpadProjects() {
  const rows = await fetchAssetProjects(keyword.value.trim() || undefined, undefined);
  if (rows.length) {
    return rows;
  }
  const catalogRows = await fetchCatalogProjects();
  return catalogRows.filter(matchesCatalogKeyword).map(mapCatalogProject);
}

function matchesCatalogKeyword(row: CatalogProject) {
  const query = keyword.value.trim().toLowerCase();
  if (!query) return true;
  return [row.projectCode, row.projectName]
    .some((value) => (value || '').toLowerCase().includes(query));
}

function mapCatalogProject(row: CatalogProject): AssetProject {
  const realNas = row.assetSource === 'NAS_REAL' || row.assetSource === 'REAL_NAS';
  return {
    projectId: row.projectId,
    code: row.projectCode,
    name: row.projectName,
    industryType: row.projectStage || null,
    projectStage: row.projectStage,
    projectManagerName: null,
    assetStatus: 'ACTIVE',
    assetSource: row.assetSource,
    modelCount: 0,
    totalSizeBytes: row.totalSizeBytes,
    lastModelUpdatedAt: null,
    projectSource: realNas ? 'REAL_NAS' : row.assetSource,
    projectCategory: realNas ? 'REAL_NAS_PROJECT' : null,
    onboardingStatus: row.fileCount > 0 ? 'ASSETS_REGISTERED' : 'PATH_MAPPED',
    fileCount: row.fileCount,
    dominantFileKinds: [],
    lastScanAt: null,
    hasMasterData: false,
    hasDeliveryStandard: false,
    governanceReady: false
  };
}

function resetFilters() {
  sourceFilter.value = 'ALL';
  onboardingFilter.value = 'ALL';
  managerFilter.value = 'ALL';
  updatedRange.value = null;
  projectSortOrder.value = 'ASC';
  pageNo.value = 1;
}

function showCreateProjectNotice() {
  ElMessage.info('当前试运行阶段由管理员接入真实项目；本按钮暂不创建项目。');
}

function openDetail(row: AssetProject) {
  rememberProject(row.projectId);
  router.push({ name: 'data-steward-asset-detail', params: { projectId: row.projectId } });
}

function openDetailById(projectId: number) {
  const row = projects.value.find((item) => item.projectId === projectId);
  if (row) {
    openDetail(row);
  }
}

function openProjectFiles(row: AssetProject) {
  rememberProject(row.projectId);
  router.push({ name: 'data-steward-asset-detail', params: { projectId: row.projectId }, query: { tab: 'files' } });
}

function handleTodoClick(item: TodoCard) {
  const row = item.project ?? recommendedProject.value;
  if (!row) {
    if (item.routeName) {
      router.push({ name: item.routeName });
    }
    return;
  }
  rememberProject(row.projectId);
  if (item.key === 'bim') {
    router.push({ name: 'data-steward-asset-detail', params: { projectId: row.projectId }, query: { tab: 'bim' } });
    return;
  }
  router.push({ name: item.routeName || 'data-steward-asset-detail', params: { projectId: row.projectId } });
}

function governanceStage(row: AssetProject) {
  const status = row.onboardingStatus;
  if (status === 'GOVERNANCE_READY') {
    return {
      index: 4,
      shortLabel: '交付闭环',
      actionLabel: '看交付状态',
      hint: '底座已具备，下一步在文档/图纸交付页查看缺项、审核和预检查。',
      routeName: 'project-work-document-delivery'
    } as const;
  }
  if (status === 'MASTERDATA_INITIALIZED') {
    return {
      index: 3,
      shortLabel: '交付闭环',
      actionLabel: '进入文档交付',
      hint: '主数据已初始化，下一步查看文档/图纸应交项和缺失项。',
      routeName: 'project-work-document-delivery'
    } as const;
  }
  if (status === 'ASSETS_REGISTERED') {
    return {
      index: 2,
      shortLabel: '工程主数据',
      actionLabel: '查看接入评估',
      hint: '资产目录已登记，下一步查看接入评估并确认主数据草案。',
      routeName: 'project-master-data-initialization'
    } as const;
  }
  if (status === 'PATH_MAPPED') {
    return {
      index: 1,
      shortLabel: '资产接入',
      actionLabel: '查看接入评估',
      hint: '路径已映射但资产或主数据仍需补齐，先看接入评估。',
      routeName: 'project-master-data-initialization'
    } as const;
  }
  return {
    index: 1,
    shortLabel: '资产接入',
    actionLabel: '进入项目',
    hint: '先进入项目工作台，确认资产目录和接入前置条件。',
    routeName: 'data-steward-asset-detail'
  } as const;
}

function projectProgress(row: AssetProject) {
  const status = row.onboardingStatus;
  if (status === 'GOVERNANCE_READY') return 86;
  if (status === 'MASTERDATA_INITIALIZED') return 62;
  if (status === 'ASSETS_REGISTERED') return 42;
  if (status === 'PATH_MAPPED') return 26;
  return Number(row.fileCount ?? 0) > 0 ? 18 : 8;
}

function projectRisk(row: AssetProject) {
  const risk = qualityOverview.value?.topRiskProjects?.find((item) => item.projectId === row.projectId);
  const count = risk?.totalRiskCount ?? 0;
  if (!row.hasMasterData || count >= 10) return { level: 'high', label: '高' };
  if (!row.hasDeliveryStandard || count > 0) return { level: 'medium', label: '中' };
  return { level: 'low', label: '低' };
}

function riskFromQualityProject(item: AssetQualityProjectRisk): LaunchpadRisk {
  const level = item.totalRiskCount >= 10 ? 'high' : item.totalRiskCount > 0 ? 'medium' : 'low';
  const label = level === 'high' ? '高风险' : level === 'medium' ? '中风险' : '低风险';
  return {
    projectId: item.projectId,
    projectName: item.projectName,
    reason: riskReason(item),
    level,
    label
  };
}

function riskReason(item: AssetQualityProjectRisk) {
  if (item.missingStoragePathCount > 0) return `对象存储/路径风险 ${formatCount(item.missingStoragePathCount)} 个文件`;
  if (item.pendingReviewCount > 0) return `待审核 ${formatCount(item.pendingReviewCount)} 个文件`;
  if (item.missingDisciplineCount > 0) return `专业待完善 ${formatCount(item.missingDisciplineCount)} 个文件`;
  if (item.missingChecksumCount > 0) return `checksum 待补 ${formatCount(item.missingChecksumCount)} 个文件`;
  return `治理项 ${formatCount(item.totalRiskCount)} 个`;
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

function projectTimestamp(row: AssetProject) {
  const value = row.lastScanAt || row.lastModelUpdatedAt;
  return value ? new Date(value).getTime() : 0;
}

function projectRecentText(row: AssetProject) {
  const timestamp = projectTimestamp(row);
  if (!timestamp) return '-';
  const now = Date.now();
  const diffDays = Math.floor((now - timestamp) / 86_400_000);
  if (diffDays <= 0) {
    return formatDate(row.lastScanAt || row.lastModelUpdatedAt).replace(/^\d{4}\/\d{2}\/\d{2}\s*/, '今天 ');
  }
  if (diffDays === 1) return '昨天';
  if (diffDays < 7) return `${diffDays} 天前`;
  return formatDate(row.lastScanAt || row.lastModelUpdatedAt);
}

function projectSourceText(value?: string | null, category?: string | null) {
  if (category === 'TEST_PROJECT') return '测试';
  if (category === 'SAMPLE_TEMPLATE') return '样例/模板';
  if (category === 'ARCHIVED_HISTORY') return '归档';
  if (category === 'REAL_NAS_PROJECT') return '真实项目';
  const labels: Record<string, string> = {
    REAL_NAS: '真实项目',
    SAMPLE_TEMPLATE: '样例/模板',
    TEST: '测试',
    ARCHIVED_HISTORY: '归档',
    MANUAL: '手工/API'
  };
  return labels[value || ''] ?? '未分类';
}

function noticeTitle(actionCode: string, summary: string | null) {
  if (summary) return summary;
  const labels: Record<string, string> = {
    FILE_ACCESS_PREVIEW_ALLOWED: '文件预览访问记录',
    FILE_ACCESS_DOWNLOAD_ALLOWED: '文件下载访问记录',
    ASSET_REGISTERED: '资产登记记录',
    EMPLOYEE_PROJECT_ROLES_UPDATED: '员工项目权限更新',
    NAS_FILE_OBJECTIFIED: '对象存储副本更新'
  };
  return labels[actionCode] ?? actionCode;
}
</script>

<style scoped>
.launchpad-page {
  gap: 14px;
}

.launchpad-page__header {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(360px, 520px);
  gap: 24px;
  align-items: center;
}

.launchpad-page__header h1 {
  margin: 0;
  color: var(--ux4-text-strong);
  font-size: 24px;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: 0;
}

.launchpad-page__header p {
  margin: 6px 0 0;
  color: var(--ux4-text-muted);
  font-size: 14px;
  line-height: 1.6;
}

.launchpad-page__tools {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.launchpad-search :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 10px;
}

.launchpad-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 14px;
  min-width: 0;
}

.launchpad-main,
.launchpad-sidebar {
  display: grid;
  align-content: start;
  gap: 14px;
  min-width: 0;
}

.launchpad-hero-grid {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(360px, 1.05fr);
  gap: 14px;
  min-width: 0;
}

.launchpad-panel {
  border: 1px solid var(--ux4-border-soft);
  border-radius: 12px;
  background: var(--ux4-surface);
  box-shadow: var(--ux4-shadow-card);
  min-width: 0;
}

.launchpad-section-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px 10px;
}

.launchpad-section-title strong {
  color: var(--ux4-text-strong);
  font-size: 18px;
  line-height: 1.35;
  font-weight: 700;
}

.launchpad-section-title span {
  color: var(--ux4-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.launchpad-section-title--row {
  align-items: center;
}

.launchpad-recommend {
  padding-bottom: 14px;
}

.launchpad-recommend__card {
  margin: 0 16px;
  border: 1px solid var(--ux4-border-soft);
  border-radius: 10px;
  overflow: hidden;
  background: oklch(0.995 0.004 255);
}

.launchpad-recommend__cover {
  min-height: 128px;
  display: grid;
  align-content: end;
  gap: 14px;
  padding: 18px;
  background-size: cover;
  background-position: center;
  position: relative;
  isolation: isolate;
}

.launchpad-recommend__cover::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -1;
  background:
    linear-gradient(90deg, rgba(20, 48, 112, 0.78), rgba(20, 48, 112, 0.08)),
    linear-gradient(180deg, rgba(16, 38, 84, 0.1), rgba(16, 38, 84, 0.34));
}

.launchpad-recommend__cover span {
  justify-self: end;
  border-radius: 999px;
  background: oklch(0.52 0.19 257);
  color: oklch(0.99 0.004 255);
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  padding: 8px 10px;
}

.launchpad-recommend__cover strong {
  max-width: 22em;
  color: oklch(0.99 0.004 255);
  font-size: 20px;
  line-height: 1.35;
  font-weight: 700;
}

.launchpad-recommend__meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
  padding: 14px 16px 4px;
}

.launchpad-recommend__meta div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.launchpad-recommend__meta dt {
  color: var(--ux4-text-muted);
  font-size: 12px;
}

.launchpad-recommend__meta dd {
  margin: 0;
  color: var(--ux4-text-strong);
  font-size: 13px;
  font-weight: 650;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.launchpad-recommend__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 16px 14px;
}

.launchpad-empty,
.launchpad-side-empty {
  display: grid;
  gap: 6px;
  margin: 0 16px 16px;
  padding: 18px;
  border: 1px dashed var(--ux4-border);
  border-radius: 10px;
  color: var(--ux4-text-muted);
  font-size: 13px;
}

.launchpad-empty strong {
  color: var(--ux4-text-strong);
}

.launchpad-empty p {
  margin: 0;
  line-height: 1.6;
}

.launchpad-todo__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  padding: 22px 16px 16px;
}

.launchpad-todo-card {
  appearance: none;
  min-height: 174px;
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 11px;
  border: 1px solid var(--ux4-border-soft);
  border-radius: 10px;
  background: oklch(0.998 0.004 255);
  color: var(--ux4-text);
  cursor: pointer;
  font-family: inherit;
  text-align: center;
  transition:
    transform var(--ux4-motion-base) var(--ux4-ease),
    border-color var(--ux4-motion-base) var(--ux4-ease),
    box-shadow var(--ux4-motion-base) var(--ux4-ease);
}

.launchpad-todo-card:hover {
  transform: translateY(-2px);
  border-color: var(--ux4-blue-border);
  box-shadow: var(--ux4-shadow-raised);
}

.launchpad-todo-card:focus-visible {
  outline: 3px solid var(--ux4-focus);
  outline-offset: 2px;
}

.launchpad-todo-card__icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  font-weight: 800;
}

.launchpad-todo-card.is-red .launchpad-todo-card__icon {
  color: oklch(0.62 0.19 25);
  background: var(--ux4-red-soft);
}

.launchpad-todo-card.is-amber .launchpad-todo-card__icon {
  color: oklch(0.62 0.15 75);
  background: var(--ux4-amber-soft);
}

.launchpad-todo-card.is-blue .launchpad-todo-card__icon {
  color: var(--ux4-blue);
  background: var(--ux4-blue-soft);
}

.launchpad-todo-card.is-green .launchpad-todo-card__icon {
  color: oklch(0.56 0.15 155);
  background: var(--ux4-green-soft);
}

.launchpad-todo-card > span:not(.launchpad-todo-card__icon) {
  color: var(--ux4-text);
  font-size: 13px;
  font-weight: 650;
}

.launchpad-todo-card strong {
  color: oklch(0.18 0.055 260);
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.launchpad-todo-card em {
  color: var(--ux4-blue);
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
}

.launchpad-filters {
  display: grid;
  grid-template-columns: 150px 170px 170px minmax(280px, 1fr) auto;
  gap: 14px;
  align-items: end;
  padding: 14px 16px;
  border: 1px solid var(--ux4-border-soft);
  border-radius: 12px;
  background: var(--ux4-surface);
  box-shadow: var(--ux4-shadow-card);
}

.launchpad-filters label {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.launchpad-filters label > span {
  color: var(--ux4-text-muted);
  font-size: 12px;
  font-weight: 650;
}

.launchpad-filters :deep(.el-select),
.launchpad-filters :deep(.el-date-editor) {
  width: 100%;
}

.launchpad-projects {
  overflow: hidden;
}

.launchpad-projects__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--ux4-border-soft);
}

.launchpad-projects__header strong {
  color: var(--ux4-text-strong);
  font-size: 16px;
}

.launchpad-projects__header strong span {
  color: var(--ux4-text-muted);
  font-weight: 600;
}

.launchpad-projects__header p {
  margin: 4px 0 0;
  color: var(--ux4-text-muted);
  font-size: 12px;
}

.launchpad-table {
  --el-table-header-bg-color: oklch(0.985 0.008 255);
  --el-table-row-hover-bg-color: oklch(0.972 0.018 255);
  border: none;
  box-shadow: none;
}

.launchpad-table :deep(.el-table__row td:first-child::before) {
  display: none;
}

.launchpad-project-title {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.launchpad-project-title strong {
  color: var(--ux4-text-strong);
  font-size: 13px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.launchpad-project-title span,
.launchpad-code {
  color: var(--ux4-text);
  font-size: 12px;
}

.launchpad-code {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.launchpad-phase {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  border-radius: 7px;
  background: var(--ux4-blue-soft);
  color: var(--ux4-blue);
  font-size: 12px;
  font-weight: 700;
  padding: 0 8px;
}

.launchpad-progress {
  display: inline-block;
  width: 72px;
  height: 6px;
  border-radius: 999px;
  background: oklch(0.91 0.018 255);
  vertical-align: middle;
  overflow: hidden;
}

.launchpad-progress span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, oklch(0.55 0.18 257), oklch(0.62 0.15 257));
}

.launchpad-progress-text {
  margin-left: 8px;
  color: var(--ux4-text);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.launchpad-risk-pill,
.launchpad-side-list b {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  padding: 0 9px;
}

.launchpad-risk-pill.is-high,
.launchpad-side-list b.is-high {
  color: oklch(0.58 0.18 25);
  background: var(--ux4-red-soft);
  border: 1px solid var(--ux4-red-border);
}

.launchpad-risk-pill.is-medium,
.launchpad-side-list b.is-medium {
  color: oklch(0.58 0.14 75);
  background: var(--ux4-amber-soft);
  border: 1px solid var(--ux4-amber-border);
}

.launchpad-risk-pill.is-low,
.launchpad-side-list b.is-low {
  color: oklch(0.48 0.13 155);
  background: var(--ux4-green-soft);
  border: 1px solid var(--ux4-green-border);
}

.launchpad-row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.launchpad-row-actions :deep(.el-button) {
  margin-left: 0;
}

.launchpad-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 18px 16px;
  border-top: 1px solid var(--ux4-border-soft);
}

.launchpad-pagination > span {
  color: var(--ux4-text-muted);
  font-size: 12px;
}

.launchpad-status {
  padding-bottom: 14px;
}

.launchpad-donut-wrap {
  display: grid;
  grid-template-columns: 138px minmax(0, 1fr);
  gap: 18px;
  align-items: center;
  padding: 16px 18px 18px;
}

.launchpad-donut {
  width: 132px;
  height: 132px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  position: relative;
  box-shadow: inset 0 0 0 1px var(--ux4-border-soft);
}

.launchpad-donut::after {
  content: "";
  position: absolute;
  inset: 22px;
  border-radius: inherit;
  background: var(--ux4-surface);
  box-shadow: 0 0 0 1px var(--ux4-border-soft);
}

.launchpad-donut__content {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 6px;
  text-align: center;
}

.launchpad-donut__content span {
  color: var(--ux4-text-strong);
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.launchpad-donut__content em {
  color: var(--ux4-text-muted);
  font-size: 12px;
  font-style: normal;
  line-height: 1;
}

.launchpad-status__legend {
  display: grid;
  gap: 13px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.launchpad-status__legend li {
  display: grid;
  grid-template-columns: 10px 1fr auto;
  gap: 9px;
  align-items: center;
  min-width: 0;
}

.launchpad-status__legend span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.launchpad-status__legend strong,
.launchpad-status__legend em {
  color: var(--ux4-text);
  font-size: 13px;
  font-style: normal;
}

.launchpad-status__legend em {
  color: var(--ux4-text-muted);
  font-weight: 650;
}

.launchpad-side-list {
  display: grid;
  gap: 0;
  padding: 0 12px 14px;
}

.launchpad-side-list button {
  appearance: none;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 76px;
  border: 0;
  border-bottom: 1px solid oklch(0.9 0.02 25);
  background: oklch(0.975 0.025 25);
  color: var(--ux4-text);
  cursor: pointer;
  font-family: inherit;
  padding: 12px;
  text-align: left;
}

.launchpad-side-list button:first-child {
  border-radius: 10px 10px 0 0;
}

.launchpad-side-list button:last-child {
  border-bottom: 0;
  border-radius: 0 0 10px 10px;
}

.launchpad-side-list button:hover {
  background: oklch(0.965 0.035 25);
}

.launchpad-side-list__alert {
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: oklch(0.62 0.18 25);
  color: oklch(0.99 0.004 255);
  font-size: 12px;
  font-weight: 800;
}

.launchpad-side-list span:not(.launchpad-side-list__alert) {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.launchpad-side-list strong {
  color: var(--ux4-text-strong);
  font-size: 13px;
  font-weight: 750;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.launchpad-side-list em {
  color: var(--ux4-text-muted);
  font-size: 12px;
  font-style: normal;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.launchpad-notices {
  padding-bottom: 14px;
}

.launchpad-notices ul {
  display: grid;
  gap: 14px;
  margin: 0;
  padding: 8px 18px 18px;
  list-style: none;
}

.launchpad-notices li {
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.launchpad-notices li span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ux4-blue);
}

.launchpad-notices strong {
  color: var(--ux4-text);
  font-size: 13px;
  font-weight: 650;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.launchpad-notices time {
  color: var(--ux4-text-muted);
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .launchpad-layout {
    grid-template-columns: 1fr;
  }

  .launchpad-sidebar {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .launchpad-page__header,
  .launchpad-page__tools,
  .launchpad-hero-grid,
  .launchpad-filters,
  .launchpad-sidebar {
    grid-template-columns: 1fr;
  }

  .launchpad-todo__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .launchpad-recommend__meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .launchpad-pagination {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .launchpad-todo-card {
    transition: none;
  }

  .launchpad-todo-card:hover {
    transform: none;
  }
}
</style>
