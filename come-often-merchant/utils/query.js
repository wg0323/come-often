/**
 * 查询串拼接：跳过空值，统一 encodeURIComponent
 * @author come-often
 * @date 2026-11-09
 */
export const toQuery = (params = {}) => {
  const pairs = [];
  Object.keys(params).forEach((key) => {
    const value = params[key];
    if (value === undefined || value === null || value === '') {
      return;
    }
    pairs.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`);
  });
  return pairs.length > 0 ? `?${pairs.join('&')}` : '';
};
