/**
 * 小程序 canvas 2d 二维码绘制：把动态码串画成可被 wx.scanCode 识别的黑白矩阵。
 * 矩阵生成依赖 qrcode.js（qrcode-generator@1.4.4, MIT），绘制全在端上完成，后端零改动。
 * @author come-often
 * @date 2026-11-09
 */
import qrcode from './qrcode.js';

/** QR 规范要求四周留 4 个模块的静默区 */
const QUIET_ZONE = 4;

/** 纠错级别 M：33 位签到码落在 Version 3（29×29），兼顾密度与识别率 */
const ERROR_CORRECTION = 'M';

const DARK_COLOR = '#22302a';

const LIGHT_COLOR = '#ffffff';

/** SelectorQuery 只有回调形式，包成 Promise 供 async/await 使用 */
const getCanvasNode = (canvasId, scope) => new Promise((resolve, reject) => {
  wx.createSelectorQuery()
    .in(scope)
    .select(`#${canvasId}`)
    .fields({ node: true, size: true })
    .exec((results) => {
      const canvas = results?.[0]?.node;
      if (!canvas) {
        reject(new Error('二维码画布未就绪'));
        return;
      }
      resolve(canvas);
    });
});

/**
 * 把码串画到指定 canvas。canvas 的 CSS 宽高由样式固定，绝不通过 setData 动态改：
 * 低版本基础库（如 2.32.3）会在渲染层抛 this._getData is not a function。
 * 物理边长按 dpr 固定，模块边界取整到整数物理像素，既无灰边也不触发组件重建。
 * @param {object} options 绘制参数
 * @param {string} options.canvasId canvas 的 id（不带 #）
 * @param {object} options.scope 页面实例，用于 SelectorQuery.in
 * @param {string} options.text 码串原文（含业务前缀，不做任何加工）
 * @param {number} options.size canvas 的 CSS 边长（px），须与 wxss 里的固定值一致
 * @returns {Promise<void>}
 */
export const drawQrCode = async ({ canvasId, scope, text, size }) => {
  const canvas = await getCanvasNode(canvasId, scope);
  const qr = qrcode(0, ERROR_CORRECTION);
  qr.addData(text);
  qr.make();

  const count = qr.getModuleCount();
  const total = count + QUIET_ZONE * 2;
  // getSystemInfoSync 兼容项目最低基础库 2.11.1（getWindowInfo 需 2.20.1+）
  const pixelRatio = wx.getSystemInfoSync().pixelRatio ?? 1;
  const side = Math.round(size * pixelRatio);
  const cell = side / total;
  canvas.width = side;
  canvas.height = side;

  const context = canvas.getContext('2d');
  context.fillStyle = LIGHT_COLOR;
  context.fillRect(0, 0, side, side);
  context.fillStyle = DARK_COLOR;
  for (let row = 0; row < count; row += 1) {
    for (let col = 0; col < count; col += 1) {
      if (!qr.isDark(row, col)) {
        continue;
      }
      const x0 = Math.round((col + QUIET_ZONE) * cell);
      const y0 = Math.round((row + QUIET_ZONE) * cell);
      const x1 = Math.round((col + QUIET_ZONE + 1) * cell);
      const y1 = Math.round((row + QUIET_ZONE + 1) * cell);
      context.fillRect(x0, y0, x1 - x0, y1 - y0);
    }
  }
};
