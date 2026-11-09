/**
 * 商家端核销结果：整屏色块 + 震动，成功展示券面信息与抵扣金额，失败给出可直接展示的原因
 * @author come-often
 * @date 2026-11-09
 */
import { takeScanResult } from '../../utils/scanResultStore';
import { formatMoney } from '../../utils/display';

Page({
  data: {
    result: null,
    details: [],
    resultClass: 'result--fail',
  },
  onLoad() {
    const result = takeScanResult();
    if (!result) {
      wx.redirectTo({ url: '/pages/scan/scan' });
      return;
    }
    const success = result.success === true;
    this.setData({
      result,
      details: this.buildDetails(result),
      resultClass: success ? 'result--success' : 'result--fail',
    });
    if (success) {
      wx.vibrateShort({ type: 'medium' });
    } else {
      wx.vibrateLong();
    }
  },
  buildDetails(result) {
    const rows = [];
    if (result.codeType === 'COUPON') {
      rows.push({ label: '券名称', value: result.templateName || '' });
      rows.push({ label: '券类型', value: result.couponTypeText || '' });
      rows.push({ label: '面额', value: `¥${formatMoney(result.faceValue)}` });
      rows.push({ label: '抵扣金额', value: `¥${formatMoney(result.discountAmount)}` });
    } else if (result.codeType === 'RESERVATION') {
      rows.push({ label: '预约单号', value: result.bizNo || '' });
      rows.push({ label: '预约时段', value: `${result.reserveDate || ''} ${result.timeSlot || ''}` });
      rows.push({ label: '桌型', value: result.tableTypeText || '' });
      rows.push({ label: '就餐人数', value: result.peopleCount ? `${result.peopleCount} 位` : '' });
      rows.push({ label: '联系人', value: result.contactName || '' });
      rows.push({ label: '签到时间', value: result.checkinTime || '' });
    } else if (result.codeType === 'PICKUP') {
      rows.push({ label: '订单号', value: result.orderNo || '' });
      rows.push({ label: '取餐码', value: result.pickupCode || '' });
      rows.push({ label: '订单类型', value: result.orderTypeText || '' });
      rows.push({ label: '订单状态', value: result.orderStatusText || '' });
    }
    if (result.notice) {
      rows.push({ label: '提示', value: result.notice });
    }
    return rows.filter((row) => row.value);
  },
  onTapScanAgain() {
    wx.redirectTo({ url: '/pages/scan/scan' });
  },
  onTapHome() {
    wx.reLaunch({ url: '/pages/index/index' });
  },
});
