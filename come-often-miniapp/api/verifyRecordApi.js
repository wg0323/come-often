/**
 * 顾客端订单核销记录接口：本人本店的分页与详情
 * @author come-often
 * @date 2026-11-10
 */
import request from '../utils/request';

export const getVerifyRecords = (params = {}) => request('/verify-record/page', { data: params });

export const getVerifyRecord = (recordId) => request(`/verify-record/${recordId}`);
