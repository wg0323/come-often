/**
 * 门店列表：一期只有 1 家门店，结构按多店预留；入口进入桌台预约
 * @author come-often
 * @date 2026-11-09
 */
import { getStores } from '../../../api/storeApi';

Page({
  data: {
    stores: [],
    loading: false,
    loaded: false,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
    await this.loadStores();
  },

  async loadStores() {
    this.setData({ loading: true });
    try {
      const stores = await getStores();
      this.setData({ stores: stores ?? [], loaded: true });
    } catch (error) {
      wx.showToast({ title: error.message ?? '门店加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onTapStore(event) {
    const { id } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/packageOther/pages/store-detail/store-detail?storeId=${id}` });
  },

  onTapMine() {
    wx.navigateTo({ url: '/packageMember/pages/reservation-list/reservation-list' });
  },
});
