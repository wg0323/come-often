/**
 * 菜品详情页：多图、规格、加料、过敏原与预定菜规则；金额一律取 /dish/quote 的返回
 * @author come-often
 * @date 2026-11-08
 */
import { addToCart } from '../../api/cartApi';
import { checkOrder, getDishDetail, quoteDish } from '../../api/dishApi';
import { getDishReviews } from '../../api/reviewApi';
import { buildImageUrl } from '../../utils/image';

const REVIEW_PREVIEW_SIZE = 3;

const MAX_STARS = 5;

const decorateReview = (review) => ({
  ...review,
  stars: '★★★★★'.slice(0, review.rating ?? 0),
  emptyStars: '☆☆☆☆☆'.slice(0, MAX_STARS - (review.rating ?? 0)),
  images: (review.imageObjectKeys ?? []).map((objectKey) => buildImageUrl(objectKey)),
});

const collectSelected = (groups) => {
  const ids = [];
  groups.forEach((group) => {
    group.options.forEach((option) => {
      if (option.selected) {
        ids.push(option.id);
      }
    });
  });
  return ids;
};

Page({
  data: {
    dishId: '',
    detail: null,
    images: [],
    currentImage: 0,
    specGroups: [],
    addons: [],
    selectedOptionIds: [],
    selectedAddonIds: [],
    quantity: 1,
    quote: null,
    reviews: [],
    cartVisible: false,
    loading: false,
    submitting: false,
  },

  async onLoad(options) {
    await getApp().globalData.loginReady;
    const dishId = options?.id ?? '';
    if (!dishId) {
      wx.showToast({ title: '缺少菜品参数', icon: 'none' });
      return;
    }
    this.setData({ dishId });
    await this.loadDetail();
  },

  async loadDetail() {
    this.setData({ loading: true });
    try {
      const detail = await getDishDetail(this.data.dishId);
      const images = (detail.images ?? []).map((image) => ({
        ...image,
        url: buildImageUrl(image.objectKey),
      }));
      const specGroups = (detail.specGroups ?? []).map((group) => ({
        ...group,
        options: (group.options ?? []).map((option, index) => ({ ...option, selected: index === 0 })),
      }));
      const addons = (detail.addons ?? []).map((addon) => ({ ...addon, selected: false }));
      this.setData({
        detail,
        images,
        specGroups,
        addons,
        selectedOptionIds: collectSelected(specGroups),
        selectedAddonIds: [],
        quantity: 1,
      });
      wx.setNavigationBarTitle({ title: detail.dishName });
      await this.refreshQuote();
      await this.loadReviews();
    } catch (error) {
      wx.showToast({ title: error.message ?? '菜品详情加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async refreshQuote() {
    try {
      const quote = await quoteDish({
        dishId: this.data.dishId,
        quantity: this.data.quantity,
        specOptionIds: this.data.selectedOptionIds,
        addonIds: this.data.selectedAddonIds,
      });
      this.setData({ quote });
    } catch (error) {
      wx.showToast({ title: error.message ?? '金额试算失败', icon: 'none' });
    }
  },

  onTapSpecOption(event) {
    const { groupId, optionId } = event.currentTarget.dataset;
    const specGroups = this.data.specGroups.map((group) => (group.id === groupId
      ? {
        ...group,
        options: group.options.map((option) => ({ ...option, selected: option.id === optionId })),
      }
      : group));
    this.setData({ specGroups, selectedOptionIds: collectSelected(specGroups) }, () => this.refreshQuote());
  },

  onTapAddon(event) {
    const { addonId } = event.currentTarget.dataset;
    const addons = this.data.addons.map((addon) => (addon.id === addonId
      ? { ...addon, selected: !addon.selected }
      : addon));
    const selectedAddonIds = addons.filter((addon) => addon.selected).map((addon) => addon.id);
    this.setData({ addons, selectedAddonIds }, () => this.refreshQuote());
  },

  onChangeQuantity(event) {
    const { delta } = event.currentTarget.dataset;
    const quantity = Math.max(1, this.data.quantity + Number(delta));
    this.setData({ quantity }, () => this.refreshQuote());
  },

  onOpenCart() {
    this.setData({ cartVisible: true });
  },

  onCloseCart() {
    this.setData({ cartVisible: false });
  },

  async onTapPrimary() {
    const { detail, dishId, quantity, selectedOptionIds, selectedAddonIds } = this.data;
    if (!detail || detail.soldOut) {
      return;
    }
    this.setData({ submitting: true });
    try {
      // 复用 M2 的下单前校验；金额始终取服务端 quote，页面不自行计算
      await checkOrder({
        items: [{ dishId, quantity, specOptionIds: selectedOptionIds, addonIds: selectedAddonIds }],
      });
      const itemId = await addToCart({
        dishId,
        quantity,
        specOptionIds: selectedOptionIds,
        addonIds: selectedAddonIds,
      });
      if (detail.preorder) {
        wx.setStorageSync('checkoutItemIds', [`${itemId}`]);
        wx.navigateTo({ url: '/pages/order-confirm/order-confirm' });
        return;
      }
      wx.showToast({ title: '已加入购物车', icon: 'success' });
    } catch (error) {
      wx.showToast({ title: error.message ?? '暂不可下单', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  /** 详情页只展示最近 3 条，全量进评价列表页 */
  async loadReviews() {
    try {
      const result = await getDishReviews(this.data.dishId,
        { current: 1, size: REVIEW_PREVIEW_SIZE });
      this.setData({ reviews: (result?.records ?? []).map(decorateReview) });
    } catch (error) {
      this.setData({ reviews: [] });
    }
  },

  onTapAllReviews() {
    const { dishId, detail } = this.data;
    const name = encodeURIComponent(detail?.dishName ?? '');
    wx.navigateTo({ url: `/pages/dish-review/dish-review?id=${dishId}&name=${name}` });
  },

  onPreviewReviewImage(event) {
    const { url, reviewIndex } = event.currentTarget.dataset;
    const urls = this.data.reviews[reviewIndex].images;
    wx.previewImage({ current: url, urls });
  },

  onGalleryChange(event) {
    this.setData({ currentImage: event.detail.current });
  },

  onPreviewImage(event) {
    const { url } = event.currentTarget.dataset;
    wx.previewImage({ current: url, urls: this.data.images.map((image) => image.url) });
  },
});
