/**
 * 商家端订单看板接口
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';
import { toQuery } from '../utils/query';

export const pageOrders = (params) => request(`/order/page${toQuery(params)}`);

export const getOrder = (orderNo) => request(`/order/${orderNo}`);

export const transitionOrder = (orderNo, toStatus, remark) => request(`/order/${orderNo}/transition`, {
  method: 'POST',
  data: { toStatus, remark },
});

export const refundOrder = (orderNo, reason) => request(`/order/${orderNo}/refund`, {
  method: 'POST',
  data: { reason },
});
