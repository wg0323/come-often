/**
 * 顾客端认证接口
 * @author come-often
 * @date 2026-11-08
 */
import request from '../utils/request';

/** 手机号登录：phoneCode 来自 getPhoneNumber 授权，mockPhone 仅 dev 手输。 */
export const login = (data) => request('/auth/login', { method: 'POST', data, skipAuth: true });

export const logout = () => request('/auth/logout', { method: 'POST' });

export const getInfo = () => request('/auth/info');
