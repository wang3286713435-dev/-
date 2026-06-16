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
              <el-button :disabled="!canWriteNas || nasBusy" :icon="FolderAdd" @click="handleCreateDirectoryClick">新建文件夹</el-button>
            </span>
          </el-tooltip>
          <el-button :icon="Refresh" :loading="fileLoading || dirLoading || nasTrialLoading" @click="refreshBrowserViews(true)">
            刷新
          </el-button>
          <el-tooltip :content="activeDirectoryActionTip" placement="top">
            <span>
              <el-dropdown trigger="click" :disabled="!activeDir || nasBusy" @command="handleActiveDirectoryCommand">
                <el-button :disabled="!activeDir || nasBusy">
                  当前文件夹
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :disabled="!canOperateActiveDirectory || nasBusy" command="rename">重命名</el-dropdown-item>
                    <el-dropdown-item :disabled="!canOperateActiveDirectory || nasBusy" command="move">移动</el-dropdown-item>
                    <el-dropdown-item :disabled="!canQuarantineActiveDirectory || nasBusy" command="quarantine" divided>
                      移入回收站
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </span>
          </el-tooltip>
          <el-button :loading="quarantineLoading" @click="openQuarantineDrawer">回收站</el-button>
          <el-button :loading="operationsLoading" @click="openOperationsDrawer">操作记录</el-button>
        </div>
        <div class="file-browser__search">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="搜索文件名或平台资产ID"
            :prefix-icon="Search"
            @keyup.enter="reloadFiles"
            @clear="reloadFiles"
          />
          <el-button :icon="Search" @click="reloadFiles">查询</el-button>
          <el-checkbox
            v-if="hasKeyword"
            v-model="filters.searchCurrentFolderOnly"
            class="file-browser__search-scope-toggle"
            @change="reloadFiles"
          >
            仅当前文件夹及子目录
          </el-checkbox>
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
        v-if="hasKeyword"
        class="file-browser__search-alert"
        type="info"
        show-icon
        :closable="false"
        :title="searchScopeHint"
      />

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
        <el-select v-model="filters.ownershipStatus" @change="reloadFiles">
          <el-option v-for="item in ownershipStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
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

      <div class="file-browser__selection-bar">
        <div>
          <strong>{{ selectionSummary }}</strong>
          <span>单击选择，Ctrl / Command 多选，Shift 连续选择；双击打开，右键查看操作。</span>
        </div>
        <div class="file-browser__selection-actions">
          <el-tooltip :content="batchDownloadActionTip" placement="top">
            <span>
              <el-button size="small" :disabled="!selectedRegisteredFileEntries.length" @click="handleBatchDownloadClick">
                批量下载入口清单
              </el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="batchMoveActionTip" placement="top">
            <span>
              <el-button size="small" :disabled="!selectedEntries.length || Boolean(writeDisabledReason(false)) || nasBusy" @click="handleMoveSelectedClick">
                移动
              </el-button>
            </span>
          </el-tooltip>
          <el-tooltip :content="batchQuarantineActionTip" placement="top">
            <span>
              <el-button
                size="small"
                type="danger"
                plain
                :disabled="!selectedEntries.length || Boolean(writeDisabledReason(true)) || nasBusy"
                @click="handleQuarantineSelectedClick"
              >
                移入回收站
              </el-button>
            </span>
          </el-tooltip>
          <el-button v-if="selectedEntries.length" size="small" text @click="clearSelection">取消选择</el-button>
        </div>
      </div>

      <div class="file-browser__entry-surface" @click="handleTableSurfaceClick" @contextmenu.prevent="handleEmptyContextMenu">
        <el-table
          ref="tableRef"
          v-loading="fileLoading"
          :data="browserEntries"
          :row-key="entryRowKey"
          :row-class-name="entryRowClassName"
          class="master-table file-browser__entry-table"
          :empty-text="fileTableEmptyText"
          @row-click="handleEntryClick"
          @row-dblclick="handleEntryDblClick"
          @row-contextmenu="handleEntryContextMenu"
        >
          <el-table-column label="名称" min-width="320" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="file-browser__name-cell" :class="{ 'is-directory': row.kind === 'DIRECTORY' }">
                <el-icon>
                  <component :is="row.kind === 'DIRECTORY' ? Folder : Document" />
                </el-icon>
                <div>
                  <strong>{{ row.name }}</strong>
                  <span v-if="row.kind === 'DIRECTORY'">{{ row.path || '项目根目录' }}</span>
                  <span v-else-if="hasKeyword">所在位置：{{ fileLocationLabel(row.file) }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-if="hasKeyword" label="所在位置" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.kind === 'FILE'">{{ fileLocationLabel(row.file) }}</span>
              <span v-else>{{ row.path || '项目根目录' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.kind === 'DIRECTORY'" type="info">文件夹</el-tag>
              <el-tag v-else :type="fileKindTag(row.file.fileKind)">{{ row.file.fileKind }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="版本" width="90">
            <template #default="{ row }">{{ row.kind === 'FILE' ? row.file.version : '-' }}</template>
          </el-table-column>
          <el-table-column label="大小" width="120" align="right">
            <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
          </el-table-column>
          <el-table-column label="专业" width="120" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag v-if="row.kind === 'FILE'" type="info">
                {{ row.file.disciplineName || disciplineLabel(row.file.disciplineCode) || '-' }}
              </el-tag>
              <span v-else class="file-browser__muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="归属节点" min-width="190" show-overflow-tooltip>
            <template #default="{ row }">
              <template v-if="row.kind === 'FILE'">
                <div class="file-browser__ownership-cell">
                  <el-tag v-if="!isRegisteredFile(row.file)" type="warning" size="small">
                    未登记
                  </el-tag>
                  <el-tag v-else :type="ownershipStatusTag(row.file.ownershipStatus)" size="small">
                    {{ ownershipStatusLabel(row.file.ownershipStatus) }}
                  </el-tag>
                  <el-button
                    v-if="isRegisteredFile(row.file) && row.file.ownershipNodePath"
                    link
                    type="primary"
                    class="file-browser__ownership-link"
                    @click.stop="openOwnershipNode(row.file)"
                  >
                    {{ row.file.ownershipNodeLabel || row.file.ownershipNodePath }}
                  </el-button>
                  <span v-else>{{ isRegisteredFile(row.file) ? (row.file.ownershipNodeLabel || '未归属') : '需扫描入库后治理' }}</span>
                </div>
              </template>
              <span v-else class="file-browser__muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="资料用途" width="130">
            <template #default="{ row }">
              <el-tag v-if="row.kind === 'FILE' && isRegisteredFile(row.file)" effect="plain" size="small">
                {{ ownershipTypeLabel(row.file.ownershipType) }}
              </el-tag>
              <span v-else class="file-browser__muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="160">
            <template #default="{ row }">
              <div v-if="row.kind === 'FILE'" class="file-browser__status-cell">
                <el-tag v-if="!isRegisteredFile(row.file)" type="warning" size="small">未登记</el-tag>
                <el-tag v-else-if="!row.file.qualityFlags || row.file.qualityFlags.length === 0" type="success" size="small">正常</el-tag>
                <el-tag v-else type="warning" size="small">{{ row.file.qualityFlags.length }} 项待处理</el-tag>
                <span>{{ isRegisteredFile(row.file) ? (row.file.confidenceLevel === 'HIGH' ? '高置信度' : '需复核') : '需扫描入库后治理' }}</span>
              </div>
              <div v-else class="file-browser__status-cell">
                <el-tag type="info" size="small">目录</el-tag>
                <span>{{ row.fileCount }} 个登记文件</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="存储" width="170">
            <template #default="{ row }">
              <div v-if="row.kind === 'FILE'" class="file-browser__storage-cell">
                <el-tag :type="storageStateTagType(row.file)" size="small">
                  {{ storageStateLabel(row.file) }}
                </el-tag>
                <span>{{ accessSourceLabel(row.file) }}</span>
              </div>
              <span v-else class="file-browser__muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="预览产物" width="190">
            <template #default="{ row }">
              <div v-if="row.kind === 'FILE'" class="file-browser__artifact-cell">
                <el-tag
                  v-if="glandarModelStatus(row.file)"
                  :type="glandarModelTag(row.file)"
                  size="small"
                >
                  {{ glandarModelStatus(row.file)?.statusLabel }}
                </el-tag>
                <el-tag :type="previewArtifactTag(row)" size="small">
                  {{ previewArtifactLabel(row) }}
                </el-tag>
                <span>{{ previewArtifactHint(row) }}</span>
                <el-button
                  v-if="isRegisteredFile(row.file)"
                  link
                  type="primary"
                  size="small"
                  :loading="previewArtifactActionLoading(row)"
                  @click.stop="handlePreparePreviewArtifact(row)"
                >
                  {{ previewArtifactActionLabel(row) }}
                </el-button>
              </div>
              <span v-else class="file-browser__muted">-</span>
            </template>
          </el-table-column>
          <el-table-column v-if="diagnosticInfoVisible" label="技术信息 / 诊断" min-width="260">
            <template #default="{ row }">
              <div v-if="row.kind === 'FILE'" class="file-browser__diagnostic-cell">
                <span>平台资产ID：{{ row.file.assetUuid || '-' }}</span>
                <span>内部文件ID：{{ row.file.fileId ?? '-' }}</span>
                <span>扩展名：{{ row.file.fileExt || '-' }}</span>
                <span>置信度：{{ row.file.confidenceLevel ?? '-' }}</span>
                <span v-if="!isRegisteredFile(row.file)">登记状态：未入库</span>
                <span>更新时间：{{ formatDate(row.file.updatedAt) }}</span>
              </div>
              <div v-else class="file-browser__diagnostic-cell">
                <span>类型：项目内文件夹</span>
                <span>文件夹路径提示：{{ row.path || '项目根目录' }}</span>
                <span>不展示真实 NAS 绝对路径</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <div class="file-browser__row-actions">
                <el-button text @click.stop="handleOpenEntryButtonClick(row)">打开</el-button>
                <el-button text :icon="MoreFilled" aria-label="打开右键菜单" @click.stop="openContextMenuFromButton(row, $event)" />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

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

    <div
      v-if="contextMenu.visible"
      ref="contextMenuRef"
      class="file-browser__context-menu"
      :style="contextMenuStyle"
      @click.stop
      @contextmenu.prevent
    >
      <button
        v-for="item in contextMenuItems"
        :key="item.command"
        type="button"
        :class="{ 'is-danger': item.danger, 'is-disabled': item.disabled, 'is-divided': item.divided }"
        :disabled="item.disabled"
        @click="handleContextMenuItemClick(item)"
      >
        <span>{{ item.label }}</span>
        <small v-if="item.reason">{{ item.reason }}</small>
      </button>
    </div>

    <el-dialog v-model="modelPreviewDialogVisible" title="模型预览占位" width="560px">
      <div class="file-browser__model-placeholder">
        <el-alert
          title="当前只登记模型目录信息，不做 BIM 轻量化、不读取模型正文、不解析构件参数。"
          type="info"
          show-icon
          :closable="false"
        />
        <el-descriptions v-if="modelPreviewEntry?.kind === 'FILE'" :column="1" border size="small">
          <el-descriptions-item label="文件名">{{ modelPreviewEntry.file.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ modelPreviewEntry.file.fileKind }} {{ modelPreviewEntry.file.fileExt || '' }}</el-descriptions-item>
          <el-descriptions-item label="平台资产ID">{{ modelPreviewEntry.file.assetUuid || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内部文件ID">{{ modelPreviewEntry.file.fileId }}</el-descriptions-item>
          <el-descriptions-item label="处理方式">后续接入 BIM 轻量化引擎后再进入真实模型预览。</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>

    <el-dialog v-model="previewFallbackDialogVisible" title="打开受控预览入口" width="560px">
      <div class="file-browser__preview-fallback">
        <el-alert
          type="warning"
          show-icon
          :closable="false"
          title="浏览器可能拦截了新窗口。平台已创建受控访问票据，未读取文件正文，也未暴露真实 NAS 路径。"
        />
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="文件名">{{ previewFallbackFileName }}</el-descriptions-item>
          <el-descriptions-item label="入口类型">{{ previewFallbackAction === 'PREVIEW' ? '预览' : '下载' }}</el-descriptions-item>
          <el-descriptions-item label="有效期">
            {{ previewFallbackTicket ? formatDate(previewFallbackTicket.expiresAt) : '-' }}
          </el-descriptions-item>
        </el-descriptions>
        <a
          v-if="previewFallbackTicket?.accessUrl"
          class="file-browser__fallback-link"
          :href="previewFallbackTicket.accessUrl"
          target="_blank"
          rel="noopener"
          @click="previewFallbackDialogVisible = false"
        >
          点击打开受控{{ previewFallbackAction === 'PREVIEW' ? '预览' : '下载' }}入口
        </a>
      </div>
    </el-dialog>

    <el-dialog v-model="batchDownloadDialogVisible" title="批量下载入口清单" width="720px">
      <el-alert
        class="file-browser__drawer-alert"
        type="info"
        show-icon
        :closable="false"
        title="本批只为已选文件逐个创建平台 DOWNLOAD 访问票据；不生成 ZIP，不复制 NAS 文件，文件夹会被跳过。"
      />
      <div v-loading="batchDownloadLoading" class="file-browser__batch-list">
        <article v-for="item in batchDownloadRows" :key="item.key" class="file-browser__batch-item">
          <div>
            <strong>{{ item.name }}</strong>
            <span>{{ item.message }}</span>
          </div>
          <el-tag :type="batchStatusTag(item.status)" size="small">{{ batchStatusLabel(item.status) }}</el-tag>
          <el-button v-if="item.accessUrl" size="small" type="primary" @click="openBatchDownloadLink(item)">
            打开下载入口
          </el-button>
        </article>
        <el-empty v-if="!batchDownloadLoading && batchDownloadRows.length === 0" description="暂无下载入口" :image-size="56" />
      </div>
    </el-dialog>

    <el-dialog v-model="batchResultDialogVisible" title="批量操作结果" width="720px">
      <template v-if="batchOperationResult">
        <div class="file-browser__batch-summary">
          <strong>{{ batchOperationResult.title }}</strong>
          <span>
            成功 {{ batchOperationResult.successCount }} 项 /
            失败 {{ batchOperationResult.failedCount }} 项 /
            跳过 {{ batchOperationResult.skippedCount }} 项
          </span>
        </div>
        <div class="file-browser__batch-list">
          <article v-for="item in batchOperationResult.rows" :key="item.key" class="file-browser__batch-item">
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ item.message }}</span>
            </div>
            <el-tag :type="batchStatusTag(item.status)" size="small">{{ batchStatusLabel(item.status) }}</el-tag>
          </article>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="operationsDrawerVisible" title="文件管理操作记录" size="520px" @open="loadNasOperations">
      <el-alert
        class="file-browser__drawer-alert"
        type="info"
        show-icon
        :closable="false"
        title="这里仅展示文件管理操作记录，不展示真实 NAS 绝对路径。"
      />
      <div v-loading="operationsLoading" class="file-browser__drawer-list">
        <article v-for="item in nasOperations" :key="item.operationId" class="file-browser__drawer-item">
          <strong>{{ operationLabel(item.operationType) }} / {{ operationStatusLabel(item.status) }}</strong>
          <span>{{ item.targetDisplayPath || item.sourceDisplayPath || '项目根目录' }}</span>
          <em>操作编号 {{ item.operationId }} / traceId {{ item.traceId || '-' }} / {{ formatDate(item.createdAt) }}</em>
        </article>
        <el-empty v-if="!operationsLoading && nasOperations.length === 0" description="暂无文件管理操作记录" :image-size="64" />
      </div>
    </el-drawer>

    <el-drawer v-model="quarantineDrawerVisible" title="回收站" size="560px" @open="loadNasQuarantine">
      <el-alert
        class="file-browser__drawer-alert"
        type="warning"
        show-icon
        :closable="false"
        title="回收站支持恢复，不提供永久删除；列表不展示真实 NAS 绝对路径。"
      />
      <div v-loading="quarantineLoading" class="file-browser__drawer-list">
        <article v-for="item in nasQuarantine" :key="item.quarantineRecordId" class="file-browser__drawer-item">
          <div>
            <strong>{{ item.displayName }} / {{ targetTypeLabel(item.targetType) }}</strong>
            <span>{{ item.originalDisplayPath || '项目根目录' }}</span>
            <em>回收站编号 {{ item.quarantineRecordId }} / {{ quarantineStatusLabel(item.status) }} / {{ formatDate(item.createdAt) }}</em>
          </div>
          <el-button
            size="small"
            type="primary"
            :disabled="item.status !== 'QUARANTINED' || !canAdminNasProjectTrial || nasBusy"
            @click="handleRestoreQuarantineClick(item.quarantineRecordId)"
          >
            恢复
          </el-button>
        </article>
        <el-empty v-if="!quarantineLoading && nasQuarantine.length === 0" description="回收站暂无内容" :image-size="64" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Document, Folder, FolderAdd, MoreFilled, Refresh, Search, Upload } from '@element-plus/icons-vue';
import { useRoute, useRouter } from 'vue-router';

import {
  createFileAccessTicket,
  createNasDirectory,
  fetchCatalogDirectories,
  fetchCatalogDirectoryChildren,
  fetchCatalogFiles,
  fetchNasWriteTrialStatus,
  fetchNasOperations,
  fetchNasQuarantine,
  moveNasDirectory,
  moveNasFile,
  preparePreviewArtifact,
  quarantineNasDirectory,
  quarantineNasFile,
  renameNasDirectory,
  renameNasFile,
  restoreNasQuarantine,
  uploadNasFile,
  type CatalogDirectory,
  type CatalogFile,
  type AssetDiscipline,
  type FileAccessTicket,
  type NasOperationRecord,
  type NasQuarantineRecord,
  type NasWriteTrialStatus,
  type PreviewArtifact
} from '@/modules/data-steward/api/dataSteward';
import DirectoryTreePanel from '@/modules/data-steward/components/DirectoryTreePanel.vue';
import { buildDirectoryTree } from '@/modules/data-steward/utils/directoryTree';
import {
  previewActionHint,
  previewFromFileName,
  type PreviewStatusLike
} from '@/modules/data-steward/utils/previewStatus';
import {
  createFileLightweightJob,
  fetchGlandarModelFiles,
  issueLightweightViewerTicket,
  type GlandarModelFile,
  type LightweightJobCreateResponse
} from '@/modules/visualization/api/visualization';
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
  'select-file': [file: CatalogFile, origin: { clientX: number; clientY: number }];
  'blank-click': [];
  'open-metadata': [fileId: number];
  'create-checksum': [fileId: number];
  'create-batch-checksum': [];
  'ask-hermes-ownership': [fileId: number];
  'open-ownership-node': [nodePath: string];
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
  'fileSearchScope',
  'fileKind',
  'discipline',
  'fileExt',
  'qualityIssue',
  'ownershipStatus',
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
const directDirectories = ref<CatalogDirectory[]>([]);
const files = ref<CatalogFile[]>([]);
const nasOperations = ref<NasOperationRecord[]>([]);
const nasQuarantine = ref<NasQuarantineRecord[]>([]);
const nasTrialStatus = ref<NasWriteTrialStatus | null>(null);
const previewArtifacts = ref<Record<number, PreviewArtifact>>({});
const glandarModelFiles = ref<GlandarModelFile[]>([]);
const glandarModelFilesLoaded = ref(false);
let glandarModelFilesPromise: Promise<void> | null = null;
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
const previewArtifactLoadingId = ref<number | null>(null);
const advancedSearchVisible = ref(false);
const diagnosticInfoVisible = ref(false);
const treeWidth = ref(DEFAULT_TREE_WIDTH);
const resizingTree = ref(false);
const contextMenuRef = ref<HTMLElement | null>(null);
const selectedEntryKeys = ref<Set<string>>(new Set());
const lastSelectionAnchorKey = ref<string | null>(null);
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0
});
const modelPreviewDialogVisible = ref(false);
const modelPreviewEntry = ref<BrowserEntry | null>(null);
const previewFallbackDialogVisible = ref(false);
const previewFallbackTicket = ref<FileAccessTicket | null>(null);
const previewFallbackFileName = ref('');
const previewFallbackAction = ref<'PREVIEW' | 'DOWNLOAD'>('PREVIEW');
const fileAccessOpening = ref(false);
const glandarOpeningFileId = ref<number | null>(null);
const batchDownloadDialogVisible = ref(false);
const batchDownloadLoading = ref(false);
const batchDownloadRows = ref<BatchDownloadRow[]>([]);
const batchResultDialogVisible = ref(false);
const batchOperationResult = ref<BatchOperationResult | null>(null);
let resizePointerId: number | null = null;
let tableLayoutFrame = 0;
let directoryRequestId = 0;
let fileRequestId = 0;
let applyingSavedState = false;
let stateReady = false;

const filters = reactive({
  keyword: '',
  searchCurrentFolderOnly: false,
  fileKind: 'ALL',
  disciplineCode: '',
  fileExt: '',
  qualityIssue: props.initialQualityIssue || 'ALL',
  ownershipStatus: 'ALL'
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

const ownershipStatusOptions = [
  { label: '全部归属', value: 'ALL' },
  { label: '未归属', value: 'UNASSIGNED' },
  { label: '建议中', value: 'SUGGESTED' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '已驳回', value: 'REJECTED' }
];

type FileBrowserState = {
  activeDir?: string;
  keyword?: string;
  searchCurrentFolderOnly?: boolean;
  fileKind?: string;
  disciplineCode?: string;
  fileExt?: string;
  qualityIssue?: string;
  ownershipStatus?: string;
  page?: number;
  pageSize?: number;
  expandedDirs?: string[];
  lastFileId?: number | null;
  lastFileName?: string;
  updatedAt?: string;
};

type DirectoryBrowserEntry = {
  kind: 'DIRECTORY';
  key: string;
  name: string;
  path: string;
  fileCount: number;
  sizeBytes: number;
  directory: CatalogDirectory;
};

type FileBrowserEntry = {
  kind: 'FILE';
  key: string;
  name: string;
  path: string;
  fileCount: 0;
  sizeBytes: number;
  file: CatalogFile;
};

type BrowserEntry = DirectoryBrowserEntry | FileBrowserEntry;

type ContextMenuCommand =
  | 'open'
  | 'preview'
  | 'detail'
  | 'metadata'
  | 'prepare-preview-artifact'
  | 'hermes-ownership'
  | 'checksum'
  | 'rename'
  | 'move'
  | 'quarantine'
  | 'download'
  | 'batch-download';

type ContextMenuItem = {
  command: ContextMenuCommand;
  label: string;
  disabled?: boolean;
  reason?: string;
  danger?: boolean;
  divided?: boolean;
};

type BatchStatus = 'SUCCESS' | 'FAILED' | 'SKIPPED';

type BatchDownloadRow = {
  key: string;
  name: string;
  status: BatchStatus;
  message: string;
  accessUrl?: string;
};

type BatchOperationRow = {
  key: string;
  name: string;
  status: BatchStatus;
  message: string;
};

type BatchOperationResult = {
  title: string;
  rows: BatchOperationRow[];
  successCount: number;
  failedCount: number;
  skippedCount: number;
};

const fileBrowserStyle = computed(() => ({
  '--tree-width': `${treeWidth.value}px`
}));

const routeProject = computed(() => authStore.currentUser?.projects.find((project) => project.id === props.projectId) ?? null);
const hasRouteProjectAccess = computed(() => Boolean(routeProject.value));
const currentProjectRole = computed(() => routeProject.value?.roleCode ?? '');
const currentProjectMismatch = computed(() => {
  const globalProjectId = authStore.currentUser?.currentProject?.id;
  return Number.isFinite(globalProjectId) && globalProjectId !== props.projectId;
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
  if (!hasRouteProjectAccess.value) return '当前账号没有本项目权限，不能操作公司 NAS 文件。';
  if (canWriteNas.value) {
    return currentProjectMismatch.value
      ? '正在使用当前项目工作台项目执行操作，后端仍会校验你的项目权限和灰度范围。'
      : '当前目录已开启真实 NAS 写入灰度，平台会做权限、路径和审计校验。';
  }
  if (!hasNasWriteRole.value) return '当前项目角色只能查看，不能操作公司 NAS 文件。';
  return nasTrialStatus.value?.disabledReason || '当前目录暂不可执行真实 NAS 写操作。';
});

const activeDirectoryActionTip = computed(() => {
  if (!canWriteNas.value) return nasWriteActionTip.value;
  if (!activeDir.value) return '请先在左侧选择一个项目内文件夹。';
  return '将直接操作当前文件夹，平台会校验路径不越出项目。';
});

const activeDirectoryLabel = computed(() => activeDir.value || '项目根目录');
const hasKeyword = computed(() => filters.keyword.trim().length > 0);
const searchScopeHint = computed(() => {
  if (!hasKeyword.value) return '';
  if (filters.searchCurrentFolderOnly) {
    return `正在“${activeDirectoryLabel.value}”及其子目录中搜索，结果不会包含其他项目目录。`;
  }
  return '正在整个项目中搜索，结果会显示所在位置；不会暴露真实 NAS 路径。';
});
const fileTableEmptyText = computed(() => {
  if (!hasKeyword.value) return '当前文件夹暂无文件或直接子文件夹';
  return filters.searchCurrentFolderOnly ? '当前文件夹及子目录没有匹配文件' : '整个项目没有匹配文件';
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
    filters.ownershipStatus !== 'ALL' ? `归属：${ownershipStatusLabel(filters.ownershipStatus)}` : '',
    `第 ${pagination.page} 页`,
    lastFileId.value ? `最近文件：${lastFileName.value || `内部文件ID ${lastFileId.value}`}` : ''
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
  return 0;
});

const directoryTreeModel = computed(() => buildDirectoryTree(directories.value));

const directoryEntries = computed<DirectoryBrowserEntry[]>(() =>
  directDirectories.value.map((directory) => ({
    kind: 'DIRECTORY',
    key: `DIRECTORY:${directory.directoryPath}`,
    name: directory.directoryName || pathLeaf(directory.directoryPath),
    path: directory.directoryPath,
    fileCount: directory.fileCount,
    sizeBytes: directory.totalSizeBytes,
    directory
  }))
);

const fileEntries = computed<FileBrowserEntry[]>(() =>
  files.value.map((file) => ({
    kind: 'FILE',
    key: file.fileId ? `FILE:${file.fileId}` : `UNREGISTERED:${file.logicalPath || file.fileName}`,
    name: file.fileName,
    path: file.logicalPath || activeDir.value,
    fileCount: 0,
    sizeBytes: file.sizeBytes,
    file
  }))
);

const browserEntries = computed<BrowserEntry[]>(() => {
  if (hasKeyword.value) return fileEntries.value;
  return [
    ...directoryEntries.value,
    ...fileEntries.value
  ];
});

const glandarModelByFileId = computed(() => {
  const map = new Map<number, GlandarModelFile>();
  glandarModelFiles.value.forEach((item) => map.set(item.fileId, item));
  return map;
});

const selectedEntries = computed(() => {
  const keys = selectedEntryKeys.value;
  return browserEntries.value.filter((entry) => keys.has(entry.key));
});

const selectedFileEntries = computed(() => selectedEntries.value.filter((entry): entry is FileBrowserEntry => entry.kind === 'FILE'));
const selectedRegisteredFileEntries = computed(() =>
  selectedFileEntries.value.filter((entry) => isRegisteredFile(entry.file))
);
const selectedDirectoryEntries = computed(() =>
  selectedEntries.value.filter((entry): entry is DirectoryBrowserEntry => entry.kind === 'DIRECTORY')
);

const selectionSummary = computed(() => {
  if (!selectedEntries.value.length) {
    if (hasKeyword.value) {
      const scope = filters.searchCurrentFolderOnly ? '当前文件夹及子目录' : '整个项目';
      return `搜索结果：${scope}内 ${pagination.total} 个文件`;
    }
    return `当前文件夹：${directoryEntries.value.length} 个文件夹 / ${pagination.total} 个文件`;
  }
  return `已选 ${selectedEntries.value.length} 项：文件 ${selectedFileEntries.value.length} 个，文件夹 ${selectedDirectoryEntries.value.length} 个`;
});

const batchDownloadActionTip = computed(() => {
  if (selectedRegisteredFileEntries.value.length) {
    return selectedDirectoryEntries.value.length
      ? '将为已选文件创建下载入口，文件夹会跳过；本批不生成 ZIP。'
      : '将为已选文件逐个创建平台下载入口；本批不生成 ZIP。';
  }
  if (selectedFileEntries.value.some((entry) => !isRegisteredFile(entry.file))) {
    return '未登记文件需先扫描入库，本次不会创建下载入口。';
  }
  if (selectedDirectoryEntries.value.length) return '文件夹打包下载待交付包能力支持，本次只处理已选文件。';
  return '请先选择要下载的文件。';
});

const batchMoveActionTip = computed(() => {
  if (!selectedEntries.value.length) return '请先选择要移动的文件或文件夹。';
  return writeDisabledReason(false) || '将逐项调用平台现有移动接口，后端继续校验权限、灰度和路径。';
});

const batchQuarantineActionTip = computed(() => {
  if (!selectedEntries.value.length) return '请先选择要移入回收站的文件或文件夹。';
  return writeDisabledReason(true) || '将逐项移入回收站，不会永久删除。';
});

const contextMenuStyle = computed(() => ({
  left: `${contextMenu.x}px`,
  top: `${contextMenu.y}px`
}));

const contextMenuItems = computed<ContextMenuItem[]>(() => buildContextMenuItems(selectedEntries.value));

watch(
  () => [props.projectId, props.initialQualityIssue] as const,
  () => {
    initializeBrowserState();
    glandarModelFiles.value = [];
    glandarModelFilesLoaded.value = false;
    glandarModelFilesPromise = null;
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
    route.query.fileDir,
    route.query.fileKeyword,
    route.query.fileSearchScope,
    route.query.fileKind,
    route.query.discipline,
    route.query.fileExt,
    route.query.qualityIssue,
    route.query.ownershipStatus,
    route.query.filePage,
    route.query.filePageSize
  ],
  () => {
    if (!stateReady || applyingSavedState) return;
    const state = readQueryState();
    if (!state || isCurrentBrowserStateEquivalent(state)) return;
    applyBrowserState(state, true);
    clearSelection();
    void loadNasWriteTrialStatus();
    void loadFiles();
  }
);

watch(
  () => [
    activeDir.value,
    filters.keyword,
    filters.searchCurrentFolderOnly,
    filters.fileKind,
    filters.disciplineCode,
    filters.fileExt,
    filters.qualityIssue,
    filters.ownershipStatus,
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

watch(
  () => browserEntries.value.map((entry) => entry.key).join('|'),
  () => {
    pruneSelection();
  }
);

onMounted(() => {
  treeWidth.value = clampTreeWidth(readStoredTreeWidth());
  window.addEventListener('resize', handleWindowResize);
  window.addEventListener('click', closeContextMenu);
  window.addEventListener('keydown', handleGlobalKeydown);
});

onUnmounted(() => {
  stopTreeResize();
  window.removeEventListener('resize', handleWindowResize);
  window.removeEventListener('click', closeContextMenu);
  window.removeEventListener('keydown', handleGlobalKeydown);
  if (tableLayoutFrame) {
    window.cancelAnimationFrame(tableLayoutFrame);
  }
});

function initializeBrowserState() {
  applyingSavedState = true;
  stateReady = false;
  files.value = [];
  directDirectories.value = [];
  pagination.total = 0;

  const state = readQueryState() ?? readStoredBrowserState() ?? {};
  applyBrowserState(state, true);

  applyingSavedState = false;
  stateReady = true;
  persistBrowserState();
}

function applyBrowserState(state: FileBrowserState, preserveViewMemory = false) {
  applyingSavedState = true;
  const fallbackQualityIssue = props.initialQualityIssue || 'ALL';
  activeDir.value = normalizeProjectDirectoryPath(state.activeDir ?? '');
  filters.keyword = state.keyword ?? '';
  filters.searchCurrentFolderOnly = state.searchCurrentFolderOnly === true;
  filters.fileKind = normalizeFileKind(state.fileKind);
  filters.disciplineCode = state.disciplineCode ?? '';
  filters.fileExt = state.fileExt ?? '';
  filters.qualityIssue = state.qualityIssue ?? fallbackQualityIssue;
  filters.ownershipStatus = normalizeOwnershipStatus(state.ownershipStatus);
  pagination.page = positiveNumber(state.page, 1);
  pagination.pageSize = normalizePageSize(state.pageSize);
  expandedDirs.value = Array.isArray(state.expandedDirs)
    ? state.expandedDirs.filter(Boolean)
    : preserveViewMemory ? expandedDirs.value : [];
  lastFileId.value = state.lastFileId && Number.isFinite(Number(state.lastFileId)) ? Number(state.lastFileId) : null;
  lastFileName.value = state.lastFileName ?? (preserveViewMemory ? lastFileName.value : '');
  advancedSearchVisible.value = hasAdvancedFilters();
  applyingSavedState = false;
}

function readQueryState(): FileBrowserState | null {
  const hasQueryState = QUERY_KEYS.some((key) => key !== 'tab' && route.query[key] !== undefined);
  if (!hasQueryState) return null;
  return {
    activeDir: queryString(route.query.fileDir) ?? '',
    keyword: queryString(route.query.fileKeyword) ?? '',
    searchCurrentFolderOnly: queryString(route.query.fileSearchScope) === 'current',
    fileKind: queryString(route.query.fileKind) ?? 'ALL',
    disciplineCode: queryString(route.query.discipline) ?? '',
    fileExt: queryString(route.query.fileExt) ?? '',
    qualityIssue: queryString(route.query.qualityIssue) ?? props.initialQualityIssue ?? 'ALL',
    ownershipStatus: queryString(route.query.ownershipStatus) ?? 'ALL',
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
    searchCurrentFolderOnly: filters.searchCurrentFolderOnly,
    fileKind: filters.fileKind,
    disciplineCode: filters.disciplineCode,
    fileExt: filters.fileExt.trim(),
    qualityIssue: filters.qualityIssue,
    ownershipStatus: filters.ownershipStatus,
    page: pagination.page,
    pageSize: pagination.pageSize,
    expandedDirs: expandedDirs.value,
    lastFileId: lastFileId.value,
    lastFileName: lastFileName.value,
    updatedAt: new Date().toISOString()
  };
}

function isCurrentBrowserStateEquivalent(state: FileBrowserState) {
  return normalizeProjectDirectoryPath(state.activeDir ?? '') === activeDir.value
    && (state.keyword ?? '') === filters.keyword
    && (state.searchCurrentFolderOnly === true) === filters.searchCurrentFolderOnly
    && normalizeFileKind(state.fileKind) === filters.fileKind
    && (state.disciplineCode ?? '') === filters.disciplineCode
    && (state.fileExt ?? '') === filters.fileExt
    && (state.qualityIssue ?? props.initialQualityIssue ?? 'ALL') === filters.qualityIssue
    && normalizeOwnershipStatus(state.ownershipStatus) === filters.ownershipStatus
    && positiveNumber(state.page, 1) === pagination.page
    && normalizePageSize(state.pageSize) === pagination.pageSize;
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
  assignQuery(nextQuery, 'fileSearchScope', state.searchCurrentFolderOnly ? 'current' : '');
  assignQuery(nextQuery, 'fileKind', state.fileKind === 'ALL' ? '' : state.fileKind);
  assignQuery(nextQuery, 'discipline', state.disciplineCode);
  assignQuery(nextQuery, 'fileExt', state.fileExt);
  assignQuery(nextQuery, 'qualityIssue', state.qualityIssue === 'ALL' ? '' : state.qualityIssue);
  assignQuery(nextQuery, 'ownershipStatus', state.ownershipStatus === 'ALL' ? '' : state.ownershipStatus);
  assignQuery(nextQuery, 'filePage', state.page && state.page > 1 ? String(state.page) : '');
  assignQuery(nextQuery, 'filePageSize', state.pageSize && state.pageSize !== DEFAULT_PAGE_SIZE ? String(state.pageSize) : '');
  assignQuery(nextQuery, 'lastFileId', state.lastFileId ? String(state.lastFileId) : '');

  if (isSameQuery(nextQuery, route.query)) return;
  void router.replace({ path: route.path, query: nextQuery });
}

function resetViewState() {
  activeDir.value = '';
  filters.keyword = '';
  filters.searchCurrentFolderOnly = false;
  filters.fileKind = 'ALL';
  filters.disciplineCode = '';
  filters.fileExt = '';
  filters.qualityIssue = 'ALL';
  filters.ownershipStatus = 'ALL';
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

function isEntrySelected(key: string) {
  return selectedEntryKeys.value.has(key);
}

function setSelectedKeys(keys: string[], anchorKey = keys.at(-1) ?? null) {
  selectedEntryKeys.value = new Set(keys);
  lastSelectionAnchorKey.value = anchorKey;
}

function clearSelection() {
  selectedEntryKeys.value = new Set();
  lastSelectionAnchorKey.value = null;
  closeContextMenu();
}

function pruneSelection() {
  if (!selectedEntryKeys.value.size) return;
  const existing = new Set(browserEntries.value.map((entry) => entry.key));
  const next = Array.from(selectedEntryKeys.value).filter((key) => existing.has(key));
  selectedEntryKeys.value = new Set(next);
  if (lastSelectionAnchorKey.value && !existing.has(lastSelectionAnchorKey.value)) {
    lastSelectionAnchorKey.value = next.at(-1) ?? null;
  }
}

function handleEntryClick(row: BrowserEntry, _column: unknown, event: MouseEvent) {
  selectEntryByMouse(row, event);
}

function selectEntryByMouse(row: BrowserEntry, event: MouseEvent) {
  closeContextMenu();
  if (row.kind === 'FILE') {
    emit('select-file', row.file, { clientX: event.clientX, clientY: event.clientY });
  }
  const keys = browserEntries.value.map((entry) => entry.key);
  if (event.shiftKey && lastSelectionAnchorKey.value) {
    const anchorIndex = keys.indexOf(lastSelectionAnchorKey.value);
    const currentIndex = keys.indexOf(row.key);
    if (anchorIndex >= 0 && currentIndex >= 0) {
      const [start, end] = anchorIndex <= currentIndex
        ? [anchorIndex, currentIndex]
        : [currentIndex, anchorIndex];
      setSelectedKeys(keys.slice(start, end + 1), lastSelectionAnchorKey.value);
      return;
    }
  }

  if (event.metaKey || event.ctrlKey) {
    const next = new Set(selectedEntryKeys.value);
    if (next.has(row.key)) {
      next.delete(row.key);
    } else {
      next.add(row.key);
    }
    selectedEntryKeys.value = next;
    lastSelectionAnchorKey.value = row.key;
    return;
  }

  setSelectedKeys([row.key], row.key);
}

function handleEntryDblClick(row: BrowserEntry) {
  closeContextMenu();
  runAsyncAction(() => openEntry(row), '文件打开失败');
}

function handleOpenEntryButtonClick(row: BrowserEntry) {
  runAsyncAction(() => openEntry(row), '文件打开失败');
}

function handleContextMenuItemClick(item: ContextMenuItem) {
  runAsyncAction(() => runContextMenuCommand(item), '菜单操作失败');
}

function handleCreateDirectoryClick() {
  runAsyncAction(createDirectoryAction, '新建文件夹失败');
}

function handleBatchDownloadClick() {
  runAsyncAction(createBatchDownloadTickets, '批量下载入口清单创建失败');
}

function handleMoveSelectedClick() {
  runAsyncAction(moveSelectedEntries, '移动失败');
}

function handleQuarantineSelectedClick() {
  runAsyncAction(quarantineSelectedEntries, '移入回收站失败');
}

function handleRestoreQuarantineClick(recordId: number) {
  runAsyncAction(() => restoreQuarantineItem(recordId), '恢复回收站项目失败');
}

function runAsyncAction(action: () => Promise<unknown>, fallbackMessage: string) {
  void action().catch((error) => {
    if (isUserCancel(error)) return;
    ElMessage.error(toUserErrorMessage(error, fallbackMessage));
  });
}

function isUserCancel(error: unknown) {
  if (error === 'cancel' || error === 'close') return true;
  if (typeof error !== 'object' || error === null) return false;
  const action = (error as { action?: unknown }).action;
  return action === 'cancel' || action === 'close';
}

function toUserErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) return error.message;
  if (typeof error === 'string' && error.trim()) return error;
  return fallbackMessage;
}

function handleEntryContextMenu(row: BrowserEntry, _column: unknown, event: MouseEvent) {
  event.preventDefault();
  if (!selectedEntryKeys.value.has(row.key)) {
    setSelectedKeys([row.key], row.key);
  }
  void showContextMenu(event.clientX, event.clientY);
}

function openContextMenuFromButton(row: BrowserEntry, event: MouseEvent) {
  if (!selectedEntryKeys.value.has(row.key)) {
    setSelectedKeys([row.key], row.key);
  }
  const target = event.currentTarget as HTMLElement;
  const rect = target.getBoundingClientRect();
  void showContextMenu(rect.left, rect.bottom + 6);
}

function handleTableSurfaceClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null;
  if (!target) return;
  if (target.closest('.el-table__row') || target.closest('.file-browser__context-menu')) return;
  clearSelection();
  emit('blank-click');
}

function handleEmptyContextMenu(event: MouseEvent) {
  const target = event.target as HTMLElement | null;
  if (target?.closest('.el-table__row')) return;
  clearSelection();
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeContextMenu();
  }
}

function entryRowClassName({ row }: { row: BrowserEntry }) {
  const classes = ['file-browser__entry-row'];
  if (row.kind === 'DIRECTORY') classes.push('is-directory');
  if (isEntrySelected(row.key)) classes.push('is-selected');
  return classes.join(' ');
}

function entryRowKey(row: BrowserEntry) {
  return row.key;
}

async function showContextMenu(x: number, y: number) {
  const position = clampContextMenuPosition(x, y, 260, 360);
  contextMenu.x = position.x;
  contextMenu.y = position.y;
  contextMenu.visible = true;
  await nextTick();
  const rect = contextMenuRef.value?.getBoundingClientRect();
  if (!rect) return;
  const adjusted = clampContextMenuPosition(contextMenu.x, contextMenu.y, rect.width, rect.height);
  contextMenu.x = adjusted.x;
  contextMenu.y = adjusted.y;
}

function clampContextMenuPosition(x: number, y: number, width: number, height: number) {
  const padding = 8;
  return {
    x: Math.max(padding, Math.min(x, window.innerWidth - width - padding)),
    y: Math.max(padding, Math.min(y, window.innerHeight - height - padding))
  };
}

function closeContextMenu() {
  contextMenu.visible = false;
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
      directories.value = mergeDirectories(directories.value, nextDirectories, activeDir.value);
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
    const keyword = filters.keyword.trim();
    const commonParams = {
      projectId: props.projectId,
      keyword: keyword || undefined,
      fileKind: filters.fileKind === 'ALL' ? undefined : filters.fileKind,
      disciplineCode: filters.disciplineCode || undefined,
      fileExt: normalizeExt(filters.fileExt),
      qualityIssue: filters.qualityIssue === 'ALL' ? undefined : filters.qualityIssue,
      ownershipStatus: filters.ownershipStatus === 'ALL' ? undefined : filters.ownershipStatus,
      page: pagination.page,
      pageSize: pagination.pageSize
    };

    if (keyword) {
      const result = await fetchCatalogFiles({
        ...commonParams,
        directoryPath: filters.searchCurrentFolderOnly ? activeDir.value || undefined : undefined,
        directOnly: false
      });
      if (requestId === fileRequestId) {
        directDirectories.value = [];
        files.value = result.rows;
        pagination.page = result.page;
        pagination.pageSize = result.pageSize;
        pagination.total = result.total;
      }
      return;
    }

    const result = await fetchCatalogDirectoryChildren({
      ...commonParams,
      directoryPath: activeDir.value || undefined,
      directOnly: true
    });
    if (requestId === fileRequestId) {
      directDirectories.value = result.directories;
      directories.value = mergeDirectories(directories.value, result.directories, activeDir.value);
      files.value = result.files.rows;
      pagination.page = result.files.page;
      pagination.pageSize = result.files.pageSize;
      pagination.total = result.files.total;
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

async function loadGlandarModelFiles() {
  if (!Number.isFinite(props.projectId)) return;
  try {
    glandarModelFiles.value = await fetchGlandarModelFiles(props.projectId);
  } catch {
    glandarModelFiles.value = [];
  } finally {
    glandarModelFilesLoaded.value = true;
  }
}

async function ensureGlandarModelFilesLoaded() {
  if (glandarModelFilesLoaded.value) return;
  if (!glandarModelFilesPromise) {
    glandarModelFilesPromise = loadGlandarModelFiles().finally(() => {
      glandarModelFilesPromise = null;
    });
  }
  await glandarModelFilesPromise;
}

async function loadDirectoryChildrenForTree(dirPath: string) {
  if (!Number.isFinite(props.projectId)) return;
  try {
    const result = await fetchCatalogDirectoryChildren({
      projectId: props.projectId,
      directoryPath: dirPath || undefined,
      page: 1,
      pageSize: 1
    });
    directories.value = mergeDirectories(directories.value, result.directories, dirPath);
  } catch {
    // The right-hand list still loads independently; keep tree expansion lightweight.
  }
}

async function refreshBrowserViews(showSuccess = false) {
  const loaders: Promise<unknown>[] = [
    loadNasWriteTrialStatus(),
    loadDirectories(),
    loadFiles()
  ];
  if (operationsDrawerVisible.value) loaders.push(loadNasOperations(false));
  if (quarantineDrawerVisible.value) loaders.push(loadNasQuarantine(false));
  await Promise.all(loaders);
  if (showSuccess) {
    ElMessage.success('文件列表、目录树和当前目录状态已刷新');
  }
}

async function runNasOperation(action: () => Promise<{ operationId: number; message: string; traceId: string }>) {
  if (nasBusy.value) return false;
  nasBusy.value = true;
  try {
    const result = await action();
    const storageText = result.storageStatus === 'OBJECT_STORED' ? '，存储：对象存储' : '';
    ElMessage.success(`${result.message}${storageText}。操作编号 ${result.operationId}，traceId ${result.traceId}`);
    await refreshBrowserViews();
    return true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '真实 NAS 操作失败，平台未执行永久删除');
    return false;
  } finally {
    nasBusy.value = false;
    uploadingNasFile.value = false;
  }
}

function writeDisabledReason(requireAdmin: boolean) {
  if (nasBusy.value) return '操作正在执行中';
  if (nasTrialLoading.value) return '正在读取灰度状态';
  if (!hasRouteProjectAccess.value) return '无写权限：当前账号没有本项目权限';
  if (requireAdmin && currentProjectRole.value !== 'PROJECT_ADMIN') return '无写权限：需要项目管理员';
  if (!requireAdmin && !hasNasWriteRole.value) return '无写权限：当前项目角色只能查看';
  if (!nasTrialStatus.value?.enabled) return '灰度未开启';
  if (!nasTrialStatus.value.roleAllowed || !nasTrialStatus.value.accountAllowed) {
    return nasTrialStatus.value.disabledReason || '当前账号不在灰度范围内';
  }
  if (!nasTrialStatus.value.directoryAllowed) return nasTrialStatus.value.disabledReason || '当前目录不可写';
  if (requireAdmin && !canAdminNas.value) return nasTrialStatus.value.disabledReason || '当前目录不可写';
  if (!requireAdmin && !canWriteNas.value) return nasTrialStatus.value.disabledReason || '当前目录不可写';
  return '';
}

function buildContextMenuItems(entries: BrowserEntry[]): ContextMenuItem[] {
  if (!entries.length) return [];
  const writeReason = writeDisabledReason(false);
  const adminReason = writeDisabledReason(true);
  const fileCount = entries.filter((entry) => entry.kind === 'FILE').length;
  const directoryCount = entries.length - fileCount;

  if (entries.length > 1) {
    return [
      {
        command: 'move',
        label: `移动所选 ${entries.length} 项`,
        disabled: Boolean(writeReason),
        reason: writeReason || undefined
      },
      {
        command: 'quarantine',
        label: `移入回收站 ${entries.length} 项`,
        disabled: Boolean(adminReason),
        reason: adminReason || undefined,
        danger: true
      },
      {
        command: 'batch-download',
        label: '批量下载入口清单',
        disabled: fileCount === 0,
        reason: fileCount === 0
          ? '文件夹打包下载待交付包能力支持'
          : directoryCount > 0
            ? '只处理文件，文件夹会跳过'
            : undefined,
        divided: true
      },
      {
        command: 'rename',
        label: '重命名',
        disabled: true,
        reason: '多选时不能重命名'
      }
    ];
  }

  const [entry] = entries;
  if (entry.kind === 'DIRECTORY') {
    return [
      { command: 'open', label: '打开' },
      {
        command: 'rename',
        label: '重命名',
        disabled: Boolean(writeReason),
        reason: writeReason || undefined,
        divided: true
      },
      {
        command: 'move',
        label: '移动',
        disabled: Boolean(writeReason),
        reason: writeReason || undefined
      },
      {
        command: 'quarantine',
        label: '移入回收站',
        disabled: Boolean(adminReason),
        reason: adminReason || undefined,
        danger: true
      },
      {
        command: 'download',
        label: '下载',
        disabled: true,
        reason: '文件夹打包下载待交付包能力支持',
        divided: true
      }
    ];
  }

  const preview = previewForFileEntry(entry);
  const isModel = preview.previewMode === 'BIM_LIGHTWEIGHT';
  const modelStatus = isModel ? glandarModelStatus(entry.file) : null;
  const unregisteredReason = isRegisteredFile(entry.file) ? '' : '未登记文件需先扫描入库后治理';
  return [
    {
      command: 'open',
      label: isModel
        ? modelStatus?.taskStatus === 'READY'
          ? '打开轻量化模型'
          : modelStatus?.supported
            ? '提交 / 查看轻量化'
            : '模型暂不支持轻量化'
        : '打开 / 预览',
      disabled: Boolean(unregisteredReason),
      reason: unregisteredReason || (isModel && modelStatus && !modelStatus.supported ? modelStatus.unsupportedReason || '当前模型格式暂不支持轻量化' : undefined)
    },
    { command: 'detail', label: '详情', disabled: Boolean(unregisteredReason), reason: unregisteredReason || undefined },
    ...(!isModel
      ? [{
          command: 'prepare-preview-artifact' as const,
          label: '准备预览产物状态',
          disabled: Boolean(unregisteredReason),
          reason: unregisteredReason || '仅登记对象化状态或转换占位，不读取文件正文'
        }]
      : []),
    { command: 'metadata', label: '治理', disabled: Boolean(unregisteredReason), reason: unregisteredReason || undefined },
    { command: 'hermes-ownership', label: '询问 Hermes 归属建议', disabled: Boolean(unregisteredReason), reason: unregisteredReason || undefined },
    { command: 'checksum', label: '补 checksum', disabled: Boolean(unregisteredReason), reason: unregisteredReason || undefined },
    {
      command: 'rename',
      label: '重命名',
      disabled: Boolean(writeReason || unregisteredReason),
      reason: writeReason || unregisteredReason || undefined,
      divided: true
    },
    {
      command: 'move',
      label: '移动',
      disabled: Boolean(writeReason || unregisteredReason),
      reason: writeReason || unregisteredReason || undefined
    },
    {
      command: 'quarantine',
      label: '移入回收站',
      disabled: Boolean(adminReason || unregisteredReason),
      reason: adminReason || unregisteredReason || undefined,
      danger: true
    },
    { command: 'download', label: '下载', disabled: Boolean(unregisteredReason), reason: unregisteredReason || undefined, divided: true }
  ];
}

async function runContextMenuCommand(item: ContextMenuItem) {
  if (item.disabled) return;
  closeContextMenu();
  const entries = selectedEntries.value;
  if (!entries.length) return;
  if (item.command === 'batch-download') {
    await createBatchDownloadTickets();
    return;
  }
  if (item.command === 'move') {
    if (entries.length > 1) {
      await moveSelectedEntries();
    } else {
      await moveEntry(entries[0]);
    }
    return;
  }
  if (item.command === 'quarantine') {
    if (entries.length > 1) {
      await quarantineSelectedEntries();
    } else {
      await quarantineEntry(entries[0]);
    }
    return;
  }

  const [entry] = entries;
  if (item.command === 'open' || item.command === 'preview') {
    await openEntry(entry);
  } else if (item.command === 'prepare-preview-artifact' && entry.kind === 'FILE') {
    await preparePreviewArtifactForEntry(entry);
  } else if (item.command === 'detail' && entry.kind === 'FILE' && isRegisteredFile(entry.file) && entry.file.fileId != null) {
    rememberFile(entry.file);
    emit('open-detail', entry.file.fileId);
  } else if (item.command === 'metadata' && entry.kind === 'FILE' && isRegisteredFile(entry.file) && entry.file.fileId != null) {
    rememberFile(entry.file);
    emit('open-metadata', entry.file.fileId);
  } else if (item.command === 'hermes-ownership' && entry.kind === 'FILE' && isRegisteredFile(entry.file) && entry.file.fileId != null) {
    rememberFile(entry.file);
    emit('ask-hermes-ownership', entry.file.fileId);
  } else if (item.command === 'checksum' && entry.kind === 'FILE' && isRegisteredFile(entry.file) && entry.file.fileId != null) {
    rememberFile(entry.file);
    emit('create-checksum', entry.file.fileId);
  } else if (item.command === 'rename') {
    await renameEntry(entry);
  } else if (item.command === 'download' && entry.kind === 'FILE') {
    await openControlledFileAccess(entry.file, 'DOWNLOAD');
  }
}

async function openEntry(entry: BrowserEntry) {
  if (entry.kind === 'DIRECTORY') {
    selectDir(entry.path);
    return;
  }
  await openFileByPreviewStrategy(entry);
}

async function openFileByPreviewStrategy(entry: FileBrowserEntry) {
  if (!isRegisteredFile(entry.file)) {
    ElMessage.warning('这是直接从真实 NAS 当前目录读取到的未登记文件，需扫描入库后才能预览、治理或归属。');
    return;
  }
  rememberFile(entry.file);
  const preview = previewForFileEntry(entry);
  if (preview.previewMode === 'BROWSER_NATIVE' && preview.previewAvailable) {
    await openControlledFileAccess(entry.file, 'PREVIEW');
    return;
  }
  if (preview.previewMode === 'BIM_LIGHTWEIGHT') {
    await openGlandarPreviewOrSubmit(entry);
    return;
  }
  if (preview.previewMode === 'OFFICE_CONVERSION' || preview.previewMode === 'CAD_CONVERSION') {
    emit('open-preview', entry.file.fileId);
    ElMessage.info(previewActionHint(preview));
    return;
  }
  emit('open-detail', entry.file.fileId);
}

async function openGlandarPreviewOrSubmit(entry: FileBrowserEntry) {
  if (!isRegisteredFile(entry.file) || entry.file.fileId == null) {
    openModelPreviewPlaceholder(entry);
    return;
  }
  await ensureGlandarModelFilesLoaded();
  const modelStatus = glandarModelStatus(entry.file);
  if (!modelStatus) {
    openModelPreviewPlaceholder(entry);
    ElMessage.info('当前模型尚未进入葛兰岱尔轻量化清单，暂时保留占位预览。');
    return;
  }
  if (!modelStatus.supported) {
    openModelPreviewPlaceholder(entry);
    ElMessage.warning(modelStatus.unsupportedReason || '当前模型格式暂不支持葛兰岱尔轻量化。');
    return;
  }
  if (glandarOpeningFileId.value) return;
  glandarOpeningFileId.value = entry.file.fileId;
  try {
    let jobId = modelStatus.latestJobId;
    let status = modelStatus.taskStatus;
    if (!jobId || status === 'NOT_STARTED' || status === 'FAILED') {
      const created = await createFileLightweightJob(props.projectId, entry.file.fileId, false);
      jobId = created.jobId;
      status = created.taskStatus;
      upsertGlandarModelStatus(entry.file, created);
    }
    if (status !== 'READY') {
      ElMessage.info('葛兰岱尔轻量化任务已提交或正在处理，稍后刷新后即可预览。');
      await loadGlandarModelFiles();
      return;
    }
    if (!jobId) {
      ElMessage.warning('轻量化任务缺少编号，暂时无法打开 Viewer。');
      return;
    }
    const ticket = await issueLightweightViewerTicket(props.projectId, jobId);
    if (!ticket.viewerAvailable || !ticket.ticketIssued) {
      ElMessage.warning(ticket.blockedReason || 'Viewer 暂不可用，请稍后刷新。');
      await loadGlandarModelFiles();
      return;
    }
    const routeLocation = router.resolve({
      name: 'glandar-model-preview',
      query: {
        projectId: String(props.projectId),
        jobId,
        fileName: entry.file.fileName
      }
    });
    window.open(routeLocation.href, '_blank', 'noopener,noreferrer');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '葛兰岱尔模型预览打开失败');
  } finally {
    glandarOpeningFileId.value = null;
  }
}

function glandarModelStatus(file: CatalogFile) {
  if (!isRegisteredFile(file) || file.fileId == null) return null;
  return glandarModelByFileId.value.get(file.fileId) ?? null;
}

function glandarModelTag(file: CatalogFile) {
  const modelStatus = glandarModelStatus(file);
  if (modelStatus && !modelStatus.supported) return 'info';
  const status = modelStatus?.taskStatus;
  if (status === 'READY') return 'success';
  if (status === 'FAILED') return 'danger';
  if (status === 'RUNNING' || status === 'UPLOADED' || status === 'SUBMITTED') return 'warning';
  return 'info';
}

function upsertGlandarModelStatus(file: CatalogFile, created: LightweightJobCreateResponse) {
  if (!isRegisteredFile(file) || file.fileId == null) return;
  const current = glandarModelByFileId.value.get(file.fileId);
  const next: GlandarModelFile = {
    projectId: props.projectId,
    fileId: file.fileId,
    assetUuid: file.assetUuid || '',
    fileName: file.fileName,
    extension: file.fileExt || created.modelFormat,
    fileKind: file.fileKind,
    sizeBytes: file.sizeBytes,
    versionNo: file.version || 'V1',
    relativePathHint: '项目模型资产',
    lightweightStatus: created.taskStatus === 'READY'
      ? 'READY'
      : created.taskStatus === 'FAILED'
        ? 'FAILED'
        : 'RUNNING',
    latestJobId: created.jobId,
    taskStatus: created.taskStatus,
    progress: created.progressPercent,
    failureReason: created.blockedReason,
    viewerAvailable: created.viewerAvailable,
    supported: true,
    unsupportedReason: null,
    statusLabel: created.statusLabel,
    actionHint: created.actionHint,
    updatedAt: new Date().toISOString()
  };
  if (!current) {
    glandarModelFiles.value = [...glandarModelFiles.value, next];
    return;
  }
  glandarModelFiles.value = glandarModelFiles.value.map((item) => item.fileId === file.fileId
    ? {
        ...item,
        ...next
      }
    : item);
}

function handlePreparePreviewArtifact(entry: FileBrowserEntry) {
  const preview = previewForFileEntry(entry);
  if (preview.previewMode === 'BIM_LIGHTWEIGHT') {
    runAsyncAction(() => openGlandarPreviewOrSubmit(entry), 'BIM 轻量化状态处理失败');
    return;
  }
  runAsyncAction(() => preparePreviewArtifactForEntry(entry), '预览产物状态准备失败，平台未读取文件正文');
}

async function preparePreviewArtifactForEntry(entry: FileBrowserEntry) {
  if (!isRegisteredFile(entry.file) || entry.file.fileId == null) {
    ElMessage.warning('未登记文件需先扫描入库后才能登记预览产物状态。');
    return;
  }
  if (previewArtifactLoadingId.value != null) return;
  previewArtifactLoadingId.value = entry.file.fileId;
  try {
    const artifact = await preparePreviewArtifact(entry.file.fileId);
    if (artifact) {
      previewArtifacts.value = {
        ...previewArtifacts.value,
        [entry.file.fileId]: artifact
      };
      const message = artifact.message || previewArtifactLabel(entry);
      if (artifact.previewStatus === 'AVAILABLE' && artifact.storageState === 'OBJECT_STORED') {
        ElMessage.success(message);
      } else {
        ElMessage.info(message);
      }
    }
  } finally {
    previewArtifactLoadingId.value = null;
  }
}

async function openControlledFileAccess(row: CatalogFile, action: 'PREVIEW' | 'DOWNLOAD') {
  if (!isRegisteredFile(row) || row.fileId == null) {
    ElMessage.warning('未登记文件需先扫描入库后才能创建受控访问入口。');
    return;
  }
  if (fileAccessOpening.value) return;
  let popup = tryOpenBlankPopup();
  fileAccessOpening.value = true;
  try {
    const ticket = await createFileAccessTicket(row.fileId, action);
    if (openAccessTicket(ticket, popup)) {
      ElMessage.success(fileAccessTicketMessage(ticket, action));
      return;
    }
    popup = null;
    showAccessFallback(ticket, row.fileName, action);
  } catch (error) {
    closePopupQuietly(popup);
    if (!isUserCancel(error)) {
      ElMessage.error(toUserErrorMessage(error, '文件访问票据创建失败，平台未执行任何写操作'));
    }
  } finally {
    fileAccessOpening.value = false;
  }
}

function fileAccessTicketMessage(ticket: { readSource?: string; fallbackUsed?: boolean; userMessage?: string }, action: 'PREVIEW' | 'DOWNLOAD') {
  const actionLabel = action === 'PREVIEW' ? '预览入口已打开' : '下载入口已打开';
  const source = (ticket.readSource || '').toUpperCase();
  if (ticket.fallbackUsed) return `${actionLabel}，本次使用 NAS fallback，已记录审计。`;
  if (source === 'OBJECT_STORAGE') return `${actionLabel}，本次从对象存储读取。`;
  if (source === 'LEGACY_NAS') return `${actionLabel}，本次按历史 NAS 受控读取。`;
  return ticket.userMessage || actionLabel;
}

function tryOpenBlankPopup() {
  try {
    const popup = window.open('about:blank', '_blank');
    if (popup) {
      try {
        popup.opener = null;
      } catch {
        // Ignore browsers that do not allow mutating opener on the blank popup.
      }
    }
    return popup;
  } catch {
    return null;
  }
}

function openAccessTicket(ticket: FileAccessTicket, popup: Window | null) {
  if (popup) {
    try {
      popup.location.href = ticket.accessUrl;
      return true;
    } catch {
      closePopupQuietly(popup);
    }
  }
  return tryOpenAccessUrl(ticket.accessUrl);
}

function tryOpenAccessUrl(accessUrl: string) {
  try {
    return Boolean(window.open(accessUrl, '_blank', 'noopener'));
  } catch {
    return false;
  }
}

function closePopupQuietly(popup: Window | null) {
  try {
    popup?.close();
  } catch {
    // Ignore browser-specific popup close failures.
  }
}

function showAccessFallback(ticket: FileAccessTicket, fileName: string, action: 'PREVIEW' | 'DOWNLOAD') {
  previewFallbackTicket.value = ticket;
  previewFallbackFileName.value = ticket.fileName || fileName;
  previewFallbackAction.value = action;
  previewFallbackDialogVisible.value = true;
  ElMessage.warning('浏览器可能拦截了新窗口，请在弹窗中手动打开受控访问入口。');
}

function openModelPreviewPlaceholder(entry: BrowserEntry) {
  modelPreviewEntry.value = entry;
  modelPreviewDialogVisible.value = true;
}

function previewArtifactForEntry(entry: FileBrowserEntry): PreviewArtifact | null {
  if (!isRegisteredFile(entry.file) || entry.file.fileId == null) return null;
  return previewArtifacts.value[entry.file.fileId] ?? null;
}

function previewArtifactTag(entry: FileBrowserEntry) {
  if (!isRegisteredFile(entry.file)) return 'warning';
  const preview = previewForFileEntry(entry);
  if (preview.previewMode === 'BIM_LIGHTWEIGHT') return glandarModelTag(entry.file);
  const artifact = previewArtifactForEntry(entry);
  if (artifact) {
    if (artifact.previewStatus === 'AVAILABLE' && artifact.storageState === 'OBJECT_STORED') return 'success';
    if (artifact.previewStatus === 'NEEDS_CONVERSION' || artifact.storageState === 'PENDING') return 'warning';
    if (artifact.previewStatus === 'BLOCKED' || artifact.generationStatus === 'FAILED') return 'danger';
    return 'info';
  }
  if (preview.previewStatus === 'AVAILABLE') return 'success';
  if (preview.previewStatus === 'NEEDS_CONVERSION') return 'warning';
  if (preview.previewStatus === 'BLOCKED') return 'danger';
  return 'info';
}

function previewArtifactLabel(entry: FileBrowserEntry) {
  if (!isRegisteredFile(entry.file)) return '未登记';
  const preview = previewForFileEntry(entry);
  if (preview.previewMode === 'BIM_LIGHTWEIGHT') {
    const modelStatus = glandarModelStatus(entry.file);
    if (!modelStatus) return '待轻量化';
    if (!modelStatus.supported) return '不支持轻量化';
    if (modelStatus.taskStatus === 'READY' && modelStatus.viewerAvailable) return '可在线预览';
    if (modelStatus.taskStatus === 'READY') return '已轻量化';
    if (modelStatus.taskStatus === 'FAILED') return '轻量化失败';
    if (modelStatus.taskStatus === 'RUNNING' || modelStatus.taskStatus === 'UPLOADED' || modelStatus.taskStatus === 'SUBMITTED') {
      return '轻量化中';
    }
    return '待轻量化';
  }
  const artifact = previewArtifactForEntry(entry);
  if (artifact) {
    if (artifact.previewStatus === 'AVAILABLE' && artifact.storageState === 'OBJECT_STORED') return '对象化预览';
    if (artifact.previewStatus === 'NEEDS_CONVERSION') return '需转换';
    if (artifact.previewStatus === 'UNSUPPORTED') return '暂不支持';
    if (artifact.previewStatus === 'NOT_STARTED') return '待准备';
    return artifact.previewStatus || '-';
  }
  if (preview.previewMode === 'BROWSER_NATIVE') return '可对象化';
  if (preview.conversionRequired) return '需转换占位';
  if (preview.downloadOnly) return '仅原文件';
  return '暂不支持';
}

function previewArtifactHint(entry: FileBrowserEntry) {
  if (!isRegisteredFile(entry.file)) return '需扫描入库';
  const preview = previewForFileEntry(entry);
  if (preview.previewMode === 'BIM_LIGHTWEIGHT') {
    const modelStatus = glandarModelStatus(entry.file);
    if (!modelStatus) return '从模型入口提交或查看轻量化任务';
    if (!modelStatus.supported) return modelStatus.unsupportedReason || '当前模型格式暂不支持轻量化';
    if (modelStatus.taskStatus === 'READY' && modelStatus.viewerAvailable) return '已完成轻量化，可打开 Viewer';
    if (modelStatus.taskStatus === 'FAILED') return modelStatus.failureReason || '轻量化失败，可重试';
    return modelStatus.actionHint || '轻量化任务提交后可在线预览';
  }
  const artifact = previewArtifactForEntry(entry);
  if (artifact?.message) {
    return artifact.message;
  }
  if (preview.previewMode === 'BROWSER_NATIVE') return '通过受控预览票据打开';
  if (preview.conversionRequired) return '当前只登记占位';
  return previewActionHint(preview);
}

function previewArtifactActionLabel(entry: FileBrowserEntry) {
  const preview = previewForFileEntry(entry);
  if (preview.previewMode !== 'BIM_LIGHTWEIGHT') return '准备状态';
  const modelStatus = glandarModelStatus(entry.file);
  if (modelStatus?.taskStatus === 'READY' && modelStatus.viewerAvailable) return '打开预览';
  if (modelStatus?.taskStatus === 'FAILED') return '重试轻量化';
  return '提交 / 查看';
}

function previewArtifactActionLoading(entry: FileBrowserEntry) {
  if (!isRegisteredFile(entry.file) || entry.file.fileId == null) return false;
  const preview = previewForFileEntry(entry);
  if (preview.previewMode === 'BIM_LIGHTWEIGHT') return glandarOpeningFileId.value === entry.file.fileId;
  return previewArtifactLoadingId.value === entry.file.fileId;
}

function previewForFileEntry(entry: FileBrowserEntry): PreviewStatusLike {
  return previewFromFileName(entry.file.fileName, entry.file.fileKind);
}

function isRegisteredFile(file: CatalogFile) {
  return file.registered !== false && Number.isFinite(Number(file.fileId));
}

function normalizeStorageState(file: CatalogFile) {
  if (!isRegisteredFile(file)) return 'UNREGISTERED';
  const state = (file.storageState || '').toUpperCase();
  if (state) return state;
  const provider = (file.storageProvider || '').toUpperCase();
  return provider === 'OBJECT_STORAGE' || provider === 'MINIO' || provider === 'S3_COMPATIBLE'
    ? 'OBJECT_STORED'
    : 'NAS_ONLY';
}

function storageStateLabel(file: CatalogFile) {
  switch (normalizeStorageState(file)) {
    case 'OBJECT_STORED':
      return '已对象化';
    case 'MIGRATION_PENDING':
      return '对象化中';
    case 'MIGRATION_FAILED':
      return '对象化失败';
    case 'OBJECT_UNREADABLE':
      return '对象异常';
    case 'UNREGISTERED':
      return '未登记';
    default:
      return '历史 NAS';
  }
}

function accessSourceLabel(file: CatalogFile) {
  if (!isRegisteredFile(file)) return '需扫描入库';
  switch (normalizeStorageState(file)) {
    case 'OBJECT_STORED':
      return 'NAS 侧 MinIO';
    case 'MIGRATION_PENDING':
      return '等待平台处理';
    case 'MIGRATION_FAILED':
      return '仍按历史 NAS';
    case 'OBJECT_UNREADABLE':
      return '对象副本不可读';
    default:
      return '尚未对象化';
  }
}

function storageStateTagType(file: CatalogFile) {
  switch (normalizeStorageState(file)) {
    case 'OBJECT_STORED':
      return 'success';
    case 'MIGRATION_PENDING':
      return 'warning';
    case 'MIGRATION_FAILED':
    case 'OBJECT_UNREADABLE':
      return 'danger';
    case 'UNREGISTERED':
      return 'info';
    default:
      return 'info';
  }
}

function fileLocationLabel(file: CatalogFile) {
  const path = normalizeProjectDirectoryPath(file.logicalPath || '');
  if (!path) return '项目根目录';
  const parts = splitPath(path);
  if (parts.length <= 1) return '项目根目录';
  return joinPath(parts.slice(0, -1), path.startsWith('/')) || '项目根目录';
}

async function renameEntry(entry: BrowserEntry) {
  if (entry.kind === 'DIRECTORY') {
    await renameDirectoryEntry(entry);
  } else {
    await renameFileAction(entry.file);
  }
}

async function moveEntry(entry: BrowserEntry) {
  if (entry.kind === 'DIRECTORY') {
    await moveDirectoryEntry(entry);
  } else {
    await moveFileAction(entry.file);
  }
}

async function quarantineEntry(entry: BrowserEntry) {
  if (entry.kind === 'DIRECTORY') {
    await quarantineDirectoryEntry(entry);
  } else {
    await quarantineFileAction(entry.file);
  }
}

async function renameDirectoryEntry(entry: DirectoryBrowserEntry) {
  if (writeDisabledReason(false)) return;
  const { value } = await ElMessageBox.prompt('请输入新的文件夹名称', '重命名文件夹', {
    inputValue: pathLeaf(entry.path),
    confirmButtonText: '确认重命名',
    cancelButtonText: '取消',
    inputPattern: /^(?!\.{1,2}$)[^/\\:]+$/,
    inputErrorMessage: '名称不能包含路径分隔符或特殊符号'
  });
  await confirmNasOperation('重命名文件夹', entry.path);
  const ok = await runNasOperation(() => renameNasDirectory(props.projectId, { sourcePath: entry.path, newName: value }));
  if (ok && activeDir.value === entry.path) {
    selectDir(parentPath(entry.path) ? `${parentPath(entry.path)}/${value}` : value);
  }
}

async function moveDirectoryEntry(entry: DirectoryBrowserEntry) {
  if (writeDisabledReason(false)) return;
  const targetDirectory = await promptTargetDirectory('移动文件夹', entry.path);
  await confirmNasOperation('移动文件夹', `${entry.path} -> ${targetDirectory || '项目根目录'}`);
  const ok = await runNasOperation(() => moveNasDirectory(props.projectId, { sourcePath: entry.path, targetDirectory }));
  if (ok && activeDir.value === entry.path) selectDir(joinDirectoryPath(targetDirectory, pathLeaf(entry.path)));
}

async function quarantineDirectoryEntry(entry: DirectoryBrowserEntry) {
  if (writeDisabledReason(true)) return;
  const { value } = await ElMessageBox.prompt('可填写删除原因', '移入回收站', {
    confirmButtonText: '确认移入回收站',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：误传资料，待管理员复核'
  });
  await confirmNasOperation('移入回收站', entry.path);
  const ok = await runNasOperation(() => quarantineNasDirectory(props.projectId, { sourcePath: entry.path, reason: value }));
  if (ok && activeDir.value === entry.path) selectDir(parentPath(entry.path));
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
  const newDirectoryPath = joinDirectoryPath(activeDir.value, value);
  const ok = await runNasOperation(() => createNasDirectory(props.projectId, { parentPath: activeDir.value, name: value }));
  if (ok) selectDir(newDirectoryPath);
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
  const targetDirectory = await promptTargetDirectory('移动当前文件夹', activeDir.value);
  await confirmNasOperation('移动文件夹', `${activeDir.value} -> ${targetDirectory || '项目根目录'}`);
  const previousDir = activeDir.value;
  const ok = await runNasOperation(() => moveNasDirectory(props.projectId, { sourcePath: previousDir, targetDirectory }));
  if (ok) selectDir(joinDirectoryPath(targetDirectory, pathLeaf(previousDir)));
}

async function quarantineActiveDirectory() {
  if (!canQuarantineActiveDirectory.value) return;
  const { value } = await ElMessageBox.prompt('可填写删除原因', '删除到回收站', {
    confirmButtonText: '确认移入回收站',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：误传资料，待管理员复核'
  });
  await confirmNasOperation('删除到回收站', activeDir.value);
  const previousDir = activeDir.value;
  const ok = await runNasOperation(() => quarantineNasDirectory(props.projectId, { sourcePath: previousDir, reason: value }));
  if (ok) selectDir(parentPath(previousDir));
}

async function renameFileAction(row: CatalogFile) {
  if (!isRegisteredFile(row) || row.fileId == null) {
    ElMessage.warning('未登记文件需先扫描入库后才能重命名。');
    return;
  }
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
  if (!isRegisteredFile(row) || row.fileId == null) {
    ElMessage.warning('未登记文件需先扫描入库后才能移动。');
    return;
  }
  const targetDirectory = await promptTargetDirectory('移动文件');
  await confirmNasOperation('移动文件', `${row.fileName} -> ${targetDirectory || '项目根目录'}`);
  await runNasOperation(() => moveNasFile(props.projectId, row.fileId, targetDirectory));
}

async function quarantineFileAction(row: CatalogFile) {
  if (!isRegisteredFile(row) || row.fileId == null) {
    ElMessage.warning('未登记文件需先扫描入库后才能移入回收站。');
    return;
  }
  const { value } = await ElMessageBox.prompt('可填写删除原因', '删除文件到回收站', {
    confirmButtonText: '确认移入回收站',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：误传资料，待管理员复核'
  });
  await confirmNasOperation('删除文件到回收站', row.fileName);
  await runNasOperation(() => quarantineNasFile(props.projectId, row.fileId, value));
}

async function createBatchDownloadTickets() {
  if (!selectedEntries.value.length) {
    ElMessage.warning('请先选择要下载的文件');
    return;
  }
  batchDownloadRows.value = selectedEntries.value
    .filter((entry) => entry.kind === 'DIRECTORY' || (entry.kind === 'FILE' && !isRegisteredFile(entry.file)))
    .map((entry) => ({
      key: entry.key,
      name: entry.name,
      status: 'SKIPPED' as BatchStatus,
      message: entry.kind === 'DIRECTORY'
        ? '文件夹打包下载待交付包能力支持，本次只处理已选文件。'
        : '未登记文件需先扫描入库后才能创建下载入口。'
    }));
  batchDownloadDialogVisible.value = true;
  if (!selectedRegisteredFileEntries.value.length) {
    return;
  }

  batchDownloadLoading.value = true;
  for (const entry of selectedRegisteredFileEntries.value) {
    const row: BatchDownloadRow = {
      key: entry.key,
      name: entry.file.fileName,
      status: 'FAILED',
      message: '正在创建平台下载入口'
    };
    batchDownloadRows.value = [...batchDownloadRows.value, row];
    try {
      const ticket = await createFileAccessTicket(entry.file.fileId!, 'DOWNLOAD');
      Object.assign(row, ticketToDownloadRow(entry, ticket));
    } catch (error) {
      row.status = 'FAILED';
      row.message = error instanceof Error ? error.message : '下载入口创建失败';
    }
    batchDownloadRows.value = [...batchDownloadRows.value];
  }
  batchDownloadLoading.value = false;
}

function ticketToDownloadRow(entry: FileBrowserEntry, ticket: FileAccessTicket): BatchDownloadRow {
  return {
    key: entry.key,
    name: ticket.fileName || entry.file.fileName,
    status: 'SUCCESS',
    message: `下载入口已创建，有效期至 ${formatDate(ticket.expiresAt)}。`,
    accessUrl: ticket.accessUrl
  };
}

function openBatchDownloadLink(item: BatchDownloadRow) {
  if (!item.accessUrl) return;
  if (!tryOpenAccessUrl(item.accessUrl)) {
    ElMessage.warning('浏览器可能拦截了新窗口，请允许本站弹窗后重试下载入口。');
  }
}

async function moveSelectedEntries() {
  const entries = [...selectedEntries.value];
  if (!entries.length) {
    ElMessage.warning('请先选择要移动的文件或文件夹');
    return;
  }
  const reason = writeDisabledReason(false);
  if (reason) {
    ElMessage.warning(reason);
    return;
  }
  const targetDirectory = await promptTargetDirectory('批量移动所选项目');
  await confirmNasOperation('批量移动', `${entries.length} 项 -> ${targetDirectory || '项目根目录'}`);
  await runBatchNasOperation('批量移动', entries, async (entry) => {
    if (entry.kind === 'DIRECTORY') {
      if (sameOrChild(targetDirectory, entry.path)) {
        return {
          status: 'SKIPPED',
          message: '不能把文件夹移动到自身或子文件夹内。'
        };
      }
      const result = await moveNasDirectory(props.projectId, { sourcePath: entry.path, targetDirectory });
      return { status: 'SUCCESS', message: result.message };
    }
    if (!isRegisteredFile(entry.file) || entry.file.fileId == null) {
      return { status: 'SKIPPED', message: '未登记文件需先扫描入库后才能移动。' };
    }
    const result = await moveNasFile(props.projectId, entry.file.fileId, targetDirectory);
    return { status: 'SUCCESS', message: result.message };
  });
}

async function quarantineSelectedEntries() {
  const entries = [...selectedEntries.value];
  if (!entries.length) {
    ElMessage.warning('请先选择要移入回收站的文件或文件夹');
    return;
  }
  const reason = writeDisabledReason(true);
  if (reason) {
    ElMessage.warning(reason);
    return;
  }
  const { value } = await ElMessageBox.prompt('可填写批量移入回收站原因', '批量移入回收站', {
    confirmButtonText: '继续确认',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：误传资料，待管理员复核'
  });
  await confirmNasOperation('批量移入回收站', `${entries.length} 项；不会永久删除`);
  await runBatchNasOperation('批量移入回收站', entries, async (entry) => {
    if (entry.kind === 'DIRECTORY') {
      const result = await quarantineNasDirectory(props.projectId, { sourcePath: entry.path, reason: value });
      return { status: 'SUCCESS', message: result.message };
    }
    if (!isRegisteredFile(entry.file) || entry.file.fileId == null) {
      return { status: 'SKIPPED', message: '未登记文件需先扫描入库后才能移入回收站。' };
    }
    const result = await quarantineNasFile(props.projectId, entry.file.fileId, value);
    return { status: 'SUCCESS', message: result.message };
  });
}

async function runBatchNasOperation(
  title: string,
  entries: BrowserEntry[],
  worker: (entry: BrowserEntry) => Promise<{ status: BatchStatus; message: string }>
) {
  if (nasBusy.value) return;
  nasBusy.value = true;
  const rows: BatchOperationRow[] = [];
  try {
    for (const entry of entries) {
      try {
        const result = await worker(entry);
        rows.push({
          key: entry.key,
          name: entry.name,
          status: result.status,
          message: result.message
        });
      } catch (error) {
        rows.push({
          key: entry.key,
          name: entry.name,
          status: 'FAILED',
          message: error instanceof Error ? error.message : '操作失败'
        });
      }
    }
    batchOperationResult.value = {
      title,
      rows,
      successCount: rows.filter((item) => item.status === 'SUCCESS').length,
      failedCount: rows.filter((item) => item.status === 'FAILED').length,
      skippedCount: rows.filter((item) => item.status === 'SKIPPED').length
    };
    batchResultDialogVisible.value = true;
    await refreshBrowserViews();
    pruneSelection();
  } finally {
    nasBusy.value = false;
  }
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
    if (showError) ElMessage.error(error instanceof Error ? error.message : '回收站加载失败');
  } finally {
    quarantineLoading.value = false;
  }
}

async function restoreQuarantineItem(recordId: number) {
  await confirmNasOperation('恢复回收站项目', `回收站编号 ${recordId}`);
  await runNasOperation(() => restoreNasQuarantine(props.projectId, recordId));
}

async function promptTargetDirectory(title: string, movingSourcePath = '') {
  const { value } = await ElMessageBox.prompt(
    '请输入目标文件夹的项目内相对路径，留空表示项目根目录。不要输入真实 NAS 地址。',
    title,
    {
      confirmButtonText: '确认移动',
      cancelButtonText: '取消',
      inputPlaceholder: directoryInputPlaceholder(),
      inputValidator: (input) => validateTargetDirectoryInput(input, movingSourcePath),
      inputErrorMessage: '请输入项目内相对目录，例如：平台试运行区/收件箱'
    }
  );
  return normalizeDirectoryInput(value);
}

function validateTargetDirectoryInput(input: string, movingSourcePath = '') {
  const raw = input == null ? '' : String(input).trim();
  if (!raw) return true;
  if (raw.startsWith('/') || raw.startsWith('~') || raw.includes('\\') || raw.includes(':') || raw.includes('//')) {
    return '只能填写项目内相对目录，不能填写真实 NAS 地址或绝对路径。';
  }
  const normalized = normalizeDirectoryInput(raw);
  if (!normalized) return true;
  if (splitPath(normalized).some((segment) => segment === '.' || segment === '..')) {
    return '目录不能包含 . 或 ..。';
  }
  if (movingSourcePath && sameOrChild(normalized, movingSourcePath)) {
    return '不能把文件夹移动到自身或子文件夹内。';
  }
  if (!directoryExists(normalized)) {
    return '目标文件夹不在当前目录树中，请先新建或刷新后再选择。';
  }
  return true;
}

function directoryInputPlaceholder() {
  const examples = directories.value
    .map((item) => normalizeDirectoryPath(item.directoryPath))
    .filter(Boolean)
    .slice(0, 2);
  return examples.length ? `例如：${examples.join(' 或 ')}` : '留空表示项目根目录';
}

function normalizeDirectoryInput(value: string) {
  let normalized = value.trim();
  while (normalized.endsWith('/')) {
    normalized = normalized.slice(0, -1);
  }
  while (normalized.startsWith('./')) {
    normalized = normalized.slice(2);
  }
  return normalized;
}

function directoryExists(path: string) {
  if (!path) return true;
  return directories.value.some((item) => normalizeDirectoryPath(item.directoryPath) === path);
}

function mergeDirectories(existing: CatalogDirectory[], incoming: CatalogDirectory[], parentPath = '') {
  return dedupeDirectories([
    ...existing,
    ...ancestorDirectories(parentPath),
    ...incoming
  ]);
}

function dedupeDirectories(items: CatalogDirectory[]) {
  const byPath = new Map<string, CatalogDirectory>();
  for (const item of items) {
      const path = normalizeProjectDirectoryPath(item.directoryPath || '');
    if (!path) continue;
    const previous = byPath.get(path);
    byPath.set(path, {
      ...previous,
      ...item,
      directoryPath: path,
      directoryName: item.directoryName || previous?.directoryName || pathLeaf(path),
      fileCount: Math.max(previous?.fileCount ?? 0, item.fileCount ?? 0),
      totalSizeBytes: Math.max(previous?.totalSizeBytes ?? 0, item.totalSizeBytes ?? 0),
      hasChildren: Boolean(previous?.hasChildren || item.hasChildren),
      physicalDirectory: Boolean(previous?.physicalDirectory || item.physicalDirectory)
    });
  }
  return Array.from(byPath.values()).sort((left, right) =>
    left.directoryPath.localeCompare(right.directoryPath, 'zh-CN')
  );
}

function ancestorDirectories(path: string): CatalogDirectory[] {
  const parts = splitPath(path);
  if (parts.length <= 1) return [];
  return parts.slice(0, -1).map((_, index) => {
    const directoryPath = parts.slice(0, index + 1).join('/');
    return {
      directoryPath,
      projectId: props.projectId,
      projectCode: routeProject.value?.code ?? '',
      fileCount: 0,
      totalSizeBytes: 0,
      directoryName: pathLeaf(directoryPath),
      hasChildren: true,
      physicalDirectory: true
    };
  });
}

function sameOrChild(path: string, root: string) {
  return path === root || path.startsWith(`${root}/`);
}

function handleActiveDirectoryCommand(command: string | number | object) {
  const action = String(command);
  if (action === 'rename') {
    runAsyncAction(renameActiveDirectory, '重命名当前文件夹失败');
  } else if (action === 'move') {
    runAsyncAction(moveActiveDirectory, '移动当前文件夹失败');
  } else if (action === 'quarantine') {
    runAsyncAction(quarantineActiveDirectory, '移入回收站失败');
  }
}

function selectDir(dirPath: string) {
  closeContextMenu();
  selectedEntryKeys.value = new Set();
  lastSelectionAnchorKey.value = null;
  const nextDir = normalizeProjectDirectoryPath(dirPath);
  activeDir.value = nextDir;
  rememberExpandedAncestors(nextDir);
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
  filters.ownershipStatus = 'ALL';
  reloadFiles();
}

function toggleExpandedDir(path: string, expanded: boolean) {
  const next = new Set(expandedDirs.value);
  if (expanded) {
    next.add(path);
    void loadDirectoryChildrenForTree(path);
  } else {
    next.delete(path);
  }
  expandedDirs.value = Array.from(next);
}

function rememberExpandedAncestors(path: string) {
  if (!path) return;
  const parts = splitPath(path);
  if (parts.length <= 1) return;
  const hasLeadingSlash = path.startsWith('/');
  const ancestors = parts
    .slice(0, -1)
    .map((_, index) => joinPath(parts.slice(0, index + 1), hasLeadingSlash))
    .filter(Boolean);
  expandedDirs.value = Array.from(new Set([...expandedDirs.value, ...ancestors]));
}

function rememberFile(row: CatalogFile) {
  if (!isRegisteredFile(row) || row.fileId == null) {
    lastFileId.value = null;
    lastFileName.value = row.fileName;
    return;
  }
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
    if (row.fileId == null) return;
    emit('open-preview', row.fileId);
  } else if (action === 'detail') {
    if (row.fileId == null) return;
    emit('open-detail', row.fileId);
  } else if (action === 'metadata') {
    if (row.fileId == null) return;
    emit('open-metadata', row.fileId);
  } else if (action === 'checksum') {
    if (row.fileId == null) return;
    emit('create-checksum', row.fileId);
  } else if (action === 'rename-file') {
    runAsyncAction(() => renameFileAction(row), '重命名文件失败');
  } else if (action === 'move-file') {
    runAsyncAction(() => moveFileAction(row), '移动文件失败');
  } else if (action === 'quarantine-file') {
    runAsyncAction(() => quarantineFileAction(row), '移入回收站失败');
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

function normalizeProjectDirectoryPath(path: string) {
  const normalized = normalizeDirectoryPath(path);
  const parts = splitPath(normalized);
  if (!parts.length) return '';
  if (isProjectRootWrapperSegment(parts[0])) {
    return parts.slice(1).join('/');
  }
  return normalized;
}

function isProjectRootWrapperSegment(segment: string) {
  const code = routeProject.value?.code ?? '';
  const name = routeProject.value?.name ?? props.rootLabel;
  const candidates = [
    props.rootLabel,
    name,
    code && name ? `${code}-${name}` : '',
    code && name ? `${code}_${name}` : '',
    code && name ? `${code} ${name}` : ''
  ]
    .map((item) => item.trim())
    .filter(Boolean);
  return candidates.some((candidate) => segment === candidate);
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

function joinDirectoryPath(parent: string, child: string) {
  const safeParent = normalizeDirectoryPath(parent);
  const safeChild = normalizeDirectoryPath(child);
  return safeParent ? `${safeParent}/${safeChild}` : safeChild;
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
    FILE_QUARANTINE: '删除文件到回收站',
    DIRECTORY_RENAME: '重命名文件夹',
    DIRECTORY_MOVE: '移动文件夹',
    DIRECTORY_QUARANTINE: '删除文件夹到回收站',
    QUARANTINE_RESTORE: '恢复回收站项目'
  };
  return labels[value] ?? value;
}

function operationStatusLabel(value: string) {
  const labels: Record<string, string> = {
    SUCCEEDED: '成功',
    FAILED: '失败',
    PENDING: '处理中'
  };
  return labels[value] ?? value;
}

function quarantineStatusLabel(value: string) {
  const labels: Record<string, string> = {
    QUARANTINED: '在回收站',
    RESTORED: '已恢复',
    FAILED: '恢复失败'
  };
  return labels[value] ?? value;
}

function targetTypeLabel(value: string) {
  const labels: Record<string, string> = {
    FILE: '文件',
    DIRECTORY: '文件夹'
  };
  return labels[value] ?? value;
}

function batchStatusLabel(value: BatchStatus) {
  const labels: Record<BatchStatus, string> = {
    SUCCESS: '成功',
    FAILED: '失败',
    SKIPPED: '跳过'
  };
  return labels[value];
}

function batchStatusTag(value: BatchStatus) {
  const tags: Record<BatchStatus, 'success' | 'danger' | 'info'> = {
    SUCCESS: 'success',
    FAILED: 'danger',
    SKIPPED: 'info'
  };
  return tags[value];
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
    || filters.qualityIssue !== 'ALL'
    || filters.ownershipStatus !== 'ALL';
}

function normalizeFileKind(value: string | undefined) {
  return fileKindOptions.some((item) => item.value === value) ? String(value) : 'ALL';
}

function normalizeOwnershipStatus(value: string | undefined) {
  return ownershipStatusOptions.some((item) => item.value === value) ? String(value) : 'ALL';
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

function ownershipStatusLabel(value?: string | null) {
  const map: Record<string, string> = {
    UNASSIGNED: '未归属',
    SUGGESTED: '建议中',
    CONFIRMED: '已确认',
    REJECTED: '已驳回'
  };
  return map[value || 'UNASSIGNED'] || '未归属';
}

function ownershipStatusTag(value?: string | null) {
  if (value === 'CONFIRMED') return 'success';
  if (value === 'SUGGESTED') return 'warning';
  if (value === 'REJECTED') return 'danger';
  return 'info';
}

function ownershipTypeLabel(value?: string | null) {
  const map: Record<string, string> = {
    DELIVERY: '正式交付',
    PROCESS: '过程资料',
    MODEL: '模型资料',
    DRAWING_EXCHANGE: '图纸收发',
    REFERENCE: '参考资料',
    ARCHIVE: '归档资料',
    PENDING_REVIEW: '待判定'
  };
  return map[value || ''] || '待判定';
}

function openOwnershipNode(file: CatalogFile) {
  if (!isRegisteredFile(file) || !file.ownershipNodePath) return;
  emit('open-ownership-node', file.ownershipNodePath);
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

.file-browser__search-scope-toggle {
  margin-left: 0;
  white-space: nowrap;
}

.file-browser__search-alert {
  margin-bottom: var(--zy-sp-3);
  border-radius: var(--zy-radius-base);
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

.file-browser__selection-bar {
  display: flex;
  gap: var(--zy-sp-3);
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--zy-sp-3);
  padding: var(--zy-sp-2) var(--zy-sp-3);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
}

.file-browser__selection-bar > div:first-child {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.file-browser__selection-bar strong,
.file-browser__selection-bar span {
  display: block;
  min-width: 0;
}

.file-browser__selection-bar strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.file-browser__selection-bar span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.45;
}

.file-browser__selection-actions {
  display: inline-flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  justify-content: flex-end;
}

.file-browser__entry-surface {
  min-width: 0;
}

.file-browser__entry-table :deep(.file-browser__entry-row) {
  cursor: default;
  user-select: none;
}

.file-browser__entry-table :deep(.file-browser__entry-row.is-directory .el-table__cell) {
  background: rgba(248, 250, 252, 0.72);
}

.file-browser__entry-table :deep(.file-browser__entry-row.is-selected .el-table__cell) {
  background: var(--zy-blue-50) !important;
  box-shadow: inset 0 1px 0 rgba(37, 99, 235, 0.12), inset 0 -1px 0 rgba(37, 99, 235, 0.12);
}

.file-browser__entry-table :deep(.file-browser__entry-row.is-selected .el-table__cell:first-child) {
  box-shadow:
    inset 3px 0 0 var(--zy-blue-500),
    inset 0 1px 0 rgba(37, 99, 235, 0.12),
    inset 0 -1px 0 rgba(37, 99, 235, 0.12);
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

.file-browser__name-cell.is-directory .el-icon {
  color: var(--zy-blue-600);
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

.file-browser__muted {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.file-browser__row-actions {
  display: inline-flex;
  gap: var(--zy-sp-1);
  align-items: center;
}

.file-browser__status-cell,
.file-browser__artifact-cell,
.file-browser__storage-cell,
.file-browser__diagnostic-cell {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.file-browser__status-cell span,
.file-browser__artifact-cell span,
.file-browser__storage-cell span,
.file-browser__diagnostic-cell span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.45;
}

.file-browser__artifact-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.file-browser__context-menu {
  position: fixed;
  z-index: 3000;
  display: grid;
  min-width: 240px;
  max-width: 300px;
  padding: 6px;
  border: 1px solid rgba(148, 163, 184, 0.36);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.18);
}

.file-browser__context-menu button {
  display: grid;
  gap: 2px;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: var(--zy-radius-sm);
  background: transparent;
  color: var(--zy-ink);
  cursor: pointer;
  font-family: inherit;
  font-size: var(--zy-fs-sm);
  text-align: left;
}

.file-browser__context-menu button:hover:not(:disabled) {
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
}

.file-browser__context-menu button.is-divided {
  margin-top: 5px;
  border-top: var(--zy-border-soft);
  border-radius: 0 0 var(--zy-radius-sm) var(--zy-radius-sm);
}

.file-browser__context-menu button.is-danger {
  color: #b91c1c;
}

.file-browser__context-menu button.is-disabled,
.file-browser__context-menu button:disabled {
  color: var(--zy-subtle);
  cursor: not-allowed;
}

.file-browser__context-menu span {
  font-weight: var(--zy-fw-medium);
}

.file-browser__context-menu small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.35;
}

.file-browser__model-placeholder {
  display: grid;
  gap: var(--zy-sp-3);
}

.file-browser__preview-fallback {
  display: grid;
  gap: var(--zy-sp-3);
}

.file-browser__fallback-link {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: fit-content;
  min-height: 34px;
  padding: 0 var(--zy-sp-4);
  border-radius: var(--zy-radius-sm);
  background: var(--el-color-primary);
  color: #fff;
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-medium);
  text-decoration: none;
}

.file-browser__fallback-link:hover {
  background: var(--el-color-primary-dark-2);
}

.file-browser__batch-summary {
  display: grid;
  gap: 4px;
  margin-bottom: var(--zy-sp-3);
  padding: var(--zy-sp-3);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
}

.file-browser__batch-summary strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
}

.file-browser__batch-summary span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.file-browser__batch-list {
  display: grid;
  gap: var(--zy-sp-2);
  max-height: 420px;
  overflow: auto;
}

.file-browser__batch-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: var(--zy-sp-2);
  align-items: center;
  padding: var(--zy-sp-3);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
}

.file-browser__batch-item > div {
  min-width: 0;
}

.file-browser__batch-item strong,
.file-browser__batch-item span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-browser__batch-item strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
}

.file-browser__batch-item span {
  margin-top: 3px;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.file-browser__ownership-cell {
  display: flex;
  min-width: 0;
  gap: 8px;
  align-items: center;
}

.file-browser__ownership-cell span:last-child {
  min-width: 0;
  overflow: hidden;
  color: var(--zy-text-soft);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-browser__ownership-link {
  min-width: 0;
  max-width: 130px;
  justify-content: flex-start;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

  .file-browser__selection-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .file-browser__selection-actions {
    justify-content: flex-start;
  }

  .file-browser__batch-item {
    grid-template-columns: 1fr;
  }
}
</style>
