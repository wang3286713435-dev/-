<template>
  <section class="mvp-page agent-preview-page">
    <div class="mvp-page__header">
      <div>
        <h1>Agent 预览</h1>
        <p>企业 Agent 可见范围预览与权限证明 — 本批只读，以下操作当前禁止：写库、移动文件、正文抽取、向量化、自动审批、自动删除、模型轻量化</p>
      </div>
    </div>

    <!-- Section 1: Catalog Sample -->
    <section class="agent-section">
      <h2>1. 资产目录样例</h2>
      <p>Agent 可通过只读接口查询以下资产元数据。可直接在样例文件上验证当前用户权限。</p>

      <div class="agent-filters">
        <el-select
          v-model="sampleProjectId"
          clearable
          filterable
          placeholder="选择项目查看样例"
          style="width: 280px"
          @change="loadSampleFiles"
        >
          <el-option
            v-for="p in projects"
            :key="p.projectId"
            :label="`${p.projectCode} ${p.projectName}`"
            :value="p.projectId"
          />
        </el-select>
        <el-button type="primary" :disabled="!sampleProjectId" @click="loadSampleFiles">加载样例</el-button>
      </div>

      <el-table v-loading="sampleLoading" :data="sampleFiles" class="master-table" empty-text="请选择项目加载样例">
        <el-table-column label="文件ID" width="90" align="right">
          <template #default="{ row }">{{ row.fileId }}</template>
        </el-table-column>
        <el-table-column label="文件名" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.fileName }}</template>
        </el-table-column>
        <el-table-column label="扩展名" width="80">
          <template #default="{ row }">
            <span :class="{ 'field-visible': row.agentReadable }">{{ row.fileExt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <span :class="{ 'field-visible': row.agentReadable }">{{ row.fileKind }}</span>
          </template>
        </el-table-column>
        <el-table-column label="专业" width="100">
          <template #default="{ row }">
            <span :class="{ 'field-visible': row.agentReadable }">{{ row.disciplineCode || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="100" align="right">
          <template #default="{ row }">
            <span :class="{ 'field-visible': row.agentReadable }">{{ formatBytes(row.sizeBytes) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="路径可见" width="90">
          <template #default="{ row }">
            <el-tag :type="row.storagePathVisible ? 'success' : 'info'" size="small">
              {{ row.storagePathVisible ? '可见' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :loading="proofLoading && proofFileId === row.fileId"
              @click="verifyPermissionFromRow(row)"
            >
              验证权限
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <!-- Section 2: Permission Proof -->
    <section class="agent-section">
      <h2>2. 权限证明</h2>
      <p>可从上方文件列表点击「验证权限」，也可手动输入文件 ID，查询当前用户是否可访问该文件。文件 ID 是平台为每个资产文件生成的唯一编号。</p>

      <div class="agent-filters">
        <el-input-number v-model="proofFileId" :min="1" placeholder="文件ID" style="width: 180px" />
        <el-button type="primary" :loading="proofLoading" :disabled="!proofFileId" @click="checkPermission">
          查询权限证明
        </el-button>
      </div>

      <div v-if="proofResult" class="permission-result-card">
        <div v-if="selectedProofTarget" class="proof-target-card">
          <h3>当前验证对象</h3>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="查询方式">
              {{ selectedProofTarget.source === 'table' ? '来自上方文件列表' : '手动输入' }}
            </el-descriptions-item>
            <el-descriptions-item label="文件ID">{{ selectedProofTarget.fileId }}</el-descriptions-item>
            <el-descriptions-item v-if="selectedProofTarget.source === 'table'" label="文件名">
              {{ selectedProofTarget.fileName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="selectedProofTarget.source === 'table'" label="项目编号">
              {{ selectedProofTarget.projectCode || '-' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="selectedProofTarget.source === 'table'" label="项目名称">
              {{ selectedProofTarget.projectName || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="permission-decision" :class="{ allowed: proofResult.allowed, denied: !proofResult.allowed }">
          <strong>{{ proofResult.decision === 'ALLOWED' ? '允许访问' : '拒绝访问' }}</strong>
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="决策">{{ proofResult.decision }}</el-descriptions-item>
          <el-descriptions-item label="操作者类型">{{ proofResult.actorType }}</el-descriptions-item>
          <el-descriptions-item label="项目范围">{{ proofResult.projectScope || '无' }}</el-descriptions-item>
          <el-descriptions-item label="原因码">{{ proofResult.reasonCode }}</el-descriptions-item>
          <el-descriptions-item label="原因说明">{{ proofResult.reasonText }}</el-descriptions-item>
          <el-descriptions-item label="追踪ID">
            <span class="trace-mono">{{ proofResult.traceId }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="检查时间">{{ formatDate(proofResult.checkedAt) }}</el-descriptions-item>
        </el-descriptions>
        <div class="evidence-list">
          <h4>证据</h4>
          <div v-for="e in proofResult.evidence" :key="e.type" class="evidence-item">
            <el-tag size="small">{{ e.label }}</el-tag>
            <span>{{ e.value }}</span>
            <el-tag v-if="e.sensitive" type="danger" size="small">敏感</el-tag>
          </div>
        </div>
      </div>
    </section>

    <!-- Section 3: Agent Visible Fields -->
    <section class="agent-section">
      <h2>3. Agent 可见字段合同</h2>
      <p>以下为 Agent 当前可通过只读接口获取的字段清单。未列出字段不可被 Agent 读取。</p>

      <div class="field-contract">
        <div class="field-contract-group">
          <h3>可见字段</h3>
          <el-tag v-for="f in visibleFields" :key="f" type="success" size="small" style="margin: 3px">{{ f }}</el-tag>
        </div>
        <div class="field-contract-group">
          <h3>脱敏/隐藏字段</h3>
          <el-tag v-for="f in hiddenFields" :key="f" type="info" size="small" style="margin: 3px">{{ f }}</el-tag>
        </div>
        <div class="field-contract-group">
          <h3>路径可见性规则</h3>
          <ul class="rule-list">
            <li>本机内部开发环境：管理员可见真实 NAS 路径</li>
            <li>普通项目用户：可查看授权项目内文件逻辑路径</li>
            <li>无项目权限用户：路径字段返回空或隐藏</li>
            <li>客户环境 / shared-dev / staging：默认隐藏真实 NAS 路径</li>
          </ul>
        </div>
      </div>
    </section>

    <!-- Section 4: Prohibited Actions -->
    <section class="agent-section">
      <h2>4. 禁止动作清单 — 本批只读</h2>
      <p>以下操作在本批次中明确禁止，不提供任何入口或能力：</p>

      <el-table :data="prohibitedActions" class="master-table" empty-text="">
        <el-table-column label="禁止动作" min-width="200">
          <template #default="{ row }">
            <span class="prohibited-action">{{ row.action }}</span>
          </template>
        </el-table-column>
        <el-table-column label="原因" min-width="300">
          <template #default="{ row }">{{ row.reason }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="">
            <el-tag type="danger" size="small">禁止</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-alert
        type="warning"
        title="本批次只读提示"
        description="当前页面不包含 Agent 写库、文件移动/删除、正文抽取、向量化、自动审批、自动治理或模型轻量化的真实动作入口。如需执行受控操作，请等待后续批次的安全设计和验收。"
        :closable="false"
        show-icon
        style="margin-top: 16px"
      />
    </section>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import {
  fetchCatalogProjects,
  fetchCatalogFiles,
  fetchFilePermissionProof,
  type CatalogProject,
  type CatalogFile,
  type PermissionProof
} from '@/modules/data-steward/api/dataSteward';

type ProofTarget = {
  fileId: number;
  source: 'table' | 'manual';
  fileName?: string;
  projectCode?: string;
  projectName?: string;
};

const projects = ref<CatalogProject[]>([]);
const sampleProjectId = ref<number | undefined>();
const sampleFiles = ref<CatalogFile[]>([]);
const sampleLoading = ref(false);

const proofFileId = ref<number | undefined>();
const proofResult = ref<PermissionProof | null>(null);
const proofLoading = ref(false);
const selectedProofTarget = ref<ProofTarget | null>(null);

watch(proofFileId, value => {
  if (!selectedProofTarget.value || selectedProofTarget.value.fileId === value) return;
  proofResult.value = null;
  selectedProofTarget.value = value ? { fileId: value, source: 'manual' } : null;
});

const visibleFields = [
  'fileName', 'fileExt', 'fileKind', 'disciplineCode', 'disciplineName',
  'version', 'sizeBytes', 'checksum', 'status', 'confidenceLevel',
  'storageProvider', 'logicalPath', 'qualityFlags', 'lastVerifiedAt', 'updatedAt'
];

const hiddenFields = [
  'storagePath (NAS真实路径 - 客户环境默认隐藏)',
  'permissionTags',
  'confidentialityLevel',
  'projectScope',
  'indexEligibility'
];

const prohibitedActions = [
  { action: 'Agent 直接增删改数据库', reason: '未完成写操作安全设计，禁止直写业务底表' },
  { action: 'Agent 自动移动/删除/修复 NAS 文件', reason: 'NAS 原文件只读影子导入，不允许 Agent 变更原文件' },
  { action: 'Agent 读取文件正文（PDF/Office/DWG/RVT/IFC）', reason: '未完成正文抽取安全设计和审计' },
  { action: 'Agent 将文件正文写入向量库/搜索引擎', reason: '未完成向量化权限/脱敏设计' },
  { action: 'Agent 自动下结论或自动审批', reason: '所有治理结论必须人工确认' },
  { action: 'Agent 自动触发治理动作', reason: 'Agent 不可绕过审核流程' },
  { action: '模型轻量化', reason: '二期能力，未进入本批范围' },
  { action: '构件级解析/搜索/碰撞检查', reason: '二期能力，未进入本批范围' },
  { action: '多 Agent 调度真实业务动作', reason: '未完成多 Agent 协调安全设计' },
  { action: '承诺客户生产级权限体系', reason: '本批只做权限证明和预览，不做生产级授权' }
];

function loadSampleFiles() {
  if (!sampleProjectId.value) return;
  sampleLoading.value = true;
  fetchCatalogFiles({ projectId: sampleProjectId.value, pageSize: 10 })
    .then(result => { sampleFiles.value = result.rows; })
    .finally(() => { sampleLoading.value = false; });
}

function checkPermission() {
  if (!proofFileId.value) return;
  const tableTarget = selectedProofTarget.value?.source === 'table' && selectedProofTarget.value.fileId === proofFileId.value
    ? selectedProofTarget.value
    : null;
  runPermissionCheck(tableTarget ?? { fileId: proofFileId.value, source: 'manual' });
}

function verifyPermissionFromRow(row: CatalogFile) {
  const target: ProofTarget = {
    fileId: row.fileId,
    fileName: row.fileName,
    projectCode: row.projectCode,
    projectName: row.projectName,
    source: 'table'
  };
  proofFileId.value = row.fileId;
  runPermissionCheck(target);
}

function runPermissionCheck(target: ProofTarget) {
  selectedProofTarget.value = target;
  proofLoading.value = true;
  fetchFilePermissionProof(target.fileId).then(result => {
    proofResult.value = result;
  }).finally(() => { proofLoading.value = false; });
}

function formatBytes(bytes: number): string {
  if (!bytes || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let i = 0; let v = bytes;
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
  return `${v.toFixed(i === 0 ? 0 : 2)} ${units[i]}`;
}

function formatDate(v: string | null): string {
  if (!v) return '-';
  return new Date(v).toLocaleString('zh-CN', { hour12: false });
}

fetchCatalogProjects().then(list => { projects.value = list ?? []; });
</script>

<style scoped>
.agent-preview-page {
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
}
.agent-section {
  margin-bottom: 28px;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}
.agent-section h2 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
}
.agent-section > p {
  margin: 0 0 12px 0;
  font-size: 13px;
  color: #909399;
}
.agent-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}
.field-visible {
  font-weight: 600;
  color: #67c23a;
}
.permission-result-card {
  margin-top: 12px;
}
.proof-target-card {
  margin-bottom: 12px;
}
.proof-target-card h3 {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #606266;
}
.permission-decision {
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 12px;
  font-size: 16px;
  text-align: center;
}
.permission-decision.allowed {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}
.permission-decision.denied {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fde2e2;
}
.evidence-list {
  margin-top: 12px;
}
.evidence-list h4 {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #606266;
}
.evidence-item {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 4px 0;
  min-width: 0;
  overflow-wrap: anywhere;
}
.trace-mono {
  font-family: monospace;
  font-size: 12px;
  overflow-wrap: anywhere;
}
.field-contract {
  min-width: 0;
  max-width: 100%;
}
.field-contract-group {
  margin-bottom: 14px;
  min-width: 0;
  max-width: 100%;
  overflow-wrap: anywhere;
}
.field-contract-group h3 {
  margin: 0 0 6px 0;
  font-size: 13px;
  color: #606266;
}
.rule-list {
  margin: 4px 0;
  padding-left: 20px;
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}
.prohibited-action {
  font-weight: 600;
  color: #f56c6c;
}
.agent-preview-page :deep(.el-table) {
  width: 100%;
  max-width: 100%;
}
.agent-preview-page :deep(.el-table__inner-wrapper),
.agent-preview-page :deep(.el-table__body-wrapper),
.agent-preview-page :deep(.el-table__header-wrapper) {
  min-width: 0;
}
.agent-preview-page :deep(.el-descriptions),
.agent-preview-page :deep(.el-alert),
.agent-preview-page :deep(.el-tag),
.agent-preview-page :deep(.el-descriptions__content) {
  max-width: 100%;
}
.agent-preview-page :deep(.el-descriptions__content) {
  overflow-wrap: anywhere;
}
</style>
