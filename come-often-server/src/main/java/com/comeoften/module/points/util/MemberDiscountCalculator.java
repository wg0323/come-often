package com.comeoften.module.points.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 会员等级折扣口径：{@code 折扣额 = 商品总额 × (1 − discount_rate)}，四舍五入到分。
 *
 * <p>基数取**商品总额**而不是券后金额：先打折再用券，与顾客直觉一致，也避免「用了券折扣变小」的争议；
 * 折扣额独立落 {@code trade_order.member_discount}，不折进 {@code discount_amount}（否则污染 M5 券 ROI）。
 * 折扣率 1.00 表示无折扣，直接返回 0，不做 0.00 元的空流水式记账。</p>
 */
public final class MemberDiscountCalculator {

    private static final BigDecimal NONE = BigDecimal.ONE;

    private MemberDiscountCalculator() {
    }

    public static BigDecimal amountOf(BigDecimal totalAmount, BigDecimal discountRate) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (totalAmount == null || totalAmount.signum() <= 0 || discountRate == null) {
            return zero;
        }
        if (discountRate.signum() <= 0 || discountRate.compareTo(NONE) > 0) {
            throw new BizException(ErrorCode.MEMBER_DISCOUNT_INVALID,
                    "会员折扣率必须在 0 ~ 1 之间，当前为 " + discountRate.toPlainString());
        }
        if (discountRate.compareTo(NONE) == 0) {
            return zero;
        }
        return totalAmount.multiply(NONE.subtract(discountRate))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
