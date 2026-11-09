package com.comeoften.module.coupon.constant;

/**
 * 券模板状态。取值与 coupon_template.status 一致：0-草稿，1-发放中，2-已停用。
 */
public enum CouponTemplateStatus {

    DRAFT(0, "草稿"),

    ISSUING(1, "发放中"),

    STOPPED(2, "已停用");

    private final int code;
    private final String description;

    CouponTemplateStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static CouponTemplateStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CouponTemplateStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public static String describe(Integer code) {
        CouponTemplateStatus status = of(code);
        return status == null ? "" : status.getDescription();
    }
}
