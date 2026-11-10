import { http } from './request';
import type { PageData } from './dish';

/** 团购套餐明细；菜名是后端查库后的快照，编辑时只需提交菜品 ID 与份数。 */
export interface BundleItem {
  dishId: string;
  dishName: string;
  quantity: number;
  sort: number;
}

export interface BundleItemPayload {
  dishId: string;
  quantity: number;
}

export interface BundleVO {
  id: string;
  bundleName: string;
  description: string;
  bundlePrice: number;
  originalPrice: number | null;
  totalStock: number;
  remainStock: number;
  status: number;
  statusText: string;
  items: BundleItem[];
  createTime: string;
}

export interface BundleSavePayload {
  id?: string;
  bundleName: string;
  description?: string;
  bundlePrice: number;
  originalPrice?: number | null;
  totalStock: number;
  status?: number;
  items: BundleItemPayload[];
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

/** 套餐管理：整体覆盖式保存，明细每次全量提交。 */
export const bundleApi = {
  page: (params: Record<string, string | number | undefined>): Promise<PageData<BundleVO>> =>
    http<PageData<BundleVO>>(`/admin/bundle/page?${toQuery(params)}`),
  detail: (id: string): Promise<BundleVO> => http<BundleVO>(`/admin/bundle/${id}`),
  save: (payload: BundleSavePayload): Promise<string> =>
    http<string>('/admin/bundle/save', { method: 'POST', body: JSON.stringify(payload) }),
  setStatus: (id: string, status: number): Promise<void> =>
    http<void>(`/admin/bundle/${id}/status?status=${status}`, { method: 'POST' }),
  remove: (id: string): Promise<void> =>
    http<void>(`/admin/bundle/${id}/remove`, { method: 'POST' }),
};
