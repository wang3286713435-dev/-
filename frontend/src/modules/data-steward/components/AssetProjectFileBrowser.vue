<template>
  <div
    ref="browserRef"
    class="file-browser"
    :class="{ 'is-resizing': resizingTree }"
    :style="fileBrowserStyle"
  >
    <div class="file-browser__tree-pane">
      <div v-if="dirLoadFailed" class="file-browser__tree-error">
        <p>目录结构加载失败，请稍后重试；右侧文件列表仍可使用。</p>
        <el-button size="small" type="primary" :loading="dirLoading" @click="loadDirectories()">重试目录加载</el-button>
      </div>
      <DirectoryTreePanel
        v-else
        :directories="directories"
        :active-path="activeDir"
        :root-label="rootLabel"
        :loading="dirLoading"
        empty-description="暂无可浏览目录"
        @select="selectDir"
        @enter="enterDir"
      />
    </div>

    <div
      class="file-browser__resize-handle"
      role="separator"
      aria-label="调整目录树宽度"
      tabindex="0"
      title="拖动调整目录树宽度，双击恢复默认"
      @pointerdown="startTreeResize"
      @dblclick="resetTreeWidth"
      @keydown.left.prevent="nudgeTreeWidth(-24)"
      @keydown.right.prevent="nudgeTreeWidth(24)"
    />

    <section class="file-browser__table">
      <div class="file-browser__actionbar">
        <div class="file-browser__safe-actions">
          <el-tooltip :content="readonlyActionTip" placement="top">
            <span>
              <el-button type="primary" disabled :icon="Upload">上传文件</el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="readonlyActionTip" placement="top">
            <span>
              <el-button disabled :icon="FolderAdd">新建文件夹</el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="readonlyActionTip" placement="top">
            <span>
              <el-button disabled>导入文件目录</el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="readonlyActionTip" placement="top">
            <span>
              <el-button disabled>引入目录模板</el-button>
            </span>
          </el-tooltip>
        </div>
        <div class="file-browser__search">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索文件或ID"
            :prefix-icon="Search"
            @keyup.enter="reloadFiles"
            @clear="reloadFiles"
          />
          <el-button :icon="Search" @click="reloadFiles">查询</el-button>
          <el-button text @click="advancedSearchVisible = !advancedSearchVisible">
            高级搜索
            <el-icon class="file-browser__chevron" :class="{ 'is-open': advancedSearchVisible }">
              <ArrowDown />
            </el-icon>
          </el-button>
        </div>
      </div>

      <div v-if="advancedSearchVisible" class="file-browser__advanced">
        <el-select v-model="filters.fileKind" @change="reloadFiles">
          <el-option v-for="item in fileKindOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.disciplineCode" clearable filterable placeholder="专业" @change="reloadFiles">
          <el-option v-for="item in disciplineOptions" :key="item.code" :label="item.name" :value="item.code" />
        </el-select>
        <el-input v-model="filters.fileExt" clearable placeholder="扩展名" @keyup.enter="reloadFiles" @clear="reloadFiles" />
        <el-select v-model="filters.qualityIssue" @change="reloadFiles">
          <el-option v-for="item in qualityIssueOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button @click="resetAdvancedFilters">重置</el-button>
      </div>

      <el-alert
        v-if="filters.qualityIssue === 'MISSING_CHECKSUM'"
        class="file-browser__checksum-alert"
        type="warning"
        show-icon
        :closable="false"
      >
        <template #title>
          checksum 是文件指纹，缺失不影响查找文件，但会影响重复识别和后续审计。建议按目录或类型分批补算，单次最多创建 500 个后台任务。
        </template>
        <el-button size="small" type="warning" plain :loading="props.batchChecksumCreating" @click="$emit('create-batch-checksum')">
          创建本项目补算任务
        </el-button>
      </el-alert>

      <div class="file-browser__breadcrumb">
        <button type="button" :disabled="!activeDir" @click="goParentDir">返回上级</button>
        <button type="button" :class="{ 'is-active': !activeDir }" @click="selectDir('')">{{ rootLabel }}</button>
        <template v-for="item in breadcrumbItems" :key="item.path">
          <span>/</span>
          <button type="button" :class="{ 'is-active': item.path === activeDir }" @click="selectDir(item.path)">
            {{ item.label }}
          </button>
        </template>
      </div>

      <el-table
        ref="tableRef"
        v-loading="fileLoading"
        :data="files"
        class="master-table"
        empty-text="暂无文件资产"
      >
        <el-table-column label="名称 / ID" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="file-browser__name-cell">
              <el-icon><Document /></el-icon>
              <div>
                <strong>{{ row.fileName }}</strong>
                <span>文件ID: {{ row.fileId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileKind" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="fileKindTag(row.fileKind)">{{ row.fileKind }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileExt" label="扩展名" width="90" />
        <el-table-column label="专业" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag type="info">{{ row.disciplineName || disciplineLabel(row.disciplineCode) || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column label="大小" width="120" align="right">
          <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
        </el-table-column>
        <el-table-column prop="confidenceLevel" label="置信度" width="100">
          <template #default="{ row }">
            <el-tag :type="row.confidenceLevel === 'HIGH' ? 'success' : 'warning'">
              {{ row.confidenceLevel ?? '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="质量问题" width="130">
          <template #default="{ row }">
            <el-tag v-if="!row.qualityFlags || row.qualityFlags.length === 0" type="success" size="small">正常</el-tag>
            <el-tag v-else type="warning" size="small">{{ row.qualityFlags.length }} 项</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-dropdown trigger="click" @command="handleRowCommand($event, row)">
              <el-button text>
                更多
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="preview">预览/下载</el-dropdown-item>
                  <el-dropdown-item command="detail">详情</el-dropdown-item>
                  <el-dropdown-item command="metadata">治理</el-dropdown-item>
                  <el-dropdown-item command="checksum">补 checksum</el-dropdown-item>
                  <el-dropdown-item disabled divided>移动</el-dropdown-item>
                  <el-dropdown-item disabled>删除</el-dropdown-item>
                  <el-dropdown-item disabled>更新版本</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="file-browser__pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[20, 50, 100, 200]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @change="loadFiles"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { ArrowDown, Document, FolderAdd, MoreFilled, Search, Upload } from '@element-plus/icons-vue';

import {
  fetchCatalogDirectories,
  fetchCatalogFiles,
  type CatalogDirectory,
  type CatalogFile,
  type AssetDiscipline
} from '@/modules/data-steward/api/dataSteward';
import DirectoryTreePanel from '@/modules/data-steward/components/DirectoryTreePanel.vue';

const props = defineProps<{
  projectId: number;
  rootLabel: string;
  disciplineOptions: AssetDiscipline[];
  initialQualityIssue?: string;
  batchChecksumCreating?: boolean;
}>();

const emit = defineEmits<{
  'open-preview': [fileId: number];
  'open-detail': [fileId: number];
  'open-metadata': [fileId: number];
  'create-checksum': [fileId: number];
  'create-batch-checksum': [];
}>();

const TREE_WIDTH_KEY = 'delivery.dataSteward.fileBrowser.treeWidth';
const DEFAULT_TREE_WIDTH = 320;
const MIN_TREE_WIDTH = 240;
const MIN_TABLE_WIDTH = 560;
const MAX_TREE_WIDTH = 640;

const browserRef = ref<HTMLElement | null>(null);
const tableRef = ref<{ doLayout?: () => void } | null>(null);
const directories = ref<CatalogDirectory[]>([]);
const files = ref<CatalogFile[]>([]);
const activeDir = ref('');
const fileLoading = ref(false);
const dirLoading = ref(false);
const dirLoadFailed = ref(false);
const advancedSearchVisible = ref(false);
const treeWidth = ref(DEFAULT_TREE_WIDTH);
const resizingTree = ref(false);
let resizePointerId: number | null = null;
let tableLayoutFrame = 0;
let directoryRequestId = 0;
let fileRequestId = 0;

const filters = reactive({
  keyword: '',
  fileKind: 'ALL',
  disciplineCode: '',
  fileExt: '',
  qualityIssue: props.initialQualityIssue || 'ALL'
});

const pagination = reactive({
  page: 1,
  pageSize: 50,
  total: 0
});

const fileKindOptions = [
  { label: '全部', value: 'ALL' },
  { label: '模型', value: 'MODEL' },
  { label: '图纸', value: 'DRAWING' },
  { label: '文档', value: 'DOCUMENT' },
  { label: '表格', value: 'SPREADSHEET' },
  { label: '汇报', value: 'PRESENTATION' },
  { label: '归档包', value: 'ARCHIVE' }
];

const readonlyActionTip = '当前阶段只读接管 NAS 资产，不写入真实目录；新建、上传、移动、删除将在受控写操作阶段开放。';

const qualityIssueOptions = [
  { label: '全部质量', value: 'ALL' },
  { label: '缺 checksum', value: 'MISSING_CHECKSUM' },
  { label: '缺置信度', value: 'MISSING_CONFIDENCE' },
  { label: '专业待完善', value: 'MISSING_DISCIPLINE' },
  { label: '版本缺失', value: 'MISSING_VERSION' },
  { label: '路径缺失', value: 'MISSING_STORAGE_PATH' },
  { label: '零大小', value: 'ZERO_SIZE_FILE' }
];

const fileBrowserStyle = computed(() => ({
  '--tree-width': `${treeWidth.value}px`
}));

const breadcrumbItems = computed(() => {
  if (!activeDir.value) return [];
  const parts = splitPath(activeDir.value);
  const visibleParts = parts.slice(directoryPrefixLength.value);
  const hasLeadingSlash = activeDir.value.startsWith('/');
  return visibleParts.map((label, index) => {
    const originalIndex = directoryPrefixLength.value + index;
    return {
      label,
      path: joinPath(parts.slice(0, originalIndex + 1), hasLeadingSlash)
    };
  });
});

const directoryPrefixLength = computed(() => {
  const parsed = directories.value
    .map((directory) => splitPath(normalizeDirectoryPath(directory.directoryPath)))
    .filter((parts) => parts.length > 0);

  if (!parsed.length) return 0;

  const [first, ...rest] = parsed;
  let prefixLength = first.length;
  for (const parts of rest) {
    prefixLength = Math.min(prefixLength, parts.length);
    for (let index = 0; index < prefixLength; index += 1) {
      if (first[index] !== parts[index]) {
        prefixLength = index;
        break;
      }
    }
  }
  return prefixLength;
});

watch(
  () => [props.projectId, props.initialQualityIssue] as const,
  () => {
    activeDir.value = '';
    files.value = [];
    pagination.page = 1;
    pagination.total = 0;
    filters.qualityIssue = props.initialQualityIssue || 'ALL';
    void loadDirectories();
    void loadFiles();
  },
  { immediate: true }
);

onMounted(() => {
  treeWidth.value = clampTreeWidth(readStoredTreeWidth());
  window.addEventListener('resize', handleWindowResize);
});

onUnmounted(() => {
  stopTreeResize();
  window.removeEventListener('resize', handleWindowResize);
  if (tableLayoutFrame) {
    window.cancelAnimationFrame(tableLayoutFrame);
  }
});

async function loadDirectories() {
  if (!Number.isFinite(props.projectId)) return;
  const requestId = ++directoryRequestId;
  dirLoading.value = true;
  dirLoadFailed.value = false;
  try {
    const nextDirectories = await fetchCatalogDirectories(props.projectId);
    if (requestId === directoryRequestId) {
      directories.value = nextDirectories;
      dirLoadFailed.value = false;
    }
  } catch (error) {
    if (requestId === directoryRequestId) {
      dirLoadFailed.value = true;
      ElMessage.error('目录结构加载失败，请稍后重试；右侧文件列表仍可使用。');
    }
  } finally {
    if (requestId === directoryRequestId) {
      dirLoading.value = false;
    }
  }
}

async function loadFiles() {
  if (!Number.isFinite(props.projectId)) return;
  const requestId = ++fileRequestId;
  fileLoading.value = true;
  try {
    const result = await fetchCatalogFiles({
      projectId: props.projectId,
      directoryPath: activeDir.value || undefined,
      keyword: filters.keyword.trim() || undefined,
      fileKind: filters.fileKind === 'ALL' ? undefined : filters.fileKind,
      disciplineCode: filters.disciplineCode || undefined,
      fileExt: normalizeExt(filters.fileExt),
      qualityIssue: filters.qualityIssue === 'ALL' ? undefined : filters.qualityIssue,
      page: pagination.page,
      pageSize: pagination.pageSize
    });
    if (requestId === fileRequestId) {
      files.value = result.rows;
      pagination.page = result.page;
      pagination.pageSize = result.pageSize;
      pagination.total = result.total;
    }
  } catch (error) {
    if (requestId === fileRequestId) {
      ElMessage.error(error instanceof Error ? error.message : '文件资产加载失败');
    }
  } finally {
    if (requestId === fileRequestId) {
      fileLoading.value = false;
    }
  }
}

function selectDir(dirPath: string) {
  activeDir.value = dirPath;
  pagination.page = 1;
  void loadFiles();
}

function enterDir(dirPath: string) {
  selectDir(dirPath);
}

function startTreeResize(event: PointerEvent) {
  if (event.button !== 0) return;
  resizingTree.value = true;
  resizePointerId = event.pointerId;
  (event.currentTarget as HTMLElement).setPointerCapture?.(event.pointerId);
  window.addEventListener('pointermove', handleTreeResize);
  window.addEventListener('pointerup', stopTreeResize);
  resizeTreeTo(event.clientX);
}

function handleTreeResize(event: PointerEvent) {
  if (resizePointerId !== null && event.pointerId !== resizePointerId) return;
  resizeTreeTo(event.clientX);
}

function stopTreeResize() {
  if (!resizingTree.value && resizePointerId === null) return;
  resizingTree.value = false;
  resizePointerId = null;
  window.removeEventListener('pointermove', handleTreeResize);
  window.removeEventListener('pointerup', stopTreeResize);
  storeTreeWidth(treeWidth.value);
  scheduleTableLayout();
}

function resizeTreeTo(clientX: number) {
  const rect = browserRef.value?.getBoundingClientRect();
  if (!rect) return;
  treeWidth.value = clampTreeWidth(clientX - rect.left);
  scheduleTableLayout();
}

function resetTreeWidth() {
  treeWidth.value = clampTreeWidth(DEFAULT_TREE_WIDTH);
  storeTreeWidth(treeWidth.value);
  scheduleTableLayout();
}

function nudgeTreeWidth(delta: number) {
  treeWidth.value = clampTreeWidth(treeWidth.value + delta);
  storeTreeWidth(treeWidth.value);
  scheduleTableLayout();
}

function handleWindowResize() {
  treeWidth.value = clampTreeWidth(treeWidth.value);
  scheduleTableLayout();
}

function clampTreeWidth(value: number) {
  const containerWidth = browserRef.value?.clientWidth ?? 0;
  const maxByContainer = containerWidth > 0
    ? Math.max(MIN_TREE_WIDTH, containerWidth - MIN_TABLE_WIDTH - 10)
    : MAX_TREE_WIDTH;
  const maxWidth = Math.min(MAX_TREE_WIDTH, maxByContainer);
  return Math.round(Math.min(Math.max(value, MIN_TREE_WIDTH), maxWidth));
}

function scheduleTableLayout() {
  if (tableLayoutFrame) {
    window.cancelAnimationFrame(tableLayoutFrame);
  }
  tableLayoutFrame = window.requestAnimationFrame(() => {
    void nextTick(() => {
      tableRef.value?.doLayout?.();
    });
  });
}

function readStoredTreeWidth() {
  const value = window.localStorage.getItem(TREE_WIDTH_KEY);
  const parsed = value ? Number(value) : DEFAULT_TREE_WIDTH;
  return Number.isFinite(parsed) ? parsed : DEFAULT_TREE_WIDTH;
}

function storeTreeWidth(value: number) {
  window.localStorage.setItem(TREE_WIDTH_KEY, String(value));
}

function reloadFiles() {
  pagination.page = 1;
  void loadFiles();
}

function resetAdvancedFilters() {
  filters.fileKind = 'ALL';
  filters.disciplineCode = '';
  filters.fileExt = '';
  filters.qualityIssue = 'ALL';
  reloadFiles();
}

function goParentDir() {
  if (!activeDir.value) return;
  const items = breadcrumbItems.value;
  if (items.length <= 1) {
    selectDir('');
    return;
  }
  selectDir(items[items.length - 2].path);
}

function handleRowCommand(command: string | number | object, row: CatalogFile) {
  const action = String(command);
  if (action === 'preview') {
    emit('open-preview', row.fileId);
  } else if (action === 'detail') {
    emit('open-detail', row.fileId);
  } else if (action === 'metadata') {
    emit('open-metadata', row.fileId);
  } else if (action === 'checksum') {
    emit('create-checksum', row.fileId);
  }
}

function normalizeExt(value: string) {
  const next = value.trim();
  if (!next) return undefined;
  return next.startsWith('.') ? next : `.${next}`;
}

function normalizeDirectoryPath(path: string) {
  return path.trim().replace(/\/+$/, '');
}

function splitPath(path: string) {
  return normalizeDirectoryPath(path).split('/').filter(Boolean);
}

function joinPath(parts: string[], hasLeadingSlash: boolean) {
  if (!parts.length) return '';
  const next = parts.join('/');
  return hasLeadingSlash ? `/${next}` : next;
}

function fileKindTag(value: string) {
  if (value === 'MODEL') return 'success';
  if (value === 'DRAWING') return 'primary';
  return 'info';
}

function disciplineLabel(code: string | null | undefined) {
  if (!code) return '-';
  const found = props.disciplineOptions.find((item) => item.code === code);
  return found?.name ?? code;
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
.file-browser {
  display: grid;
  grid-template-columns: var(--tree-width) 10px minmax(0, 1fr);
  align-items: stretch;
  gap: 10px;
  min-height: 420px;
}

.file-browser__tree-pane {
  min-width: 0;
}

.file-browser__tree-error {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 10px;
  background: #fff;
}

.file-browser__tree-error p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  text-align: center;
  line-height: 1.6;
}

.file-browser__resize-handle {
  position: relative;
  min-height: 100%;
  border-radius: 8px;
  cursor: col-resize;
  outline: none;
}

.file-browser__resize-handle::before {
  position: absolute;
  inset: 10px 3px;
  content: '';
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.28);
  transition: background 0.15s ease, transform 0.15s ease;
}

.file-browser__resize-handle:hover::before,
.file-browser__resize-handle:focus-visible::before,
.file-browser.is-resizing .file-browser__resize-handle::before {
  background: rgba(37, 99, 235, 0.55);
  transform: scaleX(1.35);
}

.file-browser__table {
  min-width: 0;
}

.file-browser__actionbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.file-browser__safe-actions,
.file-browser__search {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.file-browser__safe-actions > span {
  display: inline-flex;
}

.file-browser__search {
  min-width: min(100%, 520px);
  justify-content: flex-end;
}

.file-browser__search .el-input {
  width: 240px;
}

.file-browser__chevron {
  margin-left: 4px;
  transition: transform 0.15s ease;
}

.file-browser__chevron.is-open {
  transform: rotate(180deg);
}

.file-browser__advanced {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 8px;
  background: #f8fafc;
}

.file-browser__checksum-alert {
  margin-bottom: 12px;
}

.file-browser__checksum-alert :deep(.el-alert__content) {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 0;
}

.file-browser__breadcrumb {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
  margin-bottom: 12px;
  padding: 10px 12px;
  overflow-x: auto;
  border-radius: 8px;
  background: #eff6ff;
  color: #475569;
  white-space: nowrap;
}

.file-browser__breadcrumb button {
  flex: 0 0 auto;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 13px;
}

.file-browser__breadcrumb button:disabled {
  color: #94a3b8;
  cursor: not-allowed;
}

.file-browser__breadcrumb button.is-active {
  color: #1d4ed8;
  font-weight: 700;
}

.file-browser__name-cell {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  min-width: 0;
}

.file-browser__name-cell .el-icon {
  color: #64748b;
}

.file-browser__name-cell strong,
.file-browser__name-cell span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-browser__name-cell strong {
  color: #0f172a;
  font-size: 13px;
}

.file-browser__name-cell span {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.file-browser__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 900px) {
  .file-browser {
    grid-template-columns: 1fr;
  }

  .file-browser__resize-handle {
    display: none;
  }

  .file-browser__search {
    justify-content: flex-start;
  }

  .file-browser__advanced {
    grid-template-columns: 1fr;
  }
}
</style>
