<template>
  <section class="mvp-page catalog-page">
    <div class="mvp-page__header">
      <div>
        <h1>资产目录</h1>
        <p>按目录浏览已登记资产，查看元数据、存储路径可见性、专业、版本、质量状态和 Agent 可见性</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </div>

    <div class="catalog-filters">
      <el-select
        v-model="filters.projectId"
        clearable
        filterable
        placeholder="选择项目"
        style="width: 280px"
        @change="onProjectChange"
      >
        <el-option
          v-for="p in projects"
          :key="p.projectId"
          :label="`${p.projectCode} ${p.projectName}`"
          :value="p.projectId"
        />
      </el-select>
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="文件名、路径关键词"
        style="width: 220px"
        @keyup.enter="loadFiles"
        @clear="loadFiles"
      />
      <el-input
        v-model="filters.version"
        clearable
        placeholder="版本，如 V1"
        style="width: 130px"
        @keyup.enter="loadFiles"
        @clear="loadFiles"
      />
      <el-select v-model="filters.fileExt" clearable placeholder="文件扩展名" style="width: 140px">
        <el-option label="rvt" value="rvt" />
        <el-option label="dwg" value="dwg" />
        <el-option label="ifc" value="ifc" />
        <el-option label="nwd" value="nwd" />
        <el-option label="nwc" value="nwc" />
        <el-option label="dxf" value="dxf" />
        <el-option label="pdf" value="pdf" />
        <el-option label="doc" value="doc" />
        <el-option label="docx" value="docx" />
        <el-option label="xls" value="xls" />
        <el-option label="xlsx" value="xlsx" />
        <el-option label="glb" value="glb" />
      </el-select>
      <el-select v-model="filters.fileKind" clearable placeholder="文件类型" style="width: 120px">
        <el-option label="模型" value="MODEL" />
        <el-option label="图纸" value="DRAWING" />
        <el-option label="文档" value="DOCUMENT" />
        <el-option label="表格" value="SPREADSHEET" />
        <el-option label="汇报" value="PRESENTATION" />
      </el-select>
      <el-select v-model="filters.disciplineCode" clearable placeholder="专业" style="width: 120px">
        <el-option label="建筑" value="ARCHITECTURE" />
        <el-option label="结构" value="STRUCTURE" />
        <el-option label="暖通" value="HVAC" />
        <el-option label="给排水" value="PLUMBING" />
        <el-option label="电气" value="ELECTRICAL" />
        <el-option label="消防" value="FIRE_PROTECTION" />
        <el-option label="智能化" value="INTELLIGENT" />
        <el-option label="综合" value="GENERAL" />
        <el-option label="燃气" value="GAS" />
      </el-select>
      <el-select v-model="filters.qualityIssue" clearable placeholder="质量问题" style="width: 160px">
        <el-option label="缺校验码" value="MISSING_CHECKSUM" />
        <el-option label="缺置信度" value="MISSING_CONFIDENCE" />
        <el-option label="专业待完善" value="MISSING_DISCIPLINE" />
        <el-option label="版本缺失" value="MISSING_VERSION" />
        <el-option label="路径缺失" value="MISSING_STORAGE_PATH" />
        <el-option label="零大小文件" value="ZERO_SIZE_FILE" />
      </el-select>
      <el-button type="primary" @click="loadFiles">查询</el-button>
    </div>

    <div class="catalog-body">
      <DirectoryTreePanel
        :directories="directories"
        :active-path="filters.directoryPath"
        :root-label="selectedProjectName"
        :enabled="Boolean(filters.projectId)"
        :loading="directoriesLoading"
        empty-description="暂无目录"
        disabled-description="选择项目后浏览目录"
        @select="selectDirectoryPath"
      />

      <div class="catalog-file-panel">
        <div v-if="filters.directoryPath" class="catalog-active-directory">
          <span>当前目录</span>
          <el-tag closable @close="clearDirectory">{{ activeDirectoryLabel }}</el-tag>
        </div>

        <el-table
          v-loading="loading"
          :data="tableData"
          class="master-table"
          empty-text="暂无资产文件"
          @row-click="openDetail"
        >
          <el-table-column label="项目" min-width="180">
            <template #default="{ row }">
              <div class="catalog-project-cell">
                <strong>{{ row.projectCode }}</strong>
                <span>{{ row.projectName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="文件名" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">{{ row.fileName }}</template>
          </el-table-column>
          <el-table-column prop="fileExt" label="扩展名" width="80" />
          <el-table-column prop="fileKind" label="类型" width="80" />
          <el-table-column prop="disciplineCode" label="专业" width="100" show-overflow-tooltip />
          <el-table-column prop="version" label="版本" width="80" />
          <el-table-column label="大小" width="110" align="right">
            <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
          </el-table-column>
          <el-table-column label="质量" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.qualityFlags.length === 0" type="success" size="small">正常</el-tag>
              <el-tag v-else type="warning" size="small">{{ row.qualityFlags.length }}项</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Agent可见" width="90">
            <template #default="{ row }">
              <el-tag :type="row.agentReadable ? 'success' : 'info'" size="small">
                {{ row.agentReadable ? '可见' : '不可见' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="路径可见" width="90">
            <template #default="{ row }">
              <el-tag :type="row.storagePathVisible ? 'success' : 'info'" size="small">
                {{ row.storagePathVisible ? '可见' : '隐藏' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近更新" width="160">
            <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
          </el-table-column>
        </el-table>

        <div class="catalog-pagination">
          <el-pagination
            v-model:current-page="filters.page"
            v-model:page-size="filters.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @change="loadFiles"
          />
        </div>
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="detailTitle"
      size="560px"
    >
      <template v-if="detail">
        <div class="catalog-detail-section">
          <h3>项目信息</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="项目编码">{{ detail.projectCode }}</el-descriptions-item>
            <el-descriptions-item label="项目名称">{{ detail.projectName }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="catalog-detail-section">
          <h3>文件信息</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="文件ID">{{ detail.fileId }}</el-descriptions-item>
            <el-descriptions-item label="文件名">{{ detail.fileName }}</el-descriptions-item>
            <el-descriptions-item label="扩展名">{{ detail.fileExt }}</el-descriptions-item>
            <el-descriptions-item label="文件类型">{{ detail.fileKind }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ detail.disciplineCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ detail.version || '-' }}</el-descriptions-item>
            <el-descriptions-item label="大小">{{ formatBytes(detail.sizeBytes) }}</el-descriptions-item>
            <el-descriptions-item label="校验码">
              <span v-if="detail.checksum" class="checksum-mono">{{ detail.checksum }}</span>
              <el-tag v-else type="warning" size="small">缺失</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
            <el-descriptions-item label="置信度">{{ detail.confidenceLevel || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="catalog-detail-section">
          <h3>存储信息</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="存储提供方">{{ detail.storageProvider }}</el-descriptions-item>
            <el-descriptions-item label="逻辑路径">{{ detail.logicalPath || '-' }}</el-descriptions-item>
            <el-descriptions-item label="存储路径">
              <template v-if="detail.storagePathVisible">
                <span class="path-mono">{{ detail.storagePath }}</span>
              </template>
              <template v-else>
                <el-tag type="info" size="small">{{ detail.storagePathVisibilityReason }}</el-tag>
              </template>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="catalog-detail-section">
          <h3>文件访问</h3>
          <el-descriptions v-if="detailPreview" :column="1" border size="small">
            <el-descriptions-item label="预览状态">
              <el-tag :type="previewRiskTagType(detailPreview)" size="small">
                {{ previewStatusLabel(detailPreview) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="在线查看">{{ previewOnlineStateText(detailPreview) }}</el-descriptions-item>
            <el-descriptions-item label="预览方式">{{ previewModeLabel(detailPreview) }}</el-descriptions-item>
            <el-descriptions-item label="转换状态">{{ conversionStatusLabel(detailPreview) }}</el-descriptions-item>
            <el-descriptions-item label="访问权限">{{ detailPreview.accessPolicyMessage }}</el-descriptions-item>
            <el-descriptions-item label="业务提示">{{ previewActionHint(detailPreview) }}</el-descriptions-item>
          </el-descriptions>
          <el-alert
            v-else
            title="正在加载文件访问能力"
            type="info"
            show-icon
            :closable="false"
          />
          <div class="catalog-access-actions">
            <el-button
              type="primary"
              :disabled="!canOpenPreview(detailPreview)"
              :loading="accessLoading === 'PREVIEW'"
              @click="openFileAccess('PREVIEW')"
            >
              打开预览
            </el-button>
            <el-button
              :disabled="!detailPreview?.downloadAllowed"
              :loading="accessLoading === 'DOWNLOAD'"
              @click="openFileAccess('DOWNLOAD')"
            >
              下载文件
            </el-button>
            <el-button @click="openHermesForDetail">问 Hermes</el-button>
          </div>
        </div>

        <div class="catalog-detail-section">
          <h3>治理状态</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="质量标记">
              <el-tag v-if="detail.qualityFlags.length === 0" type="success" size="small">无</el-tag>
              <el-tag v-for="flag in detail.qualityFlags" :key="flag" type="warning" size="small" style="margin-right: 4px">
                {{ flag }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最近验证">{{ formatDate(detail.lastVerifiedAt) }}</el-descriptions-item>
            <el-descriptions-item label="最近更新">{{ formatDate(detail.updatedAt) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="catalog-detail-section">
          <h3>Agent 可见性</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="Agent可读">
              <el-tag :type="detail.agentReadable ? 'success' : 'info'" size="small">
                {{ detail.agentReadable ? '是' : '否' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="可读原因">{{ detail.agentReadReason }}</el-descriptions-item>
            <el-descriptions-item label="可读字段">
              <el-tag v-for="f in detail.agentContractView" :key="f" size="small" style="margin: 2px">{{ f }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </template>
    </el-drawer>

    <HermesWorkspaceDrawer v-model="hermesDrawerVisible">
      <DataStewardPanel
        v-if="detail"
        :project-id="detail.projectId"
        page-type="asset_detail"
        source-view="FileAssetView"
        :asset-id="detail.fileId"
      />
      <el-empty v-else description="请先选择文件" :image-size="56" />
    </HermesWorkspaceDrawer>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import HermesWorkspaceDrawer from '@/modules/data-steward/components/HermesWorkspaceDrawer.vue';
import {
  createFileAccessTicket,
  fetchCatalogDirectories,
  fetchCatalogProjects,
  fetchCatalogFiles,
  fetchCatalogFileDetail,
  fetchFilePreview,
  type CatalogDirectory,
  type CatalogProject,
  type CatalogFile,
  type CatalogFileDetail,
  type FilePreview
} from '@/modules/data-steward/api/dataSteward';
import DataStewardPanel from '@/modules/data-steward/components/DataStewardPanel.vue';
import DirectoryTreePanel from '@/modules/data-steward/components/DirectoryTreePanel.vue';
import { buildDirectoryTree } from '@/modules/data-steward/utils/directoryTree';
import {
  conversionStatusLabel,
  previewActionHint,
  previewModeLabel,
  previewOnlineStateText,
  previewRiskTagType,
  previewStatusLabel
} from '@/modules/data-steward/utils/previewStatus';

const projects = ref<CatalogProject[]>([]);
const directories = ref<CatalogDirectory[]>([]);
const tableData = ref<CatalogFile[]>([]);
const total = ref(0);
const loading = ref(false);
const directoriesLoading = ref(false);
const drawerVisible = ref(false);
const hermesDrawerVisible = ref(false);
const detail = ref<CatalogFileDetail | null>(null);
const detailPreview = ref<FilePreview | null>(null);
const accessLoading = ref<'PREVIEW' | 'DOWNLOAD' | null>(null);

const filters = ref({
  projectId: undefined as number | undefined,
  keyword: '',
  directoryPath: '',
  version: '',
  fileExt: '',
  fileKind: '',
  disciplineCode: '',
  qualityIssue: '',
  page: 1,
  pageSize: 20
});

const detailTitle = computed(() => detail.value ? `${detail.value.fileName} - 详情` : '文件详情');
const selectedProjectName = computed(() => {
  if (!filters.value.projectId) return '未选择项目';
  const project = projects.value.find(item => item.projectId === filters.value.projectId);
  return project ? `${project.projectName} (${project.projectCode})` : '当前项目';
});
const directoryModel = computed(() => buildDirectoryTree(directories.value));
const activeDirectoryLabel = computed(() => {
  if (!filters.value.directoryPath) return '项目根目录';
  return directoryModel.value.labelByPath.get(filters.value.directoryPath) ?? compactDirectoryName(filters.value.directoryPath);
});

function loadPage() {
  fetchCatalogProjects().then(list => { projects.value = list ?? []; });
  loadFiles();
}

function onProjectChange() {
  filters.value.page = 1;
  filters.value.directoryPath = '';
  loadDirectories();
  loadFiles();
}

function loadDirectories() {
  directories.value = [];
  if (!filters.value.projectId) return;
  directoriesLoading.value = true;
  fetchCatalogDirectories(filters.value.projectId)
    .then(list => { directories.value = list ?? []; })
    .finally(() => { directoriesLoading.value = false; });
}

function selectDirectoryPath(directoryPath: string) {
  filters.value.directoryPath = directoryPath;
  filters.value.page = 1;
  loadFiles();
}

function clearDirectory() {
  filters.value.directoryPath = '';
  filters.value.page = 1;
  loadFiles();
}

function loadFiles() {
  loading.value = true;
  fetchCatalogFiles({
    projectId: filters.value.projectId,
    keyword: filters.value.keyword || undefined,
    directoryPath: filters.value.directoryPath || undefined,
    version: filters.value.version || undefined,
    fileExt: filters.value.fileExt || undefined,
    fileKind: filters.value.fileKind || undefined,
    disciplineCode: filters.value.disciplineCode || undefined,
    qualityIssue: filters.value.qualityIssue || undefined,
    page: filters.value.page,
    pageSize: filters.value.pageSize
  })
    .then(result => {
      tableData.value = result.rows;
      total.value = result.total;
    })
    .finally(() => { loading.value = false; });
}

function openDetail(row: CatalogFile) {
  detailPreview.value = null;
  hermesDrawerVisible.value = false;
  fetchCatalogFileDetail(row.fileId).then(d => {
    detail.value = d;
    drawerVisible.value = true;
  });
  fetchFilePreview(row.fileId)
    .then(p => { detailPreview.value = p; })
    .catch(() => { detailPreview.value = null; });
}

function canOpenPreview(preview: FilePreview | null) {
  return Boolean(preview?.previewAvailable && preview.previewAllowed);
}

async function openFileAccess(action: 'PREVIEW' | 'DOWNLOAD') {
  const preview = detailPreview.value;
  if (!preview) {
    ElMessage.warning('请先等待文件访问能力加载完成');
    return;
  }
  if (action === 'PREVIEW' && !canOpenPreview(preview)) {
    ElMessage.warning(previewActionHint(preview) || preview.accessPolicyMessage || '当前文件暂不可预览');
    return;
  }
  if (action === 'DOWNLOAD' && !preview.downloadAllowed) {
    ElMessage.warning(preview.accessPolicyMessage || '当前账号没有下载权限');
    return;
  }
  const popup = window.open('', '_blank', 'noopener');
  accessLoading.value = action;
  try {
    const ticket = await createFileAccessTicket(preview.fileId, action);
    if (popup) {
      popup.location.href = ticket.accessUrl;
    } else {
      window.open(ticket.accessUrl, '_blank', 'noopener');
    }
    ElMessage.success(action === 'PREVIEW' ? '预览入口已打开' : '下载入口已打开');
  } catch (error) {
    popup?.close();
    ElMessage.error(error instanceof Error ? error.message : '文件访问票据创建失败');
  } finally {
    accessLoading.value = null;
  }
}

function openHermesForDetail() {
  if (!detail.value) return;
  hermesDrawerVisible.value = true;
}

function compactDirectoryName(path: string): string {
  if (!path) return '根目录';
  const normalized = path.replace(/\/+$/, '');
  const segments = normalized.split('/').filter(Boolean);
  return segments.at(-1) ?? normalized;
}

function formatBytes(bytes: number): string {
  if (!bytes || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let i = 0;
  let v = bytes;
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
  return `${v.toFixed(i === 0 ? 0 : 2)} ${units[i]}`;
}

function formatDate(v: string | null): string {
  if (!v) return '-';
  const d = new Date(v);
  return d.toLocaleString('zh-CN', { hour12: false });
}

loadPage();
</script>

<style scoped>
.catalog-filters {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.catalog-body {
  display: grid;
  grid-template-columns: minmax(240px, 320px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
.catalog-file-panel {
  min-width: 0;
}
.catalog-active-directory {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
  color: #606266;
  font-size: 13px;
}
.catalog-project-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.catalog-project-cell span {
  font-size: 12px;
  color: #909399;
}
.catalog-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.catalog-detail-section {
  margin-bottom: 20px;
}
.catalog-detail-section h3 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}
.catalog-access-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}
.checksum-mono {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}
.path-mono {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
}
@media (max-width: 1100px) {
  .catalog-body {
    grid-template-columns: 1fr;
  }
}
</style>
