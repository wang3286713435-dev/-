<template>
  <section class="mvp-page">
    <div class="mvp-page__header">
      <div>
        <h1>智慧大屏</h1>
        <p>{{ projectLabel }}</p>
      </div>
      <el-button :icon="Refresh" @click="loadPage">刷新</el-button>
    </div>

    <section class="mvp-dashboard">
      <article v-for="item in cards" :key="item.label" class="mvp-stat mvp-stat--large">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.unit }}</em>
      </article>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import { fetchDashboardSummary, type DashboardSummary } from '@/modules/work-center/api/delivery';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const summary = ref<DashboardSummary | null>(null);

const projectId = computed(() => authStore.currentProjectId);
const projectLabel = computed(() => authStore.currentUser?.currentProject.name ?? '等待项目上下文');
const cards = computed(() => {
  const item = summary.value;
  if (!item) return [];
  return [
    { label: '部位节点', value: item.sectionNodeCount, unit: '个' },
    { label: '交付标准', value: item.deliverableDefinitionCount, unit: '项' },
    { label: '文件资源', value: item.fileCount, unit: '份' },
    { label: '模型集成', value: item.publishedModelCount, unit: '个已发布' },
    { label: '管理对象', value: item.managedObjectCount, unit: '个' },
    { label: '交付视图', value: item.documentBindingCount + item.drawingBindingCount, unit: '条' }
  ];
});

watch(projectId, () => loadPage(), { immediate: true });

async function loadPage() {
  if (!projectId.value) return;
  try {
    summary.value = await fetchDashboardSummary(projectId.value);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '智慧大屏加载失败');
  }
}
</script>
