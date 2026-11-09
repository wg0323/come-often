<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { categoryApi, type CategorySavePayload, type DishCategory } from '@/api/dish';

const rows = ref<DishCategory[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const editingId = ref<string | null>(null);
const saving = ref(false);
const form = reactive<CategorySavePayload>({ categoryName: '', sort: 0, show: true });

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const load = async (): Promise<void> => {
  loading.value = true;
  try {
    rows.value = await categoryApi.list();
  } catch (error) {
    ElMessage.error(errorMessage(error, '分类加载失败'));
  } finally {
    loading.value = false;
  }
};

const openCreate = (): void => {
  editingId.value = null;
  Object.assign(form, { categoryName: '', sort: rows.value.length + 1, show: true });
  dialogVisible.value = true;
};

const openEdit = (row: DishCategory): void => {
  editingId.value = row.id;
  Object.assign(form, { categoryName: row.storageName, sort: row.sort, show: true });
  dialogVisible.value = true;
};

const submit = async (): Promise<void> => {
  if (!form.categoryName?.trim()) {
    ElMessage.warning('请填写分类名称');
    return;
  }
  saving.value = true;
  try {
    if (editingId.value) {
      await categoryApi.update(editingId.value, form);
    } else {
      await categoryApi.create(form);
    }
    ElMessage.success('分类已保存');
    dialogVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '保存失败'));
  } finally {
    saving.value = false;
  }
};

const remove = async (row: DishCategory): Promise<void> => {
  try {
    await ElMessageBox.confirm(`确认删除「${row.storageName}」？`, '删除分类', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await categoryApi.remove(row.id);
    ElMessage.success('已删除');
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '删除失败'));
  }
};

onMounted(load);
</script>

<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增分类</el-button>
      <span class="muted">预定类后台存「预定」，顾客端自动显示「需提前预定」</span>
    </div>
    <el-table v-loading="loading" :data="rows" border empty-text="暂无分类">
      <el-table-column prop="storageName" label="后台名称" width="160" />
      <el-table-column prop="categoryName" label="顾客端展示名" width="180" />
      <el-table-column prop="dishCount" label="上架菜品数" width="120" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="420px">
      <el-form label-width="100px">
        <el-form-item label="分类名称">
          <el-input v-model="form.categoryName" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="是否展示">
          <el-switch v-model="form.show" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
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
