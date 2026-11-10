<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { balanceApi } from '@/api/balance';
import type { BalanceMemberItem, BalanceRecordItem } from '@/api/balance';

const activeTab = ref('members');
const loading = ref(false);
const members = ref<BalanceMemberItem[]>([]);
const memberTotal = ref(0);
const memberCurrent = ref(1);
const memberSize = ref(10);
const keyword = ref('');
const records = ref<BalanceRecordItem[]>([]);
const recordTotal = ref(0);
const recordCurrent = ref(1);
const recordSize = ref(10);
const recordMemberId = ref('');

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const loadMembers = async (): Promise<void> => {
  loading.value = true;
  try {
    const page = await balanceApi.pageMember({
      current: memberCurrent.value,
      size: memberSize.value,
      keyword: keyword.value,
    });
    members.value = page.records;
    memberTotal.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '储值会员加载失败'));
  } finally {
    loading.value = false;
  }
};

const loadRecords = async (): Promise<void> => {
  loading.value = true;
  try {
    const page = await balanceApi.pageRecord({
      current: recordCurrent.value,
      size: recordSize.value,
      memberId: recordMemberId.value,
    });
    records.value = page.records;
    recordTotal.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '储值流水加载失败'));
  } finally {
    loading.value = false;
  }
};

const onQueryMember = async (): Promise<void> => {
  memberCurrent.value = 1;
  await loadMembers();
};

const onQueryRecord = async (): Promise<void> => {
  recordCurrent.value = 1;
  await loadRecords();
};

const onViewRecord = async (row: BalanceMemberItem): Promise<void> => {
  recordMemberId.value = row.memberId;
  recordCurrent.value = 1;
  activeTab.value = 'records';
  await loadRecords();
};

onMounted(async () => {
  await loadMembers();
  await loadRecords();
});
</script>

<template>
  <div v-loading="loading">
    <h1 class="title">储值管理</h1>
    <p class="subtitle">
      余额、累计充值、赠送与消费全部由充值 / 下单 / 退款链路产生，后台不提供手工加减余额；
      余额消费按原单 pay_amount 之外的部分计入营收（口径见 M14 B5 报表）。
    </p>
    <el-card class="page-card" shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="储值会员" name="members">
          <div class="toolbar">
            <el-input
              v-model="keyword"
              class="keyword-input"
              placeholder="昵称或手机号"
              clearable
              @keyup.enter="onQueryMember"
            />
            <el-button type="primary" @click="onQueryMember">查询</el-button>
            <span class="muted">共 {{ memberTotal }} 名会员</span>
          </div>
          <el-table :data="members" border empty-text="暂无储值会员">
            <el-table-column prop="nickname" label="昵称" min-width="140" />
            <el-table-column prop="phoneMasked" label="手机号" width="150" />
            <el-table-column label="可用余额" width="130">
              <template #default="{ row }">¥{{ row.balance }}</template>
            </el-table-column>
            <el-table-column label="累计充值" width="130">
              <template #default="{ row }">¥{{ row.totalRecharge }}</template>
            </el-table-column>
            <el-table-column label="累计赠送" width="130">
              <template #default="{ row }">¥{{ row.totalGift }}</template>
            </el-table-column>
            <el-table-column label="累计消费" width="130">
              <template #default="{ row }">¥{{ row.totalConsume }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="onViewRecord(row)">查看流水</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            layout="prev, pager, next"
            :total="memberTotal"
            :current-page="memberCurrent"
            :page-size="memberSize"
            @current-change="
              (page: number) => {
                memberCurrent = page;
                loadMembers();
              }
            "
          />
        </el-tab-pane>

        <el-tab-pane label="储值流水" name="records">
          <div class="toolbar">
            <el-input
              v-model="recordMemberId"
              class="keyword-input"
              placeholder="会员 ID，留空查全部"
              clearable
              @keyup.enter="onQueryRecord"
            />
            <el-button type="primary" @click="onQueryRecord">查询</el-button>
            <span class="muted">共 {{ recordTotal }} 条，按发生时间倒序</span>
          </div>
          <el-table :data="records" border empty-text="暂无储值流水">
            <el-table-column prop="changeTypeText" label="方向" width="110" />
            <el-table-column label="变动" width="120">
              <template #default="{ row }">
                <span :class="row.changeAmount > 0 ? 'amount--earn' : 'amount--spend'">
                  {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="balanceAfter" label="变动后余额" width="130" />
            <el-table-column prop="bizType" label="业务类型" width="120" />
            <el-table-column prop="bizId" label="业务幂等键" min-width="220" />
            <el-table-column prop="remark" label="说明" min-width="220" />
            <el-table-column prop="createTime" label="发生时间" width="170" />
          </el-table>
          <el-pagination
            class="pager"
            layout="prev, pager, next"
            :total="recordTotal"
            :current-page="recordCurrent"
            :page-size="recordSize"
            @current-change="
              (page: number) => {
                recordCurrent = page;
                loadRecords();
              }
            "
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
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
  width: 240px;
}

.muted {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.amount--earn {
  color: var(--co-success);
}

.amount--spend {
  color: var(--co-text-secondary);
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
