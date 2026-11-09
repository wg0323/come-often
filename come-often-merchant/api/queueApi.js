/**
 * 商家端排队叫号接口：当天队列、叫号与入座
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';
import { toQuery } from '../utils/query';

export const pageQueueTickets = (params = {}) => request(`/queue/page${toQuery(params)}`);

export const callTicket = (ticketNo) => request(`/queue/${encodeURIComponent(ticketNo)}/call`, {
  method: 'POST',
});

export const seatTicket = (ticketNo) => request(`/queue/${encodeURIComponent(ticketNo)}/seat`, {
  method: 'POST',
});
