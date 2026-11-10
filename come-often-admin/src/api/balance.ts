import { http } from './request';
import type { PageData } from './dish';

/** PC 后台储值行：手机号已脱敏，余额与累计值全部取接口返回。 */
export interface BalanceMemberItem {
  memberId: string;
  nickname: string;
  phoneMasked: string;
  balance: number;
  totalRecharge: number;
  totalGift: number;
  totalConsume: number;
}

export interface BalanceRecordItem {
  id: string;
  memberId: string;
  changeType: string;
  changeTypeText: string;
  changeAmount: number;
  balanceAfter: number;
  bizType: string;
  bizId: string;
  remark: string;
  createTime: string;
}

const toQuery = (params: Record<string, string | number | undefined>): string => {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value));
    }
  });
  return query.toString();
};

/** 储值管理：会员余额与储值流水；后台只读，余额变动全部来自充值、下单与退款。 */
export const balanceApi = {
  pageMember: (params: Record<string, string | number | undefined>): Promise<PageData<BalanceMemberItem>> =>
    http<PageData<BalanceMemberItem>>(`/admin/balance/member/page?${toQuery(params)}`),
  pageRecord: (params: Record<string, string | number | undefined>): Promise<PageData<BalanceRecordItem>> =>
    http<PageData<BalanceRecordItem>>(`/admin/balance/record/page?${toQuery(params)}`),
};
