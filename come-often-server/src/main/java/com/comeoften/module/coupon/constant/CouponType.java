package com.comeoften.module.coupon.constant;

/**
 * 券类型。取值与 coupon_template.coupon_type 一一对应，计划第七章要求 5 类券各能建一张。
 */
public enum CouponType {

    /** 满减券：满 threshold_amount 减 face_value。 */
    FULL_REDUCTION("满减券"),

    /** 折扣券：按 discount_rate 打折，无固定面额。 */
    DISCOUNT("折扣券"),

    /** 单品券：只抵扣 dish_id / coupon_template_dish 指定菜品。 */
    DISH("单品券"),

    /** 无门槛券：不受 threshold_amount 限制。 */
    NO_THRESHOLD("无门槛券"),

    /** 生日券：M5 按无门槛面额券发放，生日归属校验待会员生日字段落地后补。 */
    BIRTHDAY("生日券");

    private final String description;

    CouponType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按编码解析，未匹配返回 null，由调用方决定抛哪个业务码。
     */
    public static CouponType of(String code) {
        if (code == null) {
            return null;
        }
        for (CouponType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static String describe(String code) {
        CouponType type = of(code);
        return type == null ? code : type.getDescription();
    }
}
