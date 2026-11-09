package com.comeoften.module.coupon.constant;

/**
 * 核销入口。写入 coupon_use_record.verify_channel，用于审计区分 PC 输码与商家端扫码。
 * M5 只落 PC 后台入口；M9 商家端扫码复用同一 Service 并传 MERCHANT_SCAN。
 */
public enum VerifyChannel {

    ADMIN_INPUT("PC 后台输码"),

    MERCHANT_SCAN("商家端扫码");

    private final String description;

    VerifyChannel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static String describe(String code) {
        if (code == null) {
            return "";
        }
        for (VerifyChannel channel : values()) {
            if (channel.name().equals(code)) {
                return channel.getDescription();
            }
        }
        return code;
    }
}
