package com.comeoften.common;

/**
 * 预约域 Redis 键统一构建入口。计划 3.5 的键名为 slot:cap:{slotId}，
 * 按差异 4 的隔离口径补 comeoften: 前缀；值为已占用桌数 booked，DB 是最终事实源。
 */
public final class ReservationKeys {

    private static final String PREFIX = "comeoften:";

    private ReservationKeys() {
    }

    /**
     * 时段容量键：comeoften:slot:cap:{slotId}。
     */
    public static String slotCapacity(Long slotId) {
        return PREFIX + "slot:cap:" + slotId;
    }
}
