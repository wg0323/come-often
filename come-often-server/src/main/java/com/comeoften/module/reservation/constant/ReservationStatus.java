package com.comeoften.module.reservation.constant;

/**
 * 预约状态。取值与 M0 建表注释一致：PENDING/CONFIRMED/CHECKED_IN/NO_SHOW/CANCELLED。
 */
public enum ReservationStatus {

    PENDING("待确认"),

    CONFIRMED("已确认"),

    CHECKED_IN("已到店"),

    NO_SHOW("未到店"),

    CANCELLED("已取消");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按编码解析，未知返回 null，由调用方决定是否报错。
     */
    public static ReservationStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (ReservationStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String describe(String code) {
        ReservationStatus status = of(code);
        return status == null ? code : status.getDescription();
    }

    /**
     * 未到店与取消都允许从待确认/已确认发起，用同一组 SQL 条件。
     */
    public static boolean active(String code) {
        return PENDING.name().equals(code) || CONFIRMED.name().equals(code);
    }
}
