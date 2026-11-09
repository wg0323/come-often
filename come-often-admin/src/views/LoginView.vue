<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user';
import { ApiError } from '@/api/request';

const username = ref('admin');
const password = ref('');
const loading = ref(false);
const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const submit = async (): Promise<void> => {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入账号与密码');
    return;
  }
  loading.value = true;
  try {
    await userStore.login(username.value, password.value);
    ElMessage.success('登录成功');
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/';
    await router.replace(redirect);
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '登录失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="never">
      <h1 class="login-title">商家后台</h1>
      <p class="login-subtitle">员工账号登录</p>
      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="登录账号">
          <el-input v-model="username" autocomplete="username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button class="login-button" type="primary" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
      <p class="login-hint">本地初始账号：admin / manager / waiter，初始密码 Come@2026</p>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: var(--co-paper);
}

.login-card {
  width: 380px;
  padding: 16px 8px;
  border-color: var(--co-divider);
  border-radius: var(--co-radius-card);
}

.login-title {
  margin: 0;
  color: var(--co-text-primary);
  font-size: 26px;
  font-weight: 600;
  letter-spacing: 2px;
}

.login-subtitle {
  margin: 6px 0 24px;
  color: var(--co-text-secondary);
  font-size: 14px;
}

.login-button {
  width: 100%;
}

.login-hint {
  margin: 18px 0 0;
  color: var(--co-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}
</style>
