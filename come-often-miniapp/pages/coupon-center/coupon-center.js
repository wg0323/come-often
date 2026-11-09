/**
 * 领券中心（tabBar）：可领券列表、领券与卡包入口
 * @author come-often
 * @date 2026-11-09
 */
import { getCouponCenter, receiveCoupon } from '../../api/couponApi';
import { getToken, redirectToLogin } from '../../utils/request';

const decorate = (coupon) => ({
  ...coupon,
  buttonText: coupon.received ? '已领取' : (coupon.canReceive ? '立即领取' : '已领完'),
  endTimeText: coupon.endTime ? `${coupon.endTime}`.slice(0, 10) : '长期有效',
});

Page({
  data: {
    coupons: [],
    receivedCount: 0,
    loading: false,
    loadError: false,
    refresherTriggered: false,
  },

  async onLoad() {
    if (!getToken()) {
      redirectToLogin();
      return;
    }
    await getApp().globalData.loginReady;
    await this.loadCenter();
  },

  async onShow() {
    if (!this.shownOnce) {
      this.shownOnce = true;
      return;
    }
    await this.loadCenter();
  },

  /** 下拉刷新由根 scroll-view 的 refresher 接管，页面已 disableScroll */
  async onRefresherRefresh() {
    this.setData({ refresherTriggered: true });
    await this.loadCenter();
    this.setData({ refresherTriggered: false });
  },

  async onRetry() {
    await this.loadCenter();
  },

  async loadCenter() {
    this.setData({ loading: true, loadError: false });
    try {
      const list = await getCouponCenter();
      const coupons = (list ?? []).map(decorate);
      this.setData({
        coupons,
        receivedCount: coupons.filter((coupon) => coupon.received).length,
      });
    } catch (error) {
      this.setData({ loadError: true, coupons: [] });
      wx.showToast({ title: error.message ?? '券列表加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onReceive(event) {
    const { templateId, canReceive } = event.currentTarget.dataset;
    if (!canReceive) {
      wx.showToast({ title: '这张券已领取或已领完', icon: 'none' });
      return;
    }
    if (this.receiving) {
      return;
    }
    this.receiving = true;
    try {
      await receiveCoupon(templateId);
      wx.showToast({ title: '领取成功', icon: 'success' });
      await this.loadCenter();
    } catch (error) {
      wx.showToast({ title: error.message ?? '领取失败', icon: 'none' });
      await this.loadCenter();
    } finally {
      this.receiving = false;
    }
  },

  onTapWallet() {
    wx.navigateTo({ url: '/packageMember/pages/coupon-wallet/coupon-wallet' });
  },
});
