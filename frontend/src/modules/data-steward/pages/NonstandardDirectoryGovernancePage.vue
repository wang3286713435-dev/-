<template>
  <section class="mvp-page governance-page">
    <div class="mvp-page__header">
      <div>
        <h1>非标准资料治理</h1>
        <p>暂缓入库目录清单</p>
      </div>
      <div class="mvp-page__actions">
        <el-select v-model="filters.governanceStatus" clearable placeholder="状态" @change="loadPage">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filters.riskType" clearable placeholder="风险类型" @change="loadPage">
          <el-option v-for="item in riskOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          class="governance-search"
          clearable
          placeholder="目录、编码、原因"
          :prefix-icon="Search"
          @keyup.enter="loadPage"
          @clear="loadPage"
        />
        <el-button :icon="Search" @click="openDiscoverDialog">发现目录</el-button>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </div>

    <section class="mvp-dashboard">
      <article v-for="item in cards" :key="item.label" class="mvp-stat mvp-stat--large">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.unit }}</em>
      </article>
    </section>

    <el-table v-loading="loading" :data="rows" class="master-table" empty-text="暂无非标准资料">
      <el-table-column label="目录" min-width="260">
        <template #default="{ row }">
          <div class="directory-cell">
            <strong>{{ row.directoryName }}</strong>
            <span>{{ row.suggestedProjectCode || '-' }} / {{ row.suggestedProjectName || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="风险" width="150">
        <template #default="{ row }">
          <el-tag :type="riskTag(row.riskType)">{{ riskLabel(row.riskType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.governanceStatus)">{{ statusLabel(row.governanceStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reviewReason" label="原因" min-width="220" show-overflow-tooltip />
      <el-table-column label="标准目录" width="110" align="right">
        <template #default="{ row }">{{ standardFolderCount(row.standardFoldersJson) }}</template>
      </el-table-column>
      <el-table-column prop="ownerName" label="负责人" width="120" show-overflow-tooltip />
      <el-table-column prop="nasPath" label="NAS 路径" min-width="320" show-overflow-tooltip />
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button text :icon="Edit" @click="openEditDialog(row)">治理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="discoverDialogVisible" title="发现非标准目录" width="680px">
      <el-form label-width="120px" :model="discoverForm">
        <el-form-item label="NAS 根路径" required>
          <el-input v-model="discoverForm.rootPath" />
        </el-form-item>
        <el-form-item label="暂缓编码">
          <el-input v-model="discoverForm.deferredProjectCodesText" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="discoverForm.ownerName" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="discoverDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="discovering" @click="discoverDirectories">发现</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="editVisible" title="治理记录" size="680px">
      <el-form v-if="editingRow" label-width="120px" :model="editForm">
        <el-form-item label="目录">
          <el-input :model-value="editingRow.directoryName" disabled />
        </el-form-item>
        <el-form-item label="NAS 路径">
          <el-input :model-value="editingRow.nasPath" disabled />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.governanceStatus">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险类型">
          <el-select v-model="editForm.riskType">
            <el-option v-for="item in riskOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="建议编码">
          <el-input v-model="editForm.suggestedProjectCode" />
        </el-form-item>
        <el-form-item label="建议名称">
          <el-input v-model="editForm.suggestedProjectName" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="editForm.ownerName" />
        </el-form-item>
        <el-form-item label="治理原因">
          <el-input v-model="editForm.reviewReason" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="Agent 建议">
          <el-input v-model="editForm.agentSuggestion" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="人工结论">
          <el-input v-model="editForm.manualDecision" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="结论依据">
          <el-input v-model="editForm.decisionReason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Edit, Refresh, Search } from '@element-plus/icons-vue';

import {
  discoverNonstandardDirectories,
  fetchNonstandardDirectories,
  updateNonstandardDirectory,
  type NonstandardDirectory,
  type NonstandardDirectoryUpdatePayload
} from '@/modules/data-steward/api/dataSteward';

const route = useRoute();
const loading = ref(false);
const discovering = ref(false);
const saving = ref(false);
const discoverDialogVisible = ref(false);
const editVisible = ref(false);
const rows = ref<NonstandardDirectory[]>([]);
const editingRow = ref<NonstandardDirectory | null>(null);

const filters = reactive({
  governanceStatus: queryString(route.query.governanceStatus) ?? '',
  riskType: queryString(route.query.riskType) ?? '',
  keyword: queryString(route.query.keyword) ?? ''
});
const discoverForm = reactive({
  rootPath: '/Volumes/zyzn/卓羽智能项目',
  deferredProjectCodesText: '95,98,99',
  ownerName: ''
});
const editForm = reactive<NonstandardDirectoryUpdatePayload>({
  governanceStatus: '',
  riskType: '',
  suggestedProjectCode: '',
  suggestedProjectName: '',
  reviewReason: '',
  agentSuggestion: '',
  manualDecision: '',
  decisionReason: '',
  ownerName: ''
});

const statusOptions = [
  { label: '待 Agent 分析', value: 'PENDING_AGENT' },
  { label: '人工复核', value: 'HUMAN_REVIEW' },
  { label: '允许后续导入', value: 'APPROVED_FOR_IMPORT' },
  { label: '忽略', value: 'IGNORED' },
  { label: '暂缓', value: 'DEFERRED' }
];
const riskOptions = [
  { label: '用户暂缓', value: 'USER_DEFERRED' },
  { label: '编号重复', value: 'DUPLICATE_CODE' },
  { label: '参考资料', value: 'REFERENCE' },
  { label: '编码未知', value: 'UNKNOWN_CODE' },
  { label: '临时资料', value: 'TEMP' },
  { label: '项目集合', value: 'MIXED_COLLECTION' },
  { label: '其他', value: 'OTHER' }
];

const cards = computed(() => [
  { label: '治理目录', value: formatCount(rows.value.length), unit: '个' },
  { label: '待 Agent', value: formatCount(countStatus('PENDING_AGENT')), unit: '个' },
  { label: '编号重复', value: formatCount(countRisk('DUPLICATE_CODE')), unit: '个' },
  { label: '用户暂缓', value: formatCount(countRisk('USER_DEFERRED')), unit: '个' },
  { label: '可导入', value: formatCount(countStatus('APPROVED_FOR_IMPORT')), unit: '个' },
  { label: '最近更新', value: formatDate(latestUpdatedAt()), unit: '时间' }
]);

loadPage();

async function loadPage() {
  loading.value = true;
  try {
    rows.value = await fetchNonstandardDirectories({
      governanceStatus: filters.governanceStatus || undefined,
      riskType: filters.riskType || undefined,
      keyword: filters.keyword.trim() || undefined,
      limit: 300
    });
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '治理清单加载失败');
  } finally {
    loading.value = false;
  }
}

function openDiscoverDialog() {
  discoverDialogVisible.value = true;
}

async function discoverDirectories() {
  if (!discoverForm.rootPath.trim()) {
    ElMessage.warning('请填写 NAS 根路径');
    return;
  }
  discovering.value = true;
  try {
    const result = await discoverNonstandardDirectories({
      rootPath: discoverForm.rootPath.trim(),
      providerCode: 'NAS',
      deferredProjectCodes: splitList(discoverForm.deferredProjectCodesText),
      ownerName: discoverForm.ownerName.trim() || undefined
    });
    rows.value = result.rows;
    discoverDialogVisible.value = false;
    ElMessage.success(`已发现 ${result.discoveredCount} 个非标准目录`);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '非标准目录发现失败');
  } finally {
    discovering.value = false;
  }
}

function openEditDialog(row: NonstandardDirectory) {
  editingRow.value = row;
  Object.assign(editForm, {
    governanceStatus: row.governanceStatus,
    riskType: row.riskType,
    suggestedProjectCode: row.suggestedProjectCode ?? '',
    suggestedProjectName: row.suggestedProjectName ?? '',
    reviewReason: row.reviewReason ?? '',
    agentSuggestion: row.agentSuggestion ?? '',
    manualDecision: row.manualDecision ?? '',
    decisionReason: row.decisionReason ?? '',
    ownerName: row.ownerName ?? ''
  });
  editVisible.value = true;
}

async function saveEdit() {
  if (!editingRow.value) return;
  saving.value = true;
  try {
    const updated = await updateNonstandardDirectory(editingRow.value.id, compactPayload(editForm));
    rows.value = rows.value.map((item) => (item.id === updated.id ? updated : item));
    editVisible.value = false;
    ElMessage.success('治理记录已保存');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '治理记录保存失败');
  } finally {
    saving.value = false;
  }
}

function compactPayload(payload: NonstandardDirectoryUpdatePayload) {
  return Object.fromEntries(
    Object.entries(payload).map(([key, value]) => [key, typeof value === 'string' ? value.trim() : value])
  ) as NonstandardDirectoryUpdatePayload;
}

function splitList(value: string) {
  return value
    .split(/[,\n，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function countStatus(status: string) {
  return rows.value.filter((item) => item.governanceStatus === status).length;
}

function countRisk(riskType: string) {
  return rows.value.filter((item) => item.riskType === riskType).length;
}

function latestUpdatedAt() {
  return rows.value.map((item) => item.updatedAt).filter(Boolean).sort().at(-1);
}

function riskLabel(value: string) {
  return riskOptions.find((item) => item.value === value)?.label ?? value;
}

function statusLabel(value: string) {
  return statusOptions.find((item) => item.value === value)?.label ?? value;
}

function riskTag(value: string) {
  if (value === 'DUPLICATE_CODE') return 'danger';
  if (value === 'USER_DEFERRED') return 'warning';
  if (value === 'REFERENCE') return 'info';
  return 'primary';
}

function statusTag(value: string) {
  if (value === 'APPROVED_FOR_IMPORT') return 'success';
  if (value === 'IGNORED') return 'info';
  if (value === 'DEFERRED') return 'warning';
  return 'primary';
}

function standardFolderCount(value: string | null) {
  if (!value) return 0;
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.length : 0;
  } catch {
    return 0;
  }
}

function formatCount(value: number | null | undefined) {
  return Number(value ?? 0).toLocaleString('zh-CN');
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

function queryString(value: unknown) {
  if (Array.isArray(value)) {
    return typeof value[0] === 'string' ? value[0] : undefined;
  }
  return typeof value === 'string' ? value : undefined;
}
</script>

<style scoped>
.governance-page {
  min-width: 0;
}

.governance-search {
  width: 260px;
}

.directory-cell {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.directory-cell strong,
.directory-cell span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.directory-cell span {
  color: #64748b;
}

@media (max-width: 960px) {
  .governance-search {
    width: 100%;
  }
}
</style>
