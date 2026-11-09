/**
 * 首页接口定义。M2 提供 /home/summary 后由首页 onLoad 调用。
 * @author come-often
 * @date 2026-11-08
 */
import request from '../utils/request';

export const getHomeSummary = () => request('/home/summary');
