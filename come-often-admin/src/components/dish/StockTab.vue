<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { stockApi, type DishStock } from '@/api/dish';

const rows = ref<DishStock[]>([]);
const loading = ref(false);

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const load = async (): Promise<void> => {
  loading.value = true;
  try {
    rows.value = await stockApi.list();
  } catch (error) {
    ElMessage.error(errorMessage(error, '库存加载失败'));
  } finally {
    loading.value = false;
  }
};

const setSoldOut = async (row: DishStock, soldOut: boolean): Promise<void> => {
  try {
    await stockApi.apply({ dishId: row.dishId, total: 50, soldOut });
    ElMessage.success(soldOut ? '已置为今日售罄，下单校验将被拒' : '已开售');
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '操作失败'));
  }
};

const rebuild = async (): Promise<void> => {
  try {
    const count = await stockApi.rebuild();
    ElMessage.success(`已按 DB 重建 ${count} 个当日库存键`);
  } catch (error) {
    ElMessage.error(errorMessage(error, '重建失败'));
  }
};

onMounted(load);
</script>

<template>
  <div>
    <div class="toolbar">
      <el-button @click="rebuild">按 DB 重建 Redis 库存键</el-button>
      <span class="muted">DB 的 remain 为最终事实源；未配置当日库存表示不限量</span>
    </div>
    <el-table v-loading="loading" :data="rows" border empty-text="暂无上架菜品" max-height="460">
      <el-table-column prop="dishName" label="菜品" min-width="180" />
      <el-table-column prop="stockDate" label="库存日期" width="130" />
      <el-table-column prop="total" label="备货" width="100" />
      <el-table-column prop="remain" label="剩余" width="100" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.soldOut" type="danger" size="small">售罄</el-tag>
          <el-tag v-else-if="row.configured" type="success" size="small">在售</el-tag>
          <el-tag v-else type="info" size="small">未配置</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="danger" @click="setSoldOut(row, true)">一键售罄</el-button>
          <el-button link @click="setSoldOut(row, false)">开售(50)</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.muted {
  color: var(--co-text-tertiary);
  font-size: 12px;
}
</style>
