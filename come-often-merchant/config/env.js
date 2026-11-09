/**
 * 接口环境配置与商家端运行参数，默认 dev
 * @author come-often
 * @date 2026-11-09
 */
const ENV_CONFIG = {
  dev: {
    apiBaseUrl: 'http://127.0.0.1:8080/api/merchant',
  },
  test: {
    apiBaseUrl: 'http://127.0.0.1:8081/api/merchant',
  },
};

export const CURRENT_ENV = 'dev';
export const API_BASE_URL = ENV_CONFIG[CURRENT_ENV].apiBaseUrl;

/** 商家端「新订单提醒」订阅消息模板；留空时后端降级为落 message_record + 日志。 */
export const SUBSCRIBE_TEMPLATE_ID = '';

/** 前台轮询间隔（毫秒）。计划 6.2：前台靠轮询 + 震动，退到后台只能靠订阅消息。 */
export const ORDER_POLL_INTERVAL_MS = 10000;

/** 列表分页大小，接口上限 50。 */
export const PAGE_SIZE = 20;
