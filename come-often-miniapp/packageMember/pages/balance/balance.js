/**
 * 我的储值：余额概览 + 充值（档位 / 自定义金额）+ 流水
 * 赠送额与余额全部取接口返回，页面不自行计算
 * @author come-often
 * @date 2026-11-10
 */
import { getBalanceAccount, getBalanceRecords, rechargeBalance } from '../../../api/balanceApi';

const PAGE_SIZE = 20;

Page({
  data: {
    account: null,
    records: [],
    current: 1,
    hasMore: true,
    loading: false,
    loaded: false,
    submitting: false,
    amount: '',
  },

  async onLoad() {
    await getApp().globalData.loginReady;
    await this.loadAccount();
    await this.loadRecords(true);
  },

  async loadAccount() {
    try {
      const account = await getBalanceAccount();
      this.setData({ account });
    } catch (error) {
      wx.showToast({ title: error.message ?? '储值账户加载失败', icon: 'none' });
    }
  },

  async loadRecords(reset) {
    if (this.data.loading) {
      return;
    }
    const current = reset ? 1 : this.data.current + 1;
    this.setData({ loading: true });
    try {
      const page = await getBalanceRecords({ current, size: PAGE_SIZE });
      const records = reset ? (page.records ?? []) : this.data.records.concat(page.records ?? []);
      this.setData({
        records,
        current,
        hasMore: records.length < (page.total ?? 0),
        loaded: true,
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '流水加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onPickTier(event) {
    this.setData({ amount: `${event.currentTarget.dataset.amount}` });
  },

  onAmountInput(event) {
    this.setData({ amount: event.detail.value });
  },

  async onRecharge() {
    const { account, amount, submitting } = this.data;
    if (submitting || !account) {
      return;
    }
    const value = Number(amount);
    if (!Number.isFinite(value) || value <= 0) {
      wx.showToast({ title: '请输入充值金额', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    try {
      const result = await rechargeBalance(value.toFixed(2));
      wx.showToast({
        title: result.gift > 0 ? `充值成功，赠 ¥${result.gift}` : '充值成功',
        icon: 'none',
      });
      this.setData({ amount: '' });
      await this.loadAccount();
      await this.loadRecords(true);
    } catch (error) {
      wx.showToast({ title: error.message ?? '充值失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadRecords(false);
    }
  },
});
