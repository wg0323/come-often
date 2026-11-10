package com.comeoften.module.dish.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 预售时间窗的展示与下单判定。与 is_preorder（提前 1 天预定的预定菜）是两套独立功能。
 */
public final class PresaleStatusResolver {

    public static final String NOT_STARTED = "NOT_STARTED";

    public static final String SELLING = "SELLING";

    public static final String ENDED = "ENDED";

    private static final DateTimeFormatter TEXT_FORMATTER = DateTimeFormatter.ofPattern("MM月dd日 HH:mm");

    private PresaleStatusResolver() {
    }

    /**
     * 没有开售时间表示不是预售商品，返回空串；结束时间为空表示开售后不限时。
     */
    public static String resolve(LocalDateTime startTime, LocalDateTime endTime, LocalDateTime now) {
        if (startTime == null) {
            return "";
        }
        LocalDateTime cursor = now == null ? LocalDateTime.now() : now;
        if (cursor.isBefore(startTime)) {
            return NOT_STARTED;
        }
        if (endTime != null && !cursor.isBefore(endTime)) {
            return ENDED;
        }
        return SELLING;
    }

    public static String text(String status, LocalDateTime startTime) {
        if (NOT_STARTED.equals(status)) {
            return startTime == null ? "即将开售" : TEXT_FORMATTER.format(startTime) + " 开售";
        }
        if (SELLING.equals(status)) {
            return "预售中";
        }
        if (ENDED.equals(status)) {
            return "预售已结束";
        }
        return "";
    }
}
