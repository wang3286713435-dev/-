<template>
  <div ref="shellRef" class="login-shell">
    <div class="zy-hero-lightfield" aria-hidden="true">
      <span></span>
      <span></span>
      <span></span>
    </div>
    <div class="zy-spotlight" aria-hidden="true"></div>
    <ParticleField :count="22" :speed="0.22" :link-distance="160" />
    <section class="login-panel zy-glass">
      <div class="login-brand">
        <span class="login-brand__eyebrow">ZHUOYU · DATA HUB</span>
        <h1>卓羽智能数据中台</h1>
        <p>建筑空间的数据底座 · 从 BIM 模型到运营闭环</p>
      </div>

      <el-alert type="info" :closable="false" show-icon>
        <template #title>请使用管理员下发的手机号 / 用户名和初始密码登录。</template>
      </el-alert>

      <el-form
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="手机号 / 用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="请输入手机号或用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            placeholder="请输入密码"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="login-form__submit"
          :loading="submitting"
          @click="handleSubmit"
        >
          登录
        </el-button>
      </el-form>

      <div class="login-helper">
        <span>还没有账号？</span>
        <el-button text type="primary" @click="router.push({ name: 'register' })">账号注册</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';

import ParticleField from '@/modules/core/components/ParticleField.vue';
import { useSpotlight } from '@/modules/core/composables/useSpotlight';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const submitting = ref(false);
const shellRef = ref<HTMLElement | null>(null);

useSpotlight(shellRef);

onMounted(() => {
  if (route.query.fresh === '1' || route.query.fresh === 'true') {
    authStore.reset();
  }
});

const form = reactive({
  username: '',
  password: ''
});

const rules = {
  username: [{ required: true, message: '请输入手机号或用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

async function handleSubmit() {
  submitting.value = true;
  try {
    authStore.reset();
    await authStore.signIn(form.username, form.password);
    ElMessage.success('登录成功');
    const target = authStore.currentUser?.projects.length === 0 ? 'access-pending' : 'data-steward-assets';
    router.push({ name: target });
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败';
    ElMessage.error(message);
  } finally {
    submitting.value = false;
  }
}
</script>
