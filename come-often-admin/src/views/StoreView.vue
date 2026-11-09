<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  storeApi,
  type StoreInfo,
  type StoreTablePayload,
  type StoreUpdatePayload,
} from '@/api/store';

const WEEK_DAYS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
const TABLE_TYPES = [
  { value: 'HALL_CARD', label: '大厅卡座' },
  { value: 'PRIVATE_SMALL', label: '小包间 6-10 人' },
  { value: 'PRIVATE_LARGE', label: '大包间 12-20 人' },
  { value: 'WINDOW', label: '靠窗' },
];

const activeTab = ref('info');
const storeId = ref('');
const loading = ref(false);
const saving = ref(false);
const tableDialog = ref(false);
const editingTableId = ref<string | null>(null);

const infoForm = reactive({
  storeName: '',
  address: '',
  phone: '',
  businessStatus: 1,
});

const hours = reactive<Array<{ weekDay: number; openTime: string; closeTime: string }>>(
  WEEK_DAYS.map((_, index) => ({ weekDay: index + 1, openTime: '10:00', closeTime: '21:30' })),
);

const tableForm = reactive<StoreTablePayload>({ tableNo: '', tableType: 'HALL_CARD', capacity: 4, status: 0 });
const tables = ref<StoreInfo['tables']>([]);

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const applyStore = (store: StoreInfo): void => {
  infoForm.storeName = store.storeName;
  infoForm.address = store.address;
  infoForm.phone = store.phone;
  infoForm.businessStatus = store.businessStatus;
  tables.value = store.tables;
  store.businessHours.forEach((hour) => {
    const target = hours.find((item) => item.weekDay === hour.weekDay);
    if (target) {
      target.openTime = hour.openTime;
      target.closeTime = hour.closeTime;
    }
  });
};

const load = async (): Promise<void> => {
  loading.value = true;
  try {
    const stores = await storeApi.list();
    const store = stores[0];
    if (!store) {
      ElMessage.warning('尚未配置门店');
      return;
    }
    storeId.value = store.id;
    applyStore(store);
  } catch (error) {
    ElMessage.error(errorMessage(error, '门店加载失败'));
  } finally {
    loading.value = false;
  }
};

const submitInfo = async (): Promise<void> => {
  if (!infoForm.storeName.trim()) {
    ElMessage.warning('请填写门店名称');
    return;
  }
  saving.value = true;
  try {
    const payload: StoreUpdatePayload = {
      storeName: infoForm.storeName,
      address: infoForm.address,
      phone: infoForm.phone,
      businessStatus: infoForm.businessStatus,
      businessHours: hours.map((hour) => ({ ...hour })),
    };
    await storeApi.update(storeId.value, payload);
    ElMessage.success('门店信息与营业时间已保存');
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '保存失败'));
  } finally {
    saving.value = false;
  }
};

const openTable = (row?: StoreInfo['tables'][number]): void => {
  editingTableId.value = row?.id ?? null;
  Object.assign(tableForm, row
    ? { tableNo: row.tableNo, tableType: row.tableType, capacity: row.capacity, status: row.status }
    : { tableNo: '', tableType: 'HALL_CARD', capacity: 4, status: 0 });
  tableDialog.value = true;
};

const submitTable = async (): Promise<void> => {
  if (!tableForm.tableNo.trim()) {
    ElMessage.warning('请填写桌号');
    return;
  }
  try {
    if (editingTableId.value) {
      await storeApi.updateTable(storeId.value, editingTableId.value, tableForm);
    } else {
      await storeApi.createTable(storeId.value, tableForm);
    }
    tableDialog.value = false;
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '桌台保存失败'));
  }
};

const removeTable = async (row: StoreInfo['tables'][number]): Promise<void> => {
  try {
    await ElMessageBox.confirm(`确认删除桌台「${row.tableNo}」？`, '删除桌台', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await storeApi.removeTable(storeId.value, row.id);
    await load();
  } catch (error) {
    ElMessage.error(errorMessage(error, '删除失败'));
  }
};

onMounted(load);
</script>

<template>
  <div v-loading="loading">
    <h1 class="title">门店管理</h1>
    <p class="subtitle">门店信息、一周营业时间与桌台；桌号与容量全部由数据配置。</p>
    <el-card class="page-card" shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="门店信息" name="info">
          <el-form label-width="120px" class="form-block">
            <el-form-item label="门店名称"><el-input v-model="infoForm.storeName" /></el-form-item>
            <el-form-item label="详细地址"><el-input v-model="infoForm.address" /></el-form-item>
            <el-form-item label="门店电话"><el-input v-model="infoForm.phone" /></el-form-item>
            <el-form-item label="营业状态">
              <el-switch v-model="infoForm.businessStatus" :active-value="1" :inactive-value="0" active-text="营业中" inactive-text="休息中" />
            </el-form-item>
            <el-form-item label="营业时间">
              <div class="hours">
                <div v-for="hour in hours" :key="hour.weekDay" class="hour-row">
                  <span class="hour-label">{{ WEEK_DAYS[hour.weekDay - 1] }}</span>
                  <el-input v-model="hour.openTime" class="hour-input" placeholder="10:00" />
                  <span class="muted">至</span>
                  <el-input v-model="hour.closeTime" class="hour-input" placeholder="21:30" />
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="submitInfo">保存门店与营业时间</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="桌台" name="tables">
          <div class="toolbar">
            <el-button type="primary" @click="openTable()">新增桌台</el-button>
            <span class="muted">共 {{ tables.length }} 张桌台</span>
          </div>
          <el-table :data="tables" border empty-text="暂无桌台">
            <el-table-column prop="tableNo" label="桌号" width="120" />
            <el-table-column label="桌型" width="180">
              <template #default="{ row }">{{ TABLE_TYPES.find((item) => item.value === row.tableType)?.label ?? row.tableType }}</template>
            </el-table-column>
            <el-table-column prop="capacity" label="容纳人数" width="120" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">{{ ['空闲', '占用', '已预约'][row.status] ?? '空闲' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button link type="primary" @click="openTable(row)">编辑</el-button>
                <el-button link type="danger" @click="removeTable(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="tableDialog" :title="editingTableId ? '编辑桌台' : '新增桌台'" width="440px">
      <el-form label-width="110px">
        <el-form-item label="桌号"><el-input v-model="tableForm.tableNo" placeholder="如 A01" /></el-form-item>
        <el-form-item label="桌型">
          <el-select v-model="tableForm.tableType" style="width: 100%">
            <el-option v-for="item in TABLE_TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="容纳人数"><el-input-number v-model="tableForm.capacity" :min="1" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="tableForm.status" style="width: 100%">
            <el-option label="空闲" :value="0" />
            <el-option label="占用" :value="1" />
            <el-option label="已预约" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tableDialog = false">取消</el-button>
        <el-button type="primary" @click="submitTable">保存</el-button>
      </template>
    </el-dialog>
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

.form-block {
  max-width: 620px;
  padding-top: 12px;
}

.hours {
  width: 100%;
}

.hour-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.hour-label {
  width: 48px;
  color: var(--co-text-secondary);
}

.hour-input {
  width: 110px;
}

.muted {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
