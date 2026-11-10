package com.comeoften.module.balance.util;

import com.comeoften.config.BalanceProperties.GiftTier;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充值赠送口径：取「不超过充值金额的最高档位」的固定赠送额，不做比例累加。
 *
 * <p>例：档位 500:50 / 1000:120 / 2000:300 时，充 800 送 50，充 2000 送 300。
 * 固定额比比例更容易被顾客理解，也不会出现「充 999 送 99.9」这种奇怪数字。</p>
 */
public final class BalanceGiftCalculator {

    private BalanceGiftCalculator() {
    }

    public static BigDecimal giftOf(BigDecimal amount, List<GiftTier> tiers) {
        if (amount == null || amount.signum() <= 0 || tiers == null || tiers.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal gift = BigDecimal.ZERO;
        for (GiftTier tier : tiers) {
            if (amount.compareTo(tier.getAmount()) >= 0) {
                gift = tier.getGift();
            }
        }
        return gift.setScale(2);
    }

}
