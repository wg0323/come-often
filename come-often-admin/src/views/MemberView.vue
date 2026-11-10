<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { pointsApi } from '@/api/points';
import type { MemberLevelItem, PointsMemberItem, PointsRecordItem, PointsRule } from '@/api/points';

interface RuleRow {
  label: string;
  value: string;
}

interface LevelForm {
  levelId: string;
  levelCode: string;
  levelName: string;
  minAmount: number;
  minPoints: number;
  discountRate: number;
  sort: number;
}

const activeTab = ref('rule');
const loading = ref(false);
const rule = ref<PointsRule | null>(null);
const levels = ref<MemberLevelItem[]>([]);
const members = ref<PointsMemberItem[]>([]);
const memberTotal = ref(0);
const memberCurrent = ref(1);
const memberSize = ref(10);
const keyword = ref('');
const records = ref<PointsRecordItem[]>([]);
const recordTotal = ref(0);
const recordCurrent = ref(1);
const recordSize = ref(10);
const recordMemberId = ref('');
const levelVisible = ref(false);
const levelForm = ref<LevelForm>({
  levelId: '',
  levelCode: '',
  levelName: '',
  minAmount: 0,
  minPoints: 0,
  discountRate: 1,
  sort: 1,
});

const ruleRows = computed<RuleRow[]>(() => {
  const value = rule.value;
  if (!value) {
    return [];
  }
  return [
    { label: '消费得积分', value: `每 1 元 +${value.orderEarnRate} 分，按实付金额向下取整` },
    { label: '积分抵现', value: `${value.exchangeRate} 积分抵 ¥1，抵现后订单至少保留 ¥0.01` },
    { label: '每日签到', value: `+${value.signInPoints} 分 / 天` },
    { label: '每日分享', value: `+${value.sharePoints} 分 / 天` },
    { label: '完善资料', value: `+${value.profilePoints} 分，终身一次` },
    { label: '评价', value: `+${value.reviewPoints} 分，提交评价后发放` },
    { label: '积分有效期', value: `${value.validDays} 天，每日 00:05 批处理过期` },
    { label: '等级判定', value: '年度消费额与累计积分同时达标才升级，只升不降' },
  ];
});

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const loadRule = async (): Promise<void> => {
  loading.value = true;
  try {
    rule.value = await pointsApi.rule();
  } catch (error) {
    ElMessage.error(errorMessage(error, '积分规则加载失败'));
  } finally {
    loading.value = false;
  }
};

const loadLevels = async (): Promise<void> => {
  try {
    levels.value = await pointsApi.levels();
  } catch (error) {
    ElMessage.error(errorMessage(error, '等级配置加载失败'));
  }
};

const loadMembers = async (): Promise<void> => {
  loading.value = true;
  try {
    const page = await pointsApi.pageMember({
      current: memberCurrent.value,
      size: memberSize.value,
      keyword: keyword.value,
    });
    members.value = page.records;
    memberTotal.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '会员积分加载失败'));
  } finally {
    loading.value = false;
  }
};

const loadRecords = async (): Promise<void> => {
  loading.value = true;
  try {
    const page = await pointsApi.pageRecord({
      current: recordCurrent.value,
      size: recordSize.value,
      memberId: recordMemberId.value,
    });
    records.value = page.records;
    recordTotal.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '积分流水加载失败'));
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

const onViewRecord = async (row: PointsMemberItem): Promise<void> => {
  recordMemberId.value = row.memberId;
  recordCurrent.value = 1;
  activeTab.value = 'records';
  await loadRecords();
};

const openLevelEdit = (row: MemberLevelItem): void => {
  levelForm.value = {
    levelId: row.levelId,
    levelCode: row.levelCode,
    levelName: row.levelName,
    minAmount: row.minAmount,
    minPoints: row.minPoints,
    discountRate: row.discountRate,
    sort: row.sort,
  };
  levelVisible.value = true;
};

const submitLevel = async (): Promise<void> => {
  try {
    await pointsApi.updateLevel(levelForm.value.levelId, {
      levelName: levelForm.value.levelName,
      minAmount: levelForm.value.minAmount,
      minPoints: levelForm.value.minPoints,
      discountRate: levelForm.value.discountRate,
      sort: levelForm.value.sort,
    });
    ElMessage.success('等级配置已更新');
    levelVisible.value = false;
    await loadLevels();
  } catch (error) {
    ElMessage.error(errorMessage(error, '等级配置保存失败'));
  }
};

const onRunExpire = async (): Promise<void> => {
  try {
    await ElMessageBox.confirm('将立即执行一次积分过期批处理，与每日 00:05 任务同一条链路。', '执行积分过期', {
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    const expired = await pointsApi.runExpire();
    ElMessage.success(`本次过期积分 ${expired} 分`);
    await loadRecords();
  } catch (error) {
    ElMessage.error(errorMessage(error, '积分过期执行失败'));
  }
};

onMounted(async () => {
  await loadRule();
  await loadLevels();
});
</script>

<template>
  <div v-loading="loading">
    <h1 class="title">用户管理</h1>
    <p class="subtitle">积分规则、等级配置与会员积分；余额、门槛与抵扣额一律取接口返回，页面不做业务计算。</p>
    <el-card class="page-card" shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="积分规则" name="rule">
          <div class="toolbar">
            <el-button @click="loadRule">刷新</el-button>
            <span class="muted">{{ rule ? rule.exchangeText : '规则加载中' }}</span>
          </div>
          <el-table :data="ruleRows" border empty-text="暂无积分规则">
            <el-table-column prop="label" label="规则项" width="180" />
            <el-table-column prop="value" label="取值与说明" min-width="320" />
          </el-table>
          <p class="hint">规则参数在 application-dev.yml 的 comeoften.points.* 中配置，可用环境变量覆盖；后台只读。</p>
        </el-tab-pane>

        <el-tab-pane label="等级配置" name="levels">
          <div class="toolbar">
            <el-button @click="loadLevels">刷新</el-button>
            <span class="muted">共 {{ levels.length }} 个等级，按排序值升序</span>
          </div>
          <el-table :data="levels" border empty-text="暂无等级配置">
            <el-table-column prop="levelName" label="等级名称" min-width="140" />
            <el-table-column prop="levelCode" label="编码" width="120" />
            <el-table-column label="年度消费额门槛" width="160">
              <template #default="{ row }">¥{{ row.minAmount }}</template>
            </el-table-column>
            <el-table-column prop="minPoints" label="累计积分门槛" width="140" />
            <el-table-column prop="discountRate" label="折扣率" width="110" />
            <el-table-column prop="sort" label="排序" width="90" />
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openLevelEdit(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
          <p class="hint">折扣率当前只作权益展示，未参与订单金额计算；等级在订单完成后自动结算。</p>
        </el-tab-pane>

        <el-tab-pane label="会员积分" name="members">
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
          <el-table :data="members" border empty-text="暂无会员">
            <el-table-column prop="nickname" label="昵称" min-width="140" />
            <el-table-column prop="phoneMasked" label="手机号" width="150" />
            <el-table-column prop="levelName" label="等级" width="120" />
            <el-table-column prop="balance" label="可用积分" width="110" />
            <el-table-column prop="totalEarned" label="累计获得" width="110" />
            <el-table-column prop="totalUsed" label="累计使用" width="110" />
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

        <el-tab-pane label="积分流水" name="records">
          <div class="toolbar">
            <el-input
              v-model="recordMemberId"
              class="keyword-input"
              placeholder="会员 ID，留空查全部"
              clearable
              @keyup.enter="onQueryRecord"
            />
            <el-button type="primary" @click="onQueryRecord">查询</el-button>
            <el-button @click="onRunExpire">执行积分过期</el-button>
            <span class="muted">共 {{ recordTotal }} 条流水，按发生时间倒序</span>
          </div>
          <el-table :data="records" border empty-text="暂无积分流水">
            <el-table-column prop="changeTypeText" label="方向" width="100" />
            <el-table-column label="变动" width="100">
              <template #default="{ row }">
                <span :class="row.changeAmount > 0 ? 'amount--earn' : 'amount--spend'">
                  {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="balanceAfter" label="变动后余额" width="120" />
            <el-table-column prop="bizType" label="业务类型" width="120" />
            <el-table-column prop="bizId" label="业务幂等键" min-width="200" />
            <el-table-column prop="remark" label="说明" min-width="200" />
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

    <el-dialog v-model="levelVisible" title="编辑等级配置" width="480px">
      <el-form label-width="130px">
        <el-form-item label="等级编码">
          <el-input :model-value="levelForm.levelCode" disabled />
        </el-form-item>
        <el-form-item label="等级名称">
          <el-input v-model="levelForm.levelName" placeholder="如 银卡会员" />
        </el-form-item>
        <el-form-item label="年度消费额">
          <el-input-number v-model="levelForm.minAmount" :min="0" :step="100" :precision="2" />
        </el-form-item>
        <el-form-item label="累计积分">
          <el-input-number v-model="levelForm.minPoints" :min="0" :step="100" />
        </el-form-item>
        <el-form-item label="折扣率">
          <el-input-number v-model="levelForm.discountRate" :min="0.01" :max="1" :step="0.05" :precision="2" />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="levelForm.sort" :min="1" :step="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelVisible = false">取消</el-button>
        <el-button type="primary" @click="submitLevel">保存</el-button>
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
  width: 240px;
}

.muted {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.hint {
  margin: 12px 0 0;
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
