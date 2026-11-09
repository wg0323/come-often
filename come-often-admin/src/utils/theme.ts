/**
 * 读取 variables.scss 里的 CSS 变量。图表配置必须用 token 而不是写死色值，
 * 页面与图表共用同一套品牌色。
 */
export const readCssVar = (name: string): string =>
  getComputedStyle(document.documentElement).getPropertyValue(name).trim();
