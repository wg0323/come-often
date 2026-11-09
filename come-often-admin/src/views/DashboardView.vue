<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { statsApi } from '@/api/stats';
import type { StatsDashboard } from '@/api/stats';

interface ReservationCountItem {
  label: string;
  value: number;
}

/** PC 看板不是操作台，60 秒刷新一次够了；M9 商家端是 10 秒。 */
const POLL_INTERVAL_MS = 60000;

const loading = ref(false);
const dashboard = ref<StatsDashboard | null>(null);
let pollTimer: number | null = null;

const summary = computed(() => dashboard.value?.summary ?? null);
const reservations = computed(() => dashboard.value?.reservations ?? null);

const reservationCounts = computed<ReservationCountItem[]>(() => {
  const day = reservations.value;
  if (!day) {
    return [];
  }
  return [
    { label: '待确认', value: day.pending },
    { label: '已确认', value: day.confirmed },
    { label: '已到店', value: day.checkedIn },
    { label: '未到店', value: day.noShow },
    { label: '已取消', value: day.cancelled },
  ];
});

const formatMoney = (value: number | null | undefined): string =>
  value === null || value === undefined ? '—' : `¥${value.toFixed(2)}`;

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

/**
 * 轮询静默刷新：不显示整页 loading、失败也不弹提示，避免每分钟闪一次遮罩或刷屏报错。
 */
const fetchDashboard = async (showMask: boolean): Promise<void> => {
  if (showMask) {
    loading.value = true;
  }
  try {
    dashboard.value = await statsApi.dashboard();
  } catch (error) {
    if (showMask) {
      ElMessage.error(errorMessage(error, '经营看板加载失败'));
    }
  } finally {
    if (showMask) {
      loading.value = false;
    }
  }
};

const load = async (): Promise<void> => {
  await fetchDashboard(true);
};

const onPoll = (): void => {
  if (document.visibilityState !== 'visible') {
    return;
  }
  void fetchDashboard(false);
};

onMounted(async () => {
  await load();
  pollTimer = window.setInterval(onPoll, POLL_INTERVAL_MS);
});

onUnmounted(() => {
  if (pollTimer !== null) {
    window.clearInterval(pollTimer);
    pollTimer = null;
  }
});
</script>

<template>
  <div v-loading="loading" class="page">
    <div class="page-head">
      <div>
        <h1 class="title">经营看板</h1>
        <p class="subtitle">
          今天怎么样：净营收已扣退款，客单价 = 净营收 ÷ 有效订单数；预约与热销为今日口径。
        </p>
      </div>
      <div class="head-actions">
        <span class="muted">
          {{ dashboard ? `${dashboard.statDate} ${dashboard.updatedAt} 刷新 · 每 60 秒自动刷新` : '加载中' }}
        </span>
        <el-button :loading="loading" @click="load">立即刷新</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <el-card class="metric-card" shadow="never">
        <p class="metric-label">今日净营收</p>
        <p class="metric-value">{{ formatMoney(summary?.netRevenue) }}</p>
        <p class="metric-note">退款冲减 {{ formatMoney(summary?.refundAmount) }}</p>
      </el-card>
      <el-card class="metric-card" shadow="never">
        <p class="metric-label">今日订单数</p>
        <p class="metric-value">{{ summary?.orderCount ?? 0 }}</p>
        <p class="metric-note">不含已取消与整单退款</p>
      </el-card>
      <el-card class="metric-card" shadow="never">
        <p class="metric-label">今日客单价</p>
        <p class="metric-value">{{ formatMoney(summary?.avgOrderAmount) }}</p>
        <p class="metric-note">净营收 ÷ 有效订单数</p>
      </el-card>
    </div>

    <div class="board-grid">
      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-head">
            <span class="card-title">今日预约</span>
            <span class="muted">共 {{ reservations?.total ?? 0 }} 单（不含已取消）</span>
          </div>
        </template>
        <div class="status-row">
          <div v-for="item in reservationCounts" :key="item.label" class="status-item">
            <span class="status-value">{{ item.value }}</span>
            <span class="status-label">{{ item.label }}</span>
          </div>
        </div>
        <el-table :data="reservations?.slots ?? []" border empty-text="今日时段容量尚未生成" max-height="260">
          <el-table-column prop="timeSlot" label="时段" width="90" />
          <el-table-column label="已订 / 容量">
            <template #default="{ row }">{{ row.booked }} / {{ row.capacity }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-head">
            <span class="card-title">今日热销 Top 5</span>
            <span class="muted">销量口径，同一菜品不同规格合并</span>
          </div>
        </template>
        <el-table :data="dashboard?.hotDishes ?? []" border empty-text="今日还没有已支付订单">
          <el-table-column prop="rank" label="#" width="56" />
          <el-table-column prop="dishName" label="菜品" min-width="140" />
          <el-table-column prop="quantity" label="销量" width="80" />
          <el-table-column label="销售额" width="110">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.title {
  margin: 0 0 8px;
  color: var(--co-text-primary);
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 2px;
}

.subtitle {
  margin: 0;
  color: var(--co-text-secondary);
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.muted {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--co-space-card);
  margin-bottom: var(--co-space-card);
}

.metric-card,
.page-card {
  border-color: var(--co-divider);
  border-radius: var(--co-radius-card);
}

.metric-label {
  margin: 0 0 8px;
  color: var(--co-text-secondary);
  font-size: 14px;
}

.metric-value {
  margin: 0 0 6px;
  color: var(--co-text-primary);
  font-size: 28px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.metric-note {
  margin: 0;
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.board-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--co-space-card);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  color: var(--co-text-primary);
  font-weight: 600;
}

.status-row {
  display: flex;
  gap: 8px;
  margin-bottom: var(--co-space-card);
}

.status-item {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
  border: 1px solid var(--co-divider);
  border-radius: var(--co-radius-tag);
}

.status-value {
  color: var(--co-text-primary);
  font-size: 20px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.status-label {
  color: var(--co-text-tertiary);
  font-size: 12px;
}
</style>
