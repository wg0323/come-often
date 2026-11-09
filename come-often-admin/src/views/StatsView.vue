<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts/core';
import { LineChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { statsApi } from '@/api/stats';
import type { StatsQuery, StatsReport } from '@/api/stats';
import { readCssVar } from '@/utils/theme';

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer]);

type EChartsInstance = ReturnType<typeof echarts.init>;

interface GranularityOption {
  label: string;
  value: string;
}

const GRANULARITY_OPTIONS: GranularityOption[] = [
  { label: '按日', value: 'DAY' },
  { label: '按周', value: 'WEEK' },
  { label: '按月', value: 'MONTH' },
];

const EXPORT_LABELS: Record<string, string> = {
  trend: '营收趋势',
  'hot-dish': '热销排行',
  'coupon-roi': '券ROI',
  'high-value-member': '高价值用户',
};

const TOP_N = 10;

/** 与 Element Plus DatePicker 的 modelValue 类型保持一致，避免 v-model 双向类型不匹配。 */
type DateRangeModel = string | number | Date | Array<string | number | Date>;

const DEFAULT_RANGE_DAYS = 7;

const toDateString = (date: Date): string =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;

const defaultRange = (): string[] => {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - (DEFAULT_RANGE_DAYS - 1));
  return [toDateString(start), toDateString(end)];
};

const dateRange = ref<DateRangeModel>(defaultRange());
const granularity = ref('DAY');
const report = ref<StatsReport | null>(null);
const loading = ref(false);
const exporting = ref('');
const chartRef = ref<HTMLDivElement | null>(null);
let chart: EChartsInstance | null = null;

const errorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

const formatMoney = (value: number | null | undefined): string =>
  value === null || value === undefined ? '—' : `¥${value.toFixed(2)}`;

const formatPercent = (value: number | null | undefined): string =>
  value === null || value === undefined ? '—' : `${value.toFixed(2)}%`;

const formatRatio = (value: number | null | undefined): string =>
  value === null || value === undefined ? '—' : value.toFixed(2);

const formatTime = (value: string | null): string =>
  value === null || value === '' ? '—' : value.replace('T', ' ').slice(0, 16);

const currentQuery = (): StatsQuery => {
  const values = Array.isArray(dateRange.value) ? dateRange.value.map((item) => String(item)) : [];
  return {
    startDate: values[0],
    endDate: values[1],
    granularity: granularity.value,
    topN: TOP_N,
  };
};

const renderChart = (): void => {
  const points = report.value?.trend ?? [];
  if (!chartRef.value) {
    return;
  }
  if (!chart) {
    chart = echarts.init(chartRef.value);
  }
  const textColor = readCssVar('--co-text-tertiary');
  const lineColor = readCssVar('--co-divider');
  const revenueColor = readCssVar('--co-cinnabar');
  const orderColor = readCssVar('--co-ink');
  chart.setOption(
    {
      grid: { left: 8, right: 8, top: 36, bottom: 8, containLabel: true },
      tooltip: { trigger: 'axis' },
      legend: { top: 0, textStyle: { color: textColor }, data: ['净营收', '订单数'] },
      xAxis: {
        type: 'category',
        data: points.map((point) => point.label),
        axisLine: { lineStyle: { color: lineColor } },
        axisLabel: { color: textColor },
      },
      yAxis: [
        {
          type: 'value',
          name: '元',
          splitLine: { lineStyle: { color: lineColor } },
          axisLabel: { color: textColor },
        },
        {
          type: 'value',
          name: '单',
          splitLine: { show: false },
          axisLabel: { color: textColor },
        },
      ],
      series: [
        {
          name: '净营收',
          type: 'line',
          smooth: true,
          symbolSize: 6,
          data: points.map((point) => point.netRevenue),
          itemStyle: { color: revenueColor },
          lineStyle: { color: revenueColor, width: 2 },
          areaStyle: { color: revenueColor, opacity: 0.08 },
        },
        {
          name: '订单数',
          type: 'line',
          yAxisIndex: 1,
          smooth: true,
          symbolSize: 4,
          data: points.map((point) => point.orderCount),
          itemStyle: { color: orderColor },
          lineStyle: { color: orderColor, width: 2, type: 'dashed' },
        },
      ],
    },
    true,
  );
};

const load = async (): Promise<void> => {
  loading.value = true;
  try {
    report.value = await statsApi.report(currentQuery());
    await nextTick();
    renderChart();
  } catch (error) {
    ElMessage.error(errorMessage(error, '报表加载失败'));
  } finally {
    loading.value = false;
  }
};

const onQuery = async (): Promise<void> => {
  await load();
};

const onGranularityChange = async (): Promise<void> => {
  await load();
};

const onExport = async (type: string): Promise<void> => {
  const query = currentQuery();
  exporting.value = type;
  try {
    const blob = await statsApi.exportExcel(type, query);
    // 文件名跟请求区间走，不跟已渲染的报表走：改了区间没点查询就导出时不会错标日期
    const suffix = `${query.startDate ?? ''}_${query.endDate ?? ''}`;
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `报表-${EXPORT_LABELS[type]}-${suffix}.xlsx`;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    // 立刻 revoke 会让部分浏览器取消下载，留一个宏任务再释放
    window.setTimeout(() => URL.revokeObjectURL(link.href), 0);
    ElMessage.success('导出完成');
  } catch (error) {
    ElMessage.error(errorMessage(error, '导出失败'));
  } finally {
    exporting.value = '';
  }
};

const onResize = (): void => {
  chart?.resize();
};

onMounted(async () => {
  window.addEventListener('resize', onResize);
  await load();
});

onUnmounted(() => {
  window.removeEventListener('resize', onResize);
  chart?.dispose();
  chart = null;
});
</script>

<template>
  <div v-loading="loading" class="page">
    <h1 class="title">数据报表</h1>
    <p class="subtitle">
      一段时间怎么样：营收按支付自然日汇总并扣退款，退款按原订单支付时间冲减；相邻期间数据同时受支付与退款影响时也能对平。
    </p>

    <el-card class="page-card" shadow="never">
      <div class="toolbar">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="起始日"
          end-placeholder="结束日"
          :clearable="false"
          class="range-picker"
        />
        <el-radio-group v-model="granularity" @change="onGranularityChange">
          <el-radio-button v-for="item in GRANULARITY_OPTIONS" :key="item.value" :value="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="onQuery">查询</el-button>
        <span class="muted">
          {{ report ? `${report.range.startDate} 至 ${report.range.endDate} · ${report.range.granularityText}` : '' }}
        </span>
      </div>
    </el-card>

    <div class="metric-grid">
      <el-card class="metric-card" shadow="never">
        <p class="metric-label">净营收</p>
        <p class="metric-value">{{ formatMoney(report?.summary.netRevenue) }}</p>
        <p class="metric-note">已扣退款 {{ formatMoney(report?.summary.refundAmount) }}</p>
      </el-card>
      <el-card class="metric-card" shadow="never">
        <p class="metric-label">有效订单数</p>
        <p class="metric-value">{{ report?.summary.orderCount ?? 0 }}</p>
        <p class="metric-note">不含已取消与整单退款</p>
      </el-card>
      <el-card class="metric-card" shadow="never">
        <p class="metric-label">客单价</p>
        <p class="metric-value">{{ formatMoney(report?.summary.avgOrderAmount) }}</p>
        <p class="metric-note">净营收 ÷ 有效订单数</p>
      </el-card>
      <el-card class="metric-card" shadow="never">
        <p class="metric-label">有支付单会员</p>
        <p class="metric-value">{{ report?.member.paidMemberCount ?? 0 }}</p>
        <p class="metric-note">复购率 {{ formatPercent(report?.member.repurchaseRate) }}</p>
      </el-card>
    </div>

    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-head">
          <span class="card-title">营收与订单趋势</span>
          <el-button link type="primary" :loading="exporting === 'trend'" @click="onExport('trend')">
            导出 Excel
          </el-button>
        </div>
      </template>
      <div ref="chartRef" class="chart"></div>
      <el-table :data="report?.trend ?? []" border empty-text="区间内没有数据" max-height="320">
        <el-table-column prop="bucketStart" label="时间桶" min-width="120" />
        <el-table-column label="净营收" width="130">
          <template #default="{ row }">{{ formatMoney(row.netRevenue) }}</template>
        </el-table-column>
        <el-table-column label="退款金额" width="120">
          <template #default="{ row }">{{ formatMoney(row.refundAmount) }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column label="客单价" width="120">
          <template #default="{ row }">{{ formatMoney(row.avgOrderAmount) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="split-grid">
      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-head">
            <span class="card-title">热销排行 Top {{ TOP_N }}</span>
            <el-button link type="primary" :loading="exporting === 'hot-dish'" @click="onExport('hot-dish')">
              导出 Excel
            </el-button>
          </div>
        </template>
        <el-table :data="report?.hotDishes ?? []" border empty-text="区间内没有已支付订单">
          <el-table-column prop="rank" label="#" width="56" />
          <el-table-column prop="dishName" label="菜品" min-width="140" />
          <el-table-column prop="quantity" label="销量" width="80" />
          <el-table-column label="销售额" width="120">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="page-card" shadow="never">
        <template #header>
          <div class="card-head">
            <span class="card-title">会员复购与留存</span>
            <span class="muted">留存为次日留存，考察首单在区间内的新客</span>
          </div>
        </template>
        <div class="status-row">
          <div class="status-item">
            <span class="status-value">{{ report?.member.repurchaseMemberCount ?? 0 }}</span>
            <span class="status-label">复购会员</span>
          </div>
          <div class="status-item">
            <span class="status-value">{{ formatPercent(report?.member.repurchaseRate) }}</span>
            <span class="status-label">复购率</span>
          </div>
          <div class="status-item">
            <span class="status-value">{{ report?.member.retentionCount ?? 0 }}</span>
            <span class="status-label">次日回访</span>
          </div>
          <div class="status-item">
            <span class="status-value">{{ formatPercent(report?.member.retentionRate) }}</span>
            <span class="status-label">次日留存率</span>
          </div>
        </div>
        <p class="muted">复购率分母为区间内有支付单的会员；留存分母为 {{ report?.member.retentionCohortSize ?? 0 }} 位新客。</p>
        <el-table :data="report?.member.highValueUsers ?? []" border empty-text="区间内没有可排行的会员">
          <el-table-column prop="rank" label="#" width="56" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="phoneMasked" label="手机号" width="130" />
          <el-table-column prop="levelName" label="等级" width="110" />
          <el-table-column prop="orderCount" label="订单数" width="90" />
          <el-table-column label="净消费" width="120">
            <template #default="{ row }">{{ formatMoney(row.netAmount) }}</template>
          </el-table-column>
          <el-table-column label="最近消费" width="150">
            <template #default="{ row }">{{ formatTime(row.lastPaidTime) }}</template>
          </el-table-column>
        </el-table>
        <div class="card-foot">
          <el-button link type="primary" :loading="exporting === 'high-value-member'"
            @click="onExport('high-value-member')">
            导出高价值用户
          </el-button>
        </div>
      </el-card>
    </div>

    <el-card class="page-card" shadow="never">
      <template #header>
        <div class="card-head">
          <span class="card-title">券 ROI</span>
          <span class="muted">领取与核销按区间统计；带动金额为净额，折扣成本取用券订单抵扣快照</span>
          <el-button link type="primary" :loading="exporting === 'coupon-roi'" @click="onExport('coupon-roi')">
            导出 Excel
          </el-button>
        </div>
      </template>
      <el-table :data="report?.couponRoi ?? []" border empty-text="还没有券模板">
        <el-table-column prop="templateName" label="券模板" min-width="180" />
        <el-table-column prop="couponTypeText" label="类型" width="110" />
        <el-table-column prop="total" label="发行总量" width="100" />
        <el-table-column prop="receivedCount" label="领取" width="90" />
        <el-table-column prop="usedCount" label="核销" width="90" />
        <el-table-column label="核销率" width="100">
          <template #default="{ row }">{{ formatPercent(row.useRate) }}</template>
        </el-table-column>
        <el-table-column prop="orderCount" label="用券订单" width="100" />
        <el-table-column label="带动金额" width="120">
          <template #default="{ row }">{{ formatMoney(row.revenueAmount) }}</template>
        </el-table-column>
        <el-table-column label="折扣成本" width="120">
          <template #default="{ row }">{{ formatMoney(row.discountAmount) }}</template>
        </el-table-column>
        <el-table-column label="投产比" width="100">
          <template #default="{ row }">{{ formatRatio(row.roi) }}</template>
        </el-table-column>
      </el-table>
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
  margin-bottom: var(--co-space-card);
  border-color: var(--co-divider);
  border-radius: var(--co-radius-card);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.range-picker {
  width: 260px;
}

.muted {
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--co-space-card);
  margin-bottom: var(--co-space-card);
}

.metric-card {
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
  font-size: 26px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.metric-note {
  margin: 0;
  color: var(--co-text-tertiary);
  font-size: 12px;
}

.split-grid {
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

.card-foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.chart {
  width: 100%;
  height: 260px;
  margin-bottom: var(--co-space-card);
}

.status-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
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
  font-size: 18px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.status-label {
  color: var(--co-text-tertiary);
  font-size: 12px;
}
</style>
