package com.comeoften.module.payment.util;

import com.comeoften.module.order.util.OrderAmountCalculator;

import java.math.BigDecimal;

/**
 * 退款金额口径：只退实际收到的钱。
 *
 * <p>含预定菜的订单在线只收 200 元定金（整单不足 200 时按整单收），尾款到店付、从未在线收取，
 * 因此退款上限取 payment_record.pay_amount，绝不取 trade_order.pay_amount，否则会把未收的尾款退出去。</p>
 */
public final class RefundAmountResolver {

    private RefundAmountResolver() {
    }

    public static BigDecimal refundable(BigDecimal paidAmount, BigDecimal alreadyRefunded) {
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal refunded = alreadyRefunded == null ? BigDecimal.ZERO : alreadyRefunded;
        return OrderAmountCalculator.scale(paid.subtract(refunded).max(BigDecimal.ZERO));
    }
}
