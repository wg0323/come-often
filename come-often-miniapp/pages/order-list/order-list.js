/**
 * 订单列表页：全部 / 待支付 / 待上菜 / 待自提 / 待评价 / 已完成 / 退款售后 + 触底分页
 * @author come-often
 * @date 2026-11-09
 */
import { getOrders } from '../../api/orderApi';

/** needReview 表示该 Tab 不是单一状态，而是「已完成且未评价」的组合条件 */
const TABS = [
  { key: '', label: '全部' },
  { key: 'CREATED', label: '待支付' },
  { key: 'PAID,ACCEPTED', label: '待上菜' },
  { key: 'READY', label: '待自提' },
  { key: 'COMPLETED', label: '待评价', needReview: true },
  { key: 'COMPLETED,CANCELLED', label: '已完成' },
  { key: 'REFUNDING,REFUNDED', label: '退款售后' },
];

const PAGE_SIZE = 10;

Page({
  data: {
    tabs: TABS,
    activeTab: '',
    orders: [],
    current: 1,
    hasMore: true,
    loading: false,
    loaded: false,
  },

  /** 「我的」订单宫格带 tab 进来时直接落到对应 Tab，未命中则留在「全部」 */
  onLoad(options = {}) {
    const { tab = '' } = options;
    if (tab && TABS.some((item) => item.key === tab)) {
      this.setData({ activeTab: tab });
    }
  },

  async onShow() {
    await getApp().globalData.loginReady;
    await this.loadOrders(true);
  },

  async loadOrders(reset) {
    if (this.data.loading) {
      return;
    }
    const current = reset ? 1 : this.data.current + 1;
    this.setData({ loading: true });
    try {
      const params = { current, size: PAGE_SIZE };
      const active = TABS.find((item) => item.key === this.data.activeTab);
      if (active?.needReview) {
        // 待评价由后端按「已完成 + 评价表无记录」组合判定，不再传 status
        params.needReview = true;
      } else if (active?.key) {
        params.status = active.key;
      }
      const page = await getOrders(params);
      const records = page.records ?? [];
      const orders = reset ? records : this.data.orders.concat(records);
      this.setData({
        orders,
        current,
        hasMore: orders.length < (page.total ?? 0),
        loaded: true,
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '订单加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onTabChange(event) {
    const { key } = event.currentTarget.dataset;
    if (key === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: key, orders: [], current: 1, hasMore: true }, () => {
      this.loadOrders(true);
    });
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

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadOrders(false);
    }
  },
});
