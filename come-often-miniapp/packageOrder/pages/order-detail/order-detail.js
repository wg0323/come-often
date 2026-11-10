/**
 * 订单详情页（订单分包）：取餐码 / 到店核销码、金额快照、明细与状态日志
 * @author come-often
 * @date 2026-11-08
 */
import { cancelOrder, getOrderDetail, getOrderQrCode, payOrder } from '../../../api/orderApi';
import { buildImageUrl } from '../../../utils/image';

Page({
  data: {
    orderNo: '',
    order: null,
    qrImageBase64: '',
    loading: false,
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
    await this.loadDetail();
  },

  async loadDetail() {
    this.setData({ loading: true });
    try {
      const order = await getOrderDetail(this.data.orderNo);
      this.setData({
        order: {
          ...order,
          items: (order.items ?? []).map((item) => ({
            ...item,
            imageUrl: buildImageUrl(item.objectKey),
          })),
        },
      });
      await this.loadQrCode();
    } catch (error) {
      wx.showToast({ title: error.message ?? '订单详情加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /** 二维码只是展示增强：出图失败时清空图片，码串仍在页面上，可人工输码 */
  async loadQrCode() {
    try {
      const qr = await getOrderQrCode(this.data.orderNo);
      this.setData({ qrImageBase64: qr?.qrImageBase64 ?? '' });
    } catch (error) {
      this.setData({ qrImageBase64: '' });
    }
  },

  async onPay() {
    if (this.data.submitting) {
      return;
    }
    // M4：金额由服务端订单快照给出，页面不传金额也不做计算
    this.setData({ submitting: true });
    try {
      await payOrder(this.data.orderNo);
      wx.showToast({ title: '支付成功', icon: 'success' });
      await this.loadDetail();
    } catch (error) {
      wx.showToast({ title: error.message ?? '支付失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  async onCancel() {
    if (this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });
    try {
      await cancelOrder(this.data.orderNo);
      wx.showToast({ title: '订单已取消', icon: 'none' });
      await this.loadDetail();
    } catch (error) {
      wx.showToast({ title: error.message ?? '取消失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  onApplyRefund() {
    wx.navigateTo({
      url: `/packageOrder/pages/refund-apply/refund-apply?orderNo=${this.data.orderNo}`,
    });
  },

  onGoReview() {
    wx.navigateTo({ url: `/packageOrder/pages/review/review?orderNo=${this.data.orderNo}` });
  },

  /** 订单列表已移出 tabBar：从列表进来的直接回退，其余入口（确认订单等）新建页面 */
  onGoOrders() {
    const pages = getCurrentPages();
    const previous = pages.length > 1 ? pages[pages.length - 2] : null;
    if (previous && previous.route === 'pages/order-list/order-list') {
      wx.navigateBack();
      return;
    }
    wx.navigateTo({ url: '/pages/order-list/order-list' });
  },
});
