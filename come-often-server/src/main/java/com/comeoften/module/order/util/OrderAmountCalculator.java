package com.comeoften.module.order.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.dish.util.DishConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单金额口径。商品总额由 /api/app/dish/quote（下单主流程复用 DishOrderService.check）给出，
 * 这里只做优惠、定金与实付的汇总；定金是整单金额的一部分，不额外加收。
 */
public final class OrderAmountCalculator {

    private static final int SCALE = 2;

    private OrderAmountCalculator() {
    }

    /**
     * 预定菜整单定金：min(200, 整单金额)；非预定单为 0。与 M2 的 quote / order-check 同口径。
     */
    public static BigDecimal deposit(BigDecimal totalAmount, boolean containsPreorder) {
        requireAmount(totalAmount);
        if (!containsPreorder) {
            return scale(BigDecimal.ZERO);
        }
        return scale(DishConstants.PREORDER_DEPOSIT.min(totalAmount));
    }

    /**
     * 实付金额 = 商品总额 − 优惠 − 积分抵现。定金属于实付的一部分，不重复扣减也不额外加收。
     */
    public static BigDecimal pay(BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal pointsAmount) {
        requireAmount(totalAmount);
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal points = pointsAmount == null ? BigDecimal.ZERO : pointsAmount;
        BigDecimal result = totalAmount.subtract(discount).subtract(points);
        if (result.signum() < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "优惠金额不能超过商品总额");
        }
        return scale(result);
    }

    /**
     * 到店待付尾款 = 实付金额 − 已付定金。
     */
    public static BigDecimal balance(BigDecimal payAmount, BigDecimal depositAmount) {
        requireAmount(payAmount);
        BigDecimal deposit = depositAmount == null ? BigDecimal.ZERO : depositAmount;
        return scale(payAmount.subtract(deposit).max(BigDecimal.ZERO));
    }

    /**
     * 首次在线支付金额：含预定菜只收定金（min(200, 整单)），普通单收整单实付。
     * 定金订单的尾款到店付，从未在线收取，因此退款上限只能取已收金额而非 payAmount。
     */
    public static BigDecimal firstPayment(BigDecimal payAmount, BigDecimal depositAmount) {
        requireAmount(payAmount);
        BigDecimal deposit = depositAmount == null ? BigDecimal.ZERO : depositAmount;
        return deposit.signum() > 0 ? scale(deposit.min(payAmount)) : scale(payAmount);
    }

    public static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP)
                : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static void requireAmount(BigDecimal value) {
        if (value == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "金额不能为空");
        }
    }
}
