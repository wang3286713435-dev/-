<template>
  <section class="mvp-page asset-page">
    <ProjectWorkspaceNav v-if="Number.isFinite(projectId)" :project-id="projectId" />

    <header class="asset-command-center">
      <div class="asset-command-center__copy">
        <span>数据管家工作台</span>
        <h1>{{ projectTitle }}</h1>
        <p>{{ projectSubTitle }}</p>
      </div>
      <div class="asset-command-center__meta">
        <el-tag type="info" effect="plain">{{ project?.assetSource || '内部资产' }}</el-tag>
        <el-tag :type="project?.assetStatus === 'ACTIVE' ? 'success' : 'info'" effect="plain">
          {{ project?.assetStatus || '未加载' }}
        </el-tag>
      </div>
      <div class="asset-command-center__actions">
        <el-button :icon="View" type="success" @click="goAgentGovernance">开始交付治理</el-button>
        <el-button :icon="ChatDotRound" type="primary" @click="hermesDrawerVisible = true">问 Hermes</el-button>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </header>

    <section class="asset-module-grid" aria-label="数据管家模块">
      <button
        v-for="item in moduleCards"
        :key="item.name"
        class="asset-module-card"
        type="button"
        @click="openModule(item)"
      >
        <span>{{ item.group }}</span>
        <strong>{{ item.label }}</strong>
        <em>{{ item.description }}</em>
      </button>
    </section>

    <el-tabs v-model="activeTab" class="asset-tabs">
      <el-tab-pane label="资产驾驶舱" name="dashboard">
        <section v-loading="loading" class="asset-dashboard-grid">
          <div class="asset-dashboard-panel asset-dashboard-panel--overview">
            <div class="asset-dashboard-panel__header">
              <div>
                <h2>资产总览</h2>
                <span>按当前项目实时汇总文件元数据</span>
              </div>
              <span>{{ formatDate(statistics?.lastUpdatedAt) }}</span>
            </div>
            <div class="asset-kpi-grid">
              <article v-for="item in cards" :key="item.label" class="asset-kpi">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <em>{{ item.unit }}</em>
              </article>
            </div>
          </div>

          <div class="asset-dashboard-panel asset-dashboard-panel--risks">
            <div class="asset-dashboard-panel__header">
              <div>
                <h2>治理风险</h2>
                <span>点击风险卡片进入文件筛选或任务视图</span>
              </div>
              <el-tag :type="riskSignalCount > 0 ? 'warning' : 'success'" effect="plain">
                {{ formatCount(riskSignalCount) }} 项
              </el-tag>
            </div>
            <div class="asset-risk-grid">
              <button
                v-for="risk in riskCards"
                :key="risk.key"
                class="asset-risk-card"
                type="button"
                @click="openRiskCard(risk)"
              >
                <span>{{ risk.label }}</span>
                <strong>{{ formatCount(risk.count) }}</strong>
                <em>{{ risk.helper }}</em>
              </button>
            </div>
          </div>

          <div class="asset-dashboard-panel">
            <div class="asset-dashboard-panel__header">
              <div>
                <h2>文件类型</h2>
                <span>按登记类型统计容量与数量</span>
              </div>
            </div>
            <div class="asset-bars">
              <article v-for="item in fileKindRows" :key="item.fileKind" class="asset-bar-row">
                <div>
                  <strong>{{ fileKindLabel(item.fileKind) }}</strong>
                  <span>{{ formatCount(item.fileCount) }} 份 / {{ formatBytes(item.totalSizeBytes) }}</span>
                </div>
                <div class="asset-bar-row__track">
                  <span :style="{ width: barWidth(item.totalSizeBytes, maxFileKindSize) }" />
                </div>
              </article>
              <el-empty v-if="fileKindRows.length === 0" description="暂无文件类型统计" :image-size="56" />
            </div>
          </div>

          <div class="asset-dashboard-panel">
            <div class="asset-dashboard-panel__header">
              <div>
                <h2>专业分布</h2>
                <span>辅助判断资产登记是否完整</span>
              </div>
            </div>
            <div class="asset-bars">
              <article v-for="item in disciplineRows" :key="item.discipline" class="asset-bar-row">
                <div>
                  <strong>{{ disciplineLabel(item.discipline) }}</strong>
                  <span>{{ formatCount(item.fileCount) }} 份 / {{ formatBytes(item.totalSizeBytes) }}</span>
                </div>
                <div class="asset-bar-row__track asset-bar-row__track--muted">
                  <span :style="{ width: barWidth(item.totalSizeBytes, maxDisciplineSize) }" />
                </div>
              </article>
              <el-empty v-if="disciplineRows.length === 0" description="暂无专业统计" :image-size="56" />
            </div>
          </div>

          <div class="asset-dashboard-panel">
            <div class="asset-dashboard-panel__header">
              <div>
                <h2>最近扫描</h2>
                <span>追踪 NAS 接管任务状态</span>
              </div>
              <el-button text @click="activeTab = 'scans'">查看全部</el-button>
            </div>
            <div class="asset-activity-list">
              <article v-for="item in recentScans" :key="item.id" class="asset-activity-item">
                <div>
                  <strong>任务 {{ item.id }} / {{ item.rootCode }}</strong>
                  <span>{{ scanProgressHint(item) }}</span>
                </div>
                <el-tag :type="scanStatusTag(item.status)" size="small">{{ item.status }}</el-tag>
              </article>
              <el-empty v-if="recentScans.length === 0" description="暂无扫描任务" :image-size="56" />
            </div>
          </div>

          <div class="asset-dashboard-panel">
            <div class="asset-dashboard-panel__header">
              <div>
                <h2>最近入库</h2>
                <span>最近登记或更新的文件资产</span>
              </div>
              <el-button text @click="activeTab = 'files'">进入文件管理</el-button>
            </div>
            <div class="asset-activity-list">
              <article v-for="item in recentFiles" :key="item.fileId" class="asset-activity-item">
                <div>
                  <strong>{{ item.fileName }}</strong>
                  <span>文件ID {{ item.fileId }} / {{ formatBytes(item.sizeBytes) }} / {{ formatDate(item.updatedAt) }}</span>
                </div>
                <el-tag size="small">{{ item.fileKind }}</el-tag>
              </article>
              <el-empty v-if="recentFiles.length === 0" description="暂无最近文件" :image-size="56" />
            </div>
          </div>

          <div class="asset-dashboard-panel asset-dashboard-panel--events">
            <div class="asset-dashboard-panel__header">
              <div>
                <h2>治理动态</h2>
                <span>最近审计与事件流记录</span>
              </div>
              <span>{{ formatDate(qualityOverview?.latestEventAt) }}</span>
            </div>
            <div class="asset-event-list">
              <article v-for="item in recentEvents" :key="item.id" class="asset-event-item">
                <strong>{{ item.actionCode }}</strong>
                <span>{{ item.summary || item.eventType }}</span>
                <em>{{ formatDate(item.createdAt) }}</em>
              </article>
              <el-empty v-if="recentEvents.length === 0" description="暂无治理动态" :image-size="56" />
            </div>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="文件管理" name="files">
        <AssetProjectFileBrowser
          v-if="Number.isFinite(projectId)"
          :key="`${projectId}-${fileBrowserRefreshKey}-${catalogInitialQualityIssue}`"
          :project-id="projectId"
          :root-label="projectRootLabel"
          :discipline-options="disciplineOptions"
          :initial-quality-issue="catalogInitialQualityIssue"
          :batch-checksum-creating="batchChecksumCreating"
          @open-preview="openPreviewById"
          @open-detail="openFileDetailById"
          @open-metadata="openMetadataById"
          @create-checksum="createChecksumById"
          @create-batch-checksum="createBatchChecksumForProject"
        />
        <el-empty v-else description="请先选择项目" :image-size="56" />
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
          <el-table-column label="失败原因" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ safePathText(row.failureReason) }}</template>
          </el-table-column>
          <el-table-column label="路径提示" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">{{ row.lastScannedPath ? '扫描路径已隐藏，仅保留任务状态。' : '-' }}</template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="路径映射" name="mappings">
        <el-table v-loading="loading" :data="pathMappings" class="master-table" empty-text="暂无路径映射">
          <el-table-column prop="providerCode" label="存储" width="120" />
          <el-table-column prop="matchStrategy" label="匹配方式" width="130" />
          <el-table-column label="路径提示" min-width="320" show-overflow-tooltip>
            <template #default="{ row }">{{ pathMappingHint(row) }}</template>
          </el-table-column>
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
                <el-tag :type="previewRiskTagType(currentPreview)">
                  {{ previewStatusLabel(currentPreview) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="预览方式">{{ previewModeLabel(currentPreview) }}</el-descriptions-item>
              <el-descriptions-item label="转换状态">{{ conversionStatusLabel(currentPreview) }}</el-descriptions-item>
              <el-descriptions-item label="后续处理">
                {{ previewOnlineStateText(currentPreview) }}
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
              :title="previewActionHint(currentPreview)"
              :type="previewRiskTagType(currentPreview)"
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
              <template v-if="selectedFile.storagePath">
                <el-tag type="info" size="small">底层路径已隐藏，请使用平台受控预览或下载入口</el-tag>
              </template>
              <template v-else>
                <el-tag type="info" size="small">路径已隐藏，请使用平台受控预览或下载入口</el-tag>
              </template>
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
          <el-button :icon="ChatDotRound" @click="openHermesForFile(selectedFile.fileId)">问 Hermes 助手</el-button>
        </section>
      </template>
    </el-drawer>

    <HermesWorkspaceDrawer v-model="hermesDrawerVisible">
      <DataStewardPanel
        v-if="Number.isFinite(projectId)"
        :project-id="projectId"
        page-type="project_detail"
        source-view="FileAssetView"
        :asset-id="hermesAssetId"
        :current-route="route.fullPath"
        :project-code="project?.code"
        :project-name="project?.name"
        page-title="项目工作台"
      />
      <el-empty v-else description="请先选择项目" :image-size="56" />
    </HermesWorkspaceDrawer>

    <el-dialog v-model="previewDialogVisible" title="文件预览状态" width="640px">
      <div v-loading="previewLoading" class="preview-dialog-body">
        <template v-if="selectedPreview">
          <div class="preview-state-panel">
            <el-tag :type="previewRiskTagType(selectedPreview)" size="large">
              {{ previewStatusLabel(selectedPreview) }}
            </el-tag>
            <div>
              <strong>{{ selectedPreview.fileName }}</strong>
              <span>{{ selectedPreview.projectCode }} {{ selectedPreview.projectName }}</span>
            </div>
          </div>
          <el-alert
            title="当前仅提供受控预览入口和转换状态判断，不读取文件正文，也不执行真实模型轻量化或 Office/CAD 转换。"
            type="info"
            show-icon
            :closable="false"
          />
          <el-descriptions class="preview-descriptions" :column="1" border size="small">
            <el-descriptions-item label="文件ID">{{ selectedPreview.fileId }}</el-descriptions-item>
            <el-descriptions-item label="文件类型">{{ selectedPreview.fileKind }} {{ selectedPreview.fileExt }}</el-descriptions-item>
            <el-descriptions-item label="预览方式">{{ previewModeLabel(selectedPreview) }}</el-descriptions-item>
            <el-descriptions-item label="转换状态">{{ conversionStatusLabel(selectedPreview) }}</el-descriptions-item>
            <el-descriptions-item label="是否可直接预览">
              {{ previewOnlineStateText(selectedPreview) }}
            </el-descriptions-item>
            <el-descriptions-item label="访问权限">{{ selectedPreview.accessPolicyMessage }}</el-descriptions-item>
            <el-descriptions-item label="业务提示">{{ previewActionHint(selectedPreview) }}</el-descriptions-item>
            <el-descriptions-item label="策略说明">{{ selectedPreview.message }}</el-descriptions-item>
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
import type { RouteRecordName } from 'vue-router';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ChatDotRound, Refresh, View } from '@element-plus/icons-vue';

import {
  createChecksumJob,
  createBatchChecksumJobs,
  fetchAssetDisciplines,
  fetchAssetPathMappings,
  fetchAssetProjects,
  fetchAssetQualityOverview,
  fetchAssetScanTasks,
  fetchAssetStatistics,
  fetchAssetJob,
  fetchCatalogFiles,
  fetchFileAsset,
  fetchFilePreview,
  createFileAccessTicket,
  retryAssetJob,
  updateFileAssetMetadata,
  type AssetJob,
  type AssetDiscipline,
  type AssetPathMapping,
  type AssetProject,
  type AssetQualityOverview,
  type AssetScanTask,
  type AssetStatistics,
  type CatalogFile,
  type FileAsset,
  type FilePreview
} from '@/modules/data-steward/api/dataSteward';
import AssetProjectFileBrowser from '@/modules/data-steward/components/AssetProjectFileBrowser.vue';
import DataStewardPanel from '@/modules/data-steward/components/DataStewardPanel.vue';
import HermesWorkspaceDrawer from '@/modules/data-steward/components/HermesWorkspaceDrawer.vue';
import {
  conversionStatusLabel,
  previewActionHint,
  previewActionLabel,
  previewModeLabel,
  previewOnlineStateText,
  previewRiskTagType,
  previewStatusLabel
} from '@/modules/data-steward/utils/previewStatus';
import ProjectWorkspaceNav from '@/modules/core/components/ProjectWorkspaceNav.vue';

const route = useRoute();
const router = useRouter();
const projectId = computed(() => Number(route.params.projectId));

const loading = ref(false);
const activeTab = ref('dashboard');
const project = ref<AssetProject | null>(null);
const statistics = ref<AssetStatistics | null>(null);
const qualityOverview = ref<AssetQualityOverview | null>(null);
const scanTasks = ref<AssetScanTask[]>([]);
const pathMappings = ref<AssetPathMapping[]>([]);
const disciplineOptions = ref<AssetDiscipline[]>([]);
const recentFiles = ref<CatalogFile[]>([]);
const detailDrawerVisible = ref(false);
const selectedFile = ref<FileAsset | null>(null);
const metadataDialogVisible = ref(false);
const metadataSaving = ref(false);
const checksumCreating = ref(false);
const batchChecksumCreating = ref(false);
const previewDialogVisible = ref(false);
const previewLoading = ref(false);
const accessActionLoading = ref<'PREVIEW' | 'DOWNLOAD' | null>(null);
const selectedPreview = ref<FilePreview | null>(null);
const hermesDrawerVisible = ref(false);
const hermesAssetId = ref<number | undefined>();
const checksumJobDialogVisible = ref(false);
const checksumJobLoading = ref(false);
const checksumJobRetrying = ref(false);
const selectedChecksumJob = ref<AssetJob | null>(null);
const selectedChecksumJobFile = ref<FileAsset | null>(null);
const fileBrowserRefreshKey = ref(0);
let pageLoadRequestId = 0;
let previewRequestId = 0;
let checksumJobRequestId = 0;
let checksumJobTimer: ReturnType<typeof window.setInterval> | null = null;
const metadataForm = reactive({
  fileId: undefined as number | undefined,
  fileKind: '',
  discipline: '',
  versionNo: '',
  confidenceLevel: '',
  reviewStatus: ''
});

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
const assetTabs = new Set(['dashboard', 'files', 'scans', 'mappings']);
const moduleCards: Array<{
  label: string;
  group: string;
  description: string;
  name?: RouteRecordName;
  tab?: string;
}> = [
  { label: '资产驾驶舱', group: '总览', description: '项目资产、容量、治理风险', tab: 'dashboard' },
  { label: '文件管理', group: '文件', description: '目录树、文件表、预览和治理', tab: 'files' },
  { label: '模型集成', group: '模型', description: '登记模型集成与发布状态', name: 'project-data-steward-models' },
  { label: '管理对象', group: '对象', description: '设备、系统和构件台账', name: 'project-data-steward-objects' },
  { label: '交付治理助手', group: '交付', description: '体检、缺失解释和人工确认补交', name: 'project-work-agent-governance' },
  { label: '事项列表', group: '治理', description: '缺项、低置信和失败扫描', name: 'project-data-steward-issues' },
  { label: '任务列表', group: '任务', description: '扫描、checksum 等后台任务', name: 'project-data-steward-tasks' },
  { label: '导出列表', group: '交付', description: '文件清单和报表导出', name: 'project-data-steward-exports' },
  { label: '文件服务', group: '服务', description: '预览、下载、权限与禁用写操作', name: 'project-data-steward-file-service' }
];
const projectTitle = computed(() => {
  if (!project.value) return `项目 ${projectId.value}`;
  return `${project.value.code} ${project.value.name}`;
});
const projectSubTitle = computed(() => {
  if (!project.value) return '资产明细';
  return [project.value.projectStage, project.value.projectManagerName].filter(Boolean).join(' / ') || '资产明细';
});
const projectRootLabel = computed(() => project.value?.name ?? `项目 ${projectId.value}`);
const detailTitle = computed(() => selectedFile.value ? `${selectedFile.value.fileName} - 文件详情` : '文件详情');
const currentPreview = computed(() => {
  if (!selectedFile.value || selectedPreview.value?.fileId !== selectedFile.value.fileId) return null;
  return selectedPreview.value;
});
const catalogInitialQualityIssue = computed(() => queryString(route.query.qualityIssue) ?? 'ALL');
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
    { label: '文档文件', value: formatCount(documentFileCount.value), unit: '份' },
    { label: '项目容量', value: formatBytes(item?.totalSizeBytes), unit: '已登记' },
    { label: '路径映射', value: formatCount(pathMappings.value.length), unit: '条' }
  ];
});
const projectScans = computed(() => {
  const code = project.value?.code;
  return scanTasks.value.filter((item) => item.projectId === projectId.value || (code && item.projectCode === code));
});
const recentScans = computed(() => [...projectScans.value]
  .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
  .slice(0, 5));
const recentEvents = computed(() => qualityOverview.value?.recentEvents?.slice(0, 6) ?? []);
const riskSignalCount = computed(() => Number(qualityOverview.value?.riskSignalCount ?? 0));
const riskCards = computed(() => {
  const item = qualityOverview.value;
  return [
    { key: 'missing-checksum', label: '缺 checksum', count: item?.missingChecksumCount ?? 0, helper: '影响重复识别', qualityIssue: 'MISSING_CHECKSUM', tab: 'files' },
    { key: 'missing-discipline', label: '专业待完善', count: item?.missingDisciplineCount ?? 0, helper: '影响筛选统计', qualityIssue: 'MISSING_DISCIPLINE', tab: 'files' },
    { key: 'missing-confidence', label: '缺置信度', count: item?.missingConfidenceCount ?? 0, helper: '需要人工确认', qualityIssue: 'MISSING_CONFIDENCE', tab: 'files' },
    { key: 'pending-review', label: '待审核候选', count: item?.pendingReviewCount ?? 0, helper: '扫描后未确认', tab: 'scans' },
    { key: 'failed-scan', label: '扫描失败', count: item?.failedScanCount ?? 0, helper: '需要排查路径', tab: 'scans' },
    { key: 'running-scan', label: '运行中任务', count: item?.runningScanCount ?? 0, helper: '后台处理中', tab: 'scans' }
  ];
});
const fileKindRows = computed(() => statistics.value?.byFileKind ?? []);
const disciplineRows = computed(() => statistics.value?.byDiscipline ?? []);
const maxFileKindSize = computed(() => Math.max(...fileKindRows.value.map((item) => item.totalSizeBytes), 0));
const maxDisciplineSize = computed(() => Math.max(...disciplineRows.value.map((item) => item.totalSizeBytes), 0));
const documentFileCount = computed(() => {
  const kinds = new Set(['DOCUMENT', 'SPREADSHEET', 'PRESENTATION', 'ARCHIVE']);
  return fileKindRows.value
    .filter((item) => kinds.has(item.fileKind))
    .reduce((sum, item) => sum + Number(item.fileCount ?? 0), 0);
});

watch(
  () => [route.params.projectId, route.query.qualityIssue, route.query.tab],
  () => {
    const nextTab = queryString(route.query.tab);
    if (nextTab && assetTabs.has(nextTab)) {
      activeTab.value = nextTab;
    }
    if (catalogInitialQualityIssue.value && catalogInitialQualityIssue.value !== 'ALL') {
      activeTab.value = 'files';
    }
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
    const [projectsResult, statisticsResult, qualityResult, scansResult, mappingsResult, disciplinesResult, recentFilesResult] =
      await Promise.allSettled([
      fetchAssetProjects(),
      fetchAssetStatistics(projectId.value),
      fetchAssetQualityOverview(projectId.value),
      fetchAssetScanTasks(),
      fetchAssetPathMappings(projectId.value),
      fetchAssetDisciplines(projectId.value),
      fetchCatalogFiles({ projectId: projectId.value, page: 1, pageSize: 6 })
    ]);
    if (requestId !== pageLoadRequestId) return;
    if (projectsResult.status === 'rejected' || statisticsResult.status === 'rejected') {
      throw projectsResult.status === 'rejected' ? projectsResult.reason : statisticsResult.reason;
    }
    const projects = projectsResult.value;
    project.value = projects.find((item) => item.projectId === projectId.value) ?? null;
    statistics.value = statisticsResult.value;
    qualityOverview.value = qualityResult.status === 'fulfilled' ? qualityResult.value : null;
    scanTasks.value = scansResult.status === 'fulfilled' ? scansResult.value : [];
    pathMappings.value = mappingsResult.status === 'fulfilled' ? mappingsResult.value : [];
    disciplineOptions.value = disciplinesResult.status === 'fulfilled' ? disciplinesResult.value : [];
    recentFiles.value = recentFilesResult.status === 'fulfilled' ? recentFilesResult.value.rows : [];
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '项目资产加载失败');
  } finally {
    if (requestId === pageLoadRequestId) {
      loading.value = false;
    }
  }
}

function resetProjectData() {
  project.value = null;
  statistics.value = null;
  qualityOverview.value = null;
  scanTasks.value = [];
  pathMappings.value = [];
  disciplineOptions.value = [];
  recentFiles.value = [];
  selectedFile.value = null;
  selectedPreview.value = null;
  selectedChecksumJob.value = null;
  selectedChecksumJobFile.value = null;
  detailDrawerVisible.value = false;
  previewDialogVisible.value = false;
  hermesDrawerVisible.value = false;
  hermesAssetId.value = undefined;
  checksumJobDialogVisible.value = false;
  stopChecksumJobPolling();
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
    ElMessage.warning(previewActionHint(preview) || preview.accessPolicyMessage || '当前文件暂不可预览');
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
    refreshFileBrowser();
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

async function createBatchChecksumForProject() {
  if (!Number.isFinite(projectId.value) || batchChecksumCreating.value) return;
  try {
    await ElMessageBox.confirm(
      '平台会为当前项目最多 500 个缺 checksum 文件创建后台补算任务。这个操作只读取 NAS 文件并写回指纹，不会修改、移动或删除原文件。建议在网络和 NAS 负载较低时执行。',
      '创建 checksum 补算任务',
      {
        confirmButtonText: '创建任务',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
  } catch {
    return;
  }

  batchChecksumCreating.value = true;
  try {
    const count = await createBatchChecksumJobs(projectId.value);
    if (count > 0) {
      ElMessage.success(`已创建 ${count} 个 checksum 后台任务`);
    } else {
      ElMessage.info('当前项目没有需要补算的 checksum 文件');
    }
    await loadPage();
    refreshFileBrowser();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量创建 checksum 任务失败');
  } finally {
    batchChecksumCreating.value = false;
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
  refreshFileBrowser();
}

function isTerminalJobStatus(status: string) {
  return ['SUCCEEDED', 'FAILED', 'CANCELED'].includes(status);
}

async function openPreviewById(fileId: number) {
  try {
    const file = await fetchFileAsset(fileId);
    await openPreview(file);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件资产加载失败');
  }
}

async function openFileDetailById(fileId: number) {
  try {
    const file = await fetchFileAsset(fileId);
    await openFileDetail(file);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件资产加载失败');
  }
}

async function openMetadataById(fileId: number) {
  try {
    const file = await fetchFileAsset(fileId);
    openMetadataDialog(file);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件资产加载失败');
  }
}

async function createChecksumById(fileId: number) {
  try {
    const file = await fetchFileAsset(fileId);
    await createChecksum(file);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件资产加载失败');
  }
}

function openRiskCard(risk: { tab: string; qualityIssue?: string }) {
  activeTab.value = risk.tab;
  if (risk.qualityIssue) {
    void router.replace({
      name: String(route.name),
      params: route.params,
      query: { ...route.query, qualityIssue: risk.qualityIssue }
    });
  }
}

function openModule(item: { name?: RouteRecordName; tab?: string }) {
  if (item.tab) {
    activeTab.value = item.tab;
    return;
  }
  if (item.name) {
    void router.push({ name: item.name, params: { projectId: projectId.value } });
  }
}

function goAgentGovernance() {
  void router.push({ name: 'project-work-agent-governance', params: { projectId: projectId.value } });
}

function openHermesForFile(fileId: number) {
  hermesAssetId.value = fileId;
  hermesDrawerVisible.value = true;
}

function pathMappingHint(row: AssetPathMapping) {
  const provider = row.providerCode || 'NAS';
  const strategy = row.matchStrategy || 'PREFIX';
  return `${provider}/${strategy}/底层路径已隐藏`;
}

function scanProgressHint(item: AssetScanTask) {
  const progress = safePathText(item.progressMessage);
  if (progress && progress !== '-') {
    return progress;
  }
  return item.lastScannedPath ? '扫描路径已隐藏，仅保留任务状态。' : '暂无进度说明';
}

function safePathText(value: string | null | undefined) {
  if (!value) return '-';
  if (/nas:\/\/|smb:\/\/|afp:\/\/|\/Volumes\/|\/Users\/|storage_path|storage_uri|storagePath|storageUri/i.test(value)) {
    return '底层路径已隐藏';
  }
  return value;
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

function refreshFileBrowser() {
  fileBrowserRefreshKey.value += 1;
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

function barWidth(value: number | null | undefined, max: number) {
  if (!max) return '0%';
  const percent = Math.max(4, Math.round((Number(value ?? 0) / max) * 100));
  return `${Math.min(100, percent)}%`;
}

function fileKindLabel(value: string | null | undefined) {
  const labels: Record<string, string> = {
    MODEL: '模型',
    DRAWING: '图纸',
    DOCUMENT: '文档',
    SPREADSHEET: '表格',
    PRESENTATION: '汇报',
    MODEL_VIEWER: '轻量化模型',
    ARCHIVE: '归档包',
    OTHER: '其他'
  };
  if (!value) return '未分类';
  return labels[value] ?? value;
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

.asset-command-center {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 16px;
  align-items: center;
  padding: 18px 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(20, 184, 166, 0.06)),
    #fbfdff;
}

.asset-command-center__copy {
  min-width: 0;
}

.asset-command-center__copy > span {
  display: inline-block;
  margin-bottom: 6px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.asset-command-center__copy h1 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.25;
}

.asset-command-center__copy p {
  margin: 6px 0 0;
  color: #64748b;
}

.asset-command-center__meta,
.asset-command-center__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.asset-tabs {
  min-width: 0;
  padding: 16px;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
}

.asset-module-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  min-width: 0;
}

.asset-module-card {
  min-width: 0;
  padding: 13px 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #ffffff;
  color: #0f172a;
  text-align: left;
  cursor: pointer;
}

.asset-module-card:hover {
  border-color: rgba(37, 99, 235, 0.36);
  background: #f8fbff;
}

.asset-module-card span,
.asset-module-card em {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.asset-module-card strong {
  display: block;
  margin: 5px 0 3px;
  overflow: hidden;
  color: #0f172a;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-dashboard-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px;
  min-width: 0;
}

.asset-dashboard-panel {
  grid-column: span 6;
  min-width: 0;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 8px;
  background: #fbfdff;
}

.asset-dashboard-panel--overview,
.asset-dashboard-panel--events {
  grid-column: span 12;
}

.asset-dashboard-panel__header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 14px;
}

.asset-dashboard-panel__header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 16px;
}

.asset-dashboard-panel__header span {
  color: #64748b;
  font-size: 12px;
}

.asset-kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.asset-kpi {
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  border-radius: 8px;
  background: #ffffff;
}

.asset-kpi span,
.asset-kpi em {
  display: block;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.asset-kpi strong {
  display: block;
  margin: 6px 0 4px;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.15;
  overflow-wrap: anywhere;
}

.asset-risk-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.asset-risk-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid rgba(245, 158, 11, 0.24);
  border-radius: 8px;
  background: #fffaf0;
  color: #92400e;
  text-align: left;
  cursor: pointer;
}

.asset-risk-card:hover {
  border-color: rgba(37, 99, 235, 0.32);
  background: #eff6ff;
  color: #1d4ed8;
}

.asset-risk-card span,
.asset-risk-card em {
  display: block;
  font-size: 12px;
  font-style: normal;
}

.asset-risk-card strong {
  display: block;
  margin: 6px 0 4px;
  color: #0f172a;
  font-size: 22px;
}

.asset-bars,
.asset-activity-list,
.asset-event-list {
  display: grid;
  gap: 10px;
}

.asset-bar-row {
  display: grid;
  gap: 7px;
}

.asset-bar-row > div:first-child,
.asset-activity-item {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

.asset-bar-row strong,
.asset-activity-item strong,
.asset-event-item strong {
  color: #0f172a;
  font-size: 13px;
}

.asset-bar-row span,
.asset-activity-item span,
.asset-event-item span,
.asset-event-item em {
  color: #64748b;
  font-size: 12px;
}

.asset-bar-row__track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
}

.asset-bar-row__track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #2563eb;
}

.asset-bar-row__track--muted {
  background: rgba(20, 184, 166, 0.12);
}

.asset-bar-row__track--muted span {
  background: #0f766e;
}

.asset-activity-item,
.asset-event-item {
  min-width: 0;
  padding: 10px 12px;
  border-radius: 8px;
  background: #ffffff;
}

.asset-activity-item > div,
.asset-event-item {
  min-width: 0;
}

.asset-activity-item strong,
.asset-activity-item span,
.asset-event-item strong,
.asset-event-item span,
.asset-event-item em {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-event-item {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr) 140px;
  gap: 12px;
  align-items: center;
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
  .asset-command-center {
    grid-template-columns: 1fr;
  }

  .asset-command-center__meta,
  .asset-command-center__actions {
    justify-content: flex-start;
  }

  .asset-kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .asset-module-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .asset-dashboard-panel {
    grid-column: span 12;
  }

  .asset-toolbar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .asset-kpi-grid,
  .asset-module-grid,
  .asset-risk-grid {
    grid-template-columns: 1fr;
  }

  .asset-event-item {
    grid-template-columns: 1fr;
  }

  .asset-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
