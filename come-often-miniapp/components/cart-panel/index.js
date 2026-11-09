/**
 * 购物车底部弹层：从下弹出的外卖式购物车
 * 删除走两条路径：数量减到 0 自动移除，或左滑露出删除区点击移除；不再有常驻「删除」文字
 * 金额一律取服务端 /cart 的试算结果，页面不自行计算金额
 * @author come-often
 * @date 2026-11-09
 */
import { clearCart, getCart, removeCartItem, updateCartQuantity } from '../../api/cartApi';
import { buildImageUrl } from '../../utils/image';

const REMOVE_WIDTH = 160;

const mapItems = (cart) => (cart.items ?? []).map((item) => ({
  ...item,
  imageUrl: buildImageUrl(item.objectKey),
  selected: !item.invalid,
  offsetX: 0,
}));

Component({
  properties: {
    visible: {
      type: Boolean,
      value: false,
    },
  },

  data: {
    items: [],
    totalAmount: '0.00',
    totalCount: 0,
    loading: false,
    draggingIndex: -1,
  },

  observers: {
    visible(value) {
      if (value) {
        this.loadCart();
      }
    },
  },

  methods: {
    noop() {},

    async loadCart() {
      this.setData({ loading: true });
      try {
        const cart = await getCart();
        const items = mapItems(cart);
        const totalCount = items.reduce((sum, item) => sum + (item.quantity ?? 0), 0);
        this.setData({
          items,
          totalCount,
          totalAmount: cart.totalAmount ?? '0.00',
          draggingIndex: -1,
        });
        this.triggerEvent('change', { count: totalCount });
      } catch (error) {
        wx.showToast({ title: error.message ?? '购物车加载失败', icon: 'none' });
      } finally {
        this.setData({ loading: false });
      }
    },

    onClose() {
      this.triggerEvent('close');
    },

    onToggleItem(event) {
      const { itemId } = event.currentTarget.dataset;
      const items = this.data.items.map((item) => (item.itemId === itemId
        ? { ...item, selected: !item.selected }
        : item));
      this.setData({ items });
    },

    /** 数量减到 0 直接移除，不再向后端提交 0 */
    async onChangeQuantity(event) {
      const { itemId, delta } = event.currentTarget.dataset;
      const target = this.data.items.find((item) => item.itemId === itemId);
      if (!target) {
        return;
      }
      const quantity = target.quantity + Number(delta);
      try {
        if (quantity <= 0) {
          await removeCartItem(itemId);
        } else {
          await updateCartQuantity(itemId, quantity);
        }
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
        wx.showToast({ title: error.message ?? '移除失败', icon: 'none' });
      }
    },

    /** px → rpx，触摸位移与 WXSS 的 rpx 宽度才对得上 */
    getPxToRpx() {
      if (!this.pxToRpx) {
        const { windowWidth } = wx.getSystemInfoSync();
        this.pxToRpx = windowWidth ? 750 / windowWidth : 2;
      }
      return this.pxToRpx;
    },

    collapseRows(activeIndex) {
      const items = this.data.items.map((item, index) => (index !== activeIndex && item.offsetX !== 0
        ? { ...item, offsetX: 0 }
        : item));
      this.setData({ items, draggingIndex: activeIndex });
    },

    onTouchStart(event) {
      const { itemId, index } = event.currentTarget.dataset;
      const touch = event.touches[0];
      const active = this.data.items.find((item) => item.itemId === itemId);
      this.touchStart = {
        x: touch.clientX,
        y: touch.clientY,
        itemId,
        offsetX: active?.offsetX ?? 0,
      };
      this.collapseRows(Number(index));
    },

    onTouchMove(event) {
      if (!this.touchStart) {
        return;
      }
      const touch = event.touches[0];
      const dx = touch.clientX - this.touchStart.x;
      const dy = touch.clientY - this.touchStart.y;
      // 竖向滚动交给 scroll-view，只有明显横向手势才展开删除区
      if (Math.abs(dy) > Math.abs(dx)) {
        return;
      }
      const index = this.data.items.findIndex((item) => item.itemId === this.touchStart.itemId);
      if (index < 0) {
        return;
      }
      const offsetX = Math.max(-REMOVE_WIDTH,
        Math.min(0, this.touchStart.offsetX + dx * this.getPxToRpx()));
      this.setData({ [`items[${index}].offsetX`]: offsetX });
    },

    onTouchEnd() {
      if (!this.touchStart) {
        return;
      }
      const index = this.data.items.findIndex((item) => item.itemId === this.touchStart.itemId);
      if (index >= 0) {
        const expanded = this.data.items[index].offsetX <= -REMOVE_WIDTH / 2;
        this.setData({ [`items[${index}].offsetX`]: expanded ? -REMOVE_WIDTH : 0 });
      }
      this.touchStart = null;
      this.setData({ draggingIndex: -1 });
    },

    async onClear() {
      const { confirm } = await wx.showModal({
        title: '清空购物车',
        content: '确定要移除全部菜品吗？',
        confirmText: '清空',
      });
      if (!confirm) {
        return;
      }
      try {
        await clearCart();
        await this.loadCart();
      } catch (error) {
        wx.showToast({ title: error.message ?? '清空失败', icon: 'none' });
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
      this.triggerEvent('close');
      wx.navigateTo({ url: '/pages/order-confirm/order-confirm' });
    },
  },
});
