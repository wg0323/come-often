import { http } from './request';
import type { PageData } from './dish';

/** 券类型编码与后端 CouponType 一一对应，顺序即后台下拉的展示顺序。 */
export interface CouponTypeOption {
  value: string;
  label: string;
}

export const COUPON_TYPES: CouponTypeOption[] = [
  { value: 'FULL_REDUCTION', label: '满减券' },
  { value: 'DISCOUNT', label: '折扣券' },
  { value: 'DISH', label: '单品券' },
  { value: 'NO_THRESHOLD', label: '无门槛券' },
  { value: 'BIRTHDAY', label: '生日券' },
];

export interface CouponTemplateItem {
  id: string;
  templateName: string;
  couponType: string;
  couponTypeText: string;
  faceValue: number;
  discountRate: number | null;
  thresholdAmount: number;
  dishId: string | null;
  dishIds: string[];
  total: number;
  remain: number;
  validDays: number;
  startTime: string | null;
  endTime: string | null;
  stackable: number;
  status: number;
  statusText: string;
  createTime: string;
}

export interface CouponTemplatePayload {
  templateName: string;
  couponType: string;
  faceValue?: number;
  discountRate?: number;
  thresholdAmount?: number;
  dishId?: string;
  dishIds?: string[];
  total: number;
  validDays?: number;
  startTime?: string;
  endTime?: string;
  stackable?: number;
  status?: number;
}

export interface CouponGrantPayload {
  templateId: string;
  memberIds: string[];
}

export interface CouponVerifyPayload {
  code: string;
  orderNo?: string;
}

export interface CouponVerifyResult {
  userCouponId: string;
  templateId: string;
  templateName: string;
  couponType: string;
  couponTypeText: string;
  faceValue: number;
  thresholdAmount: number;
  discountAmount: number;
  memberId: string;
  storeId: string;
  operatorUserId: string;
  verifyChannel: string;
  verifyChannelText: string;
  verifyTime: string;
}

export interface CouponUseRecordItem {
  id: string;
  userCouponId: string;
  templateName: string;
  couponTypeText: string;
  memberId: string;
  orderId: string | null;
  discountAmount: number;
  operatorUserId: string;
  operatorName: string;
  verifyChannel: string;
  verifyChannelText: string;
  verifyTime: string;
}

/** 券发放规则：新客 / 生日 / 复购 / 沉睡唤醒，事件与每日任务共用同一份配置。 */
export interface CouponRuleItem {
  id: string;
  ruleCode: string;
  ruleName: string;
  ruleType: string;
  ruleTypeText: string;
  couponTemplateId: string;
  templateName: string;
  triggerValue: number;
  maxPerMember: number;
  isEnabled: number;
  periodText: string;
  remark: string;
  createTime: string;
}

export interface CouponRulePayload {
  ruleCode: string;
  ruleName: string;
  ruleType: string;
  couponTemplateId: string;
  triggerValue?: number;
  maxPerMember?: number;
  enabled?: boolean;
  remark?: string;
}

export interface CouponRoiItem {
  templateId: string;
  templateName: string;
  couponType: string;
  couponTypeText: string;
  total: number;
  remain: number;
  receivedCount: number;
  usedCount: number;
  orderCount: number;
  discountAmount: number;
  revenueAmount: number;
  roi: number | null;
  useRate: number;
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

/** 营销管理：模板 CRUD、定向发券、核销台、核销记录与券 ROI。金额与抵扣全部取接口返回。 */
export const couponApi = {
  pageTemplate: (params: Record<string, string | number | undefined>): Promise<PageData<CouponTemplateItem>> =>
    http<PageData<CouponTemplateItem>>(`/admin/coupon/template/page?${toQuery(params)}`),
  createTemplate: (payload: CouponTemplatePayload): Promise<CouponTemplateItem> =>
    http<CouponTemplateItem>('/admin/coupon/template', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  updateTemplate: (templateId: string, payload: CouponTemplatePayload): Promise<CouponTemplateItem> =>
    http<CouponTemplateItem>(`/admin/coupon/template/${templateId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  updateStatus: (templateId: string, status: number): Promise<CouponTemplateItem> =>
    http<CouponTemplateItem>(`/admin/coupon/template/${templateId}/status?status=${status}`, {
      method: 'POST',
    }),
  grant: (payload: CouponGrantPayload): Promise<number> =>
    http<number>('/admin/coupon/grant', { method: 'POST', body: JSON.stringify(payload) }),
  verify: (payload: CouponVerifyPayload): Promise<CouponVerifyResult> =>
    http<CouponVerifyResult>('/admin/coupon/verify', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  pageRecord: (params: Record<string, string | number | undefined>): Promise<PageData<CouponUseRecordItem>> =>
    http<PageData<CouponUseRecordItem>>(`/admin/coupon/record/page?${toQuery(params)}`),
  roi: (templateId?: string): Promise<CouponRoiItem[]> =>
    http<CouponRoiItem[]>(`/admin/coupon/roi${templateId ? `?templateId=${templateId}` : ''}`),
  rules: (): Promise<CouponRuleItem[]> => http<CouponRuleItem[]>('/admin/coupon/rule'),
  saveRule: (payload: CouponRulePayload): Promise<CouponRuleItem> =>
    http<CouponRuleItem>('/admin/coupon/rule', {
      method: 'POST',
      body: JSON.stringify(payload),
    }),
  updateRuleStatus: (ruleId: string, enabled: boolean): Promise<CouponRuleItem> =>
    http<CouponRuleItem>(`/admin/coupon/rule/${ruleId}/status?enabled=${enabled}`, {
      method: 'POST',
    }),
  runRule: (ruleType: string): Promise<number> =>
    http<number>(`/admin/coupon/rule/run?ruleType=${ruleType}`, { method: 'POST' }),
};
