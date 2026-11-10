package com.comeoften.module.order.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderAmountCalculatorTest {

    @Test
    void should_charge_200_deposit_when_preorder_total_exceeds_200() {
        assertThat(OrderAmountCalculator.deposit(new BigDecimal("238.00"), true))
                .isEqualByComparingTo("200.00");
    }

    @Test
    void should_charge_only_order_total_when_preorder_total_below_200() {
        assertThat(OrderAmountCalculator.deposit(new BigDecimal("128.00"), true))
                .isEqualByComparingTo("128.00");
    }

    @Test
    void should_charge_no_deposit_when_order_has_no_preorder_dish() {
        assertThat(OrderAmountCalculator.deposit(new BigDecimal("238.00"), false))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void should_not_add_deposit_on_top_of_total_when_computing_pay_amount() {
        BigDecimal total = new BigDecimal("238.00");
        BigDecimal pay = OrderAmountCalculator.pay(total, BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(pay).isEqualByComparingTo("238.00");
        BigDecimal deposit = OrderAmountCalculator.deposit(total, true);
        assertThat(OrderAmountCalculator.balance(pay, deposit)).isEqualByComparingTo("38.00");
    }

    @Test
    void should_subtract_discount_and_points_when_computing_pay_amount() {
        assertThat(OrderAmountCalculator.pay(new BigDecimal("100.00"),
                new BigDecimal("10.00"), new BigDecimal("5.50")))
                .isEqualByComparingTo("84.50");
    }

    @Test
    void should_scale_to_two_decimals_when_amount_has_more_precision() {
        assertThat(OrderAmountCalculator.scale(new BigDecimal("12.345")))
                .isEqualByComparingTo("12.35");
    }

    @Test
    void should_charge_deposit_only_when_preorder_order_is_paid_online() {
        assertThat(OrderAmountCalculator.firstPayment(new BigDecimal("238.00"), new BigDecimal("200.00")))
                .isEqualByComparingTo("200.00");
    }

    @Test
    void should_charge_full_pay_amount_when_order_has_no_deposit() {
        assertThat(OrderAmountCalculator.firstPayment(new BigDecimal("136.00"), BigDecimal.ZERO))
                .isEqualByComparingTo("136.00");
    }

    @Test
    void should_cap_first_payment_at_pay_amount_when_total_is_below_200() {
        assertThat(OrderAmountCalculator.firstPayment(new BigDecimal("128.00"), new BigDecimal("200.00")))
                .isEqualByComparingTo("128.00");
    }

    @Test
    void should_reject_when_discount_exceeds_total() {
        assertThatThrownBy(() -> OrderAmountCalculator.pay(new BigDecimal("10.00"),
                new BigDecimal("20.00"), BigDecimal.ZERO))
                .isInstanceOf(BizException.class);
    }

    @Test
    void should_subtract_member_discount_and_balance_when_computing_pay_amount() {
        assertThat(OrderAmountCalculator.pay(new BigDecimal("100.00"), new BigDecimal("10.00"),
                new BigDecimal("5.50"), new BigDecimal("8.00"), new BigDecimal("20.00")))
                .isEqualByComparingTo("56.50");
    }

    @Test
    void should_keep_legacy_amount_when_new_deductions_are_zero() {
        assertThat(OrderAmountCalculator.pay(new BigDecimal("100.00"), new BigDecimal("10.00"),
                new BigDecimal("5.50"), BigDecimal.ZERO, BigDecimal.ZERO))
                .isEqualByComparingTo("84.50");
    }

    @Test
    void should_subtract_only_balance_when_member_discount_is_zero() {
        assertThat(OrderAmountCalculator.pay(new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00")))
                .isEqualByComparingTo("80.00");
    }

    @Test
    void should_subtract_only_member_discount_when_balance_is_zero() {
        assertThat(OrderAmountCalculator.pay(new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("8.00"), BigDecimal.ZERO))
                .isEqualByComparingTo("92.00");
    }

    @Test
    void should_treat_null_member_discount_and_balance_as_zero() {
        assertThat(OrderAmountCalculator.pay(new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, null, null))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void should_reject_when_member_discount_and_balance_exceed_total() {
        assertThatThrownBy(() -> OrderAmountCalculator.pay(new BigDecimal("50.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("30.00"), new BigDecimal("25.00")))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    void should_reject_when_a_deduction_component_is_negative() {
        assertThatThrownBy(() -> OrderAmountCalculator.pay(new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("-1.00"), BigDecimal.ZERO))
                .isInstanceOf(BizException.class)
                .extracting(throwable -> ((BizException) throwable).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }
}
