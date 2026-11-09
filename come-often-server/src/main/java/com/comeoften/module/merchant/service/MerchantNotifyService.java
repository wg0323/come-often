package com.comeoften.module.merchant.service;

import com.comeoften.module.merchant.vo.MerchantNotifyVO;

/**
 * 商家端新订单提醒。前台靠轮询 + 震动，退到后台只能靠商家端自己的订阅消息；
 * 模板未接入时降级为落 message_record + 日志，不接第三方推送、不做常驻长连接。
 */
public interface MerchantNotifyService {

    /**
     * 发送（或降级落库）一条新订单提醒。订单需处于待接单 / 制作中 / 待取餐状态。
     */
    MerchantNotifyVO remindNewOrder(Long storeId, String orderNo, Long operatorUserId);
}
