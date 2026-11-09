<script setup lang="ts">
import { computed } from 'vue';
import { RouterView, useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user';
import type { MenuItem } from '@/api/auth';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const isLoginPage = computed(() => route.path === '/login');
const menus = computed(() => userStore.menus);
const realName = computed(() => userStore.info?.realName ?? '');

const onMenuClick = (menu: MenuItem): void => {
  const routed = router.getRoutes().some((route) => route.path === menu.path);
  if (routed) {
    router.push(menu.path);
    return;
  }
  ElMessage.info(`${menu.name} 将在后续阶段接入`);
};

const onLogout = async (): Promise<void> => {
  try {
    await userStore.logout();
  } finally {
    await router.replace('/login');
  }
};
</script>

<template>
  <el-container v-if="!isLoginPage" class="app-shell">
    <el-aside class="app-aside" width="200px">
      <div class="brand">come-often</div>
      <nav class="menu">
        <a
          v-for="menu in menus"
          :key="menu.id"
          class="menu-item"
          :class="{ 'menu-item--active': route.path === menu.path }"
          href="javascript:void(0)"
          @click="onMenuClick(menu)"
        >
          {{ menu.name }}
        </a>
      </nav>
    </el-aside>
    <el-container>
      <el-header class="app-header" height="56px">
        <span>商家后台</span>
        <div class="header-right">
          <span class="user-name">{{ realName }}</span>
          <el-button link type="primary" @click="onLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
  <RouterView v-else />
</template>

<style scoped lang="scss">
.app-shell {
  min-height: 100vh;
}

.app-aside {
  background-color: var(--co-ink);
}

.brand {
  padding: 20px 16px;
  color: var(--co-card);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 2px;
}

.menu {
  display: flex;
  flex-direction: column;
}

.menu-item {
  padding: 12px 16px;
  color: var(--co-paper);
  text-decoration: none;
}

.menu-item--active {
  background-color: var(--co-cinnabar);
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--co-divider);
  background-color: var(--co-card);
  color: var(--co-text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  color: var(--co-text-secondary);
  font-size: 14px;
}

.app-main {
  padding: var(--co-space-page);
  background-color: var(--co-paper);
}
</style>
