package com.comeoften.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 库存 Redis 键统一构建入口。本机 Redis 与多个项目共用，统一加 comeoften: 前缀隔离。
 */
public final class StockKeys {

    private static final String PREFIX = "comeoften:";

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private StockKeys() {
    }

    /**
     * 菜品每日库存键：comeoften:dish:store:{storeId}:{dishId}:{yyyyMMdd}。
     */
    public static String dish(Long storeId, Long dishId, LocalDate stockDate) {
        return PREFIX + "dish:stock:" + storeId + ":" + dishId + ":" + DAY_FORMATTER.format(stockDate);
    }

    /**
     * 券模板库存键：comeoften:coupon:stock:{templateId}。DB 的 coupon_template.remain 是最终事实源。
     */
    public static String coupon(Long templateId) {
        return PREFIX + "coupon:stock:" + templateId;
    }
}
