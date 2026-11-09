/**
 * 商家端扫码：只有一个扫码按钮，码型由后端 scan/resolve 判定并直接执行
 * @author come-often
 * @date 2026-11-09
 */
import { resolveScan } from '../../api/scanApi';
import { setScanResult } from '../../utils/scanResultStore';

Page({
  data: {
    loading: false,
  },
  async onTapScan() {
    if (this.data.loading) {
      return;
    }
    let code = '';
    try {
      const result = await wx.scanCode({ scanType: ['qrCode', 'barCode'] });
      code = result?.result ?? '';
    } catch (error) {
      // 用户取消扫码不算错误
      console.warn('扫码取消或失败', error);
      return;
    }
    if (!code) {
      wx.showToast({ title: '没有读到码内容', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    wx.showLoading({ title: '核销中', mask: true });
    try {
      const resolved = await resolveScan(code);
      wx.hideLoading();
      setScanResult(resolved);
      wx.navigateTo({ url: '/pages/scan-result/scan-result' });
    } catch (error) {
      wx.hideLoading();
      wx.showToast({ title: error.message ?? '扫码核销失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
  onTapHome() {
    wx.reLaunch({ url: '/pages/index/index' });
  },
});
