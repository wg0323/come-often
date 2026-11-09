/**
 * 商家端预约管理接口
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';
import { toQuery } from '../utils/query';

export const pageReservations = (params) => request(`/reservation/page${toQuery(params)}`);

export const confirmReservation = (reserveNo) => request(`/reservation/${reserveNo}/confirm`, {
  method: 'POST',
});

export const rejectReservation = (reserveNo, remark) => request(`/reservation/${reserveNo}/reject`, {
  method: 'POST',
  data: { remark },
});

export const checkinReservation = (reserveNo) => request(`/reservation/${reserveNo}/checkin`, {
  method: 'POST',
});
