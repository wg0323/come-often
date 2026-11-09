/**
 * 顾客端购物车接口：列表、加购、改数量、删除与清空
 * @author come-often
 * @date 2026-11-08
 */
import request from '../utils/request';

export const getCart = () => request('/cart');

export const addToCart = (payload) => request('/cart', { method: 'POST', data: payload });

export const updateCartQuantity = (itemId, quantity) => request(`/cart/${itemId}/quantity`, {
  method: 'PUT',
  data: { quantity },
});

export const removeCartItem = (itemId) => request(`/cart/${itemId}`, { method: 'DELETE' });

export const clearCart = () => request('/cart', { method: 'DELETE' });
