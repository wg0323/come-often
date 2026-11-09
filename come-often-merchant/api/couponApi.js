/**
 * 商家端核销记录接口
 * @author come-often
 * @date 2026-11-09
 */
import request from '../utils/request';
import { toQuery } from '../utils/query';

export const pageRecords = (params) => request(`/coupon/record/page${toQuery(params)}`);

export const listOperators = (date) => request(`/coupon/record/operators${toQuery({ date })}`);
