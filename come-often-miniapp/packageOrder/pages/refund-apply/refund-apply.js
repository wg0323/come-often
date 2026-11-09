/**
 * 退款申请页（订单分包）：填写原因并提交，金额由服务端按已收流水计算
 * @author come-often
 * @date 2026-11-08
 */
import { applyRefund } from '../../../api/orderApi';

Page({
  data: {
    orderNo: '',
    reason: '',
    submitting: false,
  },

  async onLoad(options) {
    await getApp().globalData.loginReady;
    const orderNo = options?.orderNo ?? '';
    if (!orderNo) {
      wx.showToast({ title: '缺少订单号', icon: 'none' });
      return;
    }
    this.setData({ orderNo });
  },

  onReasonInput(event) {
    this.setData({ reason: event.detail.value });
  },

  onBack() {
    wx.navigateBack();
  },

  async onSubmit() {
    if (this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });
    try {
      const refund = await applyRefund(this.data.orderNo, this.data.reason.trim());
      const message = refund.status === 0 ? '已提交退款审核' : '退款已受理';
      wx.showToast({ title: message, icon: 'none' });
      const orderNo = this.data.orderNo;
      setTimeout(() => {
        wx.redirectTo({ url: `/packageOrder/pages/order-detail/order-detail?orderNo=${orderNo}` });
      }, 800);
    } catch (error) {
      wx.showToast({ title: error.message ?? '退款申请失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
