import { http } from './request';

export interface BusinessHour {
  id: string;
  weekDay: number;
  weekDayText: string;
  openTime: string;
  closeTime: string;
}

export interface StoreTable {
  id: string;
  tableNo: string;
  tableType: string;
  capacity: number;
  status: number;
}

export interface StoreInfo {
  id: string;
  storeName: string;
  address: string;
  phone: string;
  longitude: number | null;
  latitude: number | null;
  businessStatus: number;
  businessStatusText: string;
  businessHours: BusinessHour[];
  tables: StoreTable[];
}

export interface StoreUpdatePayload {
  storeName: string;
  address?: string;
  phone?: string;
  businessStatus?: number;
  businessHours?: Array<{ weekDay: number; openTime: string; closeTime: string }>;
}

export interface StoreTablePayload {
  tableNo: string;
  tableType: string;
  capacity: number;
  status?: number;
}

export const storeApi = {
  list: (): Promise<StoreInfo[]> => http<StoreInfo[]>('/admin/store/list'),
  detail: (id: string): Promise<StoreInfo> => http<StoreInfo>(`/admin/store/${id}`),
  update: (id: string, payload: StoreUpdatePayload): Promise<void> =>
    http<void>(`/admin/store/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  createTable: (id: string, payload: StoreTablePayload): Promise<string> =>
    http<string>(`/admin/store/${id}/tables`, { method: 'POST', body: JSON.stringify(payload) }),
  updateTable: (id: string, tableId: string, payload: StoreTablePayload): Promise<string> =>
    http<string>(`/admin/store/${id}/tables/${tableId}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    }),
  removeTable: (id: string, tableId: string): Promise<void> =>
    http<void>(`/admin/store/${id}/tables/${tableId}`, { method: 'DELETE' }),
};
