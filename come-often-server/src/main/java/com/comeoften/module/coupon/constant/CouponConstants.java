package com.comeoften.module.coupon.constant;

/**
 * 券域常量。动态券码按计划 3.6：短序号 6 位 + HMAC-SHA256 截 10 位，时间窗 30 秒。
 */
public final class CouponConstants {

    /** 券码时间窗秒数，与计划 3.6 的 epochSecond / 30 一致。 */
    public static final long TIME_WINDOW_SECONDS = 30L;

    /** 短序号位数，写入 user_coupon.code_seq。 */
    public static final int CODE_SEQ_LENGTH = 6;

    /** 签名截取位数。 */
    public static final int SIGN_LENGTH = 10;

    /** 商家端扫码分流前缀（计划 3.12：C → 券核销）。PC 输码时前缀可省略。 */
    public static final String CODE_PREFIX = "C";

    /** 短序号撞 uk_code_seq 时的重试次数。 */
    public static final int CODE_SEQ_MAX_RETRY = 5;

    /** 券码即将过期的提醒提前量（小时）。 */
    public static final long EXPIRE_REMIND_AHEAD_HOURS = 24L;

    private CouponConstants() {
    }
}
