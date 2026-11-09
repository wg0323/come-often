<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { mqApi, orderApi, refundApi } from '@/api/order';
import type { MqMessageItem, OrderDetail, OrderSummary, RefundItem } from '@/api/order';

interface StatusTab {
  label: string;
  value: string;
}

const STATUS_TABS: StatusTab[] = [
  { label: '全部', value: '' },
  { label: '待接单', value: 'PAID' },
  { label: '制作中', value: 'ACCEPTED' },
  { label: '待取餐', value: 'READY' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
];

const NEXT_ACTION: Record<string, { label: string; toStatus: string }> = {
  PAID: { label: '接单', toStatus: 'ACCEPTED' },
  ACCEPTED: { label: '出餐', toStatus: 'READY' },
  READY: { label: '确认取餐', toStatus: 'COMPLETED' },
};

const REFUND_TABS: StatusTab[] = [
  { label: '全部', value: '' },
  { label: '待审核', value: '0' },
  { label: '已退款', value: '1' },
  { label: '已拒绝', value: '2' },
];

const activeTab = ref('orders');
const activeStatus = ref('');
const keyword = ref('');
const current = ref(1);
const size = ref(10);
const total = ref(0);
const orders = ref<OrderSummary[]>([]);
const messages = ref<MqMessageItem[]>([]);
const messageTotal = ref(0);
const refunds = ref<RefundItem[]>([]);
const refundTotal = ref(0);
const refundStatus = ref('');
const refundCurrent = ref(1);
const refundSize = ref(10);
const refundOrderNo = ref('');
const loading = ref(false);
const detailVisible = ref(false);
const detail = ref<OrderDetail | null>(null);

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const loadOrders = async (): Promise<void> => {
  loading.value = true;
  try {
    const page = await orderApi.page({
      current: current.value,
      size: size.value,
      status: activeStatus.value,
      keyword: keyword.value,
    });
    orders.value = page.records;
    total.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '订单加载失败'));
  } finally {
    loading.value = false;
  }
};

const loadMessages = async (): Promise<void> => {
  try {
    const page = await mqApi.page({ current: 1, size: 10 });
    messages.value = page.records;
    messageTotal.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '本地消息加载失败'));
  }
};

const onStatusChange = async (): Promise<void> => {
  current.value = 1;
  await loadOrders();
};

const onPageChange = async (page: number): Promise<void> => {
  current.value = page;
  await loadOrders();
};

const openDetail = async (orderNo: string): Promise<void> => {
  try {
    detail.value = await orderApi.detail(orderNo);
    detailVisible.value = true;
  } catch (error) {
    ElMessage.error(errorMessage(error, '订单详情加载失败'));
  }
};

const transition = async (row: OrderSummary): Promise<void> => {
  const action = NEXT_ACTION[row.status];
  if (!action) {
    return;
  }
  try {
    await ElMessageBox.confirm(`确认对订单「${row.orderNo}」执行「${action.label}」？`, '推进订单', {
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await orderApi.transition(row.orderNo, { toStatus: action.toStatus, remark: action.label });
    ElMessage.success(`${action.label}成功`);
    await loadOrders();
  } catch (error) {
    ElMessage.error(errorMessage(error, '状态推进失败'));
  }
};

const republish = async (messageId: string): Promise<void> => {
  try {
    await mqApi.republish(messageId);
    ElMessage.success('已提交重放');
    await loadMessages();
  } catch (error) {
    ElMessage.error(errorMessage(error, '重放失败'));
  }
};

const loadRefunds = async (): Promise<void> => {
  loading.value = true;
  try {
    const page = await refundApi.page({
      current: refundCurrent.value,
      size: refundSize.value,
      status: refundStatus.value,
    });
    refunds.value = page.records;
    refundTotal.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '退款单加载失败'));
  } finally {
    loading.value = false;
  }
};

const onRefundStatusChange = async (): Promise<void> => {
  refundCurrent.value = 1;
  await loadRefunds();
};

const onRefundPageChange = async (page: number): Promise<void> => {
  refundCurrent.value = page;
  await loadRefunds();
};

const auditRefund = async (row: RefundItem, approved: boolean): Promise<void> => {
  const action = approved ? '通过退款' : '驳回退款';
  try {
    await ElMessageBox.confirm(`确认对退款单「${row.refundNo}」执行「${action}」？`, '退款审核', {
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await refundApi.audit(row.refundNo, { approved, remark: action });
    ElMessage.success(`${action}成功`);
    await loadRefunds();
    await loadOrders();
  } catch (error) {
    ElMessage.error(errorMessage(error, '审核失败'));
  }
};

const applyMerchantRefund = async (): Promise<void> => {
  const orderNo = refundOrderNo.value.trim();
  if (!orderNo) {
    ElMessage.warning('请输入订单号');
    return;
  }
  try {
    await ElMessageBox.confirm(`确认为订单「${orderNo}」主动发起退款？`, '商家主动退款', {
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await refundApi.applyByAdmin(orderNo, { reason: '商家无法履约，主动退还' });
    ElMessage.success('退款已提交');
    refundOrderNo.value = '';
    await loadRefunds();
    await loadOrders();
  } catch (error) {
    ElMessage.error(errorMessage(error, '发起退款失败'));
  }
};

onMounted(async () => {
  await loadOrders();
  await loadMessages();
  await loadRefunds();
});
</script>

<template>
  <div v-loading="loading">
    <h1 class="title">订单看板</h1>
    <p class="subtitle">接单、出餐、确认取餐与订单明细；含预定菜的订单强制到店用餐，定金在整单内抵扣。</p>
    <el-card class="page-card" shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="订单列表" name="orders">
          <div class="toolbar">
            <el-select v-model="activeStatus" class="status-select" @change="onStatusChange">
              <el-option v-for="tab in STATUS_TABS" :key="tab.value" :label="tab.label" :value="tab.value" />
            </el-select>
            <el-input v-model="keyword" class="keyword-input" placeholder="订单号" clearable @keyup.enter="onStatusChange" />
            <el-button type="primary" @click="onStatusChange">查询</el-button>
            <span class="muted">共 {{ total }} 单</span>
          </div>
          <el-table :data="orders" border empty-text="暂无订单">
            <el-table-column prop="orderNo" label="订单号" min-width="200" />
            <el-table-column label="类型" width="110">
              <template #default="{ row }">{{ row.orderType === 'DINE_IN' ? '到店用餐' : '自提' }}</template>
            </el-table-column>
            <el-table-column prop="itemSummary" label="菜品" min-width="160" />
            <el-table-column label="实付" width="100">
              <template #default="{ row }">¥{{ row.payAmount }}</template>
            </el-table-column>
            <el-table-column label="定金" width="100">
              <template #default="{ row }">¥{{ row.depositAmount }}</template>
            </el-table-column>
            <el-table-column prop="pickupCode" label="取餐码 / 核销码" width="150" />
            <el-table-column prop="statusText" label="状态" width="110" />
            <el-table-column prop="createTime" label="下单时间" width="170" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row.orderNo)">详情</el-button>
                <el-button
                  v-if="NEXT_ACTION[row.status]"
                  link
                  type="primary"
                  @click="transition(row)"
                >{{ NEXT_ACTION[row.status].label }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            layout="prev, pager, next"
            :current-page="current"
            :page-size="size"
            :total="total"
            @current-change="onPageChange"
          />
        </el-tab-pane>

        <el-tab-pane label="消息可靠性" name="messages">
          <div class="toolbar">
            <span class="muted">本地消息表共 {{ messageTotal }} 条；PENDING 由每分钟补偿任务重发，FAILED / DEAD 需人工处理</span>
            <el-button @click="loadMessages">刷新</el-button>
          </div>
          <el-table :data="messages" border empty-text="暂无本地消息">
            <el-table-column prop="messageId" label="消息 ID" min-width="260" />
            <el-table-column prop="routingKey" label="路由键" width="150" />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column prop="retryCount" label="重发次数" width="100" />
            <el-table-column prop="errorCause" label="失败原因" min-width="160" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button link type="primary" @click="republish(row.messageId)">重放</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="退款审核" name="refunds">
          <div class="toolbar">
            <el-select v-model="refundStatus" class="status-select" @change="onRefundStatusChange">
              <el-option v-for="tab in REFUND_TABS" :key="tab.value" :label="tab.label" :value="tab.value" />
            </el-select>
            <el-button @click="loadRefunds">刷新</el-button>
            <el-input v-model="refundOrderNo" class="keyword-input" placeholder="按订单号主动退款" clearable @keyup.enter="applyMerchantRefund" />
            <el-button type="primary" @click="applyMerchantRefund">发起退款</el-button>
            <span class="muted">共 {{ refundTotal }} 笔；制作中发起的退款在此审核，定金单由门店按订单号主动发起</span>
          </div>
          <el-table :data="refunds" border empty-text="暂无退款单">
            <el-table-column prop="refundNo" label="退款单号" min-width="200" />
            <el-table-column prop="orderNo" label="订单号" min-width="200" />
            <el-table-column label="退款金额" width="110">
              <template #default="{ row }">¥{{ row.refundAmount }}</template>
            </el-table-column>
            <el-table-column prop="reason" label="原因" min-width="160" />
            <el-table-column prop="statusText" label="状态" width="110" />
            <el-table-column prop="createTime" label="申请时间" width="170" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button link type="primary" @click="auditRefund(row, true)">通过</el-button>
                  <el-button link type="primary" @click="auditRefund(row, false)">驳回</el-button>
                </template>
                <span v-else class="muted">已处理</span>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            layout="prev, pager, next"
            :current-page="refundCurrent"
            :page-size="refundSize"
            :total="refundTotal"
            @current-change="onRefundPageChange"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="760px">
      <template v-if="detail">
        <div class="detail-head">
          <span class="detail-no">{{ detail.orderNo }}</span>
          <span class="detail-status">{{ detail.statusText }}</span>
          <span class="detail-code">{{ detail.pickupCode || '无取餐码' }}</span>
        </div>
        <div class="amount-grid">
          <span>商品总额</span><span>¥{{ detail.totalAmount }}</span>
          <span>定金（整单内抵扣）</span><span>¥{{ detail.depositAmount }}</span>
          <span>实付金额</span><span>¥{{ detail.payAmount }}</span>
          <span>到店待付尾款</span><span>¥{{ detail.balanceAmount }}</span>
        </div>
        <p v-if="detail.depositNote" class="note">{{ detail.depositNote }}</p>
        <p class="note">类型：{{ detail.orderType === 'DINE_IN' ? '到店用餐' : '自提' }}　备注：{{ detail.remark || '无' }}</p>
        <el-table :data="detail.items" border size="small" empty-text="无明细">
          <el-table-column prop="dishName" label="菜品" min-width="160" />
          <el-table-column prop="specDesc" label="规格" min-width="120" />
          <el-table-column prop="addonDesc" label="加料" min-width="120" />
          <el-table-column prop="quantity" label="份数" width="80" />
          <el-table-column prop="amount" label="小计" width="100" />
        </el-table>
        <p class="section-label">状态日志</p>
        <el-table :data="detail.logs" border size="small" empty-text="无日志">
          <el-table-column prop="fromStatus" label="原状态" width="120" />
          <el-table-column prop="toStatus" label="新状态" width="120" />
          <el-table-column prop="operatorType" label="操作方" width="110" />
          <el-table-column prop="remark" label="说明" min-width="180" />
          <el-table-column prop="createTime" label="时间" width="170" />
        </el-table>
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
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.status-select {
  width: 160px;
}

.keyword-input {
  width: 220px;
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

.detail-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-no {
  color: var(--co-text-primary);
  font-weight: 600;
}

.detail-status {
  color: var(--co-cinnabar);
}

.detail-code {
  color: var(--co-text-secondary);
}

.amount-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
  margin-bottom: 12px;
  padding: var(--co-space-card);
  border: 1px solid var(--co-divider);
  border-radius: var(--co-radius-button);
  color: var(--co-text-secondary);
}

.note {
  margin: 8px 0;
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.section-label {
  margin: 16px 0 8px;
  color: var(--co-text-primary);
  font-weight: 600;
}
</style>
