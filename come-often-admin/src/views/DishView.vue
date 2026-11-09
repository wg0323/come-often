<script setup lang="ts">
import { ref } from 'vue';
import CategoryTab from '@/components/dish/CategoryTab.vue';
import DishListTab from '@/components/dish/DishListTab.vue';
import OperationTab from '@/components/dish/OperationTab.vue';
import StockTab from '@/components/dish/StockTab.vue';

const activeTab = ref('dishes');
</script>

<template>
  <div>
    <h1 class="title">菜品管理</h1>
    <p class="subtitle">菜品、分类、每日库存与首页运营位；新增菜品可一次配置两规格一加料。</p>
    <el-card class="page-card" shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="菜品列表" name="dishes"><DishListTab /></el-tab-pane>
        <el-tab-pane label="分类管理" name="categories"><CategoryTab /></el-tab-pane>
        <el-tab-pane label="每日库存" name="stocks"><StockTab /></el-tab-pane>
        <el-tab-pane label="轮播与公告" name="operations"><OperationTab /></el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.title {
  margin: 0 0 8px;
  color: var(--co-text-primary);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 2px;
}

.subtitle {
  margin: 0 0 20px;
  color: var(--co-text-secondary);
}

.page-card {
  border-color: var(--co-divider);
  border-radius: var(--co-radius-card);
}
</style>
