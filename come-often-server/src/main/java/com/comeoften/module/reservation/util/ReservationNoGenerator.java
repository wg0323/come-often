package com.comeoften.module.reservation.util;

import com.comeoften.module.reservation.constant.ReservationConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 预约单号生成：RE + yyyyMMddHHmmss + 6 位随机十六进制，唯一性由 uk_reserve_no 兜底，
 * 冲突时调用方重试。单号同时是签到码的业务序号，不做二次编码。
 */
public final class ReservationNoGenerator {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final int RANDOM_BOUND = 0x1000000;

    private ReservationNoGenerator() {
    }

    public static String next() {
        return ReservationConstants.RESERVE_NO_PREFIX + LocalDateTime.now().format(NO_FORMATTER)
                + String.format(Locale.ROOT, "%06X", ThreadLocalRandom.current().nextInt(RANDOM_BOUND));
    }
}
