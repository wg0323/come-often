/**
 * 购物车页：勾选、改数量、删除；金额一律取服务端返回的试算结果
 * @author come-often
 * @date 2026-11-08
 */
import { getCart, removeCartItem, updateCartQuantity } from '../../api/cartApi';
import { buildImageUrl } from '../../utils/image';

const mapItems = (cart) => (cart.items ?? []).map((item) => ({
  ...item,
  imageUrl: buildImageUrl(item.objectKey),
  selected: !item.invalid,
}));

Page({
  data: {
    cart: null,
    items: [],
    loading: false,
  },

  async onShow() {
    await getApp().globalData.loginReady;
    await this.loadCart();
  },

  async loadCart() {
    this.setData({ loading: true });
    try {
      const cart = await getCart();
      this.setData({ cart, items: mapItems(cart) });
    } catch (error) {
      wx.showToast({ title: error.message ?? '购物车加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onToggleItem(event) {
    const { itemId } = event.currentTarget.dataset;
    const items = this.data.items.map((item) => (item.itemId === itemId
      ? { ...item, selected: !item.selected }
      : item));
    this.setData({ items });
  },

  async onChangeQuantity(event) {
    const { itemId, delta } = event.currentTarget.dataset;
    const target = this.data.items.find((item) => item.itemId === itemId);
    if (!target) {
      return;
    }
    const quantity = Math.max(1, target.quantity + Number(delta));
    try {
      await updateCartQuantity(itemId, quantity);
      await this.loadCart();
    } catch (error) {
      wx.showToast({ title: error.message ?? '数量修改失败', icon: 'none' });
    }
  },

  async onRemove(event) {
    const { itemId } = event.currentTarget.dataset;
    try {
      await removeCartItem(itemId);
      await this.loadCart();
    } catch (error) {
      wx.showToast({ title: error.message ?? '删除失败', icon: 'none' });
    }
  },

  onCheckout() {
    const ids = this.data.items
      .filter((item) => item.selected && !item.invalid)
      .map((item) => `${item.itemId}`);
    if (ids.length === 0) {
      wx.showToast({ title: '请先选择可结算的商品', icon: 'none' });
      return;
    }
    wx.setStorageSync('checkoutItemIds', ids);
    wx.navigateTo({ url: '/pages/order-confirm/order-confirm' });
  },

  onGoDish() {
    wx.switchTab({ url: '/pages/dish-list/dish-list' });
  },
});
