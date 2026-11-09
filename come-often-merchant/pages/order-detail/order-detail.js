/**
 * 商家端订单详情：菜品明细、备注、取餐码、状态日志、接单 / 出餐 / 确认取餐与发起退单
 * @author come-often
 * @date 2026-11-09
 */
import { getOrder, refundOrder, transitionOrder } from '../../api/orderApi';
import { ORDER_STATUS_CLASS, ORDER_STATUS_TEXT, formatMoney, formatTime } from '../../utils/display';

const ACTIONS = {
  PAID: { toStatus: 'ACCEPTED', label: '接单' },
  ACCEPTED: { toStatus: 'READY', label: '出餐' },
  READY: { toStatus: 'COMPLETED', label: '确认取餐' },
};

/** 可发起退单的状态：待接单 / 制作中 / 待取餐。 */
const REFUNDABLE = ['PAID', 'ACCEPTED', 'READY'];

Page({
  data: {
    orderNo: '',
    order: null,
    items: [],
    logs: [],
    action: null,
    canRefund: false,
    loading: false,
    submitting: false,
    refresherTriggered: false,
  },
  onLoad(options) {
    this.setData({ orderNo: options?.orderNo ?? '' });
  },
  onShow() {
    this.loadOrder();
  },
  /** 下拉刷新由根 scroll-view 的 refresher 接管，页面已 disableScroll */
  async onRefresherRefresh() {
    this.setData({ refresherTriggered: true });
    await this.loadOrder();
    this.setData({ refresherTriggered: false });
  },
  async loadOrder() {
    if (!this.data.orderNo) {
      return;
    }
    this.setData({ loading: true });
    try {
      const order = await getOrder(this.data.orderNo);
      this.setData({
        order: {
          ...order,
          statusText: ORDER_STATUS_TEXT[order.status] ?? order.status,
          statusClass: ORDER_STATUS_CLASS[order.status] ?? 'tag--done',
          payAmountText: formatMoney(order.payAmount),
          totalAmountText: formatMoney(order.totalAmount),
          discountAmountText: formatMoney(order.discountAmount),
          balanceAmountText: formatMoney(order.balanceAmount),
          createTimeText: formatTime(order.createTime),
          remarkText: order.remark ? order.remark : '无备注',
          orderTypeText: order.orderType === 'DINE_IN' ? '到店用餐' : '自提',
        },
        items: (order.items ?? []).map((item, index) => ({
          ...item,
          key: `${item.dishId}-${index}`,
          amountText: formatMoney(item.amount),
          specText: [item.specDesc, item.addonDesc].filter(Boolean).join(' / '),
        })),
        logs: (order.logs ?? []).map((log, index) => {
          const fromText = ORDER_STATUS_TEXT[log.fromStatus] || log.fromStatus || '创建';
          const toText = ORDER_STATUS_TEXT[log.toStatus] || log.toStatus;
          return {
            ...log,
            key: `${index}-${log.toStatus}`,
            statusText: `${fromText} → ${toText}`,
            timeText: formatTime(log.createTime),
          };
        }),
        action: ACTIONS[order.status] ?? null,
        canRefund: REFUNDABLE.indexOf(order.status) >= 0,
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
  async onTapAction() {
    const { action } = this.data;
    const { orderNo } = this.data;
    if (!action || this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });
    wx.showLoading({ title: '处理中', mask: true });
    try {
      await transitionOrder(orderNo, action.toStatus, '商家端操作');
      wx.hideLoading();
      wx.showToast({ title: '操作成功', icon: 'success' });
      await this.loadOrder();
    } catch (error) {
      wx.hideLoading();
      wx.showToast({ title: error.message ?? '操作失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },
  onTapRefund() {
    wx.showModal({
      title: '发起退单',
      content: '退款金额由系统按已收款计算；制作中与待取餐订单需 PC 后台审核。',
      confirmText: '确认退单',
      success: async (result) => {
        if (!result.confirm) {
          return;
        }
        await this.submitRefund();
      },
    });
  },
  async submitRefund() {
    if (this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });
    wx.showLoading({ title: '提交中', mask: true });
    try {
      await refundOrder(this.data.orderNo, '商家端发起退款');
      wx.hideLoading();
      wx.showToast({ title: '已提交', icon: 'success' });
      await this.loadOrder();
    } catch (error) {
      wx.hideLoading();
      wx.showToast({ title: error.message ?? '提交失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
