package com.comeoften.module.order.constant;

/**
 * 核销渠道。由推进订单状态的操作方派生，避免各处各写一份字符串。
 */
public enum VerifyChannel {

    MERCHANT_SCAN("商家端扫码"),

    ADMIN_INPUT("PC 后台确认"),

    MINIAPP("顾客端自助"),

    SYSTEM("系统自动完成");

    private final String description;

    VerifyChannel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static String of(OperatorType operatorType) {
        if (operatorType == null) {
            return SYSTEM.name();
        }
        switch (operatorType) {
            case MERCHANT:
                return MERCHANT_SCAN.name();
            case ADMIN:
                return ADMIN_INPUT.name();
            case MEMBER:
                return MINIAPP.name();
            default:
                return SYSTEM.name();
        }
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
