/**
 * 团购套餐：在售套餐列表与一键下单（下单后跳订单详情完成支付）
 * @author come-often
 * @date 2026-11-10
 */
import { getBundles, purchaseBundle } from '../../api/bundleApi';

Page({
  data: {
    bundles: [],
    loading: false,
    loaded: false,
    buying: false,
  },

  async onShow() {
    await getApp().globalData.loginReady;
    await this.loadBundles();
  },

  async loadBundles() {
    this.setData({ loading: true });
    try {
      const bundles = await getBundles();
      this.setData({ bundles: bundles ?? [], loaded: true });
    } catch (error) {
      wx.showToast({ title: error.message ?? '套餐加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onRefresh() {
    await this.loadBundles();
  },

  async onTapBuy(event) {
    const { id, name } = event.currentTarget.dataset;
    if (this.data.buying) {
      return;
    }
    const result = await wx.showModal({
      title: '购买团购套餐',
      content: `确认购买「${name}」1 份？下单后请在订单详情页完成支付。`,
      confirmText: '确认下单',
    });
    if (!result.confirm) {
      return;
    }
    this.setData({ buying: true });
    try {
      const order = await purchaseBundle(id, 1);
      wx.showToast({ title: '下单成功', icon: 'success' });
      wx.navigateTo({
        url: `/packageOrder/pages/order-detail/order-detail?orderNo=${order.orderNo}`,
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '下单失败', icon: 'none' });
    } finally {
      this.setData({ buying: false });
    }
  },
});
