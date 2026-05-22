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
        :expanded-paths="expandedDirs"
        :root-label="rootLabel"
        :loading="dirLoading"
        empty-description="暂无可浏览目录"
        @select="selectDir"
        @enter="enterDir"
        @toggle-expand="toggleExpandedDir"
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
          <input ref="uploadInputRef" class="file-browser__upload-input" type="file" @change="handleUploadFilePicked" />
          <el-tooltip :content="nasWriteActionTip" placement="top">
            <span>
              <el-button type="primary" :disabled="!canWriteNas || nasBusy" :loading="uploadingNasFile" :icon="Upload" @click="openUploadPicker">
                上传文件
              </el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="nasWriteActionTip" placement="top">
            <span>
              <el-button :disabled="!canWriteNas || nasBusy" :icon="FolderAdd" @click="createDirectoryAction">新建文件夹</el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="activeDirectoryActionTip" placement="top">
            <span>
              <el-button :disabled="!canOperateActiveDirectory || nasBusy" @click="renameActiveDirectory">重命名当前文件夹</el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="activeDirectoryActionTip" placement="top">
            <span>
              <el-button :disabled="!canOperateActiveDirectory || nasBusy" @click="moveActiveDirectory">移动当前文件夹</el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="quarantineActionTip" placement="top">
            <span>
              <el-button type="danger" plain :disabled="!canQuarantineActiveDirectory || nasBusy" @click="quarantineActiveDirectory">
                删除到隔离区
              </el-button>
            </span>
          </el-tooltip>
          <el-button :loading="quarantineLoading" @click="openQuarantineDrawer">隔离区</el-button>
          <el-button :loading="operationsLoading" @click="openOperationsDrawer">操作记录</el-button>
        </div>
        <div class="file-browser__search">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索文件名或平台文件ID"
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
          <el-button text @click="diagnosticInfoVisible = !diagnosticInfoVisible">
            {{ diagnosticInfoVisible ? '收起技术信息' : '技术信息' }}
          </el-button>
        </div>
      </div>

      <el-alert
        class="file-browser__trial-alert"
        :type="nasTrialAlertType"
        show-icon
        :closable="false"
      >
        <template #title>
          <strong>{{ nasTrialTitle }}</strong>
          <span>{{ nasTrialSummary }}</span>
        </template>
      </el-alert>

      <div class="file-browser__continuity" data-m1e-continuity-bar>
        <div>
          <strong>{{ continuityTitle }}</strong>
          <span>{{ continuitySummary }}</span>
        </div>
        <div class="file-browser__continuity-actions">
          <el-button v-if="lastFileId" size="small" @click="openLastFile">打开最近文件</el-button>
          <el-button size="small" @click="resetViewState">重置视图</el-button>
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
        <el-table-column label="文件名" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="file-browser__name-cell">
              <el-icon><Document /></el-icon>
              <div>
                <strong>{{ row.fileName }}</strong>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileKind" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="fileKindTag(row.fileKind)">{{ row.fileKind }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="90" />
        <el-table-column label="大小" width="120" align="right">
          <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
        </el-table-column>
        <el-table-column label="专业" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag type="info">{{ row.disciplineName || disciplineLabel(row.disciplineCode) || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <div class="file-browser__status-cell">
              <el-tag v-if="!row.qualityFlags || row.qualityFlags.length === 0" type="success" size="small">正常</el-tag>
              <el-tag v-else type="warning" size="small">{{ row.qualityFlags.length }} 项待处理</el-tag>
              <span>{{ row.confidenceLevel === 'HIGH' ? '高置信度' : '需复核' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="diagnosticInfoVisible" label="技术信息 / 诊断" min-width="260">
          <template #default="{ row }">
            <div class="file-browser__diagnostic-cell">
              <span>平台文件ID：{{ row.fileId }}</span>
              <span>扩展名：{{ row.fileExt || '-' }}</span>
              <span>置信度：{{ row.confidenceLevel ?? '-' }}</span>
              <span>更新时间：{{ formatDate(row.updatedAt) }}</span>
            </div>
          </template>
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
                  <el-dropdown-item :disabled="!canWriteNas || nasBusy" command="rename-file" divided>重命名</el-dropdown-item>
                  <el-dropdown-item :disabled="!canWriteNas || nasBusy" command="move-file">移动</el-dropdown-item>
                  <el-dropdown-item :disabled="!canAdminNas || nasBusy" command="quarantine-file">删除到隔离区</el-dropdown-item>
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

    <el-drawer v-model="operationsDrawerVisible" title="真实 NAS 操作记录" size="520px" @open="loadNasOperations">
      <el-alert
        class="file-browser__drawer-alert"
        type="info"
        show-icon
        :closable="false"
        title="这里仅展示受控操作记录，不展示真实 NAS 绝对路径。"
      />
      <div v-loading="operationsLoading" class="file-browser__drawer-list">
        <article v-for="item in nasOperations" :key="item.operationId" class="file-browser__drawer-item">
          <strong>{{ operationLabel(item.operationType) }} / {{ item.status }}</strong>
          <span>{{ item.targetDisplayPath || item.sourceDisplayPath || '项目根目录' }}</span>
          <em>操作编号 {{ item.operationId }} / traceId {{ item.traceId || '-' }} / {{ formatDate(item.createdAt) }}</em>
        </article>
        <el-empty v-if="!operationsLoading && nasOperations.length === 0" description="暂无受控 NAS 操作记录" :image-size="64" />
      </div>
    </el-drawer>

    <el-drawer v-model="quarantineDrawerVisible" title="隔离区" size="560px" @open="loadNasQuarantine">
      <el-alert
        class="file-browser__drawer-alert"
        type="warning"
        show-icon
        :closable="false"
        title="隔离区支持恢复，不提供永久删除；列表不展示真实 NAS 绝对路径。"
      />
      <div v-loading="quarantineLoading" class="file-browser__drawer-list">
        <article v-for="item in nasQuarantine" :key="item.quarantineRecordId" class="file-browser__drawer-item">
          <div>
            <strong>{{ item.displayName }} / {{ item.targetType }}</strong>
            <span>{{ item.originalDisplayPath }}</span>
            <em>隔离编号 {{ item.quarantineRecordId }} / {{ formatDate(item.createdAt) }}</em>
          </div>
          <el-button
            size="small"
            type="primary"
            :disabled="item.status !== 'QUARANTINED' || !canAdminNasProjectTrial || nasBusy"
            @click="restoreQuarantineItem(item.quarantineRecordId)"
          >
            恢复
          </el-button>
        </article>
        <el-empty v-if="!quarantineLoading && nasQuarantine.length === 0" description="暂无隔离项" :image-size="64" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Document, FolderAdd, MoreFilled, Search, Upload } from '@element-plus/icons-vue';
import { useRoute, useRouter } from 'vue-router';

import {
  createNasDirectory,
  fetchCatalogDirectories,
  fetchCatalogFiles,
  fetchNasWriteTrialStatus,
  fetchNasOperations,
  fetchNasQuarantine,
  moveNasDirectory,
  moveNasFile,
  quarantineNasDirectory,
  quarantineNasFile,
  renameNasDirectory,
  renameNasFile,
  restoreNasQuarantine,
  uploadNasFile,
  type CatalogDirectory,
  type CatalogFile,
  type AssetDiscipline,
  type NasOperationRecord,
  type NasQuarantineRecord,
  type NasWriteTrialStatus
} from '@/modules/data-steward/api/dataSteward';
import DirectoryTreePanel from '@/modules/data-steward/components/DirectoryTreePanel.vue';
import { useAuthStore } from '@/stores/auth';

const props = defineProps<{
  projectId: number;
  rootLabel: string;
  disciplineOptions: AssetDiscipline[];
  initialQualityIssue?: string;
  batchChecksumCreating?: boolean;
  active?: boolean;
}>();

const emit = defineEmits<{
  'open-preview': [fileId: number];
  'open-detail': [fileId: number];
  'open-metadata': [fileId: number];
  'create-checksum': [fileId: number];
  'create-batch-checksum': [];
}>();

const TREE_WIDTH_KEY = 'delivery.dataSteward.fileBrowser.treeWidth';
const STATE_KEY_PREFIX = 'delivery.dataSteward.fileBrowser.state';
const DEFAULT_TREE_WIDTH = 320;
const MIN_TREE_WIDTH = 240;
const MIN_TABLE_WIDTH = 560;
const MAX_TREE_WIDTH = 640;
const DEFAULT_PAGE_SIZE = 50;
const PAGE_SIZE_OPTIONS = new Set([20, 50, 100, 200]);
const QUERY_KEYS = [
  'tab',
  'fileDir',
  'fileKeyword',
  'fileKind',
  'discipline',
  'fileExt',
  'qualityIssue',
  'filePage',
  'filePageSize',
  'lastFileId'
];

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const browserRef = ref<HTMLElement | null>(null);
const tableRef = ref<{ doLayout?: () => void } | null>(null);
const uploadInputRef = ref<HTMLInputElement | null>(null);
const directories = ref<CatalogDirectory[]>([]);
const files = ref<CatalogFile[]>([]);
const nasOperations = ref<NasOperationRecord[]>([]);
const nasQuarantine = ref<NasQuarantineRecord[]>([]);
const nasTrialStatus = ref<NasWriteTrialStatus | null>(null);
const activeDir = ref('');
const expandedDirs = ref<string[]>([]);
const lastFileId = ref<number | null>(null);
const lastFileName = ref('');
const fileLoading = ref(false);
const dirLoading = ref(false);
const dirLoadFailed = ref(false);
const nasBusy = ref(false);
const uploadingNasFile = ref(false);
const operationsDrawerVisible = ref(false);
const quarantineDrawerVisible = ref(false);
const operationsLoading = ref(false);
const quarantineLoading = ref(false);
const nasTrialLoading = ref(false);
const nasTrialLoadFailed = ref(false);
const advancedSearchVisible = ref(false);
const diagnosticInfoVisible = ref(false);
const treeWidth = ref(DEFAULT_TREE_WIDTH);
const resizingTree = ref(false);
let resizePointerId: number | null = null;
let tableLayoutFrame = 0;
let directoryRequestId = 0;
let fileRequestId = 0;
let applyingSavedState = false;
let stateReady = false;

const filters = reactive({
  keyword: '',
  fileKind: 'ALL',
  disciplineCode: '',
  fileExt: '',
  qualityIssue: props.initialQualityIssue || 'ALL'
});

const pagination = reactive({
  page: 1,
  pageSize: DEFAULT_PAGE_SIZE,
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

const qualityIssueOptions = [
  { label: '全部质量', value: 'ALL' },
  { label: '缺 checksum', value: 'MISSING_CHECKSUM' },
  { label: '缺置信度', value: 'MISSING_CONFIDENCE' },
  { label: '专业待完善', value: 'MISSING_DISCIPLINE' },
  { label: '版本缺失', value: 'MISSING_VERSION' },
  { label: '路径缺失', value: 'MISSING_STORAGE_PATH' },
  { label: '零大小', value: 'ZERO_SIZE_FILE' }
];

type FileBrowserState = {
  activeDir?: string;
  keyword?: string;
  fileKind?: string;
  disciplineCode?: string;
  fileExt?: string;
  qualityIssue?: string;
  page?: number;
  pageSize?: number;
  expandedDirs?: string[];
  lastFileId?: number | null;
  lastFileName?: string;
  updatedAt?: string;
};

const fileBrowserStyle = computed(() => ({
  '--tree-width': `${treeWidth.value}px`
}));

const currentProjectRole = computed(() => {
  const current = authStore.currentUser?.projects.find((project) => project.id === props.projectId);
  return current?.roleCode ?? authStore.currentUser?.currentProject?.roleCode ?? '';
});

const hasNasWriteRole = computed(() => ['DELIVERY_ENGINEER', 'PROJECT_ADMIN'].includes(currentProjectRole.value));
const canWriteNas = computed(() => hasNasWriteRole.value && Boolean(nasTrialStatus.value?.canWrite));
const canAdminNasProjectTrial = computed(() => currentProjectRole.value === 'PROJECT_ADMIN'
  && Boolean(nasTrialStatus.value?.enabled)
  && Boolean(nasTrialStatus.value?.roleAllowed)
  && Boolean(nasTrialStatus.value?.accountAllowed));
const canAdminNas = computed(() => currentProjectRole.value === 'PROJECT_ADMIN' && Boolean(nasTrialStatus.value?.canWrite));
const canOperateActiveDirectory = computed(() => canWriteNas.value && Boolean(activeDir.value));
const canQuarantineActiveDirectory = computed(() => canAdminNas.value && Boolean(activeDir.value));

const nasWriteActionTip = computed(() => {
  if (nasTrialLoading.value) return '正在读取真实 NAS 写入灰度状态。';
  if (canWriteNas.value) return '当前目录已开启真实 NAS 写入灰度，平台会做权限、路径和审计校验。';
  if (!hasNasWriteRole.value) return '当前项目角色只能查看，不能操作公司 NAS 文件。';
  return nasTrialStatus.value?.disabledReason || '当前目录暂不可执行真实 NAS 写操作。';
});

const activeDirectoryActionTip = computed(() => {
  if (!canWriteNas.value) return nasWriteActionTip.value;
  if (!activeDir.value) return '请先在左侧选择一个项目内文件夹。';
  return '将直接操作当前文件夹，平台会校验路径不越出项目。';
});

const quarantineActionTip = computed(() => {
  if (currentProjectRole.value !== 'PROJECT_ADMIN') return '删除到隔离区和恢复仅限项目管理员。';
  if (!canAdminNas.value) return nasTrialStatus.value?.disabledReason || '当前目录暂不可执行真实 NAS 写操作。';
  if (!activeDir.value) return '请先选择要隔离的文件夹。';
  return '删除只会移入隔离区，不提供永久删除。';
});

const nasTrialAlertType = computed(() => {
  if (nasTrialLoadFailed.value) return 'error';
  if (!nasTrialStatus.value?.enabled) return 'info';
  return canWriteNas.value ? 'warning' : 'info';
});

const nasTrialTitle = computed(() => {
  if (nasTrialLoadFailed.value) return '真实 NAS 写入灰度状态加载失败';
  if (!nasTrialStatus.value?.enabled) return '真实 NAS 写入灰度未开启';
  return '真实 NAS 写入灰度已开启';
});

const nasTrialSummary = computed(() => {
  if (nasTrialLoadFailed.value) return '为避免误操作，当前页面已禁用真实 NAS 写按钮；请稍后刷新。';
  if (!nasTrialStatus.value) return '正在确认当前项目的灰度开关、可写目录和账号边界。';
  const roots = nasTrialStatus.value.allowedRelativeRoots.length
    ? nasTrialStatus.value.allowedRelativeRoots.map(formatAllowedRoot).join('、')
    : '未配置可写目录';
  const roles = nasTrialStatus.value.allowedRoleCodes.join('、') || '未配置角色';
  const reason = nasTrialStatus.value.canWrite ? '当前目录允许操作。' : nasTrialStatus.value.disabledReason;
  return `可写范围：${roots}；允许角色：${roles}；${reason}`;
});

const continuityTitle = computed(() => lastFileId.value ? '已恢复项目文件管理位置' : '文件管理会记住本项目位置');
const continuitySummary = computed(() => {
  const parts = [
    activeDir.value ? `目录：${pathLeaf(activeDir.value)}` : '目录：项目根目录',
    filters.keyword.trim() ? `关键词：${filters.keyword.trim()}` : '',
    filters.fileKind !== 'ALL' ? `类型：${fileKindLabel(filters.fileKind)}` : '',
    filters.disciplineCode ? `专业：${disciplineLabel(filters.disciplineCode)}` : '',
    filters.fileExt.trim() ? `扩展名：${normalizeExt(filters.fileExt)}` : '',
    filters.qualityIssue !== 'ALL' ? `质量：${qualityIssueLabel(filters.qualityIssue)}` : '',
    `第 ${pagination.page} 页`,
    lastFileId.value ? `最近文件：${lastFileName.value || `平台文件ID ${lastFileId.value}`}` : ''
  ].filter(Boolean);
  return `${parts.join(' / ')}。可重置视图，不会修改 NAS 文件。`;
});

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
    initializeBrowserState();
    void loadNasWriteTrialStatus();
    void loadDirectories();
    void loadFiles();
  },
  { immediate: true }
);

watch(
  () => [props.projectId, activeDir.value] as const,
  () => {
    void loadNasWriteTrialStatus();
  }
);

watch(
  () => props.active,
  (active) => {
    if (active) {
      persistBrowserState(true);
    }
  }
);

watch(
  () => [
    activeDir.value,
    filters.keyword,
    filters.fileKind,
    filters.disciplineCode,
    filters.fileExt,
    filters.qualityIssue,
    pagination.page,
    pagination.pageSize,
    expandedDirs.value.join('|'),
    lastFileId.value,
    lastFileName.value
  ],
  () => {
    if (!stateReady || applyingSavedState) return;
    persistBrowserState();
  }
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

function initializeBrowserState() {
  applyingSavedState = true;
  stateReady = false;
  files.value = [];
  pagination.total = 0;

  const state = readQueryState() ?? readStoredBrowserState() ?? {};
  const fallbackQualityIssue = props.initialQualityIssue || 'ALL';
  activeDir.value = state.activeDir ?? '';
  filters.keyword = state.keyword ?? '';
  filters.fileKind = normalizeFileKind(state.fileKind);
  filters.disciplineCode = state.disciplineCode ?? '';
  filters.fileExt = state.fileExt ?? '';
  filters.qualityIssue = state.qualityIssue ?? fallbackQualityIssue;
  pagination.page = positiveNumber(state.page, 1);
  pagination.pageSize = normalizePageSize(state.pageSize);
  expandedDirs.value = Array.isArray(state.expandedDirs) ? state.expandedDirs.filter(Boolean) : [];
  lastFileId.value = state.lastFileId && Number.isFinite(Number(state.lastFileId)) ? Number(state.lastFileId) : null;
  lastFileName.value = state.lastFileName ?? '';
  advancedSearchVisible.value = hasAdvancedFilters();

  applyingSavedState = false;
  stateReady = true;
  persistBrowserState();
}

function readQueryState(): FileBrowserState | null {
  const hasQueryState = QUERY_KEYS.some((key) => key !== 'tab' && route.query[key] !== undefined);
  if (!hasQueryState) return null;
  return {
    activeDir: queryString(route.query.fileDir) ?? '',
    keyword: queryString(route.query.fileKeyword) ?? '',
    fileKind: queryString(route.query.fileKind) ?? 'ALL',
    disciplineCode: queryString(route.query.discipline) ?? '',
    fileExt: queryString(route.query.fileExt) ?? '',
    qualityIssue: queryString(route.query.qualityIssue) ?? props.initialQualityIssue ?? 'ALL',
    page: positiveNumber(queryString(route.query.filePage), 1),
    pageSize: normalizePageSize(positiveNumber(queryString(route.query.filePageSize), DEFAULT_PAGE_SIZE)),
    lastFileId: positiveNumber(queryString(route.query.lastFileId), 0) || null
  };
}

function readStoredBrowserState(): FileBrowserState | null {
  try {
    const raw = window.localStorage.getItem(projectStateKey());
    if (!raw) return null;
    const parsed = JSON.parse(raw) as FileBrowserState;
    return typeof parsed === 'object' && parsed !== null ? parsed : null;
  } catch {
    return null;
  }
}

function persistBrowserState(forceRouteSync = false) {
  if (!Number.isFinite(props.projectId)) return;
  const state = currentBrowserState();
  window.localStorage.setItem(projectStateKey(), JSON.stringify(state));
  if (props.active || forceRouteSync) {
    syncBrowserStateToRoute(state);
  }
}

function currentBrowserState(): FileBrowserState {
  return {
    activeDir: activeDir.value,
    keyword: filters.keyword.trim(),
    fileKind: filters.fileKind,
    disciplineCode: filters.disciplineCode,
    fileExt: filters.fileExt.trim(),
    qualityIssue: filters.qualityIssue,
    page: pagination.page,
    pageSize: pagination.pageSize,
    expandedDirs: expandedDirs.value,
    lastFileId: lastFileId.value,
    lastFileName: lastFileName.value,
    updatedAt: new Date().toISOString()
  };
}

function syncBrowserStateToRoute(state: FileBrowserState) {
  const nextQuery: Record<string, string> = {};
  for (const [key, value] of Object.entries(route.query)) {
    if (!QUERY_KEYS.includes(key) && typeof value === 'string') {
      nextQuery[key] = value;
    }
  }
  nextQuery.tab = 'files';
  assignQuery(nextQuery, 'fileDir', state.activeDir);
  assignQuery(nextQuery, 'fileKeyword', state.keyword);
  assignQuery(nextQuery, 'fileKind', state.fileKind === 'ALL' ? '' : state.fileKind);
  assignQuery(nextQuery, 'discipline', state.disciplineCode);
  assignQuery(nextQuery, 'fileExt', state.fileExt);
  assignQuery(nextQuery, 'qualityIssue', state.qualityIssue === 'ALL' ? '' : state.qualityIssue);
  assignQuery(nextQuery, 'filePage', state.page && state.page > 1 ? String(state.page) : '');
  assignQuery(nextQuery, 'filePageSize', state.pageSize && state.pageSize !== DEFAULT_PAGE_SIZE ? String(state.pageSize) : '');
  assignQuery(nextQuery, 'lastFileId', state.lastFileId ? String(state.lastFileId) : '');

  if (isSameQuery(nextQuery, route.query)) return;
  void router.replace({ path: route.path, query: nextQuery });
}

function resetViewState() {
  activeDir.value = '';
  filters.keyword = '';
  filters.fileKind = 'ALL';
  filters.disciplineCode = '';
  filters.fileExt = '';
  filters.qualityIssue = 'ALL';
  pagination.page = 1;
  pagination.pageSize = DEFAULT_PAGE_SIZE;
  pagination.total = 0;
  expandedDirs.value = [];
  lastFileId.value = null;
  lastFileName.value = '';
  advancedSearchVisible.value = false;
  window.localStorage.removeItem(projectStateKey());
  persistBrowserState(true);
  void loadFiles();
  ElMessage.success('文件管理视图已重置');
}

async function loadNasWriteTrialStatus() {
  if (!Number.isFinite(props.projectId)) return;
  nasTrialLoading.value = true;
  nasTrialLoadFailed.value = false;
  try {
    nasTrialStatus.value = await fetchNasWriteTrialStatus(props.projectId, activeDir.value);
  } catch (error) {
    nasTrialStatus.value = null;
    nasTrialLoadFailed.value = true;
    ElMessage.error(error instanceof Error ? error.message : '真实 NAS 写入灰度状态加载失败');
  } finally {
    nasTrialLoading.value = false;
  }
}

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

async function runNasOperation(action: () => Promise<{ operationId: number; message: string; traceId: string }>) {
  if (nasBusy.value) return false;
  nasBusy.value = true;
  try {
    const result = await action();
    ElMessage.success(`${result.message}。操作编号 ${result.operationId}，traceId ${result.traceId}`);
    await Promise.all([loadNasWriteTrialStatus(), loadDirectories(), loadFiles(), loadNasOperations(false), loadNasQuarantine(false)]);
    return true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '真实 NAS 操作失败，平台未执行永久删除');
    return false;
  } finally {
    nasBusy.value = false;
    uploadingNasFile.value = false;
  }
}

async function confirmNasOperation(actionLabel: string, targetLabel: string) {
  await ElMessageBox.confirm(
    `将直接操作公司 NAS 文件：${actionLabel}。项目：${props.rootLabel}。目标：${targetLabel || '项目根目录'}。平台不会读取文件正文，不会永久删除，也不会展示真实 NAS 绝对路径。`,
    '确认真实 NAS 操作',
    {
      type: 'warning',
      confirmButtonText: '确认执行',
      cancelButtonText: '取消'
    }
  );
}

async function createDirectoryAction() {
  if (!canWriteNas.value) return;
  const { value } = await ElMessageBox.prompt('请输入新文件夹名称', '新建文件夹', {
    confirmButtonText: '确认创建',
    cancelButtonText: '取消',
    inputPattern: /^(?!\.{1,2}$)[^/\\:]+$/,
    inputErrorMessage: '名称不能包含路径分隔符或特殊符号'
  });
  await confirmNasOperation('新建文件夹', activeDir.value || '项目根目录');
  await runNasOperation(() => createNasDirectory(props.projectId, { parentPath: activeDir.value, name: value }));
}

function openUploadPicker() {
  if (!canWriteNas.value || nasBusy.value) return;
  uploadInputRef.value?.click();
}

async function handleUploadFilePicked(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  uploadingNasFile.value = true;
  try {
    await confirmNasOperation('上传文件', activeDir.value || '项目根目录');
    await runNasOperation(() => uploadNasFile(props.projectId, { parentPath: activeDir.value, file }));
  } catch {
    uploadingNasFile.value = false;
  }
}

async function renameActiveDirectory() {
  if (!canOperateActiveDirectory.value) return;
  const { value } = await ElMessageBox.prompt('请输入新的文件夹名称', '重命名当前文件夹', {
    inputValue: pathLeaf(activeDir.value),
    confirmButtonText: '确认重命名',
    cancelButtonText: '取消',
    inputPattern: /^(?!\.{1,2}$)[^/\\:]+$/,
    inputErrorMessage: '名称不能包含路径分隔符或特殊符号'
  });
  await confirmNasOperation('重命名文件夹', activeDir.value);
  const previousDir = activeDir.value;
  const ok = await runNasOperation(() => renameNasDirectory(props.projectId, { sourcePath: previousDir, newName: value }));
  if (ok) selectDir(parentPath(previousDir) ? `${parentPath(previousDir)}/${value}` : value);
}

async function moveActiveDirectory() {
  if (!canOperateActiveDirectory.value) return;
  const { value } = await ElMessageBox.prompt('请输入目标文件夹相对路径，留空表示项目根目录', '移动当前文件夹', {
    confirmButtonText: '确认移动',
    cancelButtonText: '取消'
  });
  await confirmNasOperation('移动文件夹', `${activeDir.value} -> ${value || '项目根目录'}`);
  const previousDir = activeDir.value;
  const ok = await runNasOperation(() => moveNasDirectory(props.projectId, { sourcePath: previousDir, targetDirectory: value }));
  if (ok) selectDir(value ? `${value}/${pathLeaf(previousDir)}` : pathLeaf(previousDir));
}

async function quarantineActiveDirectory() {
  if (!canQuarantineActiveDirectory.value) return;
  const { value } = await ElMessageBox.prompt('可填写隔离原因', '删除到隔离区', {
    confirmButtonText: '确认移入隔离区',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：误传资料，待管理员复核'
  });
  await confirmNasOperation('删除到隔离区', activeDir.value);
  const previousDir = activeDir.value;
  const ok = await runNasOperation(() => quarantineNasDirectory(props.projectId, { sourcePath: previousDir, reason: value }));
  if (ok) selectDir(parentPath(previousDir));
}

async function renameFileAction(row: CatalogFile) {
  const { value } = await ElMessageBox.prompt('请输入新的文件名', '重命名文件', {
    inputValue: row.fileName,
    confirmButtonText: '确认重命名',
    cancelButtonText: '取消',
    inputPattern: /^(?!\.{1,2}$)[^/\\:]+$/,
    inputErrorMessage: '名称不能包含路径分隔符或特殊符号'
  });
  await confirmNasOperation('重命名文件', row.fileName);
  await runNasOperation(() => renameNasFile(props.projectId, row.fileId, value));
}

async function moveFileAction(row: CatalogFile) {
  const { value } = await ElMessageBox.prompt('请输入目标文件夹相对路径，留空表示项目根目录', '移动文件', {
    confirmButtonText: '确认移动',
    cancelButtonText: '取消'
  });
  await confirmNasOperation('移动文件', `${row.fileName} -> ${value || '项目根目录'}`);
  await runNasOperation(() => moveNasFile(props.projectId, row.fileId, value));
}

async function quarantineFileAction(row: CatalogFile) {
  const { value } = await ElMessageBox.prompt('可填写隔离原因', '删除文件到隔离区', {
    confirmButtonText: '确认移入隔离区',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：误传资料，待管理员复核'
  });
  await confirmNasOperation('删除文件到隔离区', row.fileName);
  await runNasOperation(() => quarantineNasFile(props.projectId, row.fileId, value));
}

async function openOperationsDrawer() {
  operationsDrawerVisible.value = true;
  await loadNasOperations();
}

async function openQuarantineDrawer() {
  quarantineDrawerVisible.value = true;
  await loadNasQuarantine();
}

async function loadNasOperations(showError = true) {
  if (!Number.isFinite(props.projectId)) return;
  operationsLoading.value = true;
  try {
    nasOperations.value = await fetchNasOperations(props.projectId, 50);
  } catch (error) {
    if (showError) ElMessage.error(error instanceof Error ? error.message : '操作记录加载失败');
  } finally {
    operationsLoading.value = false;
  }
}

async function loadNasQuarantine(showError = true) {
  if (!Number.isFinite(props.projectId)) return;
  quarantineLoading.value = true;
  try {
    nasQuarantine.value = await fetchNasQuarantine(props.projectId, undefined, 50);
  } catch (error) {
    if (showError) ElMessage.error(error instanceof Error ? error.message : '隔离区加载失败');
  } finally {
    quarantineLoading.value = false;
  }
}

async function restoreQuarantineItem(recordId: number) {
  await confirmNasOperation('恢复隔离项', `隔离编号 ${recordId}`);
  await runNasOperation(() => restoreNasQuarantine(props.projectId, recordId));
}

function selectDir(dirPath: string) {
  activeDir.value = dirPath;
  rememberExpandedAncestors(dirPath);
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

function toggleExpandedDir(path: string, expanded: boolean) {
  const next = new Set(expandedDirs.value);
  if (expanded) {
    next.add(path);
  } else {
    next.delete(path);
  }
  expandedDirs.value = Array.from(next);
}

function rememberExpandedAncestors(path: string) {
  if (!path) return;
  const parts = splitPath(path);
  const hasLeadingSlash = path.startsWith('/');
  const ancestors = parts
    .map((_, index) => joinPath(parts.slice(0, index + 1), hasLeadingSlash))
    .filter(Boolean);
  expandedDirs.value = Array.from(new Set([...expandedDirs.value, ...ancestors]));
}

function rememberFile(row: CatalogFile) {
  lastFileId.value = row.fileId;
  lastFileName.value = row.fileName;
}

function openLastFile() {
  if (!lastFileId.value) return;
  emit('open-detail', lastFileId.value);
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
  rememberFile(row);
  if (action === 'preview') {
    emit('open-preview', row.fileId);
  } else if (action === 'detail') {
    emit('open-detail', row.fileId);
  } else if (action === 'metadata') {
    emit('open-metadata', row.fileId);
  } else if (action === 'checksum') {
    emit('create-checksum', row.fileId);
  } else if (action === 'rename-file') {
    void renameFileAction(row);
  } else if (action === 'move-file') {
    void moveFileAction(row);
  } else if (action === 'quarantine-file') {
    void quarantineFileAction(row);
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

function pathLeaf(path: string) {
  const parts = splitPath(path);
  return parts.at(-1) ?? '项目根目录';
}

function formatAllowedRoot(path: string) {
  return path ? path : '项目根目录';
}

function parentPath(path: string) {
  const parts = splitPath(path);
  return parts.length <= 1 ? '' : parts.slice(0, -1).join('/');
}

function operationLabel(value: string) {
  const labels: Record<string, string> = {
    DIRECTORY_CREATE: '新建文件夹',
    FILE_UPLOAD: '上传文件',
    FILE_RENAME: '重命名文件',
    FILE_MOVE: '移动文件',
    FILE_QUARANTINE: '删除文件到隔离区',
    DIRECTORY_RENAME: '重命名文件夹',
    DIRECTORY_MOVE: '移动文件夹',
    DIRECTORY_QUARANTINE: '删除文件夹到隔离区',
    QUARANTINE_RESTORE: '恢复隔离项'
  };
  return labels[value] ?? value;
}

function projectStateKey() {
  return `${STATE_KEY_PREFIX}.${props.projectId}`;
}

function assignQuery(query: Record<string, string>, key: string, value: string | number | null | undefined) {
  const next = value === undefined || value === null ? '' : String(value).trim();
  if (next) {
    query[key] = next;
  } else {
    delete query[key];
  }
}

function isSameQuery(next: Record<string, string>, current: Record<string, unknown>) {
  const currentNormalized: Record<string, string> = {};
  for (const [key, value] of Object.entries(current)) {
    if (Array.isArray(value)) {
      if (value[0] !== undefined) currentNormalized[key] = String(value[0]);
    } else if (value !== undefined && value !== null) {
      currentNormalized[key] = String(value);
    }
  }
  return JSON.stringify(next) === JSON.stringify(currentNormalized);
}

function hasAdvancedFilters() {
  return filters.fileKind !== 'ALL'
    || Boolean(filters.disciplineCode)
    || Boolean(filters.fileExt.trim())
    || filters.qualityIssue !== 'ALL';
}

function normalizeFileKind(value: string | undefined) {
  return fileKindOptions.some((item) => item.value === value) ? String(value) : 'ALL';
}

function normalizePageSize(value: number | string | undefined) {
  const parsed = positiveNumber(value, DEFAULT_PAGE_SIZE);
  return PAGE_SIZE_OPTIONS.has(parsed) ? parsed : DEFAULT_PAGE_SIZE;
}

function positiveNumber(value: number | string | undefined | null, fallback: number) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

function queryString(value: unknown) {
  if (Array.isArray(value)) return value[0] ? String(value[0]) : undefined;
  return value ? String(value) : undefined;
}

function fileKindTag(value: string) {
  if (value === 'MODEL') return 'success';
  if (value === 'DRAWING') return 'primary';
  return 'info';
}

function fileKindLabel(value: string) {
  return fileKindOptions.find((item) => item.value === value)?.label ?? value;
}

function disciplineLabel(code: string | null | undefined) {
  if (!code) return '-';
  const found = props.disciplineOptions.find((item) => item.code === code);
  return found?.name ?? code;
}

function qualityIssueLabel(value: string) {
  return qualityIssueOptions.find((item) => item.value === value)?.label ?? value;
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
  gap: var(--zy-sp-2);
  min-height: 420px;
}

.file-browser__tree-pane {
  min-width: 0;
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  overflow: hidden;
}

.file-browser__tree-error {
  display: flex;
  flex-direction: column;
  gap: var(--zy-sp-3);
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: var(--zy-sp-6);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
}

.file-browser__tree-error p {
  margin: 0;
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
  text-align: center;
  line-height: 1.65;
}

.file-browser__resize-handle {
  position: relative;
  min-height: 100%;
  border-radius: var(--zy-radius-base);
  cursor: col-resize;
  outline: none;
}

.file-browser__resize-handle::before {
  position: absolute;
  inset: 10px 3px;
  content: '';
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.24);
  transition:
    background var(--zy-duration-2) var(--zy-ease),
    transform var(--zy-duration-2) var(--zy-ease);
}

.file-browser__resize-handle:hover::before,
.file-browser__resize-handle:focus-visible::before,
.file-browser.is-resizing .file-browser__resize-handle::before {
  background: var(--zy-blue-500);
  transform: scaleX(1.35);
}

.file-browser__table {
  min-width: 0;
}

.file-browser__actionbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: var(--zy-sp-2);
  margin-bottom: var(--zy-sp-3);
}

.file-browser__safe-actions,
.file-browser__search {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  align-items: center;
}

.file-browser__safe-actions > span {
  display: inline-flex;
}

.file-browser__upload-input {
  display: none;
}

.file-browser__search {
  min-width: min(100%, 520px);
  justify-content: flex-end;
}

.file-browser__search .el-input {
  width: 240px;
}

.file-browser__trial-alert {
  margin-bottom: var(--zy-sp-3);
  border-radius: var(--zy-radius-base);
}

.file-browser__trial-alert :deep(.el-alert) {
  border: var(--zy-border-soft);
  border-left-width: 3px;
}

.file-browser__trial-alert :deep(.el-alert__title) {
  display: grid;
  gap: 4px;
  line-height: 1.55;
}

.file-browser__trial-alert strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.file-browser__trial-alert span {
  color: var(--zy-text-soft);
  font-size: var(--zy-fs-xs);
}

.file-browser__chevron {
  margin-left: 4px;
  transition: transform var(--zy-duration-2) var(--zy-ease);
}

.file-browser__chevron.is-open {
  transform: rotate(180deg);
}

.file-browser__advanced {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: var(--zy-sp-2);
  margin-bottom: var(--zy-sp-3);
  padding: var(--zy-sp-3);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
}

.file-browser__continuity {
  display: flex;
  gap: var(--zy-sp-3);
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--zy-sp-3);
  padding: var(--zy-sp-2) var(--zy-sp-3);
  border: 1px solid rgba(37, 99, 235, 0.18);
  border-left: 3px solid var(--zy-blue-500);
  border-radius: var(--zy-radius-base);
  background: var(--zy-blue-50);
}

.file-browser__continuity > div:first-child {
  min-width: 0;
}

.file-browser__continuity strong,
.file-browser__continuity span {
  display: block;
  min-width: 0;
}

.file-browser__continuity strong {
  color: #1e3a8a;
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.file-browser__continuity span {
  margin-top: 3px;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.55;
}

.file-browser__continuity-actions {
  display: inline-flex;
  flex: 0 0 auto;
  gap: var(--zy-sp-2);
}

.file-browser__checksum-alert {
  margin-bottom: var(--zy-sp-3);
}

.file-browser__checksum-alert :deep(.el-alert__content) {
  display: flex;
  gap: var(--zy-sp-3);
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-width: 0;
}

.file-browser__breadcrumb {
  display: flex;
  gap: var(--zy-sp-2);
  align-items: center;
  min-width: 0;
  margin-bottom: var(--zy-sp-3);
  padding: var(--zy-sp-2) var(--zy-sp-3);
  overflow-x: auto;
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
  color: var(--zy-text-soft);
  white-space: nowrap;
  font-family: var(--zy-font-mono);
}

.file-browser__breadcrumb button {
  flex: 0 0 auto;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-family: inherit;
  font-size: var(--zy-fs-xs);
  padding: 2px 6px;
  border-radius: var(--zy-radius-sm);
  transition: background var(--zy-duration-2) var(--zy-ease);
}

.file-browser__breadcrumb button:hover:not(:disabled) {
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
}

.file-browser__breadcrumb button:disabled {
  color: var(--zy-subtle);
  cursor: not-allowed;
}

.file-browser__breadcrumb button.is-active {
  background: var(--zy-blue-100);
  color: var(--zy-blue-700);
  font-weight: var(--zy-fw-bold);
}

.file-browser__breadcrumb > span {
  color: var(--zy-subtle);
  font-size: var(--zy-fs-xs);
}

.file-browser__name-cell {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--zy-sp-2);
  align-items: center;
  min-width: 0;
}

.file-browser__name-cell .el-icon {
  color: var(--zy-muted);
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
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-medium);
}

.file-browser__name-cell span {
  margin-top: 2px;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.file-browser__status-cell,
.file-browser__diagnostic-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.file-browser__status-cell span,
.file-browser__diagnostic-cell span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.45;
}

.file-browser__diagnostic-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-browser__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--zy-sp-3);
}

.file-browser__drawer-alert {
  margin-bottom: var(--zy-sp-3);
}

.file-browser__drawer-list {
  display: grid;
  gap: var(--zy-sp-2);
}

.file-browser__drawer-item {
  display: flex;
  gap: var(--zy-sp-3);
  align-items: center;
  justify-content: space-between;
  padding: var(--zy-sp-3);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
}

.file-browser__drawer-item > div {
  min-width: 0;
}

.file-browser__drawer-item strong,
.file-browser__drawer-item span,
.file-browser__drawer-item em {
  display: block;
  min-width: 0;
}

.file-browser__drawer-item strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.file-browser__drawer-item span {
  margin-top: 4px;
  overflow: hidden;
  color: var(--zy-text-soft);
  font-size: var(--zy-fs-xs);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-browser__drawer-item em {
  margin-top: 4px;
  color: var(--zy-subtle);
  font-size: var(--zy-fs-xs);
  font-style: normal;
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

  .file-browser__continuity {
    align-items: stretch;
    flex-direction: column;
  }

  .file-browser__continuity-actions {
    justify-content: flex-start;
  }
}
</style>
