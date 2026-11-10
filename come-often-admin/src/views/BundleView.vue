<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { dishApi } from '@/api/dish';
import type { DishListItem } from '@/api/dish';
import { bundleApi } from '@/api/bundle';
import type { BundleItemPayload, BundleSavePayload, BundleVO } from '@/api/bundle';

interface BundleForm {
  id: string;
  bundleName: string;
  description: string;
  bundlePrice: number;
  originalPrice: number;
  totalStock: number;
  status: number;
  items: BundleItemPayload[];
}

const emptyForm = (): BundleForm => ({
  id: '',
  bundleName: '',
  description: '',
  bundlePrice: 0,
  originalPrice: 0,
  totalStock: 0,
  status: 1,
  items: [{ dishId: '', quantity: 1 }],
});

const loading = ref(false);
const submitting = ref(false);
const bundles = ref<BundleVO[]>([]);
const total = ref(0);
const current = ref(1);
const size = ref(10);
const keyword = ref('');
const statusFilter = ref('');
const dishes = ref<DishListItem[]>([]);
const dialogVisible = ref(false);
const form = reactive<BundleForm>(emptyForm());

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const loadBundles = async (): Promise<void> => {
  loading.value = true;
  try {
    const page = await bundleApi.page({
      current: current.value,
      size: size.value,
      keyword: keyword.value,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value),
    });
    bundles.value = page.records;
    total.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '套餐列表加载失败'));
  } finally {
    loading.value = false;
  }
};

const loadDishes = async (): Promise<void> => {
  try {
    const page = await dishApi.page({ current: 1, size: 200 });
    dishes.value = page.records;
  } catch (error) {
    ElMessage.error(errorMessage(error, '菜品列表加载失败'));
  }
};

const onQuery = async (): Promise<void> => {
  current.value = 1;
  await loadBundles();
};

const openCreate = (): void => {
  Object.assign(form, emptyForm());
  dialogVisible.value = true;
};

const openEdit = async (row: BundleVO): Promise<void> => {
  try {
    const detail = await bundleApi.detail(row.id);
    Object.assign(form, {
      id: detail.id,
      bundleName: detail.bundleName,
      description: detail.description ?? '',
      bundlePrice: detail.bundlePrice,
      originalPrice: detail.originalPrice ?? 0,
      totalStock: detail.totalStock,
      status: detail.status,
      items: detail.items.map((item) => ({ dishId: item.dishId, quantity: item.quantity })),
    });
    dialogVisible.value = true;
  } catch (error) {
    ElMessage.error(errorMessage(error, '套餐详情加载失败'));
  }
};

const addItem = (): void => {
  form.items.push({ dishId: '', quantity: 1 });
};

const removeItem = (index: number): void => {
  form.items.splice(index, 1);
};

const submit = async (): Promise<void> => {
  if (!form.bundleName.trim() || form.bundlePrice <= 0) {
    ElMessage.warning('请填写套餐名称与大于 0 的售价');
    return;
  }
  const items = form.items.filter((item) => item.dishId !== '' && item.quantity > 0);
  if (items.length === 0) {
    ElMessage.warning('套餐至少包含一道菜');
    return;
  }
  const payload: BundleSavePayload = {
    bundleName: form.bundleName.trim(),
    description: form.description.trim(),
    bundlePrice: form.bundlePrice,
    originalPrice: form.originalPrice > 0 ? form.originalPrice : null,
    totalStock: form.totalStock,
    status: form.status,
    items,
  };
  if (form.id) {
    payload.id = form.id;
  }
  submitting.value = true;
  try {
    await bundleApi.save(payload);
    ElMessage.success(form.id ? '套餐已更新' : '套餐已创建');
    dialogVisible.value = false;
    await loadBundles();
  } catch (error) {
    ElMessage.error(errorMessage(error, '保存失败'));
  } finally {
    submitting.value = false;
  }
};

const toggleStatus = async (row: BundleVO): Promise<void> => {
  try {
    await bundleApi.setStatus(row.id, row.status === 1 ? 0 : 1);
    ElMessage.success(row.status === 1 ? '已下架' : '已上架');
    await loadBundles();
  } catch (error) {
    ElMessage.error(errorMessage(error, '上下架失败'));
  }
};

const removeBundle = async (row: BundleVO): Promise<void> => {
  try {
    await ElMessageBox.confirm(`确认删除套餐「${row.bundleName}」？已购订单保留历史快照。`, '删除套餐', {
      type: 'warning',
    });
    await bundleApi.remove(row.id);
    ElMessage.success('套餐已删除');
    await loadBundles();
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(errorMessage(error, '删除失败'));
    }
  }
};

onMounted(async () => {
  await loadBundles();
  await loadDishes();
});
</script>

<template>
  <div v-loading="loading">
    <h1 class="title">团购套餐</h1>
    <p class="subtitle">
      套餐是独立商品，按套餐价结算；M13 的三张团餐券仍是营销券，互不影响。
      编辑明细是整体覆盖式，每次全量提交。
    </p>
    <el-card class="page-card" shadow="never">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          class="keyword-input"
          placeholder="套餐名称"
          clearable
          @keyup.enter="onQuery"
        />
        <el-select v-model="statusFilter" class="status-select" placeholder="全部状态" clearable>
          <el-option label="上架" value="1" />
          <el-option label="下架" value="0" />
        </el-select>
        <el-button type="primary" @click="onQuery">查询</el-button>
        <el-button type="success" @click="openCreate">新增套餐</el-button>
        <span class="muted">共 {{ total }} 个套餐</span>
      </div>

      <el-table :data="bundles" border empty-text="暂无团购套餐">
        <el-table-column prop="bundleName" label="套餐名称" min-width="160" />
        <el-table-column label="明细" min-width="240">
          <template #default="{ row }">
            <span v-for="item in row.items" :key="item.dishId" class="item-tag">
              {{ item.dishName }} × {{ item.quantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="120">
          <template #default="{ row }">¥{{ row.bundlePrice }}</template>
        </el-table-column>
        <el-table-column label="库存" width="140">
          <template #default="{ row }">{{ row.remainStock }} / {{ row.totalStock }}</template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="removeBundle(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="prev, pager, next"
        :total="total"
        :current-page="current"
        :page-size="size"
        @current-change="
          (page: number) => {
            current = page;
            loadBundles();
          }
        "
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑套餐' : '新增套餐'"
      width="720px"
      top="6vh"
    >
      <el-form label-width="120px">
        <el-form-item label="套餐名称">
          <el-input v-model="form.bundleName" placeholder="如 双人尝鲜套餐" />
        </el-form-item>
        <el-form-item label="套餐说明">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="售价（元）">
          <el-input-number v-model="form.bundlePrice" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="划线价（元）">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="总库存">
          <el-input-number v-model="form.totalStock" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="上架"
            inactive-text="下架"
          />
        </el-form-item>
        <el-form-item label="套餐明细">
          <div class="editor-block">
            <div v-for="(item, index) in form.items" :key="index" class="editor-row">
              <el-select v-model="item.dishId" placeholder="选择菜品" filterable class="dish-select">
                <el-option
                  v-for="dish in dishes"
                  :key="dish.id"
                  :label="dish.dishName"
                  :value="dish.id"
                />
              </el-select>
              <el-input-number v-model="item.quantity" :min="1" :max="99" />
              <el-button link type="danger" @click="removeItem(index)">移除</el-button>
            </div>
            <el-button link type="primary" @click="addItem">+ 添加一道菜</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.title {
  margin: 0;
  color: var(--co-text-primary);
  font-size: 28px;
  font-weight: 600;
}

.subtitle {
  margin: 8px 0 16px;
  color: var(--co-text-secondary);
  font-size: 14px;
}

.page-card {
  border: 1px solid var(--co-divider);
  border-radius: var(--co-radius-card);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.keyword-input {
  width: 220px;
}

.status-select {
  width: 140px;
}

.muted {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.item-tag {
  margin-right: 8px;
  color: var(--co-text-secondary);
  font-size: 12px;
}

.editor-block {
  width: 100%;
}

.editor-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.dish-select {
  flex: 1;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
