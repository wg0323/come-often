package com.comeoften.module.stats.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 报表金额与比率换算。金额一律两位小数、四舍五入；分母为零返回 null，
 * 由前端显示为「—」，绝不返回 0 假装有值。
 */
public final class StatsAmountUtils {

    private static final int AMOUNT_SCALE = 2;

    private StatsAmountUtils() {
    }

    public static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 统一两位小数，避免 0E-8、1234.500000 这类值直接出现在响应体里。
     */
    public static BigDecimal scale(BigDecimal value) {
        return zeroIfNull(value).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 净额 = 已支付 − 已退款。
     */
    public static BigDecimal net(BigDecimal paid, BigDecimal refund) {
        return scale(zeroIfNull(paid).subtract(zeroIfNull(refund)));
    }

    /**
     * 客单价 = 净营收 / 有效订单数；订单数为零返回 null。
     */
    public static BigDecimal average(BigDecimal netRevenue, long orderCount) {
        if (orderCount <= 0L) {
            return null;
        }
        return netRevenue.divide(BigDecimal.valueOf(orderCount), AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 比率百分比，保留两位；分母为零返回 null。
     */
    public static BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0L) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100L))
                .divide(BigDecimal.valueOf(denominator), AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 投产比 = 带动金额 / 成本；成本非正数返回 null（成本为 0 时比值无意义）。
     */
    public static BigDecimal ratio(BigDecimal revenue, BigDecimal cost) {
        BigDecimal divisor = zeroIfNull(cost);
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return zeroIfNull(revenue).divide(divisor, AMOUNT_SCALE, RoundingMode.HALF_UP);
    }
}
