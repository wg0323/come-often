/**
 * 顾客端储值接口：账户概览（含赠送档位）、充值、流水
 * 充值当前是 Mock 渠道直接入账，真实微信支付待商户号到位后替换
 * @author come-often
 * @date 2026-11-10
 */
import request from '../utils/request';

export const getBalanceAccount = () => request('/balance/account');

export const rechargeBalance = (amount) => request('/balance/recharge', {
  method: 'POST',
  data: { amount },
});

export const getBalanceRecords = (params = {}) => request('/balance/record/page', { data: params });
