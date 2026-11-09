/**
 * 顾客端门店接口：列表与详情（含营业时间与桌台）
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';

export const getStores = () => request('/store/list');

export const getStoreDetail = (storeId) => request(`/store/${storeId}`);
