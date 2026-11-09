/**
 * 商家端菜品售罄接口
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';

export const listSoldOut = () => request('/dish/soldout');

export const toggleSoldOut = (dishId, soldOut) => request('/dish/soldout', {
  method: 'PUT',
  data: { dishId, soldOut },
});
