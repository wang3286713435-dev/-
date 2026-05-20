<template>
  <nav class="project-workspace-nav" aria-label="项目工作台导航">
    <div class="project-workspace-nav__summary">
      <el-button text size="small" :icon="Back" @click="router.push({ name: 'data-steward-assets' })">
        资产总览
      </el-button>
      <div class="project-workspace-nav__project">
        <strong>{{ projectName }}</strong>
        <span v-if="projectCode">{{ projectCode }}</span>
        <span>负责人：{{ projectManagerName }}</span>
      </div>
    </div>

    <div class="project-workspace-nav__groups">
      <div class="project-workspace-nav__group">
        <div class="project-workspace-nav__group-head">
          <strong>数据管家</strong>
          <small>文件资产、预览和治理风险</small>
        </div>
        <el-button
          text
          size="small"
          :type="isAssetTab('dashboard') ? 'primary' : undefined"
          @click="go('data-steward-asset-detail')"
        >
          资产驾驶舱
        </el-button>
        <el-button
          text
          size="small"
          :type="isAssetTab('files') ? 'primary' : undefined"
          @click="goAssetTab('files')"
        >
          文件管理
        </el-button>
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
      </div>
      <div class="project-workspace-nav__group">
        <div class="project-workspace-nav__group-head">
          <strong>工程主数据</strong>
          <small>部位、节点类型和交付标准</small>
        </div>
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
          :type="isActive(['project-master-data-initialization']) ? 'primary' : undefined"
          @click="go('project-master-data-initialization')"
        >
          初始化向导
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
      <div class="project-workspace-nav__group">
        <div class="project-workspace-nav__group-head">
          <strong>工作中心</strong>
          <small>文档图纸交付、审核整改和预检查</small>
        </div>
        <el-button
          text
          size="small"
          :type="isActive(['project-work-document-delivery']) ? 'primary' : undefined"
          @click="go('project-work-document-delivery')"
        >
          文档交付
        </el-button>
        <el-button
          text
          size="small"
          :type="isActive(['project-work-drawing-delivery']) ? 'primary' : undefined"
          @click="go('project-work-drawing-delivery')"
        >
          图纸交付
        </el-button>
        <el-button
          text
          size="small"
          :type="isActive(['project-work-rectifications']) ? 'primary' : undefined"
          @click="go('project-work-rectifications')"
        >
          整改闭环
        </el-button>
        <el-button
          text
          size="small"
          :type="isActive(['project-work-agent-governance']) ? 'primary' : undefined"
          @click="go('project-work-agent-governance')"
        >
          交付治理
        </el-button>
        <el-button
          text
          size="small"
          :type="isActive(['project-work-dashboard']) ? 'primary' : undefined"
          @click="go('project-work-dashboard')"
        >
          项目驾驶舱
        </el-button>
      </div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Back } from '@element-plus/icons-vue';
import { useRoute, useRouter } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const props = defineProps<{
  projectId: number;
}>();

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const project = computed(() =>
  authStore.currentUser?.projects.find((item) => item.id === props.projectId)
);
const projectName = computed(() => project.value?.name ?? `项目 ${props.projectId}`);
const projectCode = computed(() => project.value?.code ?? '');
const projectManagerName = computed(() => project.value?.projectManagerName || '待维护');

function go(name: string) {
  router.push({ name, params: { projectId: props.projectId } });
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
  gap: 10px;
  min-width: 0;
  margin-bottom: 14px;
  padding: 12px 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: #ffffff;
}

.project-workspace-nav__summary {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.project-workspace-nav__project {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.project-workspace-nav__project strong {
  min-width: 0;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-workspace-nav__project span {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
}

.project-workspace-nav__groups {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  min-width: 0;
}

.project-workspace-nav__group {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 5px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 8px;
  background: #f8fafc;
}

.project-workspace-nav__group-head {
  display: flex;
  flex: 0 0 128px;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  padding-top: 2px;
}

.project-workspace-nav__group-head strong {
  color: #303133;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.project-workspace-nav__group-head small {
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
}

@media (max-width: 1180px) {
  .project-workspace-nav__groups {
    grid-template-columns: 1fr;
  }

  .project-workspace-nav__group-head {
    flex-basis: 160px;
  }
}

@media (max-width: 720px) {
  .project-workspace-nav__summary {
    align-items: flex-start;
    flex-direction: column;
  }

  .project-workspace-nav__project {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .project-workspace-nav__group-head {
    flex-basis: 100%;
  }
}
</style>
