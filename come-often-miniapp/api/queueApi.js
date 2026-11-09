/**
 * 顾客端排队叫号接口：取号、我的号与取消
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';

export const takeQueueTicket = (partySize) => request('/queue/ticket', {
  method: 'POST',
  data: { partySize },
});

export const getCurrentQueueTicket = () => request('/queue/current');

export const cancelQueueTicket = (ticketNo) => request(`/queue/ticket/${encodeURIComponent(ticketNo)}/cancel`, {
  method: 'POST',
});
