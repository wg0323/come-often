package com.comeoften.module.merchant.util;

import com.comeoften.module.dish.util.DishConstants;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoldOutStockResolverTest {

    @Test
    void should_keep_total_when_marking_sold_out() {
        assertThat(SoldOutStockResolver.resolveTotal(Boolean.TRUE, 30, true)).isNull();
    }

    @Test
    void should_restore_current_total_when_selling_again() {
        assertThat(SoldOutStockResolver.resolveTotal(Boolean.TRUE, 30, false)).isEqualTo(30);
    }

    @Test
    void should_use_default_total_when_stock_is_not_configured() {
        assertThat(SoldOutStockResolver.resolveTotal(Boolean.FALSE, 0, false))
                .isEqualTo(DishConstants.DEFAULT_STOCK_TOTAL);
        assertThat(SoldOutStockResolver.resolveTotal(null, null, false))
                .isEqualTo(DishConstants.DEFAULT_STOCK_TOTAL);
    }

    @Test
    void should_use_default_total_when_configured_total_is_zero() {
        assertThat(SoldOutStockResolver.resolveTotal(Boolean.TRUE, 0, false))
                .isEqualTo(DishConstants.DEFAULT_STOCK_TOTAL);
    }
}
