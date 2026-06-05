<template>
  <section class="ownership-panel">
    <header class="ownership-panel__header">
      <div>
        <span class="zy-code-chip">M2J</span>
        <h2>工程树人工复核</h2>
        <p>在资产推导结果上批量复核、调整归属节点和资料类型；不会移动或修改 NAS 文件。</p>
      </div>
      <div class="ownership-panel__actions">
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-button :loading="recommendLoading" @click="loadRecommendations">生成归属建议</el-button>
        <el-button type="primary" :disabled="!displayUnassignedFiles" :loading="applyLoading" @click="confirmApplyAll">
          确认全部未归属文件
        </el-button>
      </div>
    </header>

    <section class="ownership-kpis">
      <article>
        <span>项目文件</span>
        <strong>{{ formatCount(displayTotalFiles) }}</strong>
        <em>{{ projectScopeLabel }}</em>
      </article>
      <article>
        <span>已有归属</span>
        <strong>{{ formatCount(displayAssignedFiles) }}</strong>
        <em>{{ displayAssignmentRate }}%</em>
      </article>
      <article>
        <span>未归属</span>
        <strong>{{ formatCount(displayUnassignedFiles) }}</strong>
        <em>{{ displayUnassignedFiles > 0 ? '待建立归属' : '目标已达成' }}</em>
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

    <section class="ownership-business-tabs">
      <el-tabs v-model="businessTab">
        <el-tab-pane label="工程树草案" name="draft">
          <div class="ownership-business__header">
            <div>
              <strong>对象化后工程树优化草案</strong>
              <span>{{ treeDraft?.analysisBoundary || '只按目录、文件名和元数据生成草案。' }}</span>
            </div>
            <el-button size="small" :loading="draftApplyLoading" :disabled="!treeDraft?.nodes?.length" @click="confirmApplyTreeDraft">
              确认草案
            </el-button>
          </div>
          <el-table v-loading="draftLoading" :data="treeDraft?.nodes ?? []" class="master-table" empty-text="暂无工程树草案">
            <el-table-column prop="nodeLabel" label="草案节点" min-width="140" />
            <el-table-column prop="fileCount" label="文件" width="80" />
            <el-table-column prop="modelCount" label="模型" width="80" />
            <el-table-column prop="drawingCount" label="图纸" width="80" />
            <el-table-column prop="formalDeliveryCandidateCount" label="候选" width="90" />
            <el-table-column prop="currentMissingDeliverableCount" label="缺口" width="90" />
            <el-table-column prop="recommendationReason" label="推荐原因" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="模型/图纸缺口" name="gap">
          <div class="ownership-business__header">
            <div>
              <strong>目录级模型 / 图纸缺口</strong>
              <span>{{ modelDrawingGap?.analysisBoundary || '不是 BIM 构件级解析，不能证明模型内部内容。' }}</span>
            </div>
            <el-tag type="warning" effect="plain">catalog-only</el-tag>
          </div>
          <el-table v-loading="gapLoading" :data="modelDrawingGap?.rows ?? []" class="master-table" empty-text="暂无缺口分析">
            <el-table-column prop="nodeLabel" label="节点" min-width="130" />
            <el-table-column label="状态" min-width="150">
              <template #default="{ row }">
                <el-tag :type="gapStatusTag(row.gapStatus)" size="small">{{ gapStatusLabel(row.gapStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="modelCount" label="模型" width="80" />
            <el-table-column prop="drawingCount" label="图纸" width="80" />
            <el-table-column prop="processCount" label="过程" width="80" />
            <el-table-column prop="recommendation" label="建议" min-width="300" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="待交付候选" name="candidates">
          <div class="ownership-business__header">
            <div>
              <strong>缺失应交项候选文件</strong>
              <span>{{ deliveryCandidates?.analysisBoundary || '候选不会自动挂接，必须人工确认。' }}</span>
            </div>
            <el-tag type="info" effect="plain">缺失 {{ deliveryCandidates?.missingCount ?? 0 }} / 候选 {{ deliveryCandidates?.candidateCount ?? 0 }}</el-tag>
          </div>
          <el-table v-loading="candidatesLoading" :data="deliveryCandidates?.rows ?? []" class="master-table" empty-text="暂无候选文件">
            <el-table-column prop="targetName" label="交付目标" min-width="130" show-overflow-tooltip />
            <el-table-column prop="deliverableTypeName" label="交付类型" min-width="140" show-overflow-tooltip />
            <el-table-column prop="fileName" label="推荐文件" min-width="220" show-overflow-tooltip />
            <el-table-column prop="confidence" label="置信度" width="90" />
            <el-table-column prop="recommendationReason" label="推荐依据" min-width="260" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

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
          ref="ownershipTreeRef"
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
          <div class="ownership-node-toolbar">
            <el-select v-model="nodeFilters.status" placeholder="归属状态" @change="reloadNodeFiles">
              <el-option v-for="item in statusFilterOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="nodeFilters.ownershipType" placeholder="资料类型" @change="reloadNodeFiles">
              <el-option v-for="item in ownershipTypeFilterOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-checkbox v-model="nodeFilters.reviewOnly" @change="reloadNodeFiles">仅看待复核</el-checkbox>
            <span class="ownership-node-toolbar__selection">
              已选 {{ selectedNodeFiles.length }} 个文件
            </span>
            <el-button size="small" :disabled="!selectedNodeFiles.length" :loading="batchLoading" @click="confirmBatchReview('CONFIRM')">
              批量确认
            </el-button>
            <el-button size="small" :disabled="!selectedNodeFiles.length" :loading="batchLoading" @click="confirmBatchReview('REJECT')">
              批量驳回
            </el-button>
            <el-button size="small" :disabled="!selectedNodeFiles.length" :loading="batchLoading" @click="openTypeDialog">
              批量改归属类型
            </el-button>
            <el-button size="small" :disabled="!selectedNodeFiles.length" :loading="batchLoading" @click="openMoveDialog">
              批量移到其他节点
            </el-button>
          </div>
          <el-table
            v-loading="nodeFilesLoading"
            :data="nodeFiles"
            class="master-table"
            empty-text="当前节点暂无已归属文件"
            row-key="fileId"
            @selection-change="handleNodeFileSelection"
          >
            <el-table-column type="selection" width="44" />
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
            <el-table-column label="资料类型" width="140">
              <template #default="{ row }">
                <el-tag effect="plain">{{ ownershipTypeLabel(row.ownershipType) }}</el-tag>
              </template>
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

    <el-dialog v-model="typeDialogVisible" title="批量修改归属类型" width="420px">
      <el-form label-position="top">
        <el-form-item label="目标资料类型">
          <el-select v-model="batchTypeForm.ownershipType" placeholder="请选择目标类型">
            <el-option
              v-for="item in ownershipTypeActionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="复核说明">
          <el-input v-model="batchTypeForm.reason" type="textarea" :rows="3" placeholder="可填写人工判断依据" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="submitTypeUpdate">确认修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveDialogVisible" title="批量移动工程节点" width="520px">
      <el-form label-position="top">
        <el-form-item label="目标工程节点">
          <el-select
            v-model="batchMoveForm.nodePath"
            filterable
            placeholder="请选择目标节点"
            @change="handleMoveTargetChange"
          >
            <el-option
              v-for="item in nodeOptions"
              :key="item.nodePath"
              :label="item.nodePath"
              :value="item.nodePath"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="复核说明">
          <el-input v-model="batchMoveForm.reason" type="textarea" :rows="3" placeholder="可填写人工移动原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="submitNodeMove">确认移动</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';

import {
  applyFileOwnershipTreeDraft,
  applyFileOwnershipRecommendations,
  fetchAssetStatistics,
  fetchFileOwnershipCoverage,
  fetchFileOwnershipNodeFiles,
  fetchFileOwnershipTreeDraft,
  fetchFileOwnershipTree,
  fetchModelDrawingGap,
  recommendFileOwnership,
  reviewFileOwnershipAssignments,
  type FileOwnershipCoverage,
  type FileOwnershipFileRow,
  type FileOwnershipReviewAction,
  type FileOwnershipRecommendation,
  type FileOwnershipTreeDraft,
  type FileOwnershipTree,
  type FileOwnershipTreeNode,
  type ModelDrawingGap
} from '@/modules/data-steward/api/dataSteward';
import { fetchDeliveryCandidates, type DeliveryCandidatesResponse } from '@/modules/work-center/api/delivery';

const props = defineProps<{
  projectId: number;
  active?: boolean;
  focusNodePath?: string;
}>();

const emit = defineEmits<{
  updated: [];
}>();

const loading = ref(false);
const recommendLoading = ref(false);
const applyLoading = ref(false);
const draftLoading = ref(false);
const gapLoading = ref(false);
const candidatesLoading = ref(false);
const draftApplyLoading = ref(false);
const businessTab = ref('draft');
const coverage = ref<FileOwnershipCoverage | null>(null);
const assetFileTotal = ref<number | null>(null);
const tree = ref<FileOwnershipTree | null>(null);
const treeDraft = ref<FileOwnershipTreeDraft | null>(null);
const modelDrawingGap = ref<ModelDrawingGap | null>(null);
const deliveryCandidates = ref<DeliveryCandidatesResponse | null>(null);
const recommendations = ref<FileOwnershipRecommendation[]>([]);
const selectedNode = ref<FileOwnershipTreeNode | null>(null);
const nodeFiles = ref<FileOwnershipFileRow[]>([]);
const nodeFilesLoading = ref(false);
const batchLoading = ref(false);
const nodeFilePage = ref(1);
const nodeFilePageSize = ref(20);
const nodeFileTotal = ref(0);
const selectedNodeFiles = ref<FileOwnershipFileRow[]>([]);
const ownershipTreeRef = ref<{ setCurrentKey: (key: string) => void } | null>(null);
const typeDialogVisible = ref(false);
const moveDialogVisible = ref(false);
const nodeFilters = reactive({
  status: 'ALL',
  ownershipType: 'ALL',
  reviewOnly: false
});
const batchTypeForm = reactive({
  ownershipType: 'PROCESS',
  reason: ''
});
const batchMoveForm = reactive({
  nodePath: '',
  nodeKey: '',
  nodeLabel: '',
  reason: ''
});

const treeNodes = computed(() => tree.value?.nodes ?? []);
const displayTotalFiles = computed(() => {
  const coverageTotal = Number(coverage.value?.totalFiles ?? 0);
  const assetTotal = Number(assetFileTotal.value ?? 0);
  return coverageTotal > 0 ? coverageTotal : assetTotal;
});
const displayAssignedFiles = computed(() => Number(coverage.value?.assignedFiles ?? 0));
const displayUnassignedFiles = computed(() => Math.max(0, displayTotalFiles.value - displayAssignedFiles.value));
const displayAssignmentRate = computed(() => {
  if (!displayTotalFiles.value) return 0;
  return Number(((displayAssignedFiles.value / displayTotalFiles.value) * 100).toFixed(2));
});
const projectScopeLabel = computed(() => {
  const name = coverage.value?.projectName || tree.value?.projectName || '当前项目';
  if (!displayTotalFiles.value) return `${name} 暂无登记文件`;
  if (!displayAssignedFiles.value) return `${name} 待建立工程树归属`;
  return `${name} 资产`;
});
const nodeOptions = computed(() => flattenNodes(treeNodes.value).filter((node) => node.nodePath));
const recommendationRows = computed(() => {
  if (!selectedNode.value) return recommendations.value;
  return recommendations.value.filter((row) => row.suggestedNodePath.startsWith(selectedNode.value?.nodePath || ''));
});
const statusFilterOptions = [
  { label: '全部状态', value: 'ALL' },
  { label: '已确认', value: 'CONFIRMED' },
  { label: '建议中', value: 'SUGGESTED' },
  { label: '已驳回', value: 'REJECTED' }
];
const ownershipTypeActionOptions = [
  { label: '正式交付资料', value: 'DELIVERY' },
  { label: '过程资料', value: 'PROCESS' },
  { label: '模型资料', value: 'MODEL' },
  { label: '图纸收发', value: 'DRAWING_EXCHANGE' },
  { label: '参考归档', value: 'REFERENCE' },
  { label: '待判定', value: 'PENDING_REVIEW' }
];
const ownershipTypeFilterOptions = [
  { label: '全部类型', value: 'ALL' },
  ...ownershipTypeActionOptions,
  { label: '归档资料', value: 'ARCHIVE' }
];

onMounted(() => {
  if (props.active) void loadAll();
});

watch(() => props.active, (active) => {
  if (active) void loadAll();
});

watch(() => props.projectId, () => {
  if (props.active) void loadAll();
});

watch(() => props.focusNodePath, (nodePath) => {
  if (props.active && nodePath) {
    void focusNode(nodePath);
  }
});

async function loadAll() {
  loading.value = true;
  draftLoading.value = true;
  gapLoading.value = true;
  candidatesLoading.value = true;
  try {
    const [nextCoverage, nextTree, nextRecommendations, nextDraft, nextGap, nextCandidates, nextAssetStats] = await Promise.all([
      fetchFileOwnershipCoverage(props.projectId),
      fetchFileOwnershipTree(props.projectId),
      recommendFileOwnership(props.projectId, { limit: 80, includeAssigned: false, source: 'RULE' }),
      fetchFileOwnershipTreeDraft(props.projectId),
      fetchModelDrawingGap(props.projectId),
      fetchDeliveryCandidates(props.projectId, undefined, 'SECTION'),
      fetchAssetStatistics(props.projectId)
    ]);
    coverage.value = nextCoverage;
    assetFileTotal.value = Number(nextAssetStats?.fileCount ?? 0);
    tree.value = nextTree;
    recommendations.value = nextRecommendations.rows;
    treeDraft.value = nextDraft;
    modelDrawingGap.value = nextGap;
    deliveryCandidates.value = nextCandidates;
    selectedNode.value = findNodeByPath(nextTree.nodes, props.focusNodePath || selectedNode.value?.nodePath || '') ?? nextTree.nodes[0] ?? null;
    if (selectedNode.value) await setTreeCurrentNode(selectedNode.value.nodePath);
    nodeFilePage.value = 1;
    await loadNodeFiles();
  } finally {
    loading.value = false;
    draftLoading.value = false;
    gapLoading.value = false;
    candidatesLoading.value = false;
  }
}

async function confirmApplyTreeDraft() {
  if (!treeDraft.value?.nodes?.length) return;
  await ElMessageBox.confirm(
    '确认后只记录这版工程树草案已由人工查看，不会覆盖正式工程树，不会移动或修改 NAS 文件，也不会自动挂接交付。',
    '确认工程树草案',
    { type: 'info', confirmButtonText: '确认草案', cancelButtonText: '取消' }
  );
  draftApplyLoading.value = true;
  try {
    const result = await applyFileOwnershipTreeDraft(props.projectId, {
      confirmed: true,
      nodeKeys: treeDraft.value.nodes.map((node) => node.nodeKey)
    });
    ElMessage.success(result.message || '工程树草案已确认');
  } finally {
    draftApplyLoading.value = false;
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
      status: nodeFilters.status === 'ALL' ? undefined : nodeFilters.status,
      ownershipType: nodeFilters.ownershipType === 'ALL' ? undefined : nodeFilters.ownershipType,
      reviewOnly: nodeFilters.reviewOnly,
      page: nodeFilePage.value,
      pageSize: nodeFilePageSize.value
    });
    nodeFiles.value = result.rows;
    selectedNodeFiles.value = [];
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

function reloadNodeFiles() {
  nodeFilePage.value = 1;
  void loadNodeFiles();
}

function handleNodeFileSelection(rows: FileOwnershipFileRow[]) {
  selectedNodeFiles.value = rows;
}

async function focusNode(nodePath: string) {
  const node = findNodeByPath(treeNodes.value, nodePath);
  if (!node) return;
  selectedNode.value = node;
  await setTreeCurrentNode(node.nodePath);
  nodeFilePage.value = 1;
  await loadNodeFiles();
}

async function setTreeCurrentNode(nodePath: string) {
  await nextTick();
  ownershipTreeRef.value?.setCurrentKey(nodePath);
}

function findNodeByPath(nodes: FileOwnershipTreeNode[], nodePath: string): FileOwnershipTreeNode | null {
  if (!nodePath) return null;
  for (const node of nodes) {
    if (node.nodePath === nodePath) return node;
    const found = findNodeByPath(node.children ?? [], nodePath);
    if (found) return found;
  }
  return null;
}

function flattenNodes(nodes: FileOwnershipTreeNode[]): FileOwnershipTreeNode[] {
  return nodes.flatMap((node) => [node, ...flattenNodes(node.children ?? [])]);
}

async function confirmBatchReview(action: FileOwnershipReviewAction) {
  const count = selectedNodeFiles.value.length;
  if (!count) return;
  const label = action === 'CONFIRM' ? '确认' : '驳回';
  await ElMessageBox.confirm(
    `将对已选 ${count} 个文件执行“批量${label}归属”。该操作只更新平台归属元数据，不会移动、删除、重命名或读取 NAS 文件。`,
    `批量${label}归属`,
    { type: action === 'REJECT' ? 'warning' : 'info', confirmButtonText: `确认${label}`, cancelButtonText: '取消' }
  );
  await submitBatchReview(action, {
    reason: action === 'CONFIRM' ? '人工批量确认归属。' : '人工批量驳回归属，等待重新判断。'
  });
}

function openTypeDialog() {
  if (!selectedNodeFiles.value.length) return;
  batchTypeForm.ownershipType = selectedNodeFiles.value[0]?.ownershipType || 'PROCESS';
  batchTypeForm.reason = '';
  typeDialogVisible.value = true;
}

function openMoveDialog() {
  if (!selectedNodeFiles.value.length) return;
  const fallbackNode = selectedNode.value ?? nodeOptions.value[0];
  batchMoveForm.nodePath = fallbackNode?.nodePath || '';
  batchMoveForm.nodeKey = fallbackNode?.nodeKey || '';
  batchMoveForm.nodeLabel = fallbackNode?.nodeLabel || '';
  batchMoveForm.reason = '';
  moveDialogVisible.value = true;
}

function handleMoveTargetChange(nodePath: string) {
  const node = findNodeByPath(treeNodes.value, nodePath);
  batchMoveForm.nodeKey = node?.nodeKey || nodePath;
  batchMoveForm.nodeLabel = node?.nodeLabel || nodePath.split('/').filter(Boolean).pop() || '待判定';
}

async function submitTypeUpdate() {
  if (!selectedNodeFiles.value.length) return;
  const label = ownershipTypeLabel(batchTypeForm.ownershipType);
  await ElMessageBox.confirm(
    `将把已选 ${selectedNodeFiles.value.length} 个文件的归属类型改为“${label}”。不会改变正式交付挂接，也不会操作 NAS 文件。`,
    '确认修改归属类型',
    { type: 'warning', confirmButtonText: '确认修改', cancelButtonText: '取消' }
  );
  await submitBatchReview('UPDATE_TYPE', {
    ownershipType: batchTypeForm.ownershipType,
    reason: batchTypeForm.reason || `人工批量修改归属类型为“${label}”。`
  });
  typeDialogVisible.value = false;
}

async function submitNodeMove() {
  if (!selectedNodeFiles.value.length || !batchMoveForm.nodePath) return;
  await ElMessageBox.confirm(
    `将把已选 ${selectedNodeFiles.value.length} 个文件移动到工程节点“${batchMoveForm.nodePath}”。只更新平台归属记录，不移动 NAS 文件。`,
    '确认移动工程节点',
    { type: 'warning', confirmButtonText: '确认移动', cancelButtonText: '取消' }
  );
  await submitBatchReview('MOVE_NODE', {
    nodePath: batchMoveForm.nodePath,
    nodeKey: batchMoveForm.nodeKey,
    nodeLabel: batchMoveForm.nodeLabel,
    reason: batchMoveForm.reason || `人工批量移动到工程节点“${batchMoveForm.nodePath}”。`
  });
  moveDialogVisible.value = false;
}

async function submitBatchReview(
  action: FileOwnershipReviewAction,
  extra: { ownershipType?: string; nodePath?: string; nodeKey?: string; nodeLabel?: string; reason?: string } = {}
) {
  batchLoading.value = true;
  try {
    const result = await reviewFileOwnershipAssignments(props.projectId, {
      confirmed: true,
      fileIds: selectedNodeFiles.value.map((row) => row.fileId),
      action,
      ...extra
    });
    ElMessage.success(`归属复核完成：更新 ${result.updatedCount}，跳过 ${result.skippedCount}，失败 ${result.failedCount}`);
    emit('updated');
    const selectedPath = selectedNode.value?.nodePath || '';
    await Promise.all([
      fetchFileOwnershipCoverage(props.projectId).then((next) => { coverage.value = next; }),
      fetchAssetStatistics(props.projectId).then((next) => { assetFileTotal.value = Number(next?.fileCount ?? 0); }),
      fetchFileOwnershipTree(props.projectId).then((next) => { tree.value = next; })
    ]);
    selectedNode.value = findNodeByPath(treeNodes.value, selectedPath) ?? selectedNode.value;
    await loadNodeFiles();
  } finally {
    batchLoading.value = false;
  }
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

function gapStatusLabel(status?: string | null) {
  const map: Record<string, string> = {
    HAS_MODEL_AND_DRAWING: '有模型有图纸',
    DRAWING_MISSING_MODEL: '有图纸缺模型',
    MODEL_MISSING_DRAWING: '有模型缺图纸',
    PROCESS_ONLY: '只有过程资料',
    NEEDS_REVIEW: '待人工判断'
  };
  return map[status || ''] || '待人工判断';
}

function gapStatusTag(status?: string | null) {
  if (status === 'HAS_MODEL_AND_DRAWING') return 'success';
  if (status === 'DRAWING_MISSING_MODEL' || status === 'MODEL_MISSING_DRAWING') return 'warning';
  if (status === 'PROCESS_ONLY') return 'info';
  return 'danger';
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

.ownership-business-tabs {
  padding: 16px;
  border: 1px solid rgba(91, 124, 255, 0.16);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 20px 48px rgba(42, 55, 104, 0.08);
}

.ownership-business__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.ownership-business__header > div {
  display: grid;
  gap: 4px;
}

.ownership-business__header span {
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
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

.ownership-node-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.ownership-node-toolbar .el-select {
  width: 150px;
}

.ownership-node-toolbar__selection {
  color: #667085;
  font-size: 13px;
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
