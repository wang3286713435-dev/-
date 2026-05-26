<template>
  <nav class="project-workspace-nav" aria-label="项目工作台导航">
    <div class="project-workspace-nav__summary">
      <el-button
        text
        size="small"
        :icon="Back"
        class="project-workspace-nav__back"
        @click="router.push({ name: 'data-steward-assets' })"
      >
        资产总览
      </el-button>
      <span class="project-workspace-nav__divider" aria-hidden="true">/</span>
      <div class="project-workspace-nav__project">
        <strong :title="projectName">{{ projectName }}</strong>
        <span v-if="projectCode" class="project-workspace-nav__code">{{ projectCode }}</span>
        <span class="project-workspace-nav__owner">负责人 · {{ projectManagerName }}</span>
      </div>
      <el-tag
        size="small"
        :type="masterDataReady ? 'success' : 'warning'"
        effect="plain"
        class="project-workspace-nav__stage-tag"
      >
        <span class="zy-status-dot" :class="masterDataReady ? 'zy-status-dot--success' : 'zy-status-dot--warning'"></span>
        {{ masterDataReady ? '主数据已就绪' : '先确认主数据' }}
      </el-tag>
    </div>

    <div class="project-workspace-nav__groups">
      <section class="project-workspace-nav__group" data-step="01">
        <header class="project-workspace-nav__group-head">
          <span class="project-workspace-nav__step-num">01</span>
          <div class="project-workspace-nav__group-meta">
            <strong>项目资产</strong>
            <small>真实文件、目录、模型与对象</small>
          </div>
          <span class="zy-code-chip">ASSET</span>
        </header>
        <div class="project-workspace-nav__primary">
          <el-button
            size="small"
            :type="isAssetTab('dashboard') ? 'primary' : undefined"
            :plain="!isAssetTab('dashboard')"
            @click="go('data-steward-asset-detail')"
          >
            项目可视化
          </el-button>
          <el-button
            size="small"
            :type="isAssetTab('files') ? 'primary' : undefined"
            :plain="!isAssetTab('files')"
            @click="goAssetTab('files')"
          >
            文件管理
          </el-button>
        </div>
        <div class="project-workspace-nav__secondary">
          <details :open="isAssetMoreActive">
            <summary>更多入口</summary>
            <div class="project-workspace-nav__more-list">
              <el-button
                text
                size="small"
                :type="isActive(['project-data-steward-models']) ? 'primary' : undefined"
                @click="go('project-data-steward-models')"
              >
                模型集成
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-data-steward-objects']) ? 'primary' : undefined"
                @click="go('project-data-steward-objects')"
              >
                管理对象
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-data-steward-issues']) ? 'primary' : undefined"
                @click="go('project-data-steward-issues')"
              >
                事项
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-data-steward-tasks']) ? 'primary' : undefined"
                @click="go('project-data-steward-tasks')"
              >
                任务
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-data-steward-exports']) ? 'primary' : undefined"
                @click="go('project-data-steward-exports')"
              >
                导出
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-data-steward-file-service']) ? 'primary' : undefined"
                @click="go('project-data-steward-file-service')"
              >
                文件服务
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-data-steward-storage-migration']) ? 'primary' : undefined"
                @click="go('project-data-steward-storage-migration')"
              >
                对象存储
              </el-button>
            </div>
          </details>
        </div>
      </section>

      <section class="project-workspace-nav__group" data-step="02">
        <header class="project-workspace-nav__group-head">
          <span class="project-workspace-nav__step-num">02</span>
          <div class="project-workspace-nav__group-meta">
            <strong>工程主数据</strong>
            <small>部位、节点类型与交付物标准</small>
          </div>
          <span class="zy-code-chip">DATA</span>
        </header>
        <div class="project-workspace-nav__primary">
          <el-button
            size="small"
            :type="isActive(['project-master-data-initialization']) ? 'primary' : undefined"
            :plain="!isActive(['project-master-data-initialization'])"
            @click="go('project-master-data-initialization')"
          >
            工程主数据
          </el-button>
        </div>
        <div class="project-workspace-nav__secondary">
          <details :open="isMasterDataMoreActive">
            <summary>更多入口</summary>
            <div class="project-workspace-nav__more-list">
              <el-button
                text
                size="small"
                :type="isActive(['project-master-data-sections']) ? 'primary' : undefined"
                @click="go('project-master-data-sections')"
              >
                部位树
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-master-data-node-types']) ? 'primary' : undefined"
                @click="go('project-master-data-node-types')"
              >
                节点类型
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-master-data-deliverable-standard']) ? 'primary' : undefined"
                @click="go('project-master-data-deliverable-standard')"
              >
                交付物标准
              </el-button>
            </div>
          </details>
        </div>
      </section>

      <section
        class="project-workspace-nav__group"
        :class="{ 'is-gated': !masterDataReady }"
        data-step="03"
      >
        <header class="project-workspace-nav__group-head">
          <span class="project-workspace-nav__step-num">03</span>
          <div class="project-workspace-nav__group-meta">
            <strong>交付工作中心</strong>
            <small>补交、审核、整改与预检查</small>
          </div>
          <span class="zy-code-chip">OPS</span>
        </header>
        <p v-if="!masterDataReady" class="project-workspace-nav__gate">
          请先在 <strong>初始化向导</strong> 生成 / 确认工程主数据草案，再进入正常交付流程。
        </p>
        <div class="project-workspace-nav__primary">
          <el-button
            size="small"
            :type="isActive(['project-work-dashboard']) ? 'primary' : undefined"
            :plain="!isActive(['project-work-dashboard'])"
            @click="goWorkCenter('project-work-dashboard')"
          >
            交付状态
          </el-button>
          <el-button
            size="small"
            :type="isActive(['project-work-document-delivery']) ? 'primary' : undefined"
            :plain="!isActive(['project-work-document-delivery'])"
            @click="goWorkCenter('project-work-document-delivery')"
          >
            文档交付
          </el-button>
        </div>
        <div class="project-workspace-nav__secondary">
          <details :open="isWorkMoreActive">
            <summary>更多入口</summary>
            <div class="project-workspace-nav__more-list">
              <el-button
                text
                size="small"
                :type="isActive(['project-work-drawing-delivery']) ? 'primary' : undefined"
                @click="goWorkCenter('project-work-drawing-delivery')"
              >
                图纸交付
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-work-rectifications']) ? 'primary' : undefined"
                @click="goWorkCenter('project-work-rectifications')"
              >
                整改闭环
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-work-delivery-package']) ? 'primary' : undefined"
                @click="goWorkCenter('project-work-delivery-package')"
              >
                交付包 / 档案目录
              </el-button>
              <el-button
                text
                size="small"
                :type="isActive(['project-work-agent-governance']) ? 'primary' : undefined"
                @click="goWorkCenter('project-work-agent-governance')"
              >
                交付治理助手
              </el-button>
            </div>
          </details>
        </div>
      </section>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Back } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';

import { fetchInitializationStatus, type InitializationStatus } from '@/modules/master-data/api/masterData';
import { useAuthStore } from '@/stores/auth';

const props = defineProps<{
  projectId: number;
}>();

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const initializationStatus = ref<InitializationStatus | null>(null);

const project = computed(() =>
  authStore.currentUser?.projects.find((item) => item.id === props.projectId)
);
const projectName = computed(() => project.value?.name ?? `项目 ${props.projectId}`);
const projectCode = computed(() => project.value?.code ?? '');
const projectManagerName = computed(() => project.value?.projectManagerName || '待维护');
const masterDataReady = computed(() =>
  Boolean(initializationStatus.value?.ready || initializationStatus.value?.standardStatus?.deliverableStandardReady)
);
const isAssetMoreActive = computed(() => isActive([
  'project-data-steward-models',
  'project-data-steward-objects',
  'project-data-steward-issues',
  'project-data-steward-tasks',
  'project-data-steward-exports',
  'project-data-steward-file-service',
  'project-data-steward-storage-migration'
]));
const isMasterDataMoreActive = computed(() => isActive([
  'project-master-data-sections',
  'project-master-data-node-types',
  'project-master-data-deliverable-standard'
]));
const isWorkMoreActive = computed(() => isActive([
  'project-work-drawing-delivery',
  'project-work-rectifications',
  'project-work-delivery-package',
  'project-work-agent-governance'
]));

watch(
  () => props.projectId,
  () => {
    void loadInitializationStatus();
  },
  { immediate: true }
);

async function loadInitializationStatus() {
  if (!Number.isFinite(props.projectId)) return;
  try {
    initializationStatus.value = await fetchInitializationStatus(props.projectId);
  } catch {
    initializationStatus.value = null;
  }
}

function go(name: string) {
  router.push({ name, params: { projectId: props.projectId } });
}

function goWorkCenter(name: string) {
  if (!masterDataReady.value) {
    ElMessage.warning('请先生成 / 确认工程主数据草案；工作中心页面会保留阻塞提示。');
  }
  go(name);
}

function goAssetTab(tab: string) {
  router.push({ name: 'data-steward-asset-detail', params: { projectId: props.projectId }, query: { tab } });
}

function isActive(names: string[]) {
  return names.includes(String(route.name ?? ''));
}

function isAssetTab(tab: string) {
  if (String(route.name ?? '') !== 'data-steward-asset-detail') return false;
  const current = typeof route.query.tab === 'string' ? route.query.tab : 'dashboard';
  return current === tab;
}
</script>

<style scoped>
.project-workspace-nav {
  display: grid;
  gap: var(--zy-sp-3);
  min-width: 0;
  margin-bottom: var(--zy-sp-4);
  padding: var(--zy-sp-4) var(--zy-sp-5);
  border: var(--zy-border);
  border-radius: var(--zy-radius-base);
  background:
    linear-gradient(180deg, var(--zy-surface) 0%, var(--zy-surface-soft) 100%);
  box-shadow: var(--zy-shadow-xs);
  position: relative;
}

.project-workspace-nav::before {
  content: "";
  position: absolute;
  left: 0;
  top: var(--zy-sp-4);
  bottom: var(--zy-sp-4);
  width: 3px;
  background: var(--zy-blue-500);
  border-radius: 0 2px 2px 0;
}

.project-workspace-nav__summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--zy-sp-3);
  min-width: 0;
  padding-bottom: var(--zy-sp-3);
  border-bottom: var(--zy-border-soft);
}

.project-workspace-nav__back :deep(.el-button) {
  font-weight: var(--zy-fw-medium);
  color: var(--zy-muted);
}

.project-workspace-nav__divider {
  color: var(--zy-subtle);
  font-size: var(--zy-fs-sm);
  margin: 0 -4px;
}

.project-workspace-nav__project {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: var(--zy-sp-2) var(--zy-sp-3);
  min-width: 0;
  flex: 1 1 auto;
}

.project-workspace-nav__project strong {
  min-width: 0;
  max-width: 360px;
  color: var(--zy-ink);
  font-size: var(--zy-fs-lg);
  font-weight: var(--zy-fw-semi);
  letter-spacing: -0.01em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-workspace-nav__code {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: var(--zy-radius-sm);
  background: var(--zy-bg);
  border: var(--zy-border-soft);
  color: var(--zy-muted);
  font-family: var(--zy-font-mono);
  font-size: var(--zy-fs-xs);
  font-weight: var(--zy-fw-semi);
}

.project-workspace-nav__owner {
  flex: 0 0 auto;
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
}

.project-workspace-nav__stage-tag {
  flex: 0 0 auto;
}

.project-workspace-nav__stage-tag .zy-status-dot {
  margin-right: 6px;
  transform: translateY(-1px);
}

.project-workspace-nav__groups {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--zy-sp-3);
  min-width: 0;
}

.project-workspace-nav__group {
  display: grid;
  align-content: start;
  gap: var(--zy-sp-2);
  min-width: 0;
  padding: var(--zy-sp-3) var(--zy-sp-4);
  border: var(--zy-border-soft);
  border-radius: var(--zy-radius-base);
  background: var(--zy-surface);
  transition: border-color var(--zy-duration-2) var(--zy-ease);
}

.project-workspace-nav__group:hover {
  border-color: rgba(37, 99, 235, 0.18);
}

.project-workspace-nav__group.is-gated {
  background: var(--zy-surface-soft);
}

.project-workspace-nav__group-head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--zy-sp-3);
  min-width: 0;
  padding-bottom: var(--zy-sp-2);
  border-bottom: 1px dashed var(--zy-line-soft);
}

.project-workspace-nav__step-num {
  display: inline-grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background: var(--zy-blue-50);
  color: var(--zy-blue-700);
  font-family: var(--zy-font-mono);
  font-size: 11px;
  font-weight: var(--zy-fw-bold);
  letter-spacing: 0;
  line-height: 1;
}

.project-workspace-nav__group-meta {
  display: grid;
  gap: 1px;
  min-width: 0;
}

.project-workspace-nav__group-meta strong {
  color: var(--zy-ink);
  font-size: var(--zy-fs-base);
  font-weight: var(--zy-fw-semi);
  letter-spacing: -0.005em;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.project-workspace-nav__group-meta small {
  color: var(--zy-muted);
  font-size: var(--zy-fs-xs);
  line-height: 1.4;
}

.project-workspace-nav__primary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  min-width: 0;
}

.project-workspace-nav__primary :deep(.el-button) {
  justify-content: center;
  width: 100%;
  min-width: 0;
  min-height: 30px;
  margin-left: 0;
  padding: 4px 10px;
  border-radius: var(--zy-radius-base);
  font-size: var(--zy-fs-sm);
  font-weight: var(--zy-fw-medium);
}

.project-workspace-nav__primary :deep(.el-button.is-plain) {
  background: var(--zy-surface);
  border-color: var(--zy-line);
  color: var(--zy-text-soft);
}

.project-workspace-nav__primary :deep(.el-button.is-plain:hover) {
  background: var(--zy-blue-50);
  border-color: var(--zy-blue-500);
  color: var(--zy-blue-700);
}

.project-workspace-nav__primary :deep(.el-button--primary:not(.is-plain)) {
  box-shadow: 0 1px 0 rgba(37, 99, 235, 0.15);
}

.project-workspace-nav__primary :deep(.el-button:only-child) {
  grid-column: 1 / -1;
}

.project-workspace-nav__secondary {
  display: grid;
  gap: var(--zy-sp-1);
  min-width: 0;
  padding-top: 2px;
}

.project-workspace-nav__secondary details {
  min-width: 0;
}

.project-workspace-nav__secondary summary {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  width: fit-content;
  color: var(--zy-subtle);
  font-size: 11px;
  font-weight: var(--zy-fw-semi);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  user-select: none;
}

.project-workspace-nav__secondary summary:hover {
  color: var(--zy-blue-700);
}

.project-workspace-nav__more-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px var(--zy-sp-1);
  min-width: 0;
  padding-top: 4px;
}

.project-workspace-nav__secondary details:not([open]) .project-workspace-nav__more-list {
  display: none;
}

.project-workspace-nav__secondary :deep(.el-button) {
  min-width: 0;
  min-height: 26px;
  margin-left: 0;
  padding: 2px 8px;
  font-size: var(--zy-fs-xs);
  color: var(--zy-muted);
}

.project-workspace-nav__secondary :deep(.el-button:hover) {
  color: var(--zy-blue-600);
  background: transparent;
}

.project-workspace-nav__secondary :deep(.el-button--primary.is-text) {
  color: var(--zy-blue-700);
  font-weight: var(--zy-fw-semi);
}

.project-workspace-nav__gate {
  margin: 0;
  padding: var(--zy-sp-2) var(--zy-sp-3);
  border: 1px solid rgba(245, 158, 11, 0.28);
  border-left: 3px solid var(--zy-amber-500);
  border-radius: var(--zy-radius-base);
  background: var(--zy-amber-50);
  color: #92400e;
  font-size: var(--zy-fs-xs);
  line-height: 1.55;
}

.project-workspace-nav__gate strong {
  color: #78350f;
  font-weight: var(--zy-fw-semi);
}

@media (max-width: 1180px) {
  .project-workspace-nav__groups {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .project-workspace-nav {
    padding: var(--zy-sp-3) var(--zy-sp-4);
  }

  .project-workspace-nav__summary {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--zy-sp-2);
  }

  .project-workspace-nav__project {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .project-workspace-nav__primary {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
