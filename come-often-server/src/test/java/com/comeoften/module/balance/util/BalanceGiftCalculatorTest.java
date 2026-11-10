package com.comeoften.module.balance.util;

import com.comeoften.config.BalanceProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 充值赠送口径：取不超过充值金额的最高档位，给固定赠送额。
 */
class BalanceGiftCalculatorTest {

    private static final String TIERS = "500:50,1000:120,2000:300";

    @Test
    void should_pick_highest_tier_not_exceeding_recharge_amount() {
        BalanceProperties properties = properties();
        assertThat(BalanceGiftCalculator.giftOf(new BigDecimal("500.00"), properties.giftTiers()))
                .isEqualByComparingTo("50");
        assertThat(BalanceGiftCalculator.giftOf(new BigDecimal("800.00"), properties.giftTiers()))
                .isEqualByComparingTo("50");
        assertThat(BalanceGiftCalculator.giftOf(new BigDecimal("2000.00"), properties.giftTiers()))
                .isEqualByComparingTo("300");
    }

    @Test
    void should_give_no_gift_when_amount_is_below_lowest_tier() {
        assertThat(BalanceGiftCalculator.giftOf(new BigDecimal("499.99"), properties().giftTiers()))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void should_give_no_gift_when_amount_or_tiers_absent() {
        assertThat(BalanceGiftCalculator.giftOf(null, properties().giftTiers()))
                .isEqualByComparingTo("0.00");
        assertThat(BalanceGiftCalculator.giftOf(new BigDecimal("800.00"), null))
                .isEqualByComparingTo("0.00");
    }

    private BalanceProperties properties() {
        BalanceProperties properties = new BalanceProperties();
        properties.setRechargeTiers(TIERS);
        return properties;
    }
}
