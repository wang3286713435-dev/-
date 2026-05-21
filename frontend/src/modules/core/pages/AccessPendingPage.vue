<template>
  <section class="mvp-page access-pending-page">
    <div class="access-pending-panel">
      <el-result icon="info" title="账号已创建" sub-title="当前还没有可访问项目">
        <template #extra>
          <div class="access-pending-copy">
            <p>请联系平台管理员分配项目权限。授权完成后，重新登录或刷新页面即可进入项目工作区。</p>
            <p>在获得项目权限前，平台不会展示任何项目数据。</p>
          </div>
          <el-button type="primary" @click="handleLogout">退出登录</el-button>
        </template>
      </el-result>
    </div>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const router = useRouter();

async function handleLogout() {
  await authStore.signOut();
  router.replace({ name: 'login' });
}
</script>
