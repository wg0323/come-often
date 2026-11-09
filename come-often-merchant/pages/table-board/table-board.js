/**
 * 商家端桌台看板：三态宫格，包间显示人数与预约人，点格改状态
 * @author come-often
 * @date 2026-11-09
 */
import { getTableBoard, updateTableStatus } from '../../api/tableApi';
import { TABLE_STATUS_OPTIONS, TABLE_STATUS_TEXT } from '../../utils/display';

const todayString = () => {
  const now = new Date();
  const month = `${now.getMonth() + 1}`.padStart(2, '0');
  const day = `${now.getDate()}`.padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
};

const STATUS_CLASS = {
  0: 'cell--idle',
  1: 'cell--busy',
  2: 'cell--booked',
};

Page({
  data: {
    date: todayString(),
    tables: [],
    loading: false,
    submitting: false,
    refresherTriggered: false,
  },
  onShow() {
    this.loadBoard();
  },
  /** 下拉刷新由根 scroll-view 的 refresher 接管，页面已 disableScroll */
  async onRefresherRefresh() {
    this.setData({ refresherTriggered: true });
    await this.loadBoard();
    this.setData({ refresherTriggered: false });
  },
  async loadBoard() {
    this.setData({ loading: true });
    try {
      const board = await getTableBoard(this.data.date);
      const reservations = board?.reservations ?? [];
      const byTable = {};
      reservations.forEach((item) => {
        if (item.tableId) {
          byTable[item.tableId] = item;
        }
      });
      const tables = (board?.tables ?? []).map((table) => {
        const reservation = byTable[table.tableId] ?? null;
        return {
          ...table,
          statusText: TABLE_STATUS_TEXT[table.status] ?? '未知',
          statusClass: STATUS_CLASS[table.status] ?? 'cell--idle',
          isPrivate: `${table.tableType}`.indexOf('PRIVATE') === 0,
          peopleText: reservation ? `${reservation.peopleCount} 位` : '',
          contactText: reservation ? reservation.contactName : '',
          timeText: reservation ? reservation.timeSlot : '',
        };
      });
      this.setData({ tables });
    } catch (error) {
      wx.showToast({ title: error.message ?? '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
  onDateChange(event) {
    this.setData({ date: event.detail.value });
    this.loadBoard();
  },
  onTapCell(event) {
    const { tableid } = event.currentTarget.dataset;
    wx.showActionSheet({
      itemList: TABLE_STATUS_OPTIONS.map((item) => item.label),
      success: async (result) => {
        const option = TABLE_STATUS_OPTIONS[result.tapIndex];
        await this.submitStatus(tableid, option.status);
      },
    });
  },
  async submitStatus(tableId, status) {
    if (this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });
    wx.showLoading({ title: '更新中', mask: true });
    try {
      await updateTableStatus(tableId, status);
      wx.hideLoading();
      wx.showToast({ title: '已更新', icon: 'success' });
      await this.loadBoard();
    } catch (error) {
      wx.hideLoading();
      wx.showToast({ title: error.message ?? '更新失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
