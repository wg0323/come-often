package com.comeoften.module.coupon.constant;

/**
 * 用户券状态。取值与 user_coupon.status 一致：0-可用，1-已使用，2-已过期。
 */
public enum UserCouponStatus {

    AVAILABLE(0, "可用"),

    USED(1, "已使用"),

    EXPIRED(2, "已过期");

    private final int code;
    private final String description;

    UserCouponStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserCouponStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserCouponStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public static String describe(Integer code) {
        UserCouponStatus status = of(code);
        return status == null ? "" : status.getDescription();
    }
}
