/**
 * 商家端统一扫码接口
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';

export const resolveScan = (code) => request('/scan/resolve', {
  method: 'POST',
  data: { code },
});
