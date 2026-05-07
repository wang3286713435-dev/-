<template>
  <div class="app-layout">
    <aside class="app-layout__sidebar">
      <div class="app-layout__brand">
        <strong>数字化交付平台</strong>
        <span>一期基础工程</span>
      </div>
      <SidebarMenu :menus="menus" />
    </aside>

    <div class="app-layout__main">
      <header class="app-layout__header">
        <div class="app-layout__project">
          <span class="app-layout__label">当前项目</span>
          <el-select
            v-model="selectedProjectId"
            size="large"
            class="app-layout__project-select"
            @change="handleProjectChange"
          >
            <el-option
              v-for="project in projects"
              :key="project.id"
              :label="`${project.code} | ${project.name}`"
              :value="project.id"
            />
          </el-select>
        </div>

        <div class="app-layout__actions">
          <div class="app-layout__user">
            <strong>{{ authStore.currentUser?.displayName }}</strong>
            <span>{{ authStore.currentUser?.username }}</span>
          </div>
          <el-button text @click="handleLogout">退出</el-button>
        </div>
      </header>

      <main class="app-layout__content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

import SidebarMenu from '@/modules/core/components/SidebarMenu.vue';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const router = useRouter();

const selectedProjectId = ref<number | null>(null);

const projects = computed(() => authStore.currentUser?.projects ?? []);
const menus = computed(() => authStore.currentUser?.menus ?? []);

watch(
  () => authStore.currentUser?.currentProject.id,
  (projectId) => {
    selectedProjectId.value = projectId ?? null;
  },
  { immediate: true }
);

async function handleProjectChange(projectId: number) {
  try {
    await authStore.changeProject(projectId);
    ElMessage.success('项目已切换');
    if (router.currentRoute.value.name !== 'home') {
      router.push({ name: 'home' });
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : '项目切换失败';
    ElMessage.error(message);
  }
}

async function handleLogout() {
  await authStore.signOut();
  router.replace({ name: 'login' });
}
</script>
