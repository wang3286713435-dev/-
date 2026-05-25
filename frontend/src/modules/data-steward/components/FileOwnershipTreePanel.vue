<template>
  <section class="ownership-panel">
    <header class="ownership-panel__header">
      <div>
        <span class="zy-code-chip">M2I</span>
        <h2>资产推导工程树</h2>
        <p>先让 105 的每个文件都有清楚归属，再由人工判断哪些进入正式交付挂接。</p>
      </div>
      <div class="ownership-panel__actions">
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-button :loading="recommendLoading" @click="loadRecommendations">生成归属建议</el-button>
        <el-button type="primary" :disabled="!coverage?.unassignedFiles" :loading="applyLoading" @click="confirmApplyAll">
          确认全部未归属文件
        </el-button>
      </div>
    </header>

    <section class="ownership-kpis">
      <article>
        <span>项目文件</span>
        <strong>{{ formatCount(coverage?.totalFiles) }}</strong>
        <em>105 试点资产</em>
      </article>
      <article>
        <span>已有归属</span>
        <strong>{{ formatCount(coverage?.assignedFiles) }}</strong>
        <em>{{ coverage?.assignmentCoverageRate ?? 0 }}%</em>
      </article>
      <article>
        <span>未归属</span>
        <strong>{{ formatCount(coverage?.unassignedFiles) }}</strong>
        <em>目标为 0</em>
      </article>
      <article>
        <span>已确认</span>
        <strong>{{ formatCount(coverage?.confirmedFiles) }}</strong>
        <em>人工确认口径</em>
      </article>
    </section>

    <el-alert
      class="ownership-panel__notice"
      type="info"
      show-icon
      :closable="false"
      title="这是资产推导树，不是最终工程结构。文件归属不是正式交付完成；过程文件、参考资料和待判定资料也必须有归属，但只有人工确认的正式应交资料才会进入文档/图纸交付挂接。"
    />

    <section class="ownership-layout">
      <aside class="ownership-tree">
        <header>
          <strong>资产推导树</strong>
          <span>点击节点查看已归属文件</span>
        </header>
        <el-tree
          v-loading="loading"
          :data="treeNodes"
          node-key="nodePath"
          :props="{ label: 'nodeLabel', children: 'children' }"
          default-expand-all
          highlight-current
          @node-click="selectNode"
        >
          <template #default="{ data }">
            <div class="ownership-tree__node">
              <span>{{ data.nodeLabel }}</span>
              <el-tag size="small" effect="plain">{{ data.fileCount }}</el-tag>
            </div>
          </template>
        </el-tree>
      </aside>

      <main class="ownership-detail">
        <div class="ownership-detail__summary">
          <div>
            <span>当前节点</span>
            <strong>{{ selectedNode?.nodePath || '请选择工程树节点' }}</strong>
          </div>
          <el-tag v-if="selectedNode" type="success" effect="plain">{{ ownershipTypeLabel(selectedNode.ownershipType) }}</el-tag>
        </div>

        <section v-if="selectedNode" class="ownership-node-metrics">
          <article>
            <span>节点文件</span>
            <strong>{{ formatCount(selectedNode.fileCount) }}</strong>
          </article>
          <article>
            <span>已确认</span>
            <strong>{{ formatCount(selectedNode.confirmedFileCount) }}</strong>
          </article>
          <article>
            <span>建议中</span>
            <strong>{{ formatCount(selectedNode.suggestedFileCount) }}</strong>
          </article>
        </section>

        <section class="ownership-node-files">
          <header>
            <div>
              <strong>当前节点文件</strong>
              <span>只展示目录级元数据和脱敏路径提示，不读取文件正文。</span>
            </div>
            <el-tag type="info" effect="plain">{{ nodeFileTotal }} 个文件</el-tag>
          </header>
          <el-table
            v-loading="nodeFilesLoading"
            :data="nodeFiles"
            class="master-table"
            empty-text="当前节点暂无已归属文件"
          >
            <el-table-column label="文件" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="ownership-file-cell">
                  <strong>{{ row.fileName }}</strong>
                  <span>{{ row.displayPath }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="归属节点" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.ownershipNodePath }}</template>
            </el-table-column>
            <el-table-column label="归属状态" width="110">
              <template #default="{ row }">
                <el-tag :type="ownershipStatusTag(row.ownershipStatus)" effect="plain">
                  {{ ownershipStatusLabel(row.ownershipStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="置信度" width="90">
              <template #default="{ row }">
                <el-tag :type="confidenceTag(row.ownershipConfidence)" effect="plain">
                  {{ confidenceLabel(row.ownershipConfidence) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="说明" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.reason }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="nodeFileTotal > nodeFilePageSize"
            v-model:current-page="nodeFilePage"
            v-model:page-size="nodeFilePageSize"
            class="ownership-node-files__pagination"
            layout="total, prev, pager, next"
            :total="nodeFileTotal"
            :page-sizes="[20, 50]"
            @change="loadNodeFiles"
          />
        </section>

        <section class="ownership-recommendations">
          <header>
            <div>
              <strong>待处理文件建议</strong>
              <span>Hermes 可辅助解释，当前默认用目录级元数据规则推荐。</span>
            </div>
            <el-tag type="info" effect="plain">{{ recommendationRows.length }} 条</el-tag>
          </header>
          <el-table
            v-loading="recommendLoading"
            :data="recommendationRows"
            class="master-table"
            empty-text="暂无未归属文件建议"
          >
            <el-table-column label="文件" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="ownership-file-cell">
                  <strong>{{ row.fileName }}</strong>
                  <span>{{ row.displayPath }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="建议归属" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <strong>{{ row.suggestedNodePath }}</strong>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="130">
              <template #default="{ row }">
                <el-tag effect="plain">{{ ownershipTypeLabel(row.ownershipType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="置信度" width="100">
              <template #default="{ row }">
                <el-tag :type="confidenceTag(row.confidence)">{{ confidenceLabel(row.confidence) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="原因" min-width="240" show-overflow-tooltip>
              <template #default="{ row }">{{ row.reason }}</template>
            </el-table-column>
          </el-table>
        </section>
      </main>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

import {
  applyFileOwnershipRecommendations,
  fetchFileOwnershipCoverage,
  fetchFileOwnershipNodeFiles,
  fetchFileOwnershipTree,
  recommendFileOwnership,
  type FileOwnershipCoverage,
  type FileOwnershipFileRow,
  type FileOwnershipRecommendation,
  type FileOwnershipTree,
  type FileOwnershipTreeNode
} from '@/modules/data-steward/api/dataSteward';

const props = defineProps<{
  projectId: number;
  active?: boolean;
}>();

const emit = defineEmits<{
  updated: [];
}>();

const loading = ref(false);
const recommendLoading = ref(false);
const applyLoading = ref(false);
const coverage = ref<FileOwnershipCoverage | null>(null);
const tree = ref<FileOwnershipTree | null>(null);
const recommendations = ref<FileOwnershipRecommendation[]>([]);
const selectedNode = ref<FileOwnershipTreeNode | null>(null);
const nodeFiles = ref<FileOwnershipFileRow[]>([]);
const nodeFilesLoading = ref(false);
const nodeFilePage = ref(1);
const nodeFilePageSize = ref(20);
const nodeFileTotal = ref(0);

const treeNodes = computed(() => tree.value?.nodes ?? []);
const recommendationRows = computed(() => {
  if (!selectedNode.value) return recommendations.value;
  return recommendations.value.filter((row) => row.suggestedNodePath.startsWith(selectedNode.value?.nodePath || ''));
});

onMounted(() => {
  if (props.active) void loadAll();
});

watch(() => props.active, (active) => {
  if (active) void loadAll();
});

watch(() => props.projectId, () => {
  if (props.active) void loadAll();
});

async function loadAll() {
  loading.value = true;
  try {
    const [nextCoverage, nextTree, nextRecommendations] = await Promise.all([
      fetchFileOwnershipCoverage(props.projectId),
      fetchFileOwnershipTree(props.projectId),
      recommendFileOwnership(props.projectId, { limit: 80, includeAssigned: false, source: 'RULE' })
    ]);
    coverage.value = nextCoverage;
    tree.value = nextTree;
    recommendations.value = nextRecommendations.rows;
    selectedNode.value = nextTree.nodes[0] ?? null;
    nodeFilePage.value = 1;
    await loadNodeFiles();
  } finally {
    loading.value = false;
  }
}

async function loadNodeFiles() {
  if (!selectedNode.value) {
    nodeFiles.value = [];
    nodeFileTotal.value = 0;
    return;
  }
  nodeFilesLoading.value = true;
  try {
    const result = await fetchFileOwnershipNodeFiles(props.projectId, {
      nodePath: selectedNode.value.nodePath,
      page: nodeFilePage.value,
      pageSize: nodeFilePageSize.value
    });
    nodeFiles.value = result.rows;
    nodeFilePage.value = result.page;
    nodeFilePageSize.value = result.pageSize;
    nodeFileTotal.value = result.total;
  } finally {
    nodeFilesLoading.value = false;
  }
}

async function loadRecommendations() {
  recommendLoading.value = true;
  try {
    const response = await recommendFileOwnership(props.projectId, { limit: 120, includeAssigned: false, source: 'RULE' });
    recommendations.value = response.rows;
    ElMessage.success(`已生成 ${response.totalCount} 条归属建议`);
  } finally {
    recommendLoading.value = false;
  }
}

async function confirmApplyAll() {
  await ElMessageBox.confirm(
    '将把当前未归属文件按平台推荐写入工程树归属。不会移动、删除、重命名 NAS 文件，也不会把文件自动挂接成正式交付。',
    '确认 105 文件归属',
    { type: 'warning', confirmButtonText: '确认写入归属', cancelButtonText: '取消' }
  );
  applyLoading.value = true;
  try {
    const result = await applyFileOwnershipRecommendations(props.projectId, {
      confirmed: true,
      applyAllUnassigned: true,
      source: 'RULE'
    });
    ElMessage.success(`归属写入完成：新增 ${result.createdCount}，更新 ${result.updatedCount}，失败 ${result.failedCount}`);
    emit('updated');
    await loadAll();
  } finally {
    applyLoading.value = false;
  }
}

function selectNode(node: FileOwnershipTreeNode) {
  selectedNode.value = node;
  nodeFilePage.value = 1;
  void loadNodeFiles();
}

function ownershipTypeLabel(type?: string | null) {
  const map: Record<string, string> = {
    DELIVERY: '正式交付',
    PROCESS: '过程资料',
    MODEL: '模型资料',
    DRAWING_EXCHANGE: '图纸收发',
    REFERENCE: '参考资料',
    ARCHIVE: '归档资料',
    PENDING_REVIEW: '待判定',
    PROJECT: '项目根',
    GROUP: '分组'
  };
  return map[type || ''] || '待判定';
}

function confidenceLabel(value?: string | null) {
  const map: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' };
  return map[value || ''] || '中';
}

function confidenceTag(value?: string | null) {
  if (value === 'HIGH') return 'success';
  if (value === 'LOW') return 'warning';
  return 'info';
}

function ownershipStatusLabel(value?: string | null) {
  const map: Record<string, string> = {
    CONFIRMED: '已确认',
    SUGGESTED: '建议中',
    REJECTED: '已驳回'
  };
  return map[value || ''] || '未归属';
}

function ownershipStatusTag(value?: string | null) {
  if (value === 'CONFIRMED') return 'success';
  if (value === 'SUGGESTED') return 'warning';
  if (value === 'REJECTED') return 'danger';
  return 'info';
}

function formatCount(value?: number | null) {
  return Number(value || 0).toLocaleString('zh-CN');
}
</script>

<style scoped>
.ownership-panel {
  display: grid;
  gap: 18px;
}

.ownership-panel__header,
.ownership-layout,
.ownership-kpis article,
.ownership-recommendations,
.ownership-node-files,
.ownership-detail__summary,
.ownership-node-metrics article {
  border: 1px solid rgba(91, 124, 255, 0.16);
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 20px 48px rgba(42, 55, 104, 0.08);
}

.ownership-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  border-radius: 20px;
  padding: 22px;
}

.ownership-panel__header h2 {
  margin: 8px 0 4px;
  font-size: 24px;
}

.ownership-panel__header p,
.ownership-panel__header span,
.ownership-kpis span,
.ownership-kpis em,
.ownership-detail__summary span,
.ownership-recommendations header span,
.ownership-file-cell span {
  color: #667085;
}

.ownership-panel__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.ownership-kpis,
.ownership-node-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.ownership-kpis article,
.ownership-node-metrics article {
  border-radius: 18px;
  padding: 18px;
}

.ownership-kpis strong,
.ownership-node-metrics strong {
  display: block;
  margin: 8px 0;
  font-size: 26px;
}

.ownership-panel__notice {
  border-radius: 14px;
}

.ownership-layout {
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  min-height: 560px;
  overflow: hidden;
  border-radius: 22px;
}

.ownership-tree {
  min-width: 0;
  padding: 18px;
  border-right: 1px solid rgba(91, 124, 255, 0.14);
  overflow: auto;
}

.ownership-tree header,
.ownership-recommendations header,
.ownership-node-files header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.ownership-tree header span {
  color: #667085;
  font-size: 12px;
}

.ownership-tree__node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.ownership-tree__node span {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ownership-detail {
  display: grid;
  align-content: start;
  gap: 16px;
  min-width: 0;
  padding: 18px;
}

.ownership-detail__summary {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  border-radius: 18px;
  padding: 16px;
}

.ownership-detail__summary strong {
  display: block;
  margin-top: 4px;
}

.ownership-recommendations,
.ownership-node-files {
  min-width: 0;
  border-radius: 18px;
  padding: 16px;
}

.ownership-node-files__pagination {
  margin-top: 12px;
  justify-content: flex-end;
}

.ownership-file-cell {
  display: grid;
  gap: 4px;
}

@media (max-width: 1180px) {
  .ownership-layout,
  .ownership-kpis,
  .ownership-node-metrics {
    grid-template-columns: 1fr;
  }

  .ownership-tree {
    border-right: 0;
    border-bottom: 1px solid rgba(91, 124, 255, 0.14);
    max-height: 320px;
  }
}
</style>
