/**
 * 确认订单页：金额全部取服务端 /order/preview；堂食/自提、桌码与自提时间来自点餐页
 * @author come-often
 * @date 2026-11-09
 */
import { createOrder, previewOrder } from '../../api/orderApi';

const pad = (value) => `${value}`.padStart(2, '0');

const formatDate = (date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

const DAY_MS = 24 * 60 * 60 * 1000;

const DINE_IN_LEAD_MINUTES = 10;

const NO_COUPON = { userCouponId: '', label: '不使用优惠券' };

Page({
  data: {
    itemIds: [],
    preview: null,
    couponOptions: [NO_COUPON],
    couponIndex: 0,
    selectedCouponId: '',
    couponLabel: '不使用优惠券',
    usePointsEnabled: false,
    useBalanceEnabled: false,
    dineInDate: '',
    dineInTime: '12:00',
    orderModeText: '自提 · 立即取餐',
    remark: '',
    loading: false,
    submitting: false,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
    const itemIds = wx.getStorageSync('checkoutItemIds') ?? [];
    if (!Array.isArray(itemIds) || itemIds.length === 0) {
      wx.showToast({ title: '请先选择购物车商品', icon: 'none' });
      wx.navigateBack();
      return;
    }
    const app = getApp();
    if (app.globalData.orderMode === 'DINE_IN' && !app.globalData.boundTableCode) {
      wx.showToast({ title: '请先扫码绑定桌码', icon: 'none' });
      wx.navigateBack();
      return;
    }
    if (app.globalData.orderMode === 'PICKUP' && !app.globalData.pickupTime) {
      wx.showToast({ title: '请先选择自提预约时间', icon: 'none' });
      wx.navigateBack();
      return;
    }
    this.setData({
      itemIds,
      dineInDate: formatDate(new Date(Date.now() + 2 * DAY_MS)),
      orderModeText: this.describeOrderMode(),
    });
    await this.loadPreview();
  },

  describeOrderMode() {
    const app = getApp();
    if (app.globalData.orderMode === 'DINE_IN') {
      return `堂食 · 桌码 ${app.globalData.boundTableCode}`;
    }
    if (app.globalData.orderMode === 'PICKUP') {
      return `自提 · ${app.globalData.pickupTime}`;
    }
    return '自提 · 立即取餐';
  },

  /** 普通堂食由服务端要求到店时间，按「现在到店」给 10 分钟余量；预定菜仍用页面选的时间 */
  immediateDineInTime() {
    const date = new Date(Date.now() + DINE_IN_LEAD_MINUTES * 60 * 1000);
    return `${formatDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
  },

  /** 桌码写进订单备注，商家端订单详情可直接看到，不改动既有订单表结构 */
  decorateRemark(remark) {
    const app = getApp();
    const code = app.globalData.boundTableCode || wx.getStorageSync('boundTableCode') || '';
    const tableNote = app.globalData.orderMode === 'DINE_IN' && code ? `[桌码:${code}]` : '';
    return `${tableNote}${remark ?? ''}`.trim();
  },

  buildPayload() {
    const {
      itemIds, preview, dineInDate, dineInTime, selectedCouponId, remark,
    } = this.data;
    const app = getApp();
    const payload = { itemIds, remark: this.decorateRemark(remark), orderType: 'PICKUP' };
    // 桌台 ID 只随堂食单上送：含预定菜会强制 DINE_IN，因此按「是否将以堂食提交」判断
    const dineInIntent = (preview && preview.containsPreorder) || app.globalData.orderMode === 'DINE_IN';
    const tableId = app.globalData.boundTableId || wx.getStorageSync('boundTableId') || '';
    if (dineInIntent && tableId) {
      payload.tableId = tableId;
    }
    if (selectedCouponId) {
      payload.userCouponId = selectedCouponId;
    }
    // 用多少积分抵现由服务端给出上限，页面只回传「全部用掉」这一个动作
    if (this.data.usePointsEnabled && preview && preview.maxUsablePoints > 0) {
      payload.usePoints = preview.maxUsablePoints;
    }
    // 余额抵扣同样只回传开关：抵扣金额由服务端按可用余额与「至少留 0.01 元在线支付」算
    if (this.data.useBalanceEnabled && preview && preview.balanceAvailable > 0) {
      payload.useBalance = true;
    }
    if (preview && preview.containsPreorder) {
      payload.orderType = 'DINE_IN';
      payload.dineInTime = `${dineInDate} ${dineInTime}:00`;
      return payload;
    }
    if (app.globalData.orderMode === 'DINE_IN') {
      payload.orderType = 'DINE_IN';
      payload.dineInTime = this.immediateDineInTime();
      return payload;
    }
    if (app.globalData.pickupTime) {
      payload.pickupTime = app.globalData.pickupTime;
    }
    return payload;
  },

  async loadPreview() {
    this.setData({ loading: true });
    try {
      const preview = await previewOrder(this.buildPayload());
      this.setData({
        preview,
        couponOptions: this.buildCouponOptions(preview),
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '确认页加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /** 可用的券由服务端算好抵扣额，页面只拼展示文案 */
  buildCouponOptions(preview) {
    const usable = (preview.availableCoupons ?? []).filter((coupon) => coupon.usable);
    const options = usable.map((coupon) => ({
      userCouponId: coupon.userCouponId,
      label: `${coupon.templateName}（抵 ¥${coupon.discountAmount}）`,
    }));
    return [NO_COUPON, ...options];
  },

  async onCouponChange(event) {
    const index = Number(event.detail.value);
    const option = this.data.couponOptions[index] ?? NO_COUPON;
    this.setData({
      couponIndex: index,
      selectedCouponId: option.userCouponId,
      couponLabel: option.label,
    });
    await this.loadPreview();
  },

  async onPointsToggle(event) {
    this.setData({ usePointsEnabled: event.detail.value }, () => this.loadPreview());
  },

  async onBalanceToggle(event) {
    this.setData({ useBalanceEnabled: event.detail.value }, () => this.loadPreview());
  },

  onDineInDateChange(event) {
    this.setData({ dineInDate: event.detail.value }, () => this.loadPreview());
  },

  onDineInTimeChange(event) {
    this.setData({ dineInTime: event.detail.value }, () => this.loadPreview());
  },

  onRemarkInput(event) {
    this.setData({ remark: event.detail.value });
  },

  /** 桌码绑定只对当次就餐有效：下单成功后清掉本地与 globalData，避免下次进店沿用旧桌 */
  clearTableBinding() {
    const app = getApp();
    wx.removeStorageSync('boundTableCode');
    wx.removeStorageSync('boundTableId');
    app.globalData.boundTableCode = '';
    app.globalData.boundTableId = '';
  },

  async onSubmit() {
    if (!this.data.preview || this.data.submitting) {
      return;
    }
    this.setData({ submitting: true });
    try {
      const order = await createOrder(this.buildPayload());
      wx.removeStorageSync('checkoutItemIds');
      this.clearTableBinding();
      wx.redirectTo({
        url: `/packageOrder/pages/order-detail/order-detail?orderNo=${order.orderNo}`,
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '下单失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },
});
