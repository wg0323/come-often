/**
 * 积分明细：账户概览 + 签到 / 分享入口 + 流水分页，金额与积分全部取接口返回
 * @author come-often
 * @date 2026-11-08
 */
import { getPointsAccount, getPointsRecords, sharePoints, signInPoints } from '../../../api/pointsApi';

const PAGE_SIZE = 20;

Page({
  data: {
    account: null,
    records: [],
    current: 1,
    hasMore: true,
    loading: false,
    loaded: false,
    signing: false,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
    await this.loadAccount();
    await this.loadRecords(true);
  },

  onShow() {
    if (this.data.loaded) {
      this.loadAccount();
    }
  },

  async loadAccount() {
    try {
      const account = await getPointsAccount();
      this.setData({ account });
    } catch (error) {
      wx.showToast({ title: error.message ?? '积分加载失败', icon: 'none' });
    }
  },

  async loadRecords(reset) {
    if (this.data.loading) {
      return;
    }
    const current = reset ? 1 : this.data.current + 1;
    this.setData({ loading: true });
    try {
      const page = await getPointsRecords({ current, size: PAGE_SIZE });
      const records = reset ? (page.records ?? []) : this.data.records.concat(page.records ?? []);
      this.setData({
        records,
        current,
        hasMore: records.length < (page.total ?? 0),
        loaded: true,
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '流水加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async onSignIn() {
    if (this.data.signing) {
      return;
    }
    this.setData({ signing: true });
    try {
      const account = await signInPoints();
      this.setData({ account });
      wx.showToast({ title: `签到成功 +${account.earnedPoints}`, icon: 'none' });
      await this.loadRecords(true);
    } catch (error) {
      wx.showToast({ title: error.message ?? '签到失败', icon: 'none' });
    } finally {
      this.setData({ signing: false });
    }
  },

  async onShare() {
    try {
      const account = await sharePoints();
      this.setData({ account });
      wx.showToast({ title: `分享成功 +${account.earnedPoints}`, icon: 'none' });
      await this.loadRecords(true);
    } catch (error) {
      wx.showToast({ title: error.message ?? '分享失败', icon: 'none' });
    }
  },

  onTapSignIn() {
    wx.navigateTo({ url: '/packageMember/pages/sign-in/sign-in' });
  },

  onTapLevel() {
    wx.navigateTo({ url: '/packageMember/pages/member-level/member-level' });
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadRecords(false);
    }
  },
});
