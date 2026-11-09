package com.comeoften.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 订单域 Redis 键统一构建入口。与 StockKeys 一样带 comeoften: 前缀，避免与其他项目冲突。
 */
public final class OrderKeys {

    private static final String PREFIX = "comeoften:";

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private OrderKeys() {
    }

    /**
     * 自提取餐码当日序号：comeoften:order:pickup:seq:{storeId}:{yyyyMMdd}。
     */
    public static String pickupSequence(Long storeId, LocalDate bizDate) {
        return PREFIX + "order:pickup:seq:" + storeId + ":" + DAY_FORMATTER.format(bizDate);
    }

    /**
     * 到店核销码当日序号：comeoften:order:dinein:seq:{storeId}:{yyyyMMdd}。
     */
    public static String dineInSequence(Long storeId, LocalDate bizDate) {
        return PREFIX + "order:dinein:seq:" + storeId + ":" + DAY_FORMATTER.format(bizDate);
    }

    /**
     * 消费者幂等键，按消息业务 ID 去重，带 TTL 防止无限增长。
     */
    public static String consumeIdempotent(String messageId) {
        return PREFIX + "mq:idem:" + messageId;
    }
}
