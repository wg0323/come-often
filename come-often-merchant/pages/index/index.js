/**
 * 商家端工作台：订单三态数字、今日预约与营收、按权限渲染的功能宫格
 * @author come-often
 * @date 2026-11-09
 */
import { getToken } from '../../utils/request';
import { getSummary } from '../../api/workbenchApi';
import { formatMoney } from '../../utils/display';

const ALL_GRIDS = [
  { key: 'order', label: '订单看板', perm: 'order:accept', url: '/pages/order-list/order-list' },
  { key: 'scan', label: '扫码核销', perm: 'scan:use', url: '/pages/scan/scan' },
  { key: 'reservation', label: '预约管理', perm: 'reservation:audit', url: '/pages/reservation-list/reservation-list' },
  { key: 'table', label: '桌台看板', perm: 'table:manage', url: '/pages/table-board/table-board' },
  { key: 'queue', label: '排队叫号', perm: 'table:manage', url: '/pages/queue/queue' },
  { key: 'soldout', label: '菜品售罄', perm: 'dish:soldout', url: '/pages/dish-soldout/dish-soldout' },
  { key: 'record', label: '核销记录', perm: 'coupon:record', url: '/pages/coupon-record/coupon-record' },
];

Page({
  data: {
    stats: [
      { key: 'PAID', label: '待接单', count: 0, statusClass: 'stat-count--pending', modifier: 'pending' },
      { key: 'ACCEPTED', label: '制作中', count: 0, statusClass: 'stat-count--cooking', modifier: 'cooking' },
      { key: 'READY', label: '待取餐', count: 0, statusClass: 'stat-count--ready', modifier: 'ready' },
    ],
    todayReservationCount: 0,
    todayRevenueText: '0',
    grids: [],
    employeeName: '',
    roleText: '',
    storeText: '',
    loading: false,
    refresherTriggered: false,
  },
  async onShow() {
    const app = getApp();
    if (app.globalData.loginReady) {
      await app.globalData.loginReady;
    }
    if (!getToken()) {
      wx.reLaunch({ url: '/pages/login/login' });
      return;
    }
    const { permissions, userInfo } = app.globalData;
    const roles = userInfo?.roles ?? [];
    this.setData({
      grids: ALL_GRIDS.filter((grid) => (permissions ?? []).includes(grid.perm)),
      employeeName: userInfo?.realName ?? '',
      roleText: roles.join(' / '),
    });
    await this.loadSummary();
  },
  /** 下拉刷新由根 scroll-view 的 refresher 接管，页面已 disableScroll */
  async onRefresherRefresh() {
    this.setData({ refresherTriggered: true });
    await this.loadSummary();
    this.setData({ refresherTriggered: false });
  },
  async loadSummary() {
    this.setData({ loading: true });
    try {
      const summary = await getSummary();
      this.setData({
        stats: this.data.stats.map((item) => {
          if (item.key === 'PAID') {
            return { ...item, count: summary?.pendingAcceptCount ?? 0 };
          }
          if (item.key === 'ACCEPTED') {
            return { ...item, count: summary?.cookingCount ?? 0 };
          }
          return { ...item, count: summary?.readyCount ?? 0 };
        }),
        todayReservationCount: summary?.todayReservationCount ?? 0,
        todayRevenueText: formatMoney(summary?.todayRevenue),
        storeText: summary?.storeName ?? '',
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
  onTapStat(event) {
    const { status } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/order-list/order-list?status=${status}` });
  },
  onTapGrid(event) {
    const { url } = event.currentTarget.dataset;
    if (!url) {
      return;
    }
    wx.navigateTo({ url });
  },
});
