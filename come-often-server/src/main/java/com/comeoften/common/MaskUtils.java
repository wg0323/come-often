package com.comeoften.common;

/**
 * 敏感字段脱敏。手机号只保留前 3 位与后 4 位，中间位固定用 4 个星号。
 */
public final class MaskUtils {

    private static final int PHONE_PREFIX_LENGTH = 3;

    private static final int PHONE_SUFFIX_START = 7;

    private static final String PHONE_MASK = "****";

    private MaskUtils() {
    }

    /**
     * 手机号脱敏；长度不足的脏数据原样返回，不做截断。
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < PHONE_SUFFIX_START) {
            return phone;
        }
        return phone.substring(0, PHONE_PREFIX_LENGTH) + PHONE_MASK + phone.substring(PHONE_SUFFIX_START);
    }
}
