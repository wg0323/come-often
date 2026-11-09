import { http, httpBlob } from './request';

export interface StatsSummary {
  netRevenue: number;
  refundAmount: number;
  orderCount: number;
  avgOrderAmount: number | null;
}

export interface StatsTrendPoint {
  bucketStart: string;
  label: string;
  netRevenue: number;
  refundAmount: number;
  orderCount: number;
  avgOrderAmount: number | null;
}

export interface StatsHotDish {
  rank: number;
  dishId: string;
  dishName: string;
  quantity: number;
  amount: number;
  orderCount: number;
}

export interface StatsCouponRoi {
  templateId: string;
  templateName: string;
  couponType: string;
  couponTypeText: string;
  total: number;
  receivedCount: number;
  usedCount: number;
  useRate: number | null;
  orderCount: number;
  revenueAmount: number;
  discountAmount: number;
  roi: number | null;
}

export interface StatsHighValueMember {
  rank: number;
  memberId: string;
  nickname: string;
  phoneMasked: string;
  levelName: string;
  orderCount: number;
  netAmount: number;
  lastPaidTime: string | null;
}

export interface StatsMember {
  paidMemberCount: number;
  repurchaseMemberCount: number;
  repurchaseRate: number | null;
  retentionCohortSize: number;
  retentionCount: number;
  retentionRate: number | null;
  highValueUsers: StatsHighValueMember[];
}

export interface StatsRange {
  startDate: string;
  endDate: string;
  granularity: string;
  granularityText: string;
  topN: number;
}

export interface StatsReport {
  range: StatsRange;
  summary: StatsSummary;
  trend: StatsTrendPoint[];
  hotDishes: StatsHotDish[];
  couponRoi: StatsCouponRoi[];
  member: StatsMember;
}

export interface StatsReservationSlot {
  timeSlot: string;
  capacity: number;
  booked: number;
}

export interface StatsReservationDay {
  date: string;
  total: number;
  pending: number;
  confirmed: number;
  checkedIn: number;
  noShow: number;
  cancelled: number;
  slots: StatsReservationSlot[];
}

export interface StatsDashboard {
  storeName: string;
  statDate: string;
  updatedAt: string;
  summary: StatsSummary;
  reservations: StatsReservationDay;
  hotDishes: StatsHotDish[];
}

export interface StatsQuery {
  startDate?: string;
  endDate?: string;
  granularity?: string;
  topN?: number;
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

export const statsApi = {
  dashboard: (): Promise<StatsDashboard> => http<StatsDashboard>('/admin/stats/dashboard'),
  report: (params: StatsQuery): Promise<StatsReport> =>
    http<StatsReport>(`/admin/stats/report?${toQuery({ ...params })}`),
  exportExcel: (type: string, params: StatsQuery): Promise<Blob> =>
    httpBlob(`/admin/stats/export?${toQuery({ type, ...params })}`),
};
