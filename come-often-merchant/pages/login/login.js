/**
 * 商家端首次登录：员工账号密码 + 微信绑定
 * @author come-often
 * @date 2026-11-08
 */
import { getInfo, loginWithAccount } from '../../api/authApi';
import { buildLoginPayload } from '../../utils/request';

Page({
  data: {
    username: 'manager',
    password: '',
    loading: false,
    errorText: '',
  },
  onInputUsername(event) {
    this.setData({ username: event.detail.value });
  },
  onInputPassword(event) {
    this.setData({ password: event.detail.value });
  },
  async onTapLogin() {
    const { username, password } = this.data;
    if (!username || !password) {
      this.setData({ errorText: '请输入账号与密码' });
      return;
    }
    this.setData({ loading: true, errorText: '' });
    try {
      const payload = await buildLoginPayload();
      await loginWithAccount(username, password, payload);
      const info = await getInfo();
      const app = getApp();
      app.applyUserInfo(info);
      app.globalData.loginReady = Promise.resolve(info);
      wx.reLaunch({ url: '/pages/index/index' });
    } catch (error) {
      this.setData({ errorText: error.message ?? '登录失败' });
    } finally {
      this.setData({ loading: false });
    }
  },
});
