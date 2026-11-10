/**
 * 顾客端团购套餐接口：在售列表、详情与按份数下单
 * @author come-often
 * @date 2026-11-10
 */
import request from '../utils/request';

export const getBundles = () => request('/bundle/list');

export const getBundleDetail = (bundleId) => request(`/bundle/${bundleId}`);

/** 金额由服务端按套餐价计算，端上只传份数 */
export const purchaseBundle = (bundleId, quantity = 1) =>
  request(`/bundle/${bundleId}/purchase`, { method: 'POST', data: { quantity } });
