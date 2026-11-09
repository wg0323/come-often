/**
 * 评价页（订单分包）：评分 + 内容 + 可选单道菜品 + 最多 3 张带图，提交后发放积分
 * @author come-often
 * @date 2026-11-09
 */
import { getOrderDetail } from '../../../api/orderApi';
import { createReview } from '../../../api/reviewApi';
import { uploadReviewImage } from '../../../utils/upload';

const MAX_IMAGES = 3;
const MAX_CONTENT = 500;
const DEFAULT_RATING = 5;

const rateOptions = (rating) => [1, 2, 3, 4, 5].map((value) => ({
  value,
  active: value <= rating,
}));

Page({
  data: {
    orderNo: '',
    order: null,
    rating: DEFAULT_RATING,
    rateOptions: rateOptions(DEFAULT_RATING),
    content: '',
    maxContent: MAX_CONTENT,
    dishes: [],
    selectedDishId: '',
    images: [],
    maxImages: MAX_IMAGES,
    canReview: false,
    loading: false,
    submitting: false,
  },

  async onLoad(options) {
    await getApp().globalData.loginReady;
    const orderNo = options?.orderNo ?? '';
    if (!orderNo) {
      wx.showToast({ title: '缺少订单号', icon: 'none' });
      return;
    }
    this.setData({ orderNo });
    await this.loadOrder();
  },

  async loadOrder() {
    this.setData({ loading: true });
    try {
      const order = await getOrderDetail(this.data.orderNo);
      // 菜单里同一道菜可能因规格不同出现多行，评价只允许选一道，按 dishId 去重
      const seen = new Set();
      const dishes = (order.items ?? [])
        .filter((item) => {
          if (!item.dishId || seen.has(item.dishId)) {
            return false;
          }
          seen.add(item.dishId);
          return true;
        })
        .map((item) => ({ dishId: item.dishId, dishName: item.dishName }));
      this.setData({ order, dishes, canReview: Boolean(order.canReview) });
    } catch (error) {
      wx.showToast({ title: error.message ?? '订单加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onRate(event) {
    const value = Number(event.currentTarget.dataset.value);
    this.setData({ rating: value, rateOptions: rateOptions(value) });
  },

  onContentInput(event) {
    this.setData({ content: event.detail.value });
  },

  onSelectDish(event) {
    const { dishId } = event.currentTarget.dataset;
    this.setData({ selectedDishId: dishId === this.data.selectedDishId ? '' : dishId });
  },

  async onChooseImage() {
    const remain = this.data.maxImages - this.data.images.length;
    if (remain <= 0) {
      wx.showToast({ title: `最多 ${MAX_IMAGES} 张`, icon: 'none' });
      return;
    }
    try {
      const media = await wx.chooseMedia({ count: remain, mediaType: ['image'], sizeType: ['compressed'] });
      const files = media?.tempFiles ?? [];
      for (const file of files) {
        const objectKey = await uploadReviewImage(file.tempFilePath);
        this.setData({
          images: [...this.data.images, { path: file.tempFilePath, objectKey }],
        });
      }
    } catch (error) {
      wx.showToast({ title: error.message ?? '图片上传失败', icon: 'none' });
    }
  },

  onRemoveImage(event) {
    const index = Number(event.currentTarget.dataset.index);
    const images = this.data.images.filter((item, position) => position !== index);
    this.setData({ images });
  },

  onPreviewImage(event) {
    const { url } = event.currentTarget.dataset;
    wx.previewImage({ current: url, urls: this.data.images.map((item) => item.path) });
  },

  async onSubmit() {
    if (this.data.submitting) {
      return;
    }
    if (!this.data.canReview) {
      wx.showToast({ title: '该订单当前不可评价', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    try {
      await createReview({
        orderNo: this.data.orderNo,
        rating: this.data.rating,
        content: this.data.content.trim(),
        dishId: this.data.selectedDishId || undefined,
        imageObjectKeys: this.data.images.map((item) => item.objectKey),
      });
      wx.showToast({ title: '评价成功', icon: 'none' });
      const orderNo = this.data.orderNo;
      setTimeout(() => {
        wx.redirectTo({ url: `/packageOrder/pages/order-detail/order-detail?orderNo=${orderNo}` });
      }, 800);
    } catch (error) {
      wx.showToast({ title: error.message ?? '评价提交失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
