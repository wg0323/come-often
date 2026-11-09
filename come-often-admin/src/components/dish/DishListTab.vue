<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { categoryApi, dishApi, stockApi, type DishCategory, type DishListItem } from '@/api/dish';
import { buildImageUrl } from '@/utils/image';
import DishFormDialog from './DishFormDialog.vue';

const rows = ref<DishListItem[]>([]);
const categories = ref<DishCategory[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const total = ref(0);

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  categoryId: '',
  status: undefined as number | undefined,
});

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const load = async (): Promise<void> => {
  loading.value = true;
  try {
    const page = await dishApi.page({
      current: query.current,
      size: query.size,
      keyword: query.keyword,
      categoryId: query.categoryId,
      status: query.status,
    });
    rows.value = page.records;
    total.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '菜品列表加载失败'));
  } finally {
    loading.value = false;
  }
};

const search = (): void => {
  query.current = 1;
  void load();
};

const openCreate = (): void => {
  editingId.value = null;
  dialogVisible.value = true;
};

const openEdit = (row: DishListItem): void => {
  editingId.value = row.id;
  dialogVisible.value = true;
};

const toggleStatus = async (row: DishListItem): Promise<void> => {
  try {
    await dishApi.setStatus(row.id, row.status === 1 ? 0 : 1);
    ElMessage.success(row.status === 1 ? '已下架，顾客端列表不再可见' : '已上架');
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '操作失败'));
  }
};

const toggleSoldOut = async (row: DishListItem): Promise<void> => {
  try {
    await stockApi.apply({ dishId: row.id, total: 50, soldOut: !row.soldOut });
    ElMessage.success(row.soldOut ? '已开售' : '已置为今日售罄');
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '操作失败'));
  }
};

const remove = async (row: DishListItem): Promise<void> => {
  try {
    await ElMessageBox.confirm(`确认删除「${row.dishName}」？`, '删除菜品', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await dishApi.remove(row.id);
    ElMessage.success('已删除');
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '删除失败'));
  }
};

onMounted(async () => {
  try {
    categories.value = await categoryApi.list();
  } catch (error) {
    ElMessage.error(errorMessage(error, '分类加载失败'));
  }
  await load();
});
</script>

<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="菜名关键词" class="toolbar-input" clearable @keyup.enter="search" />
      <el-select v-model="query.categoryId" placeholder="全部分类" clearable class="toolbar-select">
        <el-option v-for="category in categories" :key="category.id" :label="category.storageName" :value="category.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable class="toolbar-select">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-button @click="search">查询</el-button>
      <el-button type="primary" @click="openCreate">新增菜品并上架</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border empty-text="暂无菜品">
      <el-table-column label="列表图" width="100">
        <template #default="{ row }">
          <el-image v-if="row.listImage" :src="buildImageUrl(row.listImage)" fit="cover" class="dish-thumb" />
          <span v-else class="muted">无图</span>
        </template>
      </el-table-column>
      <el-table-column label="菜品" min-width="180">
        <template #default="{ row }">
          <div class="dish-name">{{ row.dishName }}</div>
          <div class="muted">{{ row.subtitle }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column label="售价" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="月售 / 技艺" min-width="140">
        <template #default="{ row }">{{ row.preorder ? row.craftInfo : `月售 ${row.monthlySales}` }}</template>
      </el-table-column>
      <el-table-column label="标签" min-width="120">
        <template #default="{ row }">
          <el-tag v-for="tag in row.tags" :key="tag" size="small" class="tag">{{ tag }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="库存" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.soldOut" type="danger" size="small">售罄</el-tag>
          <span v-else class="muted">在售</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link @click="toggleStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
          <el-button link @click="toggleSoldOut(row)">{{ row.soldOut ? '开售' : '售罄' }}</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="() => load()"
        @size-change="search"
      />
    </div>

    <DishFormDialog
      v-model="dialogVisible"
      :dish-id="editingId"
      :categories="categories"
      @saved="load"
    />
  </div>
</template>

<style scoped lang="scss">
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.toolbar-input {
  width: 200px;
}

.toolbar-select {
  width: 140px;
}

.dish-thumb {
  width: 72px;
  height: 54px;
  border-radius: var(--co-radius-tag);
}

.dish-name {
  color: var(--co-text-primary);
  font-weight: 600;
}

.muted {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.tag {
  margin-right: 4px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
