/**
 * 顾客端评价接口：提交评价、菜品评价分页与评分聚合
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';

export const createReview = (payload) => request('/review', { method: 'POST', data: payload });

export const getDishReviews = (dishId, params = {}) => request(`/review/dish/${dishId}`, { data: params });

export const getDishRating = (dishId) => request(`/review/dish/${dishId}/rating`);
