/**
 * 我的卡包：未使用 / 已使用 / 已过期 三 Tab，金额与条件文案全部取接口返回
 * @author come-often
 * @date 2026-11-08
 */
import { getCoupons } from '../../../api/couponApi';

const TABS = [
  { key: 'unused', label: '未使用' },
  { key: 'used', label: '已使用' },
  { key: 'expired', label: '已过期' },
];

const PAGE_SIZE = 20;

Page({
  data: {
    tabs: TABS,
    activeTab: 'unused',
    coupons: [],
    current: 1,
    hasMore: true,
    loading: false,
    loaded: false,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
    await this.loadCoupons(true);
  },

  onShow() {
    // 从券码页核销回来时刷新当前 Tab，避免展示已使用的券
    if (this.data.loaded) {
      this.loadCoupons(true);
    }
  },

  async loadCoupons(reset) {
    if (this.data.loading) {
      return;
    }
    const current = reset ? 1 : this.data.current + 1;
    this.setData({ loading: true });
    try {
      const page = await getCoupons({ tab: this.data.activeTab, current, size: PAGE_SIZE });
      const records = page.records ?? [];
      const coupons = reset ? records : this.data.coupons.concat(records);
      this.setData({
        coupons,
        current,
        hasMore: coupons.length < (page.total ?? 0),
        loaded: true,
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '卡包加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onTabChange(event) {
    const { key } = event.currentTarget.dataset;
    if (key === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: key, coupons: [], current: 1, hasMore: true }, () => {
      this.loadCoupons(true);
    });
  },

  onTapCoupon(event) {
    const { id, status } = event.currentTarget.dataset;
    if (status !== 0) {
      return;
    }
    wx.navigateTo({ url: `/packageMember/pages/coupon-code/coupon-code?userCouponId=${id}` });
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadCoupons(false);
    }
  },
});
