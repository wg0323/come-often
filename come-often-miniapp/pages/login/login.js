/**
 * 顾客端登录页：微信手机号授权登录，游客 AppID 下用手输手机号兜底
 * @author come-often
 * @date 2026-11-09
 */
import { login } from '../../api/authApi';
import { CURRENT_ENV } from '../../config/env';
import { getOrCreateMockKey, getToken, setToken } from '../../utils/request';

const PHONE_PATTERN = /^1[3-9]\d{9}$/;
const HOME_PAGE = '/pages/index/index';

const getWxLoginCode = () => new Promise((resolve) => {
  wx.login({
    success: (result) => resolve(result?.code ?? ''),
    fail: () => resolve(''),
  });
});

Page({
  data: {
    devMode: CURRENT_ENV === 'dev',
    mockPhone: '',
    loading: false,
  },

  onLoad() {
    if (getToken()) {
      wx.reLaunch({ url: HOME_PAGE });
    }
  },

  onGetPhoneNumber(event) {
    const phoneCode = event.detail?.code ?? '';
    if (!phoneCode) {
      wx.showToast({
        title: this.data.devMode ? '未获取授权，请用下方开发模式登录' : '需要手机号授权才能登录',
        icon: 'none',
      });
      return;
    }
    this.submit({ phoneCode });
  },

  onInputPhone(event) {
    this.setData({ mockPhone: event.detail?.value ?? '' });
  },

  onDevLogin() {
    const phone = String(this.data.mockPhone).trim();
    if (!PHONE_PATTERN.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' });
      return;
    }
    this.submit({ mockPhone: phone });
  },

  /** code 只用于记录 openid，取不到不阻塞手机号登录 */
  async submit(payload) {
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    try {
      const code = await getWxLoginCode();
      const result = await login({ ...payload, code, mockKey: getOrCreateMockKey() });
      setToken(result.token);
      getApp().globalData.memberId = result.userId;
      wx.reLaunch({ url: HOME_PAGE });
    } catch (error) {
      wx.showToast({ title: error.message ?? '登录失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
});
