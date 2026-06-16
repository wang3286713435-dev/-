<template>
  <section class="mvp-page profile-page">
    <div class="mvp-page__header">
      <div>
        <h1>个人中心</h1>
        <p>维护个人资料和登录密码。</p>
      </div>
    </div>

    <div class="profile-grid">
      <section class="profile-card">
        <div class="profile-card__title">
          <span class="profile-avatar">{{ userInitial }}</span>
          <div>
            <strong>{{ authStore.currentUser?.displayName || authStore.currentUser?.username }}</strong>
            <small>{{ authStore.currentUser?.username }}</small>
          </div>
        </div>
        <dl class="profile-meta">
          <div>
            <dt>手机号</dt>
            <dd>{{ authStore.currentUser?.phoneNumber || '-' }}</dd>
          </div>
          <div>
            <dt>部门</dt>
            <dd>{{ authStore.currentUser?.departmentName || '-' }}</dd>
          </div>
          <div>
            <dt>可访问项目</dt>
            <dd>{{ authStore.currentUser?.projects.length ?? 0 }} 个</dd>
          </div>
          <div>
            <dt>最近登录</dt>
            <dd>{{ formatDate(authStore.currentUser?.lastLoginAt ?? null) }}</dd>
          </div>
        </dl>
      </section>

      <section class="profile-card">
        <div class="profile-card__section-title">
          <strong>个人资料</strong>
          <span>用户名和手机号由管理员维护</span>
        </div>
        <el-form label-position="top" class="profile-form">
          <el-form-item label="姓名">
            <el-input v-model.trim="profileForm.displayName" maxlength="128" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="部门">
            <el-input v-model.trim="profileForm.departmentName" maxlength="128" placeholder="请输入部门" />
          </el-form-item>
          <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
        </el-form>
      </section>

      <section class="profile-card">
        <div class="profile-card__section-title">
          <strong>修改密码</strong>
          <span>修改后请使用新密码登录</span>
        </div>
        <el-form label-position="top" class="profile-form">
          <el-form-item label="当前密码">
            <el-input v-model="passwordForm.currentPassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-64 位" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              placeholder="再次输入新密码"
              @keyup.enter="changePassword"
            />
          </el-form-item>
          <el-button type="primary" :loading="passwordSaving" @click="changePassword">更新密码</el-button>
        </el-form>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

import { changeCurrentUserPassword, updateCurrentUserProfile } from '@/modules/core/api/profile';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const profileSaving = ref(false);
const passwordSaving = ref(false);

const profileForm = reactive({
  displayName: '',
  departmentName: ''
});

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const userInitial = computed(() => {
  const name = authStore.currentUser?.displayName || authStore.currentUser?.username || 'U';
  return name.trim().slice(0, 1).toUpperCase();
});

onMounted(() => {
  profileForm.displayName = authStore.currentUser?.displayName ?? '';
  profileForm.departmentName = authStore.currentUser?.departmentName ?? '';
});

async function saveProfile() {
  if (!profileForm.displayName) {
    ElMessage.error('请输入姓名');
    return;
  }
  profileSaving.value = true;
  try {
    await updateCurrentUserProfile({
      displayName: profileForm.displayName,
      departmentName: profileForm.departmentName || undefined
    });
    await authStore.loadCurrentUser();
    ElMessage.success('个人资料已保存');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    profileSaving.value = false;
  }
}

async function changePassword() {
  if (!passwordForm.currentPassword) {
    ElMessage.error('请输入当前密码');
    return;
  }
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 64) {
    ElMessage.error('新密码长度应为 6-64 位');
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致');
    return;
  }
  passwordSaving.value = true;
  try {
    await changeCurrentUserPassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    });
    passwordForm.currentPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    ElMessage.success('密码已更新');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密码更新失败');
  } finally {
    passwordSaving.value = false;
  }
}

function formatDate(value: string | null) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleString('zh-CN', { hour12: false });
}
</script>

<style scoped>
.profile-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(260px, 0.85fr) minmax(320px, 1fr) minmax(320px, 1fr);
}

.profile-card {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 18px;
  box-shadow: var(--zy-shadow-sm);
  padding: 20px;
}

.profile-card__title {
  align-items: center;
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
}

.profile-card__title strong,
.profile-card__section-title strong {
  color: var(--zy-text-strong);
  display: block;
  font-size: var(--zy-fs-lg);
}

.profile-card__title small,
.profile-card__section-title span {
  color: var(--zy-text-muted);
  display: block;
  font-size: var(--zy-fs-sm);
  margin-top: 4px;
}

.profile-avatar {
  align-items: center;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  border-radius: 18px;
  color: #fff;
  display: inline-flex;
  font-size: 24px;
  font-weight: var(--zy-fw-bold);
  height: 56px;
  justify-content: center;
  width: 56px;
}

.profile-meta {
  display: grid;
  gap: 12px;
  margin: 0;
}

.profile-meta div {
  background: rgba(248, 250, 252, 0.86);
  border-radius: 12px;
  padding: 12px;
}

.profile-meta dt {
  color: var(--zy-text-muted);
  font-size: var(--zy-fs-xs);
  margin-bottom: 6px;
}

.profile-meta dd {
  color: var(--zy-text-strong);
  font-weight: var(--zy-fw-semi);
  margin: 0;
}

.profile-card__section-title {
  margin-bottom: 16px;
}

.profile-form :deep(.el-button) {
  width: 100%;
}

@media (max-width: 1200px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
