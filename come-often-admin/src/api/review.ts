import { http } from './request';
import type { PageData } from './dish';

/** 后台审核列表行；昵称已脱敏，状态三态由审核接口切换。 */
export interface ReviewItem {
  id: string;
  dishId: string | null;
  dishName: string;
  nickname: string;
  rating: number;
  content: string;
  withImage: boolean;
  imageObjectKeys: string[];
  status: string;
  statusText: string;
  auditUserId: string | null;
  auditTime: string | null;
  orderNo: string;
  createTime: string;
}

const toQuery = (params: Record<string, string | number | undefined>): string => {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  });
  return search.toString();
};

/** 评价审核：只有通过的评价计入菜品评分与前台评价列表。 */
export const reviewApi = {
  page: (params: Record<string, string | number | undefined>): Promise<PageData<ReviewItem>> =>
    http<PageData<ReviewItem>>(`/admin/review/page?${toQuery(params)}`),
  detail: (id: string): Promise<ReviewItem> => http<ReviewItem>(`/admin/review/${id}`),
  audit: (id: string, status: string, remark = ''): Promise<ReviewItem> =>
    http<ReviewItem>(`/admin/review/${id}/audit`, {
      method: 'POST',
      body: JSON.stringify({ status, remark }),
    }),
};
