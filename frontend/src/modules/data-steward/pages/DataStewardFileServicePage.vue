<template>
  <section class="mvp-page file-service-page">
    <div class="mvp-page__header">
      <div>
        <h1>文件服务与对象存储</h1>
        <p>{{ projectLabel }}，集中管理受控文件访问和对象存储镜像任务。</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" :loading="loading" @click="refresh">刷新</el-button>
      </div>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="service-notice"
      title="当前只做对象存储镜像"
      description="NAS 原文件保留；任务中心不会生成语义证据，不代表 Hermes 已理解文件正文，也不会展示底层路径或底层对象定位信息。"
    />

    <section class="m3g-readiness">
      <article class="readiness-card">
        <span>对象存储就绪</span>
        <strong>{{ readinessStatusLabel(readiness?.readinessStatus) }}</strong>
        <el-tag :type="readinessTagType(readiness?.readinessStatus)" effect="plain">
          {{ endpointTypeLabel(readiness?.endpointType) }}
        </el-tag>
        <p>{{ readiness?.message || '正在读取对象存储 readiness。' }}</p>
      </article>
      <article class="readiness-card">
        <span>读写探测</span>
        <strong>{{ readiness?.readable && readiness?.writable ? '可读写' : '未完全通过' }}</strong>
        <p>只做专用 smoke 探测，不返回服务地址、底层对象定位信息或密钥。</p>
      </article>
      <article class="readiness-card readiness-card--wide">
        <span>全项目对象化覆盖率</span>
        <strong>{{ formatPercent(inventory?.objectificationCoverageRate) }}%</strong>
        <p>
          {{ formatCount(inventory?.totalProjects) }} 个项目，
          {{ formatCount(inventory?.totalFiles) }} 个文件，
          仍在 NAS {{ formatCount(inventory?.nasOnlyFiles) }} 个。
        </p>
      </article>
    </section>

    <section class="service-section read-policy-section">
      <div class="service-section__header">
        <div>
          <h2>读取策略与 fallback 状态</h2>
          <span>预览和下载先走对象存储；历史文件仍可按受控 NAS 读取，异常不会被伪装成成功。</span>
        </div>
        <el-tag :type="readPolicy?.nasFallbackEnabled ? 'warning' : 'success'" effect="plain">
          {{ readPolicy?.nasFallbackEnabled ? 'NAS fallback 已开启' : 'NAS fallback 已关闭' }}
        </el-tag>
      </div>
      <div class="m3g-policy-grid">
        <article>
          <span>对象优先读取</span>
          <strong>{{ readPolicy?.objectFirstEnabled ? '已启用' : '未启用' }}</strong>
          <p>已对象化文件默认从 NAS 侧 MinIO 读取。</p>
        </article>
        <article>
          <span>历史 NAS 文件</span>
          <strong>{{ formatCount(readPolicy?.nasOnlyCount) }}</strong>
          <p>访问时标记为 LEGACY_NAS，不显示真实路径。</p>
        </article>
        <article>
          <span>对象异常</span>
          <strong>{{ formatCount(readPolicy?.objectUnreadableCount) }}</strong>
          <p>对象副本不可读时返回错误，不静默回退。</p>
        </article>
        <article>
          <span>近 7 天 fallback</span>
          <strong>{{ formatCount(readPolicy?.recentNasFallbackCount) }}</strong>
          <p>如开启 fallback，访问票据和审计必须显式标记。</p>
        </article>
      </div>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="service-notice"
        :title="readPolicy?.policyMessage || '正在读取对象优先策略'"
        :description="`已对象化 ${formatCount(readPolicy?.objectStoredCount)}，迁移中 ${formatCount(readPolicy?.migrationPendingCount)}，迁移失败 ${formatCount(readPolicy?.migrationFailedCount)}，近 7 天对象读取异常 ${formatCount(readPolicy?.recentObjectReadFailureCount)}。`"
      />
    </section>

    <el-alert
      v-if="readiness?.readinessStatus === 'LOCAL_DEV_ONLY'"
      type="warning"
      :closable="false"
      show-icon
      class="service-notice"
      title="当前对象存储仍是本机开发环境"
      description="尚未确认 NAS 侧 MinIO，不能启动真实全项目对象化；本页 dry-run 只生成计划，不复制文件。"
    />

    <el-tabs v-model="activeTab" class="service-tabs">
      <el-tab-pane label="对象存储迁移" name="migration">
        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>项目对象化盘点</h2>
              <span>只基于 MySQL 台账和对象版本统计，不递归扫描真实 NAS。</span>
            </div>
            <el-tag type="info" effect="plain">M3G-6 覆盖率盘点</el-tag>
          </div>

          <el-table :data="inventoryRows" row-key="projectId" empty-text="暂无项目对象化盘点">
            <el-table-column label="项目" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
            </el-table-column>
            <el-table-column label="分类" width="120">
              <template #default="{ row }">
                <el-tag :type="projectCategoryTagType(row.projectCategory)" size="small">
                  {{ projectCategoryLabel(row.projectCategory) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="文件" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.totalFiles) }}</template>
            </el-table-column>
            <el-table-column label="已对象化" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.objectStoredFiles) }}</template>
            </el-table-column>
            <el-table-column label="仍在 NAS" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.nasOnlyFiles) }}</template>
            </el-table-column>
            <el-table-column label="待对象化容量" width="140" align="right">
              <template #default="{ row }">{{ formatBytes(row.estimatedObjectificationBytes) }}</template>
            </el-table-column>
            <el-table-column label="路径风险" width="100" align="right">
              <template #default="{ row }">{{ formatCount(row.unreadablePathFiles) }}</template>
            </el-table-column>
            <el-table-column label="覆盖率" width="150">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.objectificationCoverageRate || 0)" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column label="checksum" width="130">
              <template #default="{ row }">{{ formatPercent(row.checksumCoverageRate) }}%</template>
            </el-table-column>
            <el-table-column label="风险" width="90">
              <template #default="{ row }">
                <el-tag :type="riskTagType(row.riskLevel)" size="small">{{ riskLabel(row.riskLevel) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>105 全量对象化试运行</h2>
              <span>全量计划覆盖当前项目所有登记文件；执行时只取下一批，完成后继续刷新并推进。</span>
            </div>
            <div class="dry-run-actions__buttons">
              <el-button :loading="fullPlanLoading" @click="loadFullPlan">刷新全量计划</el-button>
              <el-button
                type="danger"
                plain
                :loading="fullPlanExecutionLoading"
                :disabled="!canExecuteFullPlanNextBatch"
                @click="executeFullPlanNextBatch"
              >
                执行下一批
              </el-button>
            </div>
          </div>

          <template v-if="fullPlan">
            <div class="dry-run-summary">
              <div>
                <span>项目文件</span>
                <strong>{{ formatCount(fullPlan.totalFileCount) }}</strong>
              </div>
              <div>
                <span>已对象化</span>
                <strong>{{ formatCount(fullPlan.objectStoredCount) }}</strong>
              </div>
              <div>
                <span>未对象化</span>
                <strong>{{ formatCount(fullPlan.nasOnlyCount) }}</strong>
              </div>
              <div>
                <span>覆盖率</span>
                <strong>{{ formatPercent(fullPlan.objectificationCoverageRate) }}%</strong>
              </div>
              <div>
                <span>待对象化容量</span>
                <strong>{{ formatBytes(fullPlan.nasOnlyBytes) }}</strong>
              </div>
              <div>
                <span>checksum</span>
                <strong>{{ formatPercent(fullPlan.checksumCoverageRate) }}%</strong>
              </div>
              <div>
                <span>下一批</span>
                <strong>{{ formatCount(fullPlan.nextBatchFileCount) }}</strong>
              </div>
              <div>
                <span>剩余批次</span>
                <strong>{{ formatCount(fullPlan.estimatedRemainingBatches) }}</strong>
              </div>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="service-notice"
              title="105 全量计划已生成，当前未一次性迁移全项目"
              :description="fullPlan.riskMessages.join(' ')"
            />
            <el-alert
              v-if="fullPlanExecutionResult"
              type="success"
              :closable="false"
              show-icon
              class="service-notice"
              title="下一批对象化执行已返回"
              :description="`成功 ${formatCount(fullPlanExecutionResult.createdCount)}，跳过 ${formatCount(fullPlanExecutionResult.skippedCount)}，失败 ${formatCount(fullPlanExecutionResult.failedCount)}。`"
            />
            <div class="dry-run-actions">
              <div>
                <strong>下一批建议</strong>
                <span>{{ fullPlan.nextBatchSuggestions.join(' ') }}</span>
              </div>
              <el-tag type="info" effect="plain">
                单批 {{ formatCount(fullPlan.batchFileLimit) }} 个 / {{ formatBytes(fullPlan.batchBytesLimit) }}
              </el-tag>
            </div>
            <el-table :data="fullPlan.nextBatchItems" row-key="fileId" empty-text="暂无可执行下一批文件">
              <el-table-column prop="assetUuid" label="平台资产ID" min-width="220" show-overflow-tooltip />
              <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
              <el-table-column prop="fileKind" label="类型" width="90" />
              <el-table-column label="大小" width="110" align="right">
                <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
              </el-table-column>
              <el-table-column prop="checksumStatus" label="checksum" width="150" />
              <el-table-column prop="reason" label="计划原因" min-width="180" show-overflow-tooltip />
            </el-table>
            <el-table
              v-if="fullPlan.governanceItems.length > 0"
              :data="fullPlan.governanceItems"
              row-key="fileId"
              class="task-detail__rows"
              empty-text="暂无治理项"
            >
              <el-table-column prop="assetUuid" label="治理资产ID" min-width="220" show-overflow-tooltip />
              <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
              <el-table-column prop="storageStatus" label="状态" width="150" />
              <el-table-column prop="reason" label="治理原因" min-width="220" show-overflow-tooltip />
            </el-table>
          </template>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>105 对象化长跑控制</h2>
              <span>按硬上限分批推进，可暂停、继续和重试失败项；不启动无边界后台迁移。</span>
            </div>
            <div class="dry-run-actions__buttons">
              <el-button :loading="longRunLoading" @click="loadLongRunStatus">刷新状态</el-button>
              <el-button
                type="danger"
                plain
                :loading="longRunActionLoading"
                :disabled="!canStartLongRun"
                @click="startLongRun"
              >
                开始 / 继续
              </el-button>
              <el-button
                plain
                :loading="longRunActionLoading"
                :disabled="!canPauseLongRun"
                @click="pauseLongRun"
              >
                暂停
              </el-button>
              <el-button
                type="primary"
                plain
                :loading="longRunActionLoading"
                :disabled="!canResumeLongRun"
                @click="resumeLongRun"
              >
                继续
              </el-button>
              <el-button plain :loading="longRunActionLoading" @click="retryLongRunFailures">
                重试失败项
              </el-button>
            </div>
          </div>

          <div class="multi-plan-form">
            <label class="long-run-field">
              <span>每批文件数</span>
              <el-input-number v-model="longRunForm.batchFileLimit" :min="1" :max="15" controls-position="right" />
            </label>
            <label class="long-run-field">
              <span>每批容量 MB</span>
              <el-input-number v-model="longRunForm.batchBytesMb" :min="1" :max="512" controls-position="right" />
            </label>
            <label class="long-run-field">
              <span>单文件上限 MB</span>
              <el-input-number v-model="longRunForm.maxFileSizeMb" :min="1" :max="500" controls-position="right" />
            </label>
            <label class="long-run-field">
              <span>连续批次数</span>
              <el-input-number v-model="longRunForm.maxContinuousBatches" :min="1" :max="5" controls-position="right" />
            </label>
            <el-switch
              v-model="longRunForm.continueOnFailure"
              active-text="失败后继续"
              inactive-text="失败即停"
            />
          </div>

          <template v-if="longRunStatus">
            <div class="dry-run-summary">
              <div>
                <span>长跑状态</span>
                <strong>{{ longRunStateLabel(longRunStatus.runState) }}</strong>
              </div>
              <div>
                <span>已完成批次</span>
                <strong>{{ formatCount(longRunStatus.processedBatchCount) }}</strong>
              </div>
              <div>
                <span>已对象化</span>
                <strong>{{ formatCount(longRunStatus.objectStoredCount) }}</strong>
              </div>
              <div>
                <span>剩余可执行</span>
                <strong>{{ formatCount(longRunStatus.eligibleRemainingCount) }}</strong>
              </div>
              <div>
                <span>治理项</span>
                <strong>{{ formatCount(longRunStatus.governanceItemCount) }}</strong>
              </div>
              <div>
                <span>覆盖率</span>
                <strong>{{ formatPercent(longRunStatus.objectificationCoverageRate) }}%</strong>
              </div>
              <div>
                <span>本次成功</span>
                <strong>{{ formatCount(longRunStatus.createdCount) }}</strong>
              </div>
              <div>
                <span>本次失败</span>
                <strong>{{ formatCount(longRunStatus.failedCount) }}</strong>
              </div>
            </div>
            <div class="dry-run-actions">
              <div>
                <strong>批次边界</strong>
                <span>
                  单批 {{ formatCount(longRunStatus.batchFileLimit) }} 个 /
                  {{ formatBytes(longRunStatus.batchBytesLimit) }}，连续
                  {{ formatCount(longRunStatus.maxContinuousBatches) }} 批。
                </span>
              </div>
              <el-tag :type="longRunStateTagType(longRunStatus.runState)" effect="plain">
                {{ longRunStateLabel(longRunStatus.runState) }}
              </el-tag>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="service-notice"
              title="105 长跑控制已就绪"
              :description="longRunStatus.warnings.join(' ')"
            />
            <el-alert
              v-if="longRunStatus.lastFailureReason"
              type="warning"
              :closable="false"
              show-icon
              class="service-notice"
              title="最近失败原因"
              :description="longRunStatus.lastFailureReason"
            />
            <el-table
              v-if="longRunStatus.governanceReasons.length > 0"
              :data="longRunStatus.governanceReasons"
              row-key="reasonCode"
              class="task-detail__rows"
              empty-text="暂无治理原因分组"
            >
              <el-table-column prop="reasonCode" label="治理原因" min-width="220" show-overflow-tooltip />
              <el-table-column prop="message" label="说明" min-width="320" show-overflow-tooltip />
              <el-table-column label="文件数" width="120" align="right">
                <template #default="{ row }">{{ formatCount(row.fileCount) }}</template>
              </el-table-column>
            </el-table>
            <el-table
              v-if="longRunStatus.governanceItems.length > 0"
              :data="longRunStatus.governanceItems"
              row-key="fileId"
              class="task-detail__rows"
              empty-text="暂无治理项"
            >
              <el-table-column prop="assetUuid" label="治理资产ID" min-width="220" show-overflow-tooltip />
              <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
              <el-table-column label="大小" width="110" align="right">
                <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
              </el-table-column>
              <el-table-column prop="storageStatus" label="状态" width="150" />
              <el-table-column prop="reason" label="治理原因" min-width="220" show-overflow-tooltip />
            </el-table>
          </template>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>M3G-7R 全项目对象化跑批</h2>
              <span>按全项目队列持续推进真实项目对象化；执行前必须 dry-run 和人工确认，硬上限防止无边界迁移。</span>
            </div>
            <div class="dry-run-actions__buttons">
              <el-button :loading="runOverviewLoading" @click="loadRunOverview">刷新队列</el-button>
              <el-button
                type="primary"
                :loading="runDryRunLoading"
                :disabled="!readinessReady"
                @click="runAllProjectDryRun"
              >
                生成 dry-run
              </el-button>
            </div>
          </div>

          <div class="m3g-policy-grid">
            <article>
              <span>项目上限</span>
              <strong>{{ formatCount(runOverview?.maxProjectCount) }}</strong>
            </article>
            <article>
              <span>总文件上限</span>
              <strong>{{ formatCount(runOverview?.maxTotalFiles) }}</strong>
            </article>
            <article>
              <span>总容量上限</span>
              <strong>{{ formatBytes(runOverview?.maxTotalBytes) }}</strong>
            </article>
            <article>
              <span>连续批次数</span>
              <strong>{{ formatCount(runOverview?.maxContinuousBatches) }}</strong>
            </article>
          </div>

          <div class="dry-run-summary">
            <div>
              <span>全局文件</span>
              <strong>{{ formatCount(runOverview?.totalFiles) }}</strong>
            </div>
            <div>
              <span>已对象化</span>
              <strong>{{ formatCount(runOverview?.objectStoredFiles) }}</strong>
            </div>
            <div>
              <span>仍在 NAS</span>
              <strong>{{ formatCount(runOverview?.nasOnlyFiles) }}</strong>
            </div>
            <div>
              <span>失败 / 治理</span>
              <strong>{{ formatCount(runOverview?.migrationFailedFiles) }} / {{ formatCount(runOverview?.governanceItemCount) }}</strong>
            </div>
            <div>
              <span>对象化覆盖率</span>
              <strong>{{ formatPercent(runOverview?.objectificationCoverageRate) }}%</strong>
            </div>
            <div>
              <span>checksum 覆盖率</span>
              <strong>{{ formatPercent(runOverview?.checksumCoverageRate) }}%</strong>
            </div>
            <div>
              <span>可执行项目</span>
              <strong>{{ formatCount(runOverview?.executableProjectCount) }}</strong>
            </div>
            <div>
              <span>需治理项目</span>
              <strong>{{ formatCount(runOverview?.governanceProjectCount) }}</strong>
            </div>
          </div>

          <div class="multi-plan-form run-plan-form">
            <label class="long-run-field">
              <span>最多项目</span>
              <el-input-number v-model="runForm.maxProjects" :min="1" :max="5" controls-position="right" />
            </label>
            <label class="long-run-field">
              <span>总文件</span>
              <el-input-number v-model="runForm.maxTotalFiles" :min="1" :max="200" controls-position="right" />
            </label>
            <label class="long-run-field">
              <span>每项目文件</span>
              <el-input-number v-model="runForm.maxFilesPerProject" :min="1" :max="50" controls-position="right" />
            </label>
            <label class="long-run-field">
              <span>总容量 MB</span>
              <el-input-number v-model="runForm.maxTotalMb" :min="1" :max="2048" controls-position="right" />
            </label>
            <label class="long-run-field">
              <span>单项目 MB</span>
              <el-input-number v-model="runForm.maxBytesPerProjectMb" :min="1" :max="2048" controls-position="right" />
            </label>
            <label class="long-run-field">
              <span>单文件 MB</span>
              <el-input-number v-model="runForm.maxFileSizeMb" :min="1" :max="500" controls-position="right" />
            </label>
            <label class="long-run-field">
              <span>连续批次</span>
              <el-input-number v-model="runForm.maxContinuousBatches" :min="1" :max="3" controls-position="right" />
            </label>
            <el-switch v-model="runForm.continueOnFailure" active-text="失败后继续" inactive-text="失败即停" />
          </div>

          <el-alert
            type="info"
            :closable="false"
            show-icon
            class="service-notice"
            title="全项目跑批安全边界"
            :description="(runOverview?.warnings ?? ['只复制对象存储副本；不移动、不删除、不重命名、不覆盖 NAS 原文件；不读取文件正文。']).join(' ')"
          />

          <div class="dry-run-actions">
            <div>
              <strong>执行控制</strong>
              <span>{{ runExecutionHint }}</span>
            </div>
            <div class="dry-run-actions__buttons">
              <el-button
                type="danger"
                plain
                :loading="runExecutionLoading"
                :disabled="!canExecuteRun"
                @click="startAllProjectRun"
              >
                开始
              </el-button>
              <el-button
                type="primary"
                plain
                :loading="runExecutionLoading"
                :disabled="!readinessReady"
                @click="continueAllProjectRun"
              >
                继续
              </el-button>
              <el-button plain :loading="runPauseLoading" @click="pauseAllProjectRun">暂停</el-button>
              <el-button plain :loading="runRetryLoading" :disabled="!readinessReady" @click="retryFailedAllProjectRun">
                重试失败项
              </el-button>
            </div>
          </div>

          <template v-if="runPlanResult">
            <div class="dry-run-summary">
              <div>
                <span>规划项目</span>
                <strong>{{ formatCount(runPlanResult.plannedProjectCount) }}</strong>
              </div>
              <div>
                <span>选中文件</span>
                <strong>{{ formatCount(runPlanResult.selectedFileCount) }}</strong>
              </div>
              <div>
                <span>预估容量</span>
                <strong>{{ formatBytes(runPlanResult.selectedTotalBytes) }}</strong>
              </div>
              <div>
                <span>预估批次</span>
                <strong>{{ formatCount(runPlanResult.estimatedBatches) }}</strong>
              </div>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="service-notice"
              title="全项目 dry-run 已生成，未创建迁移任务"
              :description="runPlanResult.riskMessages.join(' ')"
            />
            <el-table :data="runPlanResult.projects" row-key="projectId" empty-text="暂无 dry-run 项目">
              <el-table-column label="项目" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
              </el-table-column>
              <el-table-column label="选中文件" width="100" align="right">
                <template #default="{ row }">{{ formatCount(row.selectedFileCount) }}</template>
              </el-table-column>
              <el-table-column label="容量" width="120" align="right">
                <template #default="{ row }">{{ formatBytes(row.selectedTotalBytes) }}</template>
              </el-table-column>
              <el-table-column label="跳过" width="80" align="right">
                <template #default="{ row }">{{ formatCount(row.objectStoredSkipCount) }}</template>
              </el-table-column>
              <el-table-column label="风险" min-width="260" show-overflow-tooltip>
                <template #default="{ row }">{{ row.riskMessages.join(' ') }}</template>
              </el-table-column>
            </el-table>
          </template>

          <template v-if="runExecutionResult">
            <div class="dry-run-summary">
              <div>
                <span>执行项目</span>
                <strong>{{ formatCount(runExecutionResult.selectedProjectCount) }}</strong>
              </div>
              <div>
                <span>执行文件</span>
                <strong>{{ formatCount(runExecutionResult.selectedFileCount) }}</strong>
              </div>
              <div>
                <span>成功</span>
                <strong>{{ formatCount(runExecutionResult.createdCount) }}</strong>
              </div>
              <div>
                <span>跳过 / 失败</span>
                <strong>{{ formatCount(runExecutionResult.skippedCount) }} / {{ formatCount(runExecutionResult.failedCount) }}</strong>
              </div>
            </div>
            <el-alert
              type="success"
              :closable="false"
              show-icon
              class="service-notice"
              title="全项目跑批执行已返回"
              :description="runExecutionResult.warnings.join(' ')"
            />
            <el-table :data="runExecutionResult.projectResults" row-key="taskId" empty-text="暂无执行结果">
              <el-table-column label="项目" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
              </el-table-column>
              <el-table-column prop="taskId" label="任务ID" width="90" />
              <el-table-column label="文件" width="90" align="right">
                <template #default="{ row }">{{ formatCount(row.selectedFileCount) }}</template>
              </el-table-column>
              <el-table-column label="成功" width="80" align="right">
                <template #default="{ row }">{{ formatCount(row.successCount) }}</template>
              </el-table-column>
              <el-table-column label="跳过" width="80" align="right">
                <template #default="{ row }">{{ formatCount(row.skippedCount) }}</template>
              </el-table-column>
              <el-table-column label="失败" width="80" align="right">
                <template #default="{ row }">{{ formatCount(row.failureCount) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.taskStatus)" size="small">{{ taskStatusLabel(row.taskStatus) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </template>

          <el-table
            v-loading="runOverviewLoading"
            :data="runExecutableRows"
            row-key="projectId"
            empty-text="暂无可执行项目"
          >
            <el-table-column label="可执行项目" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
            </el-table-column>
            <el-table-column label="仍在 NAS" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.nasOnlyFiles) }}</template>
            </el-table-column>
            <el-table-column label="失败" width="90" align="right">
              <template #default="{ row }">{{ formatCount(row.migrationFailedFiles) }}</template>
            </el-table-column>
            <el-table-column label="覆盖率" width="140">
              <template #default="{ row }">{{ formatPercent(row.objectificationCoverageRate) }}%</template>
            </el-table-column>
            <el-table-column label="说明" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">{{ row.riskMessages.join(' ') }}</template>
            </el-table-column>
          </el-table>

          <el-table
            v-if="runGovernanceRows.length > 0"
            :data="runGovernanceRows.slice(0, 8)"
            row-key="projectId"
            class="task-detail__rows"
            empty-text="暂无需治理项目"
          >
            <el-table-column label="需治理项目" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
            </el-table-column>
            <el-table-column label="原因" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ runQueueReasonLabel(row.queueReason) }}</template>
            </el-table-column>
            <el-table-column label="治理项" width="100" align="right">
              <template #default="{ row }">{{ formatCount(row.governanceItemCount) }}</template>
            </el-table-column>
            <el-table-column label="说明" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">{{ row.riskMessages.join(' ') }}</template>
            </el-table-column>
          </el-table>

          <el-table
            v-if="runCompletedRows.length > 0"
            :data="runCompletedRows.slice(0, 6)"
            row-key="projectId"
            class="task-detail__rows"
            empty-text="暂无已完成项目"
          >
            <el-table-column label="已完成项目" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
            </el-table-column>
            <el-table-column label="总文件" width="100" align="right">
              <template #default="{ row }">{{ formatCount(row.totalFiles) }}</template>
            </el-table-column>
            <el-table-column label="覆盖率" width="140">
              <template #default="{ row }">{{ formatPercent(row.objectificationCoverageRate) }}%</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="runQueueStatusTagType(row.queueStatus)" size="small">
                  {{ runQueueStatusLabel(row.queueStatus) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>M3G-7 多真实项目对象化 Wave 1</h2>
              <span>自动筛选非 105 的低风险真实项目；先生成 dry-run，再人工确认执行小批对象化。</span>
            </div>
            <div class="dry-run-actions__buttons">
              <el-button :loading="waveCandidatesLoading || waveReportLoading" @click="refreshWave1">刷新候选</el-button>
              <el-button
                type="primary"
                :loading="waveDryRunLoading"
                :disabled="!readinessReady || waveDefaultProjectIds.length === 0"
                @click="runWaveDryRun"
              >
                生成 Wave 1 dry-run
              </el-button>
            </div>
          </div>

          <div class="m3g-policy-grid">
            <article>
              <span>候选项目上限</span>
              <strong>{{ formatCount(waveCandidates?.maxProjectCount) }}</strong>
            </article>
            <article>
              <span>总文件上限</span>
              <strong>{{ formatCount(waveCandidates?.maxTotalFiles) }}</strong>
            </article>
            <article>
              <span>总容量上限</span>
              <strong>{{ formatBytes(waveCandidates?.maxTotalBytes) }}</strong>
            </article>
            <article>
              <span>单文件上限</span>
              <strong>{{ formatBytes(waveCandidates?.maxFileSizeBytes) }}</strong>
            </article>
          </div>

          <el-alert
            type="info"
            :closable="false"
            show-icon
            class="service-notice"
            title="Wave 1 受控边界"
            :description="(waveCandidates?.warnings ?? ['本轮只复制对象存储副本，不移动、不删除、不重命名、不覆盖 NAS 原文件。']).join(' ')"
          />

          <el-table
            v-loading="waveCandidatesLoading"
            :data="waveCandidates?.candidates ?? []"
            row-key="projectId"
            empty-text="暂无符合 Wave 1 条件的候选项目"
          >
            <el-table-column label="候选项目" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
            </el-table-column>
            <el-table-column label="总文件" width="100" align="right">
              <template #default="{ row }">{{ formatCount(row.totalFiles) }}</template>
            </el-table-column>
            <el-table-column label="仍在 NAS" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.nasOnlyFiles) }}</template>
            </el-table-column>
            <el-table-column label="建议小批" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.recommendedFileCount) }}</template>
            </el-table-column>
            <el-table-column label="建议容量" width="120" align="right">
              <template #default="{ row }">{{ formatBytes(row.recommendedBytes) }}</template>
            </el-table-column>
            <el-table-column label="覆盖率" width="150">
              <template #default="{ row }">
                <el-progress :percentage="Number(row.objectificationCoverageRate || 0)" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column label="治理提示" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">{{ (row.riskMessages ?? []).join(' ') }}</template>
            </el-table-column>
          </el-table>

          <el-table
            v-if="(waveCandidates?.excludedProjects ?? []).length > 0"
            :data="(waveCandidates?.excludedProjects ?? []).slice(0, 8)"
            row-key="projectId"
            class="task-detail__rows"
            empty-text="暂无排除项目"
          >
            <el-table-column label="排除项目" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
            </el-table-column>
            <el-table-column label="原因" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ waveExclusionReasonLabel(row.exclusionReason) }}</template>
            </el-table-column>
            <el-table-column label="仍在 NAS" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.nasOnlyFiles) }}</template>
            </el-table-column>
            <el-table-column label="治理提示" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">{{ (row.riskMessages ?? []).join(' ') }}</template>
            </el-table-column>
          </el-table>

          <template v-if="wavePlanResult">
            <div class="dry-run-summary">
              <div>
                <span>规划项目</span>
                <strong>{{ formatCount(wavePlanResult.plannedProjectCount) }}</strong>
              </div>
              <div>
                <span>选中文件</span>
                <strong>{{ formatCount(wavePlanResult.selectedFileCount) }}</strong>
              </div>
              <div>
                <span>预估容量</span>
                <strong>{{ formatBytes(wavePlanResult.selectedTotalBytes) }}</strong>
              </div>
              <div>
                <span>预估批次</span>
                <strong>{{ formatCount(wavePlanResult.estimatedBatches) }}</strong>
              </div>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="service-notice"
              title="Wave 1 dry-run 已生成，未创建迁移任务"
              :description="wavePlanResult.riskMessages.join(' ')"
            />
            <div class="dry-run-actions">
              <div>
                <strong>确认执行 Wave 1 小批对象化</strong>
                <span>{{ waveExecutionHint }}</span>
              </div>
              <div class="dry-run-actions__buttons">
                <el-button
                  type="danger"
                  plain
                  :loading="waveExecutionLoading"
                  :disabled="!canExecuteWave"
                  @click="executeWavePlan"
                >
                  确认执行 Wave 1
                </el-button>
              </div>
            </div>
            <template v-if="waveExecutionResult">
              <div class="dry-run-summary">
                <div>
                  <span>执行项目</span>
                  <strong>{{ formatCount(waveExecutionResult.selectedProjectCount) }}</strong>
                </div>
                <div>
                  <span>执行文件</span>
                  <strong>{{ formatCount(waveExecutionResult.selectedFileCount) }}</strong>
                </div>
                <div>
                  <span>成功</span>
                  <strong>{{ formatCount(waveExecutionResult.createdCount) }}</strong>
                </div>
                <div>
                  <span>跳过 / 失败</span>
                  <strong>{{ formatCount(waveExecutionResult.skippedCount) }} / {{ formatCount(waveExecutionResult.failedCount) }}</strong>
                </div>
              </div>
              <el-alert
                type="success"
                :closable="false"
                show-icon
                class="service-notice"
                title="Wave 1 执行已返回"
                :description="waveExecutionResult.warnings.join(' ')"
              />
              <el-table :data="waveExecutionResult.projectResults" row-key="taskId" empty-text="暂无 Wave 1 执行结果">
                <el-table-column label="项目" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
                </el-table-column>
                <el-table-column prop="taskId" label="任务ID" width="90" />
                <el-table-column label="文件" width="90" align="right">
                  <template #default="{ row }">{{ formatCount(row.selectedFileCount) }}</template>
                </el-table-column>
                <el-table-column label="成功" width="80" align="right">
                  <template #default="{ row }">{{ formatCount(row.successCount) }}</template>
                </el-table-column>
                <el-table-column label="跳过" width="80" align="right">
                  <template #default="{ row }">{{ formatCount(row.skippedCount) }}</template>
                </el-table-column>
                <el-table-column label="失败" width="80" align="right">
                  <template #default="{ row }">{{ formatCount(row.failureCount) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag :type="statusType(row.taskStatus)" size="small">{{ taskStatusLabel(row.taskStatus) }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <el-table :data="wavePlanResult.projects" row-key="projectId" empty-text="暂无 Wave 1 dry-run 项目">
              <el-table-column label="项目" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
              </el-table-column>
              <el-table-column label="选中文件" width="100" align="right">
                <template #default="{ row }">{{ formatCount(row.selectedFileCount) }}</template>
              </el-table-column>
              <el-table-column label="容量" width="120" align="right">
                <template #default="{ row }">{{ formatBytes(row.selectedTotalBytes) }}</template>
              </el-table-column>
              <el-table-column label="跳过" width="80" align="right">
                <template #default="{ row }">{{ formatCount(row.objectStoredSkipCount) }}</template>
              </el-table-column>
              <el-table-column label="风险" min-width="260" show-overflow-tooltip>
                <template #default="{ row }">{{ row.riskMessages.join(' ') }}</template>
              </el-table-column>
            </el-table>
          </template>

          <el-table
            v-if="waveReports"
            v-loading="waveReportLoading"
            :data="waveReports.projects.slice(0, 8)"
            row-key="projectId"
            class="task-detail__rows"
            empty-text="暂无 Wave 1 覆盖率报告"
          >
            <el-table-column label="覆盖率报告" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
            </el-table-column>
            <el-table-column label="总文件" width="100" align="right">
              <template #default="{ row }">{{ formatCount(row.totalFiles) }}</template>
            </el-table-column>
            <el-table-column label="已对象化" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.objectStoredFiles) }}</template>
            </el-table-column>
            <el-table-column label="仍在 NAS" width="110" align="right">
              <template #default="{ row }">{{ formatCount(row.nasOnlyFiles) }}</template>
            </el-table-column>
            <el-table-column label="覆盖率" width="140">
              <template #default="{ row }">{{ formatPercent(row.objectificationCoverageRate) }}%</template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.executable ? 'success' : 'info'" size="small">
                  {{ row.executable ? '候选' : '排除' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>多项目小批规划</h2>
              <span>历史过渡能力；M3G-6 主验收以 105 全量计划和连续分批执行为准。</span>
            </div>
            <el-button type="primary" :loading="multiPlanLoading" @click="runMultiProjectDryRun">生成多项目计划</el-button>
          </div>

          <div class="multi-plan-form">
            <el-input
              v-model="multiPlanForm.projectIdsText"
              clearable
              placeholder="项目ID，可留空使用可访问项目；例如 503,506"
            />
            <el-switch
              v-model="multiPlanForm.realProjectsOnly"
              active-text="仅真实项目"
              inactive-text="全部可访问"
            />
            <el-select v-model="multiPlanForm.storageState" placeholder="存储状态">
              <el-option label="仅 NAS" value="NAS_ONLY" />
              <el-option label="全部" value="ANY" />
              <el-option label="迁移失败" value="MIGRATION_FAILED" />
            </el-select>
            <el-input v-model="multiPlanForm.extensionsText" clearable placeholder="扩展名：pdf,dwg,docx" />
            <el-input-number v-model="multiPlanForm.limit" :min="1" :max="15" controls-position="right" />
            <el-input-number v-model="multiPlanForm.maxFilesPerProject" :min="1" :max="15" controls-position="right" />
          </div>

          <div class="m3g-policy-grid">
            <article>
              <span>总容量上限</span>
              <strong>{{ formatBytes(multiPlanMaxTotalBytes) }}</strong>
            </article>
            <article>
              <span>单项目容量上限</span>
              <strong>{{ formatBytes(multiPlanMaxBytesPerProject) }}</strong>
            </article>
            <article>
              <span>并发上限预留</span>
              <strong>{{ multiPlanForm.concurrencyLimit }}</strong>
            </article>
            <article>
              <span>暂停 / 继续</span>
              <strong>字段预留</strong>
            </article>
          </div>

          <template v-if="multiPlanResult">
            <div class="dry-run-summary">
              <div>
                <span>规划项目</span>
                <strong>{{ formatCount(multiPlanResult.plannedProjectCount) }}</strong>
              </div>
              <div>
                <span>选中文件</span>
                <strong>{{ formatCount(multiPlanResult.selectedFileCount) }}</strong>
              </div>
              <div>
                <span>预估容量</span>
                <strong>{{ formatBytes(multiPlanResult.selectedTotalBytes) }}</strong>
              </div>
              <div>
                <span>预估批次</span>
                <strong>{{ formatCount(multiPlanResult.estimatedBatches) }}</strong>
              </div>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="service-notice"
              title="多项目 dry-run 已生成，未创建迁移任务"
              :description="multiPlanResult.riskMessages.join(' ')"
            />
            <div class="dry-run-actions">
              <div>
                <strong>受控多项目小批对象化</strong>
                <span>{{ multiExecutionHint }}</span>
              </div>
              <div class="dry-run-actions__buttons">
                <el-button
                  type="danger"
                  plain
                  :loading="multiExecutionLoading"
                  :disabled="!canExecuteMultiPlan"
                  @click="executeMultiProjectPlan"
                >
                  确认执行小批对象化
                </el-button>
              </div>
            </div>
            <template v-if="multiExecutionResult">
              <div class="dry-run-summary">
                <div>
                  <span>执行项目</span>
                  <strong>{{ formatCount(multiExecutionResult.selectedProjectCount) }}</strong>
                </div>
                <div>
                  <span>执行文件</span>
                  <strong>{{ formatCount(multiExecutionResult.selectedFileCount) }}</strong>
                </div>
                <div>
                  <span>成功</span>
                  <strong>{{ formatCount(multiExecutionResult.createdCount) }}</strong>
                </div>
                <div>
                  <span>跳过 / 失败</span>
                  <strong>{{ formatCount(multiExecutionResult.skippedCount) }} / {{ formatCount(multiExecutionResult.failedCount) }}</strong>
                </div>
              </div>
              <el-alert
                type="success"
                :closable="false"
                show-icon
                class="service-notice"
                title="小批对象化执行已返回"
                :description="multiExecutionResult.warnings.join(' ')"
              />
              <el-alert
                v-if="multiExecutionResult.failureReasons.length > 0"
                type="warning"
                :closable="false"
                show-icon
                class="service-notice"
                title="存在失败原因，可在任务详情中重试或复核"
                :description="multiExecutionResult.failureReasons.join(' ')"
              />
              <el-table :data="multiExecutionResult.projectResults" row-key="taskId" empty-text="暂无执行结果">
                <el-table-column label="项目" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
                </el-table-column>
                <el-table-column prop="taskId" label="任务ID" width="90" />
                <el-table-column label="文件" width="80" align="right">
                  <template #default="{ row }">{{ formatCount(row.selectedFileCount) }}</template>
                </el-table-column>
                <el-table-column label="成功" width="80" align="right">
                  <template #default="{ row }">{{ formatCount(row.successCount) }}</template>
                </el-table-column>
                <el-table-column label="跳过" width="80" align="right">
                  <template #default="{ row }">{{ formatCount(row.skippedCount) }}</template>
                </el-table-column>
                <el-table-column label="失败" width="80" align="right">
                  <template #default="{ row }">{{ formatCount(row.failureCount) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="120">
                  <template #default="{ row }">
                    <el-tag :type="statusType(row.taskStatus)" size="small">{{ taskStatusLabel(row.taskStatus) }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <el-table :data="multiPlanResult.projects" row-key="projectId" empty-text="暂无多项目规划结果">
              <el-table-column label="项目" min-width="220" show-overflow-tooltip>
                <template #default="{ row }">{{ row.projectCode }} {{ row.projectName }}</template>
              </el-table-column>
              <el-table-column label="分类" width="120">
                <template #default="{ row }">
                  <el-tag :type="projectCategoryTagType(row.projectCategory)" size="small">
                    {{ projectCategoryLabel(row.projectCategory) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="选中文件" width="100" align="right">
                <template #default="{ row }">{{ formatCount(row.selectedFileCount) }}</template>
              </el-table-column>
              <el-table-column label="容量" width="120" align="right">
                <template #default="{ row }">{{ formatBytes(row.selectedTotalBytes) }}</template>
              </el-table-column>
              <el-table-column label="跳过" width="80" align="right">
                <template #default="{ row }">{{ formatCount(row.objectStoredSkipCount) }}</template>
              </el-table-column>
              <el-table-column label="风险" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">{{ row.riskMessages.join(' ') }}</template>
              </el-table-column>
            </el-table>
          </template>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>单项目对象化 dry-run</h2>
              <span>生成筛选计划，不创建迁移任务、不复制文件、不修改 NAS。</span>
            </div>
            <el-button type="primary" :loading="dryRunLoading" @click="runDryRun">生成 dry-run 计划</el-button>
          </div>

          <div class="dry-run-form">
            <el-input v-model="dryRunForm.directoryPath" clearable placeholder="逻辑目录，可留空，例如 01_文件收发" />
            <el-select v-model="dryRunForm.storageState" placeholder="存储状态">
              <el-option label="全部" value="ANY" />
              <el-option label="仅 NAS" value="NAS_ONLY" />
              <el-option label="迁移失败" value="MIGRATION_FAILED" />
            </el-select>
            <el-select v-model="dryRunForm.checksumState" placeholder="checksum">
              <el-option label="全部" value="ANY" />
              <el-option label="已有 checksum" value="HAS_CHECKSUM" />
              <el-option label="缺少 checksum" value="MISSING_CHECKSUM" />
            </el-select>
            <el-input v-model="dryRunForm.extensionsText" clearable placeholder="扩展名：pdf,dwg,rvt，可留空" />
            <el-input-number v-model="dryRunForm.limit" :min="1" :max="5000" controls-position="right" />
          </div>

          <template v-if="dryRunResult">
            <div class="dry-run-summary">
              <div>
                <span>选中文件</span>
                <strong>{{ formatCount(dryRunResult.selectedFileCount) }}</strong>
              </div>
              <div>
                <span>预估容量</span>
                <strong>{{ formatBytes(dryRunResult.selectedTotalBytes) }}</strong>
              </div>
              <div>
                <span>预估批次</span>
                <strong>{{ formatCount(dryRunResult.estimatedBatches) }}</strong>
              </div>
              <div>
                <span>已对象化跳过</span>
                <strong>{{ formatCount(dryRunResult.objectStoredSkipCount) }}</strong>
              </div>
            </div>
            <div class="dry-run-actions">
              <div>
                <strong>105 小批灰度对象化</strong>
                <span>{{ dryRunGrayHint }}</span>
              </div>
              <div class="dry-run-actions__buttons">
                <el-button :disabled="dryRunExecutableFileIds.length === 0" @click="appendDryRunSelection">
                  加入小批清单
                </el-button>
                <el-button
                  type="primary"
                  :loading="creating"
                  :disabled="!canRunDryRunGrayTask"
                  @click="createTaskFromDryRun"
                >
                  执行小批灰度
                </el-button>
              </div>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              class="service-notice"
              :title="dryRunResult.dryRun && !dryRunResult.migrationStarted ? 'dry-run 已生成，未启动迁移' : '请检查 dry-run 状态'"
              :description="dryRunResult.riskMessages.join(' ')"
            />
            <el-table :data="dryRunResult.sampleItems" row-key="fileId" empty-text="暂无样本文件">
              <el-table-column prop="assetUuid" label="平台资产ID" min-width="230" show-overflow-tooltip />
              <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
              <el-table-column prop="fileKind" label="类型" width="90" />
              <el-table-column prop="extension" label="扩展名" width="90" />
              <el-table-column label="大小" width="110" align="right">
                <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
              </el-table-column>
              <el-table-column prop="checksumStatus" label="checksum" width="150" />
              <el-table-column prop="storageStatus" label="存储状态" width="150" />
              <el-table-column prop="reason" label="计划原因" min-width="180" show-overflow-tooltip />
            </el-table>
          </template>
        </section>

        <section class="migration-summary">
          <div class="migration-metric">
            <span>文件总数</span>
            <strong>{{ formatCount(summary?.totalFileCount) }}</strong>
          </div>
          <div class="migration-metric">
            <span>已对象化</span>
            <strong>{{ formatCount(summary?.objectStoredCount) }}</strong>
          </div>
          <div class="migration-metric">
            <span>仍在 NAS</span>
            <strong>{{ formatCount(summary?.nasOnlyCount) }}</strong>
          </div>
          <div class="migration-metric">
            <span>异常任务</span>
            <strong>{{ formatCount(summary?.failedTaskCount) }}</strong>
          </div>
          <div class="migration-progress">
            <span>对象化覆盖率 {{ objectStoredPercent }}%</span>
            <el-progress :percentage="objectStoredPercent" :stroke-width="8" :show-text="false" />
          </div>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>创建迁移任务</h2>
              <span>只能显式选择文件；单次最多 {{ summary?.maxFilesPerTask ?? 10 }} 个文件，单文件上限 {{ formatBytes(summary?.maxFileSizeBytes) }}。</span>
            </div>
            <el-tag type="warning" effect="plain">受控镜像</el-tag>
          </div>

          <div class="migration-create">
            <el-form label-position="top">
              <el-form-item label="目标对象存储">
                <el-segmented v-model="createForm.targetProvider" :options="targetProviderOptions" />
              </el-form-item>
              <el-form-item label="待迁移内部文件 ID">
                <el-input
                  v-model="createForm.fileIdsText"
                  type="textarea"
                  :rows="3"
                  placeholder="输入内部文件 ID，用逗号、空格或换行分隔。也可以从下方文件选择器加入。"
                />
              </el-form-item>
            </el-form>
            <div class="migration-create__side">
              <strong>已选择 {{ selectedFileIds.length }} 个文件</strong>
              <span>任务创建后会逐个校验项目归属、生命周期、大小限制和对象版本幂等。</span>
              <el-button
                type="primary"
                :icon="Plus"
                :loading="creating"
                :disabled="selectedFileIds.length === 0"
                @click="createTask"
              >
                创建迁移任务
              </el-button>
            </div>
          </div>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>选择项目文件</h2>
              <span>按文件名或平台资产 ID 搜索，仅登记文件可加入迁移任务。</span>
            </div>
            <el-button :icon="Search" :loading="candidateLoading" @click="loadCandidates">查询</el-button>
          </div>

          <div class="candidate-toolbar">
            <el-input
              v-model="candidateFilters.keyword"
              clearable
              placeholder="搜索文件名或平台资产ID"
              @keyup.enter="loadCandidates"
            />
            <el-select v-model="candidateFilters.fileKind" placeholder="文件类型">
              <el-option label="全部" value="" />
              <el-option label="文档" value="DOCUMENT" />
              <el-option label="图纸" value="DRAWING" />
              <el-option label="模型" value="MODEL" />
            </el-select>
            <el-button @click="appendCandidateSelection">加入迁移清单</el-button>
          </div>

          <el-table
            v-loading="candidateLoading"
            :data="candidateRows"
            row-key="fileId"
            empty-text="暂无可选文件"
            @selection-change="handleCandidateSelection"
          >
            <el-table-column type="selection" width="48" :selectable="isCandidateSelectable" />
            <el-table-column label="平台资产ID" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">{{ row.assetUuid || '-' }}</template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
            <el-table-column prop="fileKind" label="类型" width="90" />
            <el-table-column label="大小" width="120" align="right">
              <template #default="{ row }">{{ formatBytes(row.sizeBytes) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.registered === false ? 'warning' : 'success'" size="small">
                  {{ row.registered === false ? '未登记' : '已登记' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>迁移任务</h2>
              <span>任务只返回业务状态和平台资产 ID，不展示底层对象定位。</span>
            </div>
          </div>

          <el-table v-loading="taskLoading" :data="tasks" row-key="taskId" empty-text="暂无迁移任务" @row-click="openTask">
            <el-table-column prop="taskId" label="任务ID" width="90" />
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusType(row.taskStatus)" size="small">{{ taskStatusLabel(row.taskStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="targetProvider" label="目标" width="120" />
            <el-table-column label="数量" width="150">
              <template #default="{ row }">
                {{ row.totalCount }} / 成功 {{ row.successCount }} / 跳过 {{ row.skippedCount }} / 失败 {{ row.failureCount }}
              </template>
            </el-table-column>
            <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column label="更新时间" width="170">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click.stop="openTask(row)">详情</el-button>
                <el-button text :disabled="row.failureCount === 0" @click.stop="retryTask(row)">重试</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="文件访问安全" name="access">
        <section class="service-grid">
          <article v-for="item in enabledServices" :key="item.title" class="service-card">
            <el-tag type="success" effect="plain">已开放</el-tag>
            <h2>{{ item.title }}</h2>
            <p>{{ item.description }}</p>
            <el-button text type="primary" @click="openService(item.target)">进入</el-button>
          </article>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>未开放写操作</h2>
              <span>这些能力会直接改变 NAS 文件，当前只作为受控灰度能力保留。</span>
            </div>
            <el-tag type="warning" effect="plain">需要审批和回滚方案</el-tag>
          </div>
          <div class="disabled-action-grid">
            <article v-for="item in disabledActions" :key="item.title" class="disabled-action">
              <strong>{{ item.title }}</strong>
              <span>{{ item.reason }}</span>
              <el-button disabled size="small">受控开放</el-button>
            </article>
          </div>
        </section>

        <section class="service-section">
          <div class="service-section__header">
            <div>
              <h2>安全口径</h2>
              <span>当前文件服务遵循文件访问安全闭环和对象存储证据链边界。</span>
            </div>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="真实路径">普通项目用户不可见，使用平台逻辑路径和受控访问入口。</el-descriptions-item>
            <el-descriptions-item label="预览与下载">通过短时票据访问，预览权限和下载权限分离。</el-descriptions-item>
            <el-descriptions-item label="对象存储">只展示对象化状态，不展示底层对象定位信息或底层 URI。</el-descriptions-item>
            <el-descriptions-item label="Hermes">只读辅助，不能执行写库、NAS 操作或自动审批。</el-descriptions-item>
          </el-descriptions>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="detailVisible" title="迁移任务详情" size="760px">
      <template v-if="selectedTask">
        <section class="task-detail">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="任务ID">{{ selectedTask.taskId }}</el-descriptions-item>
            <el-descriptions-item label="目标">{{ selectedTask.targetProvider }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ taskStatusLabel(selectedTask.taskStatus) }}</el-descriptions-item>
            <el-descriptions-item label="存储状态">{{ storageStateLabel(selectedTask.storageState) }}</el-descriptions-item>
            <el-descriptions-item label="数量">
              {{ selectedTask.totalCount }} / 成功 {{ selectedTask.successCount }} / 跳过 {{ selectedTask.skippedCount }} / 失败 {{ selectedTask.failureCount }}
            </el-descriptions-item>
            <el-descriptions-item label="说明">{{ selectedTask.message }}</el-descriptions-item>
          </el-descriptions>

          <el-table :data="selectedTask.rows" row-key="rowId" class="task-detail__rows">
            <el-table-column label="平台资产ID" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">{{ row.assetUuid || '-' }}</template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
            <el-table-column prop="fileKind" label="类型" width="90" />
            <el-table-column label="迁移状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusType(row.migrationStatus)" size="small">{{ migrationStatusLabel(row.migrationStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="存储状态" width="120">
              <template #default="{ row }">{{ storageStateLabel(row.storageState) }}</template>
            </el-table-column>
            <el-table-column prop="resultCode" label="结果码" width="150" show-overflow-tooltip />
            <el-table-column prop="message" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column label="完成时间" width="170">
              <template #default="{ row }">{{ formatDate(row.completedAt) }}</template>
            </el-table-column>
          </el-table>
        </section>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import type { RouteRecordName } from 'vue-router';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh, Search } from '@element-plus/icons-vue';

import {
  createStorageMigrationTask,
  continueStorageObjectificationRun,
  dryRunStorageObjectificationRun,
  dryRunStorageObjectificationWave,
  dryRunMultiProjectStorageObjectificationPlan,
  dryRunStorageObjectificationPlan,
  executeStorageObjectificationWave,
  executeMultiProjectStorageObjectificationPlan,
  fetchCatalogFiles,
  fetchStorageObjectificationRunOverview,
  fetchStorageObjectificationRunProjects,
  fetchStorageObjectificationWaveCandidates,
  fetchStorageObjectificationWaveReports,
  fetchStorageObjectificationLongRun,
  fetchStorageObjectificationFullPlan,
  fetchStorageObjectificationInventory,
  fetchStorageMigrationSummary,
  fetchStorageMigrationTask,
  fetchStorageMigrationTasks,
  fetchStorageProviderReadiness,
  fetchStorageReadPolicy,
  retryStorageMigrationTask,
  pauseStorageObjectificationRun,
  pauseStorageObjectificationLongRun,
  retryFailedStorageObjectificationRun,
  resumeStorageObjectificationLongRun,
  retryStorageObjectificationLongRunFailures,
  startStorageObjectificationLongRun,
  startStorageObjectificationRun,
  type CatalogFile,
  type MultiProjectStorageObjectificationDryRun,
  type MultiProjectStorageObjectificationExecuteResult,
  type ProjectStorageObjectificationInventory,
  type StorageObjectificationDryRun,
  type StorageObjectificationFullPlan,
  type StorageObjectificationInventory,
  type StorageObjectificationLongRun,
  type StorageObjectificationRunOverview,
  type StorageObjectificationRunProjects,
  type StorageObjectificationWaveCandidates,
  type StorageObjectificationWaveReports,
  type StorageMigrationSummary,
  type StorageMigrationTaskDetail,
  type StorageMigrationTaskListItem,
  type StorageProviderReadiness,
  type StorageReadPolicy
} from '@/modules/data-steward/api/dataSteward';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const activeTab = ref(route.name === 'project-data-steward-file-service' ? 'access' : 'migration');
const loading = ref(false);
const taskLoading = ref(false);
const candidateLoading = ref(false);
const creating = ref(false);
const dryRunLoading = ref(false);
const fullPlanLoading = ref(false);
const fullPlanExecutionLoading = ref(false);
const longRunLoading = ref(false);
const longRunActionLoading = ref(false);
const multiPlanLoading = ref(false);
const multiExecutionLoading = ref(false);
const waveCandidatesLoading = ref(false);
const waveDryRunLoading = ref(false);
const waveExecutionLoading = ref(false);
const waveReportLoading = ref(false);
const runOverviewLoading = ref(false);
const runDryRunLoading = ref(false);
const runExecutionLoading = ref(false);
const runPauseLoading = ref(false);
const runRetryLoading = ref(false);
const detailVisible = ref(false);
const summary = ref<StorageMigrationSummary | null>(null);
const readiness = ref<StorageProviderReadiness | null>(null);
const readPolicy = ref<StorageReadPolicy | null>(null);
const inventory = ref<StorageObjectificationInventory | null>(null);
const dryRunResult = ref<StorageObjectificationDryRun | null>(null);
const fullPlan = ref<StorageObjectificationFullPlan | null>(null);
const fullPlanExecutionResult = ref<MultiProjectStorageObjectificationExecuteResult | null>(null);
const longRunStatus = ref<StorageObjectificationLongRun | null>(null);
const multiPlanResult = ref<MultiProjectStorageObjectificationDryRun | null>(null);
const multiExecutionResult = ref<MultiProjectStorageObjectificationExecuteResult | null>(null);
const waveCandidates = ref<StorageObjectificationWaveCandidates | null>(null);
const wavePlanResult = ref<MultiProjectStorageObjectificationDryRun | null>(null);
const waveExecutionResult = ref<MultiProjectStorageObjectificationExecuteResult | null>(null);
const waveReports = ref<StorageObjectificationWaveReports | null>(null);
const runOverview = ref<StorageObjectificationRunOverview | null>(null);
const runProjects = ref<StorageObjectificationRunProjects | null>(null);
const runPlanResult = ref<MultiProjectStorageObjectificationDryRun | null>(null);
const runExecutionResult = ref<MultiProjectStorageObjectificationExecuteResult | null>(null);
const tasks = ref<StorageMigrationTaskListItem[]>([]);
const selectedTask = ref<StorageMigrationTaskDetail | null>(null);
const candidateRows = ref<CatalogFile[]>([]);
const candidateSelection = ref<CatalogFile[]>([]);

const createForm = reactive({
  targetProvider: 'MINIO',
  fileIdsText: ''
});

const candidateFilters = reactive({
  keyword: '',
  fileKind: ''
});

const dryRunForm = reactive({
  directoryPath: '',
  storageState: 'NAS_ONLY',
  checksumState: 'ANY',
  extensionsText: '',
  limit: 200
});

const multiPlanForm = reactive({
  projectIdsText: '',
  realProjectsOnly: true,
  storageState: 'NAS_ONLY',
  checksumState: 'ANY',
  extensionsText: 'pdf,dwg,docx,xlsx,pptx',
  limit: 15,
  maxFilesPerProject: 5,
  maxTotalMb: 100,
  maxBytesPerProjectMb: 50,
  concurrencyLimit: 1
});

const longRunForm = reactive({
  batchFileLimit: 5,
  batchBytesMb: 50,
  maxFileSizeMb: 10,
  maxContinuousBatches: 2,
  continueOnFailure: true
});

const runForm = reactive({
  maxProjects: 5,
  maxTotalFiles: 100,
  maxFilesPerProject: 20,
  maxTotalMb: 1024,
  maxBytesPerProjectMb: 512,
  maxFileSizeMb: 200,
  maxContinuousBatches: 1,
  continueOnFailure: true
});

const controlledExpansionMaxFilesTotal = 15;
const controlledExpansionMaxFilesPerProject = 15;
const controlledExpansionMaxTotalBytes = 100 * 1024 * 1024;
const controlledExpansionMaxBytesPerProject = 50 * 1024 * 1024;
const fullPlanBatchFileLimit = 15;
const fullPlanBatchBytesLimit = 50 * 1024 * 1024;

const targetProviderOptions = [
  { label: 'MinIO', value: 'MINIO' },
  { label: 'S3-compatible', value: 'S3_COMPATIBLE' }
];

const projectId = computed(() => {
  const routeId = Number(route.params.projectId);
  return Number.isFinite(routeId) && routeId > 0 ? routeId : authStore.currentProjectId;
});

const projectLabel = computed(() => {
  const current = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return current ? `${current.code} ${current.name}` : '等待项目上下文';
});

const selectedFileIds = computed(() => {
  const typedIds = parseFileIds(createForm.fileIdsText);
  const selectedIds = candidateSelection.value
    .map((row) => Number(row.fileId))
    .filter((fileId) => Number.isFinite(fileId) && fileId > 0);
  return Array.from(new Set([...typedIds, ...selectedIds]));
});

const objectStoredPercent = computed(() => {
  const total = Number(summary.value?.totalFileCount ?? 0);
  if (!total) return 0;
  return Math.round((Number(summary.value?.objectStoredCount ?? 0) / total) * 100);
});

const readinessReady = computed(() => (
  readiness.value?.endpointType === 'NAS_SIDE_MINIO'
  && readiness.value?.readinessStatus === 'READY'
  && readiness.value?.writable === true
));

const dryRunExecutableItems = computed(() => {
  const maxFiles = Number(summary.value?.maxFilesPerTask ?? 10);
  const maxSize = Number(summary.value?.maxFileSizeBytes ?? 10 * 1024 * 1024);
  return (dryRunResult.value?.sampleItems ?? [])
    .filter((item) => item.storageStatus === 'NAS_ONLY')
    .filter((item) => ['ELIGIBLE_DRY_RUN', 'MISSING_CHECKSUM'].includes(item.reason))
    .filter((item) => Number(item.sizeBytes ?? 0) <= maxSize)
    .slice(0, maxFiles);
});

const dryRunExecutableFileIds = computed(() => dryRunExecutableItems.value.map((item) => item.fileId));

const canRunDryRunGrayTask = computed(() => readinessReady.value && dryRunExecutableFileIds.value.length > 0);

const dryRunGrayHint = computed(() => {
  if (!readinessReady.value) {
    return '需要 NAS 侧 MinIO READY 后才能执行；dry-run 本身仍是只读计划。';
  }
  if (!dryRunResult.value) {
    return '先生成 dry-run 计划，再从其中选择安全小样本。';
  }
  if (dryRunExecutableFileIds.value.length === 0) {
    return '当前 dry-run 样本没有符合小批灰度条件的 NAS_ONLY 文件。';
  }
  return `将使用 dry-run 样本中的 ${dryRunExecutableFileIds.value.length} 个文件；NAS 原文件保留，只复制副本到对象存储。`;
});

const inventoryRows = computed<ProjectStorageObjectificationInventory[]>(() => {
  const rows = inventory.value?.projects ?? [];
  if (projectId.value) {
    const current = rows.find((row) => Number(row.projectId) === Number(projectId.value));
    return current ? [current, ...rows.filter((row) => Number(row.projectId) !== Number(projectId.value)).slice(0, 5)] : rows.slice(0, 6);
  }
  return rows.slice(0, 8);
});

const fullPlanNextBatchFileIds = computed(() => (fullPlan.value?.nextBatchItems ?? [])
  .map((item) => Number(item.fileId))
  .filter((fileId) => Number.isFinite(fileId) && fileId > 0));

const canExecuteFullPlanNextBatch = computed(() => (
  readinessReady.value
  && Boolean(projectId.value)
  && fullPlanNextBatchFileIds.value.length > 0
));

const longRunBatchBytes = computed(() => Math.max(1, Number(longRunForm.batchBytesMb || 50)) * 1024 * 1024);
const longRunMaxFileSizeBytes = computed(() => Math.max(1, Number(longRunForm.maxFileSizeMb || 10)) * 1024 * 1024);
const canStartLongRun = computed(() => (
  readinessReady.value
  && Boolean(projectId.value)
  && projectId.value === 503
  && longRunStatus.value?.runState !== 'PAUSED'
));

const canResumeLongRun = computed(() => (
  readinessReady.value
  && Boolean(projectId.value)
  && projectId.value === 503
  && longRunStatus.value?.runState === 'PAUSED'
));

const canPauseLongRun = computed(() => Boolean(projectId.value) && projectId.value === 503 && longRunStatus.value?.runState !== 'PAUSED');

const multiPlanMaxTotalBytes = computed(() => Math.max(1, Number(multiPlanForm.maxTotalMb || 100)) * 1024 * 1024);
const multiPlanMaxBytesPerProject = computed(() => Math.max(1, Number(multiPlanForm.maxBytesPerProjectMb || 50)) * 1024 * 1024);

const multiPlanExecutableItems = computed(() => {
  const maxFilesPerProject = Math.min(Number(multiPlanForm.maxFilesPerProject || controlledExpansionMaxFilesPerProject), controlledExpansionMaxFilesPerProject);
  const maxTotalBytes = Math.min(multiPlanMaxTotalBytes.value, controlledExpansionMaxTotalBytes);
  const maxProjectBytes = Math.min(multiPlanMaxBytesPerProject.value, controlledExpansionMaxBytesPerProject);
  const maxFileBytes = Number(summary.value?.maxFileSizeBytes ?? 10 * 1024 * 1024);
  const selected: Array<{ projectId: number; fileId: number; sizeBytes: number }> = [];
  let selectedTotalBytes = 0;
  for (const project of multiPlanResult.value?.projects ?? []) {
    if (!project.realNasProject) continue;
    let projectFileCount = 0;
    let projectTotalBytes = 0;
    for (const item of project.sampleItems ?? []) {
      const fileId = Number(item.fileId);
      const sizeBytes = Number(item.sizeBytes ?? 0);
      if (!Number.isFinite(fileId) || fileId <= 0) continue;
      if (item.storageStatus !== 'NAS_ONLY') continue;
      if (!['ELIGIBLE_DRY_RUN', 'MISSING_CHECKSUM'].includes(item.reason)) continue;
      if (sizeBytes > maxFileBytes) continue;
      if (projectFileCount + 1 > maxFilesPerProject) continue;
      if (projectTotalBytes + sizeBytes > maxProjectBytes) continue;
      if (selected.length + 1 > controlledExpansionMaxFilesTotal) continue;
      if (selectedTotalBytes + sizeBytes > maxTotalBytes) continue;
      selected.push({ projectId: project.projectId, fileId, sizeBytes });
      projectFileCount += 1;
      projectTotalBytes += sizeBytes;
      selectedTotalBytes += sizeBytes;
    }
  }
  return selected;
});

const multiPlanExecutableFileIds = computed(() => multiPlanExecutableItems.value.map((item) => item.fileId));
const multiPlanExecutableProjectIds = computed(() => Array.from(new Set(multiPlanExecutableItems.value.map((item) => item.projectId))));
const canExecuteMultiPlan = computed(() => readinessReady.value && multiPlanExecutableFileIds.value.length > 0);

const multiExecutionHint = computed(() => {
  if (!readinessReady.value) {
    return '需要 NAS 侧 MinIO READY 后才能执行；规划本身仍可只读生成。';
  }
  if (!multiPlanResult.value) {
    return '先生成多项目 dry-run 计划，再确认执行受控小批。';
  }
  if (multiPlanExecutableFileIds.value.length === 0) {
    return '当前计划没有符合多项目小批条件的 NAS_ONLY 文件。';
  }
  return `将执行 ${multiPlanExecutableProjectIds.value.length} 个真实项目、${multiPlanExecutableFileIds.value.length} 个文件；只复制副本，不改动 NAS 原文件。`;
});

const waveDefaultProjectIds = computed(() => (waveCandidates.value?.candidates ?? [])
  .map((row) => Number(row.projectId))
  .filter((projectId) => Number.isFinite(projectId) && projectId > 0)
  .slice(0, Number(waveCandidates.value?.maxProjectCount ?? 3)));

const waveExecutableItems = computed(() => {
  const maxFilesPerProject = Math.min(
    Number(waveCandidates.value?.maxFilesPerProject ?? controlledExpansionMaxFilesPerProject),
    controlledExpansionMaxFilesPerProject
  );
  const maxTotalBytes = Math.min(Number(waveCandidates.value?.maxTotalBytes ?? controlledExpansionMaxTotalBytes), controlledExpansionMaxTotalBytes);
  const maxProjectBytes = Math.min(
    Number(waveCandidates.value?.maxBytesPerProject ?? controlledExpansionMaxBytesPerProject),
    controlledExpansionMaxBytesPerProject
  );
  const maxFileBytes = Number(waveCandidates.value?.maxFileSizeBytes ?? summary.value?.maxFileSizeBytes ?? 10 * 1024 * 1024);
  const selected: Array<{ projectId: number; fileId: number; sizeBytes: number }> = [];
  let selectedTotalBytes = 0;
  for (const project of wavePlanResult.value?.projects ?? []) {
    let projectFileCount = 0;
    let projectTotalBytes = 0;
    for (const item of project.sampleItems ?? []) {
      const fileId = Number(item.fileId);
      const sizeBytes = Number(item.sizeBytes ?? 0);
      if (!Number.isFinite(fileId) || fileId <= 0) continue;
      if (item.storageStatus !== 'NAS_ONLY') continue;
      if (!['ELIGIBLE_DRY_RUN', 'MISSING_CHECKSUM'].includes(item.reason)) continue;
      if (sizeBytes > maxFileBytes) continue;
      if (projectFileCount + 1 > maxFilesPerProject) continue;
      if (projectTotalBytes + sizeBytes > maxProjectBytes) continue;
      if (selected.length + 1 > controlledExpansionMaxFilesTotal) continue;
      if (selectedTotalBytes + sizeBytes > maxTotalBytes) continue;
      selected.push({ projectId: project.projectId, fileId, sizeBytes });
      projectFileCount += 1;
      projectTotalBytes += sizeBytes;
      selectedTotalBytes += sizeBytes;
    }
  }
  return selected;
});

const waveExecutableFileIds = computed(() => waveExecutableItems.value.map((item) => item.fileId));
const waveExecutableProjectIds = computed(() => Array.from(new Set(waveExecutableItems.value.map((item) => item.projectId))));
const canExecuteWave = computed(() => readinessReady.value && waveExecutableFileIds.value.length > 0);

const waveExecutionHint = computed(() => {
  if (!readinessReady.value) {
    return '需要 NAS 侧 MinIO READY 后才能执行；Wave 1 候选和 dry-run 本身仍是只读检查。';
  }
  if (!wavePlanResult.value) {
    return '先生成 Wave 1 dry-run，再确认执行低风险小批。';
  }
  if (waveExecutableFileIds.value.length === 0) {
    return '当前 Wave 1 dry-run 没有符合执行条件的 NAS_ONLY 文件。';
  }
  return `将执行 ${waveExecutableProjectIds.value.length} 个非 105 真实项目、${waveExecutableFileIds.value.length} 个文件；只复制副本，不改动 NAS 原文件。`;
});

const runMaxTotalBytes = computed(() => Math.max(1, Number(runForm.maxTotalMb || 1024)) * 1024 * 1024);
const runMaxBytesPerProject = computed(() => Math.max(1, Number(runForm.maxBytesPerProjectMb || 512)) * 1024 * 1024);
const runMaxFileSizeBytes = computed(() => Math.max(1, Number(runForm.maxFileSizeMb || 200)) * 1024 * 1024);

const runExecutableRows = computed(() => (runProjects.value?.projects ?? runOverview.value?.projects ?? [])
  .filter((row) => row.queueStatus === 'EXECUTABLE')
  .slice(0, 8));

const runGovernanceRows = computed(() => (runProjects.value?.projects ?? runOverview.value?.projects ?? [])
  .filter((row) => row.queueStatus === 'GOVERNANCE_REQUIRED'));

const runCompletedRows = computed(() => (runProjects.value?.projects ?? runOverview.value?.projects ?? [])
  .filter((row) => row.queueStatus === 'COMPLETED'));

const canExecuteRun = computed(() => readinessReady.value && Boolean(runPlanResult.value?.selectedFileCount));

const runExecutionHint = computed(() => {
  if (!readinessReady.value) {
    return '需要 NAS 侧 MinIO READY 后才能执行；队列和 dry-run 仍可作为只读盘点。';
  }
  if (runOverview.value?.paused) {
    return '当前跑批已暂停，可点击继续恢复受控跑批。';
  }
  if (!runPlanResult.value) {
    return '先生成全项目 dry-run，再人工确认开始；所有执行都会套用项目、文件数、容量和连续批次硬上限。';
  }
  if (!runPlanResult.value.selectedFileCount) {
    return '当前 dry-run 没有可执行文件，需查看治理项目或失败项。';
  }
  return `本轮计划 ${runPlanResult.value.plannedProjectCount} 个项目、${runPlanResult.value.selectedFileCount} 个文件，只复制对象存储副本，不改动 NAS 原文件。`;
});

const enabledServices: Array<{ title: string; description: string; target: RouteRecordName }> = [
  { title: '文件预览', description: '查看预览状态，并通过短时票据打开可预览文件。', target: 'data-steward-asset-detail' },
  { title: '下载权限', description: '下载和预览分开判断，普通查看者不能下载。', target: 'data-steward-asset-detail' },
  { title: '权限证明', description: '验证当前用户对指定文件的访问权限和原因。', target: 'data-steward-agent-preview' },
  { title: 'Hermes 只读辅助', description: '围绕资产目录回答问题，不读取正文也不执行写操作。', target: 'data-steward-asset-detail' }
];

const disabledActions = [
  { title: '目录全量迁移', reason: '当前只允许显式选择文件，不开放目录或项目一键迁移。' },
  { title: '生成语义证据', reason: 'documents / chunks 和索引能力在 M4 后置。' },
  { title: '移动文件', reason: '会影响路径追溯和交付绑定，必须具备回滚方案。' },
  { title: '重命名文件', reason: '会影响文件查找、版本链和审计证据。' },
  { title: '真实删除', reason: '必须走申请、审批、隔离、恢复和到期永久删除。' },
  { title: '批量打包下载', reason: '涉及大容量、权限聚合和审计，后续单独开放。' }
];

watch(
  () => route.name,
  () => {
    activeTab.value = route.name === 'project-data-steward-file-service' ? 'access' : 'migration';
  }
);

watch(
  () => projectId.value,
  () => {
    void refresh();
  }
);

onMounted(() => {
  void refresh();
});

async function refresh() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    await Promise.all([
      loadReadiness(),
      loadReadPolicy(),
      loadInventory(),
      loadSummary(),
      loadFullPlan(),
      loadLongRunStatus(),
      loadRunOverview(),
      loadWaveCandidates(),
      loadWaveReports(),
      loadTasks(),
      loadCandidates()
    ]);
  } finally {
    loading.value = false;
  }
}

async function loadReadiness() {
  readiness.value = await fetchStorageProviderReadiness();
}

async function loadReadPolicy() {
  readPolicy.value = await fetchStorageReadPolicy();
}

async function loadInventory() {
  inventory.value = await fetchStorageObjectificationInventory();
}

async function loadSummary() {
  if (!projectId.value) return;
  summary.value = await fetchStorageMigrationSummary(projectId.value);
}

async function loadFullPlan() {
  if (!projectId.value) return;
  fullPlanLoading.value = true;
  try {
    fullPlan.value = await fetchStorageObjectificationFullPlan(projectId.value, {
      checksumState: 'ANY',
      batchFileLimit: fullPlanBatchFileLimit,
      batchBytesLimit: fullPlanBatchBytesLimit
    });
  } finally {
    fullPlanLoading.value = false;
  }
}

async function loadLongRunStatus() {
  if (!projectId.value || projectId.value !== 503) {
    longRunStatus.value = null;
    return;
  }
  longRunLoading.value = true;
  try {
    longRunStatus.value = await fetchStorageObjectificationLongRun(projectId.value);
  } finally {
    longRunLoading.value = false;
  }
}

async function loadWaveCandidates() {
  waveCandidatesLoading.value = true;
  try {
    waveCandidates.value = await fetchStorageObjectificationWaveCandidates();
  } finally {
    waveCandidatesLoading.value = false;
  }
}

async function loadWaveReports() {
  waveReportLoading.value = true;
  try {
    waveReports.value = await fetchStorageObjectificationWaveReports();
  } finally {
    waveReportLoading.value = false;
  }
}

async function refreshWave1() {
  await Promise.all([loadWaveCandidates(), loadWaveReports()]);
}

async function loadRunOverview() {
  runOverviewLoading.value = true;
  try {
    const [overview, projects] = await Promise.all([
      fetchStorageObjectificationRunOverview(),
      fetchStorageObjectificationRunProjects()
    ]);
    runOverview.value = overview;
    runProjects.value = projects;
  } finally {
    runOverviewLoading.value = false;
  }
}

async function loadTasks() {
  if (!projectId.value) return;
  taskLoading.value = true;
  try {
    tasks.value = await fetchStorageMigrationTasks(projectId.value);
  } finally {
    taskLoading.value = false;
  }
}

async function loadCandidates() {
  if (!projectId.value) return;
  candidateLoading.value = true;
  try {
    const result = await fetchCatalogFiles({
      projectId: projectId.value,
      keyword: candidateFilters.keyword || undefined,
      fileKind: candidateFilters.fileKind || undefined,
      page: 1,
      pageSize: 20
    });
    candidateRows.value = result.rows;
  } finally {
    candidateLoading.value = false;
  }
}

function handleCandidateSelection(rows: CatalogFile[]) {
  candidateSelection.value = rows;
}

function appendCandidateSelection() {
  const ids = selectedFileIds.value;
  createForm.fileIdsText = ids.join(', ');
  ElMessage.success(`已加入 ${ids.length} 个文件`);
}

function appendDryRunSelection() {
  if (dryRunExecutableFileIds.value.length === 0) return;
  createForm.fileIdsText = dryRunExecutableFileIds.value.join(', ');
  ElMessage.success(`已加入 ${dryRunExecutableFileIds.value.length} 个 dry-run 小样本`);
}

function isCandidateSelectable(row: CatalogFile) {
  return row.registered !== false && Number.isFinite(Number(row.fileId));
}

async function createTask() {
  if (!projectId.value || selectedFileIds.value.length === 0) return;
  const confirmed = await confirmAction('将创建对象存储镜像任务。NAS 原文件会保留，平台不会读取文件正文，也不会生成语义证据。');
  if (!confirmed) return;
  await submitMigrationTask(selectedFileIds.value);
}

async function createTaskFromDryRun() {
  if (!projectId.value || dryRunExecutableFileIds.value.length === 0) return;
  if (!readinessReady.value) {
    ElMessage.warning('NAS 侧 MinIO 尚未 READY，不能执行真实对象化灰度。');
    return;
  }
  const confirmed = await confirmAction('将按 dry-run 小样本执行 105 对象化灰度。NAS 原文件保留，只复制副本到对象存储；不会读取正文，也不会写语义索引。');
  if (!confirmed) return;
  await submitMigrationTask(dryRunExecutableFileIds.value);
}

async function submitMigrationTask(fileIds: number[]) {
  if (!projectId.value || fileIds.length === 0) return;
  creating.value = true;
  try {
    const detail = await createStorageMigrationTask(projectId.value, {
      fileIds,
      targetProvider: createForm.targetProvider
    });
    selectedTask.value = detail;
    detailVisible.value = true;
    createForm.fileIdsText = '';
    candidateSelection.value = [];
    ElMessage.success('迁移任务已创建');
    await Promise.all([loadInventory(), loadSummary(), loadTasks()]);
  } finally {
    creating.value = false;
  }
}

async function runDryRun() {
  if (!projectId.value) return;
  dryRunLoading.value = true;
  try {
    dryRunResult.value = await dryRunStorageObjectificationPlan(projectId.value, {
      directoryPath: dryRunForm.directoryPath || undefined,
      storageState: dryRunForm.storageState as 'ANY' | 'NAS_ONLY' | 'MIGRATION_FAILED',
      checksumState: dryRunForm.checksumState as 'ANY' | 'HAS_CHECKSUM' | 'MISSING_CHECKSUM',
      extensions: parseExtensions(dryRunForm.extensionsText),
      limit: dryRunForm.limit
    });
    ElMessage.success('dry-run 计划已生成，未启动真实迁移');
  } finally {
    dryRunLoading.value = false;
  }
}

async function runMultiProjectDryRun() {
  multiPlanLoading.value = true;
  try {
    multiExecutionResult.value = null;
    multiPlanResult.value = await dryRunMultiProjectStorageObjectificationPlan({
      projectIds: parseFileIds(multiPlanForm.projectIdsText),
      realProjectsOnly: multiPlanForm.realProjectsOnly,
      storageState: multiPlanForm.storageState as 'ANY' | 'NAS_ONLY' | 'MIGRATION_FAILED',
      checksumState: multiPlanForm.checksumState as 'ANY' | 'HAS_CHECKSUM' | 'MISSING_CHECKSUM',
      extensions: parseExtensions(multiPlanForm.extensionsText),
      limit: multiPlanForm.limit,
      maxTotalBytes: multiPlanMaxTotalBytes.value,
      maxFilesPerProject: multiPlanForm.maxFilesPerProject,
      maxBytesPerProject: multiPlanMaxBytesPerProject.value,
      concurrencyLimit: multiPlanForm.concurrencyLimit
    });
    ElMessage.success('多项目 dry-run 已生成，未创建迁移任务');
  } finally {
    multiPlanLoading.value = false;
  }
}

async function runWaveDryRun() {
  if (waveDefaultProjectIds.value.length === 0) {
    ElMessage.warning('当前没有符合 Wave 1 条件的候选项目。');
    return;
  }
  waveDryRunLoading.value = true;
  try {
    waveExecutionResult.value = null;
    wavePlanResult.value = await dryRunStorageObjectificationWave({
      projectIds: waveDefaultProjectIds.value,
      maxProjects: waveCandidates.value?.maxProjectCount,
      limit: waveCandidates.value?.maxTotalFiles,
      maxTotalBytes: waveCandidates.value?.maxTotalBytes,
      maxFilesPerProject: waveCandidates.value?.maxFilesPerProject,
      maxBytesPerProject: waveCandidates.value?.maxBytesPerProject
    });
    ElMessage.success('Wave 1 dry-run 已生成，未创建迁移任务');
  } finally {
    waveDryRunLoading.value = false;
  }
}

async function executeWavePlan() {
  if (!canExecuteWave.value) return;
  const confirmed = await confirmAction('将执行 M3G-7 Wave 1 多真实项目小批对象化：只复制文件副本到 NAS 侧 MinIO，不移动、不删除、不改名 NAS 原文件；不会读取正文或写语义索引。');
  if (!confirmed) return;
  waveExecutionLoading.value = true;
  try {
    waveExecutionResult.value = await executeStorageObjectificationWave({
      projectIds: waveExecutableProjectIds.value,
      fileIds: waveExecutableFileIds.value,
      confirmed: true,
      targetProvider: 'MINIO',
      limit: waveExecutableFileIds.value.length,
      maxTotalBytes: waveCandidates.value?.maxTotalBytes,
      maxFilesPerProject: waveCandidates.value?.maxFilesPerProject,
      maxBytesPerProject: waveCandidates.value?.maxBytesPerProject
    });
    ElMessage.success('Wave 1 小批对象化执行已返回');
    await Promise.all([loadInventory(), loadSummary(), loadWaveCandidates(), loadWaveReports(), loadTasks()]);
  } finally {
    waveExecutionLoading.value = false;
  }
}

function runPayload(confirmed = false) {
  return {
    maxProjects: Math.min(Math.max(1, Number(runForm.maxProjects || 5)), 5),
    maxTotalFiles: Math.min(Math.max(1, Number(runForm.maxTotalFiles || 100)), 200),
    maxFilesPerProject: Math.min(Math.max(1, Number(runForm.maxFilesPerProject || 20)), 50),
    maxTotalBytes: Math.min(runMaxTotalBytes.value, 2 * 1024 * 1024 * 1024),
    maxBytesPerProject: Math.min(runMaxBytesPerProject.value, 2 * 1024 * 1024 * 1024),
    maxFileSizeBytes: Math.min(runMaxFileSizeBytes.value, 500 * 1024 * 1024),
    maxContinuousBatches: Math.min(Math.max(1, Number(runForm.maxContinuousBatches || 1)), 3),
    continueOnFailure: runForm.continueOnFailure,
    confirmed,
    targetProvider: 'MINIO'
  };
}

async function runAllProjectDryRun() {
  runDryRunLoading.value = true;
  try {
    runExecutionResult.value = null;
    runPlanResult.value = await dryRunStorageObjectificationRun(runPayload(false));
    ElMessage.success('全项目对象化 dry-run 已生成，未创建迁移任务');
  } finally {
    runDryRunLoading.value = false;
  }
}

async function startAllProjectRun() {
  if (!canExecuteRun.value) return;
  const confirmed = await confirmAction('将按 M3G-7R 全项目对象化队列执行受控跑批：只复制副本到 NAS 侧 MinIO，不移动、不删除、不改名 NAS 原文件；不会读取正文或写语义索引。');
  if (!confirmed) return;
  runExecutionLoading.value = true;
  try {
    runExecutionResult.value = await startStorageObjectificationRun(runPayload(true));
    ElMessage.success('全项目对象化跑批已返回');
    await Promise.all([loadInventory(), loadRunOverview(), loadWaveReports()]);
  } finally {
    runExecutionLoading.value = false;
  }
}

async function continueAllProjectRun() {
  const confirmed = await confirmAction('将继续 M3G-7R 受控跑批；仍按硬上限推进，不改动 NAS 原文件。');
  if (!confirmed) return;
  runExecutionLoading.value = true;
  try {
    runExecutionResult.value = await continueStorageObjectificationRun(runPayload(true));
    ElMessage.success('全项目对象化跑批已继续');
    await Promise.all([loadInventory(), loadRunOverview(), loadWaveReports()]);
  } finally {
    runExecutionLoading.value = false;
  }
}

async function pauseAllProjectRun() {
  runPauseLoading.value = true;
  try {
    runOverview.value = await pauseStorageObjectificationRun();
    await loadRunOverview();
    ElMessage.success('全项目对象化跑批已暂停');
  } finally {
    runPauseLoading.value = false;
  }
}

async function retryFailedAllProjectRun() {
  const confirmed = await confirmAction('将只重试迁移失败项；已对象化文件会按幂等策略跳过，治理项仍需人工处理。');
  if (!confirmed) return;
  runRetryLoading.value = true;
  try {
    runExecutionResult.value = await retryFailedStorageObjectificationRun(runPayload(true));
    ElMessage.success('失败项重试已返回');
    await Promise.all([loadInventory(), loadRunOverview(), loadWaveReports()]);
  } finally {
    runRetryLoading.value = false;
  }
}

async function executeFullPlanNextBatch() {
  if (!projectId.value || !canExecuteFullPlanNextBatch.value || !fullPlan.value) return;
  const fileIds = fullPlanNextBatchFileIds.value;
  const confirmed = await confirmAction('将执行 105 全量对象化计划的下一批：只复制文件副本到 NAS 侧 MinIO，不移动、不删除、不改名 NAS 原文件；不会读取正文或写语义索引。');
  if (!confirmed) return;
  fullPlanExecutionLoading.value = true;
  try {
    fullPlanExecutionResult.value = await executeMultiProjectStorageObjectificationPlan({
      projectIds: [projectId.value],
      fileIds,
      realProjectsOnly: true,
      storageState: 'NAS_ONLY',
      checksumState: 'ANY',
      limit: fileIds.length,
      maxTotalBytes: fullPlan.value.batchBytesLimit,
      maxFilesPerProject: fileIds.length,
      maxBytesPerProject: fullPlan.value.batchBytesLimit,
      concurrencyLimit: 1,
      confirmed: true,
      targetProvider: 'MINIO'
    });
    ElMessage.success('105 下一批对象化执行已返回');
    await Promise.all([loadInventory(), loadSummary(), loadFullPlan(), loadLongRunStatus(), loadTasks()]);
  } finally {
    fullPlanExecutionLoading.value = false;
  }
}

function longRunPayload(confirmed = true) {
  return {
    batchFileLimit: Math.min(Math.max(1, Number(longRunForm.batchFileLimit || 5)), 15),
    batchBytesLimit: Math.min(longRunBatchBytes.value, 512 * 1024 * 1024),
    maxFileSizeBytes: Math.min(longRunMaxFileSizeBytes.value, 500 * 1024 * 1024),
    maxContinuousBatches: Math.min(Math.max(1, Number(longRunForm.maxContinuousBatches || 1)), 5),
    continueOnFailure: longRunForm.continueOnFailure,
    confirmed,
    targetProvider: 'MINIO'
  };
}

async function startLongRun() {
  if (!projectId.value || !canStartLongRun.value) return;
  const confirmed = await confirmAction('将按 105 长跑控制执行多个受控小批：只复制副本到 NAS 侧 MinIO，不移动、不删除、不改名 NAS 原文件；不会读取正文或写语义索引。');
  if (!confirmed) return;
  longRunActionLoading.value = true;
  try {
    longRunStatus.value = await startStorageObjectificationLongRun(projectId.value, longRunPayload(true));
    ElMessage.success('105 长跑已按受控批次推进');
    await Promise.all([loadInventory(), loadSummary(), loadFullPlan(), loadTasks()]);
  } finally {
    longRunActionLoading.value = false;
  }
}

async function pauseLongRun() {
  if (!projectId.value || !canPauseLongRun.value) return;
  longRunActionLoading.value = true;
  try {
    longRunStatus.value = await pauseStorageObjectificationLongRun(projectId.value);
    ElMessage.success('105 长跑已暂停');
  } finally {
    longRunActionLoading.value = false;
  }
}

async function resumeLongRun() {
  if (!projectId.value || !canResumeLongRun.value) return;
  const confirmed = await confirmAction('将从 105 剩余可执行项继续推进受控批次；NAS 原文件仍保持不变。');
  if (!confirmed) return;
  longRunActionLoading.value = true;
  try {
    longRunStatus.value = await resumeStorageObjectificationLongRun(projectId.value, longRunPayload(true));
    ElMessage.success('105 长跑已继续推进');
    await Promise.all([loadInventory(), loadSummary(), loadFullPlan(), loadTasks()]);
  } finally {
    longRunActionLoading.value = false;
  }
}

async function retryLongRunFailures() {
  if (!projectId.value || projectId.value !== 503) return;
  const confirmed = await confirmAction('将只按受控小批重试失败项；已对象化文件会幂等跳过，治理项仍需人工处理。');
  if (!confirmed) return;
  longRunActionLoading.value = true;
  try {
    longRunStatus.value = await retryStorageObjectificationLongRunFailures(projectId.value, longRunPayload(true));
    ElMessage.success('失败项重试已返回');
    await Promise.all([loadInventory(), loadSummary(), loadFullPlan(), loadTasks()]);
  } finally {
    longRunActionLoading.value = false;
  }
}

async function executeMultiProjectPlan() {
  if (!canExecuteMultiPlan.value) return;
  const confirmed = await confirmAction('将对 dry-run 选中的真实项目执行多项目小批对象化：只复制文件副本到 NAS 侧 MinIO，不移动、不删除、不改名 NAS 原文件；不会读取正文或写语义索引。');
  if (!confirmed) return;
  multiExecutionLoading.value = true;
  try {
    multiExecutionResult.value = await executeMultiProjectStorageObjectificationPlan({
      projectIds: multiPlanExecutableProjectIds.value,
      fileIds: multiPlanExecutableFileIds.value,
      realProjectsOnly: true,
      storageState: 'NAS_ONLY',
      checksumState: multiPlanForm.checksumState as 'ANY' | 'HAS_CHECKSUM' | 'MISSING_CHECKSUM',
      extensions: parseExtensions(multiPlanForm.extensionsText),
      limit: multiPlanExecutableFileIds.value.length,
      maxTotalBytes: Math.min(multiPlanMaxTotalBytes.value, controlledExpansionMaxTotalBytes),
      maxFilesPerProject: Math.min(Number(multiPlanForm.maxFilesPerProject || controlledExpansionMaxFilesPerProject), controlledExpansionMaxFilesPerProject),
      maxBytesPerProject: Math.min(multiPlanMaxBytesPerProject.value, controlledExpansionMaxBytesPerProject),
      concurrencyLimit: 1,
      confirmed: true,
      targetProvider: 'MINIO'
    });
    ElMessage.success('多项目小批对象化执行已返回');
    await Promise.all([loadInventory(), loadSummary(), loadTasks()]);
  } finally {
    multiExecutionLoading.value = false;
  }
}

async function openTask(row: StorageMigrationTaskListItem) {
  selectedTask.value = await fetchStorageMigrationTask(row.taskId);
  detailVisible.value = true;
}

async function retryTask(row: StorageMigrationTaskListItem) {
  const confirmed = await confirmAction('将只重试失败或可重试文件；已对象化文件会按幂等策略跳过。');
  if (!confirmed) return;
  const detail = await retryStorageMigrationTask(row.taskId);
  selectedTask.value = detail;
  detailVisible.value = true;
  ElMessage.success('重试任务已创建');
  await Promise.all([loadSummary(), loadTasks()]);
}

async function confirmAction(message: string) {
  try {
    await ElMessageBox.confirm(message, '确认迁移任务', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    });
    return true;
  } catch {
    return false;
  }
}

function openService(name: RouteRecordName) {
  if (name === 'data-steward-agent-preview') {
    void router.push({ name });
    return;
  }
  if (projectId.value) {
    void router.push({ name, params: { projectId: projectId.value } });
  }
}

function parseFileIds(value: string) {
  if (!value.trim()) return [];
  return Array.from(new Set(value
    .split(/[\s,，;；]+/)
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isFinite(item) && item > 0)));
}

function parseExtensions(value: string) {
  if (!value.trim()) return undefined;
  const items = Array.from(new Set(value
    .split(/[\s,，;；]+/)
    .map((item) => item.trim().replace(/^\./, '').toLowerCase())
    .filter(Boolean)));
  return items.length ? items : undefined;
}

function endpointTypeLabel(value: string | null | undefined) {
  return ({
    NAS_SIDE_MINIO: 'NAS 侧 MinIO',
    LOCAL_DEV_MINIO: '本机开发 MinIO',
    UNKNOWN: '待确认 endpoint'
  } as Record<string, string>)[value || ''] ?? value ?? '读取中';
}

function readinessStatusLabel(value: string | null | undefined) {
  return ({
    READY: '已就绪',
    NOT_CONFIGURED: '未配置',
    UNREACHABLE: '不可达',
    LOCAL_DEV_ONLY: '仅本机开发',
    WRITE_UNAVAILABLE: '写入不可用'
  } as Record<string, string>)[value || ''] ?? value ?? '读取中';
}

function readinessTagType(value: string | null | undefined) {
  if (value === 'READY') return 'success';
  if (value === 'LOCAL_DEV_ONLY' || value === 'WRITE_UNAVAILABLE') return 'warning';
  if (value === 'NOT_CONFIGURED' || value === 'UNREACHABLE') return 'danger';
  return 'info';
}

function riskLabel(value: string) {
  return ({
    LOW: '低',
    MEDIUM: '中',
    HIGH: '高'
  } as Record<string, string>)[value] ?? value;
}

function riskTagType(value: string) {
  if (value === 'LOW') return 'success';
  if (value === 'MEDIUM') return 'warning';
  if (value === 'HIGH') return 'danger';
  return 'info';
}

function projectCategoryLabel(value: string) {
  return ({
    REAL_NAS: '真实 NAS',
    TEST_OR_SAMPLE: '测试/样例',
    ARCHIVED: '归档',
    UNKNOWN: '待确认'
  } as Record<string, string>)[value] ?? value;
}

function projectCategoryTagType(value: string) {
  if (value === 'REAL_NAS') return 'success';
  if (value === 'TEST_OR_SAMPLE') return 'warning';
  if (value === 'ARCHIVED') return 'info';
  return 'info';
}

function waveExclusionReasonLabel(value: string | null | undefined) {
  return ({
    NON_REAL_NAS_PROJECT: '非真实 NAS 项目',
    EXCLUDED_PROJECT_CODE: '105 已完成或 95/98/99 待治理',
    TEST_OR_SAMPLE_PROJECT: '测试 / 样例 / 冒烟项目',
    NO_REGISTERED_FILES: '没有已登记文件',
    ALREADY_OBJECTIFIED: '已无 NAS_ONLY 文件',
    SOURCE_REFERENCE_REVIEW_REQUIRED: '存储引用需治理',
    HAS_FAILED_MIGRATION: '存在历史迁移失败',
    PROJECT_TOO_LARGE_FOR_WAVE1: '文件数超出 Wave 1 范围',
    PROJECT_BYTES_TOO_LARGE_FOR_WAVE1: '容量超出 Wave 1 范围'
  } as Record<string, string>)[value || ''] ?? value ?? '不符合候选条件';
}

function runQueueStatusLabel(value: string | null | undefined) {
  return ({
    EXECUTABLE: '可执行',
    GOVERNANCE_REQUIRED: '需治理',
    COMPLETED: '已完成',
    SKIPPED: '跳过'
  } as Record<string, string>)[value || ''] ?? value ?? '待确认';
}

function runQueueStatusTagType(value: string | null | undefined) {
  if (value === 'EXECUTABLE') return 'success';
  if (value === 'GOVERNANCE_REQUIRED') return 'warning';
  if (value === 'COMPLETED') return 'info';
  return 'info';
}

function runQueueReasonLabel(value: string | null | undefined) {
  return ({
    READY_FOR_RUN: '真实项目且存在可对象化文件',
    GOVERNANCE_REVIEW_REQUIRED: '项目映射或历史治理要求人工复核',
    FAILED_MIGRATION_REVIEW_REQUIRED: '存在历史迁移失败，需先治理或重试',
    UNREADABLE_PATH_REVIEW_REQUIRED: '存在路径不可读记录',
    STORAGE_REFERENCE_REVIEW_REQUIRED: '存储引用需治理',
    NO_REGISTERED_FILES: '暂无登记文件',
    ALREADY_OBJECTIFIED: '已无 NAS_ONLY 文件',
    TEST_OR_SAMPLE_PROJECT: '测试 / 样例项目',
    NON_REAL_NAS_PROJECT: '非真实 NAS 项目'
  } as Record<string, string>)[value || ''] ?? value ?? '待确认';
}

function statusType(status: string) {
  const value = (status || '').toUpperCase();
  if (['COMPLETED', 'OBJECT_STORED', 'SKIPPED'].includes(value)) return 'success';
  if (['FAILED', 'PARTIAL_FAILED', 'MIGRATION_FAILED'].includes(value)) return 'danger';
  if (['RUNNING', 'MIGRATION_PENDING', 'PENDING'].includes(value)) return 'warning';
  return 'info';
}

function taskStatusLabel(status: string) {
  return ({
    COMPLETED: '已完成',
    FAILED: '失败',
    PARTIAL_FAILED: '部分失败',
    RUNNING: '执行中'
  } as Record<string, string>)[status] ?? status;
}

function longRunStateLabel(status: string) {
  return ({
    IDLE: '待继续',
    RUNNING: '执行中',
    PAUSED: '已暂停',
    COMPLETED: '已完成',
    PARTIAL_WITH_FAILURES: '剩余治理项',
    FAILED: '失败'
  } as Record<string, string>)[status] ?? status;
}

function longRunStateTagType(status: string) {
  if (status === 'COMPLETED') return 'success';
  if (status === 'PAUSED' || status === 'PARTIAL_WITH_FAILURES') return 'warning';
  if (status === 'FAILED') return 'danger';
  if (status === 'RUNNING') return 'primary';
  return 'info';
}

function migrationStatusLabel(status: string) {
  return ({
    COMPLETED: '已完成',
    FAILED: '失败',
    SKIPPED: '已跳过',
    RUNNING: '执行中',
    PENDING: '待执行'
  } as Record<string, string>)[status] ?? status;
}

function storageStateLabel(status: string) {
  return ({
    NAS_ONLY: '仅 NAS',
    MIGRATION_PENDING: '迁移待完成',
    OBJECT_STORED: '对象已存储',
    MIGRATION_FAILED: '迁移失败',
    MIGRATION_PARTIAL: '部分完成'
  } as Record<string, string>)[status] ?? status;
}

function formatCount(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('zh-CN');
}

function formatBytes(value: number | null | undefined) {
  const size = Number(value ?? 0);
  if (!Number.isFinite(size) || size <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let current = size;
  let index = 0;
  while (current >= 1024 && index < units.length - 1) {
    current /= 1024;
    index += 1;
  }
  return `${current >= 10 || index === 0 ? current.toFixed(0) : current.toFixed(1)} ${units[index]}`;
}

function formatPercent(value: number | null | undefined) {
  const numeric = Number(value ?? 0);
  if (!Number.isFinite(numeric)) return '0.00';
  return numeric.toFixed(2);
}

function formatDate(value: string | null | undefined) {
  if (!value) return '-';
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}
</script>

<style scoped>
.file-service-page {
  min-width: 0;
}

.service-notice,
.service-tabs,
.service-section {
  margin-top: 14px;
}

.m3g-readiness {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(260px, 1.4fr);
  gap: 10px;
  margin-top: 14px;
}

.readiness-card {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-surface);
}

.readiness-card strong {
  color: var(--zy-ink);
  font-size: 22px;
  line-height: 1.1;
}

.readiness-card span,
.readiness-card p {
  margin: 0;
  color: var(--zy-muted);
  font-size: 13px;
}

.migration-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) minmax(220px, 1.2fr);
  gap: 10px;
  align-items: stretch;
}

.migration-metric,
.migration-progress,
.service-card,
.service-section,
.disabled-action {
  min-width: 0;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-surface);
}

.migration-metric {
  display: grid;
  gap: 6px;
  padding: 14px;
}

.migration-metric span,
.migration-progress span,
.service-card p,
.disabled-action span,
.service-section__header span,
.migration-create__side span {
  margin: 0;
  color: var(--zy-muted);
  font-size: 13px;
}

.migration-metric strong {
  color: var(--zy-ink);
  font-size: 24px;
  line-height: 1.1;
}

.migration-progress {
  display: grid;
  align-content: center;
  gap: 10px;
  padding: 14px;
}

.migration-create {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 16px;
  align-items: start;
}

.migration-create__side {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px dashed var(--zy-line);
  border-radius: 8px;
  background: var(--zy-bg);
}

.candidate-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 160px auto;
  gap: 10px;
  margin-bottom: 12px;
}

.dry-run-form {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 150px 160px minmax(180px, 1fr) 130px;
  gap: 10px;
  margin-bottom: 14px;
}

.multi-plan-form {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto 150px minmax(180px, 0.7fr) 130px 150px;
  gap: 10px;
  align-items: center;
}

.m3g-policy-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.m3g-policy-grid > article {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-soft);
}

.m3g-policy-grid span {
  color: var(--zy-muted);
  font-size: 12px;
}

.m3g-policy-grid strong {
  color: var(--zy-ink);
  font-size: 20px;
}

.m3g-policy-grid p {
  margin: 0;
  color: var(--zy-muted);
  font-size: 12px;
  line-height: 1.5;
}

.dry-run-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.dry-run-summary > div {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-soft);
}

.dry-run-summary span {
  color: var(--zy-muted);
  font-size: 12px;
}

.dry-run-summary strong {
  color: var(--zy-ink);
  font-size: 20px;
}

.long-run-field {
  display: grid;
  gap: 4px;
  color: var(--zy-muted);
  font-size: 12px;
}

.long-run-field :deep(.el-input-number) {
  width: 150px;
}

.dry-run-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-bg);
}

.dry-run-actions > div:first-child {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.dry-run-actions strong {
  color: var(--zy-ink);
  font-size: 14px;
}

.dry-run-actions span {
  color: var(--zy-muted);
  font-size: 13px;
}

.dry-run-actions__buttons {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.service-card {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: 14px;
}

.service-card h2 {
  margin: 0;
  color: var(--zy-ink);
  font-size: 16px;
}

.service-section {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.service-section__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.service-section__header h2 {
  margin: 0;
  font-size: 16px;
}

.disabled-action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.disabled-action {
  display: grid;
  gap: 8px;
  padding: 12px;
  background: var(--zy-bg);
}

.disabled-action strong {
  color: var(--zy-ink);
  font-size: 14px;
}

.task-detail {
  display: grid;
  gap: 16px;
}

.task-detail__rows {
  width: 100%;
}

@media (max-width: 1180px) {
  .migration-summary,
  .service-grid,
  .disabled-action-grid,
  .m3g-readiness,
  .m3g-policy-grid,
  .dry-run-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .migration-progress {
    grid-column: span 2;
  }
}

@media (max-width: 760px) {
  .migration-summary,
  .migration-create,
  .candidate-toolbar,
  .m3g-readiness,
  .m3g-policy-grid,
  .multi-plan-form,
  .dry-run-form,
  .dry-run-summary,
  .service-grid,
  .disabled-action-grid {
    grid-template-columns: 1fr;
  }

  .migration-progress {
    grid-column: auto;
  }
}
</style>
