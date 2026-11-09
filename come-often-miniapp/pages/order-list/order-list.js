/**
 * 订单列表页：状态、金额与取餐码均取接口返回，入口在「我的」与订单详情页
 * @author come-often
 * @date 2026-11-09
 */
import { getOrders } from '../../api/orderApi';

Page({
  data: {
    orders: [],
    loading: false,
  },

  async onShow() {
    await getApp().globalData.loginReady;
    await this.loadOrders();
  },

  async loadOrders() {
    this.setData({ loading: true });
    try {
      const page = await getOrders({ current: 1, size: 20 });
      this.setData({ orders: page.records ?? [] });
    } catch (error) {
      wx.showToast({ title: error.message ?? '订单加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onTapOrder(event) {
    const { orderNo } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/packageOrder/pages/order-detail/order-detail?orderNo=${orderNo}` });
  },

  /** catchtap：卡片本身点了进详情，这里只进评价页 */
  onGoReview(event) {
    const { orderNo } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/packageOrder/pages/review/review?orderNo=${orderNo}` });
  },

  onGoDish() {
    wx.switchTab({ url: '/pages/dish-list/dish-list' });
  },
});
