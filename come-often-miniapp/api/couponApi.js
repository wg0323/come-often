/**
 * 顾客端卡包接口：领券、卡包分页与动态券码
 * @author come-often
 * @date 2026-11-08
 */
import request from '../utils/request';

export const getCouponCenter = () => request('/coupon/center');

export const receiveCoupon = (templateId) => request(`/coupon/${templateId}/receive`, {
  method: 'POST',
});

export const getCoupons = (params = {}) => request('/coupon/page', { data: params });

export const getCouponCode = (userCouponId) => request(`/coupon/${userCouponId}/code`);
