<template>
  <section class="bim-page">
    <header class="bim-toolbar">
      <div>
        <span class="bim-toolbar__eyebrow">{{ projectLabel }}</span>
        <h1>{{ pageTitle }}</h1>
        <p>{{ pageSubtitle }}</p>
      </div>
      <div class="bim-toolbar__actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadPage">刷新</el-button>
        <template v-if="showWorkflowActions">
          <el-button :icon="DocumentChecked" @click="publishContract">发布契约</el-button>
          <el-button type="primary" :icon="Plus" @click="batchDialogVisible = true">创建批次</el-button>
        </template>
      </div>
    </header>

    <el-alert
      v-if="!projectId"
      type="warning"
      :closable="false"
      show-icon
      title="请先选择项目"
      description="BIM报建数据按项目隔离，当前账号没有可用项目时无法进入闭环。"
    />

    <template v-if="activeSection === 'overview'">
      <section class="bim-overview-band">
        <div class="bim-section-title">
          <h2>治理地图</h2>
          <span>把标准、插件、构件数据和闭环问题分开看</span>
        </div>
        <div class="bim-governance-map">
          <article v-for="item in governanceMap" :key="item.title" class="bim-governance-card">
            <div>
              <strong>{{ item.title }}</strong>
              <el-tag :type="item.tagType" effect="plain">{{ item.status }}</el-tag>
            </div>
            <p>{{ item.object }}</p>
            <small>{{ item.outcome }}</small>
          </article>
        </div>
      </section>

      <section class="bim-grid-two">
        <div class="bim-panel">
          <div class="bim-section-title">
            <h2>当前状态</h2>
            <span>只显示进度和风险，不展开标准细则</span>
          </div>
          <div class="bim-status-list">
            <div>
              <span>标准接入</span>
              <strong>智能化已录入，其他专业待接入</strong>
              <el-tag type="success" effect="plain">可继续深化</el-tag>
            </div>
            <div>
              <span>插件契约</span>
              <strong>字段矩阵可导出，后端暂不落库</strong>
              <el-tag type="primary" effect="plain">前端 Mock</el-tag>
            </div>
            <div>
              <span>构件数据</span>
              <strong>真实上传数据为空，数据中心可本地维护候选记录</strong>
              <el-tag type="warning" effect="plain">待插件上传</el-tag>
            </div>
            <div>
              <span>质量风险</span>
              <strong>{{ qualityIssueRows.length }} 类问题需要后续闭环</strong>
              <el-tag type="danger" effect="plain">需治理</el-tag>
            </div>
          </div>
        </div>

        <div class="bim-panel">
          <div class="bim-section-title">
            <h2>去哪里处理</h2>
            <span>总览只做导航，具体内容进入对应功能页</span>
          </div>
          <div class="bim-module-entry-list">
            <router-link v-for="item in overviewEntrypoints" :key="item.path" :to="item.path" class="bim-module-entry">
              <span>{{ item.label }}</span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.description }}</small>
            </router-link>
          </div>
        </div>
      </section>

      <section class="bim-panel">
        <div class="bim-section-title">
          <h2>闭环路径</h2>
          <span>页面按业务对象拆分，不再把标准和上传数据混成一个视图</span>
        </div>
        <div class="bim-flow-strip">
          <article v-for="step in governanceSteps" :key="step.title">
            <strong>{{ step.title }}</strong>
            <span>{{ step.object }}</span>
            <small>{{ step.result }}</small>
          </article>
        </div>
      </section>
    </template>

    <template v-else-if="activeSection === 'codeCenter'">
      <section class="bim-panel">
        <div class="bim-section-title">
          <h2>专业编码标准</h2>
          <el-tooltip content="按专业查看编码标准包。当前仅智能化已录入，未录入专业先显示状态，后续可由 MySQL 标准库驱动。" placement="top">
            <button class="bim-info-dot" type="button" aria-label="专业编码标准说明">i</button>
          </el-tooltip>
        </div>
        <div class="bim-profession-grid bim-profession-grid--compact">
          <button
            v-for="item in codeStandard?.professions ?? []"
            :key="item.code"
            class="bim-profession"
            :class="{ 'bim-profession--active': selectedProfession === item.code }"
            type="button"
            @click="selectedProfession = item.code"
          >
            <strong>{{ item.name }}</strong>
            <el-tag :type="professionStatusTag(item.status)" effect="plain">
              {{ professionStatusLabel(item.status) }}
            </el-tag>
          </button>
        </div>
      </section>

      <section class="bim-panel">
        <div class="bim-section-title">
          <h2>{{ selectedProfessionName }}标准状态</h2>
          <el-tag :type="selectedProfessionRecorded ? 'success' : 'info'" effect="plain">
            {{ selectedProfessionRecorded ? '已录入' : '待录入' }}
          </el-tag>
        </div>
        <div class="bim-standard-status-row">
          <div>
            <span>编码规则</span>
            <strong>{{ selectedProfessionRecorded ? filteredCodeRules.length : 0 }}</strong>
          </div>
          <div>
            <span>系统组</span>
            <strong>{{ selectedProfessionRecorded ? (codeStandard?.systemGroups.length ?? 0) : 0 }}</strong>
          </div>
          <div>
            <span>构件类型</span>
            <strong>{{ selectedProfessionRecorded ? componentStandardCount : 0 }}</strong>
          </div>
          <div>
            <span>规则状态</span>
            <strong>{{ selectedProfessionRecorded ? standardPackageStatusLabel(selectedStandardPackage?.packageStatus) : '待录入' }}</strong>
          </div>
        </div>
      </section>

      <section v-if="!selectedProfessionRecorded" class="bim-panel bim-pending-standard">
        <el-empty
          :description="`${selectedProfessionName}编码标准待录入`"
          :image-size="72"
        >
          <div class="bim-pending-standard__meta">
            <el-tag type="info" effect="plain">专业标准包待接入</el-tag>
            <el-tag type="info" effect="plain">数据库建议：MySQL</el-tag>
            <el-tag type="warning" effect="plain">不使用智能化标准代替</el-tag>
          </div>
        </el-empty>
      </section>

      <template v-else>
        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>编码规则</h2>
            <el-tooltip content="只展示当前专业已录入的专业编码规则和全专业公共规则。" placement="top">
              <button class="bim-info-dot" type="button" aria-label="编码规则说明">i</button>
            </el-tooltip>
          </div>
          <div class="bim-code-rule-list">
            <article v-for="rule in filteredCodeRules" :key="rule.id" class="bim-code-rule">
              <div class="bim-code-rule__main">
                <div>
                  <strong>{{ rule.title }}</strong>
                  <span>{{ rule.source }} · {{ rule.status }}</span>
                </div>
                <code>{{ rule.example }}</code>
                <div class="bim-code-rule__meta">
                  <el-tag :type="standardScopeTag(rule.scope ?? 'PROFESSION')" effect="plain">
                    {{ standardScopeLabel(rule.scope ?? 'PROFESSION') }}
                  </el-tag>
                  <el-tag effect="plain">{{ rule.applicableProfessions?.join('、') ?? '全部专业' }}</el-tag>
                </div>
                <p>{{ rule.description }}</p>
                <small v-if="rule.generationStrategy">{{ rule.generationStrategy }}</small>
                <small v-if="rule.uniquenessScope">唯一性：{{ rule.uniquenessScope }}</small>
              </div>
              <div class="bim-code-segments">
                <div v-for="segment in rule.segments" :key="`${rule.id}-${segment.name}`">
                  <span>{{ segment.name }}</span>
                  <strong>{{ segment.chars }}</strong>
                  <small>{{ segment.rule }}</small>
                  <el-tag size="small" :type="segment.required ? 'danger' : 'info'" effect="plain">
                    {{ segment.required ? '必填' : '非必填' }}
                  </el-tag>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section class="bim-grid-two">
          <div class="bim-panel">
            <div class="bim-section-title">
              <h2>智能化系统组</h2>
              <el-tag effect="plain">{{ codeStandard?.systemGroups.length ?? 0 }} 个</el-tag>
            </div>
            <el-table :data="codeStandard?.systemGroups ?? []" class="bim-table" max-height="360">
              <el-table-column prop="code" label="代码" width="90" />
              <el-table-column prop="name" label="系统名称" min-width="190" show-overflow-tooltip />
              <el-table-column prop="scope" label="适用" width="130" show-overflow-tooltip />
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.status === '已录入' ? 'success' : 'warning'" effect="plain">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="bim-panel">
            <div class="bim-section-title">
              <h2>校验样例</h2>
              <el-tooltip content="样例用于展示字段类型、半角符号、父子继承和枚举取值等校验口径。" placement="top">
                <button class="bim-info-dot" type="button" aria-label="校验样例说明">i</button>
              </el-tooltip>
            </div>
            <div class="bim-validation-list">
              <article v-for="item in codeStandard?.validationExamples ?? []" :key="`${item.label}-${item.value}`">
                <div>
                  <strong>{{ item.label }}</strong>
                  <code>{{ item.value }}</code>
                </div>
                <el-tag :type="validationStatusTag(item.status)" effect="plain">{{ validationStatusLabel(item.status) }}</el-tag>
                <p>{{ item.reason }}</p>
              </article>
            </div>
          </div>
        </section>

        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>智能化构件标识池</h2>
            <el-tag effect="plain">{{ componentStandardCount }} 类</el-tag>
          </div>
          <div class="bim-filter-row">
            <el-input
              v-model="componentStandardKeyword"
              clearable
              placeholder="筛选构件标识、映射名称、来源页签"
              aria-label="智能化构件标准筛选"
            />
          </div>
          <el-table :data="filteredComponentStandards" class="bim-table" max-height="460" empty-text="暂无构件标准">
            <el-table-column label="来源对象" min-width="250" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.sourceObjectPath }}</strong>
                  <span>{{ row.sourceSheet }} R{{ row.sourceRow }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="shenzhenComponentIdentifier" label="深圳构件标识" min-width="170" show-overflow-tooltip />
            <el-table-column prop="intelligentMappingName" label="智能化映射" min-width="170" show-overflow-tooltip />
            <el-table-column label="字段数" width="90" align="right">
              <template #default="{ row }">{{ row.fieldCount }}</template>
            </el-table-column>
            <el-table-column label="样例字段" min-width="260" show-overflow-tooltip>
              <template #default="{ row }">{{ fieldList(row.sampleFields) }}</template>
            </el-table-column>
          </el-table>
        </section>

        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>属性字段库</h2>
            <el-tag effect="plain">{{ propertyRuleCount }} 个字段</el-tag>
          </div>
          <div class="bim-filter-row">
            <el-input
              v-model="propertyKeyword"
              clearable
              placeholder="筛选字段、类型、单位、映射范围"
              aria-label="属性字段筛选"
            />
          </div>
          <el-table :data="filteredPropertyRules" class="bim-table" max-height="460" empty-text="暂无字段规则">
            <el-table-column prop="fieldName" label="字段" min-width="150" show-overflow-tooltip />
            <el-table-column prop="valueType" label="类型" width="90" />
            <el-table-column prop="unit" label="单位" width="90" />
            <el-table-column prop="valueRequirement" label="取值要求" min-width="190" show-overflow-tooltip />
            <el-table-column prop="mappingScope" label="适用范围" min-width="170" show-overflow-tooltip />
            <el-table-column prop="sourceSheet" label="来源" min-width="130" show-overflow-tooltip />
            <el-table-column label="策略" width="150">
              <template #default="{ row }">
                <el-tag :type="pluginFieldPolicyTag(row.requiredPolicy)" effect="plain">
                  {{ row.requiredPolicy }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </template>

      <section class="bim-panel">
        <el-collapse class="bim-compact-collapse">
          <el-collapse-item title="标准来源 / 分层 / 编码主导权" name="standard-meta">
            <div class="bim-grid-two">
              <div>
                <el-table :data="codeStandard?.standardSources ?? []" class="bim-table" max-height="260">
                  <el-table-column prop="name" label="来源" min-width="180" show-overflow-tooltip />
                  <el-table-column prop="fileName" label="文件" min-width="220" show-overflow-tooltip />
                  <el-table-column label="状态" width="100">
                    <template #default="{ row }">
                      <el-tag :type="standardSourceTag(row.status)" effect="plain">{{ sourceStatusLabel(row.status) }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
              <div v-if="codeStandard?.platformEncodingPolicy" class="bim-policy-card bim-policy-card--compact">
                <strong>{{ codeStandard.platformEncodingPolicy.title }}</strong>
                <div class="bim-evidence-tags">
                  <el-tag
                    v-for="item in codeStandard.platformEncodingPolicy.uploadRequiredEvidence"
                    :key="item"
                    effect="plain"
                  >
                    {{ item }}
                  </el-tag>
                </div>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </section>
    </template>

    <template v-else-if="activeSection === 'pluginContract'">
        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>插件注入边界</h2>
            <span>插件执行写入和上传，平台负责规则版本、唯一性和最终校验</span>
          </div>
          <div class="bim-contract-split">
            <article>
              <strong>插件建议写入</strong>
              <span>深圳构件标识、深圳系统标识、位号编码、构件编码、专业编码等候选值</span>
            </article>
            <article>
              <strong>插件必须上传证据</strong>
              <span>标准版本、专业、构件类型、候选编码、模型指纹、Revit ElementId、Revit UniqueId</span>
            </article>
            <article>
              <strong>平台校验确认</strong>
              <span>编码唯一性、标准版本一致性、字段完整性、冲突处理和整改闭环</span>
            </article>
          </div>
        </section>

        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>智能化插件上传建议表</h2>
            <span>给插件开发团队对齐候选字段，草案状态下不作为最终必填清单</span>
          </div>
          <div class="bim-standard-export">
            <div>
              <strong>{{ intelligentPluginFieldStandardRows.length }}</strong>
              <span>字段矩阵行</span>
            </div>
            <div>
              <strong>{{ componentStandardCount }}</strong>
              <span>智能化构件类型</span>
            </div>
            <div>
              <strong>UTF-8 CSV</strong>
              <span>Excel 可直接打开</span>
            </div>
            <el-button type="primary" :icon="Download" @click="exportIntelligentPluginStandardCsv">
              导出智能化上传建议 CSV
            </el-button>
          </div>
          <el-table
            :data="intelligentPluginFieldStandardPreview"
            class="bim-table"
            max-height="420"
            empty-text="暂无字段标准"
          >
            <el-table-column prop="shenzhenComponentIdentifier" label="深圳构件标识" min-width="160" show-overflow-tooltip />
            <el-table-column prop="intelligentMappingName" label="智能化映射" min-width="160" show-overflow-tooltip />
            <el-table-column prop="fieldName" label="字段名" min-width="140" show-overflow-tooltip />
            <el-table-column label="策略" width="160">
              <template #default="{ row }">
                <el-tag :type="pluginFieldPolicyTag(row.requiredPolicy)" effect="plain">
                  {{ row.requiredPolicy }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="valueType" label="类型" width="90" />
            <el-table-column prop="suggestedRevitParameterName" label="建议 Revit 参数" min-width="170" show-overflow-tooltip />
            <el-table-column prop="standardSource" label="来源" min-width="130" show-overflow-tooltip />
          </el-table>
        </section>

        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>候选上传字段</h2>
            <span>优先给插件团队对齐，当前不冻结最终必填清单</span>
          </div>
          <div class="bim-required-field-list">
            <article v-for="item in candidatePluginUploadFields" :key="item.name">
              <div>
                <strong>{{ item.name }}</strong>
                <code>{{ item.parameter }}</code>
              </div>
              <span>{{ item.constraint }}</span>
              <small>{{ item.fillMode }} · {{ item.note }}</small>
            </article>
          </div>
          <p class="bim-helper-note">
            字段矩阵是插件上传建议和校验草案，不代表每个构件都需要人工填写全部字段；真实样本进入平台后再沉淀最终必填清单。
          </p>
        </section>

        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>48 个智能化映射构件</h2>
            <span>按附件三当前整理条目展示，后续如附件修订则同步版本</span>
          </div>
          <el-table :data="componentStandardIndexRows" class="bim-table" max-height="520" empty-text="暂无构件清单">
            <el-table-column prop="index" label="序号" width="76" />
            <el-table-column prop="shenzhenComponentIdentifier" label="深圳构件标识" min-width="160" show-overflow-tooltip />
            <el-table-column prop="intelligentMappingName" label="智能化映射名称" min-width="170" show-overflow-tooltip />
            <el-table-column prop="sourceObjectPath" label="来源对象层级" min-width="260" show-overflow-tooltip />
            <el-table-column label="来源" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">{{ row.sourceSheet }} R{{ row.sourceRow }}</template>
            </el-table-column>
            <el-table-column label="字段数" width="90" align="right">
              <template #default="{ row }">{{ row.fieldCount }}</template>
            </el-table-column>
          </el-table>
        </section>
    </template>

    <template v-else-if="activeSection === 'dataCenter'">
        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>专业编码数据</h2>
            <span>当前仅智能化有约束草案，真实上传数据为空，其他专业待录入</span>
          </div>
          <div class="bim-data-professions">
            <button
              v-for="profession in dataCenterProfessionCards"
              :key="profession.code"
              class="bim-data-profession"
              :class="{ 'bim-data-profession--active': selectedDataCenterProfession === profession.code }"
              type="button"
              @click="selectDataCenterProfession(profession.code)"
            >
              <div>
                <strong>{{ profession.name }}</strong>
                <el-tag :type="dataCenterProfessionStatusTag(profession.status)" effect="plain">
                  {{ dataCenterProfessionStatusLabel(profession.status) }}
                </el-tag>
              </div>
              <span>{{ profession.source }}</span>
              <small v-if="profession.status === 'RECORDED'">
                {{ profession.componentCount }} 条编码记录 · {{ profession.warningCount }} 个需处理 ·
                {{ profession.missingFieldCount }} 个空值
              </small>
              <small v-else>{{ profession.note }}</small>
            </button>
          </div>
        </section>

        <section class="bim-data-summary-grid">
          <article class="bim-data-summary">
            <span>上传记录</span>
            <strong>{{ uploadedCodeRecordSummary.total }}</strong>
            <em>{{ showSampleUploadedCodeRecords ? `含 ${uploadedCodeRecordSummary.sample} 条示例` : '暂无真实插件上传' }}</em>
          </article>
          <article class="bim-data-summary">
            <span>通过</span>
            <strong>{{ uploadedCodeRecordSummary.passed }}</strong>
            <em>符合当前约束</em>
          </article>
          <article class="bim-data-summary">
            <span>预警</span>
            <strong>{{ uploadedCodeRecordSummary.warning }}</strong>
            <em>格式或字段需复核</em>
          </article>
          <article class="bim-data-summary">
            <span>冲突</span>
            <strong>{{ uploadedCodeRecordSummary.conflict }}</strong>
            <em>重复编码不覆盖</em>
          </article>
          <article class="bim-data-summary">
            <span>待确认</span>
            <strong>{{ uploadedCodeRecordSummary.pending }}</strong>
            <em>草案规则结果</em>
          </article>
          <article class="bim-data-summary">
            <span>最近更新</span>
            <strong class="bim-data-summary__date">{{ formatDate(uploadedCodeRecordSummary.lastUploadedAt) }}</strong>
            <em>本地 Mock 管理</em>
          </article>
        </section>

        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>上传编码记录</h2>
            <span>平台管理插件上传的候选编码值，当前只在前端本地维护</span>
          </div>
          <div class="bim-record-toolbar">
            <div class="bim-filter-row">
              <el-select
                v-model="uploadedCodeQuery.validationStatus"
                clearable
                placeholder="校验状态"
                aria-label="上传编码校验状态"
                @change="loadUploadedCodeRecords"
              >
                <el-option label="通过" value="PASSED" />
                <el-option label="预警" value="WARNING" />
                <el-option label="冲突" value="CONFLICT" />
                <el-option label="待确认" value="PENDING_CONFIRMATION" />
              </el-select>
              <el-input
                v-model="uploadedCodeQuery.keyword"
                clearable
                placeholder="构件、编码、模型指纹、Revit 标识"
                aria-label="上传编码记录关键词"
                @keyup.enter="loadUploadedCodeRecords"
              />
              <el-button :icon="Search" @click="loadUploadedCodeRecords">查询</el-button>
            </div>
            <div class="bim-record-toolbar__actions">
              <el-switch
                v-model="showSampleUploadedCodeRecords"
                active-text="显示校验示例"
                inactive-text="隐藏示例"
                @change="handleSampleRecordToggle"
              />
              <el-button @click="revalidateUploadedRecords">重新校验</el-button>
              <el-button type="primary" :icon="Plus" @click="openUploadedCodeDialog()">新增记录</el-button>
            </div>
          </div>
          <el-alert
            v-if="!showSampleUploadedCodeRecords && uploadedCodeRecords.length === 0"
            type="info"
            :closable="false"
            show-icon
            title="暂无真实上传编码记录"
            description="当前不伪造数据库数据。需要演示校验时，可以打开“显示校验示例”，或手动新增一条本地记录。"
          />
          <el-table
            v-loading="dataCenterLoading"
            :data="uploadedCodeRecords"
            class="bim-table"
            empty-text="暂无上传编码记录"
          >
            <el-table-column prop="professionName" label="专业" width="96" fixed="left" />
            <el-table-column label="构件" min-width="220" fixed="left" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.componentName || '-' }}</strong>
                  <span>{{ row.componentType || '构件类型待填' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="codeType" label="编码类型" min-width="130" />
            <el-table-column label="上传编码值" min-width="230" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.rawCodeValue || '-' }}</strong>
                  <span>{{ row.sourceParameterName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="模型链接" min-width="250" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.modelFingerprint || '-' }}</strong>
                  <span>{{ row.revitElementId || '-' }} / {{ row.revitUniqueId || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="校验" width="110">
              <template #default="{ row }">
                <el-tag :type="uploadedCodeStatusTag(row.validationStatus)" effect="plain">
                  {{ uploadedCodeStatusLabel(row.validationStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="问题" min-width="250" show-overflow-tooltip>
              <template #default="{ row }">{{ validationIssueSummary(row.validationIssues) }}</template>
            </el-table-column>
            <el-table-column label="来源" width="110">
              <template #default="{ row }">
                <el-tag :type="row.sample ? 'info' : 'success'" effect="plain">
                  {{ row.sample ? '示例' : '本地记录' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="170">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" :disabled="row.sample" @click="openUploadedCodeDialog(row)">编辑</el-button>
                <el-button text type="danger" :disabled="row.sample" @click="deleteUploadedRecord(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section v-if="isAllProfessionDataCenter && showSampleUploadedCodeRecords" class="bim-data-group-list">
          <article v-for="group in dataCenterProfessionGroups" :key="group.code" class="bim-panel">
            <div class="bim-section-title">
              <h2>{{ group.name }}构件数据</h2>
              <span>{{ group.note }}</span>
            </div>
            <el-empty
              v-if="group.status === 'PENDING'"
              description="该专业标准待录入，数据库数据待接入"
              :image-size="64"
            >
              <el-tag type="info" effect="plain">不会用智能化示例数据代替</el-tag>
            </el-empty>
            <el-table
              v-else
              v-loading="dataCenterLoading"
              :data="group.components"
              class="bim-table"
              empty-text="暂无构件数据"
            >
              <el-table-column label="构件" min-width="260">
                <template #default="{ row }">
                  <div class="bim-main-cell">
                    <strong>{{ row.componentName }}</strong>
                    <span>{{ row.componentCode }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="shenzhenComponentIdentifier" label="深圳构件标识" min-width="150" />
              <el-table-column label="所属系统" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">
                  <div class="bim-main-cell">
                    <strong>{{ row.systemName }}</strong>
                    <span>{{ row.systemCode }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="模型来源" min-width="210" show-overflow-tooltip>
                <template #default="{ row }">
                  <div class="bim-main-cell">
                    <strong>{{ row.modelName }}</strong>
                    <span>{{ row.batchNo }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="字段完整率" width="150">
                <template #default="{ row }">
                  <el-progress
                    :percentage="row.fieldCompletionRate"
                    :status="completionProgressStatus(row.qualityStatus)"
                    :stroke-width="8"
                  />
                </template>
              </el-table-column>
              <el-table-column label="质量" width="110">
                <template #default="{ row }">
                  <el-tag :type="dataCenterQualityStatusTag(row.qualityStatus)" effect="plain">
                    {{ dataCenterQualityStatusLabel(row.qualityStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="编码校验" width="130">
                <template #default="{ row }">
                  <el-tag :type="codeCandidateStatusTag(row.codeCandidateStatus)" effect="plain">
                    {{ codeCandidateStatusLabel(row.codeCandidateStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="uploadEvidence" label="上传证据" min-width="180" show-overflow-tooltip />
              <el-table-column label="操作" width="170" fixed="right">
                <template #default="{ row }">
                  <el-button text type="primary" @click="openDataCenterComponent(row)">详情</el-button>
                  <el-button text @click="openWorkOrder(row)">建工单</el-button>
                </template>
              </el-table-column>
            </el-table>
          </article>
        </section>

        <section v-else-if="dataCenterSummary?.status === 'PENDING'" class="bim-panel">
          <el-empty
            description="该专业标准待录入，数据库数据待接入"
            :image-size="72"
          >
            <el-tag type="info" effect="plain">不会用示例数据伪装为已上传</el-tag>
          </el-empty>
        </section>

        <section v-else-if="showSampleUploadedCodeRecords" class="bim-panel">
          <div class="bim-section-title">
            <h2>构件汇总</h2>
            <span>按后端汇总数据形态展示，后续接口直接承接这张表</span>
          </div>
          <div class="bim-filter-row">
            <el-select v-model="dataCenterQuery.batchId" clearable placeholder="批次" aria-label="批次筛选">
              <el-option
                v-for="batch in overview?.batches ?? []"
                :key="batch.id"
                :label="batch.batchName"
                :value="batch.id"
              />
            </el-select>
            <el-select
              v-model="dataCenterQuery.qualityStatus"
              clearable
              placeholder="质量状态"
              aria-label="质量状态筛选"
            >
              <el-option label="通过" value="PASSED" />
              <el-option label="预警" value="WARNING" />
              <el-option label="缺失" value="MISSING" />
            </el-select>
            <el-input
              v-model="dataCenterQuery.keyword"
              clearable
              placeholder="构件编码、标识、系统、模型"
              aria-label="数据中心关键词"
              @keyup.enter="loadDataCenterComponents"
            />
            <el-button :icon="Search" @click="loadDataCenterComponents">查询</el-button>
          </div>
          <el-table
            v-loading="dataCenterLoading"
            :data="dataCenterComponents"
            class="bim-table"
            empty-text="暂无构件数据"
          >
            <el-table-column prop="professionName" label="专业" width="100" fixed="left" />
            <el-table-column label="构件" min-width="270" fixed="left">
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.componentName }}</strong>
                  <span>{{ row.componentCode }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="shenzhenComponentIdentifier" label="深圳构件标识" min-width="150" />
            <el-table-column label="所属系统" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.systemName }}</strong>
                  <span>{{ row.systemCode }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="模型来源" min-width="210" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.modelName }}</strong>
                  <span>{{ row.batchNo }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="楼层/族类型" min-width="190" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.levelName }}</strong>
                  <span>{{ row.familyName }} / {{ row.typeName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="字段完整率" width="150">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.fieldCompletionRate"
                  :status="completionProgressStatus(row.qualityStatus)"
                  :stroke-width="8"
                />
              </template>
            </el-table-column>
            <el-table-column label="质量" width="110">
              <template #default="{ row }">
                <el-tag :type="dataCenterQualityStatusTag(row.qualityStatus)" effect="plain">
                  {{ dataCenterQualityStatusLabel(row.qualityStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="编码校验" width="130">
              <template #default="{ row }">
                <el-tag :type="codeCandidateStatusTag(row.codeCandidateStatus)" effect="plain">
                  {{ codeCandidateStatusLabel(row.codeCandidateStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" min-width="170">
              <template #default="{ row }">{{ formatDate(row.uploadedAt) }}</template>
            </el-table-column>
            <el-table-column prop="uploadEvidence" label="上传证据" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="openDataCenterComponent(row)">详情</el-button>
                <el-button text @click="openWorkOrder(row)">建工单</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
    </template>

    <template v-else-if="activeSection === 'quality'">
        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>质量治理对象</h2>
            <span>把平台已经发现的问题单独呈现，便于后续生成整改工单</span>
          </div>
          <div class="bim-quality-grid">
            <article v-for="item in qualityGovernanceItems" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
              <small>{{ item.note }}</small>
            </article>
          </div>
        </section>

        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>问题清单</h2>
            <span>当前为前端 Mock，后续由后端校验任务汇总</span>
          </div>
          <el-table :data="qualityIssueRows" class="bim-table" empty-text="暂无质量问题">
            <el-table-column prop="issueType" label="问题类型" min-width="150" />
            <el-table-column label="治理对象" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.target }}</strong>
                  <span>{{ row.componentCode }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="standard" label="对应标准" min-width="180" show-overflow-tooltip />
            <el-table-column prop="reason" label="问题说明" min-width="300" show-overflow-tooltip />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.severity === '严重' ? 'danger' : 'warning'" effect="plain">
                  {{ row.severity }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>
    </template>

    <template v-else-if="activeSection === 'batches'">
        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>报建批次</h2>
            <span>创建批次后插件按批次拉取值包并上传构件</span>
          </div>
          <el-table v-loading="loading" :data="overview?.batches ?? []" class="bim-table" empty-text="暂无批次">
            <el-table-column label="批次" min-width="220">
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.batchName }}</strong>
                  <span>{{ row.batchNo }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="契约" width="100">
              <template #default="{ row }">{{ row.contractVersionNo }}</template>
            </el-table-column>
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="batchStatusTag(row.status)" effect="plain">{{ batchStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="构件" width="90" align="right">
              <template #default="{ row }">{{ row.componentCount }}</template>
            </el-table-column>
            <el-table-column label="预警" width="90" align="right">
              <template #default="{ row }">{{ row.warningCount }}</template>
            </el-table-column>
            <el-table-column label="工单" width="90" align="right">
              <template #default="{ row }">{{ row.workOrderCount }}</template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="170">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="archiveBatch(row)">归档</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
    </template>

    <template v-else-if="activeSection === 'workOrders'">
        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>整改工单</h2>
            <span>管理方发起，建模方在模型中修改后由插件重新回填</span>
          </div>
          <el-table v-loading="loading" :data="overview?.workOrders ?? []" class="bim-table" empty-text="暂无工单">
            <el-table-column label="工单" min-width="260">
              <template #default="{ row }">
                <div class="bim-main-cell">
                  <strong>{{ row.title }}</strong>
                  <span>{{ row.fieldCode || '-' }}：{{ row.currentValue || '-' }} -> {{ row.requestedValue || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="workOrderStatusTag(row.status)" effect="plain">{{ workOrderStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="priority" label="优先级" width="100" />
            <el-table-column label="更新时间" min-width="170">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" :disabled="row.status === 'CLOSED'" @click="closeWorkOrder(row)">
                  关闭
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
    </template>

    <template v-else-if="activeSection === 'archives'">
        <section class="bim-panel">
          <div class="bim-section-title">
            <h2>归档包</h2>
            <span>保存契约版本、值包、构件数据、上传证据、整改闭环和复核记录</span>
          </div>
          <el-table v-loading="loading" :data="overview?.archives ?? []" class="bim-table" empty-text="暂无归档包">
            <el-table-column prop="archiveNo" label="归档编号" min-width="180" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag type="success" effect="plain">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="摘要" min-width="320" show-overflow-tooltip>
              <template #default="{ row }">{{ summaryText(row.summaryJson) }}</template>
            </el-table-column>
            <el-table-column label="创建时间" min-width="170">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </section>
    </template>

    <el-dialog v-model="batchDialogVisible" title="创建报建批次" width="420px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="批次名称" required>
          <el-input v-model="batchForm.batchName" placeholder="例如：C塔机电报建首轮" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitBatch">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="workOrderDialogVisible" title="创建整改工单" width="520px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="字段编码" required>
          <el-input v-model="workOrderForm.fieldCode" placeholder="例如：tag_code" />
        </el-form-item>
        <el-form-item label="工单标题" required>
          <el-input v-model="workOrderForm.title" placeholder="说明需要模型方修正的字段" />
        </el-form-item>
        <el-form-item label="目标值">
          <el-input v-model="workOrderForm.requestedValue" placeholder="管理方要求回填的值" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="workOrderForm.description" type="textarea" :rows="3" placeholder="补充复核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="workOrderDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitWorkOrder">派发</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="uploadedCodeDialogVisible"
      :title="editingUploadedCodeRecord ? '编辑上传编码记录' : '新增上传编码记录'"
      width="680px"
    >
      <el-form label-position="top" class="bim-uploaded-code-form" @submit.prevent>
        <el-form-item label="专业">
          <el-select v-model="uploadedCodeForm.professionCode" @change="syncUploadedProfessionName">
            <el-option label="智能化" value="intelligent" />
            <el-option label="建筑（待录入）" value="architecture" />
            <el-option label="结构（待录入）" value="structure" />
            <el-option label="给排水（待录入）" value="plumbing" />
            <el-option label="暖通（待录入）" value="hvac" />
            <el-option label="电气（待录入）" value="electrical" />
          </el-select>
        </el-form-item>
        <el-form-item label="构件类型">
          <el-input v-model="uploadedCodeForm.componentType" placeholder="例如：服务器、交换机、摄像机" />
        </el-form-item>
        <el-form-item label="构件名称">
          <el-input v-model="uploadedCodeForm.componentName" placeholder="例如：核心交换机 SW-Core-01" />
        </el-form-item>
        <el-form-item label="编码类型">
          <el-select v-model="uploadedCodeForm.codeType">
            <el-option label="位号编码" value="位号编码" />
            <el-option label="构件编码" value="构件编码" />
            <el-option label="深圳构件标识" value="深圳构件标识" />
            <el-option label="深圳系统标识" value="深圳系统标识" />
            <el-option label="物料编码" value="物料编码" />
            <el-option label="设备编码" value="设备编码" />
          </el-select>
        </el-form-item>
        <el-form-item label="插件上传编码值">
          <el-input v-model="uploadedCodeForm.rawCodeValue" placeholder="插件回传的原始编码值" />
        </el-form-item>
        <el-form-item label="来源 Revit 参数">
          <el-input v-model="uploadedCodeForm.sourceParameterName" placeholder="例如：DD_TagCode" />
        </el-form-item>
        <el-form-item label="模型指纹">
          <el-input v-model="uploadedCodeForm.modelFingerprint" placeholder="例如：sha256:..." />
        </el-form-item>
        <el-form-item label="Revit ElementId">
          <el-input v-model="uploadedCodeForm.revitElementId" placeholder="例如：884201" />
        </el-form-item>
        <el-form-item label="Revit UniqueId">
          <el-input v-model="uploadedCodeForm.revitUniqueId" placeholder="插件上传的 Revit UniqueId" />
        </el-form-item>
        <el-form-item label="上传批次">
          <el-input v-model="uploadedCodeForm.batchNo" placeholder="例如：BIM-CT-20260527-001" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadedCodeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitUploadedCodeRecord">保存并校验</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="dataCenterDetailDrawerVisible" title="构件上传详情" size="680px">
      <template v-if="dataCenterDetail">
        <div class="bim-detail-head">
          <strong>{{ dataCenterDetail.component.componentName }}</strong>
          <span>{{ dataCenterDetail.component.batchNo }} · {{ dataCenterDetail.component.componentCode }}</span>
        </div>
        <div class="bim-evidence-grid">
          <div>
            <span>插件版本</span>
            <strong>{{ dataCenterDetail.uploadEvidence.pluginVersion }}</strong>
          </div>
          <div>
            <span>Revit版本</span>
            <strong>{{ dataCenterDetail.uploadEvidence.revitVersion }}</strong>
          </div>
          <div>
            <span>契约版本</span>
            <strong>{{ dataCenterDetail.uploadEvidence.contractVersion }}</strong>
          </div>
          <div>
            <span>模型指纹</span>
            <strong>{{ dataCenterDetail.uploadEvidence.modelFingerprint }}</strong>
          </div>
        </div>
        <section class="bim-detail-section">
          <div class="bim-detail-section__title">
            <h3>编码与构件链接</h3>
            <span>平台校验候选编码，记录 Revit 元素与上传批次关系</span>
          </div>
          <el-alert
            v-if="dataCenterDetail.codeCandidate.validationStatus === 'CONFLICT'"
            type="error"
            :closable="false"
            show-icon
            title="发现编码冲突"
            :description="dataCenterDetail.codeCandidate.conflictReason ?? '候选编码重复，需管理方确认后派发整改。'"
          />
          <div class="bim-link-grid">
            <div>
              <span>候选编码</span>
              <strong>{{ dataCenterDetail.codeCandidate.candidateCode }}</strong>
            </div>
            <div>
              <span>标准版本</span>
              <strong>{{ dataCenterDetail.codeCandidate.standardVersion }}</strong>
            </div>
            <div>
              <span>编码校验</span>
              <el-tag :type="codeCandidateStatusTag(dataCenterDetail.codeCandidate.validationStatus)" effect="plain">
                {{ codeCandidateStatusLabel(dataCenterDetail.codeCandidate.validationStatus) }}
              </el-tag>
            </div>
            <div>
              <span>唯一性范围</span>
              <strong>{{ dataCenterDetail.codeCandidate.uniquenessScope }}</strong>
            </div>
            <div>
              <span>Revit ElementId</span>
              <strong>{{ dataCenterDetail.componentLink.revitElementId }}</strong>
            </div>
            <div>
              <span>Revit UniqueId</span>
              <strong>{{ dataCenterDetail.componentLink.revitUniqueId }}</strong>
            </div>
            <div>
              <span>预览状态</span>
              <strong>{{ previewStatusLabel(dataCenterDetail.componentLink.previewStatus) }}</strong>
            </div>
            <div>
              <span>批次链接</span>
              <strong>{{ dataCenterDetail.componentLink.batchNo }}</strong>
            </div>
          </div>
          <p class="bim-preview-note">{{ dataCenterDetail.componentLink.previewNote }}</p>
          <div class="bim-field-coverage">
            <span>
              候选字段 {{ dataCenterDetail.fieldCoverage.expectedFieldCount }} 项，已填
              {{ dataCenterDetail.fieldCoverage.filledFieldCount }} 项
            </span>
            <template v-if="dataCenterDetail.fieldCoverage.missingRequiredFields.length > 0">
              <el-tag
                v-for="field in dataCenterDetail.fieldCoverage.missingRequiredFields"
                :key="field"
                type="danger"
                effect="plain"
              >
                缺失：{{ field }}
              </el-tag>
            </template>
            <el-tag v-else type="success" effect="plain">候选字段已覆盖</el-tag>
          </div>
        </section>
        <el-table :data="dataCenterDetail.fields" class="bim-table" empty-text="暂无字段">
          <el-table-column prop="fieldName" label="字段" min-width="140" />
          <el-table-column prop="fieldValue" label="值" min-width="180" show-overflow-tooltip />
          <el-table-column prop="valueType" label="类型" width="90" />
          <el-table-column prop="unitName" label="单位" width="90" />
          <el-table-column prop="sourceParameterName" label="Revit参数" min-width="140" show-overflow-tooltip />
          <el-table-column label="校验" width="110">
            <template #default="{ row }">
              <el-tag :type="dataCenterQualityStatusTag(row.validationStatus)" effect="plain">
                {{ dataCenterQualityStatusLabel(row.validationStatus) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <pre class="bim-json">{{ prettyJson(dataCenterDetail.rawParameters) }}</pre>
      </template>
      <el-empty v-else description="请选择构件" :image-size="56" />
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { DocumentChecked, Download, Plus, Refresh, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';

import {
  archiveBimSubmissionBatch,
  closeBimSubmissionWorkOrder,
  createBimSubmissionBatch,
  createBimUploadedCodeRecord,
  createBimSubmissionWorkOrder,
  deleteBimUploadedCodeRecord,
  fetchBimDataCenterComponentDetail,
  fetchBimDataCenterComponents,
  fetchBimDataCenterOverview,
  fetchBimUploadedCodeRecords,
  fetchBimSubmissionOverview,
  publishBimSubmissionContract,
  revalidateBimUploadedCodeRecords,
  updateBimUploadedCodeRecord,
  type BimCodeCandidateStatus,
  type BimDataCenterComponent,
  type BimDataCenterComponentDetail,
  type BimDataCenterOverview,
  type BimDataCenterProfessionStatus,
  type BimPreviewStatus,
  type BimDataCenterQualityStatus,
  type BimUploadedCodeRecord,
  type BimUploadedCodeRecordPayload,
  type BimUploadedCodeStatus,
  type BimSubmissionBatch,
  type BimSubmissionOverview,
  type BimSubmissionWorkOrder
} from '@/modules/bim-submission/api/bimSubmission';
import {
  fetchBimCodeStandardOverview,
  fetchIntelligentPluginFieldStandardRows,
  type BimCodeStandardOverview,
  type IntelligentPluginFieldStandardRow,
  type ProfessionStatusCode,
  type StandardScope,
  type StandardSourceStatus,
  type ValidationStatus
} from '@/modules/bim-submission/api/codeStandard';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const route = useRoute();
const overview = ref<BimSubmissionOverview | null>(null);
const codeStandard = ref<BimCodeStandardOverview | null>(null);
const dataCenter = ref<BimDataCenterOverview | null>(null);
const dataCenterComponents = ref<BimDataCenterComponent[]>([]);
const dataCenterDetail = ref<BimDataCenterComponentDetail | null>(null);
const uploadedCodeRecords = ref<BimUploadedCodeRecord[]>([]);
const loading = ref(false);
const dataCenterLoading = ref(false);
const saving = ref(false);
const batchDialogVisible = ref(false);
const workOrderDialogVisible = ref(false);
const uploadedCodeDialogVisible = ref(false);
const dataCenterDetailDrawerVisible = ref(false);
const selectedComponent = ref<BimDataCenterComponent | null>(null);
const editingUploadedCodeRecord = ref<BimUploadedCodeRecord | null>(null);
const selectedProfession = ref('intelligent');
const selectedDataCenterProfession = ref('all');
const componentStandardKeyword = ref('');
const propertyKeyword = ref('');
const showSampleUploadedCodeRecords = ref(false);

const batchForm = reactive({ batchName: '' });
const workOrderForm = reactive({
  fieldCode: '',
  title: '',
  requestedValue: '',
  description: ''
});
const dataCenterQuery = reactive<{
  batchId?: number;
  keyword: string;
  qualityStatus: BimDataCenterQualityStatus | '';
}>({
  batchId: undefined,
  keyword: '',
  qualityStatus: ''
});
const uploadedCodeQuery = reactive<{
  keyword: string;
  validationStatus: BimUploadedCodeStatus | '';
}>({
  keyword: '',
  validationStatus: ''
});
const uploadedCodeForm = reactive<BimUploadedCodeRecordPayload>({
  professionCode: 'intelligent',
  professionName: '智能化',
  componentType: '',
  componentName: '',
  codeType: '位号编码',
  rawCodeValue: '',
  sourceParameterName: 'DD_TagCode',
  modelFingerprint: '',
  revitElementId: '',
  revitUniqueId: '',
  batchNo: '',
  standardPackageStatus: 'DRAFT'
});

const projectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => authStore.currentUser?.currentProject?.name ?? '等待项目上下文');
const activeSection = computed(() => {
  const routeName = String(route.name ?? '');
  if (routeName.endsWith('overview')) return 'overview';
  if (routeName.endsWith('code-center')) return 'codeCenter';
  if (routeName.endsWith('plugin-contract')) return 'pluginContract';
  if (routeName.endsWith('data-center')) return 'dataCenter';
  if (routeName.endsWith('quality')) return 'quality';
  if (routeName.endsWith('batches')) return 'batches';
  if (routeName.endsWith('work-orders')) return 'workOrders';
  if (routeName.endsWith('archives')) return 'archives';
  return 'overview';
});
const pageTitle = computed(() => {
  const labels: Record<string, string> = {
    overview: 'BIM报建总览',
    codeCenter: 'BIM报建编码标准中心',
    pluginContract: 'BIM报建插件契约中心',
    dataCenter: 'BIM报建数据中心',
    quality: 'BIM报建质量校验',
    batches: 'BIM报建批次闭环',
    workOrders: 'BIM报建整改工单',
    archives: 'BIM报建归档摘要'
  };
  return labels[activeSection.value];
});
const pageSubtitle = computed(() => {
  const labels: Record<string, string> = {
    overview: '先看治理范围，再进入标准、插件契约、构件数据和质量闭环。',
    codeCenter: '管理全专业编码框架和智能化约束草案，字段值等真实上传样本后再确认。',
    pluginContract: '面向插件开发团队，明确候选编码和值证据的上传契约。',
    dataCenter: '管理插件上传的候选编码记录，支持本地增删改查和平台校验演示。',
    quality: '集中展示编码、字段、版本和上传证据的质量问题。',
    batches: '管理报建批次、值包下发、插件上传和批次归档前置状态。',
    workOrders: '管理方发起整改，建模方回填后由平台复核闭环。',
    archives: '汇总契约版本、构件数据、上传证据、整改闭环和归档摘要。'
  };
  return labels[activeSection.value];
});
const showWorkflowActions = computed(() => ['batches', 'workOrders', 'archives'].includes(activeSection.value));
const activeStandardSourceCount = computed(
  () => codeStandard.value?.standardSources.filter((source) => source.status === 'ACTIVE').length ?? 0
);
const codeRuleCount = computed(() => codeStandard.value?.codeRules.length ?? 0);
const componentStandardCount = computed(() => codeStandard.value?.componentStandards.length ?? 0);
const propertyRuleCount = computed(() => codeStandard.value?.propertyRules.length ?? 0);
const selectedProfessionRow = computed(() => (
  codeStandard.value?.professions.find((item) => item.code === selectedProfession.value) ?? null
));
const selectedStandardPackage = computed(() => (
  codeStandard.value?.professionStandardPackages.find((item) => item.professionCode === selectedProfession.value) ?? null
));
const selectedProfessionName = computed(() => selectedProfessionRow.value?.name ?? '当前专业');
const selectedProfessionRecorded = computed(() => selectedProfessionRow.value?.status === 'RECORDED');
const selectedProfessionAliases = computed(() => {
  if (selectedProfession.value === 'intelligent') return ['智能化', '电气/智能化专业', '全部专业'];
  return [selectedProfessionName.value, selectedProfessionName.value.replace('专业', ''), '全部专业'];
});
const filteredCodeRules = computed(() => {
  const rows = codeStandard.value?.codeRules ?? [];
  if (!selectedProfessionRecorded.value) return [];
  return rows.filter((rule) => (
    rule.scope === 'GLOBAL'
    || rule.applicableProfessions?.some((profession) => selectedProfessionAliases.value.includes(profession))
  ));
});
const dataCenterSummary = computed(() => dataCenter.value?.summary ?? null);
const isAllProfessionDataCenter = computed(() => selectedDataCenterProfession.value === 'all');
const uploadedCodeRecordSummary = computed(() => ({
  total: uploadedCodeRecords.value.length,
  passed: uploadedCodeRecords.value.filter((record) => record.validationStatus === 'PASSED').length,
  warning: uploadedCodeRecords.value.filter((record) => record.validationStatus === 'WARNING').length,
  conflict: uploadedCodeRecords.value.filter((record) => record.validationStatus === 'CONFLICT').length,
  pending: uploadedCodeRecords.value.filter((record) => record.validationStatus === 'PENDING_CONFIRMATION').length,
  sample: uploadedCodeRecords.value.filter((record) => record.sample).length,
  lastUploadedAt: uploadedCodeRecords.value[0]?.updatedAt ?? null
}));
const dataCenterProfessionCards = computed(() => (dataCenter.value?.professionSummaries ?? []).map((profession) => {
  const rows = profession.code === 'all'
    ? uploadedCodeRecords.value
    : uploadedCodeRecords.value.filter((record) => record.professionCode === profession.code);
  if (profession.status === 'PENDING') return profession;
  return {
    ...profession,
    componentCount: rows.length,
    warningCount: rows.filter((record) => (
      record.validationStatus === 'WARNING'
      || record.validationStatus === 'CONFLICT'
      || record.validationStatus === 'PENDING_CONFIRMATION'
    )).length,
    missingFieldCount: rows.reduce((total, record) => (
      total + record.validationIssues.filter((issue) => issue.type === 'MISSING_FIELD').length
    ), 0),
    lastUploadedAt: rows[0]?.updatedAt ?? null,
    note: rows.length > 0 ? '显示插件上传候选编码记录。' : profession.note
  };
}));
const intelligentPluginFieldStandardRows = computed(() => fetchIntelligentPluginFieldStandardRows());
const intelligentPluginFieldStandardPreview = computed(() => intelligentPluginFieldStandardRows.value.slice(0, 24));
const componentStandardIndexRows = computed(() => (codeStandard.value?.componentStandards ?? []).map((row, index) => ({
  ...row,
  index: String(index + 1).padStart(2, '0')
})));
const governanceMap = computed(() => [
  {
    title: '标准治理',
    status: '智能化草案',
    tagType: 'success',
    object: `治理 ${codeRuleCount.value} 类编码规则、${componentStandardCount.value} 类智能化构件、${propertyRuleCount.value} 个候选字段规则。`,
    outcome: '输出平台校验约束，字段值不在当前阶段冻结。'
  },
  {
    title: '插件契约',
    status: '待真实样本',
    tagType: 'primary',
    object: `治理 ${intelligentPluginFieldStandardRows.value.length} 行候选上传字段和建议 Revit 参数名。`,
    outcome: '插件上传候选编码和值证据，平台再校验确认。'
  },
  {
    title: '上传编码',
    status: '平台管理',
    tagType: 'warning',
    object: `治理 ${uploadedCodeRecordSummary.value.total} 条上传编码记录及其模型来源。`,
    outcome: '支持增删改查、重复判断、待确认和冲突处理。'
  },
  {
    title: '质量闭环',
    status: '问题可见',
    tagType: 'danger',
    object: `治理 ${qualityIssueRows.value.length} 类编码、字段、版本或证据问题。`,
    outcome: '问题进入整改工单，回填后复核归档。'
  }
]);
const governanceSteps = computed(() => [
  { title: '规则维护', object: '全专业公共约束 + 智能化草案', result: '平台发布校验口径' },
  { title: '插件注入', object: 'Revit 参数和值包', result: '生成候选编码' },
  { title: '数据上传', object: '候选编码、模型指纹、Revit 标识', result: '形成上传记录' },
  { title: '质量校验', object: '格式、专业、重复、未知码段', result: '发现问题' },
  { title: '整改归档', object: '工单回填、复核记录、归档摘要', result: '形成闭环证据' }
]);
const overviewEntrypoints = [
  {
    label: '看规则',
    title: '编码标准中心',
    description: '查看附件来源、标准分层、编码规则、系统组和构件字段库。',
    path: '/bim-submission/code-center'
  },
  {
    label: '给插件',
    title: '插件契约中心',
    description: '查看插件候选上传字段，并导出草案 CSV 给插件团队。',
    path: '/bim-submission/plugin-contract'
  },
  {
    label: '查构件',
    title: '数据中心',
    description: '按专业查看插件上传构件、模型来源、Revit 标识和编码校验。',
    path: '/bim-submission/data-center'
  },
  {
    label: '看问题',
    title: '质量校验',
    description: '集中查看编码重复、字段缺失、类型待复核和版本差异。',
    path: '/bim-submission/quality'
  }
];
const candidatePluginUploadFields = [
  {
    name: '深圳构件标识',
    parameter: 'DD_SZ_ComponentId',
    constraint: '取自附件三对象名称列，按最细分类填写',
    fillMode: '插件按构件类型写入',
    note: '用于识别构件归属的深圳标准名称'
  },
  {
    name: '深圳系统标识',
    parameter: 'DD_SZ_SystemId',
    constraint: '取自附件三系统表对象名称列，按最细分类填写',
    fillMode: '插件按系统归属写入',
    note: '用于识别系统归属和跨专业查询'
  },
  {
    name: '位号编码',
    parameter: 'DD_TagCode',
    constraint: '按附件十一 Ctower 位号编码规则生成',
    fillMode: '插件按平台编码规则生成',
    note: '以 Ctower 为前缀，包含单元码、系统码、设备码'
  },
  {
    name: '构件编码',
    parameter: 'DD_ComponentCode',
    constraint: '平台构件唯一编码，后端落库前保持契约语义',
    fillMode: '插件生成候选，平台校验唯一',
    note: '用于平台查询、链接、工单和归档'
  },
  {
    name: '专业编码',
    parameter: 'DD_Discipline',
    constraint: '按平台专业枚举写入，当前智能化为首个专业包',
    fillMode: '插件按当前专业写入',
    note: '智能化首版建议写入 intelligent'
  },
  {
    name: '三级系统分类',
    parameter: 'DD_SystemLevel3',
    constraint: '按附件三系统分类字段取值，需与系统表一致',
    fillMode: '插件读取或建模方确认',
    note: '需与附件三系统分类保持一致'
  },
  {
    name: '型号规格',
    parameter: 'DD_ModelSpec',
    constraint: '按附件三属性名称和值严格填写',
    fillMode: '优先从族类型读取',
    note: '模型缺失时由建模方补录'
  },
  {
    name: '编号',
    parameter: 'DD_Number',
    constraint: '按附件三属性名称和值严格填写',
    fillMode: '读取模型或人工确认',
    note: '柜、箱、主机等设备常用'
  },
  {
    name: '电源参数',
    parameter: 'DD_Power',
    constraint: '按附件三属性名称和值严格填写，符号使用英文半角',
    fillMode: '读取模型或人工确认',
    note: '带电设备需要，需使用英文半角符号'
  },
  {
    name: '额定功率',
    parameter: 'DD_RatedPower',
    constraint: '按附件三 double 类型处理，单位单独保存',
    fillMode: '读取模型或人工确认',
    note: 'double 数值字段，单位单独保存'
  }
];
const qualityIssueRows = computed(() => {
  const uploadedIssues = uploadedCodeRecords.value.flatMap((record) => record.validationIssues.map((issue) => ({
    issueType: uploadedCodeIssueTypeLabel(issue.type),
    target: record.componentName || record.componentType || '上传编码记录',
    componentCode: record.rawCodeValue || '-',
    standard: record.codeType,
    reason: issue.message,
    severity: issue.type === 'DUPLICATE' ? '严重' : '预警'
  })));
  if (uploadedIssues.length > 0 || !showSampleUploadedCodeRecords.value) {
    return uploadedIssues;
  }
  const rows = dataCenterComponents.value;
  const conflict = rows.find((row) => row.codeCandidateStatus === 'CONFLICT');
  const missing = rows.find((row) => row.missingFieldCount > 0);
  const typeWarning = rows.find((row) => row.codeCandidateStatus === 'WARNING');
  const versionWarning = rows.find((row) => row.standardVersion === 'INTEL-V0.9-DRAFT');
  return [
    conflict && {
      issueType: '编码重复',
      target: conflict.componentName,
      componentCode: conflict.componentCode,
      standard: '平台编码唯一性规则',
      reason: conflict.codeConflictReason ?? '候选编码重复。',
      severity: '严重'
    },
    missing && {
      issueType: '候选字段缺失',
      target: missing.componentName,
      componentCode: missing.componentCode,
      standard: '附件三字段规则',
      reason: '候选字段为空，需要建模方在 Revit 模型中补录后重新上传。',
      severity: '严重'
    },
    typeWarning && {
      issueType: '字段类型待复核',
      target: typeWarning.componentName,
      componentCode: typeWarning.componentCode,
      standard: '附件三 string/double/enum 处理规则',
      reason: '字段值可以上传，但类型或枚举值仍需管理方复核。',
      severity: '预警'
    },
    versionWarning && {
      issueType: '标准版本差异',
      target: versionWarning.componentName,
      componentCode: versionWarning.componentCode,
      standard: '智能化专业标准包版本',
      reason: '旧批次仍使用 INTEL-V0.9-DRAFT，归档时需要保留版本追踪。',
      severity: '预警'
    }
  ].filter(Boolean) as Array<{
    issueType: string;
    target: string;
    componentCode: string;
    standard: string;
    reason: string;
    severity: string;
  }>;
});
const dataCenterProfessionGroups = computed(() => (dataCenter.value?.professionSummaries ?? [])
  .filter((profession) => profession.code !== 'all')
  .map((profession) => ({
    ...profession,
    components: profession.status === 'RECORDED'
      ? dataCenterComponents.value.filter((component) => component.professionCode === profession.code)
      : []
  })));
const filteredComponentStandards = computed(() => {
  const keyword = componentStandardKeyword.value.trim().toLowerCase();
  const rows = codeStandard.value?.componentStandards ?? [];
  if (!keyword) return rows;
  return rows.filter((row) => [
    row.sourceSheet,
    row.sourceObjectPath,
    row.shenzhenComponentIdentifier,
    row.intelligentMappingName,
    ...row.sampleFields
  ].some((item) => item.toLowerCase().includes(keyword)));
});
const filteredPropertyRules = computed(() => {
  const keyword = propertyKeyword.value.trim().toLowerCase();
  const rows = codeStandard.value?.propertyRules ?? [];
  if (!keyword) return rows;
  return rows.filter((row) => [
    row.fieldName,
    row.valueType,
    row.unit,
    row.valueRequirement,
    row.remark,
    row.sourceSheet,
    row.mappingScope,
    row.requiredPolicy
  ].some((item) => item.toLowerCase().includes(keyword)));
});

watch(projectId, () => {
  loadPage();
});

onMounted(() => {
  loadPage();
});

async function loadPage() {
  codeStandard.value = fetchBimCodeStandardOverview();
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [overviewResult, dataCenterResult] = await Promise.all([
      fetchBimSubmissionOverview(projectId.value),
      fetchBimDataCenterOverview(projectId.value, selectedDataCenterProfession.value)
    ]);
    overview.value = overviewResult;
    dataCenter.value = dataCenterResult;
    dataCenterComponents.value = showSampleUploadedCodeRecords.value ? dataCenterResult.components : [];
    await loadUploadedCodeRecords();
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    loading.value = false;
  }
}

async function loadDataCenter() {
  if (!projectId.value) return;
  dataCenterLoading.value = true;
  try {
    const result = await fetchBimDataCenterOverview(projectId.value, selectedDataCenterProfession.value);
    dataCenter.value = result;
    dataCenterComponents.value = showSampleUploadedCodeRecords.value ? result.components : [];
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    dataCenterLoading.value = false;
  }
}

async function loadDataCenterComponents() {
  if (!projectId.value) return;
  if (!showSampleUploadedCodeRecords.value) {
    dataCenterComponents.value = [];
    return;
  }
  dataCenterLoading.value = true;
  try {
    dataCenterComponents.value = await fetchBimDataCenterComponents(projectId.value, {
      professionCode: selectedDataCenterProfession.value,
      batchId: dataCenterQuery.batchId,
      keyword: dataCenterQuery.keyword || undefined,
      qualityStatus: dataCenterQuery.qualityStatus || undefined,
      limit: 100
    });
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    dataCenterLoading.value = false;
  }
}

async function loadUploadedCodeRecords() {
  if (!projectId.value) return;
  dataCenterLoading.value = true;
  try {
    uploadedCodeRecords.value = await fetchBimUploadedCodeRecords(projectId.value, {
      professionCode: selectedDataCenterProfession.value,
      keyword: uploadedCodeQuery.keyword || undefined,
      validationStatus: uploadedCodeQuery.validationStatus || undefined,
      includeSamples: showSampleUploadedCodeRecords.value
    });
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    dataCenterLoading.value = false;
  }
}

async function selectDataCenterProfession(code: string) {
  selectedDataCenterProfession.value = code;
  dataCenterQuery.batchId = undefined;
  dataCenterQuery.keyword = '';
  dataCenterQuery.qualityStatus = '';
  uploadedCodeQuery.keyword = '';
  uploadedCodeQuery.validationStatus = '';
  await loadDataCenter();
  await loadUploadedCodeRecords();
}

async function handleSampleRecordToggle() {
  await loadDataCenter();
  await loadUploadedCodeRecords();
}

function resetUploadedCodeForm() {
  Object.assign(uploadedCodeForm, {
    professionCode: selectedDataCenterProfession.value === 'all' ? 'intelligent' : selectedDataCenterProfession.value,
    professionName: selectedDataCenterProfession.value === 'all'
      ? '智能化'
      : dataCenterProfessionCards.value.find((item) => item.code === selectedDataCenterProfession.value)?.name ?? '智能化',
    componentType: '',
    componentName: '',
    codeType: '位号编码',
    rawCodeValue: '',
    sourceParameterName: 'DD_TagCode',
    modelFingerprint: '',
    revitElementId: '',
    revitUniqueId: '',
    batchNo: '',
    standardPackageStatus: 'DRAFT'
  } satisfies BimUploadedCodeRecordPayload);
}

function openUploadedCodeDialog(row?: BimUploadedCodeRecord) {
  editingUploadedCodeRecord.value = row ?? null;
  if (row) {
    Object.assign(uploadedCodeForm, {
      professionCode: row.professionCode,
      professionName: row.professionName,
      componentType: row.componentType,
      componentName: row.componentName,
      codeType: row.codeType,
      rawCodeValue: row.rawCodeValue,
      sourceParameterName: row.sourceParameterName,
      modelFingerprint: row.modelFingerprint,
      revitElementId: row.revitElementId,
      revitUniqueId: row.revitUniqueId,
      batchNo: row.batchNo,
      standardPackageStatus: row.standardPackageStatus
    } satisfies BimUploadedCodeRecordPayload);
  } else {
    resetUploadedCodeForm();
  }
  uploadedCodeDialogVisible.value = true;
}

function syncUploadedProfessionName() {
  const names: Record<string, string> = {
    intelligent: '智能化',
    architecture: '建筑',
    structure: '结构',
    plumbing: '给排水',
    hvac: '暖通',
    electrical: '电气'
  };
  uploadedCodeForm.professionName = names[uploadedCodeForm.professionCode] ?? uploadedCodeForm.professionCode;
}

async function submitUploadedCodeRecord() {
  if (!projectId.value) return;
  if (!uploadedCodeForm.componentName.trim() || !uploadedCodeForm.codeType.trim()) {
    ElMessage.warning('请填写构件名称和编码类型');
    return;
  }
  saving.value = true;
  syncUploadedProfessionName();
  try {
    if (editingUploadedCodeRecord.value) {
      await updateBimUploadedCodeRecord(projectId.value, editingUploadedCodeRecord.value.id, { ...uploadedCodeForm });
      ElMessage.success('上传编码记录已更新并校验');
    } else {
      await createBimUploadedCodeRecord(projectId.value, { ...uploadedCodeForm });
      ElMessage.success('上传编码记录已新增并校验');
    }
    uploadedCodeDialogVisible.value = false;
    editingUploadedCodeRecord.value = null;
    await loadUploadedCodeRecords();
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    saving.value = false;
  }
}

async function deleteUploadedRecord(row: BimUploadedCodeRecord) {
  if (!projectId.value) return;
  try {
    await ElMessageBox.confirm('确认删除这条本地上传编码记录？', '删除记录', { type: 'warning' });
    await deleteBimUploadedCodeRecord(projectId.value, row.id);
    ElMessage.success('上传编码记录已删除');
    await loadUploadedCodeRecords();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  }
}

async function revalidateUploadedRecords() {
  if (!projectId.value) return;
  try {
    await revalidateBimUploadedCodeRecords(projectId.value);
    await loadUploadedCodeRecords();
    ElMessage.success('已按当前平台约束重新校验');
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

async function publishContract() {
  if (!projectId.value) return;
  saving.value = true;
  try {
    await publishBimSubmissionContract(projectId.value);
    ElMessage.success('契约已发布');
    await loadPage();
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    saving.value = false;
  }
}

async function submitBatch() {
  if (!projectId.value || !batchForm.batchName.trim()) {
    ElMessage.warning('请填写批次名称');
    return;
  }
  saving.value = true;
  try {
    await createBimSubmissionBatch(projectId.value, batchForm.batchName.trim());
    batchDialogVisible.value = false;
    batchForm.batchName = '';
    ElMessage.success('批次已创建');
    await loadPage();
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    saving.value = false;
  }
}

async function openDataCenterComponent(row: BimDataCenterComponent) {
  if (!projectId.value) return;
  dataCenterDetailDrawerVisible.value = true;
  dataCenterDetail.value = null;
  try {
    dataCenterDetail.value = await fetchBimDataCenterComponentDetail(projectId.value, row.id);
  } catch (error) {
    ElMessage.error((error as Error).message);
  }
}

function openWorkOrder(row: BimDataCenterComponent) {
  selectedComponent.value = row;
  workOrderForm.fieldCode = '深圳构件标识';
  workOrderForm.title = `${row.componentName || row.componentCode || '构件'} 字段整改`;
  workOrderForm.requestedValue = '';
  workOrderForm.description = '';
  workOrderDialogVisible.value = true;
}

async function submitWorkOrder() {
  if (!projectId.value || !selectedComponent.value) return;
  if (!workOrderForm.fieldCode.trim() || !workOrderForm.title.trim()) {
    ElMessage.warning('请填写字段编码和工单标题');
    return;
  }
  saving.value = true;
  try {
    await createBimSubmissionWorkOrder(projectId.value, selectedComponent.value.batchId, {
      componentId: selectedComponent.value.id,
      fieldCode: workOrderForm.fieldCode.trim(),
      title: workOrderForm.title.trim(),
      requestedValue: workOrderForm.requestedValue.trim(),
      description: workOrderForm.description.trim(),
      priority: 'NORMAL'
    });
    workOrderDialogVisible.value = false;
    ElMessage.success('整改工单已派发');
    await loadPage();
  } catch (error) {
    ElMessage.error((error as Error).message);
  } finally {
    saving.value = false;
  }
}

async function closeWorkOrder(row: BimSubmissionWorkOrder) {
  if (!projectId.value) return;
  try {
    await ElMessageBox.confirm('确认关闭该整改工单？', '关闭工单', { type: 'warning' });
    await closeBimSubmissionWorkOrder(projectId.value, row.id, '管理方复核通过');
    ElMessage.success('工单已关闭');
    await loadPage();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  }
}

async function archiveBatch(row: BimSubmissionBatch) {
  if (!projectId.value) return;
  try {
    await ElMessageBox.confirm('确认生成该批次的归档包？未关闭工单会阻止归档。', '生成归档包', { type: 'warning' });
    await archiveBimSubmissionBatch(projectId.value, row.id);
    ElMessage.success('归档包已生成');
    await loadPage();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
  }
}

function batchStatusLabel(status: string) {
  const labels: Record<string, string> = {
    ISSUED: '已下发',
    UPLOADING: '上传中',
    SUBMITTED: '已提交',
    REVIEWING: '审查中',
    RECTIFICATION: '整改中',
    CLOSED: '已归档'
  };
  return labels[status] ?? status;
}

function batchStatusTag(status: string) {
  if (status === 'CLOSED') return 'success';
  if (status === 'RECTIFICATION') return 'warning';
  if (status === 'SUBMITTED' || status === 'REVIEWING') return 'primary';
  return 'info';
}

function qualityStatusLabel(status: string) {
  const labels: Record<string, string> = {
    PASSED: '通过',
    WARNING: '预警',
    MISSING: '缺失'
  };
  return labels[status] ?? status;
}

function qualityStatusTag(status: string) {
  if (status === 'PASSED') return 'success';
  if (status === 'WARNING') return 'warning';
  if (status === 'MISSING') return 'danger';
  return 'info';
}

function dataCenterQualityStatusLabel(status: BimDataCenterQualityStatus) {
  return qualityStatusLabel(status);
}

function dataCenterQualityStatusTag(status: BimDataCenterQualityStatus) {
  return qualityStatusTag(status);
}

function dataCenterProfessionStatusLabel(status: BimDataCenterProfessionStatus) {
  const labels: Record<BimDataCenterProfessionStatus, string> = {
    RECORDED: '已接入',
    PENDING: '待接入'
  };
  return labels[status];
}

function dataCenterProfessionStatusTag(status: BimDataCenterProfessionStatus) {
  return status === 'RECORDED' ? 'success' : 'info';
}

function standardScopeLabel(scope: StandardScope) {
  const labels: Record<StandardScope, string> = {
    GLOBAL: '全专业公共',
    PROFESSION: '专业标准包'
  };
  return labels[scope];
}

function standardScopeTag(scope: StandardScope) {
  return scope === 'GLOBAL' ? 'primary' : 'success';
}

function standardPackageStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    DRAFT: '草案/待确认',
    PENDING_CONFIRMATION: '待确认',
    ACTIVE: '已启用',
    PENDING: '待录入'
  };
  return status ? labels[status] ?? status : '草案/待确认';
}

function codeCandidateStatusLabel(status?: BimCodeCandidateStatus) {
  const labels: Record<BimCodeCandidateStatus, string> = {
    PASSED: '编码通过',
    WARNING: '待复核',
    CONFLICT: '编码冲突',
    PENDING_CONFIRMATION: '待确认'
  };
  return status ? labels[status] : '待校验';
}

function codeCandidateStatusTag(status?: BimCodeCandidateStatus) {
  if (status === 'PASSED') return 'success';
  if (status === 'WARNING') return 'warning';
  if (status === 'CONFLICT') return 'danger';
  return 'info';
}

function uploadedCodeStatusLabel(status: BimUploadedCodeStatus) {
  const labels: Record<BimUploadedCodeStatus, string> = {
    PASSED: '通过',
    WARNING: '预警',
    CONFLICT: '冲突',
    PENDING_CONFIRMATION: '待确认'
  };
  return labels[status];
}

function uploadedCodeStatusTag(status: BimUploadedCodeStatus) {
  if (status === 'PASSED') return 'success';
  if (status === 'WARNING') return 'warning';
  if (status === 'CONFLICT') return 'danger';
  return 'info';
}

function validationIssueSummary(issues: BimUploadedCodeRecord['validationIssues']) {
  if (issues.length === 0) return '无';
  return issues.map((issue) => issue.message).join('；');
}

function uploadedCodeIssueTypeLabel(type: BimUploadedCodeRecord['validationIssues'][number]['type']) {
  const labels: Record<BimUploadedCodeRecord['validationIssues'][number]['type'], string> = {
    FORMAT: '格式不符',
    PROFESSION: '专业未接入',
    COMPONENT_TYPE: '构件类型缺失',
    MISSING_FIELD: '上传值为空',
    DUPLICATE: '编码重复',
    UNKNOWN_SEGMENT: '未知编码类型',
    PENDING_RULE: '规则待确认'
  };
  return labels[type];
}

function previewStatusLabel(status?: BimPreviewStatus) {
  const labels: Record<BimPreviewStatus, string> = {
    METADATA_ONLY: '元数据预览',
    READY: '可定位预览',
    PENDING_MODEL_VIEWER: '待接模型预览'
  };
  return status ? labels[status] : '待接入';
}

function completionProgressStatus(status: BimDataCenterQualityStatus) {
  if (status === 'PASSED') return 'success';
  if (status === 'WARNING') return 'warning';
  return 'exception';
}

function pluginFieldPolicyTag(policy: string) {
  if (policy === '必填') return 'danger';
  if (policy === '选填') return 'success';
  return 'warning';
}

function sourceStatusLabel(status: StandardSourceStatus) {
  const labels: Record<StandardSourceStatus, string> = {
    ACTIVE: '已接入',
    FALLBACK_PENDING: '待接入',
    REFERENCE: '参考'
  };
  return labels[status];
}

function standardSourceTag(status: StandardSourceStatus) {
  if (status === 'ACTIVE') return 'success';
  if (status === 'FALLBACK_PENDING') return 'warning';
  return 'info';
}

function professionStatusLabel(status: ProfessionStatusCode) {
  const labels: Record<ProfessionStatusCode, string> = {
    RECORDED: '已录入',
    PENDING: '待录入'
  };
  return labels[status];
}

function professionStatusTag(status: ProfessionStatusCode) {
  return status === 'RECORDED' ? 'success' : 'info';
}

function validationStatusLabel(status: ValidationStatus) {
  const labels: Record<ValidationStatus, string> = {
    PASSED: '通过',
    WARNING: '预警',
    FAILED: '失败'
  };
  return labels[status];
}

function validationStatusTag(status: ValidationStatus) {
  if (status === 'PASSED') return 'success';
  if (status === 'WARNING') return 'warning';
  return 'danger';
}

function fieldList(fields: string[]) {
  return fields.length > 0 ? fields.join('、') : '按父级继承或待确认';
}

function csvCell(value: string | number | null | undefined) {
  const text = String(value ?? '');
  return `"${text.replace(/"/g, '""')}"`;
}

function exportIntelligentPluginStandardCsv() {
  const headers: Array<keyof IntelligentPluginFieldStandardRow> = [
    'profession',
    'sourceSheet',
    'sourceRow',
    'sourceObjectPath',
    'shenzhenComponentIdentifier',
    'intelligentMappingName',
    'fieldName',
    'requiredPolicy',
    'valueType',
    'unit',
    'valueRequirement',
    'suggestedRevitParameterName',
    'standardSource',
    'remark'
  ];
  const headerLabels = [
    '专业',
    '来源页签',
    '来源行',
    '构件对象层级',
    '深圳构件标识',
    '智能化映射名称',
    '字段名',
    '策略状态',
    '类型',
    '单位',
    '取值要求',
    '建议 Revit 参数名',
    '来源标准',
    '备注'
  ];
  const lines = [
    headerLabels.map(csvCell).join(','),
    ...intelligentPluginFieldStandardRows.value.map((row) => headers.map((key) => csvCell(row[key])).join(','))
  ];
  const blob = new Blob([`\uFEFF${lines.join('\n')}`], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'C塔BIM报建_智能化插件上传建议表_草案待确认.csv';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  ElMessage.success('智能化上传建议 CSV 已导出');
}

function workOrderStatusLabel(status: string) {
  const labels: Record<string, string> = {
    OPEN: '待整改',
    RETURNED: '已退回',
    RESOLVED: '待复核',
    CLOSED: '已关闭'
  };
  return labels[status] ?? status;
}

function workOrderStatusTag(status: string) {
  if (status === 'CLOSED') return 'success';
  if (status === 'RESOLVED') return 'primary';
  return 'warning';
}

function formatDate(value?: string | null) {
  if (!value) return '-';
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

function prettyJson(value?: string | Record<string, unknown> | null) {
  if (!value) return '暂无原始 Revit 参数';
  if (typeof value !== 'string') {
    return JSON.stringify(value, null, 2);
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function summaryText(value?: string | null) {
  if (!value) return '-';
  try {
    const parsed = JSON.parse(value);
    return Object.entries(parsed).map(([key, item]) => `${key}: ${item}`).join('，');
  } catch {
    return value;
  }
}
</script>

<style scoped>
.bim-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.bim-toolbar,
.bim-overview-band,
.bim-contract,
.bim-panel {
  background: var(--zy-surface);
  border: 1px solid var(--zy-border);
  border-radius: 8px;
  box-shadow: var(--zy-shadow-sm);
}

.bim-toolbar {
  align-items: flex-start;
  display: flex;
  gap: 18px;
  justify-content: space-between;
  padding: 20px;
}

.bim-toolbar h1 {
  color: var(--zy-text);
  font-size: var(--zy-fs-2xl);
  line-height: var(--zy-lh-tight);
  margin: 4px 0 8px;
}

.bim-toolbar p,
.bim-section-title span,
.bim-main-cell span,
.bim-stat em,
.bim-data-summary em,
.bim-contract__body span,
.bim-detail-head span,
.bim-evidence-grid span,
.bim-link-grid span,
.bim-field-coverage span {
  color: var(--zy-text-muted);
  font-size: var(--zy-fs-sm);
}

.bim-toolbar__eyebrow {
  color: var(--zy-primary);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

.bim-toolbar__actions,
.bim-filter-row,
.bim-record-toolbar,
.bim-record-toolbar__actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.bim-record-toolbar {
  justify-content: space-between;
  margin-bottom: 12px;
}

.bim-uploaded-code-form {
  display: grid;
  gap: 0 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.bim-stat-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.bim-data-summary-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.bim-data-group-list {
  display: grid;
  gap: 18px;
}

.bim-stat,
.bim-data-summary {
  background: var(--zy-surface);
  border: 1px solid var(--zy-border);
  border-radius: 8px;
  box-shadow: var(--zy-shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 104px;
  padding: 16px;
}

.bim-stat span,
.bim-data-summary span,
.bim-contract__body span {
  color: var(--zy-text-muted);
  font-size: var(--zy-fs-xs);
}

.bim-stat strong,
.bim-data-summary strong {
  color: var(--zy-text);
  font-size: var(--zy-fs-3xl);
  line-height: var(--zy-lh-tight);
}

.bim-data-summary__date {
  font-size: var(--zy-fs-md) !important;
  line-height: var(--zy-lh-normal) !important;
}

.bim-contract,
.bim-panel,
.bim-overview-band {
  padding: 18px;
}

.bim-section-title {
  align-items: baseline;
  display: flex;
  gap: 12px;
  justify-content: space-between;
  margin-bottom: 14px;
}

.bim-section-title h2 {
  color: var(--zy-text);
  font-size: var(--zy-fs-lg);
  margin: 0;
}

.bim-info-dot {
  align-items: center;
  background: var(--zy-surface-soft);
  border: 1px solid var(--zy-border);
  border-radius: 999px;
  color: var(--zy-text-muted);
  cursor: help;
  display: inline-flex;
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
  height: 22px;
  justify-content: center;
  padding: 0;
  width: 22px;
}

.bim-contract__body {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.bim-contract__body div {
  background: var(--zy-surface-soft);
  border: 1px solid var(--zy-border);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
}

.bim-contract__body strong,
.bim-main-cell strong,
.bim-detail-head strong {
  color: var(--zy-text);
  font-weight: var(--zy-fw-semi);
}

.bim-tabs {
  background: transparent;
}

.bim-filter-row {
  margin-bottom: 12px;
}

.bim-source-grid,
.bim-profession-grid,
.bim-standard-layer-grid,
.bim-package-list,
.bim-governance-map,
.bim-flow-strip,
.bim-contract-split,
.bim-module-entry-list,
.bim-field-policy-grid,
.bim-required-field-list,
.bim-quality-grid {
  display: grid;
  gap: 12px;
}

.bim-standard-export {
  align-items: center;
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  margin-bottom: 14px;
}

.bim-standard-export div {
  background: var(--zy-surface-soft);
  border: 1px solid var(--zy-border);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  padding: 10px 12px;
}

.bim-source-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.bim-source,
.bim-profession,
.bim-data-profession,
.bim-code-rule,
.bim-validation-list article,
.bim-standard-layer,
.bim-package,
.bim-governance-card,
.bim-module-entry,
.bim-contract-split article,
.bim-field-policy-grid article,
.bim-required-field-list article,
.bim-quality-grid article {
  background: var(--zy-surface-soft);
  border: 1px solid var(--zy-border);
  border-radius: 8px;
}

.bim-source,
.bim-standard-layer,
.bim-package,
.bim-governance-card,
.bim-module-entry,
.bim-contract-split article,
.bim-field-policy-grid article,
.bim-required-field-list article,
.bim-quality-grid article,
.bim-flow-strip article {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding: 14px;
}

.bim-source__head,
.bim-code-rule__main,
.bim-validation-list article div,
.bim-standard-layer div,
.bim-package div:first-child,
.bim-governance-card div {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.bim-source strong,
.bim-profession strong,
.bim-data-profession strong,
.bim-standard-export strong,
.bim-code-rule strong,
.bim-code-segments strong,
.bim-validation-list strong,
.bim-evidence-grid strong,
.bim-standard-layer strong,
.bim-package strong,
.bim-policy-card strong,
.bim-link-grid strong,
.bim-governance-card strong,
.bim-status-list strong,
.bim-module-entry strong,
.bim-flow-strip strong,
.bim-contract-split strong,
.bim-field-policy-grid strong,
.bim-required-field-list strong,
.bim-quality-grid strong {
  color: var(--zy-text);
}

.bim-source span,
.bim-source small,
.bim-source p,
.bim-profession span,
.bim-profession small,
.bim-data-profession span,
.bim-data-profession small,
.bim-standard-export span,
.bim-code-rule span,
.bim-code-rule p,
.bim-code-rule small,
.bim-code-segments span,
.bim-code-segments small,
.bim-validation-list p,
.bim-standard-layer span,
.bim-standard-layer small,
.bim-standard-layer p,
.bim-package span,
.bim-package small,
.bim-package p,
.bim-policy-card span,
.bim-policy-card p,
.bim-preview-note,
.bim-governance-card p,
.bim-governance-card small,
.bim-status-list span,
.bim-module-entry span,
.bim-module-entry small,
.bim-flow-strip span,
.bim-flow-strip small,
.bim-contract-split span,
.bim-field-policy-grid span,
.bim-field-policy-grid small,
.bim-required-field-list span,
.bim-required-field-list small,
.bim-quality-grid span,
.bim-quality-grid small {
  color: var(--zy-text-muted);
  font-size: var(--zy-fs-sm);
}

.bim-source p,
.bim-code-rule p,
.bim-code-rule small,
.bim-validation-list p,
.bim-standard-layer p,
.bim-package p,
.bim-policy-card p,
.bim-preview-note,
.bim-governance-card p {
  line-height: var(--zy-lh-relaxed);
  margin: 0;
}

.bim-governance-map {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.bim-status-list,
.bim-module-entry-list {
  display: grid;
  gap: 10px;
}

.bim-status-list div {
  align-items: center;
  border-bottom: 1px solid var(--zy-border);
  display: grid;
  gap: 5px;
  grid-template-columns: 110px minmax(0, 1fr) auto;
  padding: 0 0 11px;
}

.bim-status-list div:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.bim-module-entry {
  color: inherit;
  min-height: 96px;
  text-decoration: none;
  transition:
    background-color var(--zy-duration-2) var(--zy-ease-out),
    border-color var(--zy-duration-2) var(--zy-ease-out);
}

.bim-module-entry:hover {
  background: var(--zy-surface);
  border-color: var(--zy-primary);
}

.bim-flow-strip {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.bim-flow-strip article {
  background: var(--zy-surface-soft);
  border: 1px solid var(--zy-border);
  border-radius: 8px;
  min-height: 126px;
}

.bim-contract-split,
.bim-field-policy-grid,
.bim-required-field-list,
.bim-quality-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.bim-field-policy-grid,
.bim-quality-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.bim-required-field-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.bim-required-field-list article {
  gap: 7px;
}

.bim-required-field-list article div {
  align-items: center;
  display: flex;
  gap: 10px;
  justify-content: space-between;
}

.bim-required-field-list code {
  background: var(--zy-surface);
  border: 1px solid var(--zy-border);
  border-radius: 6px;
  color: var(--zy-primary);
  font-size: var(--zy-fs-xs);
  padding: 4px 7px;
  white-space: nowrap;
}

.bim-helper-note {
  color: var(--zy-text-muted);
  font-size: var(--zy-fs-sm);
  line-height: var(--zy-lh-relaxed);
  margin: 12px 0 0;
}

.bim-field-policy-grid strong,
.bim-quality-grid strong {
  font-size: var(--zy-fs-3xl);
  line-height: var(--zy-lh-tight);
}

.bim-standard-layer-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.bim-package-list {
  grid-template-columns: 1fr;
}

.bim-package__stats {
  display: grid !important;
  gap: 8px !important;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  justify-content: stretch !important;
}

.bim-package__stats small {
  background: var(--zy-surface);
  border: 1px solid var(--zy-border);
  border-radius: 6px;
  padding: 7px 8px;
}

.bim-policy-card {
  display: grid;
  gap: 12px;
}

.bim-compact-collapse {
  border: 0;
}

.bim-policy-card--compact {
  background: var(--zy-surface-soft);
  border: 1px solid var(--zy-border);
  border-radius: 8px;
  padding: 14px;
}

.bim-policy-card > div:not(.bim-evidence-tags) {
  background: var(--zy-surface-soft);
  border: 1px solid var(--zy-border);
  border-radius: 6px;
  display: grid;
  gap: 5px;
  padding: 10px 12px;
}

.bim-evidence-tags,
.bim-code-rule__meta,
.bim-field-coverage {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.bim-standard-status-row {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.bim-standard-status-row div {
  background: var(--zy-surface-soft);
  border: 1px solid var(--zy-border);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  padding: 12px;
}

.bim-standard-status-row span {
  color: var(--zy-text-muted);
  font-size: var(--zy-fs-xs);
}

.bim-standard-status-row strong {
  color: var(--zy-text);
  font-size: var(--zy-fs-lg);
}

.bim-pending-standard__meta {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.bim-profession-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.bim-profession-grid--compact .bim-profession {
  min-height: 82px;
}

.bim-profession {
  align-items: flex-start;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 132px;
  padding: 14px;
  text-align: left;
  transition:
    background-color var(--zy-duration-2) var(--zy-ease-out),
    border-color var(--zy-duration-2) var(--zy-ease-out);
}

.bim-data-professions {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.bim-data-profession {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-height: 118px;
  padding: 12px;
  text-align: left;
  transition:
    background-color var(--zy-duration-2) var(--zy-ease-out),
    border-color var(--zy-duration-2) var(--zy-ease-out);
}

.bim-data-profession div {
  align-items: center;
  display: flex;
  gap: 8px;
  justify-content: space-between;
}

.bim-profession:hover,
.bim-profession--active,
.bim-data-profession:hover,
.bim-data-profession--active {
  background: var(--zy-surface);
  border-color: var(--zy-primary);
}

.bim-code-rule-list,
.bim-validation-list {
  display: grid;
  gap: 12px;
}

.bim-code-rule {
  display: grid;
  gap: 14px;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.2fr);
  padding: 14px;
}

.bim-code-rule__main {
  flex-direction: column;
}

.bim-code-rule code,
.bim-validation-list code {
  background: var(--zy-surface);
  border: 1px solid var(--zy-border);
  border-radius: 6px;
  color: var(--zy-primary);
  display: inline-block;
  font-size: var(--zy-fs-xs);
  max-width: 100%;
  overflow-wrap: anywhere;
  padding: 5px 7px;
}

.bim-code-segments {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.bim-code-segments div {
  background: var(--zy-surface);
  border: 1px solid var(--zy-border);
  border-radius: 6px;
  display: grid;
  gap: 5px;
  min-width: 0;
  padding: 10px;
}

.bim-grid-two {
  align-items: start;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.8fr);
}

.bim-validation-list article {
  display: grid;
  gap: 8px;
  padding: 12px;
}

.bim-filter-row .el-input,
.bim-filter-row .el-select {
  max-width: 260px;
  min-width: 180px;
}

.bim-main-cell,
.bim-detail-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bim-table {
  width: 100%;
}

.bim-detail-head {
  border-bottom: 1px solid var(--zy-border);
  margin-bottom: 14px;
  padding-bottom: 12px;
}

.bim-detail-section {
  display: grid;
  gap: 12px;
  margin-bottom: 14px;
}

.bim-detail-section__title {
  display: grid;
  gap: 4px;
}

.bim-detail-section__title h3 {
  color: var(--zy-text);
  font-size: var(--zy-fs-md);
  margin: 0;
}

.bim-detail-section__title span {
  color: var(--zy-text-muted);
  font-size: var(--zy-fs-sm);
}

.bim-evidence-grid,
.bim-link-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 14px;
}

.bim-link-grid {
  margin-bottom: 0;
}

.bim-evidence-grid div,
.bim-link-grid div {
  background: var(--zy-surface-soft);
  border: 1px solid var(--zy-border);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
  padding: 10px;
}

.bim-evidence-grid strong,
.bim-link-grid strong {
  overflow-wrap: anywhere;
}

.bim-field-coverage {
  border-top: 1px solid var(--zy-border);
  padding-top: 10px;
}

.bim-json {
  background: var(--zy-surface-soft);
  border: 1px solid var(--zy-border);
  border-radius: 6px;
  color: var(--zy-text);
  font-size: var(--zy-fs-xs);
  line-height: var(--zy-lh-relaxed);
  margin: 14px 0 0;
  max-height: 260px;
  overflow: auto;
  padding: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1180px) {
  .bim-stat-grid,
  .bim-data-summary-grid,
  .bim-standard-export,
  .bim-contract__body,
  .bim-source-grid,
  .bim-standard-layer-grid,
  .bim-standard-status-row,
  .bim-governance-map,
  .bim-flow-strip,
  .bim-contract-split,
  .bim-field-policy-grid,
  .bim-required-field-list,
  .bim-quality-grid,
  .bim-profession-grid,
  .bim-data-professions,
  .bim-code-segments,
  .bim-package__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .bim-standard-export .el-button {
    grid-column: 1 / -1;
  }

  .bim-code-rule,
  .bim-grid-two {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .bim-toolbar,
  .bim-section-title,
  .bim-record-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .bim-stat-grid,
  .bim-data-summary-grid,
  .bim-standard-export,
  .bim-contract__body,
  .bim-source-grid,
  .bim-standard-layer-grid,
  .bim-standard-status-row,
  .bim-governance-map,
  .bim-flow-strip,
  .bim-contract-split,
  .bim-field-policy-grid,
  .bim-required-field-list,
  .bim-quality-grid,
  .bim-profession-grid,
  .bim-data-professions,
  .bim-evidence-grid,
  .bim-link-grid,
  .bim-code-segments {
    grid-template-columns: 1fr;
  }

  .bim-package__stats {
    grid-template-columns: 1fr;
  }

  .bim-filter-row .el-input,
  .bim-filter-row .el-select {
    max-width: none;
    width: 100%;
  }

  .bim-status-list div {
    align-items: flex-start;
    grid-template-columns: 1fr;
  }

  .bim-uploaded-code-form {
    grid-template-columns: 1fr;
  }
}
</style>
