/**
 * 商家端小程序入口：按 openid 免密登录并加载权限
 * @author come-often
 * @date 2026-11-08
 */
import { getInfo } from './api/authApi';
import { autoLogin } from './utils/request';

App({
  globalData: {
    storeId: null,
    permissions: [],
    userInfo: null,
    loginReady: null,
    loginError: '',
  },
  onLaunch() {
    this.globalData.loginReady = autoLogin()
      .then(() => getInfo())
      .then((info) => {
        this.applyUserInfo(info);
        return info;
      })
      .catch((error) => {
        this.globalData.loginError = error.message ?? '免密登录失败';
        console.warn('商家端免密登录失败', error);
      });
  },
  applyUserInfo(info) {
    this.globalData.userInfo = info;
    this.globalData.storeId = info.storeId;
    this.globalData.permissions = info.permissions ?? [];
  },
});
