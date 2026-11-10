/**
 * 排队取号：取号、查看叫号进度与取消；号码与等待桌数全部取服务端
 * @author come-often
 * @date 2026-11-09
 */
import { cancelQueueTicket, getCurrentQueueTicket, takeQueueTicket } from '../../api/queueApi';
import { getToken, redirectToLogin } from '../../utils/request';

const MIN_PEOPLE = 1;
const MAX_PEOPLE = 20;

/** 前台轮询间隔：叫号后顾客最迟 10 秒内看到，与 PC 后台排队页同频；离页立即停止 */
const POLL_INTERVAL_MS = 10 * 1000;

Page({
  data: {
    ticket: null,
    partySize: 2,
    loading: false,
    loadError: false,
    taking: false,
    refresherTriggered: false,
  },

  async onLoad() {
    if (!getToken()) {
      redirectToLogin();
      return;
    }
    await getApp().globalData.loginReady;
    await this.loadTicket();
  },

  async onShow() {
    this.startPolling();
    if (!this.shownOnce) {
      this.shownOnce = true;
      return;
    }
    await this.loadTicket(true);
  },

  onHide() {
    this.stopPolling();
  },

  onUnload() {
    this.stopPolling();
  },

  /** 页面在前台时每 10 秒拉一次当前号，叫号与过号都靠这次刷新体现 */
  startPolling() {
    this.stopPolling();
    this.pollTimer = setInterval(() => {
      if (!this.data.loading && !this.data.taking) {
        this.loadTicket(true);
      }
    }, POLL_INTERVAL_MS);
  },

  stopPolling() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  },

  /** 下拉刷新由根 scroll-view 的 refresher 接管，页面已 disableScroll */
  async onRefresherRefresh() {
    this.setData({ refresherTriggered: true });
    await this.loadTicket();
    this.setData({ refresherTriggered: false });
  },

  async onRetry() {
    await this.loadTicket();
  },

  /** silent 为 true 时不弹错误提示：轮询失败只是保持上一屏，不打扰用餐中的顾客 */
  async loadTicket(silent = false) {
    if (!silent) {
      this.setData({ loading: true, loadError: false });
    }
    try {
      this.setData({ ticket: await getCurrentQueueTicket() });
    } catch (error) {
      if (silent) {
        return;
      }
      this.setData({ loadError: true, ticket: null });
      wx.showToast({ title: error.message ?? '排队信息加载失败', icon: 'none' });
    } finally {
      if (!silent) {
        this.setData({ loading: false });
      }
    }
  },

  onChangeCount(event) {
    const delta = Number(event.currentTarget.dataset.delta);
    const partySize = Math.min(MAX_PEOPLE, Math.max(MIN_PEOPLE, this.data.partySize + delta));
    this.setData({ partySize });
  },

  async onTake() {
    if (this.data.taking) {
      return;
    }
    this.setData({ taking: true });
    try {
      this.setData({ ticket: await takeQueueTicket(this.data.partySize) });
      wx.showToast({ title: '取号成功', icon: 'success' });
    } catch (error) {
      wx.showToast({ title: error.message ?? '取号失败', icon: 'none' });
    } finally {
      this.setData({ taking: false });
    }
  },

  async onCancel() {
    const { confirm } = await wx.showModal({
      title: '取消取号',
      content: '取消后需要重新排队，确定取消吗？',
      confirmText: '确定取消',
    });
    if (!confirm) {
      return;
    }
    try {
      await cancelQueueTicket(this.data.ticket.ticketNo);
      this.setData({ ticket: null });
      wx.showToast({ title: '已取消', icon: 'success' });
    } catch (error) {
      wx.showToast({ title: error.message ?? '取消失败', icon: 'none' });
      await this.loadTicket();
    }
  },

  async onRefresh() {
    await this.loadTicket();
  },
});
