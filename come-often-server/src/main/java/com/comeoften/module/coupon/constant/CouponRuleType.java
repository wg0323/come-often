package com.comeoften.module.coupon.constant;

/**
 * 券发放规则的触发类型，取值与 {@code coupon_grant_rule.rule_type} 的注释一致。
 */
public enum CouponRuleType {

    NEW_MEMBER("新客券"),
    BIRTHDAY("生日券"),
    REPEAT_PURCHASE("复购券"),
    SLEEPING("沉睡唤醒券");

    private final String description;

    CouponRuleType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按码值解析，未匹配返回 null（历史数据不抛异常）。
     */
    public static CouponRuleType of(String code) {
        if (code == null) {
            return null;
        }
        for (CouponRuleType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
