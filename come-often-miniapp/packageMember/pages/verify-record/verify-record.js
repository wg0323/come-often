/**
 * 我的核销记录：取餐码 / 团购套餐的核销审计，触底分页
 * @author come-often
 * @date 2026-11-10
 */
import { getVerifyRecords } from '../../../api/verifyRecordApi';

const PAGE_SIZE = 10;

Page({
  data: {
    records: [],
    current: 1,
    hasMore: true,
    loading: false,
    loaded: false,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
    await this.loadRecords(true);
  },

  onShow() {
    if (this.data.loaded) {
      this.loadRecords(true);
    }
  },

  async loadRecords(reset) {
    if (this.data.loading) {
      return;
    }
    const current = reset ? 1 : this.data.current + 1;
    this.setData({ loading: true });
    try {
      const page = await getVerifyRecords({ current, size: PAGE_SIZE });
      const rows = page.records ?? [];
      const records = reset ? rows : this.data.records.concat(rows);
      this.setData({
        records,
        current,
        hasMore: records.length < (page.total ?? 0),
        loaded: true,
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '核销记录加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onTapOrder(event) {
    const { orderNo } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/packageOrder/pages/order-detail/order-detail?orderNo=${orderNo}` });
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadRecords(false);
    }
  },
});
