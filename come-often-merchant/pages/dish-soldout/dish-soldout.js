/**
 * 商家端菜品售罄：分类折叠列表，一键售罄 / 开售
 * @author come-often
 * @date 2026-11-09
 */
import { listSoldOut, toggleSoldOut } from '../../api/dishApi';
import { formatMoney } from '../../utils/display';

Page({
  data: {
    categories: [],
    loading: false,
    submitting: false,
    refresherTriggered: false,
  },
  expandedIds: {},
  onShow() {
    this.loadCategories();
  },
  /** 下拉刷新由根 scroll-view 的 refresher 接管，页面已 disableScroll */
  async onRefresherRefresh() {
    this.setData({ refresherTriggered: true });
    await this.loadCategories();
    this.setData({ refresherTriggered: false });
  },
  async loadCategories() {
    this.setData({ loading: true });
    try {
      const categories = await listSoldOut();
      this.setData({
        categories: (categories ?? []).map((category) => {
          const key = `${category.categoryId}`;
          let expanded = this.expandedIds[key];
          if (expanded === undefined) {
            expanded = true;
          }
          this.expandedIds[key] = expanded;
          return {
            ...category,
            key,
            expanded,
            dishes: (category.dishes ?? []).map((dish) => ({
              ...dish,
              key: `${dish.dishId}`,
              priceText: formatMoney(dish.price),
              remainText: dish.configured ? `剩 ${dish.remain} / ${dish.total}` : '不限量',
              actionLabel: dish.soldOut ? '开售' : '售罄',
              actionClass: dish.soldOut ? 'btn--ghost' : 'btn--danger',
            })),
          };
        }),
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
  onTapCategory(event) {
    const { categoryid } = event.currentTarget.dataset;
    const key = `${categoryid}`;
    this.expandedIds[key] = !this.expandedIds[key];
    this.setData({
      categories: this.data.categories.map((category) => (
        category.key === key ? { ...category, expanded: this.expandedIds[key] } : category
      )),
    });
  },
  async onTapToggle(event) {
    const { dishid, soldout } = event.currentTarget.dataset;
    if (this.data.submitting) {
      return;
    }
    const nextSoldOut = `${soldout}` !== 'true';
    this.setData({ submitting: true });
    wx.showLoading({ title: '处理中', mask: true });
    try {
      await toggleSoldOut(dishid, nextSoldOut);
      wx.hideLoading();
      wx.showToast({ title: nextSoldOut ? '已售罄' : '已开售', icon: 'success' });
      await this.loadCategories();
    } catch (error) {
      wx.hideLoading();
      wx.showToast({ title: error.message ?? '操作失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
