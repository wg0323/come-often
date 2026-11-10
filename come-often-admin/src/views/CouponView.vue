<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { COUPON_TYPES, couponApi } from '@/api/coupon';
import type {
  CouponRoiItem,
  CouponRuleItem,
  CouponRulePayload,
  CouponTemplateItem,
  CouponTemplatePayload,
  CouponUseRecordItem,
  CouponVerifyResult,
} from '@/api/coupon';
import { dishApi } from '@/api/dish';
import type { DishListItem } from '@/api/dish';

interface StatusTab {
  label: string;
  value: number | '';
}

const STATUS_TABS: StatusTab[] = [
  { label: '全部', value: '' },
  { label: '草稿', value: 0 },
  { label: '发放中', value: 1 },
  { label: '已停用', value: 2 },
];

interface TemplateForm {
  id: string;
  templateName: string;
  couponType: string;
  faceValue: number;
  discountRate: number;
  thresholdAmount: number;
  dishId: string;
  total: number;
  validDays: number;
  stackable: boolean;
  status: number;
}

const emptyForm = (): TemplateForm => ({
  id: '',
  templateName: '',
  couponType: 'FULL_REDUCTION',
  faceValue: 10,
  discountRate: 0.8,
  thresholdAmount: 100,
  dishId: '',
  total: 100,
  validDays: 30,
  stackable: false,
  status: 1,
});

const activeTab = ref('templates');
const loading = ref(false);
const templates = ref<CouponTemplateItem[]>([]);
const total = ref(0);
const current = ref(1);
const size = ref(10);
const keyword = ref('');
const statusFilter = ref<number | ''>('');
const records = ref<CouponUseRecordItem[]>([]);
const recordTotal = ref(0);
const recordCurrent = ref(1);
const recordSize = ref(10);
const rois = ref<CouponRoiItem[]>([]);
const RULE_TYPES = [
  { value: 'NEW_MEMBER', label: '新客券', desc: '首单支付成功后发放' },
  { value: 'BIRTHDAY', label: '生日券', desc: '每年生日当天发放，每日 00:40 任务执行' },
  { value: 'REPEAT_PURCHASE', label: '复购券', desc: '累计支付单数达标后发放' },
  { value: 'SLEEPING', label: '沉睡唤醒券', desc: '按沉睡标签发放，每自然月一次' },
];
const rules = ref<CouponRuleItem[]>([]);
const ruleVisible = ref(false);
const ruleForm = ref<CouponRulePayload>({
  ruleCode: '',
  ruleName: '',
  ruleType: 'BIRTHDAY',
  couponTemplateId: '',
  triggerValue: 0,
  maxPerMember: 1,
  enabled: false,
  remark: '',
});
const dishes = ref<DishListItem[]>([]);
const formVisible = ref(false);
const form = ref<TemplateForm>(emptyForm());
const grantVisible = ref(false);
const grantTemplateId = ref('');
const grantMemberIds = ref('');
const verifyCode = ref('');
const verifyResult = ref<CouponVerifyResult | null>(null);
const verifyFailed = ref(false);
const verifyMessage = ref('');
const verifyInput = ref<{ focus: () => void } | null>(null);

const isDiscountType = computed(() => form.value.couponType === 'DISCOUNT');
const isDishType = computed(() => form.value.couponType === 'DISH');
const isEditing = computed(() => form.value.id !== '');

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const loadTemplates = async (): Promise<void> => {
  loading.value = true;
  try {
    const page = await couponApi.pageTemplate({
      current: current.value,
      size: size.value,
      keyword: keyword.value,
      status: statusFilter.value,
    });
    templates.value = page.records;
    total.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '券模板加载失败'));
  } finally {
    loading.value = false;
  }
};

const loadRecords = async (): Promise<void> => {
  try {
    const page = await couponApi.pageRecord({ current: recordCurrent.value, size: recordSize.value });
    records.value = page.records;
    recordTotal.value = page.total;
  } catch (error) {
    ElMessage.error(errorMessage(error, '核销记录加载失败'));
  }
};

const loadRoi = async (): Promise<void> => {
  try {
    rois.value = await couponApi.roi();
  } catch (error) {
    ElMessage.error(errorMessage(error, '券 ROI 加载失败'));
  }
};

const loadDishes = async (): Promise<void> => {
  try {
    const page = await dishApi.page({ current: 1, size: 100 });
    dishes.value = page.records;
  } catch (error) {
    ElMessage.error(errorMessage(error, '菜品列表加载失败'));
  }
};

const onQuery = async (): Promise<void> => {
  current.value = 1;
  await loadTemplates();
};

const onPageChange = async (page: number): Promise<void> => {
  current.value = page;
  await loadTemplates();
};

const onRecordPageChange = async (page: number): Promise<void> => {
  recordCurrent.value = page;
  await loadRecords();
};

const formStatusTabs = computed(() => STATUS_TABS.filter((item) => item.value !== ''));

const openCreate = (): void => {
  form.value = emptyForm();
  formVisible.value = true;
};

const openEdit = (row: CouponTemplateItem): void => {
  form.value = {
    id: row.id,
    templateName: row.templateName,
    couponType: row.couponType,
    faceValue: row.faceValue,
    discountRate: row.discountRate ?? 0.8,
    thresholdAmount: row.thresholdAmount,
    dishId: row.dishId ?? '',
    total: row.total,
    validDays: row.validDays,
    stackable: row.stackable === 1,
    status: row.status,
  };
  formVisible.value = true;
};

/** 面额与折扣率按券类型二选一提交，抵扣额始终由后端计算。 */
const buildPayload = (): CouponTemplatePayload => {
  const payload: CouponTemplatePayload = {
    templateName: form.value.templateName,
    couponType: form.value.couponType,
    total: form.value.total,
    validDays: form.value.validDays,
    thresholdAmount: form.value.thresholdAmount,
    stackable: form.value.stackable ? 1 : 0,
    status: form.value.status,
  };
  if (isDiscountType.value) {
    payload.discountRate = form.value.discountRate;
    return payload;
  }
  payload.faceValue = form.value.faceValue;
  if (isDishType.value && form.value.dishId) {
    payload.dishId = form.value.dishId;
  }
  return payload;
};

const submitForm = async (): Promise<void> => {
  if (!form.value.templateName.trim()) {
    ElMessage.warning('请输入券名称');
    return;
  }
  loading.value = true;
  try {
    if (isEditing.value) {
      await couponApi.updateTemplate(form.value.id, buildPayload());
      ElMessage.success('券模板已更新');
    } else {
      await couponApi.createTemplate(buildPayload());
      ElMessage.success('券模板已创建');
    }
    formVisible.value = false;
    await loadTemplates();
  } catch (error) {
    ElMessage.error(errorMessage(error, '保存失败'));
  } finally {
    loading.value = false;
  }
};

const switchStatus = async (row: CouponTemplateItem, status: number): Promise<void> => {
  try {
    await couponApi.updateStatus(row.id, status);
    ElMessage.success(status === 1 ? '已开始发放' : '已停用');
    await loadTemplates();
  } catch (error) {
    ElMessage.error(errorMessage(error, '状态切换失败'));
  }
};

const openGrant = (row: CouponTemplateItem): void => {
  grantTemplateId.value = row.id;
  grantMemberIds.value = '';
  grantVisible.value = true;
};

const submitGrant = async (): Promise<void> => {
  const memberIds = grantMemberIds.value
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item !== '');
  if (memberIds.length === 0) {
    ElMessage.warning('请填写会员 ID，多个用逗号分隔');
    return;
  }
  try {
    const granted = await couponApi.grant({ templateId: grantTemplateId.value, memberIds });
    ElMessage.success(`已发放 ${granted} 张`);
    grantVisible.value = false;
    await loadTemplates();
  } catch (error) {
    ElMessage.error(errorMessage(error, '发券失败'));
  }
};

/** 核销成功播提示音（计划 4.9）：Web Audio 短促双音，不引入音频资源。 */
const playBeep = (): void => {
  try {
    const Context = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
    if (!Context) {
      return;
    }
    const context = new Context();
    const oscillator = context.createOscillator();
    const gain = context.createGain();
    oscillator.type = 'sine';
    oscillator.frequency.value = 880;
    gain.gain.value = 0.08;
    oscillator.connect(gain);
    gain.connect(context.destination);
    oscillator.start();
    oscillator.stop(context.currentTime + 0.16);
  } catch {
    // 提示音非关键路径，浏览器禁止自动播放时静默跳过
  }
};

const submitVerify = async (): Promise<void> => {
  const code = verifyCode.value.trim();
  if (!code) {
    ElMessage.warning('请输入券码');
    return;
  }
  verifyFailed.value = false;
  verifyResult.value = null;
  try {
    verifyResult.value = await couponApi.verify({ code });
    verifyCode.value = '';
    playBeep();
    await loadRecords();
  } catch (error) {
    verifyFailed.value = true;
    verifyMessage.value = errorMessage(error, '核销失败');
  } finally {
    await nextTick();
    verifyInput.value?.focus();
  }
};

const resetVerify = (): void => {
  verifyResult.value = null;
  verifyFailed.value = false;
  verifyMessage.value = '';
  verifyInput.value?.focus();
};

const confirmDisable = async (row: CouponTemplateItem): Promise<void> => {
  try {
    await ElMessageBox.confirm(`确认停用「${row.templateName}」？停用后顾客不可再领取。`, '停用券模板', {
      type: 'warning',
    });
  } catch {
    return;
  }
  await switchStatus(row, 2);
};

const loadRules = async (): Promise<void> => {
  try {
    rules.value = await couponApi.rules();
  } catch (error) {
    ElMessage.error(errorMessage(error, '券发放规则加载失败'));
  }
};

const openRuleCreate = (): void => {
  ruleForm.value = {
    ruleCode: '',
    ruleName: '',
    ruleType: 'BIRTHDAY',
    couponTemplateId: '',
    triggerValue: 0,
    maxPerMember: 1,
    enabled: false,
    remark: '',
  };
  ruleVisible.value = true;
};

const openRuleEdit = (row: CouponRuleItem): void => {
  ruleForm.value = {
    ruleCode: row.ruleCode,
    ruleName: row.ruleName,
    ruleType: row.ruleType,
    couponTemplateId: row.couponTemplateId,
    triggerValue: row.triggerValue,
    maxPerMember: row.maxPerMember,
    enabled: row.isEnabled === 1,
    remark: row.remark,
  };
  ruleVisible.value = true;
};

const submitRule = async (): Promise<void> => {
  if (!ruleForm.value.ruleCode || !ruleForm.value.ruleName || !ruleForm.value.couponTemplateId) {
    ElMessage.warning('规则编码、名称与券模板都不能为空');
    return;
  }
  try {
    await couponApi.saveRule(ruleForm.value);
    ElMessage.success('券发放规则已保存');
    ruleVisible.value = false;
    await loadRules();
  } catch (error) {
    ElMessage.error(errorMessage(error, '券发放规则保存失败'));
  }
};

const toggleRule = async (row: CouponRuleItem): Promise<void> => {
  try {
    await couponApi.updateRuleStatus(row.id, row.isEnabled !== 1);
    ElMessage.success(row.isEnabled === 1 ? '规则已停用' : '规则已启用');
    await loadRules();
  } catch (error) {
    ElMessage.error(errorMessage(error, '规则状态切换失败'));
  }
};

const runRule = async (ruleType: string): Promise<void> => {
  try {
    await ElMessageBox.confirm(
      '将立即执行一次该规则（与每日 00:40 任务同一条链路），已有幂等键的会员不会重复发券。',
      '执行券发放规则',
      { type: 'warning' },
    );
  } catch {
    return;
  }
  try {
    const granted = await couponApi.runRule(ruleType);
    ElMessage.success(`本次发出 ${granted} 张券`);
    await loadRules();
  } catch (error) {
    ElMessage.error(errorMessage(error, '规则执行失败'));
  }
};

onMounted(async () => {
  await loadTemplates();
  await loadDishes();
  await loadRecords();
  await loadRoi();
  await loadRules();
  await nextTick();
  verifyInput.value?.focus();
});
</script>

<template>
  <div v-loading="loading">
    <h1 class="title">营销管理</h1>
    <p class="subtitle">券模板、定向发券、核销台与券 ROI；抵扣金额一律由服务端计算，页面只展示结果。</p>
    <el-card class="page-card" shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="券模板" name="templates">
          <div class="toolbar">
            <el-select v-model="statusFilter" class="status-select" @change="onQuery">
              <el-option v-for="tab in STATUS_TABS" :key="tab.label" :label="tab.label" :value="tab.value" />
            </el-select>
            <el-input v-model="keyword" class="keyword-input" placeholder="券名称" clearable @keyup.enter="onQuery" />
            <el-button type="primary" @click="onQuery">查询</el-button>
            <el-button @click="openCreate">新建券</el-button>
            <span class="muted">共 {{ total }} 张券模板</span>
          </div>
          <el-table :data="templates" border empty-text="暂无券模板">
            <el-table-column prop="templateName" label="券名称" min-width="160" />
            <el-table-column prop="couponTypeText" label="类型" width="110" />
            <el-table-column label="面额 / 折扣" width="120">
              <template #default="{ row }">
                {{ row.couponType === 'DISCOUNT' ? `${row.discountRate} 折率` : `¥${row.faceValue}` }}
              </template>
            </el-table-column>
            <el-table-column label="门槛" width="110">
              <template #default="{ row }">¥{{ row.thresholdAmount }}</template>
            </el-table-column>
            <el-table-column label="库存" width="140">
              <template #default="{ row }">{{ row.remain }} / {{ row.total }}</template>
            </el-table-column>
            <el-table-column prop="validDays" label="有效天数" width="100" />
            <el-table-column prop="statusText" label="状态" width="100" />
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="primary" @click="openGrant(row)">发券</el-button>
                <el-button v-if="row.status === 1" link type="primary" @click="confirmDisable(row)">停用</el-button>
                <el-button v-else link type="primary" @click="switchStatus(row, 1)">开始发放</el-button>
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

        <el-tab-pane label="核销台" name="verify">
          <div class="toolbar">
            <el-input
              ref="verifyInput"
              v-model="verifyCode"
              class="verify-input"
              placeholder="扫描枪 / 手动输入券码后回车"
              clearable
              @keyup.enter="submitVerify"
            />
            <el-button type="primary" @click="submitVerify">核销</el-button>
            <el-button @click="resetVerify">清空</el-button>
          </div>
          <div v-if="verifyResult" class="verify-result verify-result--ok">
            <p class="verify-title">核销成功 · {{ verifyResult.templateName }}</p>
            <p class="verify-line">抵扣金额 ¥{{ verifyResult.discountAmount }}</p>
            <p class="verify-line">会员 {{ verifyResult.memberId }} · 核销人 {{ verifyResult.operatorUserId }} · {{ verifyResult.verifyTime }}</p>
          </div>
          <div v-else-if="verifyFailed" class="verify-result verify-result--fail">
            <p class="verify-title">核销失败</p>
            <p class="verify-line">{{ verifyMessage }}</p>
          </div>
          <p v-else class="note">已核销的券再次输入返回「券已使用」；券码 30 秒刷新一次，跨窗口后旧码失效。</p>
        </el-tab-pane>

        <el-tab-pane label="核销记录" name="records">
          <div class="toolbar">
            <el-button @click="loadRecords">刷新</el-button>
            <span class="muted">共 {{ recordTotal }} 条核销记录，按核销时间倒序</span>
          </div>
          <el-table :data="records" border empty-text="暂无核销记录">
            <el-table-column prop="templateName" label="券名称" min-width="160" />
            <el-table-column prop="couponTypeText" label="类型" width="110" />
            <el-table-column prop="memberId" label="会员 ID" min-width="180" />
            <el-table-column label="抵扣" width="100">
              <template #default="{ row }">¥{{ row.discountAmount }}</template>
            </el-table-column>
            <el-table-column prop="operatorName" label="核销员工" width="120" />
            <el-table-column prop="verifyChannelText" label="入口" width="140" />
            <el-table-column prop="verifyTime" label="核销时间" width="170" />
          </el-table>
          <el-pagination
            class="pager"
            layout="prev, pager, next"
            :current-page="recordCurrent"
            :page-size="recordSize"
            :total="recordTotal"
            @current-change="onRecordPageChange"
          />
        </el-tab-pane>

        <el-tab-pane label="发放规则" name="rules">
          <div class="toolbar">
            <el-button type="primary" @click="openRuleCreate">新建规则</el-button>
            <el-button @click="loadRules">刷新</el-button>
            <el-button @click="runRule('BIRTHDAY')">执行生日券</el-button>
            <el-button @click="runRule('SLEEPING')">执行沉睡唤醒</el-button>
            <span class="muted">新客券与复购券由支付 / 订单完成事件触发，无需手动执行</span>
          </div>
          <el-table :data="rules" border empty-text="暂无发放规则">
            <el-table-column prop="ruleName" label="规则名称" min-width="140" />
            <el-table-column prop="ruleCode" label="编码" width="150" />
            <el-table-column prop="ruleTypeText" label="类型" width="110" />
            <el-table-column prop="templateName" label="发放券模板" min-width="160" />
            <el-table-column prop="triggerValue" label="阈值" width="90" />
            <el-table-column prop="maxPerMember" label="每会员上限" width="110" />
            <el-table-column prop="periodText" label="口径" min-width="240" />
            <el-table-column label="启用" width="90">
              <template #default="{ row }">
                <el-switch :model-value="row.isEnabled === 1" @update:model-value="() => toggleRule(row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openRuleEdit(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
          <p class="note">
            幂等键：生日按自然年、沉睡唤醒按自然月、新客一次性、复购按订单，统一落 user_coupon.grant_biz_id。
            模板不是「发放中」时该规则整轮跳过并打日志。
          </p>
        </el-tab-pane>

        <el-tab-pane label="券 ROI" name="roi">
          <div class="toolbar">
            <el-button @click="loadRoi">刷新</el-button>
            <span class="muted">投产比 = 用券订单实收 / 抵扣；现场扫码核销不绑定订单，只计入使用数</span>
          </div>
          <el-table :data="rois" border empty-text="暂无数据">
            <el-table-column prop="templateName" label="券名称" min-width="160" />
            <el-table-column prop="couponTypeText" label="类型" width="110" />
            <el-table-column prop="receivedCount" label="已领取" width="100" />
            <el-table-column prop="usedCount" label="已使用" width="100" />
            <el-table-column label="核销率" width="100">
              <template #default="{ row }">{{ row.useRate }}%</template>
            </el-table-column>
            <el-table-column prop="orderCount" label="用券订单" width="110" />
            <el-table-column label="抵扣合计" width="120">
              <template #default="{ row }">¥{{ row.discountAmount }}</template>
            </el-table-column>
            <el-table-column label="实收合计" width="120">
              <template #default="{ row }">¥{{ row.revenueAmount }}</template>
            </el-table-column>
            <el-table-column label="投产比" width="110">
              <template #default="{ row }">{{ row.roi === null ? '—' : row.roi }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="formVisible" :title="isEditing ? '编辑券模板' : '新建券模板'" width="560px">
      <el-form label-width="110px">
        <el-form-item label="券名称">
          <el-input v-model="form.templateName" placeholder="如 满 100 减 20" />
        </el-form-item>
        <el-form-item label="券类型">
          <el-select v-model="form.couponType" :disabled="isEditing">
            <el-option v-for="item in COUPON_TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isDiscountType" label="折扣率">
          <el-input-number v-model="form.discountRate" :min="0.1" :max="0.99" :step="0.05" :precision="2" />
        </el-form-item>
        <el-form-item v-else label="面额（元）">
          <el-input-number v-model="form.faceValue" :min="0.01" :step="1" :precision="2" />
        </el-form-item>
        <el-form-item v-if="isDishType" label="指定菜品">
          <el-select v-model="form.dishId" placeholder="选择适用菜品" clearable>
            <el-option v-for="dish in dishes" :key="dish.id" :label="dish.dishName" :value="dish.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用门槛（元）">
          <el-input-number v-model="form.thresholdAmount" :min="0" :step="10" :precision="2" />
        </el-form-item>
        <el-form-item label="发放总量">
          <el-input-number v-model="form.total" :min="1" :step="10" />
        </el-form-item>
        <el-form-item label="有效天数">
          <el-input-number v-model="form.validDays" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="会员折扣同享">
          <el-switch v-model="form.stackable" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option v-for="tab in formStatusTabs" :key="tab.label" :label="tab.label" :value="tab.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ruleVisible" title="券发放规则" width="520px">
      <el-form label-width="120px">
        <el-form-item label="规则编码">
          <el-input v-model="ruleForm.ruleCode" placeholder="如 BIRTHDAY_2026（大写字母 / 数字 / 下划线）" />
        </el-form-item>
        <el-form-item label="规则名称">
          <el-input v-model="ruleForm.ruleName" placeholder="如 生日赠券" />
        </el-form-item>
        <el-form-item label="触发类型">
          <el-select v-model="ruleForm.ruleType">
            <el-option v-for="item in RULE_TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="券模板">
          <el-select v-model="ruleForm.couponTemplateId" placeholder="选择发放的券模板">
            <el-option
              v-for="item in templates"
              :key="item.id"
              :label="`${item.templateName}（${item.statusText}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值">
          <el-input-number v-model="ruleForm.triggerValue" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="每会员上限">
          <el-input-number v-model="ruleForm.maxPerMember" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="ruleForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <p class="note">阈值：复购券为「累计支付单数」（至少 2），沉睡唤醒券为「无支付天数」（留 0 取配置阈值）。</p>
      <template #footer>
        <el-button @click="ruleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRule">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="grantVisible" title="定向发券" width="480px">
      <el-form label-width="100px">
        <el-form-item label="会员 ID">
          <el-input v-model="grantMemberIds" placeholder="多个会员 ID 用逗号分隔" />
        </el-form-item>
      </el-form>
      <p class="note">发放与顾客领券走同一条库存链路，库存不足时只发到发完为止。</p>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGrant">发放</el-button>
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

.verify-input {
  width: 360px;
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

.note {
  margin: 8px 0;
  color: var(--co-text-tertiary);
  font-size: 12px;
}

/* 核销结果整块着色反馈（计划 4.9），状态色只用于状态 */
.verify-result {
  padding: var(--co-space-card);
  border-radius: var(--co-radius-card);
}

.verify-result--ok {
  background-color: var(--co-success);
  color: var(--co-card);
}

.verify-result--fail {
  background-color: var(--co-danger);
  color: var(--co-card);
}

.verify-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
}

.verify-line {
  margin: 4px 0;
  font-size: 14px;
}
</style>
