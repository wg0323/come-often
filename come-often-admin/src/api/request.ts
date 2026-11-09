const BASE_URL = '/api';
const TOKEN_KEY = 'adminToken';

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export class ApiError extends Error {
  readonly code: number;

  readonly status: number;

  constructor(message: string, code: number, status: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

export interface HttpOptions extends RequestInit {
  skipAuth?: boolean;
}

export const getToken = (): string | null => sessionStorage.getItem(TOKEN_KEY);

export const setToken = (token: string): void => {
  sessionStorage.setItem(TOKEN_KEY, token);
};

export const clearToken = (): void => {
  sessionStorage.removeItem(TOKEN_KEY);
};

/**
 * 统一请求封装：注入后台 token、解包业务 data、401 踢回登录页。
 * 登录接口用 skipAuth，避免登录失败也被当成登录态失效。
 */
export const http = async <T>(path: string, options: HttpOptions = {}): Promise<T> => {
  const { skipAuth = false, ...init } = options;
  const headers = new Headers(init.headers);
  headers.set('Content-Type', 'application/json');
  const token = getToken();
  if (!skipAuth && token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  const response = await fetch(`${BASE_URL}${path}`, { ...init, headers });
  const body = (await response.json()) as ApiResult<T>;
  if (response.status === 401 && !skipAuth) {
    clearToken();
    window.location.assign('/login');
    throw new ApiError('登录已失效，请重新登录', 40100, 401);
  }
  if (!response.ok) {
    throw new ApiError(body?.message ?? `请求失败：${response.status}`, body?.code ?? -1, response.status);
  }
  if (body.code !== 0) {
    throw new ApiError(body.message, body.code, response.status);
  }
  return body.data;
};

/**
 * 二进制下载封装（报表 Excel）。与 http 共用 token 注入与 401 处理，
 * 后端出错时仍返回 JSON，因此按 Content-Type 判断走错误分支，不让调用方拿到坏文件。
 */
export const httpBlob = async (path: string, options: HttpOptions = {}): Promise<Blob> => {
  const { skipAuth = false, ...init } = options;
  const headers = new Headers(init.headers);
  const token = getToken();
  if (!skipAuth && token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  const response = await fetch(`${BASE_URL}${path}`, { ...init, headers });
  if (response.status === 401 && !skipAuth) {
    clearToken();
    window.location.assign('/login');
    throw new ApiError('登录已失效，请重新登录', 40100, 401);
  }
  const contentType = response.headers.get('Content-Type') ?? '';
  if (!response.ok || contentType.includes('application/json')) {
    const body = (await response.json()) as ApiResult<unknown>;
    throw new ApiError(body?.message ?? `请求失败：${response.status}`, body?.code ?? -1, response.status);
  }
  return response.blob();
};
