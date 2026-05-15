<template>
  <section class="mvp-page asset-page">
    <div class="mvp-page__header">
      <div>
        <h1>资产总览</h1>
        <p>公司级 BIM/CAD 资产台账</p>
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

    <section class="mvp-dashboard">
      <article v-for="item in cards" :key="item.label" class="mvp-stat mvp-stat--large">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.unit }}</em>
      </article>
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
      <el-table-column prop="projectStage" label="阶段" width="120" show-overflow-tooltip />
      <el-table-column prop="projectManagerName" label="负责人" width="130" show-overflow-tooltip />
      <el-table-column label="模型数" width="110" align="right">
        <template #default="{ row }">{{ formatCount(row.modelCount) }}</template>
      </el-table-column>
      <el-table-column label="模型容量" width="130" align="right">
        <template #default="{ row }">{{ formatBytes(row.totalSizeBytes) }}</template>
      </el-table-column>
      <el-table-column label="最近更新" width="170">
        <template #default="{ row }">{{ formatDate(row.lastModelUpdatedAt) }}</template>
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
  fetchAssetStatistics,
  type AssetProject,
  type AssetStatistics
} from '@/modules/data-steward/api/dataSteward';

const router = useRouter();
const loading = ref(false);
const keyword = ref('');
const sourceFilter = ref('REAL_NAS');
const projectSortOrder = ref<'ASC' | 'DESC'>('ASC');
const projects = ref<AssetProject[]>([]);
const statistics = ref<AssetStatistics | null>(null);
const sourceOptions = [
  { label: '真实NAS', value: 'REAL_NAS' },
  { label: '全部', value: 'ALL' }
];

const cards = computed(() => {
  const item = statistics.value;
  return [
    { label: '资产项目', value: formatCount(item?.projectCount), unit: '个' },
    { label: '文件总数', value: formatCount(item?.fileCount), unit: '份' },
    { label: '模型文件', value: formatCount(item?.modelFileCount), unit: '份' },
    { label: '图纸文件', value: formatCount(item?.drawingFileCount), unit: '份' },
    { label: '资产容量', value: formatBytes(item?.totalSizeBytes), unit: '已登记' },
    { label: '最近更新', value: formatDate(item?.lastUpdatedAt), unit: '时间' }
  ];
});

const sortedProjects = computed(() => {
  return [...projects.value].sort((left, right) => {
    return projectSortOrder.value === 'ASC'
      ? left.projectId - right.projectId
      : right.projectId - left.projectId;
  });
});

loadPage();

async function loadPage() {
  loading.value = true;
  try {
    const assetSource = sourceFilter.value === 'REAL_NAS' ? 'NAS_REAL*' : undefined;
    const [nextProjects, nextStatistics] = await Promise.all([
      fetchAssetProjects(keyword.value.trim() || undefined, assetSource),
      fetchAssetStatistics(undefined, assetSource)
    ]);
    projects.value = nextProjects;
    statistics.value = nextStatistics;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '资产总览加载失败');
  } finally {
    loading.value = false;
  }
}

function openDetail(row: AssetProject) {
  router.push({ name: 'data-steward-asset-detail', params: { projectId: row.projectId } });
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

@media (max-width: 960px) {
  .asset-search,
  .asset-sort {
    width: 100%;
  }
}
</style>
