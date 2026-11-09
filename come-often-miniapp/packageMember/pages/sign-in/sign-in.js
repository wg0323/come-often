/**
 * 每日签到：显示余额与今日签到状态，规则与签到积分全部取接口返回
 * @author come-often
 * @date 2026-11-08
 */
import { getPointsAccount, getPointsRule, signInPoints } from '../../../api/pointsApi';

Page({
  data: {
    account: null,
    rule: null,
    loading: false,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
    await this.loadData();
  },

  onShow() {
    if (this.data.account) {
      this.loadData();
    }
  },

  async loadData() {
    try {
      const [account, rule] = await Promise.all([getPointsAccount(), getPointsRule()]);
      this.setData({ account, rule });
    } catch (error) {
      wx.showToast({ title: error.message ?? '签到状态加载失败', icon: 'none' });
    }
  },

  async onSignIn() {
    if (this.data.loading || this.data.account?.signedToday) {
      return;
    }
    this.setData({ loading: true });
    try {
      const account = await signInPoints();
      this.setData({ account });
      wx.showToast({ title: `签到成功 +${account.earnedPoints}`, icon: 'none' });
    } catch (error) {
      wx.showToast({ title: error.message ?? '签到失败', icon: 'none' });
      await this.loadData();
    } finally {
      this.setData({ loading: false });
    }
  },
});
