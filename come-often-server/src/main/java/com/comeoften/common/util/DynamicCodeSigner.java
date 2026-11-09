package com.comeoften.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 动态码签名公共件。码结构 = 业务序号 + HMAC-SHA256(scope|业务序号|时间窗序号) 截取固定长度，
 * 时间窗序号 = epochSecond / windowSeconds。券码与预约签到码共用本实现，靠 scope 做域隔离，
 * 不复制两套算法。
 */
public final class DynamicCodeSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private static final String SEPARATOR = "|";

    private DynamicCodeSigner() {
    }

    /**
     * 计算指定时间窗的签名。
     */
    public static String sign(String scope, String codeSeq, long window, int signLength, String secret) {
        String payload = scope + SEPARATOR + codeSeq + SEPARATOR + window;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(HEX[(value >> 4) & 0x0F]).append(HEX[value & 0x0F]);
            }
            return builder.substring(0, Math.min(signLength, builder.length()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("动态码签名失败", e);
        }
    }

    /**
     * 时间窗序号。
     */
    public static long window(long epochSecond, long windowSeconds) {
        return epochSecond / windowSeconds;
    }

    /**
     * 生成完整动态码（不含业务前缀）。
     */
    public static String build(String scope, String codeSeq, long epochSecond, long windowSeconds,
                               int signLength, String secret) {
        return codeSeq + sign(scope, codeSeq, window(epochSecond, windowSeconds), signLength, secret);
    }

    /**
     * 校验动态码：当前窗口与前 1 个窗口都算有效，容错跨窗瞬间。
     */
    public static boolean matches(String scope, String codeSeq, String sign, long epochSecond,
                                  long windowSeconds, int signLength, String secret) {
        long current = window(epochSecond, windowSeconds);
        return constantEquals(sign, sign(scope, codeSeq, current, signLength, secret))
                || constantEquals(sign, sign(scope, codeSeq, current - 1, signLength, secret));
    }

    /**
     * 本窗口剩余秒数，供小程序设置刷新定时器。
     */
    public static long secondsRemaining(long epochSecond, long windowSeconds) {
        return windowSeconds - epochSecond % windowSeconds;
    }

    private static boolean constantEquals(String left, String right) {
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }
}
