<template>
  <section class="mvp-page export-page">
    <div class="mvp-page__header">
      <div>
        <h1>导出列表</h1>
        <p>{{ projectLabel }}，导出受控元数据清单，不包含真实 NAS 绝对路径。</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="loadPreview">刷新</el-button>
      </div>
    </div>

    <el-alert
      title="本批只开放即时清单导出，不做异步大批量打包下载，也不导出底层 NAS 路径。"
      type="info"
      show-icon
      :closable="false"
    />

    <section class="export-grid">
      <article class="export-card">
        <div>
          <span>文件清单</span>
          <h2>当前项目文件元数据 CSV</h2>
          <p>包含文件 ID、文件名、类型、专业、版本、大小、项目内路径和更新时间。</p>
        </div>
        <el-button type="primary" :loading="exporting === 'files'" @click="exportFiles">导出 CSV</el-button>
      </article>
      <article class="export-card export-card--disabled">
        <div>
          <span>批量打包</span>
          <h2>文件包下载</h2>
          <p>需要容量、权限和审批策略，后续单独开放。</p>
        </div>
        <el-button disabled>未开放</el-button>
      </article>
      <article class="export-card export-card--disabled">
        <div>
          <span>模型成果</span>
          <h2>模型集成报表</h2>
          <p>后续与模型轻量化和构件解析批次一起完善。</p>
        </div>
        <el-button disabled>未开放</el-button>
      </article>
    </section>

    <section class="export-preview">
      <div class="export-preview__header">
        <div>
          <h2>导出预览</h2>
          <span>最多展示前 20 条，实际导出最多取当前项目前 1000 条元数据。</span>
        </div>
        <el-tag type="info" effect="plain">{{ previewRows.length }} 条预览</el-tag>
      </div>
      <el-table v-loading="loading" :data="previewRows" class="master-table" empty-text="暂无可导出文件">
        <el-table-column prop="fileId" label="文件ID" width="90" />
        <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
        <el-table-column prop="fileKind" label="类型" width="100" />
        <el-table-column prop="disciplineName" label="专业" width="120" />
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column label="大小" width="120">
          <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
        </el-table-column>
        <el-table-column label="项目内路径" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ safeLogicalPath(row) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import { fetchCatalogFiles, type CatalogFile } from '@/modules/data-steward/api/dataSteward';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const authStore = useAuthStore();
const loading = ref(false);
const exporting = ref('');
const previewRows = ref<CatalogFile[]>([]);

const projectId = computed(() => {
  const routeId = Number(route.params.projectId);
  return Number.isFinite(routeId) && routeId > 0 ? routeId : authStore.currentProjectId;
});
const projectLabel = computed(() => {
  const current = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return current ? `${current.code} ${current.name}` : '等待项目上下文';
});

watch(projectId, () => loadPreview(), { immediate: true });

async function loadPreview() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const page = await fetchCatalogFiles({ projectId: projectId.value, page: 1, pageSize: 20 });
    previewRows.value = page.rows;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出预览加载失败');
  } finally {
    loading.value = false;
  }
}

async function exportFiles() {
  if (!projectId.value) return;
  exporting.value = 'files';
  try {
    const page = await fetchCatalogFiles({ projectId: projectId.value, page: 1, pageSize: 1000 });
    const csv = toCsv(page.rows);
    downloadCsv(csv, `project-${projectId.value}-file-catalog.csv`);
    ElMessage.success('文件清单已生成，未包含真实 NAS 路径');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败');
  } finally {
    exporting.value = '';
  }
}

function toCsv(rows: CatalogFile[]) {
  const header = ['文件ID', '项目编码', '项目名称', '文件名', '类型', '扩展名', '专业', '版本', '大小', '项目内路径', '更新时间'];
  const body = rows.map((row) => [
    row.fileId,
    row.projectCode,
    row.projectName,
    row.fileName,
    row.fileKind,
    row.fileExt,
    row.disciplineName || row.disciplineCode || '',
    row.version,
    row.sizeBytes,
    safeLogicalPath(row),
    row.updatedAt || ''
  ]);
  return [header, ...body].map((line) => line.map(csvCell).join(',')).join('\n');
}

function safeLogicalPath(row: CatalogFile) {
  const path = row.logicalPath || row.fileName || '';
  const normalized = path.replace(/\\/g, '/');
  if (normalized.includes('/Volumes/') || normalized.startsWith('nas://') || normalized.startsWith('//')) {
    return row.fileName || '受控文件路径';
  }
  return normalized.replace(/^\/+/, '');
}

function csvCell(value: unknown) {
  const text = String(value ?? '');
  return `"${text.replace(/"/g, '""')}"`;
}

function downloadCsv(csv: string, filename: string) {
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
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
</script>

<style scoped>
.export-page {
  min-width: 0;
}

.export-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.export-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  min-width: 0;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #ffffff;
}

.export-card--disabled {
  background: #f8fafc;
}

.export-card span {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.export-card h2 {
  margin: 4px 0;
  color: #0f172a;
  font-size: 16px;
}

.export-card p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.export-preview {
  display: grid;
  gap: 12px;
}

.export-preview__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.export-preview__header h2 {
  margin: 0;
  font-size: 16px;
}

.export-preview__header span {
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .export-grid {
    grid-template-columns: 1fr;
  }
}
</style>
