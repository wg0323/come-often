/**
 * 菜品搜索页（主包）：关键词命中菜名 / 副标题 / 描述 / 标签，结果分页加载
 * @author come-often
 * @date 2026-11-09
 */
import { searchDishes } from '../../api/dishApi';
import { buildImageUrl } from '../../utils/image';
import { quickAddDish } from '../../utils/quickAdd';

const PAGE_SIZE = 10;

const decorate = (list) => list.map((item) => ({ ...item, imageUrl: buildImageUrl(item.listImage) }));

Page({
  data: {
    keyword: '',
    dishes: [],
    current: 1,
    total: 0,
    searched: false,
    empty: false,
    loading: false,
  },

  async onLoad(options) {
    await getApp().globalData.loginReady;
    // 每次请求带序号，只认最后一次，避免慢请求回来把新关键词的结果覆盖掉
    this.searchSeq = 0;
    const keyword = options?.keyword ?? '';
    if (keyword) {
      this.setData({ keyword }, () => this.onSearch());
    }
  },

  onInput(event) {
    this.setData({ keyword: event.detail.value });
  },

  async onSearch() {
    const keyword = this.data.keyword.trim();
    if (!keyword) {
      this.searchSeq += 1;
      this.setData({ dishes: [], total: 0, searched: false, empty: false, loading: false });
      return;
    }
    this.searchSeq += 1;
    await this.loadPage(1, keyword, this.searchSeq);
  },

  async loadPage(page, keyword, seq) {
    this.setData({ loading: true });
    try {
      const result = await searchDishes({ keyword, current: page, size: PAGE_SIZE });
      if (seq !== this.searchSeq) {
        return;
      }
      const records = decorate(result?.records ?? []);
      const dishes = page === 1 ? records : [...this.data.dishes, ...records];
      this.setData({
        dishes,
        current: page,
        total: result?.total ?? 0,
        searched: true,
        empty: dishes.length === 0,
      });
    } catch (error) {
      if (seq === this.searchSeq) {
        wx.showToast({ title: error.message ?? '搜索失败', icon: 'none' });
      }
    } finally {
      if (seq === this.searchSeq) {
        this.setData({ loading: false });
      }
    }
  },

  onReachBottom() {
    const { loading, dishes, total, current, keyword } = this.data;
    if (loading || dishes.length >= total || !keyword.trim()) {
      return;
    }
    this.loadPage(current + 1, keyword.trim(), this.searchSeq);
  },

  onTapDish(event) {
    const { id } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/dish-detail/dish-detail?id=${id}` });
  },

  /** 列表快捷加购：默认规格直接入车，换规格或加料要进详情页 */
  async onQuickAdd(event) {
    const { id } = event.currentTarget.dataset;
    if (this.addingId) {
      return;
    }
    this.addingId = id;
    try {
      await quickAddDish(id);
      wx.showToast({ title: '已加入购物车', icon: 'success' });
    } catch (error) {
      wx.showToast({ title: error.message ?? '加入购物车失败', icon: 'none' });
    } finally {
      this.addingId = '';
    }
  },
});
