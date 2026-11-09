/**
 * 商家端核销记录：默认当日，可按员工与日期筛选；刻意不提供撤销核销
 * @author come-often
 * @date 2026-11-09
 */
import { listOperators, pageRecords } from '../../api/couponApi';
import { formatMoney, formatTime } from '../../utils/display';
import { PAGE_SIZE } from '../../config/env';

const todayString = () => {
  const now = new Date();
  const month = `${now.getMonth() + 1}`.padStart(2, '0');
  const day = `${now.getDate()}`.padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
};

Page({
  data: {
    date: todayString(),
    operators: [{ operatorUserId: '', operatorName: '全部员工' }],
    operatorIndex: 0,
    operatorText: '全部员工',
    records: [],
    current: 1,
    total: 0,
    hasMore: false,
    loading: false,
    refresherTriggered: false,
  },
  onShow() {
    this.loadAll(true);
  },
  /** 下拉刷新由根 scroll-view 的 refresher 接管，页面已 disableScroll */
  async onRefresherRefresh() {
    this.setData({ refresherTriggered: true });
    await this.loadAll(true);
    this.setData({ refresherTriggered: false });
  },
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadRecords(false);
    }
  },
  async loadAll(reset) {
    await this.loadOperators();
    await this.loadRecords(reset);
  },
  async loadOperators() {
    try {
      const operators = await listOperators(this.data.date);
      const options = [{ operatorUserId: '', operatorName: '全部员工' }].concat(
        (operators ?? []).map((item) => ({
          operatorUserId: item.operatorUserId,
          operatorName: item.operatorName || `员工 ${item.operatorUserId}`,
        })),
      );
      this.setData({ operators: options, operatorIndex: 0, operatorText: options[0].operatorName });
    } catch (error) {
      console.warn('核销员工筛选加载失败', error);
    }
  },
  async loadRecords(reset) {
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    const current = reset ? 1 : this.data.current + 1;
    const operator = this.data.operators[this.data.operatorIndex];
    try {
      const page = await pageRecords({
        current,
        size: PAGE_SIZE,
        date: this.data.date,
        operatorUserId: operator?.operatorUserId ?? '',
      });
      const records = (page?.records ?? []).map((record) => ({
        ...record,
        key: `${record.id}`,
        discountText: formatMoney(record.discountAmount),
        verifyTimeText: formatTime(record.verifyTime),
        channelText: record.verifyChannel === 'MERCHANT_SCAN' ? '扫码核销' : '后台输码',
      }));
      const merged = reset ? records : this.data.records.concat(records);
      this.setData({
        records: merged,
        current: page?.current ?? current,
        total: page?.total ?? merged.length,
        hasMore: merged.length < (page?.total ?? merged.length),
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
  onDateChange(event) {
    this.setData({ date: event.detail.value });
    this.loadAll(true);
  },
  onOperatorChange(event) {
    const operatorIndex = Number(event.detail.value);
    const operator = this.data.operators[operatorIndex];
    this.setData({
      operatorIndex,
      operatorText: operator ? operator.operatorName : '全部员工',
    });
    this.loadRecords(true);
  },
});
