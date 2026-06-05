<template>
  <section class="mvp-page asset-page project-workbench">
    <header class="project-workbench-identity">
      <div class="project-workbench-identity__cover">
        <img :src="projectCoverImage" alt="" />
      </div>
      <div class="project-workbench-identity__main">
        <h1>{{ projectDisplayName }}</h1>
        <div class="project-workbench-identity__meta">
          <span>编码：{{ projectCodeText }}</span>
          <span>负责人：{{ projectOwnerText }}</span>
          <span>当前角色：{{ currentRoleName }}</span>
          <span>阶段：{{ projectStageText }}</span>
        </div>
        <div class="project-workbench-identity__status">
          <el-tag :type="projectPrimaryStatusTag" effect="plain">{{ projectPrimaryStatusLabel }}</el-tag>
          <el-tag :type="masterDataReady ? 'success' : 'warning'" effect="plain">
            {{ masterDataReady ? '工程主数据已就绪' : '工程主数据待确认' }}
          </el-tag>
          <el-tag type="info" effect="plain">当前项目</el-tag>
        </div>
      </div>
      <div class="project-workbench-identity__actions">
        <el-tooltip content="收藏项目占位">
          <el-button circle :icon="Star" aria-label="收藏项目占位" />
        </el-tooltip>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </header>

    <nav class="project-primary-tabs" aria-label="项目内一级导航">
      <button
        v-for="item in projectWorkspaceTabs"
        :key="item.key"
        type="button"
        :class="{ 'is-active': item.key === activeWorkspaceTabKey }"
        @click="openWorkspaceTab(item)"
      >
        {{ item.label }}
      </button>
    </nav>

    <main v-loading="loading" class="project-workbench-body">
      <section v-if="workspaceViewKey === 'dashboard'" class="workspace-view workspace-overview">
        <WorkspaceFlow :steps="workspaceFlowSteps" />

        <section class="workspace-overview__grid">
          <div class="workspace-main-column">
            <section class="workspace-next-action">
              <div>
                <span>今日建议 / 下一步行动</span>
                <strong>{{ nextActionHeadline }}</strong>
                <p>{{ nextActionHelper }}</p>
              </div>
              <el-button type="primary" @click="openWorkspaceTab(nextActionTab)">
                {{ nextActionButtonText }}
              </el-button>
            </section>

            <div class="workspace-action-grid">
              <WorkspaceCard
                v-for="item in overviewActionCards"
                :key="item.key"
                interactive
                density="compact"
                @click="openWorkspaceTab(item)"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <em>{{ item.helper }}</em>
              </WorkspaceCard>
            </div>

            <section class="workspace-panel">
              <div class="workspace-panel__header">
                <div>
                  <span>RECENT FILES</span>
                  <strong>最近文件 / 最近处理记录</strong>
                </div>
                <el-button text @click="openAssetTab('files')">进入文件管理</el-button>
              </div>
              <el-table :data="recentFiles" class="master-table workspace-compact-table" empty-text="暂无最近文件">
                <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
                <el-table-column label="类型" width="90">
                  <template #default="{ row }">{{ fileKindLabel(row.fileKind) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.checksum ? 'success' : 'warning'">
                      {{ row.checksum ? '已入库' : '待补指纹' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="更新时间" width="150">
                  <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="90" fixed="right">
                  <template #default="{ row }">
                    <el-button text @click="openFileDetailById(row.fileId)">详情</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </section>
          </div>

          <aside class="workspace-side-rail">
            <section class="workspace-panel">
              <div class="workspace-panel__header">
                <div>
                  <strong>项目健康度</strong>
                </div>
                <el-button text @click="openWorkspaceTab({ key: 'master-data', label: '工程主数据', tab: 'master-data' })">详情</el-button>
              </div>
              <div class="workspace-health-grid">
                <WorkspaceMetricCard
                  v-for="item in healthTiles"
                  :key="item.label"
                  :label="item.label"
                  :value="item.value"
                  :helper="item.helper"
                  :tone="item.tone"
                />
              </div>
            </section>

            <section class="workspace-panel">
              <div class="workspace-panel__header">
                <div>
                  <strong>风险提醒</strong>
                </div>
                <el-tag :type="riskSignalCount > 0 ? 'warning' : 'success'" effect="plain">
                  {{ formatCount(riskSignalCount) }} 项
                </el-tag>
              </div>
              <div class="workspace-risk-list">
                <button v-for="risk in riskReminderRows" :key="risk.key" type="button" @click="openRiskCard(risk)">
                  <span>{{ risk.label }}</span>
                  <strong>{{ formatCount(risk.count) }}</strong>
                  <em>{{ risk.helper }}</em>
                </button>
              </div>
            </section>

            <section class="workspace-panel">
              <div class="workspace-panel__header">
                <div>
                  <strong>Viewer 状态</strong>
                </div>
              </div>
              <div class="workspace-viewer-status">
                <el-tag :type="bimViewerTagType" effect="plain">{{ bimViewerStatusText }}</el-tag>
                <p>{{ bimViewerHint }}</p>
                <el-button :disabled="!bimReady" @click="openWorkspaceTab({ key: 'bim', label: 'BIM 协同', tab: 'bim' })">
                  查看 Viewer 状态
                </el-button>
              </div>
            </section>
          </aside>
        </section>
      </section>

      <section v-else-if="workspaceViewKey === 'files'" class="workspace-view workspace-files">
        <nav class="workspace-subtabs" aria-label="文件管理二级导航">
          <button class="is-active" type="button">全部文件</button>
          <button type="button">目录浏览</button>
          <button type="button">待归属</button>
          <button type="button">对象存储</button>
          <button type="button">质量异常</button>
        </nav>
        <div class="workspace-section-head">
          <div>
            <h2>文件管理</h2>
            <p>目录树、文件列表和右侧文件详情放在同一个工作面里；搜索仍按全项目文件口径，不混入文件夹。</p>
          </div>
          <div class="workspace-section-head__actions">
            <el-tag type="info" effect="plain">{{ formatCount(statistics?.fileCount) }} 份文件</el-tag>
            <el-tag :type="riskSignalCount > 0 ? 'warning' : 'success'" effect="plain">
              {{ riskSignalCount > 0 ? '存在治理项' : '暂无明显风险' }}
            </el-tag>
          </div>
        </div>

        <section class="workspace-files-layout">
          <div class="workspace-files-layout__browser">
            <AssetProjectFileBrowser
              v-if="Number.isFinite(projectId)"
              :key="`${projectId}-${fileBrowserRefreshKey}-${catalogInitialQualityIssue}`"
              :project-id="projectId"
              :root-label="projectRootLabel"
              :discipline-options="disciplineOptions"
              :initial-quality-issue="catalogInitialQualityIssue"
              :batch-checksum-creating="batchChecksumCreating"
              :active="workspaceViewKey === 'files'"
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
                  <span>默认收起，排查文件指纹时再展开查看任务状态。</span>
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
                    <el-table-column label="任务编号" width="110">
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
                    <el-table-column label="操作" width="120" fixed="right">
                      <template #default="{ row }">
                        <el-button size="small" text @click="openChecksumJob(row)">查看</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </el-collapse-transition>
            </section>
          </div>

          <aside class="workspace-file-inspector">
            <div class="workspace-panel__header">
              <div>
                <span>DETAIL</span>
                <strong>文件详情面板</strong>
              </div>
            </div>
            <template v-if="fileInspector">
              <div class="workspace-file-preview">
                <span>{{ fileKindLabel(fileInspector.fileKind) }}</span>
                <strong>{{ fileInspector.fileName }}</strong>
              </div>
              <dl class="workspace-detail-list">
                <div>
                  <dt>归属</dt>
                  <dd>{{ fileInspector.ownershipNodeLabel || '待确认归属' }}</dd>
                </div>
                <div>
                  <dt>对象存储</dt>
                  <dd>{{ fileStorageModeLabel(fileInspector) }}</dd>
                </div>
                <div>
                  <dt>版本</dt>
                  <dd>{{ fileInspector.version || '-' }}</dd>
                </div>
                <div>
                  <dt>更新时间</dt>
                  <dd>{{ formatDate(fileInspector.updatedAt) }}</dd>
                </div>
              </dl>
              <div class="workspace-file-inspector__actions">
                <el-button type="primary" :disabled="!fileInspector.fileId" @click="fileInspector.fileId && openFileDetailById(fileInspector.fileId)">查看详情</el-button>
                <el-button :disabled="!fileInspector.fileId" @click="fileInspector.fileId && openPreviewById(fileInspector.fileId)">预览状态</el-button>
              </div>
            </template>
            <el-empty v-else description="选择文件后查看详情；底层 NAS 路径不会展示" :image-size="54" />
            <el-alert
              title="安全边界：不展示真实 NAS 路径，预览和下载必须走 file-access 受控票据。"
              type="warning"
              show-icon
              :closable="false"
            />
          </aside>
        </section>
      </section>

      <section v-else-if="workspaceViewKey === 'master-data'" class="workspace-view workspace-master-data">
        <nav class="workspace-subtabs" aria-label="工程主数据二级导航">
          <button class="is-active" type="button">接入向导</button>
          <button type="button">部位树</button>
          <button type="button">节点类型</button>
          <button type="button">交付物标准</button>
        </nav>
        <div class="workspace-section-head">
          <div>
            <h2>工程主数据</h2>
            <p>先定义部位、节点类型和交付标准，再驱动后续缺失项、审核和归档。</p>
          </div>
          <div class="workspace-completion">
            <strong>{{ masterDataCompletion }}%</strong>
            <span>完成度</span>
          </div>
        </div>

        <div class="workspace-master-steps">
          <article v-for="item in masterDataSteps" :key="item.title" :class="{ 'is-done': item.done, 'is-current': item.current }">
            <span>{{ item.index }}</span>
            <strong>{{ item.title }}</strong>
            <em>{{ item.status }}</em>
          </article>
        </div>

        <section class="workspace-master-layout">
          <aside class="workspace-panel">
            <div class="workspace-panel__header">
              <div>
                <span>BLOCKERS</span>
                <strong>阻塞项 / 待处理</strong>
              </div>
            </div>
            <div class="workspace-blocker-list">
              <button v-for="item in masterDataBlockers" :key="item.key" type="button" @click="openWorkspaceTab(item)">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <em>{{ item.helper }}</em>
              </button>
            </div>
          </aside>

          <div class="workspace-master-layout__tree">
            <FileOwnershipTreePanel
              v-if="Number.isFinite(projectId)"
              :project-id="projectId"
              :active="workspaceViewKey === 'master-data'"
              :focus-node-path="ownershipFocusNodePath"
              @updated="handleOwnershipUpdated"
            />
            <el-empty v-else description="请先选择项目" :image-size="56" />
          </div>

          <aside class="workspace-panel">
            <div class="workspace-panel__header">
              <div>
                <span>NODE INFO</span>
                <strong>当前节点属性</strong>
              </div>
            </div>
            <dl class="workspace-detail-list">
              <div>
                <dt>部位树</dt>
                <dd>{{ initializationStatus?.sectionStatus?.sectionReady ? '已确认' : '待确认' }}</dd>
              </div>
              <div>
                <dt>节点类型</dt>
                <dd>{{ initializationStatus?.nodeTypeStatus?.nodeTypeReady ? '已锁定' : '待锁定' }}</dd>
              </div>
              <div>
                <dt>交付物标准</dt>
                <dd>{{ initializationStatus?.standardStatus?.deliverableStandardReady ? '已配置' : '待配置' }}</dd>
              </div>
              <div>
                <dt>最近更新</dt>
                <dd>{{ formatDate(qualityOverview?.latestEventAt) }}</dd>
              </div>
            </dl>
            <el-button type="primary" @click="router.push({ name: 'project-master-data-initialization', params: { projectId } })">
              打开初始化向导
            </el-button>
          </aside>
        </section>
      </section>

      <section v-else-if="workspaceViewKey === 'delivery'" class="workspace-view workspace-delivery">
        <div class="workspace-section-head">
          <div>
            <h2>交付闭环</h2>
            <p>把应交项、缺失项、审核、整改和导出预检查放在同一条工作线上。</p>
          </div>
          <div class="workspace-delivery-tabs">
            <button type="button" class="is-active">交付状态</button>
            <button type="button" @click="router.push({ name: 'project-work-document-delivery', params: { projectId } })">文档交付</button>
            <button type="button" @click="router.push({ name: 'project-work-drawing-delivery', params: { projectId } })">图纸交付</button>
            <button type="button" @click="router.push({ name: 'project-work-rectifications', params: { projectId } })">整改闭环</button>
            <button type="button" @click="openAssetTab('archive')">交付包草案</button>
          </div>
        </div>

        <section class="workspace-next-action">
          <div>
            <span>下一步行动</span>
            <strong>{{ deliveryNextAction }}</strong>
            <p>不生成真实文件包，不复制 NAS 文件；正式交付仍走文档/图纸交付页。</p>
          </div>
          <el-button type="primary" @click="router.push({ name: 'project-work-document-delivery', params: { projectId } })">
            进入待补交列表
          </el-button>
        </section>

        <section class="workspace-delivery-layout">
          <div class="workspace-panel">
            <div class="workspace-delivery-state-grid">
              <WorkspaceMetricCard
                v-for="item in deliveryStateSummary"
                :key="item.label"
                :label="item.label"
                :value="item.value"
                :helper="item.helper"
                :tone="item.tone"
              />
            </div>
            <el-table :data="deliveryRows" class="master-table workspace-compact-table" empty-text="暂无交付状态">
              <el-table-column prop="name" label="交付项" min-width="180" />
              <el-table-column prop="type" label="类型" width="110" />
              <el-table-column prop="status" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.tag">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="action" label="下一步" min-width="180" />
            </el-table>
          </div>

          <aside class="workspace-side-rail">
            <section class="workspace-panel">
              <div class="workspace-panel__header">
                <div>
                  <span>REVIEW</span>
                  <strong>审核进度</strong>
                </div>
              </div>
              <div class="workspace-timeline">
                <article v-for="item in deliveryReviewSteps" :key="item.label" :class="{ 'is-active': item.active }">
                  <span></span>
                  <div>
                    <strong>{{ item.label }}</strong>
                    <em>{{ item.helper }}</em>
                  </div>
                </article>
              </div>
            </section>
            <section class="workspace-panel">
              <div class="workspace-panel__header">
                <div>
                  <span>QUICK ACTIONS</span>
                  <strong>快捷操作</strong>
                </div>
              </div>
              <div class="workspace-quick-actions">
                <el-button @click="router.push({ name: 'project-work-document-delivery', params: { projectId } })">选择文件补交</el-button>
                <el-button @click="router.push({ name: 'project-work-rectifications', params: { projectId } })">整改记录</el-button>
                <el-button @click="openAssetTab('archive')">导出预检查</el-button>
              </div>
            </section>
          </aside>
        </section>
      </section>

      <section v-else-if="workspaceViewKey === 'bim'" class="workspace-view workspace-bim">
        <nav class="workspace-subtabs" aria-label="BIM 协同二级导航">
          <button class="is-active" type="button">BIM 看板</button>
          <button type="button">模型预览</button>
          <button type="button">轻量化状态</button>
          <button type="button">Viewer 入口</button>
        </nav>
        <div class="workspace-section-head">
          <div>
            <h2>BIM 协同</h2>
            <p>展示当前项目已登记模型和葛兰岱尔轻量化 Viewer；仅读取已有状态，不启动转换任务。</p>
          </div>
          <el-button type="primary" :disabled="!activeGlandarModel" @click="openActiveGlandarViewer">
            打开独立 Viewer
          </el-button>
        </div>

        <div class="workspace-bim-kpis">
          <WorkspaceMetricCard
            v-for="item in bimKpiCards"
            :key="item.label"
            :label="item.label"
            :value="item.value"
            :helper="item.helper"
            :tone="item.tone"
          />
        </div>

        <section class="workspace-bim-layout">
          <div class="workspace-bim-viewer" :class="{ 'has-glandar-viewer': Boolean(activeGlandarFrameUrl) }">
            <iframe
              v-if="activeGlandarFrameUrl"
              class="workspace-bim-viewer__iframe"
              :src="activeGlandarFrameUrl"
              title="葛兰岱尔轻量化模型预览"
              loading="lazy"
            />
            <div v-else class="workspace-bim-viewer__empty">
              <strong>{{ bimEmptyTitle }}</strong>
              <span>{{ bimEmptyDescription }}</span>
            </div>
            <div v-if="activeGlandarFrameUrl" class="workspace-bim-viewer__toolbar">
              <button type="button" disabled>平移 / 旋转 / 剖切 / 测量由 Viewer 内部工具栏控制</button>
            </div>
            <p>{{ bimViewerHint }}</p>
          </div>

          <aside class="workspace-side-rail">
            <section class="workspace-panel">
              <div class="workspace-panel__header">
                <div>
                  <span>MODELS</span>
                  <strong>模型列表</strong>
                </div>
              </div>
              <div class="workspace-model-list">
                <article v-for="item in bimModelRows" :key="item.name">
                  <div>
                    <strong>{{ item.name }}</strong>
                    <span>{{ item.version }}</span>
                    <em v-if="item.action">{{ item.action }}</em>
                  </div>
                  <el-tag size="small" :type="item.tag">{{ item.status }}</el-tag>
                </article>
              </div>
            </section>

            <section class="workspace-panel">
              <div class="workspace-panel__header">
                <div>
                  <span>LIGHTWEIGHT</span>
                  <strong>轻量化状态</strong>
                </div>
              </div>
              <div class="workspace-model-list">
                <article v-for="item in bimTaskRows" :key="item.name">
                  <div>
                    <strong>{{ item.name }}</strong>
                    <span>{{ item.progress }}</span>
                  </div>
                  <el-tag size="small" :type="item.tag">{{ item.status }}</el-tag>
                </article>
              </div>
            </section>
          </aside>
        </section>
      </section>

      <section v-else class="workspace-view workspace-archive">
        <div class="workspace-section-head">
          <div>
            <span>ARCHIVE</span>
            <h2>档案目录</h2>
            <p>以交付包草案、档案目录和清单导出为入口；本页不生成真实 ZIP，也不复制 NAS 文件。</p>
          </div>
          <el-button type="primary" @click="router.push({ name: 'project-work-delivery-package', params: { projectId } })">
            打开档案目录
          </el-button>
        </div>

        <section class="workspace-archive-grid">
          <article v-for="item in archiveSummaryRows" :key="item.label" class="workspace-panel">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.helper }}</p>
          </article>
        </section>

        <el-alert
          title="导出预检查是只读 dry-run：不生成真实文件包，不访问、不复制 NAS 文件，不代表正式导出完成。"
          type="info"
          show-icon
          :closable="false"
        />
      </section>

      <WorkspaceBoundaryStrip
        :items="['不展示真实 NAS 路径', '不自动连接候选文件', 'Hermes 当前为 catalog-only']"
        action-label="了解更多安全策略"
        @action="openWorkspaceTab({ key: 'files', label: '文件管理', tab: 'files' })"
      />
    </main>

    <Teleport to="body">
      <section
        v-if="detailDrawerVisible"
        class="asset-detail-float"
        role="dialog"
        aria-modal="false"
        aria-label="文件详情浮窗"
      >
        <header class="asset-detail-float__header">
          <div>
            <span>DETAIL</span>
            <strong>{{ detailTitle }}</strong>
            <small>浮窗不占用文件列表，可拖动右下角调整大小</small>
          </div>
          <el-button circle text :icon="Close" aria-label="关闭文件详情" @click="detailDrawerVisible = false" />
        </header>
        <div v-if="selectedFile" class="asset-detail-float__body">
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
        </div>
      </section>
    </Teleport>

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
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ChatDotRound, Close, Refresh, Star, View } from '@element-plus/icons-vue';

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
import WorkspaceBoundaryStrip from '@/modules/core/components/workspace/WorkspaceBoundaryStrip.vue';
import WorkspaceCard from '@/modules/core/components/workspace/WorkspaceCard.vue';
import WorkspaceFlow from '@/modules/core/components/workspace/WorkspaceFlow.vue';
import WorkspaceMetricCard from '@/modules/core/components/workspace/WorkspaceMetricCard.vue';
import DataStewardPanel from '@/modules/data-steward/components/DataStewardPanel.vue';
import FileOwnershipTreePanel from '@/modules/data-steward/components/FileOwnershipTreePanel.vue';
import HermesWorkspaceDrawer from '@/modules/data-steward/components/HermesWorkspaceDrawer.vue';
import { useAuthStore } from '@/stores/auth';
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
import { fetchGlandarModelFiles, type GlandarModelFile } from '@/modules/visualization/api/visualization';
import projectCoverImage from '@/assets/ux4/project-cover-reference.png';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const projectId = computed(() => Number(route.params.projectId));

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
const glandarModelFiles = ref<GlandarModelFile[]>([]);
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
const assetTabs = new Set(['dashboard', 'overview', 'ownership', 'master-data', 'files', 'delivery', 'bim', 'archive', 'scans', 'mappings']);
type ProjectWorkspaceTab = {
  key: string;
  label: string;
  value?: string;
  helper?: string;
  tab?: string;
};
type WorkspaceFlowStepItem = {
  key: string;
  index: string;
  title: string;
  status: string;
  state: 'done' | 'current' | 'pending' | 'locked' | 'blocked';
};
type WorkspaceMetricTone = 'default' | 'accent' | 'success' | 'warning' | 'danger';
const projectWorkspaceTabs: ProjectWorkspaceTab[] = [
  { key: 'dashboard', label: '概览', tab: 'dashboard' },
  { key: 'files', label: '文件管理', tab: 'files' },
  { key: 'master-data', label: '工程主数据', tab: 'master-data' },
  { key: 'delivery', label: '交付闭环', tab: 'delivery' },
  { key: 'bim', label: 'BIM 协同', tab: 'bim' },
  { key: 'archive', label: '档案目录', tab: 'archive' }
];
const currentUserProject = computed(() =>
  authStore.currentUser?.projects.find((item) => item.id === projectId.value) ?? null
);
const projectDisplayName = computed(() =>
  project.value?.name ?? currentUserProject.value?.name ?? `项目 ${projectId.value}`
);
const projectCodeText = computed(() =>
  project.value?.code ?? currentUserProject.value?.code ?? '-'
);
const projectOwnerText = computed(() =>
  project.value?.projectManagerName ?? currentUserProject.value?.projectManagerName ?? '待维护'
);
const currentRoleName = computed(() =>
  currentUserProject.value?.roleName ?? '项目成员'
);
const projectStageText = computed(() =>
  project.value?.projectStage ?? '阶段待维护'
);
const projectPrimaryStatusLabel = computed(() =>
  onboardingStatusLabel(project.value?.onboardingStatus) || project.value?.assetStatus || '状态待维护'
);
const projectPrimaryStatusTag = computed(() => {
  const value = project.value?.onboardingStatus;
  if (value === 'GOVERNANCE_READY') return 'success';
  if (value === 'MASTERDATA_INITIALIZED' || value === 'ASSETS_REGISTERED') return 'warning';
  return 'info';
});
const workspaceViewKey = computed(() => normalizeWorkspaceTab(activeTab.value));
const activeWorkspaceTabKey = computed(() => workspaceViewKey.value);
const masterDataReady = computed(() =>
  Boolean(initializationStatus.value?.ready || initializationStatus.value?.standardStatus?.deliverableStandardReady)
);
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
const masterDataCompletion = computed(() => {
  const checks = [
    Boolean(initializationStatus.value?.sectionStatus?.sectionReady),
    Boolean(initializationStatus.value?.nodeTypeStatus?.nodeTypeReady),
    Boolean(initializationStatus.value?.standardStatus?.deliverableStandardReady),
    masterDataReady.value
  ];
  const done = checks.filter(Boolean).length;
  return Math.round((done / checks.length) * 100);
});
const workflowStepCards = computed(() => [
  {
    key: 'assets',
    index: '1',
    title: '资产接入',
    status: Number(statistics.value?.fileCount ?? 0) > 0 ? '已完成' : '待登记',
    done: Number(statistics.value?.fileCount ?? 0) > 0,
    current: Number(statistics.value?.fileCount ?? 0) === 0,
    locked: false
  },
  {
    key: 'master-data',
    index: '2',
    title: '工程主数据',
    status: masterDataReady.value ? '已就绪' : '进行中',
    done: masterDataReady.value,
    current: Number(statistics.value?.fileCount ?? 0) > 0 && !masterDataReady.value,
    locked: Number(statistics.value?.fileCount ?? 0) === 0
  },
  {
    key: 'delivery',
    index: '3',
    title: '交付闭环',
    status: masterDataReady.value ? '可进入' : '待主数据',
    done: false,
    current: masterDataReady.value,
    locked: !masterDataReady.value
  },
  {
    key: 'bim',
    index: '4',
    title: 'BIM 协同',
    status: bimReady.value ? 'READY' : '待接入',
    done: bimReady.value,
    current: false,
    locked: Number(statistics.value?.modelFileCount ?? 0) === 0
  },
  {
    key: 'archive',
    index: '5',
    title: '档案目录',
    status: '草案',
    done: false,
    current: false,
    locked: !masterDataReady.value
  }
]);
const workspaceFlowSteps = computed<WorkspaceFlowStepItem[]>(() => workflowStepCards.value.map((item) => ({
  key: item.key,
  index: String(item.index),
  title: item.title,
  status: item.status,
  state: item.current
    ? 'current'
    : item.done
      ? 'done'
      : item.locked
        ? 'locked'
        : 'pending'
})));
const overviewActionCards = computed<ProjectWorkspaceTab[]>(() => [
  {
    key: 'files',
    label: '文件管理',
    value: formatCount(statistics.value?.fileCount),
    helper: `${formatBytes(statistics.value?.totalSizeBytes)} 已登记`,
    tab: 'files'
  },
  {
    key: 'master-data',
    label: '工程主数据',
    value: `${masterDataCompletion.value}%`,
    helper: masterDataReady.value ? '规则底座已就绪' : '仍需确认规则',
    tab: 'master-data'
  },
  {
    key: 'delivery',
    label: '交付状态',
    value: masterDataReady.value ? '可进入' : '待主数据',
    helper: '查看缺失项、审核和预检查',
    tab: 'delivery'
  }
]);
const healthTiles = computed(() => [
  { label: '总体进度', value: `${Math.min(96, Math.max(18, masterDataCompletion.value + 18))}%`, helper: masterDataReady.value ? '可推进交付' : '主数据待确认', tone: masterDataReady.value ? 'success' : 'warning' },
  { label: '数据质量', value: `${riskSignalCount.value > 0 ? 72 : 88}%`, helper: riskSignalCount.value > 0 ? '存在待治理项' : '暂无明显风险', tone: riskSignalCount.value > 0 ? 'warning' : 'success' },
  { label: '合规性', value: masterDataReady.value ? '92%' : '64%', helper: masterDataReady.value ? '规则已建立' : '规则未完成', tone: masterDataReady.value ? 'success' : 'warning' },
  { label: '活跃成员', value: '项目组', helper: currentRoleName.value, tone: 'accent' }
]);
const riskReminderRows = computed(() => riskCards.value.slice(0, 3));
const fileInspector = computed(() => recentFiles.value[0] ?? null);
const nextActionTab = computed<ProjectWorkspaceTab>(() => {
  if (!masterDataReady.value) return { key: 'master-data', label: '工程主数据', tab: 'master-data' };
  if (riskSignalCount.value > 0) return { key: 'files', label: '文件管理', tab: 'files' };
  return { key: 'delivery', label: '交付闭环', tab: 'delivery' };
});
const nextActionHeadline = computed(() => {
  if (!masterDataReady.value) return '确认工程主数据，再进入交付闭环';
  if (riskSignalCount.value > 0) return '先处理文件治理风险';
  return '进入交付中心，检查缺失项和预检查';
});
const nextActionHelper = computed(() => {
  if (!masterDataReady.value) return '部位树、节点类型和交付物标准会影响缺失项计算，建议先完成确认。';
  if (riskSignalCount.value > 0) return '文件指纹、专业、置信度等治理项会影响后续审核和归档质量。';
  return '当前项目底座已具备，可以进入文档/图纸交付和档案目录 dry-run。';
});
const nextActionButtonText = computed(() => {
  if (!masterDataReady.value) return '确认主数据';
  if (riskSignalCount.value > 0) return '处理文件治理';
  return '进入交付闭环';
});
const masterDataSteps = computed(() => [
  {
    index: '1',
    title: '确认部位树',
    status: initializationStatus.value?.sectionStatus?.sectionReady ? '已完成' : '待确认',
    done: Boolean(initializationStatus.value?.sectionStatus?.sectionReady),
    current: !initializationStatus.value?.sectionStatus?.sectionReady
  },
  {
    index: '2',
    title: '锁定节点类型',
    status: initializationStatus.value?.nodeTypeStatus?.nodeTypeReady ? '已锁定' : '待确认',
    done: Boolean(initializationStatus.value?.nodeTypeStatus?.nodeTypeReady),
    current: Boolean(initializationStatus.value?.sectionStatus?.sectionReady) && !initializationStatus.value?.nodeTypeStatus?.nodeTypeReady
  },
  {
    index: '3',
    title: '配置交付物标准',
    status: initializationStatus.value?.standardStatus?.deliverableStandardReady ? '已配置' : '待配置',
    done: Boolean(initializationStatus.value?.standardStatus?.deliverableStandardReady),
    current: Boolean(initializationStatus.value?.nodeTypeStatus?.nodeTypeReady) && !initializationStatus.value?.standardStatus?.deliverableStandardReady
  },
  {
    index: '4',
    title: '生成规则草案',
    status: masterDataReady.value ? '可用于交付' : '待生成',
    done: masterDataReady.value,
    current: false
  }
]);
const masterDataBlockers = computed<ProjectWorkspaceTab[]>(() => [
  {
    key: 'master-data',
    label: '部位树缺失',
    value: initializationStatus.value?.sectionStatus?.sectionReady ? '0' : '需确认',
    helper: '影响文件归属和应交目标',
    tab: 'master-data'
  },
  {
    key: 'master-data',
    label: '节点类型待确认',
    value: initializationStatus.value?.nodeTypeStatus?.nodeTypeReady ? '0' : '需锁定',
    helper: '影响节点属性和编码规则',
    tab: 'master-data'
  },
  {
    key: 'master-data',
    label: '交付标准待配置',
    value: initializationStatus.value?.standardStatus?.deliverableStandardReady ? '0' : '需配置',
    helper: '影响文档/图纸缺失项',
    tab: 'master-data'
  }
]);
const deliveryStateSummary = computed(() => [
  { label: '待补交', value: masterDataReady.value ? formatCount(riskSignalCount.value + 8) : '-', helper: '需选择文件', tone: masterDataReady.value ? 'warning' : 'default' },
  { label: '待审核', value: formatCount(qualityOverview.value?.pendingReviewCount), helper: '人工确认', tone: Number(qualityOverview.value?.pendingReviewCount ?? 0) > 0 ? 'warning' : 'default' },
  { label: '已整改', value: masterDataReady.value ? formatCount(Math.max(0, recentEvents.value.length)) : '-', helper: '闭环记录', tone: 'success' },
  { label: '预检查', value: masterDataReady.value ? 'dry-run' : '待主数据', helper: '只读检查', tone: 'accent' }
]);
const deliveryRows = computed(() => [
  { name: '文档交付清单', type: '文档', status: masterDataReady.value ? '可检查' : '待主数据', tag: masterDataReady.value ? 'success' : 'warning', action: '选择文件补交或导出预检查' },
  { name: '图纸交付清单', type: '图纸', status: masterDataReady.value ? '可检查' : '待主数据', tag: masterDataReady.value ? 'success' : 'warning', action: '检查缺失项和审核状态' },
  { name: '整改闭环', type: '审核', status: riskSignalCount.value > 0 ? '需关注' : '稳定', tag: riskSignalCount.value > 0 ? 'warning' : 'success', action: '查看驳回和整改记录' },
  { name: '交付包草案', type: '归档', status: 'dry-run', tag: 'info', action: '只读预检查，不生成文件包' }
]);
const deliveryReviewSteps = computed(() => [
  { label: '提交交付', helper: masterDataReady.value ? '可从文档/图纸交付页提交' : '等待主数据', active: masterDataReady.value },
  { label: '专业审核', helper: `${formatCount(qualityOverview.value?.pendingReviewCount)} 项待确认`, active: Number(qualityOverview.value?.pendingReviewCount ?? 0) > 0 },
  { label: '项目复核', helper: '由项目负责人确认', active: false },
  { label: '归档入库', helper: '交付包草案通过后进入', active: false }
]);
const deliveryNextAction = computed(() =>
  masterDataReady.value ? '查看待补交清单并提交审核' : '请先完成工程主数据确认'
);
const glandarReadyModels = computed(() =>
  glandarModelFiles.value.filter((item) => item.viewerAvailable && Boolean(item.latestJobId))
);
const activeGlandarModel = computed(() => glandarReadyModels.value[0] ?? null);
const activeGlandarFrameUrl = computed(() => {
  const item = activeGlandarModel.value;
  if (!item?.latestJobId) return '';
  const query = new URLSearchParams({
    projectId: String(projectId.value),
    jobId: item.latestJobId,
    fileName: item.fileName,
    modelFileId: String(item.fileId),
    embedded: '1',
    theme: 'light'
  });
  return `/visualization/glandar-viewer-embed?${query.toString()}`;
});
const catalogModelCount = computed(() => Number(statistics.value?.modelFileCount ?? 0));
const glandarModelCount = computed(() => glandarModelFiles.value.length);
const bimReady = computed(() => catalogModelCount.value > 0 || glandarModelCount.value > 0);
const glandarProcessingCount = computed(() =>
  glandarModelFiles.value.filter((item) => {
    const status = `${item.lightweightStatus || item.taskStatus || ''}`.toUpperCase();
    return ['PENDING', 'RUNNING', 'PROCESSING', 'SUBMITTED'].some((keyword) => status.includes(keyword));
  }).length
);
const glandarFailedCount = computed(() =>
  glandarModelFiles.value.filter((item) => {
    const status = `${item.lightweightStatus || item.taskStatus || ''}`.toUpperCase();
    return Boolean(item.failureReason) || status.includes('FAIL') || status.includes('ERROR');
  }).length
);
const bimViewerStatusText = computed(() => {
  if (activeGlandarModel.value) return 'Viewer 可用';
  if (glandarModelCount.value > 0) return '待轻量化';
  if (catalogModelCount.value > 0) return '模型已登记';
  return '待接入';
});
const bimViewerTagType = computed(() => {
  if (activeGlandarModel.value) return 'success';
  if (glandarModelCount.value > 0 || catalogModelCount.value > 0) return 'warning';
  return 'info';
});
const bimViewerHint = computed(() => {
  const model = activeGlandarModel.value;
  if (model) return `已接入葛兰岱尔轻量化 Viewer，当前嵌入展示：${model.fileName}。`;
  if (glandarModelCount.value > 0) return '已读取葛兰岱尔模型清单，但当前没有可打开的轻量化产物；页面不会自动启动转换。';
  if (catalogModelCount.value > 0) return '资产目录中已有模型文件，但尚未同步到葛兰岱尔轻量化清单；请从可视化模块查看任务状态。';
  return '当前项目暂未发现模型文件，BIM 协同不会触发模型解析或读取文件正文。';
});
const bimEmptyTitle = computed(() =>
  glandarModelCount.value > 0 ? '暂无可嵌入轻量化 Viewer' : '暂无模型预览内容'
);
const bimEmptyDescription = computed(() => {
  if (glandarModelCount.value > 0) return '已有模型清单，但缺少 viewerAvailable=true 且带 latestJobId 的轻量化结果。';
  if (catalogModelCount.value > 0) return '模型只在资产目录中登记，还没有可预览的葛兰岱尔轻量化产物。';
  return '登记模型文件并完成受控轻量化后，这里会显示模型预览。';
});
const bimKpiCards = computed(() => [
  {
    label: '模型数量',
    value: formatCount(Math.max(catalogModelCount.value, glandarModelCount.value)),
    helper: glandarModelCount.value > 0 ? '来自葛兰岱尔模型清单' : '来自资产目录登记',
    tone: 'accent'
  },
  {
    label: '可预览模型',
    value: formatCount(glandarReadyModels.value.length),
    helper: activeGlandarModel.value ? '已生成 Viewer 入口' : '等待轻量化产物',
    tone: activeGlandarModel.value ? 'success' : 'default'
  },
  {
    label: '处理中 / 失败',
    value: `${formatCount(glandarProcessingCount.value)} / ${formatCount(glandarFailedCount.value)}`,
    helper: '只读同步任务状态，不启动转换',
    tone: glandarFailedCount.value > 0 ? 'warning' : 'accent'
  },
  { label: '问题票据', value: formatCount(riskSignalCount.value), helper: '来自文件治理和协同提醒', tone: riskSignalCount.value > 0 ? 'warning' : 'success' }
]);
const bimModelRows = computed(() => {
  if (glandarModelFiles.value.length) {
    return glandarModelFiles.value.slice(0, 6).map((item) => ({
      name: item.fileName,
      version: `${(item.extension || item.fileKind || 'MODEL').toUpperCase()} · ${formatBytes(item.sizeBytes)}`,
      status: item.statusLabel || (item.viewerAvailable ? '可预览' : item.supported ? '待轻量化' : '不支持'),
      tag: item.viewerAvailable ? 'success' : item.supported ? 'warning' : 'info',
      action: item.actionHint || item.unsupportedReason || item.failureReason || item.relativePathHint || ''
    }));
  }
  const rows = recentFiles.value.filter((item) => item.fileKind === 'MODEL').slice(0, 4);
  if (rows.length) {
    return rows.map((item) => ({
      name: item.fileName,
      version: item.versionNo || fileKindLabel(item.fileKind),
      status: '目录级登记',
      tag: 'info',
      action: '尚未同步葛兰岱尔轻量化状态'
    }));
  }
  return [{ name: '暂无模型文件', version: '等待登记', status: '待接入', tag: 'info', action: '不会触发模型解析' }];
});
const bimTaskRows = computed(() => [
  {
    name: 'Glandar Viewer 入口',
    progress: activeGlandarModel.value?.fileName ?? '等待可用 latestJobId',
    status: activeGlandarModel.value ? '可打开' : '不可打开',
    tag: activeGlandarModel.value ? 'success' : 'info'
  },
  {
    name: '轻量化状态同步',
    progress: `${formatCount(glandarReadyModels.value.length)} 可预览 / ${formatCount(glandarModelCount.value)} 已同步`,
    status: glandarModelCount.value > 0 ? '已接入' : '无清单',
    tag: glandarModelCount.value > 0 ? 'success' : 'info'
  },
  { name: '协同问题同步', progress: `${formatCount(riskSignalCount.value)} 项提醒`, status: riskSignalCount.value > 0 ? '需关注' : '稳定', tag: riskSignalCount.value > 0 ? 'warning' : 'success' }
]);
const archiveSummaryRows = computed(() => [
  { label: '交付包草案', value: masterDataReady.value ? '可预检查' : '待主数据', helper: '草案只表达目录和清单，不生成真实包。' },
  { label: '档案目录', value: formatCount(documentFileCount.value), helper: '来自已登记文档、图纸和归档类文件。' },
  { label: '清单导出', value: 'dry-run', helper: '仅导出清单或预检查结果，不复制 NAS 文件。' }
]);

watch(
  () => [route.params.projectId, route.query.qualityIssue, route.query.tab, route.query.ownershipNode],
  () => {
    const nextTab = queryString(route.query.tab);
    if (nextTab && assetTabs.has(nextTab)) {
      activeTab.value = normalizeWorkspaceTab(nextTab);
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
    const [
      projectsResult,
      statisticsResult,
      qualityResult,
      scansResult,
      mappingsResult,
      disciplinesResult,
      recentFilesResult,
      initStatusResult,
      glandarModelsResult
    ] =
      await Promise.allSettled([
      fetchAssetProjects(),
      fetchAssetStatistics(projectId.value),
      fetchAssetQualityOverview(projectId.value),
      fetchAssetScanTasks(),
      fetchAssetPathMappings(projectId.value),
      fetchAssetDisciplines(projectId.value),
      fetchCatalogFiles({ projectId: projectId.value, page: 1, pageSize: 6 }),
      fetchInitializationStatus(projectId.value),
      fetchGlandarModelFiles(projectId.value)
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
    glandarModelFiles.value = glandarModelsResult.status === 'fulfilled' ? glandarModelsResult.value : [];
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
  glandarModelFiles.value = [];
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

function openActiveGlandarViewer() {
  const item = activeGlandarModel.value;
  if (!item?.latestJobId) {
    ElMessage.info('当前项目还没有可打开的葛兰岱尔轻量化模型。');
    return;
  }
  void router.push({
    name: 'glandar-model-preview',
    query: {
      projectId: String(projectId.value),
      jobId: item.latestJobId,
      fileName: item.fileName,
      modelFileId: String(item.fileId),
      theme: 'light'
    }
  });
}

function openWorkspaceTab(item: ProjectWorkspaceTab) {
  const tab = item.tab ?? item.key;
  if (tab === 'delivery' || tab === 'archive') {
    if (!masterDataReady.value) {
      ElMessage.warning('请先确认工程主数据；交付页面会继续保留阻塞提示。');
    }
  }
  openAssetTab(tab);
}

function openAssetTab(tab: string) {
  const nextTab = normalizeWorkspaceTab(tab);
  activeTab.value = nextTab;
  void router.replace({
    name: 'data-steward-asset-detail',
    params: { projectId: projectId.value },
    query: { ...route.query, tab: nextTab }
  });
}

function openOwnershipNodeFromFile(nodePath: string) {
  ownershipFocusNodePath.value = nodePath;
  activeTab.value = 'master-data';
  const { qualityIssue: _qualityIssue, ...nextQuery } = route.query;
  void router.replace({
    name: 'data-steward-asset-detail',
    params: { projectId: projectId.value },
    query: { ...nextQuery, tab: 'master-data', ownershipNode: nodePath }
  });
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

function fileStorageModeLabel(file: CatalogFile | FileAsset) {
  const status = 'storageState' in file ? file.storageState : undefined;
  const readSource = 'accessSource' in file ? file.accessSource : undefined;
  if (status === 'OBJECT_STORED' || readSource === 'OBJECT_STORAGE') return '对象存储副本可用';
  if (status === 'NAS_ONLY' || readSource === 'LEGACY_NAS') return 'NAS 侧受控读取';
  if (file.storageProvider) return `${file.storageProvider}，底层路径隐藏`;
  return '存储状态待同步';
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

function normalizeWorkspaceTab(value: string | null | undefined) {
  if (value === 'ownership' || value === 'master') return 'master-data';
  if (value === 'overview') return 'dashboard';
  if (value === 'scans' || value === 'mappings') return 'files';
  if (value === 'delivery' || value === 'bim' || value === 'archive' || value === 'files' || value === 'master-data') {
    return value;
  }
  return 'dashboard';
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

/* ---- UX4-A-R1 / Project workbench shell ---- */
.project-workbench {
  gap: 0;
  background: oklch(0.985 0.006 255);
}

.project-workbench-identity {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) auto;
  gap: 22px;
  align-items: center;
  min-width: 0;
  padding: 16px 8px 14px;
  border: 0;
  border-bottom: 1px solid oklch(0.9 0.018 255);
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.project-workbench-identity__cover {
  position: relative;
  min-width: 0;
  height: 116px;
  overflow: hidden;
  border: 1px solid oklch(0.88 0.025 255);
  border-radius: 10px;
  background: oklch(0.96 0.016 255);
  box-shadow: 0 10px 28px rgba(30, 58, 138, 0.08);
}

.project-workbench-identity__cover img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.project-workbench-identity__main {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.project-workbench-identity__eyebrow,
.workspace-section-head span,
.workspace-panel__header span {
  color: var(--zy-blue-700);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0;
}

.project-workbench-identity h1,
.workspace-section-head h2 {
  margin: 0;
  color: var(--zy-ink);
  font-weight: var(--zy-fw-semi);
  letter-spacing: 0;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.project-workbench-identity h1 {
  font-size: 26px;
  letter-spacing: 0;
}

.workspace-section-head h2 {
  font-size: 22px;
}

.project-workbench-identity__meta,
.project-workbench-identity__status,
.project-workbench-identity__actions,
.workspace-section-head__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.project-workbench-identity__meta span {
  color: oklch(0.42 0.035 260);
  font-size: 14px;
  line-height: 1.45;
}

.project-workbench-identity__actions {
  justify-content: flex-end;
}

.project-primary-tabs {
  display: flex;
  align-items: center;
  gap: 26px;
  min-width: 0;
  overflow-x: auto;
  padding: 0 8px;
  min-height: 52px;
  border-bottom: 1px solid oklch(0.9 0.018 255);
  background: transparent;
}

.project-primary-tabs button,
.workspace-delivery-tabs button,
.workspace-bim-viewer__toolbar button {
  appearance: none;
  border: 0;
  background: transparent;
  color: var(--zy-muted);
  cursor: pointer;
  font-family: inherit;
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
  letter-spacing: 0;
}

.project-primary-tabs button {
  flex: 0 0 auto;
  min-height: 52px;
  padding: 0;
  border-bottom: 2px solid transparent;
}

.project-primary-tabs button:hover,
.project-primary-tabs button.is-active {
  color: var(--zy-blue-700);
}

.project-primary-tabs button.is-active {
  border-bottom-color: var(--zy-blue-600);
}

.project-workbench-body,
.workspace-view,
.workspace-main-column,
.workspace-side-rail {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.project-workbench-body {
  padding: 14px 0 0;
}

.workspace-section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  min-width: 0;
  padding: 18px 20px;
  border: 1px solid oklch(0.88 0.024 255);
  border-radius: 12px;
  background: oklch(0.998 0.003 255);
  box-shadow: 0 10px 30px rgba(30, 64, 175, 0.04);
}

.workspace-section-head > div:first-child {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.workspace-section-head p,
.workspace-panel p,
.workspace-next-action p,
.workspace-viewer-status p,
.workspace-bim-viewer p {
  margin: 0;
  color: var(--zy-text-soft);
  font-size: var(--zy-fs-sm);
  line-height: 1.65;
}

.workspace-flow {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 18px;
  min-width: 0;
}

.workspace-flow__step {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 3px var(--zy-sp-2);
  align-content: start;
  min-width: 0;
  min-height: 86px;
  padding: 18px 18px;
  border: 1px solid oklch(0.88 0.024 255);
  border-radius: 12px;
  background: oklch(0.998 0.003 255);
  box-shadow: 0 8px 22px rgba(30, 64, 175, 0.04);
}

.workspace-flow__step:not(:last-child)::after {
  content: "";
  position: absolute;
  right: -15px;
  top: 50%;
  width: 12px;
  height: 12px;
  border-top: 1px solid oklch(0.65 0.07 255);
  border-right: 1px solid oklch(0.65 0.07 255);
  transform: translateY(-50%) rotate(45deg);
}

.workspace-flow__step > span {
  display: grid;
  place-items: center;
  grid-row: span 2;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
  font-family: var(--zy-font-mono);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-bold);
}

.workspace-flow__step strong,
.workspace-action-card strong,
.workspace-panel__header strong,
.workspace-next-action strong,
.workspace-health-grid strong,
.workspace-risk-list strong,
.workspace-delivery-state-grid strong,
.workspace-bim-kpis strong,
.workspace-panel > strong,
.workspace-completion strong {
  color: var(--zy-ink);
  font-weight: var(--zy-fw-semi);
  line-height: 1.25;
}

.workspace-flow__step strong {
  font-size: var(--zy-fs-sm);
}

.workspace-flow__step em,
.workspace-action-card em,
.workspace-health-grid em,
.workspace-risk-list em,
.workspace-detail-list dd,
.workspace-blocker-list em,
.workspace-delivery-state-grid em,
.workspace-timeline em,
.workspace-model-list span,
.workspace-bim-kpis em {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
  line-height: 1.5;
}

.workspace-flow__step.is-done {
  border-color: oklch(0.82 0.11 155);
  background: oklch(0.965 0.035 155);
}

.workspace-flow__step.is-current {
  border-color: var(--zy-blue-500);
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.08), 0 10px 30px rgba(37, 99, 235, 0.1);
}

.workspace-flow__step.is-locked {
  opacity: 0.76;
}

.workspace-overview__grid,
.workspace-delivery-layout,
.workspace-bim-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 18px;
  min-width: 0;
}

.workspace-next-action,
.workspace-panel,
.workspace-action-card,
.workspace-file-inspector,
.workspace-completion {
  min-width: 0;
  border: 1px solid oklch(0.88 0.024 255);
  border-radius: 12px;
  background: oklch(0.998 0.003 255);
  box-shadow: 0 10px 28px rgba(30, 64, 175, 0.045);
}

.workspace-next-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zy-sp-4);
  padding: 14px 18px;
  background: oklch(0.965 0.03 255);
  border-color: oklch(0.82 0.08 255);
}

.workspace-next-action > div,
.workspace-panel__header > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.workspace-next-action span {
  color: var(--zy-blue-700);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-bold);
}

.workspace-next-action strong {
  font-size: var(--zy-fs-lg);
}

.workspace-action-grid,
.workspace-health-grid,
.workspace-delivery-state-grid,
.workspace-bim-kpis,
.workspace-archive-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  min-width: 0;
}

.workspace-action-card,
.workspace-risk-list button,
.workspace-blocker-list button {
  appearance: none;
  cursor: pointer;
  display: grid;
  gap: 7px;
  padding: 18px;
  text-align: left;
}

.workspace-action-card {
  border: var(--zy-border);
}

.workspace-action-card:hover,
.workspace-risk-list button:hover,
.workspace-blocker-list button:hover {
  border-color: rgba(37, 99, 235, 0.28);
}

.workspace-action-card span,
.workspace-panel > span,
.workspace-health-grid span,
.workspace-risk-list span,
.workspace-blocker-list span,
.workspace-delivery-state-grid span,
.workspace-bim-kpis span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.workspace-action-card strong,
.workspace-bim-kpis strong {
  font-size: var(--zy-fs-2xl);
  font-variant-numeric: tabular-nums;
}

.workspace-panel,
.workspace-file-inspector {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 18px;
}

.workspace-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zy-sp-3);
  min-width: 0;
}

.workspace-health-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.workspace-health-grid article,
.workspace-delivery-state-grid article,
.workspace-bim-kpis article {
  display: grid;
  gap: 4px;
  min-width: 0;
  padding: 16px;
  border: 1px solid oklch(0.91 0.016 255);
  border-radius: 10px;
  background: oklch(0.985 0.008 255);
}

.workspace-risk-list,
.workspace-blocker-list,
.workspace-model-list,
.workspace-timeline,
.workspace-quick-actions,
.workspace-detail-list {
  display: grid;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.workspace-risk-list button,
.workspace-blocker-list button {
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface-soft);
}

.workspace-risk-list button {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.workspace-risk-list em {
  grid-column: 1 / -1;
}

.workspace-viewer-status {
  display: grid;
  gap: var(--zy-sp-3);
}

.workspace-compact-table {
  min-width: 0;
}

.workspace-files-layout,
.workspace-master-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 18px;
  min-width: 0;
  align-items: start;
}

.workspace-master-layout {
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr) minmax(240px, 320px);
  max-width: 100%;
  overflow: hidden;
}

.workspace-files-layout__browser,
.workspace-master-layout__tree {
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  contain: inline-size;
}

.workspace-master-data,
.workspace-master-data > *,
.workspace-master-layout > * {
  min-width: 0;
  max-width: 100%;
}

.workspace-master-layout .workspace-panel {
  overflow: hidden;
}

.workspace-files-layout__browser :deep(.file-browser) {
  min-height: 620px;
}

.workspace-file-inspector {
  position: sticky;
  top: 84px;
}

.workspace-file-preview {
  display: grid;
  gap: var(--zy-sp-2);
  min-height: 160px;
  padding: var(--zy-sp-4);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background:
    linear-gradient(135deg, rgba(239, 246, 255, 0.96), rgba(248, 250, 252, 0.98)),
    var(--zy-surface-soft);
}

.workspace-file-preview span {
  align-self: start;
  justify-self: start;
  padding: 4px 8px;
  border-radius: var(--zy-radius-sm);
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-bold);
}

.workspace-file-preview strong {
  align-self: end;
  color: var(--zy-ink);
  font-size: var(--zy-fs-lg);
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.workspace-detail-list {
  margin: 0;
}

.workspace-detail-list div {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: var(--zy-sp-2);
  padding: 8px 0;
  border-bottom: var(--zy-border-soft);
}

.workspace-detail-list dt {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.workspace-detail-list dd {
  margin: 0;
  color: var(--zy-ink);
  overflow-wrap: anywhere;
}

.workspace-file-inspector__actions,
.workspace-delivery-tabs,
.workspace-quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.workspace-completion {
  display: grid;
  gap: 2px;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  text-align: center;
}

.workspace-completion strong {
  color: var(--zy-blue-700);
  font-size: var(--zy-fs-3xl);
  font-variant-numeric: tabular-nums;
}

.workspace-completion span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.workspace-master-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  min-width: 0;
}

.workspace-master-steps article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 4px var(--zy-sp-2);
  min-width: 0;
  padding: 18px;
  border: 1px solid oklch(0.88 0.024 255);
  border-radius: 12px;
  background: oklch(0.998 0.003 255);
}

.workspace-master-steps span {
  display: grid;
  place-items: center;
  grid-row: span 2;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
  font-family: var(--zy-font-mono);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-bold);
}

.workspace-master-steps strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
}

.workspace-master-steps em {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
}

.workspace-master-steps article.is-done {
  background: var(--zy-green-50);
  border-color: rgba(34, 197, 94, 0.24);
}

.workspace-master-steps article.is-current {
  border-color: rgba(245, 158, 11, 0.34);
}

.workspace-delivery-tabs {
  padding: 4px;
  border-radius: 999px;
  background: oklch(0.955 0.018 255);
}

.workspace-delivery-tabs button {
  min-height: 32px;
  padding: 0 18px;
  border-radius: 999px;
}

.workspace-delivery-tabs button:hover,
.workspace-delivery-tabs button.is-active {
  background: var(--zy-blue-600);
  color: oklch(0.99 0.004 255);
}

.workspace-timeline article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--zy-sp-2);
  min-width: 0;
}

.workspace-timeline article > span {
  width: 10px;
  height: 10px;
  margin-top: 4px;
  border-radius: 999px;
  background: var(--zy-line);
}

.workspace-timeline article.is-active > span {
  background: var(--zy-blue-600);
}

.workspace-timeline strong,
.workspace-model-list strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
}

.workspace-bim-kpis {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.workspace-subtabs {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow-x: auto;
  padding: 0 0 8px;
  border-bottom: 1px solid oklch(0.9 0.018 255);
}

.workspace-subtabs button {
  appearance: none;
  min-height: 34px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: oklch(0.38 0.035 260);
  cursor: pointer;
  font-family: inherit;
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.workspace-subtabs button:hover,
.workspace-subtabs button.is-active {
  background: oklch(0.955 0.03 255);
  color: var(--zy-blue-700);
}

.workspace-bim-viewer {
  position: relative;
  display: grid;
  align-content: center;
  min-height: 520px;
  overflow: hidden;
  padding: 18px;
  border: 1px solid oklch(0.88 0.024 255);
  border-radius: 12px;
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(239, 246, 255, 0.92)),
    var(--zy-surface);
  box-shadow: 0 10px 28px rgba(30, 64, 175, 0.045);
}

.workspace-bim-viewer.has-glandar-viewer {
  align-content: stretch;
  min-height: 620px;
  padding: 0;
  background: #020617;
}

.workspace-bim-viewer__iframe {
  width: 100%;
  min-height: 560px;
  border: 0;
  background: #020617;
}

.workspace-bim-viewer__empty {
  display: grid;
  justify-items: center;
  gap: var(--zy-sp-2);
  max-width: 520px;
  margin: 0 auto;
  padding: var(--zy-sp-8);
  border: 1px dashed rgba(37, 99, 235, 0.28);
  border-radius: 14px;
  background:
    radial-gradient(circle at 30% 20%, rgba(37, 99, 235, 0.12), transparent 28%),
    rgba(255, 255, 255, 0.72);
  text-align: center;
}

.workspace-bim-viewer__empty strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-lg);
}

.workspace-bim-viewer__empty span {
  color: var(--zy-muted);
  line-height: 1.7;
}

.workspace-bim-viewer__toolbar {
  position: absolute;
  right: 16px;
  bottom: 58px;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  padding: var(--zy-sp-2);
  border-radius: var(--zy-radius-base);
  background: rgba(15, 23, 42, 0.82);
  backdrop-filter: blur(10px);
}

.workspace-bim-viewer__toolbar button {
  min-height: 30px;
  padding: 0 var(--zy-sp-3);
  border-radius: var(--zy-radius-sm);
  color: rgba(255, 255, 255, 0.86);
}

.workspace-bim-viewer p {
  position: relative;
  z-index: 1;
  margin-top: var(--zy-sp-3);
  text-align: center;
}

.workspace-bim-viewer.has-glandar-viewer p {
  position: absolute;
  inset-inline: 18px;
  bottom: 16px;
  margin: 0;
  padding: 9px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.76);
  color: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(10px);
}

.workspace-model-list article {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--zy-sp-3);
  min-width: 0;
  padding: var(--zy-sp-2) 0;
  border-bottom: var(--zy-border-soft);
}

.workspace-model-list article > div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.workspace-model-list strong,
.workspace-model-list span,
.workspace-model-list em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-model-list em {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
}

.workspace-archive-grid .workspace-panel {
  min-height: 150px;
}

.workspace-archive-grid .workspace-panel strong {
  font-size: var(--zy-fs-2xl);
}

.workspace-boundary-strip {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding: 10px 16px;
  border: 1px solid oklch(0.84 0.055 255);
  border-radius: 10px;
  background: oklch(0.965 0.026 255);
  color: oklch(0.34 0.05 260);
  font-size: var(--zy-fs-sm);
}

.workspace-boundary-strip strong {
  color: var(--zy-blue-700);
  font-weight: var(--zy-fw-semi);
}

.workspace-boundary-strip span {
  flex: 0 0 auto;
  padding: 3px 10px;
  border: 1px solid oklch(0.84 0.055 255);
  border-radius: 999px;
  background: oklch(0.99 0.004 255);
  color: oklch(0.38 0.04 260);
}

.workspace-boundary-strip button {
  appearance: none;
  margin-left: auto;
  border: 0;
  background: transparent;
  color: var(--zy-blue-700);
  cursor: pointer;
  font-family: inherit;
  font-weight: var(--zy-fw-semi);
}

@media (max-width: 1280px) {
  .workspace-flow {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .workspace-overview__grid,
  .workspace-delivery-layout,
  .workspace-bim-layout,
  .workspace-files-layout,
  .workspace-master-layout {
    grid-template-columns: 1fr;
  }

  .workspace-file-inspector {
    position: static;
  }

  .workspace-bim-kpis,
  .workspace-master-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .workspace-boundary-strip {
    flex-wrap: wrap;
  }

  .workspace-boundary-strip button {
    margin-left: 0;
  }
}

@media (max-width: 760px) {
  .project-workbench-identity,
  .workspace-section-head,
  .workspace-next-action {
    grid-template-columns: 1fr;
  }

  .project-workbench-identity {
    align-items: stretch;
  }

  .project-workbench-identity__cover {
    height: 96px;
  }

  .workspace-section-head,
  .workspace-next-action {
    flex-direction: column;
  }

  .workspace-flow,
  .workspace-action-grid,
  .workspace-health-grid,
  .workspace-delivery-state-grid,
  .workspace-bim-kpis,
  .workspace-master-steps,
  .workspace-archive-grid {
    grid-template-columns: 1fr;
  }
}

/* ---- Project identity / 成熟项目工作台头部 ---- */
.asset-project-identity {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) auto;
  gap: var(--zy-sp-5);
  align-items: center;
  min-width: 0;
  padding: var(--zy-sp-4);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
}

.asset-project-identity__cover {
  min-width: 0;
  height: 116px;
  border-radius: var(--zy-radius-base);
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.82), rgba(30, 64, 175, 0.72)),
    linear-gradient(180deg, #dbeafe 0%, #f8fafc 100%);
  overflow: hidden;
  position: relative;
}

.asset-project-identity__cover::before {
  content: "";
  position: absolute;
  inset: 14px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: calc(var(--zy-radius-base) - 2px);
}

.asset-project-identity__skyline {
  position: absolute;
  left: 24px;
  right: 24px;
  bottom: 18px;
  display: grid;
  grid-template-columns: 0.85fr 1.2fr 0.75fr 1fr;
  gap: 8px;
  align-items: end;
}

.asset-project-identity__skyline i {
  display: block;
  border-radius: 3px 3px 0 0;
  background:
    repeating-linear-gradient(
      180deg,
      rgba(255, 255, 255, 0.86) 0,
      rgba(255, 255, 255, 0.86) 4px,
      rgba(255, 255, 255, 0.18) 4px,
      rgba(255, 255, 255, 0.18) 10px
    );
  opacity: 0.94;
}

.asset-project-identity__skyline i:nth-child(1) { height: 46px; }
.asset-project-identity__skyline i:nth-child(2) { height: 72px; }
.asset-project-identity__skyline i:nth-child(3) { height: 56px; }
.asset-project-identity__skyline i:nth-child(4) { height: 84px; }

.asset-project-identity__main {
  display: grid;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.asset-project-identity__eyebrow {
  color: var(--zy-blue-700);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0;
}

.asset-project-identity h1 {
  margin: 0;
  color: var(--zy-ink);
  font-size: var(--zy-fs-3xl);
  font-weight: var(--zy-fw-semi);
  letter-spacing: 0;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.asset-project-identity__meta,
.asset-project-identity__status,
.asset-project-identity__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.asset-project-identity__meta span {
  color: var(--zy-muted);
  font-size: var(--zy-fs-sm);
  line-height: 1.45;
}

.asset-project-identity__actions {
  justify-content: flex-end;
}

.asset-project-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  overflow-x: auto;
  padding: 4px;
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  box-shadow: var(--zy-shadow-xs);
}

.asset-project-tabs button {
  appearance: none;
  border: 0;
  border-radius: var(--zy-radius-sm);
  background: transparent;
  color: var(--zy-muted);
  cursor: pointer;
  flex: 0 0 auto;
  font-family: inherit;
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
  min-height: 36px;
  padding: 0 var(--zy-sp-4);
  transition:
    background-color var(--zy-duration-2) var(--zy-ease),
    color var(--zy-duration-2) var(--zy-ease),
    box-shadow var(--zy-duration-2) var(--zy-ease);
}

.asset-project-tabs button:hover {
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
}

.asset-project-tabs button.is-active {
  background: var(--zy-blue-600);
  color: white;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.18);
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

.asset-project-details__hint {
  margin: 0;
  color: var(--zy-text-soft);
  font-size: var(--zy-fs-sm);
  line-height: 1.7;
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

.asset-tabs--content-only :deep(.el-tabs__header) {
  display: none;
}

.asset-tabs--content-only :deep(.el-tabs__content) {
  min-width: 0;
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

.asset-detail-float {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 80;
  width: min(680px, calc(100vw - 56px));
  height: min(760px, calc(100dvh - 112px));
  min-width: 420px;
  min-height: 420px;
  max-width: calc(100vw - 32px);
  max-height: calc(100dvh - 32px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  resize: both;
  border: 1px solid oklch(0.88 0.03 255);
  border-radius: 14px;
  background: oklch(0.995 0.004 255);
  box-shadow:
    0 24px 56px oklch(0.34 0.07 255 / 0.18),
    0 0 0 1px oklch(1 0 0 / 0.72) inset;
}

.asset-detail-float__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--zy-sp-3);
  padding: 14px 16px;
  border-bottom: 1px solid oklch(0.91 0.018 255);
  background: oklch(0.985 0.01 255);
}

.asset-detail-float__header > div {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.asset-detail-float__header span,
.asset-detail-float__header small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.asset-detail-float__header span {
  color: var(--zy-blue-700);
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0.08em;
}

.asset-detail-float__header strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-base);
  font-weight: var(--zy-fw-semi);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-detail-float__body {
  min-height: 0;
  overflow: auto;
  padding: 16px;
}

.asset-detail-section {
  margin-bottom: var(--zy-sp-5);
}

.asset-detail-section h3 {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  margin: 0 0 var(--zy-sp-2);
  padding: 0 10px;
  border: 1px solid oklch(0.9 0.025 255);
  border-radius: 999px;
  background: oklch(0.975 0.018 255);
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-semi);
}

.asset-detail-actions {
  position: sticky;
  bottom: -16px;
  display: flex;
  flex-wrap: wrap;
  gap: var(--zy-sp-2);
  margin: 0 -16px -16px;
  padding: 12px 16px;
  border-top: 1px solid oklch(0.91 0.018 255);
  background: oklch(0.995 0.004 255 / 0.94);
  -webkit-backdrop-filter: blur(10px);
  backdrop-filter: blur(10px);
}

.preview-message {
  margin-top: 10px;
}

.preview-dialog-body {
  min-height: 260px;
}

@media (max-width: 720px) {
  .asset-detail-float {
    inset: 12px;
    width: auto;
    height: auto;
    min-width: 0;
    min-height: 0;
    resize: none;
  }
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
  .asset-project-identity {
    grid-template-columns: 150px minmax(0, 1fr);
  }

  .asset-project-identity__actions {
    grid-column: 1 / -1;
    justify-content: flex-start;
  }

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
  .asset-project-identity {
    grid-template-columns: 1fr;
  }

  .asset-project-identity__cover {
    height: 96px;
  }

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
