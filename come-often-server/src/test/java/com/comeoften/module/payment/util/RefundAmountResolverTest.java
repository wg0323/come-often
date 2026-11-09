package com.comeoften.module.payment.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RefundAmountResolverTest {

    @Test
    void should_refund_full_paid_amount_when_order_has_no_deposit() {
        assertThat(RefundAmountResolver.refundable(new BigDecimal("136.00"), BigDecimal.ZERO))
                .isEqualByComparingTo("136.00");
    }

    @Test
    void should_refund_only_paid_deposit_when_order_contains_preorder_dish() {
        // 整单 238，线上只收 200 定金，尾款 38 到店付、从未收取，不能退出去
        assertThat(RefundAmountResolver.refundable(new BigDecimal("200.00"), BigDecimal.ZERO))
                .isEqualByComparingTo("200.00");
        assertThat(RefundAmountResolver.refundable(new BigDecimal("200.00"), BigDecimal.ZERO))
                .isNotEqualByComparingTo("238.00");
    }

    @Test
    void should_return_zero_when_amount_was_already_refunded() {
        assertThat(RefundAmountResolver.refundable(new BigDecimal("200.00"), new BigDecimal("200.00")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void should_return_remaining_amount_when_partially_refunded_before() {
        assertThat(RefundAmountResolver.refundable(new BigDecimal("200.00"), new BigDecimal("50.00")))
                .isEqualByComparingTo("150.00");
    }

    @Test
    void should_never_return_negative_when_refunded_amount_exceeds_paid_amount() {
        assertThat(RefundAmountResolver.refundable(new BigDecimal("68.00"), new BigDecimal("100.00")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void should_treat_null_amounts_as_zero() {
        assertThat(RefundAmountResolver.refundable(null, null)).isEqualByComparingTo("0.00");
    }
}
