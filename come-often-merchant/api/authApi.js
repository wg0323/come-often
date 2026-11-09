/**
 * 商家端认证接口
 * @author come-often
 * @date 2026-11-08
 */
import request from '../utils/request';

export const loginWithAccount = (username, password, payload) => request('/auth/login', {
  method: 'POST',
  data: { username, password, ...payload },
  skipAuth: true,
});

export const getInfo = () => request('/auth/info');

export const logout = () => request('/auth/logout', { method: 'POST' });
