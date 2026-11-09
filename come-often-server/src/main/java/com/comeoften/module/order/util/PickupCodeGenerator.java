package com.comeoften.module.order.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;

import java.util.regex.Pattern;

/**
 * 取餐码 / 到店核销码生成。同店同日按 Redis INCR 序号生成，A001 起，满 999 进位到 B001。
 * 自提用 A 段字母，到店核销用 D 段字母，避免两种码在同一营业日撞唯一索引；
 * 两段都按 999 进位，所以自提最多到 D，到店最多到 G。
 */
public final class PickupCodeGenerator {

    private static final long NUMBERS_PER_LETTER = 999L;

    private static final long MAX_SEQUENCE = NUMBERS_PER_LETTER * 4L;

    private static final char PICKUP_BASE_LETTER = 'A';

    private static final char DINE_IN_BASE_LETTER = 'D';

    /** 当日码串的最大字母，商家端扫码头据此判定 4 位码归属。 */
    public static final char MAX_CODE_LETTER = (char) (DINE_IN_BASE_LETTER
            + (MAX_SEQUENCE - 1L) / NUMBERS_PER_LETTER);

    /** 取餐码 / 到店核销码的形态：1 位字母 + 3 位数字。 */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-" + MAX_CODE_LETTER + "]\\d{3}$");

    private PickupCodeGenerator() {
    }

    public static String pickup(long sequence) {
        return format(PICKUP_BASE_LETTER, sequence);
    }

    public static String dineIn(long sequence) {
        return format(DINE_IN_BASE_LETTER, sequence);
    }

    /**
     * 是否为合法的取餐码 / 到店核销码，供商家端扫码分流使用，避免在别处复制一份字母区间。
     */
    public static boolean matches(String code) {
        return code != null && CODE_PATTERN.matcher(code).matches();
    }

    private static String format(char baseLetter, long sequence) {
        if (sequence <= 0L || sequence > MAX_SEQUENCE) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "取餐码序号超出当日可用范围");
        }
        long block = (sequence - 1L) / NUMBERS_PER_LETTER;
        int number = (int) ((sequence - 1L) % NUMBERS_PER_LETTER) + 1;
        char letter = (char) (baseLetter + block);
        return String.format("%c%03d", letter, number);
    }
}
