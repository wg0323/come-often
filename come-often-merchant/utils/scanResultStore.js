/**
 * 扫码结果在「扫码页 → 核销结果页」之间的一次性传递
 * @author come-often
 * @date 2026-11-09
 */
let pendingResult = null;

export const setScanResult = (value) => {
  pendingResult = value ?? null;
};

export const takeScanResult = () => {
  const value = pendingResult;
  pendingResult = null;
  return value;
};
