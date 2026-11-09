package com.comeoften.module.dish.util;

import com.comeoften.common.BizException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * 金额计算纯逻辑测试：基础价 + 规格加价 + 加料加价，再乘份数。
 */
class DishPriceCalculatorTest {

    @Test
    void should_sum_spec_and_addon_deltas_when_both_selected() {
        BigDecimal result = DishPriceCalculator.quote(new BigDecimal("30.00"),
                Arrays.asList(new BigDecimal("10.00"), new BigDecimal("2.00")),
                Collections.singletonList(new BigDecimal("3.00")), 2);
        assertThat(result).isEqualByComparingTo("90.00");
    }

    @Test
    void should_return_base_price_when_no_option_selected() {
        BigDecimal result = DishPriceCalculator.quote(new BigDecimal("28.00"), null, null, 1);
        assertThat(result).isEqualByComparingTo("28.00");
    }

    @Test
    void should_keep_two_decimal_scale_when_delta_has_more_precision() {
        BigDecimal result = DishPriceCalculator.quote(new BigDecimal("19.99"),
                Collections.singletonList(new BigDecimal("0.005")), null, 3);
        assertThat(result).isEqualByComparingTo("60.00");
    }

    @Test
    void should_treat_null_delta_as_zero_when_option_price_missing() {
        BigDecimal result = DishPriceCalculator.quote(new BigDecimal("48.00"),
                Arrays.asList(null, new BigDecimal("2.00")), null, 1);
        assertThat(result).isEqualByComparingTo("50.00");
    }

    @Test
    void should_reject_when_quantity_is_not_positive() {
        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> DishPriceCalculator.quote(BigDecimal.TEN, null, null, 0))
                .withMessageContaining("份数");
    }

    @Test
    void should_reject_when_base_price_is_null() {
        assertThatExceptionOfType(BizException.class)
                .isThrownBy(() -> DishPriceCalculator.quote(null, null, null, 1))
                .withMessageContaining("基础价");
    }
}
