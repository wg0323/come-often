package com.comeoften.module.merchant.service;

import com.comeoften.module.merchant.vo.ScanResolveVO;

/**
 * 商家端统一扫码。前端只有一个扫码按钮，码型由服务端判定并直接执行：
 * 券码走 CouponVerifyService（MERCHANT_SCAN），签到码走 ReservationService.checkinByCode，
 * 取餐码走 OrderService.transition。失败不抛异常，统一返回可展示的结果 VO。
 */
public interface MerchantScanService {

    ScanResolveVO resolve(Long storeId, Long operatorUserId, String rawCode);
}
