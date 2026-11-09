/**
 * 接口环境配置，默认 dev
 * @author come-often
 * @date 2026-11-08
 */
const ENV_CONFIG = {
  dev: {
    apiBaseUrl: 'http://127.0.0.1:8080/api/app',
    fileBaseUrl: 'http://127.0.0.1:8080/api/public/file',
  },
  test: {
    apiBaseUrl: 'http://127.0.0.1:8081/api/app',
    fileBaseUrl: 'http://127.0.0.1:8081/api/public/file',
  },
};

export const CURRENT_ENV = 'dev';
export const API_BASE_URL = ENV_CONFIG[CURRENT_ENV].apiBaseUrl;
export const FILE_BASE_URL = ENV_CONFIG[CURRENT_ENV].fileBaseUrl;
