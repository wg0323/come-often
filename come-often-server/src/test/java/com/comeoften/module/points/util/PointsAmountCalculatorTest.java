package com.comeoften.module.points.util;

import com.comeoften.common.BizException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointsAmountCalculatorTest {

    private static final int EXCHANGE_RATE = 100;

    @Test
    void should_convert_points_to_yuan_when_exchange_rate_is_100() {
        assertThat(PointsAmountCalculator.amount(500, EXCHANGE_RATE)).isEqualByComparingTo("5.00");
    }

    @Test
    void should_round_down_when_points_cannot_fill_one_cent() {
        assertThat(PointsAmountCalculator.amount(150, EXCHANGE_RATE)).isEqualByComparingTo("1.50");
        assertThat(PointsAmountCalculator.amount(99, EXCHANGE_RATE)).isEqualByComparingTo("0.99");
    }

    @Test
    void should_return_zero_amount_when_no_points_used() {
        assertThat(PointsAmountCalculator.amount(0, EXCHANGE_RATE)).isEqualByComparingTo("0.00");
    }

    @Test
    void should_cap_usable_points_when_amount_limit_is_lower_than_balance() {
        assertThat(PointsAmountCalculator.maxUsablePoints(10000, new BigDecimal("12.00"), EXCHANGE_RATE))
                .isEqualTo(1199);
    }

    @Test
    void should_cap_usable_points_by_balance_when_balance_is_lower() {
        assertThat(PointsAmountCalculator.maxUsablePoints(300, new BigDecimal("12.00"), EXCHANGE_RATE))
                .isEqualTo(300);
    }

    @Test
    void should_return_zero_when_after_discount_amount_leaves_no_room() {
        assertThat(PointsAmountCalculator.maxUsablePoints(10000, new BigDecimal("0.01"), EXCHANGE_RATE))
                .isZero();
        assertThat(PointsAmountCalculator.maxUsablePoints(0, new BigDecimal("88.00"), EXCHANGE_RATE))
                .isZero();
    }

    @Test
    void should_keep_one_cent_payable_when_requesting_all_points() {
        BigDecimal afterDiscount = new BigDecimal("1.00");
        int used = PointsAmountCalculator.resolveUsedPoints(10000, 10000, afterDiscount, EXCHANGE_RATE);
        BigDecimal pay = afterDiscount.subtract(PointsAmountCalculator.amount(used, EXCHANGE_RATE));
        assertThat(pay).isEqualByComparingTo("0.01");
    }

    @Test
    void should_reject_when_requested_points_exceed_balance() {
        assertThatThrownBy(() -> PointsAmountCalculator.resolveUsedPoints(500, 100,
                new BigDecimal("100.00"), EXCHANGE_RATE))
                .isInstanceOf(BizException.class);
    }

    @Test
    void should_use_requested_points_when_within_limits() {
        assertThat(PointsAmountCalculator.resolveUsedPoints(200, 10000, new BigDecimal("100.00"),
                EXCHANGE_RATE)).isEqualTo(200);
    }

    @Test
    void should_reject_when_order_has_no_room_left_for_points() {
        assertThatThrownBy(() -> PointsAmountCalculator.resolveUsedPoints(10, 100,
                new BigDecimal("0.01"), EXCHANGE_RATE))
                .isInstanceOf(BizException.class);
    }

    @Test
    void should_return_zero_used_points_when_request_is_absent() {
        assertThat(PointsAmountCalculator.resolveUsedPoints(null, 10000, new BigDecimal("100.00"),
                EXCHANGE_RATE)).isZero();
    }

    @Test
    void should_earn_one_point_per_yuan_when_rate_is_one() {
        assertThat(PointsAmountCalculator.earnPoints(new BigDecimal("136.80"), 1)).isEqualTo(136);
    }

    @Test
    void should_earn_no_points_when_pay_amount_is_not_positive() {
        assertThat(PointsAmountCalculator.earnPoints(BigDecimal.ZERO, 1)).isZero();
        assertThat(PointsAmountCalculator.earnPoints(null, 1)).isZero();
    }

    @Test
    void should_reject_when_exchange_rate_is_not_positive() {
        assertThatThrownBy(() -> PointsAmountCalculator.amount(100, 0))
                .isInstanceOf(BizException.class);
    }
}
