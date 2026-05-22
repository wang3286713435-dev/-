<template>
  <section class="mvp-page">
    <div class="mvp-page__header">
      <div>
        <h1>整改中心</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Download" @click="handleExportCsv">导出整改 CSV</el-button>
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
      </div>
    </div>

    <div class="mb" style="display: flex; gap: 8px">
      <el-select v-model="statusFilter" clearable placeholder="按状态筛选" style="width: 160px" @change="loadPage">
        <el-option label="全部" value="" />
        <el-option label="待处理" value="OPEN" />
        <el-option label="处理中" value="IN_PROGRESS" />
        <el-option label="已处理" value="RESOLVED" />
        <el-option label="已关闭" value="CLOSED" />
        <el-option label="已重开" value="REOPENED" />
      </el-select>
    </div>

    <section class="workflow-guide">
      <div class="workflow-guide__main">
        <span class="workflow-guide__step">整改闭环</span>
        <h2>审核驳回后，在这里处理原因并关闭问题</h2>
        <p>
          整改项来自文档 / 图纸交付审核。先看来源文件、交付类型和驳回原因，处理后标记已处理，再由负责人关闭或重新打开。
        </p>
      </div>
      <ol class="workflow-guide__steps">
        <li>待处理：说明审核已经驳回，需要项目成员处理。</li>
        <li>处理中 / 已处理：用于记录处理进度和说明。</li>
        <li>已关闭：代表本轮整改被确认完成；如仍有问题可重新打开。</li>
      </ol>
    </section>

    <el-table v-loading="loading" :data="items" class="master-table" empty-text="暂无整改项">
      <el-table-column prop="bindingFileName" label="来源文件" min-width="180" />
      <el-table-column prop="bindingDeliverableTypeName" label="交付类型" min-width="140" />
      <el-table-column prop="bindingSectionNodeName" label="部位" min-width="120" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="reason" label="驳回原因" min-width="180" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="severity" label="严重程度" width="100" />
      <el-table-column prop="updatedAt" label="更新时间" width="170">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'OPEN' || row.status === 'IN_PROGRESS' || row.status === 'REOPENED'"
            size="small" type="primary" @click="openResolveDialog(row)"
          >
            标记已处理
          </el-button>
          <el-button
            v-if="row.status === 'RESOLVED'"
            size="small" type="success" @click="handleClose(row)"
          >
            关闭
          </el-button>
          <el-button
            v-if="row.status === 'CLOSED'"
            size="small" type="warning" @click="handleReopen(row)"
          >
            重新打开
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Resolve dialog -->
    <el-dialog v-model="resolveDialogVisible" title="标记已处理" width="480px">
      <el-form label-position="top">
        <el-form-item label="处理说明" required>
          <el-input v-model="resolutionNote" type="textarea" :rows="3" maxlength="1024" placeholder="请填写处理说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resolving" :disabled="!resolutionNote.trim()" @click="handleResolve">
          确认
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Download, Refresh } from '@element-plus/icons-vue';

import {
  fetchRectifications, resolveRectification, closeRectification, reopenRectification,
  exportRectificationsCsv, type RectificationItem
} from '@/modules/work-center/api/delivery';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const items = ref<RectificationItem[]>([]);
const statusFilter = ref('');

const resolving = ref(false);
const resolveDialogVisible = ref(false);
const resolutionNote = ref('');
const resolveTarget = ref<RectificationItem | null>(null);

const projectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => authStore.currentUser?.currentProject?.name ?? '等待项目上下文');

watch(projectId, () => loadPage(), { immediate: true });

function statusTagType(status: string) {
  if (status === 'OPEN') return 'danger';
  if (status === 'IN_PROGRESS') return 'warning';
  if (status === 'RESOLVED') return 'primary';
  if (status === 'CLOSED') return 'success';
  if (status === 'REOPENED') return 'info';
  return 'info';
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    OPEN: '待处理', IN_PROGRESS: '处理中', RESOLVED: '已处理', CLOSED: '已关闭', REOPENED: '已重开'
  };
  return map[status] ?? status;
}

function formatTime(iso: string) {
  return iso?.replace('T', ' ').substring(0, 19) ?? '';
}

async function loadPage() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    items.value = (await fetchRectifications(projectId.value, statusFilter.value || undefined)) ?? [];
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '整改项加载失败');
  } finally {
    loading.value = false;
  }
}

function openResolveDialog(row: RectificationItem) {
  resolveTarget.value = row;
  resolutionNote.value = '';
  resolveDialogVisible.value = true;
}

async function handleResolve() {
  if (!projectId.value || !resolveTarget.value || !resolutionNote.value.trim()) return;
  resolving.value = true;
  try {
    await resolveRectification(projectId.value, resolveTarget.value.id, resolutionNote.value.trim());
    ElMessage.success('已标记为已处理');
    resolveDialogVisible.value = false;
    await loadPage();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败');
  } finally {
    resolving.value = false;
  }
}

async function handleClose(row: RectificationItem) {
  if (!projectId.value) return;
  try {
    await closeRectification(projectId.value, row.id);
    ElMessage.success('已关闭');
    await loadPage();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '关闭失败');
  }
}

async function handleReopen(row: RectificationItem) {
  if (!projectId.value) return;
  try {
    await reopenRectification(projectId.value, row.id);
    ElMessage.success('已重新打开');
    await loadPage();
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败');
  }
}

function handleExportCsv() {
  if (!projectId.value) return;
  exportRectificationsCsv(projectId.value);
  ElMessage.success('整改项 CSV 导出已触发下载');
}
</script>

<style scoped>
.mb {
  margin-bottom: var(--zy-sp-4);
}
</style>
