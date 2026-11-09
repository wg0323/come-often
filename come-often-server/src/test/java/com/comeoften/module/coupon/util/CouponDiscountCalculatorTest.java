package com.comeoften.module.coupon.util;

import com.comeoften.common.BizException;
import com.comeoften.module.coupon.constant.CouponType;
import com.comeoften.module.coupon.entity.CouponTemplate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponDiscountCalculatorTest {

    @Test
    void should_deduct_face_value_when_full_reduction_threshold_reached() {
        CouponTemplate template = template(CouponType.FULL_REDUCTION, "20.00", null, "100.00");
        assertThat(CouponDiscountCalculator.discount(template, new BigDecimal("238.00"), BigDecimal.ZERO))
                .isEqualByComparingTo("20.00");
    }

    @Test
    void should_reject_when_order_amount_below_threshold() {
        CouponTemplate template = template(CouponType.FULL_REDUCTION, "20.00", null, "100.00");
        assertThatThrownBy(() -> CouponDiscountCalculator.discount(template,
                new BigDecimal("88.00"), BigDecimal.ZERO))
                .isInstanceOf(BizException.class);
    }

    @Test
    void should_ignore_threshold_when_coupon_has_no_threshold() {
        CouponTemplate template = template(CouponType.NO_THRESHOLD, "10.00", null, "100.00");
        assertThat(CouponDiscountCalculator.discount(template, new BigDecimal("28.00"), BigDecimal.ZERO))
                .isEqualByComparingTo("10.00");
    }

    @Test
    void should_never_deduct_more_than_order_amount() {
        CouponTemplate template = template(CouponType.NO_THRESHOLD, "50.00", null, "0.00");
        assertThat(CouponDiscountCalculator.discount(template, new BigDecimal("18.00"), BigDecimal.ZERO))
                .isEqualByComparingTo("18.00");
    }

    @Test
    void should_compute_discount_from_rate_when_coupon_is_discount_type() {
        CouponTemplate template = template(CouponType.DISCOUNT, "0.00", "0.80", "0.00");
        assertThat(CouponDiscountCalculator.discount(template, new BigDecimal("100.00"), BigDecimal.ZERO))
                .isEqualByComparingTo("20.00");
    }

    @Test
    void should_deduct_only_dish_subtotal_when_coupon_is_dish_type() {
        CouponTemplate template = template(CouponType.DISH, "30.00", null, "0.00");
        assertThat(CouponDiscountCalculator.discount(template, new BigDecimal("238.00"),
                new BigDecimal("128.00")))
                .isEqualByComparingTo("30.00");
        assertThat(CouponDiscountCalculator.discount(template, new BigDecimal("238.00"),
                new BigDecimal("18.00")))
                .isEqualByComparingTo("18.00");
    }

    @Test
    void should_reject_when_dish_coupon_has_no_matched_dish() {
        CouponTemplate template = template(CouponType.DISH, "30.00", null, "0.00");
        assertThatThrownBy(() -> CouponDiscountCalculator.discount(template,
                new BigDecimal("238.00"), BigDecimal.ZERO))
                .isInstanceOf(BizException.class);
    }

    @Test
    void should_record_zero_amount_when_verify_discount_coupon_without_order() {
        CouponTemplate template = template(CouponType.DISCOUNT, "0.00", "0.80", "0.00");
        assertThat(CouponDiscountCalculator.verifyAmount(template)).isEqualByComparingTo("0.00");
    }

    @Test
    void should_record_face_value_when_verify_face_value_coupon_without_order() {
        CouponTemplate template = template(CouponType.FULL_REDUCTION, "20.00", null, "100.00");
        assertThat(CouponDiscountCalculator.verifyAmount(template)).isEqualByComparingTo("20.00");
    }

    @Test
    void should_render_face_and_condition_text_for_wallet() {
        CouponTemplate reduction = template(CouponType.FULL_REDUCTION, "20.00", null, "100.00");
        assertThat(CouponDiscountCalculator.faceText(reduction)).isEqualTo("¥20");
        assertThat(CouponDiscountCalculator.conditionText(reduction)).isEqualTo("满 100 元可用");
        CouponTemplate discount = template(CouponType.DISCOUNT, "0.00", "0.80", "0.00");
        assertThat(CouponDiscountCalculator.faceText(discount)).isEqualTo("8折");
    }

    private static CouponTemplate template(CouponType type, String faceValue, String discountRate,
                                           String thresholdAmount) {
        CouponTemplate template = new CouponTemplate();
        template.setCouponType(type.name());
        template.setFaceValue(new BigDecimal(faceValue));
        template.setDiscountRate(discountRate == null ? null : new BigDecimal(discountRate));
        template.setThresholdAmount(new BigDecimal(thresholdAmount));
        return template;
    }
}
