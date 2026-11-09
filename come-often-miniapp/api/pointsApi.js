/**
 * 顾客端积分接口：账户概览、明细、签到、分享、完善资料与会员等级
 * @author come-often
 * @date 2026-11-08
 */
import request from '../utils/request';

export const getPointsAccount = () => request('/points/account');

export const getPointsRecords = (params = {}) => request('/points/record/page', { data: params });

export const signInPoints = () => request('/points/sign-in', { method: 'POST' });

export const sharePoints = () => request('/points/share', { method: 'POST' });

export const completeProfilePoints = (data) => request('/points/profile', { method: 'POST', data });

export const getMemberLevel = () => request('/points/level');

export const getPointsRule = () => request('/points/rule');
