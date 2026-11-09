import { http } from './request';

const toQuery = (params: object): string => {
  const pairs = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== '')
    .map(([key, value]) => `${key}=${encodeURIComponent(String(value))}`);
  return pairs.length === 0 ? '' : `?${pairs.join('&')}`;
};

export interface ReservationStatusLog {
  fromStatus: string;
  fromStatusText: string;
  toStatus: string;
  toStatusText: string;
  operatorId: string | null;
  remark: string;
  createTime: string;
}

export interface ReservationItem {
  id: string;
  reserveNo: string;
  storeId: string;
  storeName: string | null;
  reserveDate: string;
  timeSlot: string;
  tableType: string;
  tableTypeText: string;
  peopleCount: number;
  contactName: string;
  contactPhone: string;
  remark: string;
  status: string;
  statusText: string;
  tableId: string | null;
  tableNo: string | null;
  checkinUserId: string | null;
  checkinTime: string | null;
  reserveAt: string;
  createTime: string;
  memberNickname: string | null;
  memberPhone: string | null;
  canCancel: boolean;
  canCheckin: boolean;
  logs: ReservationStatusLog[] | null;
}

export interface ReservationSlotItem {
  slotId: string;
  reserveDate: string;
  timeSlot: string;
  tableType: string;
  tableTypeText: string;
  capacity: number;
  booked: number;
  remaining: number;
  full: boolean;
}

export interface ReservationCalendarDay {
  date: string;
  total: number;
  pending: number;
  confirmed: number;
  checkedIn: number;
  noShow: number;
  cancelled: number;
  capacity: number;
  booked: number;
}

export interface TableBoardItem {
  tableId: string;
  tableNo: string;
  tableType: string;
  tableTypeText: string;
  capacity: number;
  status: number;
  statusText: string;
  reservationCount: number;
  nextTimeSlot: string | null;
}

export interface ReservationTableBoard {
  date: string;
  tables: TableBoardItem[];
  slots: ReservationSlotItem[];
  reservations: ReservationItem[];
}

export interface PageResult<T> {
  total: number;
  current: number;
  size: number;
  records: T[];
}

export interface ReservationPageQuery {
  date?: string;
  status?: string;
  keyword?: string;
  current?: number;
  size?: number;
}

export const reservationApi = {
  page: (params: ReservationPageQuery): Promise<PageResult<ReservationItem>> =>
    http<PageResult<ReservationItem>>(`/admin/reservation/page${toQuery(params)}`),
  detail: (reserveNo: string): Promise<ReservationItem> =>
    http<ReservationItem>(`/admin/reservation/${reserveNo}`),
  confirm: (reserveNo: string): Promise<ReservationItem> =>
    http<ReservationItem>(`/admin/reservation/${reserveNo}/confirm`, { method: 'POST' }),
  reject: (reserveNo: string, remark: string): Promise<ReservationItem> =>
    http<ReservationItem>(`/admin/reservation/${reserveNo}/reject`, {
      method: 'POST',
      body: JSON.stringify({ remark }),
    }),
  checkin: (reserveNo: string, tableId?: string): Promise<ReservationItem> =>
    http<ReservationItem>(
      `/admin/reservation/${reserveNo}/checkin${tableId ? `?tableId=${tableId}` : ''}`,
      { method: 'POST' },
    ),
  checkinByCode: (code: string): Promise<ReservationItem> =>
    http<ReservationItem>('/admin/reservation/checkin-by-code', {
      method: 'POST',
      body: JSON.stringify({ code }),
    }),
  calendar: (startDate: string, endDate: string): Promise<ReservationCalendarDay[]> =>
    http<ReservationCalendarDay[]>(
      `/admin/reservation/calendar?startDate=${startDate}&endDate=${endDate}`,
    ),
  tableBoard: (date: string): Promise<ReservationTableBoard> =>
    http<ReservationTableBoard>(`/admin/reservation/table-board?date=${date}`),
  slots: (date: string): Promise<ReservationSlotItem[]> =>
    http<ReservationSlotItem[]>(`/admin/reservation/slot?date=${date}`),
  generate: (days?: number): Promise<number> =>
    http<number>(`/admin/reservation/slot/generate${days ? `?days=${days}` : ''}`, {
      method: 'POST',
    }),
  rebuild: (): Promise<number> =>
    http<number>('/admin/reservation/slot/rebuild', { method: 'POST' }),
};
