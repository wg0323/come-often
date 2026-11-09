package com.comeoften.module.merchant.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.order.vo.OrderSummaryVO;
import com.comeoften.module.order.vo.OrderVO;
import com.comeoften.module.payment.vo.RefundVO;

/**
 * 商家端订单看板。只暴露接单 / 出餐 / 确认取餐三步（PAID→ACCEPTED→READY→COMPLETED），
 * 取消与退款不在看板上放开：退单统一走 {@link #refund}，规则由 M4 的状态机决定。
 */
public interface MerchantOrderService {

    PageResult<OrderSummaryVO> page(Long storeId, Long current, Long size, String status);

    OrderVO detail(Long storeId, String orderNo);

    /**
     * 推进订单状态。目标状态只允许 ACCEPTED / READY / COMPLETED，其余返回 30002。
     */
    OrderVO advance(Long storeId, String orderNo, String toStatus, String remark, Long operatorId);

    /**
     * 商家发起退单。制作前（PAID）由 M4 自动审核通过，制作中 / 待取餐进 REFUNDING 等 PC 审核。
     */
    RefundVO refund(Long storeId, String orderNo, String reason, Long operatorId);
}
