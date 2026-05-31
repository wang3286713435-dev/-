<template>
  <section class="digital-twin-portal">
    <div
      v-if="projectPanelOpen"
      class="digital-twin-project-overlay"
      aria-hidden="true"
      @click="projectPanelOpen = false"
    />

    <aside
      v-if="projectPanelOpen"
      id="digital-twin-project-panel"
      class="digital-twin-projects"
      :class="{ 'is-open': projectPanelOpen }"
      aria-label="BIM协同项目选择"
      @keydown.esc.stop="projectPanelOpen = false"
    >
      <div class="digital-twin-projects__head">
        <div>
          <span>DIGITAL TWIN · {{ filteredProjects.length }} 个项目</span>
          <h2>项目选择</h2>
          <p>选择项目后，大屏会读取该项目的真实协同数据。</p>
        </div>
        <el-button
          text
          circle
          :icon="Close"
          aria-label="收起项目选择"
          @click="projectPanelOpen = false"
        />
      </div>

      <el-input
        v-model="keyword"
        :prefix-icon="Search"
        placeholder="搜索项目名称或编码"
        clearable
      />

      <div class="digital-twin-project-scroll">
        <div v-if="filteredProjects.length > 0" class="digital-twin-project-list">
          <button
            v-for="project in filteredProjects"
            :key="project.id"
            class="digital-twin-project"
            :class="{ 'is-active': project.id === activeProjectId }"
            type="button"
            :disabled="switchingProjectId === project.id"
            @click="selectProject(project.id)"
          >
            <span>{{ project.code }}</span>
            <strong>{{ project.name }}</strong>
            <em>{{ project.roleName }} · {{ project.projectManagerName || '负责人待维护' }}</em>
          </button>
        </div>

        <el-empty
          v-else
          description="暂无匹配项目"
          :image-size="54"
        />
      </div>
    </aside>

    <main class="digital-twin-main">
      <el-alert
        v-if="switchError"
        :title="switchError"
        type="error"
        show-icon
        :closable="false"
      />

      <section class="digital-twin-project-strip" aria-label="BIM协同项目选择">
        <button
          class="digital-twin-project-trigger"
          type="button"
          aria-controls="digital-twin-project-panel"
          :aria-expanded="projectPanelOpen"
          @click="projectPanelOpen = true"
        >
          <span>当前项目</span>
          <strong>{{ activeProject?.name ?? '请选择项目' }}</strong>
          <em>{{ activeProject?.code ?? `${filteredProjects.length} 个项目可选` }}</em>
        </button>
        <p>选择项目后，下方 BIM 协同大屏会直接读取该项目的模型、图纸、交付和质量数据。</p>
      </section>

      <DigitalTwinDashboardPage v-if="activeProjectId && portalProjectReady" />
      <el-skeleton v-else-if="activeProjectId && !switchError" :rows="8" animated />
      <el-empty v-else description="请选择项目后查看 BIM 协同管理" />
    </main>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Close, Search } from '@element-plus/icons-vue';
import { useRoute } from 'vue-router';

import DigitalTwinDashboardPage from '@/modules/visualization/pages/DigitalTwinDashboardPage.vue';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const route = useRoute();
const keyword = ref('');
const projectPanelOpen = ref(false);
const switchingProjectId = ref<number | null>(null);
const switchError = ref('');

const routeProjectId = computed(() => Number(route.query.projectId));
const requestedProjectId = computed(() => {
  const id = routeProjectId.value;
  return Number.isFinite(id) && id > 0 ? id : null;
});
const activeProjectId = computed(() => requestedProjectId.value ?? authStore.currentProjectId);
const portalProjectReady = computed(() => {
  const requestedId = requestedProjectId.value;
  return !requestedId || authStore.currentProjectId === requestedId;
});
const projects = computed(() => authStore.currentUser?.projects ?? []);

const filteredProjects = computed(() => {
  const query = keyword.value.trim().toLowerCase();
  if (!query) return projects.value;
  return projects.value.filter((project) => {
    return project.name.toLowerCase().includes(query)
      || project.code.toLowerCase().includes(query)
      || project.roleName.toLowerCase().includes(query);
  });
});

const activeProject = computed(() => {
  const id = activeProjectId.value;
  return projects.value.find((project) => project.id === id) ?? null;
});

watch(requestedProjectId, (projectId) => {
  if (!projectId) return;
  void ensureRouteProjectContext(projectId);
}, { immediate: true });

async function selectProject(projectId: number) {
  if (projectId === authStore.currentProjectId) {
    projectPanelOpen.value = false;
    return;
  }
  await switchProject(projectId, true);
}

async function ensureRouteProjectContext(projectId: number) {
  if (projectId === authStore.currentProjectId || switchingProjectId.value) return;
  await switchProject(projectId, false);
}

async function switchProject(projectId: number, closePanel: boolean) {
  if (switchingProjectId.value) return;
  switchingProjectId.value = projectId;
  switchError.value = '';
  try {
    await authStore.changeProject(projectId);
    if (closePanel) projectPanelOpen.value = false;
  } catch (error) {
    switchError.value = error instanceof Error ? error.message : '项目切换失败';
  } finally {
    switchingProjectId.value = null;
  }
}
</script>

<style scoped>
.digital-twin-portal {
  align-items: start;
  display: grid;
  gap: var(--zy-sp-4);
  grid-template-columns: minmax(0, 1fr);
  min-width: 0;
  position: relative;
}

.digital-twin-projects,
.digital-twin-project-strip {
  background: var(--zy-surface);
  border: 1px solid color-mix(in srgb, var(--zy-line) 72%, transparent);
  border-radius: var(--zy-radius-base);
  box-shadow: var(--zy-shadow-xs);
}

.digital-twin-projects {
  align-content: start;
  display: flex;
  flex-direction: column;
  gap: var(--zy-sp-3);
  height: min(760px, calc(100dvh - var(--zy-header-h) - 96px));
  left: calc(var(--zy-sidebar-w) + var(--zy-content-pad));
  max-height: calc(100dvh - var(--zy-header-h) - 96px);
  min-height: 0;
  min-width: 0;
  opacity: 0;
  overflow: hidden;
  padding: var(--zy-sp-4);
  pointer-events: none;
  position: fixed;
  top: calc(var(--zy-header-h) + var(--zy-content-pad) + 56px);
  transform: translateX(-14px);
  transition:
    opacity var(--zy-duration-2) var(--zy-ease-out),
    transform var(--zy-duration-2) var(--zy-ease-out);
  width: min(420px, calc(100vw - var(--zy-sidebar-w) - var(--zy-content-pad) * 2));
  z-index: 40;
}

.digital-twin-projects.is-open {
  opacity: 1;
  pointer-events: auto;
  transform: translateX(0);
}

.digital-twin-projects__head {
  align-items: flex-start;
  display: flex;
  gap: var(--zy-sp-3);
  justify-content: space-between;
  min-width: 0;
}

.digital-twin-projects__head > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.digital-twin-projects__head span,
.digital-twin-project-trigger span {
  color: var(--zy-blue-600);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

.digital-twin-projects__head h2 {
  color: var(--zy-ink);
  line-height: 1.2;
  margin: 0;
}

.digital-twin-projects__head h2 {
  font-size: var(--zy-fs-xl);
}

.digital-twin-projects__head p,
.digital-twin-project em,
.digital-twin-project-strip p,
.digital-twin-project-trigger em {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  font-style: normal;
}

.digital-twin-project-list {
  display: grid;
  gap: 8px;
}

.digital-twin-project-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 3px;
}

.digital-twin-project {
  background: color-mix(in srgb, var(--zy-blue-50) 34%, var(--zy-surface));
  border: 1px solid color-mix(in srgb, var(--zy-line) 78%, transparent);
  border-radius: var(--zy-radius-base);
  color: var(--zy-ink);
  cursor: pointer;
  display: grid;
  gap: 4px;
  min-height: 68px;
  padding: 10px 12px;
  text-align: left;
}

.digital-twin-project:hover,
.digital-twin-project.is-active {
  border-color: color-mix(in srgb, var(--zy-blue-500) 48%, transparent);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.12);
}

.digital-twin-project.is-active {
  background: color-mix(in srgb, var(--zy-blue-50) 78%, var(--zy-surface));
}

.digital-twin-project span {
  color: var(--zy-blue-600);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

.digital-twin-project strong {
  font-size: var(--zy-fs-sm);
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.digital-twin-project em {
  line-height: 1.25;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.digital-twin-project:disabled {
  cursor: wait;
  opacity: 0.72;
}

.digital-twin-project-overlay {
  background: rgba(15, 23, 42, 0.08);
  inset: 0;
  position: fixed;
  z-index: 35;
}

.digital-twin-main {
  display: grid;
  gap: var(--zy-sp-4);
  min-width: 0;
}

.digital-twin-project-strip {
  align-items: center;
  display: flex;
  gap: var(--zy-sp-3);
  justify-content: space-between;
  padding: var(--zy-sp-2) var(--zy-sp-3);
}

.digital-twin-project-strip p {
  line-height: 1.5;
  margin: 0;
  min-width: 0;
  text-align: right;
}

.digital-twin-project-trigger {
  align-items: center;
  background: color-mix(in srgb, var(--zy-blue-50) 44%, var(--zy-surface));
  border: 1px solid color-mix(in srgb, var(--zy-blue-500) 24%, transparent);
  border-radius: calc(var(--zy-radius-base) - 2px);
  color: var(--zy-ink);
  cursor: pointer;
  display: grid;
  gap: 2px 12px;
  grid-template-columns: auto minmax(160px, auto);
  min-height: 46px;
  min-width: min(420px, 100%);
  padding: 7px 12px;
  text-align: left;
}

.digital-twin-project-trigger span {
  grid-row: 1 / 3;
  white-space: nowrap;
}

.digital-twin-project-trigger strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.digital-twin-project-trigger em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.digital-twin-project-trigger:hover,
.digital-twin-project-trigger:focus-visible {
  border-color: color-mix(in srgb, var(--zy-blue-500) 48%, transparent);
  box-shadow: var(--zy-shadow-sm);
  outline: none;
}

@media (max-width: 860px) {
  .digital-twin-projects {
    height: min(680px, calc(100dvh - 48px));
    left: var(--zy-content-pad);
    max-height: calc(100dvh - 48px);
    top: var(--zy-content-pad);
    width: calc(100vw - var(--zy-content-pad) * 2);
  }

  .digital-twin-project-strip {
    align-items: stretch;
    display: grid;
  }

  .digital-twin-project-strip p {
    text-align: left;
  }

  .digital-twin-project-trigger {
    grid-template-columns: 1fr;
  }

  .digital-twin-project-trigger span {
    grid-row: auto;
  }
}
</style>
