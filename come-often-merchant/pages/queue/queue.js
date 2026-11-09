/**
 * 商家端排队叫号：当天队列按状态筛选，支持叫号与入座
 * @author come-often
 * @date 2026-11-09
 */
import { callTicket, pageQueueTickets, seatTicket } from '../../api/queueApi';

const PAGE_SIZE = 20;

const formatTime = (value) => `${value ?? ''}`.slice(5, 16);

Page({
  data: {
    tickets: [],
    statusFilter: '',
    loading: false,
    loadError: false,
    submitting: false,
    refresherTriggered: false,
  },

  onShow() {
    this.loadTickets();
  },

  /** 下拉刷新由根 scroll-view 的 refresher 接管，页面已 disableScroll */
  async onRefresherRefresh() {
    this.setData({ refresherTriggered: true });
    await this.loadTickets();
    this.setData({ refresherTriggered: false });
  },

  onSwitchFilter(event) {
    this.setData({ statusFilter: event.currentTarget.dataset.status }, () => this.loadTickets());
  },

  async loadTickets() {
    this.setData({ loading: true, loadError: false });
    const params = { current: 1, size: PAGE_SIZE };
    if (this.data.statusFilter) {
      params.status = this.data.statusFilter;
    }
    try {
      const result = await pageQueueTickets(params);
      const tickets = (result?.records ?? []).map((ticket) => ({
        ...ticket,
        createTimeText: formatTime(ticket.createTime),
      }));
      this.setData({ tickets });
    } catch (error) {
      this.setData({ loadError: true, tickets: [] });
      wx.showToast({ title: error.message ?? '队列加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onAction(event) {
    const { ticketNo, action } = event.currentTarget.dataset;
    if (this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });
    try {
      if (action === 'call') {
        await callTicket(ticketNo);
      } else {
        await seatTicket(ticketNo);
      }
      wx.showToast({ title: '操作成功', icon: 'success' });
      await this.loadTickets();
    } catch (error) {
      wx.showToast({ title: error.message ?? '操作失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  async onRefresh() {
    await this.loadTickets();
  },
});
