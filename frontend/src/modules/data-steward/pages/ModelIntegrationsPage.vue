<template>
  <section class="mvp-page">
    <div class="mvp-page__header">
      <div>
        <h1>模型集成</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建集成</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="models" class="master-table" empty-text="暂无模型集成">
      <el-table-column prop="name" label="集成名称" min-width="180" />
      <el-table-column prop="versionNo" label="版本" width="90" />
      <el-table-column prop="componentCount" label="构件数" width="100" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="发布时间" min-width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text :icon="UploadFilled" :disabled="row.status === 'PUBLISHED'" @click="handlePublish(row.id)">
            发布
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建模型集成" width="560px">
      <el-form label-position="top" class="master-form">
        <el-form-item label="集成名称">
          <el-input v-model="form.name" maxlength="128" />
        </el-form-item>
        <el-form-item label="模型文件">
          <el-select v-model="form.modelFileId" filterable>
            <el-option v-for="file in modelFiles" :key="file.id" :label="`${file.originalName} | ${file.processStatus}`" :value="file.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.versionNo" maxlength="32" />
        </el-form-item>
        <el-form-item label="构件数">
          <el-input-number v-model="form.componentCount" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="适配载荷 JSON">
          <el-input v-model="form.adapterPayloadJson" type="textarea" :rows="3" />
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
import { computed, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus, Refresh, UploadFilled } from '@element-plus/icons-vue';

import {
  createModelIntegration,
  fetchFileResources,
  fetchModelIntegrations,
  publishModelIntegration,
  type FileResource,
  type ModelIntegration
} from '@/modules/data-steward/api/dataSteward';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const models = ref<ModelIntegration[]>([]);
const modelFiles = ref<FileResource[]>([]);

const form = reactive({
  name: '',
  modelFileId: null as number | null,
  versionNo: 'V1',
  componentCount: 0,
  adapterPayloadJson: '{"adapter":"mock-bim"}'
});

const projectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => authStore.currentUser?.currentProject.name ?? '等待项目上下文');

watch(projectId, () => loadPage(), { immediate: true });

async function loadPage() {
  if (!projectId.value) return;
  loading.value = true;
  try {
    const [nextModels, nextFiles] = await Promise.all([
      fetchModelIntegrations(projectId.value),
      fetchFileResources(projectId.value, 'MODEL')
    ]);
    models.value = nextModels;
    modelFiles.value = nextFiles;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '模型集成加载失败');
  } finally {
    loading.value = false;
  }
}

function openCreateDialog() {
  Object.assign(form, {
    name: '',
    modelFileId: modelFiles.value.find((file) => file.processStatus === 'PROCESSED')?.id ?? null,
    versionNo: 'V1',
    componentCount: 0,
    adapterPayloadJson: '{"adapter":"mock-bim"}'
  });
  dialogVisible.value = true;
}

async function handleSave() {
  if (!projectId.value || !form.modelFileId) return;
  saving.value = true;
  try {
    await createModelIntegration(projectId.value, {
      name: form.name,
      modelFileId: form.modelFileId,
      versionNo: form.versionNo,
      componentCount: form.componentCount,
      adapterPayloadJson: form.adapterPayloadJson
    });
    ElMessage.success('模型集成已创建');
    dialogVisible.value = false;
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handlePublish(integrationId: number) {
  if (!projectId.value) return;
  try {
    await publishModelIntegration(projectId.value, integrationId);
    ElMessage.success('模型已发布');
    await loadPage();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发布失败');
  }
}
</script>
