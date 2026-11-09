/**
 * 我的（tabBar）：会员概览 + 积分等级 + 功能入口 + 退出登录
 * @author come-often
 * @date 2026-11-09
 */
import { getInfo, logout } from '../../api/authApi';
import { getPointsAccount } from '../../api/pointsApi';
import { clearToken, getToken, redirectToLogin } from '../../utils/request';

const MENU_ITEMS = [
  {
    key: 'order',
    label: '我的订单',
    desc: '全部订单与评价',
    url: '/pages/order-list/order-list',
  },
  {
    key: 'cart',
    label: '购物车',
    desc: '查看与结算已选菜品',
    url: '/pages/cart/cart',
  },
  {
    key: 'queue',
    label: '我的排队号',
    desc: '到店取号与叫号进度',
    url: '/pages/queue/queue',
  },
  {
    key: 'coupon',
    label: '我的券',
    desc: '卡包与动态券码',
    url: '/packageMember/pages/coupon-wallet/coupon-wallet',
  },
  {
    key: 'points',
    label: '我的积分',
    desc: '余额、签到与流水',
    url: '/packageMember/pages/points-record/points-record',
  },
  {
    key: 'reservation',
    label: '我的预约',
    desc: '订座记录与状态',
    url: '/packageMember/pages/reservation-list/reservation-list',
  },
  {
    key: 'reserve',
    label: '预约订座',
    desc: '选时段与桌型',
    url: '/packageOther/pages/table-reserve/table-reserve',
  },
  {
    key: 'store',
    label: '门店信息',
    desc: '营业时间与地址',
    url: '/packageOther/pages/store-list/store-list',
  },
];

Page({
  data: {
    nickname: '微信用户',
    phone: '手机号未获取',
    avatarText: '客',
    balance: 0,
    levelName: '普通会员',
    menuItems: MENU_ITEMS,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
  },

  async onShow() {
    if (!getToken()) {
      redirectToLogin();
      return;
    }
    await this.loadMember();
    await this.loadAccount();
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

  async loadAccount() {
    try {
      const account = await getPointsAccount();
      this.setData({
        balance: account.balance ?? 0,
        levelName: account.levelName || '普通会员',
      });
    } catch (error) {
      this.setData({ balance: 0, levelName: '普通会员' });
    }
  },

  onTapCell(event) {
    wx.navigateTo({ url: event.currentTarget.dataset.url });
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
