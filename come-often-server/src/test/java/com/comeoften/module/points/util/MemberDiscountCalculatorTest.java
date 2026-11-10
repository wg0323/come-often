package com.comeoften.module.points.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 会员折扣口径：商品总额 × (1 − 折扣率)，四舍五入到分；1.00 视为无折扣。
 */
class MemberDiscountCalculatorTest {

    @Test
    void should_discount_five_percent_when_level_rate_is_0_95() {
        assertThat(MemberDiscountCalculator.amountOf(new BigDecimal("100.00"), new BigDecimal("0.95")))
                .isEqualByComparingTo("5.00");
    }

    @Test
    void should_round_to_cent_when_discount_is_not_exact() {
        // 136.80 × 0.05 = 6.84，正好两位小数；33.33 × 0.10 = 3.333 → 3.33
        assertThat(MemberDiscountCalculator.amountOf(new BigDecimal("136.80"), new BigDecimal("0.95")))
                .isEqualByComparingTo("6.84");
        assertThat(MemberDiscountCalculator.amountOf(new BigDecimal("33.33"), new BigDecimal("0.90")))
                .isEqualByComparingTo("3.33");
    }

    @Test
    void should_return_zero_when_level_has_no_discount() {
        assertThat(MemberDiscountCalculator.amountOf(new BigDecimal("88.00"), BigDecimal.ONE))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void should_return_zero_when_amount_or_rate_is_absent() {
        assertThat(MemberDiscountCalculator.amountOf(null, new BigDecimal("0.95")))
                .isEqualByComparingTo("0.00");
        assertThat(MemberDiscountCalculator.amountOf(new BigDecimal("88.00"), null))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void should_reject_rate_above_one_with_member_discount_code() {
        assertThatThrownBy(() ->
                MemberDiscountCalculator.amountOf(new BigDecimal("88.00"), new BigDecimal("1.20")))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_DISCOUNT_INVALID);
    }

    @Test
    void should_reject_zero_rate_instead_of_free_order() {
        assertThatThrownBy(() ->
                MemberDiscountCalculator.amountOf(new BigDecimal("88.00"), BigDecimal.ZERO))
                .isInstanceOf(BizException.class)
                .extracting(exception -> ((BizException) exception).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_DISCOUNT_INVALID);
    }
}
