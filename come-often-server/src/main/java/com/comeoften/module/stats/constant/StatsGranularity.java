package com.comeoften.module.stats.constant;

/**
 * 报表时间粒度。分桶在 Java 内完成（按日取数后聚合），SQL 只出到日，
 * 保证「按日汇总 = 按周汇总 = 按月汇总」三个粒度加起来永远等于区间汇总。
 */
public enum StatsGranularity {

    DAY("按日"),

    WEEK("按周"),

    MONTH("按月");

    private final String description;

    StatsGranularity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按编码解析，空值按默认「按日」处理；非法编码返回 null，由调用方抛参数错误。
     */
    public static StatsGranularity of(String code) {
        if (code == null || code.trim().isEmpty()) {
            return DAY;
        }
        for (StatsGranularity granularity : values()) {
            if (granularity.name().equalsIgnoreCase(code.trim())) {
                return granularity;
            }
        }
        return null;
    }

    public static String describe(StatsGranularity granularity) {
        return granularity == null ? DAY.getDescription() : granularity.getDescription();
    }
}
