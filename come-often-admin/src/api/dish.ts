import { http } from './request';

export interface PageData<T> {
  total: number;
  current: number;
  size: number;
  records: T[];
}

export interface DishCategory {
  id: string;
  categoryName: string;
  storageName: string;
  sort: number;
  dishCount: number;
  preorder: boolean;
}

export interface DishListItem {
  id: string;
  categoryId: string;
  categoryName: string;
  dishName: string;
  subtitle: string;
  price: number;
  originalPrice: number | null;
  monthlySales: number;
  craftInfo: string;
  preorder: boolean;
  soldOut: boolean;
  status: number;
  sort: number;
  listImage: string | null;
  tags: string[];
}

export interface SpecOptionPayload {
  id?: string;
  optionName: string;
  priceDelta: number;
  sort?: number;
}

export interface SpecGroupPayload {
  id?: string;
  specName: string;
  required: boolean;
  sort?: number;
  options: SpecOptionPayload[];
}

export interface AddonPayload {
  id?: string;
  addonName: string;
  priceDelta: number;
}

export interface ImagePayload {
  objectKey: string;
  imageType: string;
  sort?: number;
}

export interface DishSavePayload {
  dishName: string;
  categoryId: string;
  price: number;
  originalPrice?: number | null;
  subtitle?: string;
  description?: string;
  allergen?: string;
  craftInfo?: string;
  monthlySales?: number;
  preorder?: boolean;
  status?: number;
  sort?: number;
  stockTotal?: number;
  specGroups?: SpecGroupPayload[];
  addons?: AddonPayload[];
  images?: ImagePayload[];
  tagIds?: string[];
}

export interface DishDetail extends DishSavePayload {
  id: string;
  preorderBadge: string;
  preorderNote: string;
  depositAmount: number;
  soldOut: boolean;
  stockRemain: number | null;
  tagIds: string[];
  specGroups: SpecGroupPayload[];
  addons: AddonPayload[];
  images: ImagePayload[];
}

export interface CategorySavePayload {
  categoryName: string;
  sort?: number;
  show?: boolean;
}

export interface DishStock {
  dishId: string;
  dishName: string;
  stockDate: string;
  total: number;
  remain: number;
  soldOut: boolean;
  configured: boolean;
}

export interface BannerItem {
  id: string;
  title: string;
  objectKey: string;
  jumpType: string;
  jumpValue: string;
  weight: number;
}

export interface NoticeItem {
  id: string;
  content: string;
  noticeType: string;
}

export interface BannerSavePayload {
  title: string;
  objectKey: string;
  jumpType?: string;
  jumpValue?: string;
  weight?: number;
}

export interface NoticeSavePayload {
  content: string;
  noticeType?: string;
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

export const dishApi = {
  page: (params: Record<string, string | number | undefined>): Promise<PageData<DishListItem>> =>
    http<PageData<DishListItem>>(`/admin/dish/page?${toQuery(params)}`),
  detail: (id: string): Promise<DishDetail> => http<DishDetail>(`/admin/dish/${id}`),
  create: (payload: DishSavePayload): Promise<string> =>
    http<string>('/admin/dish', { method: 'POST', body: JSON.stringify(payload) }),
  update: (id: string, payload: DishSavePayload): Promise<string> =>
    http<string>(`/admin/dish/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  remove: (id: string): Promise<void> =>
    http<void>(`/admin/dish/${id}`, { method: 'DELETE' }),
  setStatus: (id: string, status: number): Promise<void> =>
    http<void>(`/admin/dish/${id}/status?status=${status}`, { method: 'PUT' }),
  updateSort: (id: string, sort: number): Promise<void> =>
    http<void>(`/admin/dish/${id}/sort?sort=${sort}`, { method: 'PUT' }),
};

export const categoryApi = {
  list: (): Promise<DishCategory[]> => http<DishCategory[]>('/admin/dish/category'),
  create: (payload: CategorySavePayload): Promise<string> =>
    http<string>('/admin/dish/category', { method: 'POST', body: JSON.stringify(payload) }),
  update: (id: string, payload: CategorySavePayload): Promise<string> =>
    http<string>(`/admin/dish/category/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  remove: (id: string): Promise<void> =>
    http<void>(`/admin/dish/category/${id}`, { method: 'DELETE' }),
  updateSort: (id: string, sort: number): Promise<void> =>
    http<void>(`/admin/dish/category/${id}/sort?sort=${sort}`, { method: 'PUT' }),
};

export const stockApi = {
  list: (): Promise<DishStock[]> => http<DishStock[]>('/admin/dish/stock'),
  apply: (payload: { dishId: string; total?: number; soldOut?: boolean }): Promise<void> =>
    http<void>('/admin/dish/stock', { method: 'PUT', body: JSON.stringify(payload) }),
  rebuild: (): Promise<number> => http<number>('/admin/dish/stock/rebuild', { method: 'POST' }),
};

export const bannerApi = {
  list: (): Promise<BannerItem[]> => http<BannerItem[]>('/admin/home/banners'),
  create: (payload: BannerSavePayload): Promise<string> =>
    http<string>('/admin/home/banners', { method: 'POST', body: JSON.stringify(payload) }),
  update: (id: string, payload: BannerSavePayload): Promise<string> =>
    http<string>(`/admin/home/banners/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  remove: (id: string): Promise<void> =>
    http<void>(`/admin/home/banners/${id}`, { method: 'DELETE' }),
};

export const noticeApi = {
  list: (): Promise<NoticeItem[]> => http<NoticeItem[]>('/admin/home/notices'),
  create: (payload: NoticeSavePayload): Promise<string> =>
    http<string>('/admin/home/notices', { method: 'POST', body: JSON.stringify(payload) }),
  update: (id: string, payload: NoticeSavePayload): Promise<string> =>
    http<string>(`/admin/home/notices/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  remove: (id: string): Promise<void> =>
    http<void>(`/admin/home/notices/${id}`, { method: 'DELETE' }),
};

export interface UploadResult {
  objectKey: string;
  size: number;
}

/**
 * 上传图片走 multipart，不能用只发 JSON 的 http<T>()；token 仍需手动注入。
 */
export const uploadImage = async (file: File, prefix: string): Promise<UploadResult> => {
  const form = new FormData();
  form.append('file', file);
  form.append('prefix', prefix);
  const token = sessionStorage.getItem('adminToken');
  const response = await fetch(`/api/admin/file/upload?prefix=${encodeURIComponent(prefix)}`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form,
  });
  const body = (await response.json()) as { code: number; message: string; data: UploadResult };
  if (!response.ok || body.code !== 0) {
    throw new Error(body.message || '图片上传失败');
  }
  return body.data;
};
