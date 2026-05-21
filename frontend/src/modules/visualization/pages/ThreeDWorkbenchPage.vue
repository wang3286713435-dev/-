<template>
  <section class="mvp-page">
    <div class="mvp-page__header">
      <div>
        <h1>3D 工作台</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
        <el-button type="primary" :icon="Connection" @click="handleInject">注入上下文</el-button>
      </div>
    </div>

    <section class="workbench-shell">
      <div class="workbench-scene">
        <div class="workbench-scene__grid">
          <strong>{{ context?.publishedModelCount ?? 0 }}</strong>
          <span>Published BIM Context</span>
        </div>
      </div>

      <aside class="workbench-panel">
        <h2>对象上下文</h2>
        <el-select v-model="selectedObjectId" filterable class="workbench-panel__select">
          <el-option v-for="object in context?.objects ?? []" :key="object.id" :label="`${object.name} | ${object.code}`" :value="object.id" />
        </el-select>
        <div class="workbench-panel__actions">
          <el-button :icon="Aim" :disabled="!selectedObjectId" @click="handleLocate">定位</el-button>
          <el-button :icon="MagicStick" :disabled="!selectedObjectId" @click="handleHighlight">高亮</el-button>
        </div>
        <el-table :data="context?.models ?? []" size="small" empty-text="暂无模型">
          <el-table-column prop="name" label="模型" />
          <el-table-column prop="status" label="状态" width="110" />
        </el-table>
      </aside>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Aim, Connection, MagicStick, Refresh } from '@element-plus/icons-vue';

import {
  fetchVisualizationContext,
  highlightManagedObject,
  injectVisualizationContext,
  locateManagedObject,
  type VisualizationContext
} from '@/modules/visualization/api/visualization';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const context = ref<VisualizationContext | null>(null);
const selectedObjectId = ref<number | null>(null);

const projectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => authStore.currentUser?.currentProject?.name ?? '等待项目上下文');

watch(projectId, () => loadPage(), { immediate: true });

async function loadPage() {
  if (!projectId.value) return;
  try {
    context.value = await fetchVisualizationContext(projectId.value);
    selectedObjectId.value = context.value.objects[0]?.id ?? null;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '3D 上下文加载失败');
  }
}

async function handleInject() {
  if (!projectId.value) return;
  try {
    await injectVisualizationContext(projectId.value, null, selectedObjectId.value);
    ElMessage.success('上下文已注入适配层');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上下文注入失败');
  }
}

async function handleLocate() {
  if (!projectId.value || !selectedObjectId.value) return;
  try {
    await locateManagedObject(projectId.value, selectedObjectId.value);
    ElMessage.success('已下发构件定位指令');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '定位失败');
  }
}

async function handleHighlight() {
  if (!projectId.value || !selectedObjectId.value) return;
  try {
    await highlightManagedObject(projectId.value, selectedObjectId.value);
    ElMessage.success('已下发构件高亮指令');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '高亮失败');
  }
}
</script>
