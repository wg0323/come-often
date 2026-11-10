/**
 * 积分兑菜：列本店菜品，按「菜品金额 × 兑换比例」向上取整算所需积分，整单用积分结算
 * 所需积分只用于展示，实际扣减以服务端返回的订单为准
 * @author come-often
 * @date 2026-11-10
 */
import { getDishList } from '../../../api/dishApi';
import { exchangeDish, getPointsAccount, getPointsRule } from '../../../api/pointsApi';

Page({
  data: {
    account: null,
    rule: null,
    dishes: [],
    loading: false,
    exchanging: false,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
    await this.loadAll();
  },

  async loadAll() {
    this.setData({ loading: true });
    try {
      const [account, rule, dishes] = await Promise.all([
        getPointsAccount(),
        getPointsRule(),
        getDishList({}),
      ]);
      const rate = rule.exchangeRate || 100;
      const rows = (dishes ?? [])
        .filter((dish) => !dish.preorder && !dish.soldOut)
        .map((dish) => {
          const price = Number(dish.memberPrice > 0 ? dish.memberPrice : dish.price);
          return {
            ...dish,
            exchangePrice: price.toFixed(2),
            requiredPoints: Math.ceil(price * rate),
          };
        });
      this.setData({ account, rule, dishes: rows });
    } catch (error) {
      wx.showToast({ title: error.message ?? '积分兑菜加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onExchange(event) {
    const { dishId, name, points } = event.currentTarget.dataset;
    if (this.data.exchanging) {
      return;
    }
    wx.showModal({
      title: '积分兑换',
      content: `用 ${points} 积分兑换「${name}」1 份？兑换后直接生成待取餐订单，不可退款。`,
      confirmText: '兑换',
      success: (result) => {
        if (result.confirm) {
          this.doExchange(dishId);
        }
      },
    });
  },

  async doExchange(dishId) {
    this.setData({ exchanging: true });
    try {
      const order = await exchangeDish({ dishId, quantity: 1 });
      wx.showToast({ title: '兑换成功', icon: 'none' });
      wx.redirectTo({
        url: `/packageOrder/pages/order-detail/order-detail?orderNo=${order.orderNo}`,
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '兑换失败', icon: 'none' });
    } finally {
      this.setData({ exchanging: false });
    }
  },
});
