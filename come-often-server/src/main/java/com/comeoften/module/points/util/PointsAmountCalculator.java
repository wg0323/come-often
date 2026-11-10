package com.comeoften.module.points.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 积分与金额的换算口径，全项目只在这里定义一次。
 *
 * <p>抵现方向：{@code 金额 = 积分 / 兑换比例}，向下取到分；上限受「券后金额至少留 0.01 元」约束，
 * 不允许把订单抵到 0 元（支付金额为 0 会让支付网关与定金口径同时失效）。</p>
 *
 * <p>获得方向：{@code 积分 = 实付金额 × 每元积分}，向下取整，避免出现小数积分。</p>
 */
public final class PointsAmountCalculator {

    private static final int AMOUNT_SCALE = 2;

    private static final BigDecimal MIN_PAY_AMOUNT = new BigDecimal("0.01");

    private PointsAmountCalculator() {
    }

    /**
     * 积分抵现金额，单位元，保留两位小数并向下取整。
     */
    public static BigDecimal amount(int points, int exchangeRate) {
        requireRate(exchangeRate);
        if (points <= 0) {
            return BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.DOWN);
        }
        return BigDecimal.valueOf(points)
                .divide(BigDecimal.valueOf(exchangeRate), AMOUNT_SCALE, RoundingMode.DOWN);
    }

    /**
     * 当前订单可用的最大抵扣积分：取余额与「券后金额扣掉 0.01 元」两者较小值。
     */
    public static int maxUsablePoints(int balance, BigDecimal afterDiscountAmount, int exchangeRate) {
        requireRate(exchangeRate);
        if (balance <= 0 || afterDiscountAmount == null) {
            return 0;
        }
        BigDecimal limitAmount = afterDiscountAmount.subtract(MIN_PAY_AMOUNT);
        if (limitAmount.signum() <= 0) {
            return 0;
        }
        int byAmount = limitAmount.multiply(BigDecimal.valueOf(exchangeRate))
                .setScale(0, RoundingMode.DOWN).intValue();
        return Math.min(balance, byAmount);
    }

    /**
     * 解析本次实际使用积分：超过余额直接拒绝；超过可抵金额上限时按上限收敛，
     * 让确认页的「全部抵现」在券后金额变小时仍然安全。
     */
    public static int resolveUsedPoints(Integer requested, int balance, BigDecimal afterDiscountAmount,
                                        int exchangeRate) {
        if (requested == null || requested <= 0) {
            return 0;
        }
        if (requested > balance) {
            throw new BizException(ErrorCode.POINTS_BALANCE_NOT_ENOUGH,
                    "可用积分 " + balance + " 分，不足以抵扣 " + requested + " 分");
        }
        int maxUsable = maxUsablePoints(balance, afterDiscountAmount, exchangeRate);
        if (maxUsable <= 0) {
            // 券已经把订单压到 0.01 元以内，此时再要求抵现没有可抵扣空间
            throw new BizException(ErrorCode.POINTS_USE_AMOUNT_INVALID,
                    "本单已无可抵扣金额，请取消积分抵现后重试");
        }
        return Math.min(requested, maxUsable);
    }

    /**
     * 积分兑换菜品所需积分：{@code 金额 × 每元积分}，向上取整。
     * 向上取整而不是向下，避免 0.4 分的零头白送；兑换订单金额通常很小，差 1 分可接受。
     */
    public static int pointsFor(BigDecimal amount, int exchangeRate) {
        requireRate(exchangeRate);
        if (amount == null || amount.signum() <= 0) {
            return 0;
        }
        return amount.multiply(BigDecimal.valueOf(exchangeRate))
                .setScale(0, RoundingMode.UP).intValue();
    }

    /**
     * 消费获得积分：实付金额 × 每元积分，向下取整。
     */
    public static int earnPoints(BigDecimal payAmount, int earnRate) {
        if (payAmount == null || payAmount.signum() <= 0 || earnRate <= 0) {
            return 0;
        }
        return payAmount.multiply(BigDecimal.valueOf(earnRate))
                .setScale(0, RoundingMode.DOWN).intValue();
    }

    private static void requireRate(int exchangeRate) {
        if (exchangeRate <= 0) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "积分兑换比例必须大于 0");
        }
    }
}
