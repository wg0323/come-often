/**
 * 商家端桌台看板接口
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';
import { toQuery } from '../utils/query';

export const getTableBoard = (date) => request(`/table/board${toQuery({ date })}`);

export const updateTableStatus = (tableId, status) => request(`/table/${tableId}/status`, {
  method: 'PUT',
  data: { status },
});
