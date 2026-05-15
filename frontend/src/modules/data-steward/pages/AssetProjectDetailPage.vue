<template>
  <section class="mvp-page asset-page">
    <div class="mvp-page__header">
      <div>
        <el-button text :icon="Back" @click="router.push({ name: 'data-steward-assets' })">返回资产总览</el-button>
        <h1>{{ projectTitle }}</h1>
        <p>{{ projectSubTitle }}</p>
      </div>
      <div class="mvp-page__actions">
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

    <el-tabs v-model="activeTab" class="asset-tabs">
      <el-tab-pane label="文件资产" name="files">
        <section class="asset-toolbar">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="文件名、路径"
            :prefix-icon="Search"
            @keyup.enter="reloadFilesFromFirstPage"
            @clear="reloadFilesFromFirstPage"
          />
          <el-segmented v-model="filters.fileKind" :options="fileKindOptions" @change="reloadFilesFromFirstPage" />
          <el-select v-model="filters.qualityIssue" @change="reloadFilesFromFirstPage">
            <el-option v-for="item in qualityIssueOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-select v-model="filters.discipline" clearable filterable placeholder="专业" @change="reloadFilesFromFirstPage">
            <el-option v-for="item in disciplineOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
          <el-input v-model="filters.fileExt" clearable placeholder="扩展名" @keyup.enter="reloadFilesFromFirstPage" @clear="reloadFilesFromFirstPage" />
          <el-button :icon="Search" @click="reloadFilesFromFirstPage">查询</el-button>
        </section>

        <el-table v-loading="fileLoading" :data="files" class="master-table" empty-text="暂无文件资产" @row-dblclick="openFileDetail">
          <el-table-column prop="fileId" label="文件ID" width="90" align="right" />
          <el-table-column prop="fileName" label="文件名称" min-width="260" show-overflow-tooltip />
          <el-table-column prop="fileKind" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="fileKindTag(row.fileKind)">{{ row.fileKind }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileExt" label="扩展名" width="90" />
          <el-table-column label="专业" width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag type="info">{{ disciplineLabel(row.discipline) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="versionNo" label="版本" width="90" />
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
              <el-tag v-if="qualityFlags(row).length === 0" type="success" size="small">正常</el-tag>
              <el-tag v-else type="warning" size="small">{{ qualityFlags(row).length }} 项</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column prop="storagePath" label="存储路径" min-width="280" show-overflow-tooltip />
          <el-table-column label="操作" width="310" fixed="right">
            <template #default="{ row }">
              <el-button text :icon="View" @click="openPreview(row)">预览</el-button>
              <el-button text @click="openFileDetail(row)">详情</el-button>
              <el-button text @click="openMetadataDialog(row)">治理</el-button>
              <el-button text :disabled="Boolean(row.checksum)" @click="createChecksum(row)">补 checksum</el-button>
              <el-button text :icon="CopyDocument" @click="copyPath(row.storagePath)">复制路径</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="asset-pagination">
          <el-pagination
            v-model:current-page="filePagination.pageNo"
            v-model:page-size="filePagination.pageSize"
            :page-sizes="[20, 50, 100, 200]"
            :total="filePagination.total"
            layout="total, sizes, prev, pager, next"
            @change="loadFiles"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="扫描任务" name="scans">
        <el-table v-loading="loading" :data="projectScans" class="master-table" empty-text="暂无扫描任务">
          <el-table-column prop="id" label="任务ID" width="90" />
          <el-table-column prop="rootCode" label="根编码" width="130" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="scanStatusTag(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="170">
            <template #default="{ row }">
              <el-progress :percentage="scanProgressValue(row)" :stroke-width="8" />
            </template>
          </el-table-column>
          <el-table-column label="扫描/入库/待审" width="160">
            <template #default="{ row }">
              {{ formatCount(row.totalScanned) }} / {{ formatCount(row.autoIngested) }} / {{ formatCount(row.pendingReview) }}
            </template>
          </el-table-column>
          <el-table-column prop="failureReason" label="失败原因" min-width="180" show-overflow-tooltip />
          <el-table-column prop="lastScannedPath" label="最后扫描路径" min-width="260" show-overflow-tooltip />
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="路径映射" name="mappings">
        <el-table v-loading="loading" :data="pathMappings" class="master-table" empty-text="暂无路径映射">
          <el-table-column prop="providerCode" label="存储" width="120" />
          <el-table-column prop="matchStrategy" label="匹配方式" width="130" />
          <el-table-column prop="nasPath" label="NAS路径" min-width="320" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="detailDrawerVisible" :title="detailTitle" size="640px">
      <template v-if="selectedFile">
        <section class="asset-detail-section">
          <h3>文件识别</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="文件ID">{{ selectedFile.fileId }}</el-descriptions-item>
            <el-descriptions-item label="文件名">{{ selectedFile.fileName }}</el-descriptions-item>
            <el-descriptions-item label="项目">{{ selectedFile.projectCode }} {{ selectedFile.projectName }}</el-descriptions-item>
            <el-descriptions-item label="文件类型">{{ selectedFile.fileKind }}</el-descriptions-item>
            <el-descriptions-item label="扩展名">{{ selectedFile.fileExt || '-' }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ disciplineLabel(selectedFile.discipline) }}</el-descriptions-item>
            <el-descriptions-item label="版本">{{ selectedFile.versionNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="大小">{{ formatBytes(selectedFile.sizeBytes) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="asset-detail-section">
          <h3>治理状态</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="质量问题">
              <template v-if="qualityFlags(selectedFile).length === 0">
                <el-tag type="success" size="small">无</el-tag>
              </template>
              <template v-else>
                <el-tag
                  v-for="flag in qualityFlags(selectedFile)"
                  :key="flag"
                  type="warning"
                  size="small"
                  class="quality-flag"
                >
                  {{ qualityFlagLabel(flag) }}
                </el-tag>
              </template>
            </el-descriptions-item>
            <el-descriptions-item label="checksum">
              <span v-if="selectedFile.checksum" class="mono-text">{{ selectedFile.checksum }}</span>
              <el-tag v-else type="warning" size="small">缺失</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="处理状态">{{ selectedFile.processStatus }}</el-descriptions-item>
            <el-descriptions-item label="审核状态">{{ selectedFile.reviewStatus }}</el-descriptions-item>
            <el-descriptions-item label="置信度">{{ selectedFile.confidenceLevel || '-' }}</el-descriptions-item>
            <el-descriptions-item label="生命周期">{{ selectedFile.lifecycleStatus || '-' }}</el-descriptions-item>
            <el-descriptions-item label="索引建议">{{ selectedFile.indexEligibility || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section v-loading="previewLoading && !currentPreview" class="asset-detail-section">
          <h3>预览能力</h3>
          <template v-if="currentPreview">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="预览状态">
                <el-tag :type="previewStatusTag(currentPreview.previewStatus)">
                  {{ previewStatusLabel(currentPreview.previewStatus) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="预览方式">{{ previewModeLabel(currentPreview.previewMode) }}</el-descriptions-item>
              <el-descriptions-item label="转换状态">{{ conversionStatusLabel(currentPreview.conversionStatus) }}</el-descriptions-item>
            <el-descriptions-item label="后续处理">
                {{ currentPreview.conversionRequired ? '需要接入转换服务' : '暂不需要转换' }}
              </el-descriptions-item>
              <el-descriptions-item label="预览权限">
                <el-tag :type="currentPreview.previewAllowed ? 'success' : 'info'" size="small">
                  {{ currentPreview.previewAllowed ? '允许预览' : '不可预览' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="下载权限">
                <el-tag :type="currentPreview.downloadAllowed ? 'success' : 'info'" size="small">
                  {{ currentPreview.downloadAllowed ? '允许下载' : '不可下载' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
            <el-alert
              class="preview-message"
              :title="currentPreview.accessPolicyMessage || currentPreview.message"
              :type="currentPreview.previewAvailable ? 'success' : 'info'"
              show-icon
              :closable="false"
            />
          </template>
          <el-empty v-else description="尚未加载预览状态" :image-size="44" />
        </section>

        <section class="asset-detail-section">
          <h3>来源与路径</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="来源类型">{{ selectedFile.sourceType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="存储提供方">{{ selectedFile.storageProvider }}</el-descriptions-item>
            <el-descriptions-item label="逻辑路径">{{ selectedFile.logicalPath || '-' }}</el-descriptions-item>
            <el-descriptions-item label="存储路径">
              <span class="mono-text">{{ selectedFile.storagePath || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(selectedFile.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDate(selectedFile.updatedAt) }}</el-descriptions-item>
            <el-descriptions-item label="最近验证">{{ formatDate(selectedFile.lastSeenAt) }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="asset-detail-actions">
          <el-button type="primary" @click="openMetadataDialog(selectedFile)">人工治理</el-button>
          <el-button :icon="View" :loading="previewLoading" @click="openPreview(selectedFile)">查看预览状态</el-button>
          <el-button
            type="success"
            :disabled="!canOpenPreview(currentPreview)"
            :loading="accessActionLoading === 'PREVIEW'"
            @click="openFileAccess('PREVIEW')"
          >
            打开预览
          </el-button>
          <el-button
            :disabled="!currentPreview?.downloadAllowed"
            :loading="accessActionLoading === 'DOWNLOAD'"
            @click="openFileAccess('DOWNLOAD')"
          >
            下载文件
          </el-button>
          <el-button :disabled="Boolean(selectedFile.checksum)" @click="createChecksum(selectedFile)">创建 checksum 任务</el-button>
          <el-button :icon="CopyDocument" @click="copyPath(selectedFile.storagePath)">复制路径</el-button>
        </section>
      </template>
    </el-drawer>

    <el-dialog v-model="previewDialogVisible" title="文件预览状态" width="640px">
      <div v-loading="previewLoading" class="preview-dialog-body">
        <template v-if="selectedPreview">
          <div class="preview-state-panel">
            <el-tag :type="previewStatusTag(selectedPreview.previewStatus)" size="large">
              {{ previewStatusLabel(selectedPreview.previewStatus) }}
            </el-tag>
            <div>
              <strong>{{ selectedPreview.fileName }}</strong>
              <span>{{ selectedPreview.projectCode }} {{ selectedPreview.projectName }}</span>
            </div>
          </div>
          <el-alert
            title="一期只提供预览入口和状态判断，不读取文件正文，也不执行真实模型轻量化或 Office/CAD 转换。"
            type="info"
            show-icon
            :closable="false"
          />
          <el-descriptions class="preview-descriptions" :column="1" border size="small">
            <el-descriptions-item label="文件ID">{{ selectedPreview.fileId }}</el-descriptions-item>
            <el-descriptions-item label="文件类型">{{ selectedPreview.fileKind }} {{ selectedPreview.fileExt }}</el-descriptions-item>
            <el-descriptions-item label="预览方式">{{ previewModeLabel(selectedPreview.previewMode) }}</el-descriptions-item>
            <el-descriptions-item label="转换状态">{{ conversionStatusLabel(selectedPreview.conversionStatus) }}</el-descriptions-item>
            <el-descriptions-item label="是否可直接预览">
              {{ selectedPreview.previewAvailable ? '可以接入预览入口' : '需要后续转换或查看器能力' }}
            </el-descriptions-item>
            <el-descriptions-item label="访问权限">{{ selectedPreview.accessPolicyMessage }}</el-descriptions-item>
            <el-descriptions-item label="说明">{{ selectedPreview.message }}</el-descriptions-item>
            <el-descriptions-item label="可用动作">
              <el-tag
                v-for="action in selectedPreview.supportedActions"
                :key="action"
                class="quality-flag"
                size="small"
              >
                {{ previewActionLabel(action) }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <div class="preview-actions">
            <el-button
              type="primary"
              :disabled="!canOpenPreview(selectedPreview)"
              :loading="accessActionLoading === 'PREVIEW'"
              @click="openFileAccess('PREVIEW')"
            >
              打开预览
            </el-button>
            <el-button
              :disabled="!selectedPreview.downloadAllowed"
              :loading="accessActionLoading === 'DOWNLOAD'"
              @click="openFileAccess('DOWNLOAD')"
            >
              下载文件
            </el-button>
          </div>
        </template>
        <el-empty v-else description="请选择文件查看预览状态" :image-size="56" />
      </div>
    </el-dialog>

    <el-dialog v-model="checksumJobDialogVisible" title="checksum 任务状态" width="620px" @closed="stopChecksumJobPolling">
      <div v-loading="checksumJobLoading" class="job-dialog-body">
        <template v-if="selectedChecksumJob">
          <div class="job-state-panel">
            <el-tag :type="jobStatusTag(selectedChecksumJob.status)" size="large">
              {{ jobStatusLabel(selectedChecksumJob.status) }}
            </el-tag>
            <div>
              <strong>任务 {{ selectedChecksumJob.id }}</strong>
              <span>{{ checksumJobTargetLabel }}</span>
            </div>
          </div>
          <el-progress
            :percentage="jobProgressValue(selectedChecksumJob)"
            :status="selectedChecksumJob.status === 'FAILED' ? 'exception' : selectedChecksumJob.status === 'SUCCEEDED' ? 'success' : undefined"
          />
          <el-descriptions class="job-descriptions" :column="1" border size="small">
            <el-descriptions-item label="任务类型">{{ selectedChecksumJob.jobType }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ jobStatusLabel(selectedChecksumJob.status) }}</el-descriptions-item>
            <el-descriptions-item label="进度">
              {{ formatCount(selectedChecksumJob.progressCurrent) }} / {{ formatCount(selectedChecksumJob.progressTotal) }}
            </el-descriptions-item>
            <el-descriptions-item label="进度说明">{{ selectedChecksumJob.progressMessage || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(selectedChecksumJob.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ formatDate(selectedChecksumJob.startedAt) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ formatDate(selectedChecksumJob.completedAt) }}</el-descriptions-item>
          </el-descriptions>
          <el-alert
            v-if="selectedChecksumJob.status === 'FAILED'"
            class="job-message"
            :title="selectedChecksumJob.failureReason || '任务失败，但未返回失败原因'"
            type="error"
            show-icon
            :closable="false"
          />
          <el-alert
            v-else-if="selectedChecksumJob.status === 'SUCCEEDED'"
            class="job-message"
            title="checksum 已计算完成并写回文件资产。"
            type="success"
            show-icon
            :closable="false"
          />
          <el-alert
            v-else
            class="job-message"
            title="任务已创建，后台会自动执行；你可以留在此处查看状态。"
            type="info"
            show-icon
            :closable="false"
          />
        </template>
        <el-empty v-else description="暂无 checksum 任务" :image-size="56" />
      </div>
      <template #footer>
        <el-button @click="checksumJobDialogVisible = false">关闭</el-button>
        <el-button
          v-if="selectedChecksumJob?.status === 'FAILED'"
          type="primary"
          :loading="checksumJobRetrying"
          @click="retryChecksumJob"
        >
          重试任务
        </el-button>
        <el-button v-else :loading="checksumJobLoading" @click="refreshChecksumJob">刷新状态</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="metadataDialogVisible" title="人工治理文件元数据" width="520px">
      <el-form label-width="96px">
        <el-form-item label="文件ID">
          <el-input :model-value="metadataForm.fileId ? String(metadataForm.fileId) : ''" disabled />
        </el-form-item>
        <el-form-item label="文件类型">
          <el-select v-model="metadataForm.fileKind" filterable>
            <el-option v-for="item in metadataFileKindOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业">
          <el-select v-model="metadataForm.discipline" clearable filterable placeholder="选择专业">
            <el-option v-for="item in disciplineOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="metadataForm.versionNo" maxlength="32" placeholder="如 V1" />
        </el-form-item>
        <el-form-item label="置信度">
          <el-select v-model="metadataForm.confidenceLevel" clearable>
            <el-option label="高" value="HIGH" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="低" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="metadataForm.reviewStatus" clearable>
            <el-option label="已确认" value="APPROVED" />
            <el-option label="待审核" value="PENDING" />
            <el-option label="已驳回" value="REJECTED" />
            <el-option label="自动入库" value="AUTO_INGESTED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metadataDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="metadataSaving" @click="saveMetadata">保存治理结果</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onUnmounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Back, CopyDocument, Refresh, Search, View } from '@element-plus/icons-vue';

import {
  createChecksumJob,
  fetchAssetDisciplines,
  fetchAssetPathMappings,
  fetchAssetProjects,
  fetchAssetScanTasks,
  fetchAssetStatistics,
  fetchAssetJob,
  fetchFileAsset,
  fetchFileAssetsPage,
  fetchFilePreview,
  createFileAccessTicket,
  retryAssetJob,
  updateFileAssetMetadata,
  type AssetJob,
  type AssetDiscipline,
  type AssetPathMapping,
  type AssetProject,
  type AssetScanTask,
  type AssetStatistics,
  type FileAsset,
  type FilePreview
} from '@/modules/data-steward/api/dataSteward';
import { copyText } from '@/modules/data-steward/utils/clipboard';

const route = useRoute();
const router = useRouter();
const projectId = computed(() => Number(route.params.projectId));

const loading = ref(false);
const fileLoading = ref(false);
const activeTab = ref('files');
const project = ref<AssetProject | null>(null);
const statistics = ref<AssetStatistics | null>(null);
const files = ref<FileAsset[]>([]);
const scanTasks = ref<AssetScanTask[]>([]);
const pathMappings = ref<AssetPathMapping[]>([]);
const disciplineOptions = ref<AssetDiscipline[]>([]);
const detailDrawerVisible = ref(false);
const selectedFile = ref<FileAsset | null>(null);
const metadataDialogVisible = ref(false);
const metadataSaving = ref(false);
const checksumCreating = ref(false);
const previewDialogVisible = ref(false);
const previewLoading = ref(false);
const accessActionLoading = ref<'PREVIEW' | 'DOWNLOAD' | null>(null);
const selectedPreview = ref<FilePreview | null>(null);
const checksumJobDialogVisible = ref(false);
const checksumJobLoading = ref(false);
const checksumJobRetrying = ref(false);
const selectedChecksumJob = ref<AssetJob | null>(null);
const selectedChecksumJobFile = ref<FileAsset | null>(null);
let pageLoadRequestId = 0;
let fileLoadRequestId = 0;
let previewRequestId = 0;
let checksumJobRequestId = 0;
let checksumJobTimer: ReturnType<typeof window.setInterval> | null = null;
const filters = reactive({
  keyword: '',
  fileKind: 'ALL',
  qualityIssue: queryString(route.query.qualityIssue) ?? 'ALL',
  discipline: '',
  fileExt: ''
});
const filePagination = reactive({
  pageNo: 1,
  pageSize: 50,
  total: 0
});
const metadataForm = reactive({
  fileId: undefined as number | undefined,
  fileKind: '',
  discipline: '',
  versionNo: '',
  confidenceLevel: '',
  reviewStatus: ''
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
const metadataFileKindOptions = [
  { label: '模型', value: 'MODEL' },
  { label: '图纸', value: 'DRAWING' },
  { label: '文档', value: 'DOCUMENT' },
  { label: '表格', value: 'SPREADSHEET' },
  { label: '汇报', value: 'PRESENTATION' },
  { label: '轻量化模型', value: 'MODEL_VIEWER' },
  { label: '归档包', value: 'ARCHIVE' },
  { label: '其他', value: 'OTHER' }
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

const projectTitle = computed(() => {
  if (!project.value) return `项目 ${projectId.value}`;
  return `${project.value.code} ${project.value.name}`;
});
const projectSubTitle = computed(() => {
  if (!project.value) return '资产明细';
  return [project.value.projectStage, project.value.projectManagerName].filter(Boolean).join(' / ') || '资产明细';
});
const detailTitle = computed(() => selectedFile.value ? `${selectedFile.value.fileName} - 文件详情` : '文件详情');
const currentPreview = computed(() => {
  if (!selectedFile.value || selectedPreview.value?.fileId !== selectedFile.value.fileId) return null;
  return selectedPreview.value;
});
const checksumJobTargetLabel = computed(() => {
  const file = selectedChecksumJobFile.value;
  if (!file) return '文件资产 checksum 计算';
  return `${file.fileName} / 文件ID ${file.fileId}`;
});
const cards = computed(() => {
  const item = statistics.value;
  return [
    { label: '文件总数', value: formatCount(item?.fileCount), unit: '份' },
    { label: '模型文件', value: formatCount(item?.modelFileCount), unit: '份' },
    { label: '图纸文件', value: formatCount(item?.drawingFileCount), unit: '份' },
    { label: '项目容量', value: formatBytes(item?.totalSizeBytes), unit: '已登记' },
    { label: '扫描任务', value: formatCount(projectScans.value.length), unit: '条' },
    { label: '路径映射', value: formatCount(pathMappings.value.length), unit: '条' }
  ];
});
const projectScans = computed(() => {
  const code = project.value?.code;
  return scanTasks.value.filter((item) => item.projectId === projectId.value || (code && item.projectCode === code));
});

watch(
  () => [route.params.projectId, route.query.qualityIssue],
  () => {
    filters.qualityIssue = queryString(route.query.qualityIssue) ?? 'ALL';
    filePagination.pageNo = 1;
    resetProjectData();
    void loadPage();
  },
  { immediate: true }
);

onUnmounted(() => {
  stopChecksumJobPolling();
});

async function loadPage() {
  if (!Number.isFinite(projectId.value)) return;
  const requestId = ++pageLoadRequestId;
  loading.value = true;
  try {
    const [projects, nextStatistics, nextScans, nextMappings, nextDisciplines] = await Promise.all([
      fetchAssetProjects(),
      fetchAssetStatistics(projectId.value),
      fetchAssetScanTasks(),
      fetchAssetPathMappings(projectId.value),
      fetchAssetDisciplines(projectId.value)
    ]);
    if (requestId !== pageLoadRequestId) return;
    project.value = projects.find((item) => item.projectId === projectId.value) ?? null;
    statistics.value = nextStatistics;
    scanTasks.value = nextScans;
    pathMappings.value = nextMappings;
    disciplineOptions.value = nextDisciplines;
    await loadFiles();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '项目资产加载失败');
  } finally {
    if (requestId === pageLoadRequestId) {
      loading.value = false;
    }
  }
}

async function loadFiles() {
  const requestId = ++fileLoadRequestId;
  fileLoading.value = true;
  try {
    const page = await fetchFileAssetsPage({
      projectId: projectId.value,
      fileKind: filters.fileKind === 'ALL' ? undefined : filters.fileKind,
      discipline: filters.discipline.trim() || undefined,
      fileExt: normalizeExt(filters.fileExt),
      keyword: filters.keyword.trim() || undefined,
      qualityIssue: filters.qualityIssue === 'ALL' ? undefined : filters.qualityIssue,
      pageNo: filePagination.pageNo,
      pageSize: filePagination.pageSize
    });
    if (requestId === fileLoadRequestId) {
      files.value = page.rows;
      filePagination.pageNo = page.page;
      filePagination.pageSize = page.pageSize;
      filePagination.total = page.total;
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件资产加载失败');
  } finally {
    if (requestId === fileLoadRequestId) {
      fileLoading.value = false;
    }
  }
}

function reloadFilesFromFirstPage() {
  filePagination.pageNo = 1;
  void loadFiles();
}

function resetProjectData() {
  project.value = null;
  statistics.value = null;
  files.value = [];
  scanTasks.value = [];
  pathMappings.value = [];
  disciplineOptions.value = [];
  selectedFile.value = null;
  selectedPreview.value = null;
  selectedChecksumJob.value = null;
  selectedChecksumJobFile.value = null;
  detailDrawerVisible.value = false;
  previewDialogVisible.value = false;
  checksumJobDialogVisible.value = false;
  stopChecksumJobPolling();
  filePagination.total = 0;
}

async function openFileDetail(row: FileAsset) {
  try {
    selectedFile.value = await fetchFileAsset(row.fileId);
    detailDrawerVisible.value = true;
    void loadPreview(row.fileId, false);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件详情加载失败');
  }
}

async function openPreview(row: FileAsset) {
  selectedFile.value = row;
  previewDialogVisible.value = true;
  await loadPreview(row.fileId, true);
}

async function loadPreview(fileId: number, showError: boolean) {
  const requestId = ++previewRequestId;
  if (selectedPreview.value?.fileId !== fileId) {
    selectedPreview.value = null;
  }
  previewLoading.value = true;
  try {
    const preview = await fetchFilePreview(fileId);
    if (requestId === previewRequestId) {
      selectedPreview.value = preview;
    }
  } catch (error) {
    if (requestId === previewRequestId && showError) {
      ElMessage.error(error instanceof Error ? error.message : '预览状态加载失败');
    }
  } finally {
    if (requestId === previewRequestId) {
      previewLoading.value = false;
    }
  }
}

function canOpenPreview(preview: FilePreview | null) {
  return Boolean(preview?.previewAvailable && preview.previewAllowed);
}

async function openFileAccess(action: 'PREVIEW' | 'DOWNLOAD') {
  const preview = selectedPreview.value ?? currentPreview.value;
  if (!preview) {
    ElMessage.warning('请先加载文件预览状态');
    return;
  }
  if (action === 'PREVIEW' && !canOpenPreview(preview)) {
    ElMessage.warning(preview.accessPolicyMessage || '当前文件暂不可预览');
    return;
  }
  if (action === 'DOWNLOAD' && !preview.downloadAllowed) {
    ElMessage.warning(preview.accessPolicyMessage || '当前账号没有下载权限');
    return;
  }
  const popup = window.open('', '_blank', 'noopener');
  accessActionLoading.value = action;
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
    accessActionLoading.value = null;
  }
}

function openMetadataDialog(row: FileAsset) {
  selectedFile.value = row;
  metadataForm.fileId = row.fileId;
  metadataForm.fileKind = row.fileKind;
  metadataForm.discipline = row.discipline ?? '';
  metadataForm.versionNo = row.versionNo ?? '';
  metadataForm.confidenceLevel = row.confidenceLevel ?? '';
  metadataForm.reviewStatus = row.reviewStatus ?? '';
  metadataDialogVisible.value = true;
}

async function saveMetadata() {
  if (!metadataForm.fileId) return;
  metadataSaving.value = true;
  try {
    const updated = await updateFileAssetMetadata(metadataForm.fileId, {
      fileKind: metadataForm.fileKind || undefined,
      discipline: metadataForm.discipline || undefined,
      versionNo: metadataForm.versionNo.trim() || undefined,
      confidenceLevel: metadataForm.confidenceLevel || undefined,
      reviewStatus: metadataForm.reviewStatus || undefined
    });
    selectedFile.value = updated;
    metadataDialogVisible.value = false;
    ElMessage.success('治理结果已保存');
    await loadFiles();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存治理结果失败');
  } finally {
    metadataSaving.value = false;
  }
}

async function createChecksum(row: FileAsset) {
  if (row.checksum || checksumCreating.value) return;
  checksumCreating.value = true;
  try {
    const job = await createChecksumJob(row.fileId);
    selectedChecksumJobFile.value = row;
    selectedChecksumJob.value = job;
    checksumJobDialogVisible.value = true;
    ElMessage.success('checksum 任务已创建，正在后台执行');
    startChecksumJobPolling(job.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建 checksum 任务失败');
  } finally {
    checksumCreating.value = false;
  }
}

function startChecksumJobPolling(jobId: number) {
  stopChecksumJobPolling();
  void loadChecksumJob(jobId, true);
  checksumJobTimer = window.setInterval(() => {
    void loadChecksumJob(jobId, false);
  }, 1800);
}

function stopChecksumJobPolling() {
  if (checksumJobTimer) {
    window.clearInterval(checksumJobTimer);
    checksumJobTimer = null;
  }
}

async function loadChecksumJob(jobId: number, showError: boolean) {
  const requestId = ++checksumJobRequestId;
  checksumJobLoading.value = true;
  try {
    const job = await fetchAssetJob(jobId);
    if (requestId !== checksumJobRequestId) return;
    selectedChecksumJob.value = job;
    if (isTerminalJobStatus(job.status)) {
      stopChecksumJobPolling();
      if (job.status === 'SUCCEEDED') {
        await refreshChecksumTarget(job);
      }
    }
  } catch (error) {
    if (requestId === checksumJobRequestId && showError) {
      ElMessage.error(error instanceof Error ? error.message : '任务状态加载失败');
    }
  } finally {
    if (requestId === checksumJobRequestId) {
      checksumJobLoading.value = false;
    }
  }
}

async function refreshChecksumJob() {
  if (!selectedChecksumJob.value) return;
  await loadChecksumJob(selectedChecksumJob.value.id, true);
}

async function retryChecksumJob() {
  const job = selectedChecksumJob.value;
  if (!job || checksumJobRetrying.value) return;
  checksumJobRetrying.value = true;
  try {
    await retryAssetJob(job.id);
    ElMessage.success('checksum 任务已重新提交');
    startChecksumJobPolling(job.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '任务重试失败');
  } finally {
    checksumJobRetrying.value = false;
  }
}

async function refreshChecksumTarget(job: AssetJob) {
  if (job.targetId && selectedFile.value?.fileId === job.targetId) {
    selectedFile.value = await fetchFileAsset(job.targetId);
  }
  await loadFiles();
}

function isTerminalJobStatus(status: string) {
  return ['SUCCEEDED', 'FAILED', 'CANCELED'].includes(status);
}

async function copyPath(path: string) {
  const copied = await copyText(path);
  if (copied) {
    ElMessage.success('路径已复制');
    return;
  }
  ElMessage.error('路径复制失败，请手动选中路径复制');
}

function normalizeExt(value: string) {
  const next = value.trim();
  if (!next) return undefined;
  return next.startsWith('.') ? next : `.${next}`;
}

function qualityFlags(file: FileAsset) {
  const flags: string[] = [];
  if (!file.checksum) flags.push('MISSING_CHECKSUM');
  if (!file.confidenceLevel) flags.push('MISSING_CONFIDENCE');
  if (!file.discipline || file.discipline === 'OTHER') flags.push('MISSING_DISCIPLINE');
  if (!file.versionNo) flags.push('MISSING_VERSION');
  if (!file.storagePath) flags.push('MISSING_STORAGE_PATH');
  if (Number(file.sizeBytes ?? 0) <= 0) flags.push('ZERO_SIZE_FILE');
  return flags;
}

function qualityFlagLabel(value: string) {
  const labels: Record<string, string> = {
    MISSING_CHECKSUM: '缺 checksum',
    MISSING_CONFIDENCE: '缺置信度',
    MISSING_DISCIPLINE: '专业待完善',
    MISSING_VERSION: '版本缺失',
    MISSING_STORAGE_PATH: '路径缺失',
    ZERO_SIZE_FILE: '零大小文件'
  };
  return labels[value] ?? value;
}

function queryString(value: unknown) {
  if (Array.isArray(value)) return value[0] ? String(value[0]) : undefined;
  return value ? String(value) : undefined;
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

function fileKindTag(value: string) {
  if (value === 'MODEL') return 'success';
  if (value === 'DRAWING') return 'primary';
  return 'info';
}

function previewStatusTag(value: string) {
  if (value === 'AVAILABLE') return 'success';
  if (value === 'NEEDS_CONVERSION') return 'warning';
  if (value === 'BLOCKED') return 'danger';
  return 'info';
}

function previewStatusLabel(value: string) {
  const labels: Record<string, string> = {
    AVAILABLE: '可接入预览',
    NEEDS_CONVERSION: '需要转换',
    UNSUPPORTED: '暂不支持',
    BLOCKED: '暂不可用'
  };
  return labels[value] ?? value;
}

function previewModeLabel(value: string) {
  const labels: Record<string, string> = {
    BROWSER_NATIVE: '浏览器原生预览',
    OFFICE_CONVERSION: 'Office 转换预览',
    CAD_CONVERSION: 'CAD 转换预览',
    BIM_LIGHTWEIGHT: 'BIM 轻量化预览',
    DOWNLOAD_ONLY: '仅保留受控访问',
    NONE: '暂无预览方式'
  };
  return labels[value] ?? value;
}

function conversionStatusLabel(value: string) {
  const labels: Record<string, string> = {
    NOT_REQUIRED: '不需要转换',
    NOT_STARTED: '尚未开始',
    NOT_SUPPORTED: '暂不支持'
  };
  return labels[value] ?? value;
}

function previewActionLabel(value: string) {
  const labels: Record<string, string> = {
    OPEN_PREVIEW_STATUS: '查看预览状态',
    DOWNLOAD_VIA_PLATFORM: '后续接入平台下载',
    REQUEST_CONVERSION: '后续创建转换任务',
    VIEW_METADATA: '查看元数据',
    VIEW_AUDIT: '查看审计',
    FIX_METADATA: '补齐元数据'
  };
  return labels[value] ?? value;
}

function disciplineLabel(value: string | null | undefined) {
  const found = disciplineOptions.value.find((item) => item.code === value);
  if (found) return found.name;
  const labels: Record<string, string> = {
    ARCHITECTURE: '建筑',
    STRUCTURE: '结构',
    PLUMBING: '给排水',
    HVAC: '暖通',
    ELECTRICAL: '电气',
    FIRE_PROTECTION: '消防',
    INTELLIGENT: '智能化',
    GENERAL: '综合',
    GAS: '燃气',
    OTHER: '其他'
  };
  if (!value) return '-';
  return labels[value] ?? value;
}

function scanStatusTag(value: string) {
  if (value === 'SUCCEEDED') return 'success';
  if (value === 'FAILED') return 'danger';
  if (value === 'RUNNING') return 'warning';
  return 'info';
}

function jobStatusTag(value: string) {
  if (value === 'SUCCEEDED') return 'success';
  if (value === 'FAILED') return 'danger';
  if (value === 'RUNNING') return 'warning';
  return 'info';
}

function jobStatusLabel(value: string) {
  const labels: Record<string, string> = {
    PENDING: '等待执行',
    RUNNING: '执行中',
    SUCCEEDED: '已成功',
    FAILED: '已失败',
    CANCELED: '已取消'
  };
  return labels[value] ?? value;
}

function jobProgressValue(job: AssetJob) {
  if (job.status === 'SUCCEEDED') return 100;
  const next = Number(job.progressPercent ?? 0);
  if (!Number.isFinite(next)) return 0;
  return Math.min(100, Math.max(0, Math.round(next)));
}

function scanProgressValue(task: AssetScanTask) {
  if (task.status === 'SUCCEEDED') return 100;
  const next = Number(task.progressPercent ?? 0);
  if (!Number.isFinite(next)) return 0;
  return Math.min(100, Math.max(0, Math.round(next)));
}
</script>

<style scoped>
.asset-page {
  min-width: 0;
}

.asset-tabs {
  min-width: 0;
  padding: 16px;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
}

.asset-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto 150px 150px 120px auto;
  gap: 10px;
  margin-bottom: 14px;
}

.asset-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.asset-detail-section {
  margin-bottom: 18px;
}

.asset-detail-section h3 {
  margin: 0 0 8px 0;
  padding-left: 8px;
  border-left: 3px solid #409eff;
  color: #303133;
  font-size: 14px;
}

.asset-detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-message {
  margin-top: 10px;
}

.preview-dialog-body {
  min-height: 260px;
}

.preview-state-panel {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.preview-state-panel strong,
.preview-state-panel span {
  display: block;
  min-width: 0;
  overflow-wrap: anywhere;
}

.preview-state-panel span {
  margin-top: 4px;
  color: #606266;
  font-size: 13px;
}

.preview-descriptions {
  margin-top: 12px;
}

.preview-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.job-dialog-body {
  min-height: 260px;
}

.job-state-panel {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.job-state-panel strong,
.job-state-panel span {
  display: block;
  min-width: 0;
  overflow-wrap: anywhere;
}

.job-state-panel span {
  margin-top: 4px;
  color: #606266;
  font-size: 13px;
}

.job-descriptions {
  margin-top: 12px;
}

.job-message {
  margin-top: 12px;
}

.quality-flag {
  margin-right: 4px;
}

.mono-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  overflow-wrap: anywhere;
}

@media (max-width: 1100px) {
  .asset-toolbar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .asset-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
