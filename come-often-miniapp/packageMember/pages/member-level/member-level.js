/**
 * 会员等级：当前等级、本年度消费额、累计积分与全部档位，差值由服务端算好
 * @author come-often
 * @date 2026-11-08
 */
import { getMemberLevel } from '../../../api/pointsApi';

Page({
  data: {
    overview: null,
    loading: false,
    loaded: false,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
    await this.loadOverview();
  },

  async loadOverview() {
    this.setData({ loading: true });
    try {
      const overview = await getMemberLevel();
      this.setData({ overview, loaded: true });
    } catch (error) {
      wx.showToast({ title: error.message ?? '等级加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
});
