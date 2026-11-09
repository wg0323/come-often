<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  reservationApi,
  type ReservationCalendarDay,
  type ReservationItem,
  type ReservationSlotItem,
  type ReservationTableBoard,
} from '@/api/reservation';

const STATUS_OPTIONS = [
  { value: 'PENDING', label: '待确认' },
  { value: 'CONFIRMED', label: '已确认' },
  { value: 'CHECKED_IN', label: '已到店' },
  { value: 'NO_SHOW', label: '未到店' },
  { value: 'CANCELLED', label: '已取消' },
];

const STATUS_TAG: Record<string, 'warning' | 'primary' | 'success' | 'danger' | 'info'> = {
  PENDING: 'warning',
  CONFIRMED: 'primary',
  CHECKED_IN: 'success',
  NO_SHOW: 'danger',
  CANCELLED: 'info',
};

const activeTab = ref('calendar');
const selectedDate = ref(formatDate(new Date()));
const auditDate = ref('');
const auditStatus = ref('');
const keyword = ref('');
const page = ref(1);
const size = ref(10);
const total = ref(0);
const listLoading = ref(false);
const boardLoading = ref(false);
const generating = ref(false);

const calendarDays = ref<ReservationCalendarDay[]>([]);
const slots = ref<ReservationSlotItem[]>([]);
const board = ref<ReservationTableBoard | null>(null);
const list = ref<ReservationItem[]>([]);

const detailVisible = ref(false);
const detail = ref<ReservationItem | null>(null);
const rejectVisible = ref(false);
const rejectNo = ref('');
const rejectRemark = ref('');
const codeInput = ref('');

const range = computed(() => {
  const start = selectedDate.value;
  const end = addDays(start, 6);
  return { start, end };
});

const todaySummary = computed(() => calendarDays.value.find((day) => day.date === selectedDate.value));

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const loadCalendar = async (): Promise<void> => {
  try {
    calendarDays.value = await reservationApi.calendar(range.value.start, range.value.end);
  } catch (error) {
    ElMessage.error(errorMessage(error, '预约日历加载失败'));
  }
};

const loadSlots = async (): Promise<void> => {
  try {
    slots.value = await reservationApi.slots(selectedDate.value);
  } catch (error) {
    ElMessage.error(errorMessage(error, '时段余量加载失败'));
  }
};

const loadList = async (): Promise<void> => {
  listLoading.value = true;
  try {
    const result = await reservationApi.page({
      date: auditDate.value || undefined,
      status: auditStatus.value || undefined,
      keyword: keyword.value.trim() || undefined,
      current: page.value,
      size: size.value,
    });
    list.value = result.records;
    total.value = result.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '预约列表加载失败'));
  } finally {
    listLoading.value = false;
  }
};

const loadBoard = async (): Promise<void> => {
  boardLoading.value = true;
  try {
    board.value = await reservationApi.tableBoard(selectedDate.value);
  } catch (error) {
    ElMessage.error(errorMessage(error, '桌台看板加载失败'));
  } finally {
    boardLoading.value = false;
  }
};

const refreshCalendarTab = async (): Promise<void> => {
  await Promise.all([loadCalendar(), loadSlots()]);
};

const refreshAuditTab = async (): Promise<void> => {
  page.value = 1;
  await loadList();
};

const refreshTableTab = async (): Promise<void> => {
  await loadBoard();
};

const onTabChange = async (name: string): Promise<void> => {
  if (name === 'calendar') {
    await refreshCalendarTab();
    return;
  }
  if (name === 'audit') {
    await refreshAuditTab();
    return;
  }
  await refreshTableTab();
};

const onSelectDate = async (date: string): Promise<void> => {
  selectedDate.value = date;
  await Promise.all([loadSlots(), loadCalendar()]);
  if (activeTab.value === 'tables') {
    await loadBoard();
  }
};

const generateSlots = async (): Promise<void> => {
  generating.value = true;
  try {
    const processed = await reservationApi.generate();
    ElMessage.success(`时段库存已生成，本次处理 ${processed} 个组合`);
    await refreshCalendarTab();
  } catch (error) {
    ElMessage.error(errorMessage(error, '生成失败'));
  } finally {
    generating.value = false;
  }
};

const rebuildKeys = async (): Promise<void> => {
  try {
    const count = await reservationApi.rebuild();
    ElMessage.success(`Redis 容量键已按 DB 重建，共 ${count} 个`);
  } catch (error) {
    ElMessage.error(errorMessage(error, '重建失败'));
  }
};

const openDetail = async (row: ReservationItem): Promise<void> => {
  try {
    detail.value = await reservationApi.detail(row.reserveNo);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(errorMessage(error, '详情加载失败'));
  }
};

const confirmReservation = async (row: ReservationItem): Promise<void> => {
  try {
    await reservationApi.confirm(row.reserveNo);
    ElMessage.success('已确认预约');
    await loadList();
  } catch (error) {
    ElMessage.error(errorMessage(error, '确认失败'));
  }
};

const openReject = (row: ReservationItem): void => {
  rejectNo.value = row.reserveNo;
  rejectRemark.value = '';
  rejectVisible.value = true;
};

const submitReject = async (): Promise<void> => {
  if (!rejectRemark.value.trim()) {
    ElMessage.warning('请填写拒绝原因');
    return;
  }
  try {
    await reservationApi.reject(rejectNo.value, rejectRemark.value.trim());
    rejectVisible.value = false;
    ElMessage.success('已拒绝并释放时段');
    await loadList();
  } catch (error) {
    ElMessage.error(errorMessage(error, '拒绝失败'));
  }
};

const checkinReservation = async (row: ReservationItem): Promise<void> => {
  try {
    await ElMessageBox.confirm(`确认为「${row.contactName}」签到？`, '到店签到', { type: 'info' });
  } catch {
    return;
  }
  try {
    await reservationApi.checkin(row.reserveNo);
    ElMessage.success('签到成功');
    await loadList();
  } catch (error) {
    ElMessage.error(errorMessage(error, '签到失败'));
  }
};

const submitCode = async (): Promise<void> => {
  if (!codeInput.value.trim()) {
    ElMessage.warning('请输入签到码');
    return;
  }
  try {
    const result = await reservationApi.checkinByCode(codeInput.value.trim());
    ElMessage.success(`签到成功：${result.contactName} ${result.reserveDate} ${result.timeSlot}`);
    codeInput.value = '';
    await loadList();
  } catch (error) {
    ElMessage.error(errorMessage(error, '签到码校验失败'));
  }
};

onMounted(async () => {
  await refreshCalendarTab();
});

function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function addDays(date: string, days: number): string {
  const value = new Date(`${date}T00:00:00`);
  value.setDate(value.getDate() + days);
  return formatDate(value);
}
</script>

<template>
  <div>
    <h1 class="title">预约管理</h1>
    <p class="subtitle">日历视图、到店审核与桌台状态；容量由桌台数据聚合，不写死桌号与容量。</p>

    <el-card class="page-card" shadow="never">
      <div class="toolbar">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          :clearable="false"
        />
        <el-button type="primary" :loading="generating" @click="generateSlots">生成时段库存</el-button>
        <el-button @click="rebuildKeys">重建容量键</el-button>
        <span class="muted" v-if="todaySummary">
          当日 {{ todaySummary.total }} 单，容量 {{ todaySummary.booked }}/{{ todaySummary.capacity }}
        </span>
      </div>

      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="日历视图" name="calendar">
          <div class="calendar">
            <div
              v-for="day in calendarDays"
              :key="day.date"
              class="calendar-cell"
              :class="{ 'calendar-cell--active': day.date === selectedDate }"
              @click="onSelectDate(day.date)"
            >
              <span class="calendar-date">{{ day.date.slice(5) }}</span>
              <span class="calendar-total">{{ day.total }} 单</span>
              <span class="calendar-detail">容量 {{ day.booked }}/{{ day.capacity }}</span>
              <span class="calendar-detail">待确认 {{ day.pending }}</span>
            </div>
          </div>

          <el-table :data="slots" border empty-text="该日期尚未生成时段库存">
            <el-table-column prop="timeSlot" label="时段" width="100" />
            <el-table-column prop="tableTypeText" label="桌型" width="180" />
            <el-table-column prop="capacity" label="容量" width="100" />
            <el-table-column prop="booked" label="已占" width="100" />
            <el-table-column prop="remaining" label="剩余" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.full ? 'danger' : 'success'">{{ row.full ? '已满' : '可预约' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="审核列表" name="audit">
          <div class="toolbar">
            <el-date-picker v-model="auditDate" type="date" value-format="YYYY-MM-DD" placeholder="按日期筛选" />
            <el-select v-model="auditStatus" placeholder="全部状态" clearable style="width: 160px">
              <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-input v-model="keyword" placeholder="预约单号 / 联系人 / 手机号前缀" style="width: 260px" />
            <el-button type="primary" @click="refreshAuditTab">查询</el-button>
          </div>

          <el-table v-loading="listLoading" :data="list" border empty-text="暂无预约">
            <el-table-column prop="reserveNo" label="预约单号" width="220" />
            <el-table-column label="预约时间" width="170">
              <template #default="{ row }">{{ row.reserveDate }} {{ row.timeSlot }}</template>
            </el-table-column>
            <el-table-column prop="tableTypeText" label="桌型" width="160" />
            <el-table-column prop="peopleCount" label="人数" width="80" />
            <el-table-column label="联系人" width="180">
              <template #default="{ row }">{{ row.contactName }} {{ row.contactPhone }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="STATUS_TAG[row.status] ?? 'info'">{{ row.statusText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button v-if="row.status === 'PENDING'" link type="primary" @click="confirmReservation(row)">确认</el-button>
                <el-button v-if="row.canCheckin" link type="success" @click="checkinReservation(row)">签到</el-button>
                <el-button v-if="row.status === 'PENDING'" link type="danger" @click="openReject(row)">拒绝</el-button>
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
            <div class="scan-box">
              <el-input v-model="codeInput" placeholder="扫码枪 / 手动输入 R 前缀签到码" style="width: 320px" />
              <el-button type="primary" @click="submitCode">扫码签到</el-button>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="桌台状态" name="tables">
          <div v-if="board" class="board">
            <div v-for="table in board.tables" :key="table.tableId" class="board-cell">
              <span class="board-no">{{ table.tableNo }}</span>
              <span class="board-type">{{ table.tableTypeText }}</span>
              <span class="board-meta">{{ table.capacity }} 人 · {{ table.statusText }}</span>
              <span class="board-meta" v-if="table.nextTimeSlot">下一预约 {{ table.nextTimeSlot }}</span>
              <span class="board-meta" v-else>暂无预约</span>
            </div>
          </div>

          <el-table v-if="board" v-loading="boardLoading" :data="board.slots" border empty-text="该日期尚未生成时段库存">
            <el-table-column prop="timeSlot" label="时段" width="100" />
            <el-table-column prop="tableTypeText" label="桌型" width="180" />
            <el-table-column prop="capacity" label="容量" width="100" />
            <el-table-column prop="booked" label="已占" width="100" />
            <el-table-column prop="remaining" label="剩余" width="100" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="detailVisible" title="预约详情" width="620px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="预约单号">{{ detail.reserveNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusText }}</el-descriptions-item>
        <el-descriptions-item label="预约时间">{{ detail.reserveDate }} {{ detail.timeSlot }}</el-descriptions-item>
        <el-descriptions-item label="桌型">{{ detail.tableTypeText }}</el-descriptions-item>
        <el-descriptions-item label="人数">{{ detail.peopleCount }}</el-descriptions-item>
        <el-descriptions-item label="桌号">{{ detail.tableNo ?? '未绑定' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactName }} {{ detail.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="会员">{{ detail.memberNickname ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '无' }}</el-descriptions-item>
      </el-descriptions>

      <el-table v-if="detail?.logs?.length" :data="detail.logs" class="log-table" border>
        <el-table-column prop="fromStatusText" label="原状态" width="110" />
        <el-table-column prop="toStatusText" label="新状态" width="110" />
        <el-table-column prop="remark" label="说明" />
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="拒绝预约" width="460px">
      <el-form label-width="90px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectRemark" type="textarea" :rows="3" placeholder="如：该时段包间已满" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认拒绝</el-button>
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

.calendar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.calendar-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 132px;
  padding: 12px;
  border: 1px solid var(--co-divider);
  border-radius: var(--co-radius-tag);
  background-color: var(--co-card);
  cursor: pointer;
}

.calendar-cell--active {
  border-color: var(--co-ink);
  box-shadow: 0 0 0 1px var(--co-ink) inset;
}

.calendar-date {
  color: var(--co-text-primary);
  font-size: 16px;
  font-weight: 600;
}

.calendar-total {
  color: var(--co-cinnabar);
  font-size: 13px;
}

.calendar-detail {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.pager {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
}

.scan-box {
  display: flex;
  gap: 8px;
}

.board {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.board-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 150px;
  padding: 12px;
  border: 1px solid var(--co-divider);
  border-radius: var(--co-radius-tag);
  background-color: var(--co-card);
}

.board-no {
  color: var(--co-text-primary);
  font-size: 16px;
  font-weight: 600;
}

.board-type {
  color: var(--co-gold);
  font-size: 12px;
}

.board-meta {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.log-table {
  margin-top: 16px;
}
</style>
