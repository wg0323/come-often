/**
 * 顾客端小程序入口：不做静默登录，登录态只由登录页写入
 * @author come-often
 * @date 2026-11-08
 */
App({
  globalData: {
    storeId: null,
    memberId: null,
    // tabBar 页 switchTab 不能带 query，首页要切到的菜品分类经此传递，由点单页消费后清空
    pendingCategoryId: '',
    // 点餐方式：DINE_IN 需已绑定桌码，PICKUP 需先选预约时间
    orderMode: '',
    boundTableCode: wx.getStorageSync('boundTableCode') || '',
    pickupTime: '',
    // 从菜品详情返回点餐页时自动展开购物车弹层
    pendingOpenCart: false,
    // 页面里的 await loginReady 保留为空操作；未登录时由首页与 request 统一跳登录页
    loginReady: Promise.resolve(),
  },
});
