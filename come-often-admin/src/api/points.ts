import { http } from './request';
import type { PageData } from './dish';

/** 积分规则参数来自后端配置，后台只读展示。 */
export interface PointsRule {
  orderEarnRate: number;
  exchangeRate: number;
  signInPoints: number;
  reviewPoints: number;
  sharePoints: number;
  profilePoints: number;
  validDays: number;
  exchangeText: string;
}

export interface MemberLevelItem {
  levelId: string;
  levelName: string;
  levelCode: string;
  minAmount: number;
  minPoints: number;
  discountRate: number;
  sort: number;
}

export interface MemberLevelPayload {
  levelName: string;
  minAmount: number;
  minPoints: number;
  discountRate: number;
  sort?: number;
}

export interface PointsMemberItem {
  memberId: string;
  nickname: string;
  phoneMasked: string;
  balance: number;
  totalEarned: number;
  totalUsed: number;
  levelId: string | null;
  levelCode: string;
  levelName: string;
  discountRate: number | null;
  tags: string[];
}

export interface PointsRecordItem {
  id: string;
  memberId: string;
  changeType: string;
  changeTypeText: string;
  changeAmount: number;
  bizType: string;
  bizId: string;
  balanceAfter: number;
  expireAt: string | null;
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

/** 用户管理：积分规则、等级配置、会员积分与积分流水。余额与门槛一律取接口返回。 */
export const pointsApi = {
  rule: (): Promise<PointsRule> => http<PointsRule>('/admin/points/rule'),
  levels: (): Promise<MemberLevelItem[]> => http<MemberLevelItem[]>('/admin/points/level'),
  updateLevel: (levelId: string, payload: MemberLevelPayload): Promise<MemberLevelItem> =>
    http<MemberLevelItem>(`/admin/points/level/${levelId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  pageMember: (params: Record<string, string | number | undefined>): Promise<PageData<PointsMemberItem>> =>
    http<PageData<PointsMemberItem>>(`/admin/points/member/page?${toQuery(params)}`),
  pageRecord: (params: Record<string, string | number | undefined>): Promise<PageData<PointsRecordItem>> =>
    http<PageData<PointsRecordItem>>(`/admin/points/record/page?${toQuery(params)}`),
  runExpire: (): Promise<number> => http<number>('/admin/points/expire/run', { method: 'POST' }),
  /** 与每日 00:30 的 member-tag 任务同一条链路，重复执行只统计实际变化 */
  refreshTag: (): Promise<number> => http<number>('/admin/points/tag/refresh', { method: 'POST' }),
};
