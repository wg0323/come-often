import { http } from './request';

const toQuery = (params: object): string => {
  const pairs = Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== '')
    .map(([key, value]) => `${key}=${encodeURIComponent(String(value))}`);
  return pairs.length === 0 ? '' : `?${pairs.join('&')}`;
};

export interface QueueTicketItem {
  ticketNo: string;
  partySize: number;
  status: string;
  statusText: string;
  waitingCount: number;
  createTime: string;
  calledTime: string | null;
  bizDate: string;
  memberPhone: string | null;
}

export interface PageResult<T> {
  total: number;
  current: number;
  size: number;
  records: T[];
}

export interface QueuePageQuery {
  status?: string;
  current?: number;
  size?: number;
}

export const queueApi = {
  page: (params: QueuePageQuery): Promise<PageResult<QueueTicketItem>> =>
    http<PageResult<QueueTicketItem>>(`/admin/queue/page${toQuery(params)}`),
  call: (ticketNo: string): Promise<QueueTicketItem> =>
    http<QueueTicketItem>(`/admin/queue/${ticketNo}/call`, { method: 'POST' }),
  seat: (ticketNo: string): Promise<QueueTicketItem> =>
    http<QueueTicketItem>(`/admin/queue/${ticketNo}/seat`, { method: 'POST' }),
};
