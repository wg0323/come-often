/**
 * 商家端新订单提醒埋点接口
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';
import { toQuery } from '../utils/query';

export const remindNewOrder = (orderNo) => request(`/notify/new-order${toQuery({ orderNo })}`, {
  method: 'POST',
});
