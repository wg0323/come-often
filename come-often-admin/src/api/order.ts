import { http } from './request';
import type { PageData } from './dish';

export interface OrderSummary {
  orderNo: string;
  orderType: string;
  status: string;
  statusText: string;
  totalAmount: number;
  payAmount: number;
  depositAmount: number;
  pickupCode: string;
  itemSummary: string;
  createTime: string;
}

export interface OrderItemView {
  dishId: string;
  dishName: string;
  specDesc: string;
  addonDesc: string;
  price: number;
  quantity: number;
  amount: number;
  objectKey: string;
}

export interface OrderLogView {
  fromStatus: string;
  toStatus: string;
  operatorType: string;
  remark: string;
  createTime: string;
}

export interface OrderDetail {
  id: string;
  orderNo: string;
  orderType: string;
  status: string;
  statusText: string;
  totalAmount: number;
  discountAmount: number;
  pointsAmount: number;
  depositAmount: number;
  payAmount: number;
  balanceAmount: number;
  containsPreorder: boolean;
  forcedDineIn: boolean;
  pickupCode: string;
  bizDate: string;
  pickupTime: string | null;
  dineInTime: string | null;
  remark: string;
  paidTime: string | null;
  completedTime: string | null;
  createTime: string;
  depositNote: string;
  items: OrderItemView[];
  logs: OrderLogView[];
}

export interface OrderTransitionPayload {
  toStatus: string;
  remark?: string;
}

export interface MqMessageItem {
  messageId: string;
  exchangeName: string;
  routingKey: string;
  status: string;
  retryCount: number;
  nextRetryTime: string;
  errorCause: string;
  createTime: string;
}

export interface RefundItem {
  id: string;
  refundNo: string;
  orderNo: string;
  orderStatus: string;
  refundAmount: number;
  reason: string;
  status: number;
  statusText: string;
  auditUserId: string;
  refundTime: string | null;
  createTime: string;
}

export interface RefundAuditPayload {
  approved: boolean;
  remark?: string;
}

export interface RefundApplyPayload {
  reason?: string;
}

const toQuery = (params: Record<string, string | number | undefined>): string => {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value));
    }
  });
  return query.toString();
};

/** PC 后台只使用 /api/admin/** 接口；顾客端下单与支付走小程序，不在此封装。 */
export const orderApi = {
  page: (params: Record<string, string | number | undefined>): Promise<PageData<OrderSummary>> =>
    http<PageData<OrderSummary>>(`/admin/order/page?${toQuery(params)}`),
  detail: (orderNo: string): Promise<OrderDetail> =>
    http<OrderDetail>(`/admin/order/${orderNo}`),
  transition: (orderNo: string, payload: OrderTransitionPayload): Promise<OrderDetail> =>
    http<OrderDetail>(`/admin/order/${orderNo}/transition`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  resendTimeout: (orderNo: string, delaySeconds: number): Promise<void> =>
    http<void>(`/admin/order/${orderNo}/resend-timeout?delaySeconds=${delaySeconds}`, {
      method: 'POST',
    }),
};

export const mqApi = {
  page: (params: Record<string, string | number | undefined>): Promise<PageData<MqMessageItem>> =>
    http<PageData<MqMessageItem>>(`/admin/mq/message?${toQuery(params)}`),
  republish: (messageId: string): Promise<void> =>
    http<void>(`/admin/mq/message/${messageId}/republish`, { method: 'POST' }),
};

/** M4 退款审核：金额由后端按已收流水计算，前端只提交审核结论与说明。 */
export const refundApi = {
  page: (params: Record<string, string | number | undefined>): Promise<PageData<RefundItem>> =>
    http<PageData<RefundItem>>(`/admin/refund?${toQuery(params)}`),
  audit: (refundNo: string, payload: RefundAuditPayload): Promise<RefundItem> =>
    http<RefundItem>(`/admin/refund/${refundNo}/audit`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  applyByAdmin: (orderNo: string, payload: RefundApplyPayload): Promise<RefundItem> =>
    http<RefundItem>(`/admin/refund/order/${orderNo}`, {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
};
