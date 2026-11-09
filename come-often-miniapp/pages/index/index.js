/**
 * 顾客端首页：门店营业时间 + 搜索/轮播/公告 + 取号预约入口 + 招牌菜 + 团餐券 + 最近吃过
 * @author come-often
 * @date 2026-11-09
 */
import { getCouponCenter, receiveCoupon } from '../../api/couponApi';
import { getRecommendDishes } from '../../api/dishApi';
import { getHomeSummary } from '../../api/homeApi';
import { buildImageUrl } from '../../utils/image';
import { getToken, redirectToLogin } from '../../utils/request';

const WEEK_DAY_TEXT = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

const currentWeekDay = () => {
  const day = new Date().getDay();
  return day === 0 ? 7 : day;
};

const decorateCoupon = (coupon) => ({
  ...coupon,
  buttonText: coupon.received ? '已领取' : (coupon.canReceive ? '立即领取' : '已领完'),
});

Page({
  data: {
    storeName: '本店',
    businessStatus: '营业中',
    businessStatusOpen: true,
    weekDayText: '周一',
    todayHours: '营业时间待公布',
    banners: [],
    noticeText: '',
    signatureDishes: [],
    groupMealCoupons: [],
    recommendedDishes: [],
    loading: false,
  },

  async onLoad() {
    if (!getToken()) {
      redirectToLogin();
      return;
    }
    await getApp().globalData.loginReady;
    await Promise.all([this.loadSummary(), this.loadRecommend(), this.loadCenter()]);
  },

  /** 从订单页或领券页返回首页时刷新推荐与团餐券，首次进入由 onLoad 完成 */
  async onShow() {
    if (!this.shownOnce) {
      this.shownOnce = true;
      return;
    }
    await Promise.all([this.loadRecommend(), this.loadCenter()]);
  },

  async loadSummary() {
    this.setData({ loading: true });
    try {
      const summary = await getHomeSummary();
      const banners = (summary.banners ?? []).map((banner) => ({
        ...banner,
        imageUrl: buildImageUrl(banner.objectKey),
      }));
      const signatureDishes = (summary.signatureDishes ?? []).map((dish) => ({
        ...dish,
        imageUrl: buildImageUrl(dish.listImage),
      }));
      const notices = summary.notices ?? [];
      const hours = summary.store?.businessHours ?? [];
      const today = hours.find((item) => item.weekDay === currentWeekDay());
      this.setData({
        storeName: summary.store?.storeName ?? '本店',
        businessStatus: summary.store?.businessStatusText ?? '营业中',
        businessStatusOpen: (summary.store?.businessStatus ?? 1) === 1,
        weekDayText: today?.weekDayText ?? WEEK_DAY_TEXT[new Date().getDay()],
        todayHours: today?.openTime && today?.closeTime
          ? `${today.openTime} - ${today.closeTime}`
          : '今日休息',
        banners,
        signatureDishes,
        noticeText: notices.map((notice) => notice.content).join('　·　'),
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '首页加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /** 团餐券来自领券中心接口，按 groupMeal 标记单独成板块 */
  async loadCenter() {
    if (!getToken()) {
      return;
    }
    try {
      const list = await getCouponCenter();
      const groupMealCoupons = (list ?? []).filter((coupon) => coupon.groupMeal).map(decorateCoupon);
      this.setData({ groupMealCoupons });
    } catch (error) {
      this.setData({ groupMealCoupons: [] });
    }
  },

  /** 历史订单推荐只服务登录会员：没有 token 就不请求，接口本身也在 /api/app/** 下走 401 */
  async loadRecommend() {
    if (!getToken()) {
      return;
    }
    try {
      const list = await getRecommendDishes();
      const recommendedDishes = (list ?? []).map((dish) => ({
        ...dish,
        imageUrl: buildImageUrl(dish.listImage),
      }));
      this.setData({ recommendedDishes });
    } catch (error) {
      this.setData({ recommendedDishes: [] });
    }
  },

  onTapSearch() {
    wx.navigateTo({ url: '/pages/dish-search/dish-search' });
  },

  onTapQueue() {
    wx.navigateTo({ url: '/pages/queue/queue' });
  },

  onTapReserve() {
    wx.navigateTo({ url: '/packageOther/pages/table-reserve/table-reserve' });
  },

  onTapCouponCenter() {
    wx.switchTab({ url: '/pages/coupon-center/coupon-center' });
  },

  async onReceiveCoupon(event) {
    const { templateId, canReceive } = event.currentTarget.dataset;
    if (!canReceive) {
      wx.showToast({ title: '这张券已领取或已领完', icon: 'none' });
      return;
    }
    if (this.receiving) {
      return;
    }
    this.receiving = true;
    try {
      await receiveCoupon(templateId);
      wx.showToast({ title: '领取成功，可在卡包查看', icon: 'success' });
      await this.loadCenter();
    } catch (error) {
      wx.showToast({ title: error.message ?? '领取失败', icon: 'none' });
    } finally {
      this.receiving = false;
    }
  },

  onTapBanner(event) {
    const { jumpType, jumpValue } = event.currentTarget.dataset;
    if (jumpType === 'DISH' && jumpValue) {
      wx.navigateTo({ url: `/pages/dish-detail/dish-detail?id=${jumpValue}` });
      return;
    }
    this.goDishList(jumpType === 'CATEGORY' ? jumpValue : '');
  },

  onTapDish(event) {
    const { id } = event.currentTarget.dataset;
    wx.navigateTo({ url: `/pages/dish-detail/dish-detail?id=${id}` });
  },

  /** tabBar 页不能带参数，分类 ID 经 globalData 交给点餐页 onShow 消费 */
  goDishList(categoryId) {
    getApp().globalData.pendingCategoryId = categoryId || '';
    wx.switchTab({ url: '/pages/dish-list/dish-list' });
  },
});
