/**
 * 我的预约：进行中 / 已完成 / 已取消三 Tab，支持签到码、取消与改期
 * @author come-often
 * @date 2026-11-09
 */
import {
  cancelReservation,
  getMyReservations,
  getReservationCode,
} from '../../../api/reservationApi';
import { drawQrCode } from '../../../utils/qrcodeCanvas';

const TABS = [
  { key: 'active', label: '进行中' },
  { key: 'finished', label: '已完成' },
  { key: 'cancelled', label: '已取消' },
];

const PAGE_SIZE = 10;

/** 签到码二维码的 canvas id 与 CSS 固定边长（须与 wxss 一致） */
const QR_CANVAS_ID = 'reservation-qr';

const QR_SIZE = 200;

Page({
  data: {
    tabs: TABS,
    activeTab: 'active',
    records: [],
    current: 1,
    hasMore: true,
    loading: false,
    loaded: false,
    codeVisible: false,
    codeData: null,
    qrError: false,
  },

  async onLoad() {
    await getApp().globalData.loginReady;
    await this.loadReservations(true);
  },

  onShow() {
    if (this.data.loaded) {
      this.loadReservations(true);
    }
  },

  onHide() {
    this.closeCode();
  },

  onUnload() {
    this.closeCode();
  },

  async loadReservations(reset) {
    if (this.data.loading) {
      return;
    }
    const current = reset ? 1 : this.data.current + 1;
    this.setData({ loading: true });
    try {
      const page = await getMyReservations({
        tab: this.data.activeTab,
        current,
        size: PAGE_SIZE,
      });
      const records = page.records ?? [];
      const merged = reset ? records : this.data.records.concat(records);
      this.setData({
        records: merged,
        current,
        hasMore: merged.length < (page.total ?? 0),
        loaded: true,
      });
    } catch (error) {
      wx.showToast({ title: error.message ?? '预约加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  onTabChange(event) {
    const { key } = event.currentTarget.dataset;
    if (key === this.data.activeTab) {
      return;
    }
    this.setData({ activeTab: key, records: [], current: 1, hasMore: true }, () => {
      this.loadReservations(true);
    });
  },

  async onCancel(event) {
    const { reserveNo } = event.currentTarget.dataset;
    const confirm = await new Promise((resolve) => {
      wx.showModal({
        title: '取消预约',
        content: '取消后时段将立即释放，确定取消？',
        success: resolve,
        fail: () => resolve({ confirm: false }),
      });
    });
    if (!confirm.confirm) {
      return;
    }
    try {
      await cancelReservation(reserveNo);
      wx.showToast({ title: '已取消', icon: 'success' });
      await this.loadReservations(true);
    } catch (error) {
      wx.showToast({ title: error.message ?? '取消失败', icon: 'none' });
    }
  },

  onReschedule(event) {
    const { reserveNo, storeId } = event.currentTarget.dataset;
    wx.navigateTo({
      url: `/packageOther/pages/table-reserve/table-reserve?reserveNo=${reserveNo}&storeId=${storeId}`,
    });
  },

  async onShowCode(event) {
    const { reserveNo } = event.currentTarget.dataset;
    try {
      const codeData = await getReservationCode(reserveNo);
      this.setData({
        codeVisible: true,
        codeData,
        qrError: false,
      }, () => {
        // canvas 是原生组件，等弹层渲染完成再取节点
        wx.nextTick(() => this.drawQr(codeData.codeContent));
      });
      this.scheduleRefresh(reserveNo, codeData.secondsRemaining);
    } catch (error) {
      wx.showToast({ title: error.message ?? '签到码获取失败', icon: 'none' });
    }
  },

  /** 把当前码串画成二维码；30 秒刷新后重绘 */
  async drawQr(text) {
    if (!text || !this.data.codeVisible) {
      return;
    }
    try {
      await drawQrCode({
        canvasId: QR_CANVAS_ID,
        scope: this,
        text,
        size: QR_SIZE,
      });
      // 异步期间码已刷新或弹层已关闭，丢弃过期结果
      if (this.data.codeVisible && this.data.codeData?.codeContent === text) {
        this.setData({ qrError: false });
      }
    } catch (error) {
      console.warn('签到码二维码绘制失败', error);
      if (this.data.codeVisible) {
        this.setData({ qrError: true });
      }
    }
  },

  scheduleRefresh(reserveNo, secondsRemaining) {
    this.clearCodeTimer();
    const waitMillis = Math.max(1, secondsRemaining || 30) * 1000 + 300;
    this.codeTimer = setTimeout(async () => {
      if (!this.data.codeVisible) {
        return;
      }
      try {
        const codeData = await getReservationCode(reserveNo);
        // 请求期间用户可能已关闭弹层，避免关闭后再次 setData 与续排定时器
        if (!this.data.codeVisible) {
          return;
        }
        this.setData({ codeData }, () => {
          wx.nextTick(() => this.drawQr(codeData.codeContent));
        });
        this.scheduleRefresh(reserveNo, codeData.secondsRemaining);
      } catch (error) {
        this.clearCodeTimer();
      }
    }, waitMillis);
  },

  clearCodeTimer() {
    if (this.codeTimer) {
      clearTimeout(this.codeTimer);
      this.codeTimer = null;
    }
  },

  closeCode() {
    this.clearCodeTimer();
    if (this.data.codeVisible) {
      this.setData({ codeVisible: false, codeData: null });
    }
  },

  onCopyCode() {
    const code = this.data.codeData?.codeContent;
    if (!code) {
      return;
    }
    wx.setClipboardData({
      data: code,
      success: () => wx.showToast({ title: '签到码已复制', icon: 'none' }),
    });
  },

  onTapReserve() {
    wx.navigateTo({ url: '/packageOther/pages/store-list/store-list' });
  },

  noop() {
    // 阻止遮罩层点击穿透到卡片
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadReservations(false);
    }
  },
});
