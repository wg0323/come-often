/**
 * 商家端订单看板：四 Tab 列表、前台轮询新单提醒、接单 / 出餐 / 确认取餐
 * @author come-often
 * @date 2026-11-09
 */
import { getToken } from '../../utils/request';
import { pageOrders, transitionOrder } from '../../api/orderApi';
import { getSummary } from '../../api/workbenchApi';
import { ORDER_STATUS_CLASS, ORDER_STATUS_TEXT, formatMoney, formatTime } from '../../utils/display';
import { ORDER_POLL_INTERVAL_MS, PAGE_SIZE, SUBSCRIBE_TEMPLATE_ID } from '../../config/env';

const TABS = [
  { status: 'PAID', label: '待接单' },
  { status: 'ACCEPTED', label: '制作中' },
  { status: 'READY', label: '待取餐' },
  { status: 'COMPLETED', label: '已完成' },
];

const ACTIONS = {
  PAID: { toStatus: 'ACCEPTED', label: '接单' },
  ACCEPTED: { toStatus: 'READY', label: '出餐' },
  READY: { toStatus: 'COMPLETED', label: '确认取餐' },
};

Page({
  data: {
    tabs: TABS,
    activeStatus: 'PAID',
    orders: [],
    current: 1,
    total: 0,
    hasMore: false,
    loading: false,
    actionLoading: false,
    newOrderHint: '',
    refresherTriggered: false,
  },
  pollTimer: null,
  lastPendingCount: null,
  async onLoad(options) {
    const status = options?.status;
    if (status && TABS.some((tab) => tab.status === status)) {
      this.setData({ activeStatus: status });
    }
  },
  async onShow() {
    if (!getToken()) {
      wx.reLaunch({ url: '/pages/login/login' });
      return;
    }
    await this.loadOrders(true);
    await this.syncPendingCount();
    this.startPolling();
  },
  onHide() {
    this.stopPolling();
  },
  onUnload() {
    this.stopPolling();
  },
  /** 下拉刷新与触底分页由根 scroll-view 接管，页面已 disableScroll */
  async onRefresherRefresh() {
    this.setData({ refresherTriggered: true });
    await this.loadOrders(true);
    this.setData({ refresherTriggered: false });
  },
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadOrders(false);
    }
  },
  async loadOrders(reset) {
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    const current = reset ? 1 : this.data.current + 1;
    try {
      const page = await pageOrders({
        status: this.data.activeStatus,
        current,
        size: PAGE_SIZE,
      });
      const records = (page?.records ?? []).map((order) => this.decorate(order));
      const orders = reset ? records : this.data.orders.concat(records);
      this.setData({
        orders,
        current: page?.current ?? current,
        total: page?.total ?? orders.length,
        hasMore: orders.length < (page?.total ?? orders.length),
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
  decorate(order) {
    const action = ACTIONS[order.status] ?? null;
    return {
      ...order,
      statusText: ORDER_STATUS_TEXT[order.status] ?? order.status,
      statusClass: ORDER_STATUS_CLASS[order.status] ?? 'tag--done',
      moneyText: formatMoney(order.payAmount),
      createTimeText: formatTime(order.createTime),
      remarkText: order.remark ? order.remark : '无备注',
      action,
    };
  },
  async syncPendingCount() {
    try {
      const summary = await getSummary();
      this.lastPendingCount = summary?.pendingAcceptCount ?? 0;
      this.setData({ newOrderHint: '' });
    } catch (error) {
      console.warn('工作台数字同步失败', error);
    }
  },
  startPolling() {
    this.stopPolling();
    this.pollTimer = setInterval(() => {
      this.checkNewOrders();
    }, ORDER_POLL_INTERVAL_MS);
  },
  stopPolling() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  },
  async checkNewOrders() {
    try {
      const summary = await getSummary();
      const pending = summary?.pendingAcceptCount ?? 0;
      if (this.lastPendingCount !== null && pending > this.lastPendingCount) {
        wx.vibrateLong();
        this.setData({ newOrderHint: `有新订单，待接单 ${pending} 单` });
        await this.loadOrders(true);
      }
      this.lastPendingCount = pending;
    } catch (error) {
      console.warn('轮询新订单失败', error);
    }
  },
  onTapTab(event) {
    const { status } = event.currentTarget.dataset;
    if (status === this.data.activeStatus) {
      return;
    }
    this.setData({ activeStatus: status, orders: [], current: 1, total: 0, hasMore: false });
    this.loadOrders(true);
  },
  onTapOrder(event) {
    const { orderno } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/order-detail/order-detail?orderNo=${orderno}` });
  },
  requestSubscribeOnce() {
    if (!SUBSCRIBE_TEMPLATE_ID) {
      return;
    }
    try {
      const pending = wx.requestSubscribeMessage({ tmplIds: [SUBSCRIBE_TEMPLATE_ID] });
      if (pending && typeof pending.catch === 'function') {
        pending.catch((error) => {
          console.warn('订阅消息授权失败或被拒绝', error);
        });
      }
    } catch (error) {
      console.warn('订阅消息接口不可用', error);
    }
  },
  async onTapAction(event) {
    const { orderno, tostatus } = event.currentTarget.dataset;
    if (this.data.actionLoading) {
      return;
    }
    if (tostatus === 'ACCEPTED') {
      // 计划 6.2：员工在点「接单」时授权一次商家端订阅消息
      this.requestSubscribeOnce();
    }
    this.setData({ actionLoading: true });
    wx.showLoading({ title: '处理中', mask: true });
    try {
      await transitionOrder(orderno, tostatus, '商家端操作');
      wx.hideLoading();
      wx.showToast({ title: '操作成功', icon: 'success' });
      await this.loadOrders(true);
      await this.syncPendingCount();
    } catch (error) {
      wx.hideLoading();
      wx.showToast({ title: error.message ?? '操作失败', icon: 'none' });
    } finally {
      this.setData({ actionLoading: false });
    }
  },
});
