/**
 * 统一请求封装：token 注入与 401 统一回登录页
 * @author come-often
 * @date 2026-11-08
 */
import { API_BASE_URL } from '../config/env';

const TOKEN_KEY = 'appToken';
const MOCK_KEY = 'appMockKey';
const HTTP_UNAUTHORIZED = 401;
const SUCCESS_CODE = 0;
const LOGIN_PAGE = '/pages/login/login';

export const getToken = () => wx.getStorageSync(TOKEN_KEY);
export const setToken = (token) => wx.setStorageSync(TOKEN_KEY, token);
export const clearToken = () => wx.removeStorageSync(TOKEN_KEY);

/** dev Mock 下 openid 由 mockKey 派生，持久化后可复现同一个微信号。 */
export const getOrCreateMockKey = () => {
  const existing = wx.getStorageSync(MOCK_KEY);
  if (existing) {
    return existing;
  }
  const created = `app-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  wx.setStorageSync(MOCK_KEY, created);
  return created;
};

let redirecting = false;

/** 凭证失效统一回登录页；登录页自身不跳，避免死循环。 */
export const redirectToLogin = () => {
  clearToken();
  if (redirecting) {
    return;
  }
  const pages = getCurrentPages();
  const current = pages.length > 0 ? pages[pages.length - 1].route : '';
  if (current === LOGIN_PAGE.replace(/^\//, '')) {
    return;
  }
  redirecting = true;
  wx.reLaunch({
    url: LOGIN_PAGE,
    complete: () => {
      redirecting = false;
    },
  });
};

const rawRequest = (path, options) => new Promise((resolve, reject) => {
  const header = { 'Content-Type': 'application/json', ...options.header };
  const token = options.skipAuth ? '' : getToken();
  if (token) {
    header.Authorization = `Bearer ${token}`;
  }
  wx.request({
    url: `${API_BASE_URL}${path}`,
    method: options.method ?? 'GET',
    data: options.data ?? {},
    header,
    success: resolve,
    fail: reject,
  });
});

const request = async (path, options = {}) => {
  const response = await rawRequest(path, options);
  if (response.statusCode === HTTP_UNAUTHORIZED) {
    if (!options.skipAuth) {
      redirectToLogin();
    }
    throw new Error('登录已失效，请重新登录');
  }
  if (response.statusCode < 200 || response.statusCode >= 300) {
    throw new Error(`请求失败：${response.statusCode}`);
  }
  const body = response.data ?? {};
  if (body.code !== SUCCESS_CODE) {
    throw new Error(body.message ?? '请求失败');
  }
  return body.data;
};

export default request;
