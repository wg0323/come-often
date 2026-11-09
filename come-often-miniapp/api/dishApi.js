/**
 * 顾客端菜品接口：分类、列表、详情、搜索、历史推荐、金额试算、下单前可售性校验
 * @author come-often
 * @date 2026-11-08
 */
import request from '../utils/request';

export const getCategories = () => request('/dish/categories');

export const getDishList = (params = {}) => request('/dish/list', { data: params });

/** 跨字段搜索：菜名 / 副标题 / 描述 / 标签，由服务端排序分页 */
export const searchDishes = (params = {}) => request('/dish/search', { data: params });

/** 最近 3 单吃过的菜；未登录走 401，页面不展示该模块 */
export const getRecommendDishes = () => request('/dish/recommend');

export const getDishDetail = (dishId) => request(`/dish/${dishId}`);

export const quoteDish = (payload) => request('/dish/quote', { method: 'POST', data: payload });

export const checkOrder = (payload) => request('/dish/order-check', { method: 'POST', data: payload });
