<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { queueApi, type QueueTicketItem } from '@/api/queue';

const STATUS_OPTIONS = [
  { value: 'WAITING', label: '等待中' },
  { value: 'CALLED', label: '已叫号' },
  { value: 'SEATED', label: '已入座' },
  { value: 'EXPIRED', label: '已过号' },
  { value: 'CANCELLED', label: '已取消' },
];

const STATUS_TAG: Record<string, 'warning' | 'primary' | 'success' | 'danger' | 'info'> = {
  WAITING: 'warning',
  CALLED: 'primary',
  SEATED: 'success',
  EXPIRED: 'danger',
  CANCELLED: 'info',
};

/** 与顾客端轮询同频：都是 10 秒，后台点叫号后顾客端最迟 10 秒内看到 */
const AUTO_REFRESH_MS = 10_000;

const status = ref('');
const page = ref(1);
const size = ref(10);
const total = ref(0);
const loading = ref(false);
const actingNo = ref('');
const autoRefresh = ref(true);
const list = ref<QueueTicketItem[]>([]);

let timer: ReturnType<typeof setInterval> | undefined;

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const loadList = async (): Promise<void> => {
  loading.value = true;
  try {
    const result = await queueApi.page({
      status: status.value || undefined,
      current: page.value,
      size: size.value,
    });
    list.value = result.records;
    total.value = result.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '排队列表加载失败'));
  } finally {
    loading.value = false;
  }
};

const refresh = async (): Promise<void> => {
  page.value = 1;
  await loadList();
};

const onStatusChange = async (): Promise<void> => {
  await refresh();
};

const stopAutoRefresh = (): void => {
  if (timer !== undefined) {
    clearInterval(timer);
    timer = undefined;
  }
};

const startAutoRefresh = (): void => {
  stopAutoRefresh();
  timer = setInterval(() => {
    if (autoRefresh.value) {
      void loadList();
    }
  }, AUTO_REFRESH_MS);
};

const onAutoRefreshChange = (value: boolean): void => {
  autoRefresh.value = value;
};

const callTicket = async (row: QueueTicketItem): Promise<void> => {
  try {
    await ElMessageBox.confirm(
      `叫号 ${row.ticketNo}（${row.partySize} 位）？顾客端会在 10 秒内看到叫号结果。`,
      '叫号确认',
      { type: 'info' },
    );
  } catch {
    return;
  }
  actingNo.value = row.ticketNo;
  try {
    await queueApi.call(row.ticketNo);
    ElMessage.success(`${row.ticketNo} 已叫号`);
    await loadList();
  } catch (error) {
    ElMessage.error(errorMessage(error, '叫号失败'));
  } finally {
    actingNo.value = '';
  }
};

const seatTicket = async (row: QueueTicketItem): Promise<void> => {
  try {
    await ElMessageBox.confirm(`确认 ${row.ticketNo} 已入座？`, '入座确认', { type: 'info' });
  } catch {
    return;
  }
  actingNo.value = row.ticketNo;
  try {
    await queueApi.seat(row.ticketNo);
    ElMessage.success(`${row.ticketNo} 已入座`);
    await loadList();
  } catch (error) {
    ElMessage.error(errorMessage(error, '入座失败'));
  } finally {
    actingNo.value = '';
  }
};

onMounted(async () => {
  await loadList();
  startAutoRefresh();
});

onUnmounted(() => {
  stopAutoRefresh();
});
</script>

<template>
  <div>
    <h1 class="title">排队叫号</h1>
    <p class="subtitle">当天排队看板；叫号后顾客端前台页面 10 秒内自动刷新，离页即停止轮询。</p>

    <el-card class="page-card" shadow="never">
      <div class="toolbar">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 160px" @change="onStatusChange">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" @click="refresh">查询</el-button>
        <el-switch
          :model-value="autoRefresh"
          active-text="每 10 秒自动刷新"
          @update:model-value="onAutoRefreshChange"
        />
        <span class="muted">只显示当天排队号；昨天的等待号由 00:20 的过号任务统一置为已过号。</span>
      </div>

      <el-table v-loading="loading" :data="list" border empty-text="今天还没有排队号">
        <el-table-column prop="ticketNo" label="排队号" width="110" />
        <el-table-column prop="partySize" label="人数" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="前方等待" width="110">
          <template #default="{ row }">{{ row.status === 'WAITING' ? `${row.waitingCount} 桌` : '—' }}</template>
        </el-table-column>
        <el-table-column label="联系方式" width="150">
          <template #default="{ row }">{{ row.memberPhone || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="取号时间" width="180" />
        <el-table-column label="叫号时间" width="180">
          <template #default="{ row }">{{ row.calledTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'WAITING'"
              link
              type="primary"
              :loading="actingNo === row.ticketNo"
              @click="callTicket(row)"
            >
              叫号
            </el-button>
            <el-button
              v-if="row.status === 'CALLED'"
              link
              type="success"
              :loading="actingNo === row.ticketNo"
              @click="seatTicket(row)"
            >
              入座
            </el-button>
            <span v-if="row.status !== 'WAITING' && row.status !== 'CALLED'" class="muted">无操作</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadList"
        />
      </div>
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

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.muted {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
