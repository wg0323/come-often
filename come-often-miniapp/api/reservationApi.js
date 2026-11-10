/**
 * 顾客端预约接口：元数据、时段余量、创建、我的预约、取消改期与签到码
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';

export const getReservationMeta = () => request('/reservation/meta');

export const getReservationSlots = (date) => request('/reservation/slot', { data: { date } });

/** M14 B4：指定日期时段仍可锁定的具体包间 */
export const getReservationRooms = (params = {}) => request('/reservation/rooms', { data: params });

export const createReservation = (data) => request('/reservation', { method: 'POST', data });

export const getMyReservations = (params = {}) => request('/reservation/page', { data: params });

export const getReservationDetail = (reserveNo) => request(`/reservation/${reserveNo}`);

export const cancelReservation = (reserveNo) =>
  request(`/reservation/${reserveNo}/cancel`, { method: 'POST' });

export const rescheduleReservation = (reserveNo, data) =>
  request(`/reservation/${reserveNo}/reschedule`, { method: 'POST', data });

export const getReservationCode = (reserveNo) => request(`/reservation/${reserveNo}/code`);
