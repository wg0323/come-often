/**
 * 券码详情：动态券码每 30 秒刷新，点击放大并调高屏幕亮度方便商家扫码
 * @author come-often
 * @date 2026-11-08
 */
import { getCouponCode } from '../../../api/couponApi';

const MIN_BRIGHTNESS = 0.2;

const GROUP_SIZE = 4;

/** 券码按 4 位分组，收银台人工输码时不易串位 */
const groupCode = (code) => {
  const groups = [];
  for (let index = 0; index < code.length; index += GROUP_SIZE) {
    groups.push(code.substring(index, index + GROUP_SIZE));
  }
  return groups.join(' ');
};

Page({
  data: {
    userCouponId: '',
    code: null,
    displayCode: '',
    countdown: 0,
    enlarged: false,
    loading: false,
    errorText: '',
  },

  async onLoad(query) {
    const userCouponId = query.userCouponId ?? '';
    if (!userCouponId) {
      this.setData({ errorText: '缺少券 ID' });
      return;
    }
    this.setData({ userCouponId });
    await getApp().globalData.loginReady;
    await this.refreshCode();
  },

  onUnload() {
    this.stopCountdown();
    this.restoreBrightness();
  },

  /** 拉一次券码并按剩余秒数起倒计时，到 0 自动再拉一次 */
  async refreshCode() {
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    try {
      const code = await getCouponCode(this.data.userCouponId);
      this.setData({
        code,
        displayCode: groupCode(code.code ?? ''),
        countdown: code.secondsRemaining ?? 0,
        errorText: '',
      });
      this.startCountdown();
    } catch (error) {
      this.setData({ errorText: error.message ?? '券码加载失败' });
      this.stopCountdown();
    } finally {
      this.setData({ loading: false });
    }
  },

  startCountdown() {
    this.stopCountdown();
    this.timer = setInterval(() => {
      const next = this.data.countdown - 1;
      if (next <= 0) {
        this.stopCountdown();
        this.refreshCode();
        return;
      }
      this.setData({ countdown: next });
    }, 1000);
  },

  stopCountdown() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  },

  async onToggleEnlarge() {
    const enlarged = !this.data.enlarged;
    this.setData({ enlarged });
    try {
      if (enlarged) {
        const current = await wx.getScreenBrightness();
        this.originalBrightness = current.value ?? MIN_BRIGHTNESS;
        await wx.setScreenBrightness({ value: 1 });
        return;
      }
      await this.restoreBrightness();
    } catch (error) {
      // 亮度调整失败不影响券码展示
    }
  },

  async restoreBrightness() {
    if (this.originalBrightness === undefined) {
      return;
    }
    try {
      await wx.setScreenBrightness({ value: this.originalBrightness });
    } catch (error) {
      // 忽略：部分机型不允许设置亮度
    } finally {
      this.originalBrightness = undefined;
    }
  },
});
