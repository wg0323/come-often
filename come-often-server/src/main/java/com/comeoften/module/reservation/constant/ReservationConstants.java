package com.comeoften.module.reservation.constant;

/**
 * 预约域常量。签到码复用动态码公共件，时间窗 30 秒、签名 10 位，前缀 R 与券码 C 分流。
 */
public final class ReservationConstants {

    /** 签到码时间窗秒数，与券码同一口径。 */
    public static final long TIME_WINDOW_SECONDS = 30L;

    /** 签到码签名截取位数。 */
    public static final int SIGN_LENGTH = 10;

    /** 动态码签名域，与券码的 COUPON 隔离，避免同一序号在两类码之间串用。 */
    public static final String SIGN_SCOPE = "RESERVATION";

    /** 商家端扫码分流前缀（计划 3.12：R → 预约签到）。 */
    public static final String CODE_PREFIX = "R";

    /** 预约单号前缀。 */
    public static final String RESERVE_NO_PREFIX = "RE";

    /** 预约单号里的时间部分长度：yyyyMMddHHmmss。 */
    private static final int RESERVE_NO_TIME_LENGTH = 14;

    /** 预约单号里的随机部分长度：6 位十六进制。 */
    private static final int RESERVE_NO_RANDOM_LENGTH = 6;

    /** 预约单号总长度，签到码分流据此判定码串是否完整。 */
    public static final int RESERVE_NO_LENGTH = RESERVE_NO_PREFIX.length()
            + RESERVE_NO_TIME_LENGTH + RESERVE_NO_RANDOM_LENGTH;

    /** 预约单号生成撞 uk_reserve_no 时的重试次数。 */
    public static final int RESERVE_NO_MAX_RETRY = 3;

    /** 日历查询最大跨度天数，防止一次拉爆。 */
    public static final int CALENDAR_MAX_DAYS = 31;

    private ReservationConstants() {
    }
}
