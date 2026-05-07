<template>
  <section class="home-page">
    <header class="home-page__header">
      <div>
        <h1>{{ overview?.projectName ?? '项目首页' }}</h1>
        <p>{{ overview?.projectCode ?? '等待项目上下文' }}</p>
      </div>
      <el-tag type="success" size="large">一期样板首页</el-tag>
    </header>

    <div v-if="loading" class="home-page__loading">
      <el-skeleton animated :rows="6" />
    </div>

    <template v-else-if="overview">
      <section class="metric-grid">
        <article v-for="metric in overview.metrics" :key="metric.label" class="metric-card">
          <span class="metric-card__label">{{ metric.label }}</span>
          <strong class="metric-card__value">{{ metric.value }}</strong>
          <span class="metric-card__unit">{{ metric.unit }}</span>
        </article>
      </section>

      <section class="home-page__panel">
        <div class="panel-header">
          <h2>准备情况</h2>
        </div>
        <ul class="notice-list">
          <li v-for="notice in overview.notices" :key="notice">{{ notice }}</li>
        </ul>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { ElMessage } from 'element-plus';

import { fetchHomeOverview } from '@/modules/core/api/home';
import type { HomeOverview } from '@/modules/core/api/types';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const overview = ref<HomeOverview | null>(null);
const loading = ref(false);

watch(
  () => authStore.currentProjectId,
  async (projectId) => {
    if (!projectId) {
      overview.value = null;
      return;
    }

    loading.value = true;
    try {
      overview.value = await fetchHomeOverview(projectId);
    } catch (error) {
      const message = error instanceof Error ? error.message : '首页加载失败';
      ElMessage.error(message);
    } finally {
      loading.value = false;
    }
  },
  { immediate: true }
);
</script>
