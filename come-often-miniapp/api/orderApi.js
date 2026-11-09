/**
 * 顾客端订单接口：确认页、下单、支付、退款、取消、列表与详情
 * @author come-often
 * @date 2026-11-08
 */
import request from '../utils/request';

export const previewOrder = (payload) => request('/order/preview', {
  method: 'POST',
  data: payload,
});

export const createOrder = (payload) => request('/order', {
  method: 'POST',
  data: payload,
});

export const getOrders = (params = {}) => request('/order', { data: params });

export const getOrderDetail = (orderNo) => request(`/order/${orderNo}`);

export const payOrder = (orderNo, payAmount) => request(`/order/${orderNo}/pay`, {
  method: 'POST',
  data: payAmount === undefined ? {} : { payAmount },
});

export const cancelOrder = (orderNo) => request(`/order/${orderNo}/cancel`, {
  method: 'POST',
});

/** M4：金额由服务端按已收流水计算，请求体只带原因。 */
export const applyRefund = (orderNo, reason = '') => request(`/order/${orderNo}/refund`, {
  method: 'POST',
  data: { reason },
});

export const getRefundDetail = (orderNo) => request(`/order/${orderNo}/refund`);
