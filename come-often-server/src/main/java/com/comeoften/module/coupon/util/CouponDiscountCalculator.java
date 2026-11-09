package com.comeoften.module.coupon.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.coupon.constant.CouponType;
import com.comeoften.module.coupon.entity.CouponTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 券抵扣口径。金额唯一口径在服务端，页面与后台只展示本类算出的结果。
 *
 * <p>规则：门槛按整单金额判定；面额型最多抵到整单金额；折扣券按 discount_rate 折算；
 * 单品券只抵指定菜品小计。叠加规则：一个订单最多用一张券，由 trade_order.coupon_id 单值列约束；
 * coupon_template.stackable 只决定能否与 M6 的会员折扣同享。</p>
 */
public final class CouponDiscountCalculator {

    private static final int SCALE = 2;

    private static final BigDecimal NO_DISCOUNT_RATE = BigDecimal.ONE;

    private CouponDiscountCalculator() {
    }

    /**
     * 下单 / 确认页的抵扣额。dishAmount 为单品券指定菜品的行金额小计，非单品券忽略。
     */
    public static BigDecimal discount(CouponTemplate template, BigDecimal orderAmount, BigDecimal dishAmount) {
        requireAmount(orderAmount);
        CouponType type = CouponType.of(template.getCouponType());
        if (type == null) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND, "券类型不合法：" + template.getCouponType());
        }
        assertThreshold(template, orderAmount);
        BigDecimal base = orderAmount;
        if (type == CouponType.DISH) {
            BigDecimal dish = dishAmount == null ? BigDecimal.ZERO : dishAmount;
            if (dish.signum() <= 0) {
                throw new BizException(ErrorCode.COUPON_DISH_NOT_MATCHED);
            }
            base = dish;
            return scale(faceValue(template).min(base));
        }
        BigDecimal result;
        if (type == CouponType.DISCOUNT) {
            BigDecimal rate = template.getDiscountRate() == null ? NO_DISCOUNT_RATE : template.getDiscountRate();
            result = base.multiply(NO_DISCOUNT_RATE.subtract(rate));
        } else {
            result = faceValue(template);
        }
        BigDecimal capped = result.min(base);
        return scale(capped.max(BigDecimal.ZERO));
    }

    /**
     * 现场扫码核销时没有订单金额，只能按券面额记录抵扣；折扣券无从折算，记 0。
     */
    public static BigDecimal verifyAmount(CouponTemplate template) {
        CouponType type = CouponType.of(template.getCouponType());
        if (type == null || type == CouponType.DISCOUNT) {
            return scale(BigDecimal.ZERO);
        }
        return scale(faceValue(template));
    }

    /**
     * 门槛判定。无门槛券与生日券不受 threshold_amount 限制。
     */
    public static void assertThreshold(CouponTemplate template, BigDecimal orderAmount) {
        CouponType type = CouponType.of(template.getCouponType());
        if (type == CouponType.NO_THRESHOLD || type == CouponType.BIRTHDAY || type == CouponType.DISH) {
            return;
        }
        BigDecimal threshold = template.getThresholdAmount();
        if (threshold != null && threshold.signum() > 0 && orderAmount.compareTo(threshold) < 0) {
            throw new BizException(ErrorCode.COUPON_THRESHOLD_NOT_ENOUGH,
                    "满 " + threshold.stripTrailingZeros().toPlainString() + " 元可用");
        }
    }

    /**
     * 面额展示文案：面额型「¥20」，折扣券「8折」。
     */
    public static String faceText(CouponTemplate template) {
        CouponType type = CouponType.of(template.getCouponType());
        if (type == CouponType.DISCOUNT && template.getDiscountRate() != null) {
            return template.getDiscountRate().multiply(BigDecimal.TEN)
                    .stripTrailingZeros().toPlainString() + "折";
        }
        return "¥" + faceValue(template).stripTrailingZeros().toPlainString();
    }

    /**
     * 使用条件文案。
     */
    public static String conditionText(CouponTemplate template) {
        CouponType type = CouponType.of(template.getCouponType());
        if (type == CouponType.DISH) {
            return "限指定菜品";
        }
        BigDecimal threshold = template.getThresholdAmount();
        if (type == CouponType.NO_THRESHOLD || type == CouponType.BIRTHDAY
                || threshold == null || threshold.signum() <= 0) {
            return "无门槛";
        }
        return "满 " + threshold.stripTrailingZeros().toPlainString() + " 元可用";
    }

    private static BigDecimal faceValue(CouponTemplate template) {
        BigDecimal value = template.getFaceValue();
        return value == null ? BigDecimal.ZERO : value;
    }

    private static void requireAmount(BigDecimal value) {
        if (value == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "金额不能为空");
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
