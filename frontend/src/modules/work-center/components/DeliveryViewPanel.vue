<template>
  <section class="mvp-page">
    <div class="mvp-page__header">
      <div>
        <h1>{{ title }}</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="mvp-page__actions">
        <el-button @click="handleExportCompleteness">导出完整率 CSV</el-button>
        <el-button @click="handleExportReviewSummary">导出审核汇总 CSV</el-button>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Link" @click="openCreateDialog">选择文件补交</el-button>
      </div>
    </div>

    <section class="workflow-guide">
      <div class="workflow-guide__main">
        <span class="workflow-guide__step">标准驱动交付闭环</span>
        <h2>{{ title }}从应交项到导出预检查</h2>
        <p>{{ workflowIntro }}</p>
      </div>
      <ol class="workflow-guide__steps">
        <li v-for="step in workflowSteps" :key="step">{{ step }}</li>
      </ol>
    </section>

    <section v-if="completeness" class="delivery-next-action">
      <div>
        <span>当前下一步</span>
        <strong>{{ nextActionText }}</strong>
        <p>{{ nextActionHelper }}</p>
      </div>
      <div class="delivery-next-action__actions">
        <el-button :type="nextActionButtonType" @click="handleNextAction">{{ nextActionButtonText }}</el-button>
        <el-button v-if="completeness.standardReady" @click="loadPrecheck">导出预检查</el-button>
      </div>
    </section>

    <!-- Standard readiness alert -->
    <el-alert
      v-if="completeness && !completeness.standardReady"
      class="mb"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #title>
        <strong>请先生成 / 确认工程主数据草案</strong>
      </template>
      <p class="readiness-lead">
        工作中心仍可查看当前状态，但不能表现为已经可正常交付。请先完成真实项目接入评估、草案确认、部位树、节点类型和交付物标准。
      </p>
      <ul class="issue-list">
        <li v-for="issue in completeness.readinessIssues" :key="issue">{{ issue }}</li>
      </ul>
      <template v-if="!completeness.readinessIssues?.length">
        请先在主数据管理中完善部位树、节点类型、交付物定义、交付物类型和目录模板。
      </template>
      <div class="readiness-help">
        <p>{{ readinessFixText }}</p>
        <div class="readiness-help__actions">
          <el-button size="small" type="primary" @click="goInitialization">生成/确认工程主数据草案</el-button>
          <el-button size="small" @click="goToDeliverableStandard">去配置交付物标准</el-button>
          <el-button size="small" @click="goToNodeTypes">检查节点类型</el-button>
        </div>
      </div>
    </el-alert>

    <!-- Completeness summary -->
    <section v-if="completeness && completeness.standardReady" class="completeness-card">
      <div class="completeness-card__summary">
        <span>本项目按当前交付标准</span>
        <strong>应交 {{ completeness.totalRequired }} 项</strong>
        <span>，</span>
        <strong class="text-success">已补交 {{ completeness.completedCount }} 项</strong>
        <span>，</span>
        <strong class="text-danger">缺失 {{ completeness.missingCount }} 项</strong>
      </div>
      <div class="delivery-state-grid">
        <article v-for="item in deliveryStateCards" :key="item.label">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.helper }}</small>
        </article>
      </div>
      <el-progress
        :percentage="Math.round((completeness.completionRate ?? 0) * 100)"
        :color="progressColor"
        :stroke-width="16"
      />
      <div class="completion-legend">
        <span>补交完整率：已挂接 / 应交</span>
        <span>审核通过率：{{ Math.round((completeness.approvedRate ?? 0) * 100) }}%</span>
      </div>
    </section>

    <section v-else-if="completeness" class="completeness-card completeness-card--empty">
      <p>当前{{ title }}还没有可计算的交付标准。请先在交付物标准中补齐{{ viewLabel }}类交付物类型。</p>
    </section>

    <!-- Tabs: bounded / missing -->
    <el-tabs v-if="completeness" v-model="activeTab" class="mvp-tabs">
      <el-tab-pane :label="`已挂接 ${boundCount}`" name="bound">
        <section class="mvp-stat-row">
          <article class="mvp-stat">
            <span>视图类型</span>
            <strong>{{ view?.viewType ?? viewType }}</strong>
          </article>
          <article class="mvp-stat">
            <span>挂接总数</span>
            <strong>{{ view?.totalCount ?? 0 }}</strong>
          </article>
          <article class="mvp-stat">
            <span>已绑定</span>
            <strong>{{ view?.boundCount ?? 0 }}</strong>
          </article>
        </section>

        <el-table v-loading="loading" :data="view?.rows ?? []" class="master-table" empty-text="暂无交付挂接">
          <el-table-column prop="deliverableDefinitionName" label="交付定义" min-width="170" />
          <el-table-column prop="deliverableTypeName" label="交付类型" min-width="150" />
          <el-table-column prop="fileName" label="文件" min-width="220" />
          <el-table-column prop="sectionNodeName" label="部位" min-width="140" />
          <el-table-column prop="managedObjectName" label="对象" min-width="140" />
          <el-table-column prop="versionNo" label="版本" width="90" />
          <el-table-column prop="reviewStatus" label="审核" width="100">
            <template #default="{ row }">
              <el-tag :type="reviewTagType(row.reviewStatus)" size="small">{{ reviewLabel(row.reviewStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核操作" width="240" fixed="right">
            <template #default="{ row }">
              <template v-if="row.bindingStatus === 'BOUND'">
                <el-button v-if="row.reviewStatus === 'DRAFT'" size="small" type="primary" :loading="reviewLoading[`submit-${row.id}`]" @click="handleSubmitReview(row)">
                  提交审核
                </el-button>
                <el-button v-if="row.reviewStatus === 'PENDING' || row.reviewStatus === 'REJECTED'" size="small" type="success" :loading="reviewLoading[`approve-${row.id}`]" @click="handleApprove(row)">
                  通过
                </el-button>
                <el-button v-if="row.reviewStatus === 'PENDING' || row.reviewStatus === 'REJECTED'" size="small" type="danger" @click="openRejectDialog(row)">
                  驳回
                </el-button>
                <el-button size="small" @click="openReviewRecords(row)">记录</el-button>
              </template>
            </template>
          </el-table-column>
          <el-table-column prop="bindingStatus" label="状态" width="100" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="`缺失项 ${missingRows.length}`" name="missing">
        <p class="tab-helper">缺失项表示“当前标准要求存在，但还没有选择文件完成交付”的条目。每行都说明交付定义、文件类型、目标和缺失原因，可以从这里直接选择当前项目资产目录里的文件补交。</p>
        <el-table v-loading="completenessLoading" :data="missingRows" class="master-table" empty-text="暂无缺失项">
          <el-table-column prop="targetName" label="目标" min-width="140">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.targetType === 'SECTION' ? '部位' : '对象' }}</el-tag>
              {{ row.targetName }}
            </template>
          </el-table-column>
          <el-table-column prop="deliverableDefinitionName" label="交付定义" min-width="170" />
          <el-table-column prop="deliverableTypeName" label="交付类型" min-width="150" />
          <el-table-column prop="fileKind" label="文件类型" width="100" />
          <el-table-column label="为什么缺失" min-width="260">
            <template #default="{ row }">
              {{ missingExplanation(row) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="openBindFromMissing(row)">
                选择文件补交
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- Fallback when completeness API hasn't been loaded yet -->
    <template v-if="!completeness">
      <section class="mvp-stat-row">
        <article class="mvp-stat">
          <span>视图类型</span>
          <strong>{{ view?.viewType ?? viewType }}</strong>
        </article>
        <article class="mvp-stat">
          <span>挂接总数</span>
          <strong>{{ view?.totalCount ?? 0 }}</strong>
        </article>
        <article class="mvp-stat">
          <span>已绑定</span>
          <strong>{{ view?.boundCount ?? 0 }}</strong>
        </article>
      </section>

      <el-table v-loading="loading" :data="view?.rows ?? []" class="master-table" empty-text="暂无交付挂接">
        <el-table-column prop="deliverableDefinitionName" label="交付定义" min-width="170" />
        <el-table-column prop="deliverableTypeName" label="交付类型" min-width="150" />
        <el-table-column prop="fileName" label="文件" min-width="220" />
        <el-table-column prop="sectionNodeName" label="部位" min-width="140" />
        <el-table-column prop="managedObjectName" label="对象" min-width="140" />
        <el-table-column prop="versionNo" label="版本" width="90" />
        <el-table-column prop="reviewStatus" label="审核" width="100">
          <template #default="{ row }">
            <el-tag :type="reviewTagType(row.reviewStatus)" size="small">{{ reviewLabel(row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核操作" width="240" fixed="right">
          <template #default="{ row }">
            <template v-if="row.bindingStatus === 'BOUND'">
              <el-button v-if="row.reviewStatus === 'DRAFT'" size="small" type="primary" :loading="reviewLoading[`submit-${row.id}`]" @click="handleSubmitReview(row)">
                提交审核
              </el-button>
              <el-button v-if="row.reviewStatus === 'PENDING' || row.reviewStatus === 'REJECTED'" size="small" type="success" :loading="reviewLoading[`approve-${row.id}`]" @click="handleApprove(row)">
                通过
              </el-button>
              <el-button v-if="row.reviewStatus === 'PENDING' || row.reviewStatus === 'REJECTED'" size="small" type="danger" @click="openRejectDialog(row)">
                驳回
              </el-button>
              <el-button size="small" @click="openReviewRecords(row)">记录</el-button>
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="bindingStatus" label="状态" width="100" />
      </el-table>
    </template>

    <!-- Reject dialog -->
    <el-dialog v-model="rejectDialogVisible" title="驳回交付资料" width="480px">
      <el-form label-position="top">
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectReason" type="textarea" :rows="3" maxlength="1024" placeholder="请填写驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" :disabled="!rejectReason.trim()" @click="handleReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>

    <!-- Review records drawer -->
    <el-drawer v-model="recordsDrawerVisible" title="审核记录" size="420px">
      <el-timeline v-if="reviewRecords.length">
        <el-timeline-item
          v-for="rec in reviewRecords"
          :key="rec.id"
          :timestamp="rec.createdAt"
          :color="recordColor(rec.action)"
        >
          <strong>{{ recordActionLabel(rec.action) }}</strong>
          <p v-if="rec.comment" style="margin-top: 4px; color: var(--el-text-color-secondary)">{{ rec.comment }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审核记录" />
    </el-drawer>

    <!-- Create binding dialog -->
    <el-dialog v-model="dialogVisible" :title="`选择文件补交到${title}`" width="680px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="交付物类型">
          <el-select v-model="form.deliverableTypeId" filterable>
            <el-option v-for="item in deliverableTypes" :key="item.id" :label="`${item.name} | ${item.code}`" :value="item.id" />
          </el-select>
          <div class="field-hint">这表示当前要补交哪一类资料，通常已由缺失项自动带入。</div>
        </el-form-item>
        <el-form-item label="搜索并添加文件">
          <el-select
            model-value=""
            filterable
            remote
            :remote-method="searchFiles"
            :loading="filesLoading"
            placeholder="输入关键词搜索文件，点击添加到下方列表"
            @focus="searchFiles('')"
            @change="(val: number) => { const f = searchFileCache.get(val); if (f) addFileToSelection(f); }"
          >
            <el-option
              v-for="file in files"
              :key="file.id"
              :label="`${file.originalName} | ${file.versionNo ? 'v' + file.versionNo : ''}`"
              :value="file.id"
            />
          </el-select>
          <div class="field-hint">支持搜索多个文件，逐条添加到下方已选列表。文件选择保持远程分页查询，每次最多显示 20 条当前项目已处理完成的{{ viewLabel }}文件。</div>
        </el-form-item>
        <!-- Selected files list -->
        <el-form-item v-if="selectedFiles.length" label="已选文件">
          <el-table :data="selectedFiles" size="small" max-height="180" class="compact-table">
            <el-table-column label="#" type="index" width="32" />
            <el-table-column prop="originalName" label="文件名" min-width="180" />
            <el-table-column prop="versionNo" label="版本" width="80" />
            <el-table-column label="预览状态" width="150">
              <template #default="{ row }">
                <el-tag :type="previewRiskTagType(previewForFile(row))" size="small">{{ previewStatusLabel(previewForFile(row)) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="50" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="danger" :icon="Delete" circle @click="removeSelectedFile(row.id)" />
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-form-item label="挂接目标">
          <el-segmented v-model="form.targetMode" :options="targetModeOptions" />
          <div class="field-hint">挂接目标用于说明这些文件对应哪个工程部位或管理对象。</div>
        </el-form-item>
        <el-form-item v-if="form.targetMode === 'SECTION'" label="工程部位">
          <el-select v-model="form.sectionNodeId" filterable>
            <el-option v-for="node in sectionOptions" :key="node.id" :label="node.label" :value="node.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.targetMode === 'OBJECT'" label="管理对象">
          <el-select v-model="form.managedObjectId" filterable>
            <el-option v-for="object in objects" :key="object.id" :label="`${object.name} | ${object.code}`" :value="object.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" maxlength="512" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!selectedFiles.length" @click="handleSave">
          保存 {{ selectedFiles.length ? `(${selectedFiles.length}个文件)` : '' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Batch result dialog -->
    <el-dialog v-model="batchResultVisible" title="批量挂接结果" width="560px">
      <section v-if="batchResult" class="batch-result">
        <div class="batch-result__summary">
          <el-tag type="success" size="large">创建 {{ batchResult.createdCount }}</el-tag>
          <el-tag type="warning" size="large">跳过 {{ batchResult.skippedCount }}</el-tag>
          <el-tag type="danger" size="large">失败 {{ batchResult.failedCount }}</el-tag>
        </div>
        <el-table :data="batchResult.results" size="small" max-height="300" class="master-table" style="margin-top:12px">
          <el-table-column prop="fileResourceId" label="文件ID" width="80" />
          <el-table-column label="结果" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'CREATED'" type="success" size="small">已创建</el-tag>
              <el-tag v-else-if="row.status === 'SKIPPED'" type="warning" size="small">已跳过</el-tag>
              <el-tag v-else type="danger" size="small">失败</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="bindingId" label="绑定ID" width="80" />
          <el-table-column prop="message" label="说明" min-width="160" />
        </el-table>
      </section>
      <template #footer>
        <el-button @click="batchResultVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Delivery package readiness section -->
    <section class="package-readiness">
      <div class="package-readiness__header">
        <h2>交付包准备视图</h2>
        <el-button :loading="packageLoading" @click="loadPackageSummary">
          {{ showPackageView ? '刷新准备状态' : '查看交付准备状态' }}
        </el-button>
      </div>
      <p class="field-hint">这是只读准备视图，把当前交付状态汇总为缺失、待审、已驳回和可进入交付包，不生成真实文件包。</p>

      <section v-if="packageSummary && showPackageView" class="package-readiness__content">
        <div class="package-readiness__cards">
          <article class="package-card">
            <h3>文档交付准备</h3>
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item label="应交">{{ packageSummary.documentSummary.totalRequired }}</el-descriptions-item>
              <el-descriptions-item label="已挂接">{{ packageSummary.documentSummary.boundCount }}</el-descriptions-item>
              <el-descriptions-item label="缺失">{{ packageSummary.documentSummary.missingCount }}</el-descriptions-item>
              <el-descriptions-item label="完成率">{{ Math.round(packageSummary.documentSummary.completionRate * 100) }}%</el-descriptions-item>
              <el-descriptions-item label="待审">{{ packageSummary.documentSummary.pendingReviewCount }}</el-descriptions-item>
              <el-descriptions-item label="已通过">{{ packageSummary.documentSummary.approvedCount }}</el-descriptions-item>
              <el-descriptions-item label="已驳回">{{ packageSummary.documentSummary.rejectedCount }}</el-descriptions-item>
              <el-descriptions-item label="可进入交付包">{{ packageSummary.documentSummary.reviewReadyCount }}</el-descriptions-item>
            </el-descriptions>
          </article>
          <article class="package-card">
            <h3>图纸交付准备</h3>
            <el-descriptions :column="2" size="small" border>
              <el-descriptions-item label="应交">{{ packageSummary.drawingSummary.totalRequired }}</el-descriptions-item>
              <el-descriptions-item label="已挂接">{{ packageSummary.drawingSummary.boundCount }}</el-descriptions-item>
              <el-descriptions-item label="缺失">{{ packageSummary.drawingSummary.missingCount }}</el-descriptions-item>
              <el-descriptions-item label="完成率">{{ Math.round(packageSummary.drawingSummary.completionRate * 100) }}%</el-descriptions-item>
              <el-descriptions-item label="待审">{{ packageSummary.drawingSummary.pendingReviewCount }}</el-descriptions-item>
              <el-descriptions-item label="已通过">{{ packageSummary.drawingSummary.approvedCount }}</el-descriptions-item>
              <el-descriptions-item label="已驳回">{{ packageSummary.drawingSummary.rejectedCount }}</el-descriptions-item>
              <el-descriptions-item label="可进入交付包">{{ packageSummary.drawingSummary.reviewReadyCount }}</el-descriptions-item>
            </el-descriptions>
          </article>
        </div>

        <el-table
          v-if="packageSummary.rows.length"
          v-loading="packageLoading"
          :data="packageSummary.rows"
          class="master-table"
          size="small"
          empty-text="暂无交付包明细"
        >
          <el-table-column prop="deliverableDefinitionName" label="交付定义" min-width="150" />
          <el-table-column prop="deliverableTypeName" label="交付类型" min-width="130" />
          <el-table-column prop="targetName" label="目标" min-width="120" />
          <el-table-column prop="fileName" label="文件" min-width="180">
            <template #default="{ row }">
              <span v-if="row.fileName">{{ row.fileName }}</span>
              <el-tag v-else size="small" type="danger">MISSING</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileKind" label="类型" width="80" />
          <el-table-column prop="versionNo" label="版本" width="80" />
          <el-table-column prop="reviewStatus" label="审核" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.reviewStatus" :type="reviewTagType(row.reviewStatus)" size="small">{{ reviewLabel(row.reviewStatus) }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="readinessStatus" label="准备状态" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.readinessStatus === 'READY'" type="success" size="small">可交付</el-tag>
              <el-tag v-else-if="row.readinessStatus === 'MISSING'" type="danger" size="small">缺失</el-tag>
              <el-tag v-else-if="row.readinessStatus === 'PENDING_REVIEW'" type="warning" size="small">待审核</el-tag>
              <el-tag v-else-if="row.readinessStatus === 'REJECTED'" type="danger" size="small">已驳回</el-tag>
              <el-tag v-else size="small">{{ row.readinessStatus }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无交付包明细数据" />
      </section>
    </section>

    <!-- Export precheck section -->
    <section class="package-readiness">
      <div class="package-readiness__header">
        <h2>导出预检查</h2>
        <el-button :loading="precheckLoading" @click="loadPrecheck">
          {{ showPrecheckView ? '刷新预检查' : '导出预检查' }}
        </el-button>
      </div>
      <p class="field-hint">这是只读检查，不生成真实文件包，不访问、不复制 NAS 文件，也不代表正式导出已完成。预览转换状态只影响在线查看体验，不代表文件不能作为原始文件交付。</p>

      <section v-if="precheckResult && showPrecheckView" class="package-readiness__content">
        <el-alert v-if="precheckResult.dryRun && !precheckResult.packageGenerated" type="info" :closable="false" show-icon class="mb">
          <template #title>
            预检查模式：<strong>dryRun=true</strong>，未生成交付包（<strong>packageGenerated=false</strong>）
          </template>
        </el-alert>

        <div class="precheck-stats">
          <el-row :gutter="12">
            <el-col :span="6">
              <el-statistic title="可纳入导出" :value="precheckResult.readyCount">
                <template #suffix>
                  <el-tag type="success" size="small">READY</el-tag>
                </template>
              </el-statistic>
            </el-col>
            <el-col :span="6">
              <el-statistic title="阻塞" :value="precheckResult.blockedCount">
                <template #suffix>
                  <el-tag type="danger" size="small">BLOCKED</el-tag>
                </template>
              </el-statistic>
            </el-col>
            <el-col :span="6">
              <el-statistic title="缺失" :value="precheckResult.missingCount">
                <template #suffix>
                  <el-tag type="danger" size="small">MISSING</el-tag>
                </template>
              </el-statistic>
            </el-col>
            <el-col :span="6">
              <el-statistic title="待审" :value="precheckResult.pendingReviewCount">
                <template #suffix>
                  <el-tag type="warning" size="small">待审</el-tag>
                </template>
              </el-statistic>
            </el-col>
          </el-row>
          <el-row :gutter="12" style="margin-top:12px">
            <el-col :span="6">
              <el-statistic title="已驳回" :value="precheckResult.rejectedCount">
                <template #suffix>
                  <el-tag type="danger" size="small">驳回</el-tag>
                </template>
              </el-statistic>
            </el-col>
            <el-col :span="6">
              <el-statistic title="需转换预览" :value="precheckResult.conversionRequiredCount">
                <template #suffix>
                  <el-tag type="warning" size="small">需转换</el-tag>
                </template>
              </el-statistic>
            </el-col>
            <el-col :span="6">
              <el-statistic title="暂不支持预览" :value="precheckResult.unsupportedPreviewCount">
                <template #suffix>
                  <el-tag type="info" size="small">不支持</el-tag>
                </template>
              </el-statistic>
            </el-col>
            <el-col :span="6">
              <el-statistic title="总计" :value="precheckResult.totalCount" />
            </el-col>
          </el-row>
        </div>

        <el-table
          v-if="precheckResult.rows.length"
          v-loading="precheckLoading"
          :data="precheckResult.rows"
          class="master-table"
          size="small"
          style="margin-top:16px"
          empty-text="暂无预检查明细"
        >
          <el-table-column prop="deliverableDefinitionName" label="交付定义" min-width="140" />
          <el-table-column prop="deliverableTypeName" label="交付类型" min-width="120" />
          <el-table-column prop="targetName" label="目标" min-width="130">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.targetType === 'SECTION' ? '部位' : '对象' }}</el-tag>
              {{ row.targetName ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="fileResourceId" label="文件ID" width="90">
            <template #default="{ row }">
              <span>{{ row.fileResourceId ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="fileName" label="文件" min-width="160">
            <template #default="{ row }">
              <span v-if="row.fileName">{{ row.fileName }}</span>
              <el-tag v-else size="small" type="danger">MISSING</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileKind" label="类型" width="80" />
          <el-table-column prop="versionNo" label="版本" width="80">
            <template #default="{ row }">
              <span>{{ row.versionNo ?? '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="reviewStatus" label="审核状态" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.reviewStatus" :type="reviewTagType(row.reviewStatus)" size="small">{{ reviewLabel(row.reviewStatus) }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="previewStatus" label="预览状态" width="120">
            <template #default="{ row }">
              <el-tag :type="previewRiskTagType(row)" size="small">{{ previewStatusLabel(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="previewMode" label="预览方式" width="140">
            <template #default="{ row }">
              {{ previewModeLabel(row) }}
            </template>
          </el-table-column>
          <el-table-column prop="actionHint" label="预览提示" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              {{ previewActionHint(row) }}
            </template>
          </el-table-column>
          <el-table-column prop="exportStatus" label="导出状态" width="110">
            <template #default="{ row }">
              <el-tag v-if="row.exportStatus === 'READY'" type="success" size="small">可导出</el-tag>
              <el-tag v-else-if="row.exportStatus === 'MISSING'" type="danger" size="small">缺失</el-tag>
              <el-tag v-else-if="row.exportStatus === 'REVIEW_REQUIRED'" type="warning" size="small">待审核</el-tag>
              <el-tag v-else-if="row.exportStatus === 'REJECTED'" type="danger" size="small">已驳回</el-tag>
              <el-tag v-else-if="row.exportStatus === 'BLOCKED'" type="danger" size="small">阻塞</el-tag>
              <el-tag v-else size="small">{{ row.exportStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="blockReason" label="阻塞原因" min-width="160" />
        </el-table>
        <el-empty v-else description="暂无预检查明细数据" />
      </section>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Delete, Link, Refresh } from '@element-plus/icons-vue';

import { fetchFileResourcesPage, fetchManagedObjects, type FileResource, type ManagedObject } from '@/modules/data-steward/api/dataSteward';
import {
  previewActionHint,
  previewFromFileName,
  previewModeLabel,
  previewRiskTagType,
  previewStatusLabel,
  type PreviewStatusLike
} from '@/modules/data-steward/utils/previewStatus';
import { fetchDeliverableTypes, fetchSectionTree, type DeliverableType, type SectionNode } from '@/modules/master-data/api/masterData';
import {
  createBatchDeliveryBinding, fetchDeliveryCompleteness, fetchDeliveryView,
  fetchDeliveryPackageSummary, fetchExportPrecheck, submitForReview, approveBinding, rejectBinding, fetchReviewRecords,
  exportDeliveryCompletenessCsv, exportReviewSummaryCsv,
  type BatchBindingRowResult, type BatchDeliveryBindingResponse,
  type DeliveryBinding, type DeliveryCompletenessRow, type DeliveryPackageSummaryResponse, type DeliveryView,
  type ExportPrecheckResponse, type ReviewRecordItem
} from '@/modules/work-center/api/delivery';
import { useAuthStore } from '@/stores/auth';

const props = defineProps<{
  viewType: 'DOCUMENT' | 'DRAWING';
  title: string;
}>();

const authStore = useAuthStore();
const router = useRouter();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const view = ref<DeliveryView | null>(null);
const files = ref<FileResource[]>([]);
const filesLoading = ref(false);
const objects = ref<ManagedObject[]>([]);
const allTypes = ref<DeliverableType[]>([]);
const sections = ref<SectionNode[]>([]);
const completeness = ref<Awaited<ReturnType<typeof fetchDeliveryCompleteness>> | null>(null);
const completenessLoading = ref(false);
const activeTab = ref('bound');
let fileAbortController: AbortController | null = null;
const searchFileCache = new Map<number, FileResource>();

const form = reactive({
  deliverableTypeId: null as number | null,
  targetMode: 'SECTION',
  sectionNodeId: null as number | null,
  managedObjectId: null as number | null,
  remark: ''
});

// Multi-file selection state
const selectedFiles = ref<FileResource[]>([]);
const batchResult = ref<BatchDeliveryBindingResponse | null>(null);
const batchResultVisible = ref(false);

// Delivery package readiness
const packageSummary = ref<DeliveryPackageSummaryResponse | null>(null);
const packageLoading = ref(false);
const showPackageView = ref(false);

// Export precheck
const precheckResult = ref<ExportPrecheckResponse | null>(null);
const precheckLoading = ref(false);
const showPrecheckView = ref(false);

const targetModeOptions = [
  { label: '部位', value: 'SECTION' },
  { label: '对象', value: 'OBJECT' }
];

const projectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => authStore.currentUser?.currentProject?.name ?? '等待项目上下文');
const viewLabel = computed(() => (props.viewType === 'DOCUMENT' ? '文档' : '图纸'));
const deliverableTypes = computed(() => allTypes.value.filter((type) => type.fileKind === props.viewType));
const sectionOptions = computed(() => flattenSections(sections.value));
const missingRows = computed(() => completeness.value?.rows?.filter((r) => !r.completed) ?? []);
const boundCount = computed(() => completeness.value?.completedCount ?? view.value?.boundCount ?? 0);
const draftCount = computed(() => completeness.value?.draftCount ?? statusCount('DRAFT'));
const pendingReviewCount = computed(() => completeness.value?.pendingReviewCount ?? statusCount('PENDING'));
const approvedCount = computed(() => completeness.value?.approvedCount ?? statusCount('APPROVED'));
const rejectedCount = computed(() => completeness.value?.rejectedCount ?? statusCount('REJECTED'));
const reviewReadyCount = computed(() => completeness.value?.reviewReadyCount ?? approvedCount.value);
const workflowIntro = computed(() =>
  `平台会按当前项目的工程部位和交付物标准生成${viewLabel.value}应交项。先补交文件，再提交审核；通过后才能进入交付包 dry-run 预检查，驳回项需要进入整改闭环。`
);
const workflowSteps = computed(() => [
  '确认工程主数据和交付物标准已就绪。',
  `在缺失项中选择需要补交的${viewLabel.value}资料。`,
  `从当前项目资产目录远程搜索已处理完成的${viewLabel.value}文件。`,
  '保存为草稿后提交审核，审核通过后刷新完整率和交付包准备状态。',
  '驳回项进入整改中心，处理后复审或重新补交，再执行导出预检查。'
]);
const deliveryStateCards = computed(() => [
  { label: '应交', value: completeness.value?.totalRequired ?? 0, helper: '当前标准生成' },
  { label: '已补交', value: boundCount.value, helper: '已挂接文件' },
  { label: '缺失', value: completeness.value?.missingCount ?? 0, helper: '需要选择文件' },
  { label: '草稿', value: draftCount.value, helper: '待提交审核' },
  { label: '待审', value: pendingReviewCount.value, helper: '等待审核判断' },
  { label: '已驳回', value: rejectedCount.value, helper: '进入整改闭环' },
  { label: '已通过', value: approvedCount.value, helper: '可纳入交付' },
  { label: '可导出', value: reviewReadyCount.value, helper: '预检查 READY 基础' }
]);
const nextActionText = computed(() => completeness.value?.nextActionText ?? '加载当前交付状态后，平台会提示下一步动作。');
const nextActionHelper = computed(() => {
  const code = completeness.value?.nextActionCode;
  if (code === 'COMPLETE_STANDARD') return '先回到工程主数据，生成 / 确认草案，再补齐部位树、节点类型、交付物类型和目录模板。';
  if (code === 'BIND_MISSING_FILES') return '建议从缺失项标签页进入，系统会自动带入交付类型和目标。';
  if (code === 'SUBMIT_REVIEW') return '草稿资料已经挂接，但还没有进入审核队列。';
  if (code === 'HANDLE_RECTIFICATION') return '驳回资料会自动生成整改项，请在整改中心处理后再复审。';
  if (code === 'REVIEW_PENDING') return '审核人可在已挂接列表中通过或驳回，结果会刷新交付准备状态。';
  if (code === 'EXPORT_PRECHECK') return '预检查仍是 dry-run，不生成真实文件包，也不复制或访问 NAS 文件。';
  return '当前状态用于指导普通员工按顺序完成标准驱动交付。';
});
const nextActionButtonText = computed(() => {
  const code = completeness.value?.nextActionCode;
  if (code === 'COMPLETE_STANDARD') return '生成/确认主数据';
  if (code === 'DEFINE_DELIVERABLES') return '去补标准';
  if (code === 'BIND_MISSING_FILES') return '查看缺失项';
  if (code === 'SUBMIT_REVIEW' || code === 'REVIEW_PENDING') return '查看已挂接';
  if (code === 'HANDLE_RECTIFICATION') return '处理整改';
  if (code === 'EXPORT_PRECHECK') return '执行预检查';
  return '刷新状态';
});
const nextActionButtonType = computed(() => (completeness.value?.nextActionCode === 'EXPORT_PRECHECK' ? 'success' : 'primary'));
const readinessFixText = computed(() => {
  const issues = completeness.value?.readinessIssues ?? [];
  if (issues.some((issue) => issue.includes('当前视图类型') || issue.includes('交付物类型'))) {
    return `当前缺少${viewLabel.value}类交付物类型。请到交付物标准中为这个视图补一条文件类型为 ${props.viewType} 的交付物类型。`;
  }
  if (issues.some((issue) => issue.includes('节点类型'))) {
    return '请先到节点类型页面确认并锁定节点类型，再回到交付物标准配置资料要求。';
  }
  if (issues.some((issue) => issue.includes('部位'))) {
    return '请先建立工程部位树，平台会按部位树生成后续交付缺失项。';
  }
  return '请先生成 / 确认工程主数据草案，再按工程部位树、节点类型、交付物标准和目录模板的顺序补齐前置条件。';
});
const progressColor = computed(() => {
  const rate = completeness.value?.completionRate ?? 0;
  if (rate >= 0.8) return '#67c23a';
  if (rate >= 0.5) return '#e6a23c';
  return '#f56c6c';
});

watch(projectId, () => loadPage(), { immediate: true });

async function loadPage() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [nextView, nextObjects, nextTypes, nextSections] = await Promise.all([
      fetchDeliveryView(projectId.value, props.viewType),
      fetchManagedObjects(projectId.value),
      fetchDeliverableTypes(projectId.value),
      fetchSectionTree(projectId.value)
    ]);
    view.value = nextView;
    objects.value = nextObjects;
    allTypes.value = nextTypes;
    sections.value = nextSections;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '交付视图加载失败');
  } finally {
    loading.value = false;
  }
  await loadCompleteness();
  await Promise.all([
    showPackageView.value ? loadPackageSummary() : Promise.resolve(),
    showPrecheckView.value ? loadPrecheck() : Promise.resolve()
  ]);
}

// Review state
const reviewLoading = reactive<Record<string, boolean>>({});
const rejectDialogVisible = ref(false);
const rejectReason = ref('');
const rejecting = ref(false);
const rejectTarget = ref<DeliveryBinding | null>(null);
const recordsDrawerVisible = ref(false);
const reviewRecords = ref<ReviewRecordItem[]>([]);

function reviewTagType(status: string) {
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'danger';
  if (status === 'PENDING') return 'warning';
  return 'info';
}

function reviewLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' };
  return map[status] ?? status;
}

function statusCount(status: string) {
  return view.value?.rows?.filter((row) => row.reviewStatus === status).length ?? 0;
}

function missingExplanation(row: DeliveryCompletenessRow) {
  const targetLabel = row.targetType === 'OBJECT' ? '对象' : '部位';
  return `${targetLabel}“${row.targetName}”需要交付“${row.deliverableDefinitionName} / ${row.deliverableTypeName}”，当前尚未挂接${viewLabel.value}文件。`;
}

function recordColor(action: string) {
  if (action === 'APPROVED') return '#67c23a';
  if (action === 'REJECTED') return '#f56c6c';
  if (action === 'SUBMITTED') return '#409eff';
  return '#909399';
}

function recordActionLabel(action: string) {
  const map: Record<string, string> = { SUBMITTED: '提交审核', APPROVED: '审核通过', REJECTED: '驳回' };
  return map[action] ?? action;
}

async function handleSubmitReview(row: DeliveryBinding) {
  if (!projectId.value) return;
  const key = `submit-${row.id}`;
  reviewLoading[key] = true;
  try {
    await submitForReview(projectId.value, row.id);
    ElMessage.success('已提交审核');
    await loadPage();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '提交审核失败');
  } finally {
    reviewLoading[key] = false;
  }
}

async function handleApprove(row: DeliveryBinding) {
  if (!projectId.value) return;
  const key = `approve-${row.id}`;
  reviewLoading[key] = true;
  try {
    await approveBinding(projectId.value, row.id);
    ElMessage.success('审核已通过');
    await loadPage();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '审核通过失败');
  } finally {
    reviewLoading[key] = false;
  }
}

function openRejectDialog(row: DeliveryBinding) {
  rejectTarget.value = row;
  rejectReason.value = '';
  rejectDialogVisible.value = true;
}

async function handleReject() {
  if (!projectId.value || !rejectTarget.value || !rejectReason.value.trim()) return;
  rejecting.value = true;
  try {
    await rejectBinding(projectId.value, rejectTarget.value.id, rejectReason.value.trim());
    ElMessage.success('已驳回并生成整改项');
    rejectDialogVisible.value = false;
    await loadPage();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '驳回失败');
  } finally {
    rejecting.value = false;
  }
}

async function openReviewRecords(row: DeliveryBinding) {
  if (!projectId.value) return;
  try {
    reviewRecords.value = await fetchReviewRecords(projectId.value, row.id) ?? [];
  } catch {
    reviewRecords.value = [];
  }
  recordsDrawerVisible.value = true;
}

async function loadCompleteness() {
  if (!projectId.value) return;
  completenessLoading.value = true;
  try {
    completeness.value = await fetchDeliveryCompleteness(projectId.value, props.viewType);
  } catch {
    completeness.value = null;
  } finally {
    completenessLoading.value = false;
  }
}

async function searchFiles(keyword: string) {
  if (!projectId.value) return;
  if (fileAbortController) fileAbortController.abort();
  fileAbortController = new AbortController();
  filesLoading.value = true;
  try {
    const result = await fetchFileResourcesPage(
      projectId.value,
      {
        fileKind: props.viewType,
        keyword: keyword || undefined,
        processStatus: 'PROCESSED',
        pageNo: 1,
        pageSize: 20
      },
      fileAbortController.signal
    );
    files.value = result.rows;
    for (const f of result.rows) {
      searchFileCache.set(f.id, f);
    }
  } catch {
    // aborted or error - ignore
  } finally {
    filesLoading.value = false;
  }
}

function openCreateDialog() {
  Object.assign(form, {
    deliverableTypeId: deliverableTypes.value[0]?.id ?? null,
    targetMode: 'SECTION',
    sectionNodeId: sectionOptions.value[0]?.id ?? null,
    managedObjectId: objects.value[0]?.id ?? null,
    remark: ''
  });
  files.value = [];
  selectedFiles.value = [];
  batchResult.value = null;
  batchResultVisible.value = false;
  dialogVisible.value = true;
}

function openBindFromMissing(row: DeliveryCompletenessRow) {
  Object.assign(form, {
    deliverableTypeId: row.deliverableTypeId,
    targetMode: row.targetType === 'OBJECT' ? 'OBJECT' : 'SECTION',
    sectionNodeId: row.targetType === 'SECTION' ? row.targetId : sectionOptions.value[0]?.id ?? null,
    managedObjectId: row.targetType === 'OBJECT' ? row.targetId : objects.value[0]?.id ?? null,
    remark: ''
  });
  files.value = [];
  selectedFiles.value = [];
  batchResult.value = null;
  batchResultVisible.value = false;
  dialogVisible.value = true;
}

function addFileToSelection(file: FileResource) {
  if (!selectedFiles.value.some((f) => f.id === file.id)) {
    selectedFiles.value.push(file);
  }
  files.value = [];
}

function removeSelectedFile(fileId: number) {
  selectedFiles.value = selectedFiles.value.filter((f) => f.id !== fileId);
}

function previewForFile(file: FileResource): PreviewStatusLike {
  return previewFromFileName(file.originalName, file.fileKind);
}

async function loadPrecheck() {
  if (!projectId.value) return;
  precheckLoading.value = true;
  try {
    precheckResult.value = await fetchExportPrecheck(projectId.value, props.viewType);
    showPrecheckView.value = true;
  } catch {
    precheckResult.value = null;
  } finally {
    precheckLoading.value = false;
  }
}

async function handleSave() {
  if (!projectId.value || !form.deliverableTypeId || selectedFiles.value.length === 0) return;
  saving.value = true;
  try {
    const result = await createBatchDeliveryBinding(projectId.value, {
      viewType: props.viewType,
      deliverableTypeId: form.deliverableTypeId,
      fileResourceIds: selectedFiles.value.map((f) => f.id),
      sectionNodeId: form.targetMode === 'SECTION' ? form.sectionNodeId : null,
      managedObjectId: form.targetMode === 'OBJECT' ? form.managedObjectId : null,
      bindingStatus: 'BOUND',
      reviewStatus: 'DRAFT',
      remark: form.remark
    });
    batchResult.value = result;
    batchResultVisible.value = true;
    if (result.createdCount > 0) {
      ElMessage.success(`成功保存 ${result.createdCount} 个草稿${result.skippedCount > 0 ? `，跳过 ${result.skippedCount} 个` : ''}${result.failedCount > 0 ? `，失败 ${result.failedCount} 个` : ''}，请在已挂接列表提交审核`);
    } else if (result.skippedCount > 0) {
      ElMessage.warning(`所有 ${result.skippedCount} 个文件均已挂接过，已跳过`);
    } else {
      ElMessage.error(`挂接失败：${result.failedCount} 个文件`);
    }
    dialogVisible.value = false;
    activeTab.value = 'bound';
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function loadPackageSummary() {
  if (!projectId.value) return;
  packageLoading.value = true;
  try {
    packageSummary.value = await fetchDeliveryPackageSummary(projectId.value, props.viewType);
    showPackageView.value = true;
  } catch {
    packageSummary.value = null;
  } finally {
    packageLoading.value = false;
  }
}

function handleExportCompleteness() {
  if (!projectId.value) return;
  exportDeliveryCompletenessCsv(projectId.value);
  ElMessage.success('交付完整率 CSV 导出已触发下载');
}

function handleExportReviewSummary() {
  if (!projectId.value) return;
  exportReviewSummaryCsv(projectId.value);
  ElMessage.success('审核汇总 CSV 导出已触发下载');
}

function goToDeliverableStandard() {
  if (!projectId.value) return;
  router.push({ name: 'project-master-data-deliverable-standard', params: { projectId: projectId.value } });
}

function goToNodeTypes() {
  if (!projectId.value) return;
  router.push({ name: 'project-master-data-node-types', params: { projectId: projectId.value } });
}

function goToRectifications() {
  if (!projectId.value) return;
  router.push({ name: 'project-work-rectifications', params: { projectId: projectId.value } });
}

function goInitialization() {
  if (!projectId.value) return;
  router.push({ name: 'project-master-data-initialization', params: { projectId: projectId.value } });
}

async function handleNextAction() {
  const code = completeness.value?.nextActionCode;
  if (code === 'COMPLETE_STANDARD') {
    goInitialization();
    return;
  }
  if (code === 'DEFINE_DELIVERABLES') {
    goToDeliverableStandard();
    return;
  }
  if (code === 'BIND_MISSING_FILES') {
    activeTab.value = 'missing';
    return;
  }
  if (code === 'SUBMIT_REVIEW' || code === 'REVIEW_PENDING') {
    activeTab.value = 'bound';
    return;
  }
  if (code === 'HANDLE_RECTIFICATION') {
    goToRectifications();
    return;
  }
  if (code === 'EXPORT_PRECHECK') {
    await loadPrecheck();
    return;
  }
  await loadPage();
}

function flattenSections(nodes: SectionNode[], prefix = ''): Array<{ id: number; label: string }> {
  return nodes.flatMap((node) => {
    const label = `${prefix}${node.name}`;
    return [{ id: node.id, label }, ...flattenSections(node.children ?? [], `${prefix} / `)];
  });
}
</script>

<style scoped>
.completeness-card {
  background: var(--el-fill-color-lighter, #f5f7fa);
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 16px;
}
.completeness-card--empty {
  text-align: center;
  color: var(--el-text-color-secondary);
}
.completeness-card__summary {
  margin-bottom: 12px;
  font-size: 15px;
  line-height: 1.6;
}
.delivery-next-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  background: #ffffff;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-left: 4px solid var(--el-color-primary, #409eff);
  border-radius: 8px;
}
.delivery-next-action span {
  display: block;
  margin-bottom: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.delivery-next-action strong {
  display: block;
  color: var(--el-text-color-primary);
  line-height: 1.5;
}
.delivery-next-action p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
.delivery-next-action__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}
.delivery-state-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(112px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}
.delivery-state-grid article {
  min-width: 0;
  padding: 10px 12px;
  background: #ffffff;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
}
.delivery-state-grid span,
.delivery-state-grid small {
  display: block;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.delivery-state-grid strong {
  display: block;
  color: var(--el-text-color-primary);
  font-size: 20px;
  line-height: 1.45;
}
.completion-legend {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.text-success {
  color: var(--el-color-success, #67c23a);
}
.text-danger {
  color: var(--el-color-danger, #f56c6c);
}
.issue-list {
  margin: 4px 0 0;
  padding-left: 20px;
}
.issue-list li {
  margin-bottom: 2px;
}
.readiness-help {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}
.readiness-lead {
  margin: 0 0 8px;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
.readiness-help p,
.tab-helper {
  margin: 0;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
}
.readiness-help__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tab-helper {
  margin-bottom: 12px;
}
.mb {
  margin-bottom: 16px;
}
.mvp-tabs {
  margin-top: 12px;
}
.batch-result__summary {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 8px;
}
.package-readiness {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 2px solid var(--el-border-color-light);
}
.package-readiness__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.package-readiness__header h2 {
  margin: 0;
  font-size: 16px;
}
.package-readiness__content {
  margin-top: 12px;
}
.package-readiness__cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}
.package-card h3 {
  margin: 0 0 8px;
  font-size: 14px;
}
.compact-table {
  font-size: 13px;
}
@media (max-width: 760px) {
  .delivery-next-action {
    align-items: flex-start;
    flex-direction: column;
  }
  .delivery-next-action__actions {
    justify-content: flex-start;
  }
}
</style>
