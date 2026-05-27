<template>
  <section class="digital-twin-portal">
    <section class="digital-twin-intro" aria-label="BIM协同管理亮点功能">
      <div>
        <span>BIM 协同管理</span>
        <h1>BIM协同管理</h1>
        <p>以项目为单位串联模型、图纸、设备设施、房屋空间、整改和质量风险，让 BIM 协同成为卓羽智能数据中台的高频入口。</p>
      </div>
      <div class="digital-twin-highlights">
        <button
          class="digital-twin-highlight digital-twin-highlight--project"
          type="button"
          aria-controls="digital-twin-project-panel"
          :aria-expanded="projectPanelOpen"
          @click="projectPanelOpen = true"
        >
          <span>项目选择</span>
          <strong>{{ activeProject?.name ?? '请选择项目' }}</strong>
          <em>{{ activeProject?.code ?? `${filteredProjects.length} 个项目可选` }}</em>
        </button>

        <article v-for="item in highlights" :key="item.label" class="digital-twin-highlight">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </article>
      </div>
    </section>

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

      <DigitalTwinDashboardPage v-if="activeProjectId" />
      <el-empty v-else description="请选择项目后查看 BIM 协同管理" />
    </main>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { Close, Search } from '@element-plus/icons-vue';

import DigitalTwinDashboardPage from '@/modules/visualization/pages/DigitalTwinDashboardPage.vue';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const keyword = ref('');
const projectPanelOpen = ref(false);
const switchingProjectId = ref<number | null>(null);
const switchError = ref('');

const activeProjectId = computed(() => authStore.currentProjectId);
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

const highlights = computed(() => [
  {
    label: '能力定位',
    value: 'BIM协同',
    hint: '模型、设备、空间、交付联动'
  },
  {
    label: '数据来源',
    value: '平台真实数据',
    hint: '不接施工物联演示假数据'
  }
]);

async function selectProject(projectId: number) {
  if (projectId === activeProjectId.value) {
    projectPanelOpen.value = false;
    return;
  }
  if (switchingProjectId.value) return;
  switchingProjectId.value = projectId;
  switchError.value = '';
  try {
    await authStore.changeProject(projectId);
    projectPanelOpen.value = false;
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
.digital-twin-intro {
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
.digital-twin-intro > div:first-child span {
  color: var(--zy-blue-600);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

.digital-twin-projects__head h2,
.digital-twin-intro h1 {
  color: var(--zy-ink);
  line-height: 1.2;
  margin: 0;
}

.digital-twin-projects__head h2 {
  font-size: var(--zy-fs-xl);
}

.digital-twin-projects__head p,
.digital-twin-intro p,
.digital-twin-project em,
.digital-twin-highlights em,
.digital-twin-highlights span {
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

.digital-twin-intro {
  align-items: center;
  display: grid;
  gap: var(--zy-sp-4);
  grid-column: 1 / -1;
  grid-template-columns: minmax(0, 0.95fr) minmax(420px, 1fr);
  padding: var(--zy-sp-5);
}

.digital-twin-intro > div:first-child {
  display: grid;
  gap: var(--zy-sp-2);
  min-width: 0;
}

.digital-twin-intro h1 {
  font-size: var(--zy-fs-3xl);
}

.digital-twin-intro p {
  font-size: var(--zy-fs-sm);
  line-height: 1.7;
  margin: 0;
}

.digital-twin-highlights {
  display: grid;
  gap: var(--zy-sp-3);
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.digital-twin-highlight {
  background: var(--zy-surface);
  border: 1px solid color-mix(in srgb, var(--zy-line) 74%, transparent);
  border-radius: var(--zy-radius-base);
  color: var(--zy-ink);
  display: grid;
  gap: 5px;
  min-height: 86px;
  min-width: 0;
  padding: var(--zy-sp-3);
  text-align: left;
}

.digital-twin-highlight--project {
  cursor: pointer;
}

.digital-twin-highlight--project:hover,
.digital-twin-highlight--project:focus-visible {
  border-color: color-mix(in srgb, var(--zy-blue-500) 48%, transparent);
  box-shadow: var(--zy-shadow-sm);
  outline: none;
}

.digital-twin-highlight--project span {
  color: var(--zy-blue-600);
  font-weight: var(--zy-fw-semi);
}

.digital-twin-highlights strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-lg);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .digital-twin-intro {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .digital-twin-projects {
    height: min(680px, calc(100dvh - 48px));
    left: var(--zy-content-pad);
    max-height: calc(100dvh - 48px);
    top: var(--zy-content-pad);
    width: calc(100vw - var(--zy-content-pad) * 2);
  }

  .digital-twin-highlights {
    grid-template-columns: 1fr;
  }
}
</style>
