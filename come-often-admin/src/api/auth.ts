import { http } from './request';

export interface LoginResult {
  token: string;
  expiresIn: number;
  clientType: string;
  userId: string;
  storeId: string | null;
}

export interface UserInfo {
  userId: string;
  username: string;
  realName: string;
  storeId: string;
  roles: string[];
  permissions: string[];
}

export interface MenuItem {
  id: string;
  parentId: string;
  name: string;
  path: string;
  perm: string;
  type: number;
  sort: number;
}

export const authApi = {
  login: (username: string, password: string): Promise<LoginResult> =>
    http<LoginResult>('/admin/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
      skipAuth: true,
    }),
  info: (): Promise<UserInfo> => http<UserInfo>('/admin/auth/info'),
  menus: (): Promise<MenuItem[]> => http<MenuItem[]>('/admin/auth/menus'),
  refresh: (): Promise<LoginResult> => http<LoginResult>('/admin/auth/refresh', { method: 'POST' }),
  logout: (): Promise<void> => http<void>('/admin/auth/logout', { method: 'POST' }),
};
