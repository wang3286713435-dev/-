<template>
  <div class="login-shell">
    <section class="login-panel">
      <div class="login-brand">
        <span class="login-brand__eyebrow">数字化交付平台 v1</span>
        <h1>登录平台</h1>
        <p>进入样板项目，验证一期基础工程链路。</p>
      </div>

      <el-alert type="info" :closable="false" show-icon>
        <template #title>样板账号：platform.admin / Admin@123</template>
      </el-alert>

      <el-form
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="请输入用户名" />
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
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const router = useRouter();
const submitting = ref(false);

const form = reactive({
  username: 'platform.admin',
  password: 'Admin@123'
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
};

async function handleSubmit() {
  submitting.value = true;
  try {
    await authStore.signIn(form.username, form.password);
    ElMessage.success('登录成功');
    router.push({ name: 'home' });
  } catch (error) {
    const message = error instanceof Error ? error.message : '登录失败';
    ElMessage.error(message);
  } finally {
    submitting.value = false;
  }
}
</script>
