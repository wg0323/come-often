/**
 * 商家端「我的」：员工姓名、所属门店、角色、清缓存与退出登录
 * @author come-often
 * @date 2026-11-09
 */
import { getInfo, logout } from '../../api/authApi';
import { getSummary } from '../../api/workbenchApi';
import { clearToken, getToken } from '../../utils/request';

Page({
  data: {
    realName: '',
    username: '',
    roleText: '',
    storeText: '',
    permissionCount: 0,
    loading: false,
  },
  async onShow() {
    if (!getToken()) {
      wx.reLaunch({ url: '/pages/login/login' });
      return;
    }
    this.setData({ loading: true });
    try {
      const info = await getInfo();
      const summary = await getSummary().catch(() => null);
      this.setData({
        realName: info?.realName ?? '',
        username: info?.username ?? '',
        roleText: (info?.roles ?? []).join(' / '),
        storeText: summary?.storeName ?? '',
        permissionCount: (info?.permissions ?? []).length,
      });
      const app = getApp();
      app.applyUserInfo(info);
    } catch (error) {
      wx.showToast({ title: error.message ?? '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
  onTapClearCache() {
    wx.showModal({
      title: '清除本地缓存',
      content: '清除后需要重新用员工账号登录，确定继续吗？',
      success: (result) => {
        if (!result.confirm) {
          return;
        }
        wx.clearStorageSync();
        wx.reLaunch({ url: '/pages/login/login' });
      },
    });
  },
  onTapLogout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后需要重新输入员工账号与密码。',
      success: async (result) => {
        if (!result.confirm) {
          return;
        }
        try {
          await logout();
        } catch (error) {
          console.warn('退出登录接口失败，仍清理本地凭证', error);
        }
        clearToken();
        wx.reLaunch({ url: '/pages/login/login' });
      },
    });
  },
});
