<template>
  <div class="login-shell">
    <section class="login-panel register-panel">
      <div class="login-brand">
        <span class="login-brand__eyebrow">员工试运行入口</span>
        <h1>账号注册</h1>
        <p>账号创建后可登录平台；如果还没有项目权限，请等待管理员授权。</p>
      </div>

      <el-form label-position="top" class="login-form" @submit.prevent="handleSubmit">
        <el-form-item label="用户名">
          <el-input v-model.trim="form.username" size="large" placeholder="例如 zhangsan，可留空默认用手机号" maxlength="32" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model.trim="form.phoneNumber" size="large" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model.trim="form.displayName" size="large" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model.trim="form.departmentName" size="large" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="请设置密码" />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input
            v-model="form.confirmPassword"
            size="large"
            type="password"
            show-password
            placeholder="请再次输入密码"
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
          创建账号
        </el-button>
      </el-form>

      <div class="login-helper">
        <span>已有账号？</span>
        <el-button text type="primary" @click="router.push({ name: 'login' })">返回登录</el-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

import { registerEmployee } from '@/modules/auth/api/auth';

const router = useRouter();
const submitting = ref(false);

const form = reactive({
  username: '',
  phoneNumber: '',
  displayName: '',
  departmentName: '',
  password: '',
  confirmPassword: ''
});

async function handleSubmit() {
  const error = validateForm();
  if (error) {
    ElMessage.error(error);
    return;
  }
  submitting.value = true;
  try {
    await registerEmployee({
      username: form.username || undefined,
      phoneNumber: form.phoneNumber,
      displayName: form.displayName,
      departmentName: form.departmentName || undefined,
      password: form.password
    });
    ElMessage.success('账号已创建，请登录；如暂无项目权限，请等待管理员授权。');
    router.replace({ name: 'login' });
  } catch (error) {
    const message = error instanceof Error ? error.message : '注册失败';
    ElMessage.error(message);
  } finally {
    submitting.value = false;
  }
}

function validateForm() {
  if (form.username && !/^[A-Za-z][A-Za-z0-9._-]{2,31}$/.test(form.username)) {
    return '用户名需以字母开头，支持字母、数字、点、下划线和短横线，长度 3-32 位';
  }
  if (!/^1[3-9]\d{9}$/.test(form.phoneNumber)) {
    return '请输入有效的手机号';
  }
  if (!form.displayName) {
    return '请输入姓名';
  }
  if (form.password.length < 6 || form.password.length > 64) {
    return '密码长度应为 6-64 位';
  }
  if (form.password !== form.confirmPassword) {
    return '两次输入的密码不一致';
  }
  return '';
}
</script>
