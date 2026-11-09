package com.comeoften.common;

/**
 * 兜底任务 Redis 键统一构建入口。与 OrderKeys / StockKeys 一样带 comeoften: 前缀，避免与其他项目冲突。
 */
public final class JobKeys {

    private static final String PREFIX = "comeoften:";

    private JobKeys() {
    }

    /**
     * 任务互斥锁键：comeoften:job:lock:{jobName}。由 @JobLock 切面统一构建，业务代码不手拼。
     */
    public static String lock(String jobName) {
        return PREFIX + "job:lock:" + jobName;
    }

    /**
     * 券过期提醒兜底的去重键：comeoften:job:coupon-expire:{userCouponId}。
     * 与延迟消息共用 message_record，靠 Redis 标记避免每小时重复提醒同一张券。
     */
    public static String couponExpireReminded(Long userCouponId) {
        return PREFIX + "job:coupon-expire:" + userCouponId;
    }
}
