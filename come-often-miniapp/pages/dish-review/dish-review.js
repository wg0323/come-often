/**
 * 菜品评价列表页（主包）：评分聚合 + 带图评价分页
 * @author come-often
 * @date 2026-11-09
 */
import { getDishReviews, getDishRating } from '../../api/reviewApi';
import { buildImageUrl } from '../../utils/image';

const PAGE_SIZE = 10;
const MAX_STARS = 5;

const decorate = (review) => ({
  ...review,
  stars: '★★★★★'.slice(0, review.rating ?? 0),
  emptyStars: '☆☆☆☆☆'.slice(0, MAX_STARS - (review.rating ?? 0)),
  images: (review.imageObjectKeys ?? []).map((objectKey) => buildImageUrl(objectKey)),
});

Page({
  data: {
    dishId: '',
    dishName: '',
    averageRating: '0.0',
    reviewCount: 0,
    reviews: [],
    current: 1,
    total: 0,
    loading: false,
  },

  async onLoad(options) {
    await getApp().globalData.loginReady;
    const dishId = options?.id ?? '';
    if (!dishId) {
      wx.showToast({ title: '缺少菜品参数', icon: 'none' });
      return;
    }
    const dishName = options?.name ? decodeURIComponent(options.name) : '';
    this.setData({ dishId, dishName });
    if (dishName) {
      wx.setNavigationBarTitle({ title: `${dishName} 的评价` });
    }
    await this.loadSummary();
    await this.loadPage(1);
  },

  async loadSummary() {
    try {
      const rating = await getDishRating(this.data.dishId);
      this.setData({
        averageRating: rating?.averageRating ?? '0.0',
        reviewCount: rating?.reviewCount ?? 0,
      });
    } catch (error) {
      this.setData({ averageRating: '0.0', reviewCount: 0 });
    }
  },

  async loadPage(page) {
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    try {
      const result = await getDishReviews(this.data.dishId, { current: page, size: PAGE_SIZE });
      const records = (result?.records ?? []).map(decorate);
      const reviews = page === 1 ? records : [...this.data.reviews, ...records];
      this.setData({ reviews, current: page, total: result?.total ?? 0 });
    } catch (error) {
      wx.showToast({ title: error.message ?? '评价加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onReachBottom() {
    const { loading, reviews, total, current } = this.data;
    if (loading || reviews.length >= total) {
      return;
    }
    this.loadPage(current + 1);
  },

  onPreviewImage(event) {
    const { url, reviewIndex } = event.currentTarget.dataset;
    const urls = this.data.reviews[reviewIndex].images;
    wx.previewImage({ current: url, urls });
  },
});
