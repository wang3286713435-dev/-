<template>
  <section class="mvp-page file-service-page">
    <div class="mvp-page__header">
      <div>
        <h1>文件服务</h1>
        <p>{{ projectLabel }}，集中查看平台已开放的文件访问能力和未开放的 NAS 写操作。</p>
      </div>
      <div class="mvp-page__actions">
        <el-button :icon="Refresh" @click="refresh">刷新</el-button>
      </div>
    </div>

    <section class="service-grid">
      <article v-for="item in enabledServices" :key="item.title" class="service-card">
        <el-tag type="success" effect="plain">已开放</el-tag>
        <h2>{{ item.title }}</h2>
        <p>{{ item.description }}</p>
        <el-button text type="primary" @click="openService(item.target)">进入</el-button>
      </article>
    </section>

    <section class="service-section">
      <div class="service-section__header">
        <div>
          <h2>未开放写操作</h2>
          <span>这些能力会直接改变 NAS 文件，当前只作为二期后段或三期受控能力保留。</span>
        </div>
        <el-tag type="warning" effect="plain">需要审批和回滚方案</el-tag>
      </div>
      <div class="disabled-action-grid">
        <article v-for="item in disabledActions" :key="item.title" class="disabled-action">
          <strong>{{ item.title }}</strong>
          <span>{{ item.reason }}</span>
          <el-button disabled size="small">未开放</el-button>
        </article>
      </div>
    </section>

    <section class="service-section">
      <div class="service-section__header">
        <div>
          <h2>安全口径</h2>
          <span>当前文件服务遵循 4R 文件访问安全闭环。</span>
        </div>
      </div>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="真实路径">普通项目用户不可见，使用平台逻辑路径和受控访问入口。</el-descriptions-item>
        <el-descriptions-item label="预览与下载">通过短时票据访问，预览权限和下载权限分离。</el-descriptions-item>
        <el-descriptions-item label="审计">预览、下载、拒绝和失败动作均写入审计。</el-descriptions-item>
        <el-descriptions-item label="Hermes">只读辅助，不能执行写库、NAS 操作或自动审批。</el-descriptions-item>
      </el-descriptions>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { RouteRecordName } from 'vue-router';
import { useRoute, useRouter } from 'vue-router';
import { Refresh } from '@element-plus/icons-vue';

import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const projectId = computed(() => {
  const routeId = Number(route.params.projectId);
  return Number.isFinite(routeId) && routeId > 0 ? routeId : authStore.currentProjectId;
});
const projectLabel = computed(() => {
  const current = authStore.currentUser?.projects.find((item) => item.id === projectId.value);
  return current ? `${current.code} ${current.name}` : '等待项目上下文';
});

const enabledServices: Array<{ title: string; description: string; target: RouteRecordName }> = [
  { title: '文件预览', description: '查看预览状态，并通过短时票据打开可预览文件。', target: 'data-steward-asset-detail' },
  { title: '下载权限', description: '下载和预览分开判断，普通查看者不能下载。', target: 'data-steward-asset-detail' },
  { title: '权限证明', description: '验证当前用户对指定文件的访问权限和原因。', target: 'data-steward-agent-preview' },
  { title: 'Hermes 只读辅助', description: '围绕资产目录回答问题，不读取正文也不执行写操作。', target: 'data-steward-asset-detail' }
];

const disabledActions = [
  { title: '上传文件', reason: '会新增 NAS 文件，需先建立配额、杀毒、版本和审计策略。' },
  { title: '新建文件夹', reason: '会改变真实目录结构，需先确认客户现场命名标准。' },
  { title: '移动文件', reason: '会影响路径追溯和交付绑定，必须具备回滚方案。' },
  { title: '重命名文件', reason: '会影响文件查找、版本链和审计证据。' },
  { title: '真实删除', reason: '必须走申请、审批、隔离、恢复和到期永久删除。' },
  { title: '批量打包下载', reason: '涉及大容量、权限聚合和审计，后续单独开放。' }
];

function openService(name: RouteRecordName) {
  if (name === 'data-steward-agent-preview') {
    void router.push({ name });
    return;
  }
  if (projectId.value) {
    void router.push({ name, params: { projectId: projectId.value } });
  }
}

function refresh() {
  window.location.reload();
}
</script>

<style scoped>
.file-service-page {
  min-width: 0;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.service-card,
.service-section,
.disabled-action {
  min-width: 0;
  border: 1px solid var(--zy-line);
  border-radius: 8px;
  background: var(--zy-surface);
}

.service-card {
  display: grid;
  align-content: start;
  gap: 10px;
  padding: 14px;
}

.service-card h2 {
  margin: 0;
  color: var(--zy-ink);
  font-size: 16px;
}

.service-card p,
.disabled-action span,
.service-section__header span {
  margin: 0;
  color: var(--zy-muted);
  font-size: 13px;
}

.service-section {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.service-section__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.service-section__header h2 {
  margin: 0;
  font-size: 16px;
}

.disabled-action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.disabled-action {
  display: grid;
  gap: 8px;
  padding: 12px;
  background: var(--zy-bg);
}

.disabled-action strong {
  color: var(--zy-ink);
  font-size: 14px;
}

@media (max-width: 1100px) {
  .service-grid,
  .disabled-action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .service-grid,
  .disabled-action-grid {
    grid-template-columns: 1fr;
  }
}
</style>
