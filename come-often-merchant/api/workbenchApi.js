/**
 * 商家端工作台接口
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';

export const getSummary = () => request('/workbench/summary');
