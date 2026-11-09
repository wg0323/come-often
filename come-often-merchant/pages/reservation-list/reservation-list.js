/**
 * 商家端预约管理：全部 / 待确认 / 已确认 / 已到店 / 未到店 五 Tab，一键确认、拒绝与签到
 * @author come-often
 * @date 2026-11-09
 */
import {
  checkinReservation,
  confirmReservation,
  pageReservations,
  rejectReservation,
} from '../../api/reservationApi';
import { RESERVATION_STATUS_TEXT } from '../../utils/display';
import { PAGE_SIZE } from '../../config/env';

const TABS = [
  { status: 'ALL', label: '全部' },
  { status: 'PENDING', label: '待确认' },
  { status: 'CONFIRMED', label: '已确认' },
  { status: 'CHECKED_IN', label: '已到店' },
  { status: 'NO_SHOW', label: '未到店' },
];

/** 拒绝原因必须落日志，这里给出口径固定的选项，避免小程序端弹编辑框的兼容问题。 */
const REJECT_REASONS = ['该时段已满', '顾客临时取消', '门店无法履约'];

Page({
  data: {
    tabs: TABS,
    activeStatus: 'ALL',
    date: '',
    reservations: [],
    current: 1,
    total: 0,
    hasMore: false,
    loading: false,
    submitting: false,
    refresherTriggered: false,
  },
  onShow() {
    this.loadReservations(true);
  },
  /** 下拉刷新由根 scroll-view 的 refresher 接管，页面已 disableScroll */
  async onRefresherRefresh() {
    this.setData({ refresherTriggered: true });
    await this.loadReservations(true);
    this.setData({ refresherTriggered: false });
  },
  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadReservations(false);
    }
  },
  async loadReservations(reset) {
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    const current = reset ? 1 : this.data.current + 1;
    try {
      const { date, activeStatus } = this.data;
      const page = await pageReservations({
        date,
        status: activeStatus === 'ALL' ? undefined : activeStatus,
        current,
        size: PAGE_SIZE,
      });
      const records = (page?.records ?? []).map((item) => ({
        ...item,
        key: item.reserveNo,
        statusText: RESERVATION_STATUS_TEXT[item.status] ?? item.status,
        timeText: `${item.reserveDate} ${item.timeSlot}`,
        peopleText: `${item.peopleCount} 位`,
        canConfirm: item.status === 'PENDING',
        canCheckin: item.status === 'CONFIRMED',
      }));
      const merged = reset ? records : this.data.reservations.concat(records);
      this.setData({
        reservations: merged,
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
  onTapTab(event) {
    const { status } = event.currentTarget.dataset;
    if (status === this.data.activeStatus) {
      return;
    }
    this.setData({ activeStatus: status, reservations: [], current: 1, total: 0, hasMore: false });
    this.loadReservations(true);
  },
  onDateChange(event) {
    this.setData({ date: event.detail.value });
    this.loadReservations(true);
  },
  onClearDate() {
    if (!this.data.date) {
      return;
    }
    this.setData({ date: '' }, () => this.loadReservations(true));
  },
  onTapConfirm(event) {
    const { reserveno } = event.currentTarget.dataset;
    this.submit(() => confirmReservation(reserveno));
  },
  onTapCheckin(event) {
    const { reserveno } = event.currentTarget.dataset;
    this.submit(() => checkinReservation(reserveno));
  },
  onTapReject(event) {
    const { reserveno } = event.currentTarget.dataset;
    wx.showActionSheet({
      itemList: REJECT_REASONS,
      success: (result) => {
        const reason = REJECT_REASONS[result.tapIndex];
        this.submit(() => rejectReservation(reserveno, reason));
      },
    });
  },
  async submit(action) {
    if (this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });
    wx.showLoading({ title: '处理中', mask: true });
    try {
      await action();
      wx.hideLoading();
      wx.showToast({ title: '操作成功', icon: 'success' });
      await this.loadReservations(true);
    } catch (error) {
      wx.hideLoading();
      wx.showToast({ title: error.message ?? '操作失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
