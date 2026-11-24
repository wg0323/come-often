/**
 * 我的（tabBar）：会员概览 + 储值资产 + 券与积分快捷入口 + 分区宫格 + 退出登录
 * @author come-often
 * @date 2026-11-09
 */
import { getInfo, logout } from '../../api/authApi';
import { getBalanceAccount } from '../../api/balanceApi';
import { getCoupons } from '../../api/couponApi';
import { getPointsAccount } from '../../api/pointsApi';
import { clearToken, getToken, redirectToLogin } from '../../utils/request';

/** tab 与 order-list 的 Tab key 一一对应，逗号需编码后再拼进 query */
const ORDER_ENTRIES = [
  { key: 'all', icon: '/assets/icons/order-all.png', label: '全部订单', url: '/pages/order-list/order-list' },
  { key: 'created', icon: '/assets/icons/order-pay.png', label: '待支付', url: '/pages/order-list/order-list?tab=CREATED' },
  { key: 'review', icon: '/assets/icons/order-review.png', label: '待评价', url: '/pages/order-list/order-list?tab=COMPLETED' },
  {
    key: 'refund',
    icon: '/assets/icons/order-refund.png',
    label: '退款售后',
    url: '/pages/order-list/order-list?tab=REFUNDING%2CREFUNDED',
  },
];

const SERVICE_ENTRIES = [
  { key: 'queue', icon: '/assets/icons/queue.png', label: '排队号', url: '/pages/queue/queue' },
  { key: 'reserve', icon: '/assets/icons/reserve-table.png', label: '预约订座', url: '/packageOther/pages/table-reserve/table-reserve' },
  { key: 'reservation', icon: '/assets/icons/reservation.png', label: '我的预约', url: '/packageMember/pages/reservation-list/reservation-list' },
  { key: 'store', icon: '/assets/icons/store.png', label: '门店信息', url: '/packageOther/pages/store-list/store-list' },
];

const BENEFIT_ENTRIES = [
  { key: 'bundle', icon: '/assets/icons/bundle.png', label: '团购套餐', url: '/pages/bundle-list/bundle-list' },
  { key: 'verify', icon: '/assets/icons/verify.png', label: '核销记录', url: '/packageMember/pages/verify-record/verify-record' },
  { key: 'exchange', icon: '/assets/icons/points-exchange.png', label: '积分兑菜', url: '/packageMember/pages/points-exchange/points-exchange' },
  { key: 'balance', icon: '/assets/icons/balance.png', label: '我的储值', url: '/packageMember/pages/balance/balance' },
];

Page({
  data: {
    nickname: '微信用户',
    phone: '手机号未获取',
    avatarText: '客',
    levelName: '普通会员',
    balanceAmount: 0,
    points: 0,
    couponCount: 0,
    orderEntries: ORDER_ENTRIES,
    serviceEntries: SERVICE_ENTRIES,
    benefitEntries: BENEFIT_ENTRIES,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
  },

  async onShow() {
    if (!getToken()) {
      redirectToLogin();
      return;
    }
    await Promise.all([this.loadMember(), this.loadPoints(), this.loadBalance(), this.loadCoupon()]);
  },

  async loadMember() {
    try {
      const member = await getInfo();
      const nickname = member.nickname || '微信用户';
      this.setData({
        nickname,
        phone: member.phone || '手机号未获取',
        avatarText: nickname.slice(0, 1),
      });
    } catch (error) {
      this.setData({ nickname: '微信用户', phone: '手机号未获取', avatarText: '客' });
      wx.showToast({ title: error.message ?? '会员信息加载失败', icon: 'none' });
    }
  },

  async loadPoints() {
    try {
      const account = await getPointsAccount();
      this.setData({
        points: account.balance ?? 0,
        levelName: account.levelName || '普通会员',
      });
    } catch (error) {
      this.setData({ points: 0, levelName: '普通会员' });
    }
  },

  async loadBalance() {
    try {
      const account = await getBalanceAccount();
      this.setData({ balanceAmount: account.balance ?? 0 });
    } catch (error) {
      this.setData({ balanceAmount: 0 });
    }
  },

  /** 只取分页 total 当角标，不展示券列表 */
  async loadCoupon() {
    try {
      const page = await getCoupons({ tab: 'unused', current: 1, size: 1 });
      this.setData({ couponCount: page.total ?? 0 });
    } catch (error) {
      this.setData({ couponCount: 0 });
    }
  },

  onTapEntry(event) {
    wx.navigateTo({ url: event.currentTarget.dataset.url });
  },

  onTapRecharge() {
    wx.navigateTo({ url: '/packageMember/pages/balance/balance' });
  },

  onTapLevel() {
    wx.navigateTo({ url: '/packageMember/pages/member-level/member-level' });
  },

  onTapLogout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后需重新授权手机号才能查看订单与会员权益',
      confirmText: '退出',
      confirmColor: '#b23a36',
      success: (result) => {
        if (result.confirm) {
          this.doLogout();
        }
      },
    });
  },

  async doLogout() {
    try {
      await logout();
    } catch (error) {
      // 后端吊销失败也要清本地，避免用户被困在登录态
      console.warn('退出接口失败，仅清理本地登录态', error);
    }
    clearToken();
    wx.reLaunch({ url: '/pages/login/login' });
  },
});
