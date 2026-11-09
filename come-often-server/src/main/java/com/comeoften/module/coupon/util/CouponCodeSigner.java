package com.comeoften.module.coupon.util;

import com.comeoften.common.util.DynamicCodeSigner;
import com.comeoften.module.coupon.constant.CouponConstants;

/**
 * 动态券码签名（计划 3.6）：码结构 = 短序号(6 位) + HMAC-SHA256(短序号|时间窗序号) 截取 10 位，
 * 时间窗序号 = epochSecond / 30。校验当前窗口与前 1 个窗口，容错跨窗瞬间。
 *
 * <p>算法本体已抽到 common 的 DynamicCodeSigner，预约签到码复用同一实现并以 scope 隔离。</p>
 */
public final class CouponCodeSigner {

    private static final String SCOPE = "COUPON";

    private CouponCodeSigner() {
    }

    /**
     * 计算指定时间窗的签名。
     */
    public static String sign(String codeSeq, long window, String secret) {
        return DynamicCodeSigner.sign(SCOPE, codeSeq, window, CouponConstants.SIGN_LENGTH, secret);
    }

    /**
     * 时间窗序号。
     */
    public static long window(long epochSecond) {
        return DynamicCodeSigner.window(epochSecond, CouponConstants.TIME_WINDOW_SECONDS);
    }

    /**
     * 生成完整券码（不含 C 前缀）。
     */
    public static String buildCode(String codeSeq, long epochSecond, String secret) {
        return DynamicCodeSigner.build(SCOPE, codeSeq, epochSecond, CouponConstants.TIME_WINDOW_SECONDS,
                CouponConstants.SIGN_LENGTH, secret);
    }

    /**
     * 校验券码：当前窗口与前 1 个窗口都算有效。
     */
    public static boolean matches(String codeSeq, String sign, long epochSecond, String secret) {
        return DynamicCodeSigner.matches(SCOPE, codeSeq, sign, epochSecond,
                CouponConstants.TIME_WINDOW_SECONDS, CouponConstants.SIGN_LENGTH, secret);
    }

    /**
     * 本窗口剩余秒数，供小程序设置刷新定时器。
     */
    public static long secondsRemaining(long epochSecond) {
        return DynamicCodeSigner.secondsRemaining(epochSecond, CouponConstants.TIME_WINDOW_SECONDS);
    }
}
