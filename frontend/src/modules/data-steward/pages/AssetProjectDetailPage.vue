<template>
  <section class="mvp-page asset-page">
    <header ref="commandRef" class="asset-command-center">
      <div class="zy-hero-lightfield" aria-hidden="true">
        <span></span>
        <span></span>
        <span></span>
      </div>
      <div class="zy-spotlight" aria-hidden="true"></div>
      <ParticleField :count="12" :speed="0.18" :link-distance="120" />
      <div class="asset-command-center__copy">
        <span>项目工作台</span>
        <h1>{{ projectTitle }}</h1>
        <p>{{ projectSubTitle }}</p>
      </div>
      <div class="asset-command-center__actions">
        <el-button type="primary" @click="openAssetTab('files')">文件管理</el-button>
        <el-button :icon="View" @click="openAssetTab('dashboard')">项目可视化</el-button>
        <el-button @click="openAssetTab('ownership')">工程树</el-button>
        <el-button @click="goDeliveryStatus">交付状态</el-button>
        <el-button @click="toggleProjectDetails">项目详情</el-button>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </header>

    <el-collapse v-model="projectDetailActive" class="asset-project-details">
      <el-collapse-item name="details">
        <template #title>
          <span class="asset-project-details__title">项目详情 / 技术信息</span>
          <small>平台内部 ID、资产来源、工作链路说明已收起</small>
        </template>
        <div class="asset-project-details__meta">
          <el-tag type="info" effect="plain">平台内部ID {{ projectId }}</el-tag>
          <el-tag type="info" effect="plain">{{ project?.assetSource || '内部资产' }}</el-tag>
          <el-tag :type="project?.assetStatus === 'ACTIVE' ? 'success' : 'info'" effect="plain">
            {{ project?.assetStatus || '未加载' }}
          </el-tag>
          <el-tag :type="masterDataReady ? 'success' : 'warning'" effect="plain">
            {{ masterDataReady ? '工程主数据已就绪' : '工程主数据待确认' }}
          </el-tag>
        </div>
        <section class="asset-workstream-strip" aria-label="项目工作区分区说明">
          <article
            v-for="item in workstreamCards"
            :key="item.label"
            :class="{ 'is-warning': item.phase === 'WORK_CENTER' && !masterDataReady }"
          >
            <small>{{ item.order }}</small>
            <span>{{ item.label }}</span>
            <strong>{{ item.title }}</strong>
            <em>{{ item.description }}</em>
            <b v-if="item.phase === 'WORK_CENTER'">{{ masterDataReady ? '已开放' : '需先确认工程主数据' }}</b>
          </article>
        </section>
      </el-collapse-item>
    </el-collapse>

    <section class="asset-next-actions asset-next-actions--core" aria-label="核心入口">
      <header>
        <div>
          <span class="zy-code-chip">CORE</span>
          <strong>核心入口</strong>
          <p>日常工作先从这三个入口开始。</p>
        </div>
        <el-tag :type="masterDataReady ? 'success' : 'warning'" effect="plain">
          {{ masterDataReady ? '交付状态可查看' : '交付需先确认主数据' }}
        </el-tag>
      </header>
      <div class="asset-next-actions__grid">
        <button
          v-for="item in primaryWorkspaceCards"
          :key="item.name ?? item.tab ?? item.label"
          class="asset-next-card"
          :class="{ 'is-gated': Boolean(item.requiresMasterData && !masterDataReady) }"
          type="button"
          @click="openModule(item)"
        >
          <span>{{ item.group }}</span>
          <strong>{{ item.label }}</strong>
          <em>{{ item.requiresMasterData && !masterDataReady ? '需先生成 / 确认工程主数据草案' : item.description }}</em>
        </button>
      </div>
    </section>

    <el-collapse v-model="moreToolsActive" class="asset-more-tools">
      <el-collapse-item name="tools">
        <template #title>
          <span class="asset-more-tools__title">更多工具</span>
          <small>工程主数据、模型集成、管理对象、文件服务和辅助入口</small>
        </template>
        <section class="asset-module-sections" aria-label="项目工作台更多工具">
          <article
            v-for="section in secondaryModuleSections"
            :key="section.phase"
            class="asset-module-section"
            :class="{ 'is-gated': section.phase === 'WORK_CENTER' && !masterDataReady }"
          >
            <header>
              <span>{{ section.order }}</span>
              <div>
                <strong>{{ section.label }}</strong>
                <small>{{ section.description }}</small>
              </div>
              <el-tag
                v-if="section.phase === 'WORK_CENTER'"
                size="small"
                :type="masterDataReady ? 'success' : 'warning'"
                effect="plain"
              >
                {{ masterDataReady ? '可进入交付' : '先确认主数据' }}
              </el-tag>
            </header>
            <div class="asset-module-grid">
              <button
                v-for="item in section.items"
                :key="item.name ?? item.tab ?? item.label"
                class="asset-module-card"
                :class="{ 'is-gated': Boolean(item.requiresMasterData && !masterDataReady) }"
                type="button"
                @click="openModule(item)"
              >
                <span>{{ item.group }}</span>
                <strong>{{ item.label }}</strong>
                <em>{{ item.requiresMasterData && !masterDataReady ? '需先生成 / 确认工程主数据草案' : item.description }}</em>
              </button>
            </div>
          </article>
        </section>
      </el-collapse-item>
    </el-collapse>

    <section v-if="!masterDataReady" class="asset-workspace-gate" aria-label="交付工作中心准入提示">
      <div>
        <strong>交付状态可以查看，但正式交付前需要先确认工程主数据</strong>
        <p>{{ masterDataGateText }}</p>
      </div>
      <div class="asset-workspace-gate__actions">
        <el-button type="primary" @click="goInitialization">确认工程主数据</el-button>
        <el-button @click="openModuleByName('project-master-data-deliverable-standard')">检查交付物标准</el-button>
      </div>
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
                  <span>平台资产ID {{ item.assetUuid || '-' }} / {{ formatBytes(item.sizeBytes) }} / {{ formatDate(item.updatedAt) }}</span>
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

      <el-tab-pane label="工程树可视化" name="ownership">
        <FileOwnershipTreePanel
          v-if="Number.isFinite(projectId)"
          :project-id="projectId"
          :active="activeTab === 'ownership'"
          :focus-node-path="ownershipFocusNodePath"
          @updated="handleOwnershipUpdated"
        />
        <el-empty v-else description="请先选择项目" :image-size="56" />
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
          :active="activeTab === 'files'"
          @open-preview="openPreviewById"
          @open-detail="openFileDetailById"
          @open-metadata="openMetadataById"
          @create-checksum="createChecksumById"
          @create-batch-checksum="createBatchChecksumForProject"
          @ask-hermes-ownership="openHermesForFile"
          @open-ownership-node="openOwnershipNodeFromFile"
        />
        <el-empty v-else description="请先选择项目" :image-size="56" />

        <section v-if="Number.isFinite(projectId)" class="asset-job-panel" data-m1e-checksum-jobs>
          <div class="asset-job-panel__header">
            <div>
              <h2>checksum 后台任务</h2>
              <span>默认收起，避免干扰文件目录。需要排查 checksum 时再展开查看任务编号、状态和失败原因。</span>
            </div>
            <div class="asset-job-panel__actions">
              <el-tag size="small" type="info" effect="plain">{{ checksumJobs.length }} 条</el-tag>
              <el-button size="small" @click="checksumJobsExpanded = !checksumJobsExpanded">
                {{ checksumJobsExpanded ? '收起任务' : '查看任务' }}
              </el-button>
              <el-button
                v-if="checksumJobsExpanded"
                size="small"
                :loading="checksumJobsLoading"
                @click="loadChecksumJobs(true)"
              >
                刷新任务
              </el-button>
            </div>
          </div>
          <el-collapse-transition>
            <div v-show="checksumJobsExpanded" class="asset-job-panel__body">
              <el-table
                v-loading="checksumJobsLoading"
                :data="checksumJobs"
                class="master-table asset-job-table"
                empty-text="暂无 checksum 后台任务"
              >
                <el-table-column label="后台任务编号" width="130">
                  <template #default="{ row }">#{{ row.id }}</template>
                </el-table-column>
                <el-table-column label="对应文件" min-width="260" show-overflow-tooltip>
                  <template #default="{ row }">
                    <div class="asset-job-file">
                      <strong>{{ checksumJobFileName(row) }}</strong>
                      <span>平台文件ID：{{ row.targetId || '-' }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="110">
                  <template #default="{ row }">
                    <el-tag :type="jobStatusTag(row.status)">{{ jobStatusLabel(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="进度" width="160">
                  <template #default="{ row }">
                    <el-progress :percentage="jobProgressValue(row)" :stroke-width="8" />
                  </template>
                </el-table-column>
                <el-table-column label="失败原因" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">{{ safePathText(row.failureReason) }}</template>
                </el-table-column>
                <el-table-column label="更新时间" width="170">
                  <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="150" fixed="right">
                  <template #default="{ row }">
                    <el-button size="small" text @click="openChecksumJob(row)">查看</el-button>
                    <el-button
                      v-if="row.status === 'FAILED'"
                      size="small"
                      text
                      type="primary"
                      :loading="checksumJobRetrying && selectedChecksumJob?.id === row.id"
                      @click="retryChecksumJob(row)"
                    >
                      重试
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-collapse-transition>
        </section>
      </el-tab-pane>

      <el-tab-pane label="扫描任务" name="scans">
        <el-table v-loading="loading" :data="projectScans" class="master-table" empty-text="暂无扫描任务">
          <el-table-column prop="id" label="扫描任务编号" width="120" />
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
            <el-descriptions-item label="平台资产ID">{{ selectedFile.assetUuid || '-' }}</el-descriptions-item>
            <el-descriptions-item label="文件名">{{ selectedFile.fileName }}</el-descriptions-item>
            <el-descriptions-item label="项目">{{ selectedFile.projectCode }} {{ selectedFile.projectName }}</el-descriptions-item>
            <el-descriptions-item label="内部文件ID">{{ selectedFile.fileId }}</el-descriptions-item>
            <el-descriptions-item label="项目平台内部ID">{{ selectedFile.projectId }}</el-descriptions-item>
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
            <el-descriptions-item label="文件位置提示">{{ filePathHint(selectedFile) }}</el-descriptions-item>
            <el-descriptions-item label="底层路径">
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
            <el-descriptions-item label="平台文件ID">{{ selectedPreview.fileId }}</el-descriptions-item>
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

    <el-dialog v-model="checksumJobDialogVisible" title="checksum 后台任务" width="680px" @closed="stopChecksumJobPolling">
      <div v-loading="checksumJobLoading" class="job-dialog-body">
        <template v-if="selectedChecksumJob">
          <div class="job-state-panel">
            <el-tag :type="jobStatusTag(selectedChecksumJob.status)" size="large">
              {{ jobStatusLabel(selectedChecksumJob.status) }}
            </el-tag>
            <div>
              <strong>后台任务编号 #{{ selectedChecksumJob.id }}</strong>
              <span>{{ checksumJobTargetLabel }}</span>
            </div>
          </div>
          <el-progress
            :percentage="jobProgressValue(selectedChecksumJob)"
            :status="selectedChecksumJob.status === 'FAILED' ? 'exception' : selectedChecksumJob.status === 'SUCCEEDED' ? 'success' : undefined"
          />
          <el-descriptions class="job-descriptions" :column="1" border size="small">
            <el-descriptions-item label="后台任务编号">#{{ selectedChecksumJob.id }}</el-descriptions-item>
            <el-descriptions-item label="对应文件">{{ checksumJobFileName(selectedChecksumJob) }}</el-descriptions-item>
            <el-descriptions-item label="平台文件ID">{{ selectedChecksumJob.targetId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="任务类型">{{ selectedChecksumJob.jobType }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ jobStatusLabel(selectedChecksumJob.status) }}</el-descriptions-item>
            <el-descriptions-item label="进度">
              {{ formatCount(selectedChecksumJob.progressCurrent) }} / {{ formatCount(selectedChecksumJob.progressTotal) }}
            </el-descriptions-item>
            <el-descriptions-item label="进度说明">{{ safePathText(selectedChecksumJob.progressMessage) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(selectedChecksumJob.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDate(selectedChecksumJob.updatedAt) }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ formatDate(selectedChecksumJob.startedAt) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ formatDate(selectedChecksumJob.completedAt) }}</el-descriptions-item>
          </el-descriptions>
          <el-alert
            v-if="selectedChecksumJob.status === 'FAILED'"
            class="job-message"
            :title="safePathText(selectedChecksumJob.failureReason) || '任务失败，但未返回失败原因'"
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
        <el-form-item label="平台文件ID">
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
  fetchAssetJobs,
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
import FileOwnershipTreePanel from '@/modules/data-steward/components/FileOwnershipTreePanel.vue';
import HermesWorkspaceDrawer from '@/modules/data-steward/components/HermesWorkspaceDrawer.vue';
import ParticleField from '@/modules/core/components/ParticleField.vue';
import { useSpotlight } from '@/modules/core/composables/useSpotlight';
import {
  conversionStatusLabel,
  previewActionHint,
  previewActionLabel,
  previewModeLabel,
  previewOnlineStateText,
  previewRiskTagType,
  previewStatusLabel
} from '@/modules/data-steward/utils/previewStatus';
import { fetchInitializationStatus, type InitializationStatus } from '@/modules/master-data/api/masterData';

const route = useRoute();
const router = useRouter();
const projectId = computed(() => Number(route.params.projectId));

const commandRef = ref<HTMLElement | null>(null);
useSpotlight(commandRef);

const loading = ref(false);
const activeTab = ref('dashboard');
const project = ref<AssetProject | null>(null);
const statistics = ref<AssetStatistics | null>(null);
const qualityOverview = ref<AssetQualityOverview | null>(null);
const scanTasks = ref<AssetScanTask[]>([]);
const pathMappings = ref<AssetPathMapping[]>([]);
const disciplineOptions = ref<AssetDiscipline[]>([]);
const initializationStatus = ref<InitializationStatus | null>(null);
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
const moreToolsActive = ref<string[]>([]);
const projectDetailActive = ref<string[]>([]);
const checksumJobDialogVisible = ref(false);
const checksumJobLoading = ref(false);
const checksumJobRetrying = ref(false);
const checksumJobsLoading = ref(false);
const checksumJobs = ref<AssetJob[]>([]);
const checksumJobsExpanded = ref(false);
const checksumJobFileMap = ref<Record<number, FileAsset>>({});
const selectedChecksumJob = ref<AssetJob | null>(null);
const selectedChecksumJobFile = ref<FileAsset | null>(null);
const fileBrowserRefreshKey = ref(0);
const ownershipFocusNodePath = ref('');
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
const assetTabs = new Set(['dashboard', 'ownership', 'files', 'scans', 'mappings']);
type WorkspacePhase = 'ASSET' | 'MASTER_DATA' | 'WORK_CENTER';
type ModuleCard = {
  label: string;
  group: string;
  description: string;
  phase: WorkspacePhase;
  name?: RouteRecordName;
  tab?: string;
  requiresMasterData?: boolean;
};

const workstreamCards: Array<{
  order: string;
  label: string;
  title: string;
  description: string;
  phase: WorkspacePhase;
}> = [
  { order: '01', label: '项目资产', title: '先看真实文件', description: '文件目录、文件列表、文件详情和受控预览。', phase: 'ASSET' },
  { order: '02', label: '工程主数据', title: '再定义交付规则', description: '基于资产线索确认项目结构和交付标准。', phase: 'MASTER_DATA' },
  { order: '03', label: '交付工作中心', title: '最后按规则交付', description: '文档/图纸补交、人工审核、整改和 dry-run 预检查。', phase: 'WORK_CENTER' }
];
const moduleCards: ModuleCard[] = [
  { label: '项目可视化', group: '项目资产', description: '资产驾驶舱、容量、风险和数据分布', phase: 'ASSET', tab: 'dashboard' },
  { label: '文件管理', group: '项目资产', description: '目录树、文件表、预览和治理', phase: 'ASSET', tab: 'files' },
  { label: '工程树可视化', group: '工程主数据', description: '查看每个文件归属到哪个工程节点', phase: 'MASTER_DATA', tab: 'ownership' },
  { label: '模型集成', group: '项目资产', description: '登记模型集成与发布状态', phase: 'ASSET', name: 'project-data-steward-models' },
  { label: '管理对象', group: '项目资产', description: '对象与模型集成登记', phase: 'ASSET', name: 'project-data-steward-objects' },
  { label: '文件服务', group: '项目资产', description: '预览、下载权限与禁用写操作', phase: 'ASSET', name: 'project-data-steward-file-service' },
  { label: '对象存储', group: '项目资产', description: '查看镜像任务、对象化状态和受控迁移', phase: 'ASSET', name: 'project-data-steward-storage-migration' },
  { label: '初始化向导', group: '工程主数据', description: '从目录线索生成主数据草案', phase: 'MASTER_DATA', name: 'project-master-data-initialization' },
  { label: '部位树', group: '工程主数据', description: '维护楼栋、楼层、房间和系统部位', phase: 'MASTER_DATA', name: 'project-master-data-sections' },
  { label: '节点类型', group: '工程主数据', description: '确认部位层级和锁定规则', phase: 'MASTER_DATA', name: 'project-master-data-node-types' },
  { label: '交付物标准', group: '工程主数据', description: '配置应交项、类型和验收口径', phase: 'MASTER_DATA', name: 'project-master-data-deliverable-standard' },
  { label: '交付治理助手', group: '交付工作中心', description: '辅助查看当前交付缺口，不作为必经入口', phase: 'WORK_CENTER', name: 'project-work-agent-governance', requiresMasterData: true },
  { label: '文档交付', group: '交付工作中心', description: '查看文档应交项、挂接和预检查', phase: 'WORK_CENTER', name: 'project-work-document-delivery', requiresMasterData: true },
  { label: '图纸交付', group: '交付工作中心', description: '查看图纸交付状态和缺失原因', phase: 'WORK_CENTER', name: 'project-work-drawing-delivery', requiresMasterData: true },
  { label: '整改闭环', group: '交付工作中心', description: '跟踪审核驳回、整改和复核', phase: 'WORK_CENTER', name: 'project-work-rectifications', requiresMasterData: true },
  { label: '交付包 / 档案目录', group: '交付工作中心', description: '生成只读草案、档案目录和清单导出', phase: 'WORK_CENTER', name: 'project-work-delivery-package', requiresMasterData: true },
  { label: '交付状态', group: '交付工作中心', description: '查看交付数量、绑定、审核和整改状态', phase: 'WORK_CENTER', name: 'project-work-dashboard', requiresMasterData: true }
];

const primaryWorkspaceKeys = new Set([
  'dashboard',
  'files',
  'ownership',
  'project-work-dashboard'
]);

const primaryWorkspaceCards = computed(() =>
  moduleCards.filter((item) => primaryWorkspaceKeys.has(moduleCardKey(item)))
);

const moduleSections = computed(() => [
  {
    phase: 'ASSET' as WorkspacePhase,
    order: '01',
    label: '项目资产',
    description: '看真实文件、目录、文件详情和治理风险。',
    items: moduleCards.filter((item) => item.phase === 'ASSET')
  },
  {
    phase: 'MASTER_DATA' as WorkspacePhase,
    order: '02',
    label: '工程主数据',
    description: '基于资产线索确认项目结构、节点规则和交付标准。',
    items: moduleCards.filter((item) => item.phase === 'MASTER_DATA')
  },
  {
    phase: 'WORK_CENTER' as WorkspacePhase,
    order: '03',
    label: '交付工作中心',
    description: '工作中心不是工程主数据子页面，它在主数据之后按规则推进交付。',
    items: moduleCards.filter((item) => item.phase === 'WORK_CENTER')
  }
]);

const secondaryModuleSections = computed(() =>
  moduleSections.value
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => !primaryWorkspaceKeys.has(moduleCardKey(item)))
    }))
    .filter((section) => section.items.length > 0)
);
const projectTitle = computed(() => {
  if (!project.value) return `项目 ${projectId.value}`;
  return `${project.value.code} ${project.value.name}`;
});
const projectSubTitle = computed(() => {
  if (!project.value) return '资产明细';
  return [
    projectSourceLabel(project.value),
    onboardingStatusLabel(project.value.onboardingStatus),
    project.value.projectStage,
    project.value.projectManagerName ? `负责人：${project.value.projectManagerName}` : ''
  ].filter(Boolean).join(' / ') || '资产明细';
});
const masterDataReady = computed(() =>
  Boolean(initializationStatus.value?.ready || initializationStatus.value?.standardStatus?.deliverableStandardReady)
);
const masterDataGateText = computed(() => {
  const blockers = initializationStatus.value?.blockers ?? [];
  if (blockers.length > 0) {
    return `请先生成 / 确认工程主数据草案，并处理：${blockers.slice(0, 3).join('、')}。`;
  }
  return '请先完成真实项目接入评估、工程主数据草案确认、部位树、节点类型和交付物标准，再进入正常交付。';
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
  const job = selectedChecksumJob.value;
  if (file) return `${file.fileName} / 平台资产ID ${file.assetUuid || '-'}`;
  if (job?.targetId) return `${checksumJobFileName(job)} / 内部文件ID ${job.targetId}`;
  return '文件资产 checksum 计算';
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
  () => [route.params.projectId, route.query.qualityIssue, route.query.tab, route.query.ownershipNode],
  () => {
    const nextTab = queryString(route.query.tab);
    if (nextTab && assetTabs.has(nextTab)) {
      activeTab.value = nextTab;
    }
    ownershipFocusNodePath.value = queryString(route.query.ownershipNode) ?? '';
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
    const [projectsResult, statisticsResult, qualityResult, scansResult, mappingsResult, disciplinesResult, recentFilesResult, initStatusResult] =
      await Promise.allSettled([
      fetchAssetProjects(),
      fetchAssetStatistics(projectId.value),
      fetchAssetQualityOverview(projectId.value),
      fetchAssetScanTasks(),
      fetchAssetPathMappings(projectId.value),
      fetchAssetDisciplines(projectId.value),
      fetchCatalogFiles({ projectId: projectId.value, page: 1, pageSize: 6 }),
      fetchInitializationStatus(projectId.value)
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
    initializationStatus.value = initStatusResult.status === 'fulfilled' ? initStatusResult.value : null;
    void loadChecksumJobs(false);
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
  initializationStatus.value = null;
  recentFiles.value = [];
  selectedFile.value = null;
  selectedPreview.value = null;
  selectedChecksumJob.value = null;
  selectedChecksumJobFile.value = null;
  checksumJobs.value = [];
  checksumJobFileMap.value = {};
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

async function loadChecksumJobs(showError = false) {
  if (!Number.isFinite(projectId.value) || checksumJobsLoading.value) return;
  checksumJobsLoading.value = true;
  try {
    const jobs = await fetchAssetJobs({ projectId: projectId.value, jobType: 'CHECKSUM_CALC', limit: 8 });
    checksumJobs.value = jobs;
    await hydrateChecksumJobFiles(jobs);
  } catch (error) {
    if (showError) {
      ElMessage.error(error instanceof Error ? error.message : 'checksum 后台任务加载失败');
    }
  } finally {
    checksumJobsLoading.value = false;
  }
}

async function hydrateChecksumJobFiles(jobs: AssetJob[]) {
  const nextMap = { ...checksumJobFileMap.value };
  const targetIds = Array.from(new Set(
    jobs
      .map((job) => job.targetId)
      .filter((targetId): targetId is number => targetId !== null && targetId !== undefined && Number.isFinite(Number(targetId)))
  )).slice(0, 8);
  await Promise.all(targetIds.map(async (fileId) => {
    if (nextMap[fileId]) return;
    try {
      nextMap[fileId] = await fetchFileAsset(fileId);
    } catch {
      // 任务仍可展示，文件名拿不到时退回内部文件 ID。
    }
  }));
  checksumJobFileMap.value = nextMap;
}

function checksumJobFileName(job: AssetJob) {
  const targetId = Number(job.targetId ?? 0);
  const file = targetId ? checksumJobFileMap.value[targetId] : null;
  return file?.fileName ?? (targetId ? `内部文件ID ${targetId}` : '未绑定文件');
}

function openChecksumJob(job: AssetJob) {
  selectedChecksumJob.value = job;
  selectedChecksumJobFile.value = job.targetId ? checksumJobFileMap.value[Number(job.targetId)] ?? null : null;
  checksumJobDialogVisible.value = true;
  startChecksumJobPolling(job.id);
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
    void loadChecksumJobs(false);
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
      await loadChecksumJobs(false);
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
    if (job.targetId && !selectedChecksumJobFile.value) {
      selectedChecksumJobFile.value = checksumJobFileMap.value[Number(job.targetId)] ?? null;
    }
    checksumJobs.value = checksumJobs.value.map((item) => item.id === job.id ? job : item);
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

async function retryChecksumJob(row?: AssetJob) {
  const job = row ?? selectedChecksumJob.value;
  if (!job || checksumJobRetrying.value) return;
  selectedChecksumJob.value = job;
  checksumJobRetrying.value = true;
  try {
    await retryAssetJob(job.id);
    ElMessage.success('checksum 任务已重新提交');
    await loadChecksumJobs(false);
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
  await loadChecksumJobs(false);
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
  openAssetTab(risk.tab);
  if (risk.qualityIssue) {
    void router.replace({
      name: String(route.name),
      params: route.params,
      query: { ...route.query, qualityIssue: risk.qualityIssue }
    });
  }
}

function openModule(item: ModuleCard) {
  if (item.tab) {
    openAssetTab(item.tab);
    return;
  }
  if (item.requiresMasterData && !masterDataReady.value) {
    ElMessage.warning('请先生成 / 确认工程主数据草案；交付工作中心页面会保留阻塞提示。');
  }
  if (item.name) {
    void router.push({ name: item.name, params: { projectId: projectId.value } });
  }
}

function moduleCardKey(item: ModuleCard) {
  return String(item.name ?? item.tab ?? item.label);
}

function openModuleByName(name: RouteRecordName) {
  void router.push({ name, params: { projectId: projectId.value } });
}

function openAssetTab(tab: string) {
  activeTab.value = tab;
  void router.replace({
    name: 'data-steward-asset-detail',
    params: { projectId: projectId.value },
    query: { ...route.query, tab }
  });
}

function openOwnershipNodeFromFile(nodePath: string) {
  ownershipFocusNodePath.value = nodePath;
  activeTab.value = 'ownership';
  const { qualityIssue: _qualityIssue, ...nextQuery } = route.query;
  void router.replace({
    name: 'data-steward-asset-detail',
    params: { projectId: projectId.value },
    query: { ...nextQuery, tab: 'ownership', ownershipNode: nodePath }
  });
}

function goDeliveryStatus() {
  goWorkCenterByName('project-work-dashboard');
}

function goWorkCenterByName(name: RouteRecordName) {
  if (!masterDataReady.value) {
    ElMessage.warning('请先生成 / 确认工程主数据草案；工作中心页面会保留阻塞提示。');
  }
  void router.push({ name, params: { projectId: projectId.value } });
}

function goInitialization() {
  void router.push({ name: 'project-master-data-initialization', params: { projectId: projectId.value } });
}

function toggleProjectDetails() {
  projectDetailActive.value = projectDetailActive.value.length ? [] : ['details'];
}

function openHermesForFile(fileId: number) {
  hermesAssetId.value = fileId;
  hermesDrawerVisible.value = true;
}

function handleOwnershipUpdated() {
  refreshFileBrowser();
  void loadPage();
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
  if (containsForbiddenPathText(value)) {
    if (value.includes('文件不存在')) return '文件不存在，底层路径已隐藏';
    return '底层路径已隐藏';
  }
  return value;
}

function containsForbiddenPathText(value: string) {
  return /nas:\/\/|smb:\/\/|afp:\/\/|\/Volumes\/|\/Users\/|\/tmp\/|\/private\/|\/var\/|storage_path|storage_uri|storagePath|storageUri/i.test(value);
}

function filePathHint(file: FileAsset) {
  const raw = file.logicalPath || file.storagePath || '';
  if (!raw || containsForbiddenPathText(raw) || raw.startsWith('/')) {
    return `path_hint: ${file.projectCode} 项目内已登记文件，底层目录不在前端展示`;
  }
  return `path_hint: ${raw}`;
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

function projectSourceLabel(row: AssetProject) {
  if (row.projectCategory === 'REAL_NAS_PROJECT' || row.projectSource === 'REAL_NAS') return '真实 NAS 项目';
  if (row.projectCategory === 'TEST_PROJECT') return '测试项目';
  if (row.projectCategory === 'SAMPLE_TEMPLATE') return '样例/模板项目';
  if (row.projectCategory === 'ARCHIVED_HISTORY') return '归档项目';
  return row.assetSource || row.projectSource || '内部资产';
}

function onboardingStatusLabel(value: string | null | undefined) {
  const labels: Record<string, string> = {
    PATH_MAPPED: '已映射路径',
    ASSETS_REGISTERED: '资产已登记',
    MASTERDATA_INITIALIZED: '主数据已初始化',
    GOVERNANCE_READY: '交付治理就绪'
  };
  return value ? (labels[value] ?? value) : '接入状态待维护';
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

/* ---- Command center / 项目工作台标题区 ---- */
.asset-command-center {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--zy-sp-4);
  align-items: center;
  padding: var(--zy-sp-5) var(--zy-sp-6);
  border: 1px solid color-mix(in srgb, var(--zy-line) 72%, transparent);
  border-radius: var(--zy-radius-lg);
  background: var(--zy-panel-tint);
  -webkit-backdrop-filter: blur(14px) saturate(1.04);
  backdrop-filter: blur(14px) saturate(1.04);
  box-shadow: var(--zy-shadow-soft);
  position: relative;
  overflow: hidden;
  isolation: isolate;
}

@supports not ((-webkit-backdrop-filter: blur(1px)) or (backdrop-filter: blur(1px))) {
  .asset-command-center {
    background: var(--zy-surface);
  }
}

.asset-command-center::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--zy-blue-500);
  z-index: 1;
}

.asset-command-center::after {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(37, 99, 235, 0.045) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(37, 99, 235, 0.045) 1px, transparent 1px);
  background-size: 32px 32px;
  pointer-events: none;
  mask-image: linear-gradient(to right, black 0%, transparent 65%);
  opacity: 0.7;
  z-index: 0;
}

.asset-command-center__copy,
.asset-command-center__meta,
.asset-command-center__actions {
  position: relative;
  z-index: 2;
}

.asset-command-center__copy {
  min-width: 0;
}

.asset-command-center__copy > span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  padding: 2px 8px;
  border-radius: var(--zy-radius-sm);
  background: var(--zy-blue-50);
  border: 1px solid rgba(37, 99, 235, 0.18);
  color: var(--zy-blue-700);
  font-size: 11px;
  font-weight: var(--zy-fw-semi);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  line-height: 1.2;
}

.asset-command-center__copy h1 {
  margin: 0;
  color: var(--zy-ink);
  font-size: var(--zy-fs-3xl);
  font-weight: var(--zy-fw-semi);
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.asset-command-center__copy p {
  margin: 6px 0 0;
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
  line-height: 1.65;
}

.asset-command-center__meta,
.asset-command-center__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  justify-content: flex-end;
}

/* ---- Project details / 技术信息折叠 ---- */
.asset-project-details {
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
  overflow: hidden;
}

.asset-project-details :deep(.el-collapse),
.asset-project-details :deep(.el-collapse-item__wrap) {
  border: 0;
}

.asset-project-details :deep(.el-collapse-item__header) {
  min-height: 46px;
  padding: 0 var(--zy-sp-4);
  border: 0;
}

.asset-project-details :deep(.el-collapse-item__content) {
  display: grid;
  gap: var(--zy-sp-3);
  padding: 0 var(--zy-sp-4) var(--zy-sp-4);
}

.asset-project-details__title {
  margin-right: var(--zy-sp-2);
  color: var(--zy-ink);
  font-weight: var(--zy-fw-semi);
}

.asset-project-details small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-regular);
}

.asset-project-details__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
}

/* ---- 三段工作流条 ---- */
.asset-workstream-strip {
  display: grid;
  grid-template-columns: 1.1fr 1fr 1.15fr;
  gap: var(--zy-sp-3);
  min-width: 0;
}

.asset-workstream-strip article {
  min-width: 0;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  transition: border-color var(--zy-duration-2) var(--zy-ease);
}

.asset-workstream-strip article:hover {
  border-color: var(--zy-line);
}

.asset-workstream-strip article.is-warning {
  border-color: rgba(245, 158, 11, 0.3);
  background: var(--zy-amber-50);
}

.asset-workstream-strip small {
  display: block;
  color: var(--zy-blue-700);
  font-family: var(--zy-font-mono);
  font-size: 11px;
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0;
}

.asset-workstream-strip span,
.asset-workstream-strip em {
  display: block;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
  line-height: 1.55;
}

.asset-workstream-strip strong {
  display: block;
  margin: 6px 0 4px;
  color: var(--zy-ink);
  font-size: var(--zy-fs-md);
  font-weight: var(--zy-fw-semi);
}

.asset-workstream-strip b {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  padding: 2px 8px;
  border-radius: var(--zy-radius-sm);
  background: rgba(245, 158, 11, 0.14);
  color: #92400e;
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

/* ---- Tabs ---- */
.asset-tabs {
  min-width: 0;
  padding: var(--zy-sp-5);
  background: var(--zy-surface);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  box-shadow: var(--zy-shadow-xs);
}

.asset-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: var(--zy-line-soft);
}

.asset-tabs :deep(.el-tabs__item) {
  color: var(--zy-muted);
  font-weight: var(--zy-fw-medium);
  transition: color var(--zy-duration-2) var(--zy-ease);
}

.asset-tabs :deep(.el-tabs__item.is-active) {
  color: var(--zy-blue-700);
  font-weight: var(--zy-fw-semi);
}

.asset-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--zy-blue-500);
  height: 2px;
}

/* ---- 后台任务面板 ---- */
.asset-job-panel {
  display: grid;
  gap: var(--zy-sp-2);
  margin-top: var(--zy-sp-4);
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
}

.asset-job-panel__header {
  display: flex;
  gap: var(--zy-sp-3);
  align-items: center;
  justify-content: space-between;
}

.asset-job-panel__header h2 {
  margin: 0;
  color: var(--zy-ink);
  font-size: var(--zy-fs-md);
  font-weight: var(--zy-fw-semi);
}

.asset-job-panel__header span {
  display: block;
  margin-top: 3px;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.asset-job-panel__actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  justify-content: flex-end;
}

.asset-job-panel__body {
  min-width: 0;
  padding-top: var(--zy-sp-2);
  border-top: var(--zy-border-soft);
}

.asset-job-table {
  width: 100%;
}

.asset-job-file {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.asset-job-file strong,
.asset-job-file span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-job-file strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.asset-job-file span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

/* ---- 工作中心 gate ---- */
.asset-workspace-gate {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zy-sp-4);
  min-width: 0;
  padding: var(--zy-sp-3) var(--zy-sp-5);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-left: 3px solid var(--zy-amber-500);
  border-radius: var(--zy-radius-base);
  background: var(--zy-amber-50);
}

.asset-workspace-gate strong {
  display: block;
  color: #78350f;
  font-weight: var(--zy-fw-semi);
  line-height: 1.5;
}

.asset-workspace-gate p {
  margin: 4px 0 0;
  color: #92400e;
  font-size: var(--zy-fs-sm);
  line-height: 1.65;
}

.asset-workspace-gate__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--zy-sp-2);
}

/* ---- 模块分组 ---- */
.asset-next-actions {
  display: grid;
  gap: var(--zy-sp-3);
  min-width: 0;
  padding: var(--zy-sp-4) var(--zy-sp-5);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
}

.asset-next-actions > header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--zy-sp-4);
  min-width: 0;
  padding-bottom: var(--zy-sp-3);
  border-bottom: var(--zy-border-soft);
}

.asset-next-actions > header > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.asset-next-actions > header strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-lg);
  font-weight: var(--zy-fw-semi);
  line-height: 1.4;
}

.asset-next-actions > header p {
  margin: 0;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.6;
}

.asset-next-actions__grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--zy-sp-2);
  min-width: 0;
}

.asset-next-actions--core .asset-next-actions__grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.asset-next-card {
  appearance: none;
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
  color: inherit;
  cursor: pointer;
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: var(--zy-sp-4);
  position: relative;
  text-align: left;
  transition:
    border-color var(--zy-duration-2) var(--zy-ease),
    background var(--zy-duration-2) var(--zy-ease),
    transform var(--zy-duration-2) var(--zy-ease);
}

.asset-next-card:hover {
  background: var(--zy-blue-50);
  border-color: rgba(37, 99, 235, 0.32);
  transform: translateY(-1px);
}

.asset-next-card.is-gated {
  background: var(--zy-amber-50);
  border-color: rgba(245, 158, 11, 0.28);
}

.asset-next-card span,
.asset-next-card em {
  color: var(--zy-muted);
  display: block;
  font-size: var(--zy-fs-xs);
  font-style: normal;
  line-height: 1.55;
}

.asset-next-card span {
  color: var(--zy-blue-700);
  font-family: var(--zy-font-mono);
  font-size: 11px;
  font-weight: var(--zy-fw-bold);
}

.asset-next-card strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-base);
  font-weight: var(--zy-fw-semi);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-more-tools {
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
  overflow: hidden;
}

.asset-more-tools :deep(.el-collapse),
.asset-more-tools :deep(.el-collapse-item__wrap) {
  border: 0;
}

.asset-more-tools :deep(.el-collapse-item__header) {
  min-height: 54px;
  padding: 0 var(--zy-sp-5);
  border: 0;
  color: var(--zy-ink);
  font-weight: var(--zy-fw-semi);
}

.asset-more-tools :deep(.el-collapse-item__content) {
  padding: 0 var(--zy-sp-5) var(--zy-sp-5);
}

.asset-more-tools__title {
  margin-right: var(--zy-sp-2);
}

.asset-more-tools small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-regular);
}

.asset-module-sections {
  display: grid;
  gap: var(--zy-sp-3);
  min-width: 0;
}

.asset-module-section {
  min-width: 0;
  padding: var(--zy-sp-4) var(--zy-sp-5);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
}

.asset-module-section.is-gated {
  border-color: rgba(245, 158, 11, 0.26);
  background: var(--zy-amber-50);
}

.asset-module-section > header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: var(--zy-sp-3);
  align-items: center;
  margin-bottom: var(--zy-sp-3);
  padding-bottom: var(--zy-sp-3);
  border-bottom: 1px dashed var(--zy-line-soft);
}

.asset-module-section > header > span {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: var(--zy-radius-sm);
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
  font-family: var(--zy-font-mono);
  font-size: 11px;
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0;
}

.asset-module-section > header strong {
  display: block;
  color: var(--zy-ink);
  font-size: var(--zy-fs-md);
  font-weight: var(--zy-fw-semi);
  line-height: 1.4;
}

.asset-module-section > header small,
.asset-module-section > p {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.6;
}

.asset-module-section > p {
  margin: -2px 0 var(--zy-sp-3);
}

.asset-module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: var(--zy-sp-2);
  min-width: 0;
}

.asset-module-card {
  position: relative;
  min-width: 0;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  color: var(--zy-ink);
  text-align: left;
  cursor: pointer;
  font-family: inherit;
  transition:
    border-color var(--zy-duration-2) var(--zy-ease),
    background var(--zy-duration-2) var(--zy-ease),
    transform var(--zy-duration-2) var(--zy-ease);
}

.asset-module-card:hover {
  border-color: rgba(37, 99, 235, 0.36);
  background: var(--zy-surface-soft);
}

.asset-module-card::after {
  content: "→";
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--zy-subtle);
  font-family: var(--zy-font-mono);
  font-size: var(--zy-fs-base);
  opacity: 0;
  transition: opacity var(--zy-duration-2) var(--zy-ease);
}

.asset-module-card:hover::after {
  opacity: 0.8;
}

.asset-module-card.is-gated {
  border-color: rgba(245, 158, 11, 0.28);
  background: var(--zy-amber-50);
}

.asset-module-card span,
.asset-module-card em {
  display: block;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
}

.asset-module-card span {
  font-family: var(--zy-font-mono);
  font-size: 11px;
  letter-spacing: 0;
  text-transform: uppercase;
  color: var(--zy-blue-600);
}

.asset-module-card strong {
  display: block;
  margin: 5px 0 3px;
  overflow: hidden;
  color: var(--zy-ink);
  font-size: var(--zy-fs-base);
  font-weight: var(--zy-fw-semi);
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .asset-next-actions__grid,
  .asset-next-actions--core .asset-next-actions__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .asset-next-actions > header {
    align-items: flex-start;
    flex-direction: column;
  }

  .asset-next-actions__grid,
  .asset-next-actions--core .asset-next-actions__grid {
    grid-template-columns: 1fr;
  }
}

/* ---- 资产驾驶舱网格 ---- */
.asset-dashboard-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: var(--zy-sp-3);
  min-width: 0;
}

.asset-dashboard-panel {
  grid-column: span 6;
  min-width: 0;
  padding: var(--zy-sp-5);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
}

.asset-dashboard-panel--overview,
.asset-dashboard-panel--events {
  grid-column: span 12;
}

.asset-dashboard-panel__header {
  display: flex;
  gap: var(--zy-sp-3);
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--zy-sp-4);
}

.asset-dashboard-panel__header h2 {
  margin: 0;
  color: var(--zy-ink);
  font-size: var(--zy-fs-lg);
  font-weight: var(--zy-fw-semi);
  letter-spacing: -0.01em;
}

.asset-dashboard-panel__header span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.asset-kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--zy-sp-2);
}

.asset-kpi {
  min-width: 0;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
}

.asset-kpi span,
.asset-kpi em {
  display: block;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
}

.asset-kpi strong {
  display: block;
  margin: 6px 0 4px;
  color: var(--zy-ink);
  font-size: var(--zy-fs-3xl);
  font-weight: var(--zy-fw-bold);
  line-height: 1.1;
  letter-spacing: -0.02em;
  overflow-wrap: anywhere;
  font-variant-numeric: tabular-nums;
}

.asset-risk-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--zy-sp-2);
}

.asset-risk-card {
  position: relative;
  min-width: 0;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border: 1px solid rgba(245, 158, 11, 0.28);
  border-left: 3px solid var(--zy-amber-500);
  border-radius: var(--zy-radius-base);
  background: var(--zy-amber-50);
  color: #92400e;
  text-align: left;
  cursor: pointer;
  font-family: inherit;
  transition:
    border-color var(--zy-duration-2) var(--zy-ease),
    background var(--zy-duration-2) var(--zy-ease);
}

.asset-risk-card:hover {
  border-color: var(--zy-blue-500);
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
}

.asset-risk-card span,
.asset-risk-card em {
  display: block;
  font-size: var(--zy-fs-xs);
  font-style: normal;
}

.asset-risk-card strong {
  display: block;
  margin: 6px 0 4px;
  color: var(--zy-ink);
  font-size: var(--zy-fs-3xl);
  font-weight: var(--zy-fw-bold);
  font-variant-numeric: tabular-nums;
}

.asset-bars,
.asset-activity-list,
.asset-event-list {
  display: grid;
  gap: var(--zy-sp-2);
}

.asset-bar-row {
  display: grid;
  gap: 6px;
}

.asset-bar-row > div:first-child,
.asset-activity-item {
  display: flex;
  gap: var(--zy-sp-3);
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

.asset-bar-row strong,
.asset-activity-item strong,
.asset-event-item strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.asset-bar-row span,
.asset-activity-item span,
.asset-event-item span,
.asset-event-item em {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-variant-numeric: tabular-nums;
}

.asset-bar-row__track {
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--zy-blue-50);
}

.asset-bar-row__track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--zy-blue-500);
  transition: width var(--zy-duration-4) var(--zy-ease-out);
}

.asset-bar-row__track--muted {
  background: rgba(20, 184, 200, 0.12);
}

.asset-bar-row__track--muted span {
  background: var(--zy-cyan-500);
}

.asset-activity-item,
.asset-event-item {
  min-width: 0;
  padding: var(--zy-sp-2) var(--zy-sp-3);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
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
  gap: var(--zy-sp-3);
  align-items: center;
}

.asset-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto 150px 150px 120px auto;
  gap: var(--zy-sp-2);
  margin-bottom: var(--zy-sp-3);
}

.asset-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--zy-sp-3);
}

.asset-detail-section {
  margin-bottom: var(--zy-sp-5);
}

.asset-detail-section h3 {
  margin: 0 0 var(--zy-sp-2);
  padding-left: var(--zy-sp-2);
  border-left: 3px solid var(--zy-blue-500);
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.asset-detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
}

.preview-message {
  margin-top: 10px;
}

.preview-dialog-body {
  min-height: 260px;
}

.preview-state-panel,
.job-state-panel {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--zy-sp-3);
  align-items: center;
  margin-bottom: var(--zy-sp-3);
}

.preview-state-panel strong,
.preview-state-panel span,
.job-state-panel strong,
.job-state-panel span {
  display: block;
  min-width: 0;
  overflow-wrap: anywhere;
}

.preview-state-panel span,
.job-state-panel span {
  margin-top: 4px;
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
}

.preview-descriptions,
.job-descriptions {
  margin-top: var(--zy-sp-3);
}

.preview-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  margin-top: var(--zy-sp-4);
}

.job-dialog-body {
  min-height: 260px;
}

.job-message {
  margin-top: var(--zy-sp-3);
}

.quality-flag {
  margin-right: 4px;
}

.mono-text {
  font-family: var(--zy-font-mono);
  font-size: var(--zy-fs-xs);
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

  .asset-workstream-strip,
  .asset-workspace-gate {
    align-items: flex-start;
  }

  .asset-workstream-strip {
    grid-template-columns: 1fr;
  }

  .asset-workspace-gate {
    flex-direction: column;
  }

  .asset-workspace-gate__actions {
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
