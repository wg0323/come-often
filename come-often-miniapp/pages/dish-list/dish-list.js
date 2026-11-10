/**
 * 点餐页：先选堂食/自提（堂食绑桌码、自提预约时间），再点菜；购物车为底部弹出面板
 * @author come-often
 * @date 2026-11-09
 */
import { getCart } from '../../api/cartApi';
import { getCategories, getDishList } from '../../api/dishApi';
import { bindStoreTable } from '../../api/storeApi';
import { buildImageUrl } from '../../utils/image';
import { quickAddDish } from '../../utils/quickAdd';

const TABLE_CODE_PATTERN = /^[A-Za-z0-9_-]{2,20}$/;

const pad = (value) => `${value}`.padStart(2, '0');

const formatDate = (date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

const formatClock = (date) => `${pad(date.getHours())}:${pad(date.getMinutes())}`;

/** 桌码支持纯文本 T01，也支持二维码里的 code= / scene= / t= 参数 */
const parseTableCode = (raw) => {
  const text = `${raw ?? ''}`.trim();
  const matched = text.match(/(?:code|tableCode|scene|t)=([A-Za-z0-9_-]{2,20})/);
  const code = matched ? decodeURIComponent(matched[1]) : text;
  return TABLE_CODE_PATTERN.test(code) ? code : '';
};

Page({
  data: {
    categories: [],
    activeCategoryId: '',
    dishes: [],
    loading: false,
    empty: false,
    modeVisible: false,
    modeDraft: 'DINE_IN',
    orderMode: '',
    orderModeText: '',
    tableCode: '',
    tableCodeInput: '',
    pickupDate: '',
    pickupClock: '',
    cartVisible: false,
    cartCount: 0,
    cartTotal: '0.00',
  },

  async onLoad(options) {
    const app = getApp();
    // 首页分类入口走 switchTab（不能带 query），分类 ID 由首页写入 globalData
    this.initialCategoryId = options?.categoryId || app.globalData.pendingCategoryId || '';
    app.globalData.pendingCategoryId = '';
    await app.globalData.loginReady;
    const now = new Date();
    const defaultPickup = new Date(now.getTime() + 60 * 60 * 1000);
    const boundCode = app.globalData.boundTableCode || wx.getStorageSync('boundTableCode') || '';
    // 桌台 ID 与桌码成对存在：冷启动后要把本地保存的 ID 一起恢复到 globalData，下单才带得上
    app.globalData.boundTableId = app.globalData.boundTableId
      || wx.getStorageSync('boundTableId') || '';
    this.setData({
      pickupDate: formatDate(now),
      pickupClock: formatClock(defaultPickup),
      tableCode: boundCode,
      tableCodeInput: boundCode,
    });
    this.applyOrderMode();
    await this.loadCategories();
    await this.loadDishes();
    await this.refreshCart();
  },

  /** tab 页已加载时不会再走 onLoad，返回本页时消费待切换的分类与待展开的购物车 */
  onShow() {
    const app = getApp();
    if (app.globalData.pendingOpenCart) {
      app.globalData.pendingOpenCart = false;
      this.setData({ cartVisible: true });
    }
    const pending = app.globalData.pendingCategoryId;
    if (!pending || this.data.categories.length === 0) {
      return;
    }
    app.globalData.pendingCategoryId = '';
    if (pending === this.data.activeCategoryId) {
      return;
    }
    this.setData({ activeCategoryId: pending }, () => this.loadDishes());
  },

  applyOrderMode() {
    const app = getApp();
    if (app.globalData.orderMode === 'DINE_IN') {
      const code = app.globalData.boundTableCode || wx.getStorageSync('boundTableCode') || '';
      if (!code) {
        this.setData({ modeVisible: true, modeDraft: 'DINE_IN' });
        return;
      }
      this.setData({ orderMode: 'DINE_IN', tableCode: code, orderModeText: `堂食 · ${code}` });
      return;
    }
    if (app.globalData.orderMode === 'PICKUP' && app.globalData.pickupTime) {
      this.setData({
        orderMode: 'PICKUP',
        orderModeText: `自提 · ${app.globalData.pickupTime.slice(5, 16)}`,
      });
      return;
    }
    this.setData({ modeVisible: true });
  },

  async loadCategories() {
    try {
      const categories = await getCategories();
      const matched = categories.find((item) => item.id === this.initialCategoryId);
      this.setData({
        categories,
        activeCategoryId: matched ? matched.id : (categories[0]?.id ?? ''),
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '分类加载失败', icon: 'none' });
    }
  },

  async loadDishes() {
    const { activeCategoryId } = this.data;
    if (!activeCategoryId) {
      this.setData({ dishes: [], empty: true });
      return;
    }
    this.setData({ loading: true });
    try {
      const list = await getDishList({ categoryId: activeCategoryId });
      const dishes = list.map((item) => ({ ...item, imageUrl: buildImageUrl(item.listImage) }));
      this.setData({ dishes, empty: dishes.length === 0 });
    } catch (error) {
      wx.showToast({ title: error.message ?? '菜品加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async refreshCart() {
    try {
      const cart = await getCart();
      const items = cart.items ?? [];
      this.setData({
        cartCount: items.reduce((sum, item) => sum + (item.quantity ?? 0), 0),
        cartTotal: cart.totalAmount ?? '0.00',
      });
    } catch (error) {
      console.warn('购物车角标刷新失败', error);
    }
  },

  noop() {},

  onTapSwitchMode() {
    this.setData({
      modeVisible: true,
      modeDraft: this.data.orderMode || 'DINE_IN',
      tableCodeInput: this.data.tableCode,
    });
  },

  onSelectMode(event) {
    this.setData({ modeDraft: event.currentTarget.dataset.mode });
  },

  onCancelMode() {
    if (!this.data.orderMode) {
      wx.showToast({ title: '请先选择用餐方式', icon: 'none' });
      return;
    }
    this.setData({ modeVisible: false });
  },

  async onScanTable() {
    try {
      const result = await wx.scanCode({ onlyFromCamera: false });
      const code = parseTableCode(result?.result);
      if (!code) {
        wx.showToast({ title: '无法识别桌码，请扫店内桌码', icon: 'none' });
        return;
      }
      this.bindTable(code);
    } catch (error) {
      wx.showToast({ title: error?.message ?? '扫码已取消', icon: 'none' });
    }
  },

  onTableCodeInput(event) {
    this.setData({ tableCodeInput: event.detail.value });
  },

  /** 桌码先过服务端校验：校验通过才落本地，并把桌台 ID 存下来供下单携带 */
  async bindTable(code) {
    try {
      const table = await bindStoreTable(code);
      const app = getApp();
      wx.setStorageSync('boundTableCode', code);
      wx.setStorageSync('boundTableId', table?.id ?? '');
      app.globalData.boundTableCode = code;
      app.globalData.boundTableId = table?.id ?? '';
      this.setData({ tableCode: code, tableCodeInput: code });
      wx.showToast({ title: `已绑定 ${table?.tableNo ?? code}`, icon: 'success' });
      return true;
    } catch (error) {
      wx.showToast({ title: error.message ?? '桌码校验失败', icon: 'none' });
      return false;
    }
  },

  onPickupDateChange(event) {
    this.setData({ pickupDate: event.detail.value });
  },

  onPickupClockChange(event) {
    this.setData({ pickupClock: event.detail.value });
  },

  async onConfirmMode() {
    const app = getApp();
    if (this.data.modeDraft === 'DINE_IN') {
      const code = this.data.tableCode || `${this.data.tableCodeInput ?? ''}`.trim();
      if (!code || !TABLE_CODE_PATTERN.test(code)) {
        wx.showToast({ title: '请先扫码或输入有效桌码', icon: 'none' });
        return;
      }
      const bound = await this.bindTable(code);
      if (!bound) {
        return;
      }
      app.globalData.orderMode = 'DINE_IN';
      this.setData({
        modeVisible: false,
        orderMode: 'DINE_IN',
        tableCode: code,
        tableCodeInput: code,
        orderModeText: `堂食 · ${code}`,
      });
      return;
    }
    const pickupTime = `${this.data.pickupDate} ${this.data.pickupClock}:00`;
    const pickupMillis = new Date(`${this.data.pickupDate}T${this.data.pickupClock}:00`).getTime();
    if (!Number.isFinite(pickupMillis) || pickupMillis <= Date.now()) {
      wx.showToast({ title: '预约时间需晚于当前时刻', icon: 'none' });
      return;
    }
    app.globalData.orderMode = 'PICKUP';
    app.globalData.pickupTime = pickupTime;
    this.setData({
      modeVisible: false,
      orderMode: 'PICKUP',
      orderModeText: `自提 · ${pickupTime.slice(5, 16)}`,
    });
  },

  onOpenCart() {
    this.setData({ cartVisible: true });
  },

  onCloseCart() {
    this.setData({ cartVisible: false });
    this.refreshCart();
  },

  onCartChange(event) {
    this.setData({ cartCount: event.detail.count ?? 0 });
  },

  onTapCategory(event) {
    const { id } = event.currentTarget.dataset;
    if (id === this.data.activeCategoryId) {
      return;
    }
    this.setData({ activeCategoryId: id }, () => this.loadDishes());
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
      await this.refreshCart();
      wx.showToast({ title: '已加入购物车', icon: 'success' });
    } catch (error) {
      wx.showToast({ title: error.message ?? '加入购物车失败', icon: 'none' });
    } finally {
      this.addingId = '';
    }
  },
});
