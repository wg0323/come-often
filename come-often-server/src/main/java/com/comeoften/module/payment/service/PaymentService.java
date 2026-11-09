package com.comeoften.module.payment.service;

import com.comeoften.module.order.dto.OrderPayDTO;
import com.comeoften.module.order.vo.OrderVO;

import java.util.Map;

/**
 * 支付编排。订单状态、payment_record 流水与 MQ 消息只在这里落地，渠道实现不碰业务规则。
 */
public interface PaymentService {

    /**
     * 顾客端发起支付。Mock 网关同步确认收款后走同一条入账回调；V3 网关返回预支付参数等异步通知。
     */
    OrderVO prepay(Long memberId, Long storeId, String orderNo, OrderPayDTO dto);

    /**
     * Mock 手动触发支付成功回调（/api/mock/pay/success）。金额由服务端订单快照给出，调用方不传金额。
     */
    OrderVO simulateSuccess(String orderNo);

    /**
     * 渠道异步回调（/api/pay/notify）：先验签再解析，最后走同一条入账回调。
     */
    OrderVO handleNotify(String body, Map<String, String> headers);
}
