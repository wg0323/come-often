/**
 * 统一请求封装：员工 token 注入、401 免密续登与原请求单次重放
 * @author come-often
 * @date 2026-11-08
 */
import { API_BASE_URL } from '../config/env';

const TOKEN_KEY = 'merchantToken';
const MOCK_KEY = 'merchantMockKey';
const HTTP_UNAUTHORIZED = 401;
const SUCCESS_CODE = 0;
const LOGIN_PAGE = '/pages/login/login';

let renewPromise = null;

export const getToken = () => wx.getStorageSync(TOKEN_KEY);
export const setToken = (token) => wx.setStorageSync(TOKEN_KEY, token);
export const clearToken = () => wx.removeStorageSync(TOKEN_KEY);

const getOrCreateMockKey = () => {
  const existing = wx.getStorageSync(MOCK_KEY);
  if (existing) {
    return existing;
  }
  const created = `merchant-${Date.now()}-${Math.floor(Math.random() * 100000)}`;
  wx.setStorageSync(MOCK_KEY, created);
  return created;
};

const getWxLoginCode = async () => {
  try {
    const result = await wx.login();
    return result?.code ?? `mock-code-${Date.now()}`;
  } catch (error) {
    return `mock-code-${Date.now()}`;
  }
};

/** 首次绑定与免密登录共用同一组微信凭证参数。 */
export const buildLoginPayload = async () => ({
  code: await getWxLoginCode(),
  mockKey: getOrCreateMockKey(),
});

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

const parseSuccess = (response) => {
  const body = response.data ?? {};
  if (response.statusCode !== 200 || body.code !== SUCCESS_CODE) {
    throw new Error(body.message ?? '请求失败');
  }
  return body.data;
};

/** 按 openid 免密换发新 token；未绑定或已解绑时抛错，由调用方踢回登录页。 */
export const autoLogin = async () => {
  const payload = await buildLoginPayload();
  const response = await rawRequest('/auth/auto-login', {
    method: 'POST',
    data: payload,
    skipAuth: true,
  });
  const data = parseSuccess(response);
  setToken(data.token);
  return data;
};

const renewToken = () => {
  if (!renewPromise) {
    renewPromise = autoLogin().finally(() => {
      renewPromise = null;
    });
  }
  return renewPromise;
};

const redirectToLogin = () => {
  clearToken();
  wx.reLaunch({ url: LOGIN_PAGE });
};

const request = async (path, options = {}) => {
  const response = await rawRequest(path, options);
  if (response.statusCode === HTTP_UNAUTHORIZED && !options.skipAuth && !options.retried) {
    try {
      await renewToken();
    } catch (error) {
      redirectToLogin();
      throw error;
    }
    return request(path, { ...options, retried: true });
  }
  if (response.statusCode === HTTP_UNAUTHORIZED) {
    redirectToLogin();
    throw new Error('登录已失效，请重新登录');
  }
  if (response.statusCode < 200 || response.statusCode >= 300) {
    throw new Error(`请求失败：${response.statusCode}`);
  }
  return parseSuccess(response);
};

export default request;
