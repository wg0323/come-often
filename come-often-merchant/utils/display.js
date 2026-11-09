/**
 * 商家端展示层字典与格式化：状态文案 / 状态样式 / 金额 / 时间
 * @author come-often
 * @date 2026-11-09
 */

export const ORDER_STATUS_TEXT = {
  CREATED: '待支付',
  PAID: '待接单',
  ACCEPTED: '制作中',
  READY: '待取餐',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款审核中',
  REFUNDED: '已退款',
};

/** 四个订单态对应四色，状态色只用于状态（计划 4.8 / 4.10）。 */
export const ORDER_STATUS_CLASS = {
  PAID: 'tag--pending',
  ACCEPTED: 'tag--cooking',
  READY: 'tag--ready',
  COMPLETED: 'tag--done',
  CANCELLED: 'tag--done',
  REFUNDED: 'tag--done',
  REFUNDING: 'tag--cooking',
  CREATED: 'tag--done',
};

export const RESERVATION_STATUS_TEXT = {
  PENDING: '待确认',
  CONFIRMED: '已确认',
  CHECKED_IN: '已到店',
  NO_SHOW: '未到店',
  CANCELLED: '已取消',
};

export const TABLE_STATUS_OPTIONS = [
  { status: 0, label: '空闲' },
  { status: 1, label: '占用' },
  { status: 2, label: '已预约' },
];

export const TABLE_STATUS_TEXT = {
  0: '空闲',
  1: '占用',
  2: '已预约',
};

export const formatMoney = (value) => {
  if (value === undefined || value === null || value === '') {
    return '0';
  }
  const amount = Number(value);
  if (Number.isNaN(amount)) {
    return `${value}`;
  }
  return amount.toFixed(2).replace(/\.00$/, '');
};

export const formatTime = (value) => (value ? `${value}`.replace('T', ' ').slice(0, 16) : '');
