<template>
  <section class="mvp-page quality-page">
    <div class="mvp-page__header">
      <div>
        <h1>数据质量</h1>
        <p>一期资产治理体检</p>
      </div>
      <div class="mvp-page__actions">
        <el-segmented v-model="sourceFilter" :options="sourceOptions" @change="loadPage" />
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </div>

    <section class="mvp-dashboard">
      <article v-for="item in cards" :key="item.label" class="mvp-stat mvp-stat--large">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.unit }}</em>
      </article>
    </section>

    <section class="quality-grid">
      <div class="quality-panel">
        <div class="quality-panel__header">
          <h2>体检项</h2>
          <span>{{ formatDate(overview?.latestAssetUpdatedAt) }}</span>
        </div>
        <el-table v-loading="loading" :data="overview?.metrics ?? []" class="master-table" empty-text="暂无体检结果">
          <el-table-column label="项目" min-width="160">
            <template #default="{ row }">
              <strong>{{ row.label }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="级别" width="90">
            <template #default="{ row }">
              <el-tag :type="severityTag(row.severity)">{{ severityLabel(row.severity) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="110" align="right">
            <template #default="{ row }">{{ formatCount(row.count) }}</template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="260" show-overflow-tooltip />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button text :disabled="row.count <= 0" @click="openMetric(row.code)">治理</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="quality-panel">
        <div class="quality-panel__header">
          <h2>风险项目</h2>
          <span>按风险信号排序</span>
        </div>
        <el-table
          v-loading="loading"
          :data="overview?.topRiskProjects ?? []"
          class="master-table"
          empty-text="暂无风险项目"
        >
          <el-table-column label="项目" min-width="220">
            <template #default="{ row }">
              <div class="quality-project-cell">
                <strong>{{ row.projectCode }}</strong>
                <span>{{ row.projectName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="总风险" width="96" align="right">
            <template #default="{ row }">{{ formatCount(row.totalRiskCount) }}</template>
          </el-table-column>
          <el-table-column label="缺 checksum" width="120" align="right">
            <template #default="{ row }">{{ formatCount(row.missingChecksumCount) }}</template>
          </el-table-column>
          <el-table-column label="专业待完善" width="120" align="right">
            <template #default="{ row }">{{ formatCount(row.missingDisciplineCount) }}</template>
          </el-table-column>
          <el-table-column label="零大小" width="90" align="right">
            <template #default="{ row }">{{ formatCount(row.zeroSizeFileCount) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.missingChecksumCount > 0"
                text
                @click="openProjectIssue(row.projectId, 'MISSING_CHECKSUM')"
              >
                补 checksum
              </el-button>
              <el-button
                v-if="row.missingDisciplineCount > 0"
                text
                @click="openProjectIssue(row.projectId, 'MISSING_DISCIPLINE')"
              >
                完善专业
              </el-button>
              <el-button
                v-if="row.zeroSizeFileCount > 0"
                text
                @click="openProjectIssue(row.projectId, 'ZERO_SIZE_FILE')"
              >
                查零大小
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <section class="quality-panel">
      <div class="quality-panel__header">
        <h2>最近治理事件</h2>
        <span>{{ formatDate(overview?.latestEventAt) }}</span>
      </div>
      <el-table v-loading="loading" :data="overview?.recentEvents ?? []" class="master-table" empty-text="暂无治理事件">
        <el-table-column prop="id" label="事件ID" width="100" />
        <el-table-column prop="eventType" label="类型" width="110" />
        <el-table-column prop="actionCode" label="动作" width="150" show-overflow-tooltip />
        <el-table-column prop="summary" label="摘要" min-width="360" show-overflow-tooltip />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import {
  fetchAssetQualityOverview,
  type AssetQualityOverview
} from '@/modules/data-steward/api/dataSteward';

const router = useRouter();
const loading = ref(false);
const sourceFilter = ref('REAL_NAS');
const overview = ref<AssetQualityOverview | null>(null);
const sourceOptions = [
  { label: '真实NAS', value: 'REAL_NAS' },
  { label: '全部', value: 'ALL' }
];

const cards = computed(() => {
  const item = overview.value;
  return [
    { label: '风险信号', value: formatCount(item?.riskSignalCount), unit: '项' },
    { label: '待审核', value: formatCount(item?.pendingReviewCount), unit: '条' },
    { label: '失败扫描', value: formatCount(item?.failedScanCount), unit: '条' },
    { label: '缺 checksum', value: formatCount(item?.missingChecksumCount), unit: '份' },
    { label: '非标准待治理', value: formatCount(item?.nonstandardPendingCount), unit: '个' },
    { label: '最近事件', value: formatDate(item?.latestEventAt), unit: '时间' }
  ];
});

loadPage();

async function loadPage() {
  loading.value = true;
  try {
    const assetSource = sourceFilter.value === 'REAL_NAS' ? 'NAS_REAL*' : undefined;
    overview.value = await fetchAssetQualityOverview(undefined, assetSource);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '数据质量体检加载失败');
  } finally {
    loading.value = false;
  }
}

function openMetric(code: string) {
  if (code === 'FAILED_SCAN') {
    router.push({ name: 'data-steward-scans', query: { status: 'FAILED' } });
    return;
  }
  if (code === 'RUNNING_SCAN') {
    router.push({ name: 'data-steward-scans', query: { status: 'RUNNING' } });
    return;
  }
  if (code === 'PENDING_REVIEW') {
    router.push({ name: 'data-steward-scans', query: { hasPendingReview: 'true' } });
    return;
  }
  if (code === 'NONSTANDARD_PENDING') {
    router.push({ name: 'data-steward-nonstandard-directories', query: { governanceStatus: 'PENDING_AGENT' } });
    return;
  }
  if (code === 'NONSTANDARD_APPROVED') {
    router.push({ name: 'data-steward-nonstandard-directories', query: { governanceStatus: 'APPROVED_FOR_IMPORT' } });
    return;
  }

  const projectId = firstProjectIdForIssue(code);
  if (projectId) {
    openProjectIssue(projectId, code);
    return;
  }
  ElMessage.info('请先从风险项目列表进入具体项目');
}

function openProjectIssue(projectId: number, qualityIssue: string) {
  router.push({
    name: 'data-steward-asset-detail',
    params: { projectId },
    query: { qualityIssue }
  });
}

function firstProjectIdForIssue(code: string) {
  const projects = overview.value?.topRiskProjects ?? [];
  const matched = projects.find((item) => issueCount(item, code) > 0);
  return matched?.projectId;
}

function issueCount(project: AssetQualityOverview['topRiskProjects'][number], code: string) {
  const map: Record<string, number> = {
    MISSING_CHECKSUM: project.missingChecksumCount,
    MISSING_CONFIDENCE: project.missingConfidenceCount,
    MISSING_DISCIPLINE: project.missingDisciplineCount,
    MISSING_VERSION: project.missingVersionCount,
    MISSING_STORAGE_PATH: project.missingStoragePathCount,
    ZERO_SIZE_FILE: project.zeroSizeFileCount,
    PENDING_REVIEW: project.pendingReviewCount,
    FAILED_SCAN: project.failedScanCount
  };
  return map[code] ?? 0;
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

function severityLabel(value: string) {
  const labels: Record<string, string> = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低',
    INFO: '提示'
  };
  return labels[value] ?? value;
}

function severityTag(value: string) {
  const tags: Record<string, 'danger' | 'warning' | 'info' | 'success'> = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'info',
    INFO: 'success'
  };
  return tags[value] ?? 'info';
}
</script>

<style scoped>
.quality-page {
  min-width: 0;
}

.quality-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
}

.quality-panel {
  min-width: 0;
  display: grid;
  gap: 12px;
}

.quality-panel__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.quality-panel__header h2 {
  margin: 0;
  font-size: 18px;
}

.quality-panel__header span {
  color: var(--zy-muted);
  font-size: 13px;
}

.quality-project-cell {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.quality-project-cell strong,
.quality-project-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quality-project-cell span {
  color: var(--zy-muted);
}

@media (max-width: 1180px) {
  .quality-grid {
    grid-template-columns: 1fr;
  }
}
</style>
