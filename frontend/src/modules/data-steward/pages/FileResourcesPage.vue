<template>
  <section class="mvp-page">
    <div class="mvp-page__header">
      <div>
        <h1>文件资源</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="mvp-page__actions">
        <el-segmented v-model="fileKindFilter" :options="fileKindOptions" @change="handleFilterChange" />
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">登记文件</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="files" class="master-table" empty-text="暂无文件资源">
      <el-table-column prop="originalName" label="文件名称" min-width="220" />
      <el-table-column prop="fileKind" label="类型" width="110" />
      <el-table-column prop="versionNo" label="版本" width="90" />
      <el-table-column prop="processStatus" label="处理状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.processStatus === 'PROCESSED' ? 'success' : 'warning'">{{ row.processStatus }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="businessTag" label="业务标签" min-width="120" />
      <el-table-column prop="storageUri" label="存储地址" min-width="260" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button text :icon="CircleCheck" :disabled="row.processStatus === 'PROCESSED'" @click="handleProcess(row.id)">
            处理完成
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="table-pagination">
      <el-pagination
        v-model:current-page="pagination.pageNo"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[20, 50, 100, 200]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handlePageSizeChange"
        @current-change="loadPage"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="登记文件资源" width="560px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="文件名称">
          <el-input v-model="form.originalName" maxlength="255" />
        </el-form-item>
        <el-form-item label="文件类型">
          <el-segmented v-model="form.fileKind" :options="fileKindCreateOptions" />
        </el-form-item>
        <el-form-item label="存储地址">
          <el-input v-model="form.storageUri" maxlength="512" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.versionNo" maxlength="32" />
        </el-form-item>
        <el-form-item label="业务标签">
          <el-input v-model="form.businessTag" maxlength="64" />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-segmented v-model="form.processStatus" :options="processStatusOptions" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { CircleCheck, Plus, Refresh } from '@element-plus/icons-vue';

import {
  createFileResource,
  fetchFileResourcesPage,
  processFileResource,
  type FileResource
} from '@/modules/data-steward/api/dataSteward';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const files = ref<FileResource[]>([]);
const fileKindFilter = ref('ALL');
const pagination = reactive({
  pageNo: 1,
  pageSize: 50,
  total: 0
});
let activeRequestId = 0;
let activeAbortController: AbortController | null = null;

const form = reactive({
  originalName: '',
  fileKind: 'DOCUMENT',
  storageUri: '',
  versionNo: 'V1',
  businessTag: '',
  processStatus: 'PENDING'
});

const fileKindOptions = [
  { label: '全部', value: 'ALL' },
  { label: '文档', value: 'DOCUMENT' },
  { label: '图纸', value: 'DRAWING' },
  { label: '模型', value: 'MODEL' }
];
const fileKindCreateOptions = fileKindOptions.filter((item) => item.value !== 'ALL');
const processStatusOptions = ['PENDING', 'PROCESSING', 'PROCESSED', 'FAILED'];

const projectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => authStore.currentUser?.currentProject.name ?? '等待项目上下文');

watch(projectId, () => {
  pagination.pageNo = 1;
  loadPage();
}, { immediate: true });

onBeforeUnmount(() => {
  activeAbortController?.abort();
  activeRequestId += 1;
});

async function loadPage() {
  if (!projectId.value) return;
  activeAbortController?.abort();
  const requestId = activeRequestId + 1;
  activeRequestId = requestId;
  const controller = new AbortController();
  activeAbortController = controller;
  loading.value = true;
  try {
    const result = await fetchFileResourcesPage(projectId.value, {
      fileKind: fileKindFilter.value === 'ALL' ? undefined : fileKindFilter.value,
      pageNo: pagination.pageNo,
      pageSize: pagination.pageSize
    }, controller.signal);
    if (requestId !== activeRequestId || controller.signal.aborted) return;
    files.value = result.rows;
    pagination.pageNo = result.page;
    pagination.pageSize = result.pageSize;
    pagination.total = result.total;
  } catch (error) {
    if (requestId !== activeRequestId || controller.signal.aborted) return;
    ElMessage.error(error instanceof Error ? error.message : '文件资源加载失败');
  } finally {
    if (requestId === activeRequestId) {
      loading.value = false;
    }
  }
}

function handleFilterChange() {
  pagination.pageNo = 1;
  loadPage();
}

function handlePageSizeChange() {
  pagination.pageNo = 1;
  loadPage();
}

function openCreateDialog() {
  Object.assign(form, {
    originalName: '',
    fileKind: 'DOCUMENT',
    storageUri: `minio://delivery/${Date.now()}`,
    versionNo: 'V1',
    businessTag: '',
    processStatus: 'PENDING'
  });
  dialogVisible.value = true;
}

async function handleSave() {
  if (!projectId.value) return;
  saving.value = true;
  try {
    await createFileResource(projectId.value, {
      ...form,
      businessTag: form.businessTag || undefined
    });
    ElMessage.success('文件资源已登记');
    dialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handleProcess(fileId: number) {
  if (!projectId.value) return;
  try {
    await processFileResource(projectId.value, fileId);
    ElMessage.success('文件处理状态已更新');
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '处理失败');
  }
}
</script>

<style scoped>
.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
