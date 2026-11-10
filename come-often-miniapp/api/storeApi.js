/**
 * 顾客端门店接口：列表、详情（含营业时间与桌台）与桌码绑定校验
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';

export const getStores = () => request('/store/list');

export const getStoreDetail = (storeId) => request(`/store/${storeId}`);

/** 桌码绑定：服务端校验桌号后回桌台信息，桌号不存在返回 30091 */
export const bindStoreTable = (tableCode) => request('/store/table/bind', {
  method: 'POST',
  data: { tableCode },
});
