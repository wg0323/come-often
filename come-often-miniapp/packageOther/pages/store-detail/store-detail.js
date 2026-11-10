/**
 * 门店详情：地址、电话、营业时间与桌型概况，主按钮进入桌台预约
 * @author come-often
 * @date 2026-11-09
 */
import { getStoreDetail } from '../../../api/storeApi';

const TABLE_TYPE_LABELS = {
  HALL_CARD: '大厅卡座',
  PRIVATE_SMALL: '包间（6-10 人）',
  PRIVATE_LARGE: '大包间（12-20 人）',
  WINDOW: '靠窗',
};

Page({
  data: {
    storeId: '',
    store: null,
    tableGroups: [],
    weekDayText: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    loading: false,
  },

  async onLoad(query) {
    await getApp().globalData.loginReady;
    const storeId = query.storeId ?? '';
    this.setData({ storeId });
    await this.loadStore(storeId);
  },

  async loadStore(storeId) {
    if (!storeId) {
      wx.showToast({ title: '缺少门店参数', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    try {
      const store = await getStoreDetail(storeId);
      this.setData({
        store,
        tableGroups: this.groupTables(store.tables ?? []),
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '门店加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  groupTables(tables) {
    const groups = [];
    tables.forEach((table) => {
      let group = groups.find((item) => item.type === table.tableType);
      if (!group) {
        group = {
          type: table.tableType,
          label: TABLE_TYPE_LABELS[table.tableType] ?? table.tableType,
          count: 0,
          capacity: table.capacity,
        };
        groups.push(group);
      }
      group.count += 1;
    });
    return groups;
  },

  /** wx.openLocation 不需要腾讯地图 Key，经纬度由门店详情接口下发；未配置定位时引导电话联系 */
  onTapNavigate() {
    const store = this.data.store;
    const latitude = Number(store?.latitude);
    const longitude = Number(store?.longitude);
    const located = Number.isFinite(latitude) && Number.isFinite(longitude)
      && !(latitude === 0 && longitude === 0);
    if (!located) {
      wx.showToast({ title: '门店暂未配置定位，请电话联系', icon: 'none' });
      return;
    }
    wx.openLocation({
      latitude,
      longitude,
      name: store.storeName,
      address: store.address,
      scale: 18,
    });
  },

  onTapReserve() {
    wx.navigateTo({
      url: `/packageOther/pages/table-reserve/table-reserve?storeId=${this.data.storeId}`,
    });
  },

  onTapMine() {
    wx.navigateTo({ url: '/packageMember/pages/reservation-list/reservation-list' });
  },
});
