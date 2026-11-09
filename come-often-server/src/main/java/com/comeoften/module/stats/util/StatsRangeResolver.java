package com.comeoften.module.stats.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.stats.vo.StatsDateRange;

import java.time.LocalDate;

/**
 * 查询区间解析。默认「近 7 天（含今天）」，上限 366 天：
 * 上限的作用是挡住「一次导出三年」这类把库拖死的请求，不是产品限制。
 */
public final class StatsRangeResolver {

    /** 单次查询最长天数，超过返回 30055。 */
    public static final int MAX_RANGE_DAYS = 366;

    /** 未传区间时的默认天数。 */
    public static final int DEFAULT_RANGE_DAYS = 7;

    private StatsRangeResolver() {
    }

    public static StatsDateRange resolve(LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(DEFAULT_RANGE_DAYS - 1L) : startDate;
        if (start.isAfter(end)) {
            throw new BizException(ErrorCode.STATS_DATE_RANGE_INVALID, "起始日不能晚于结束日");
        }
        long days = end.toEpochDay() - start.toEpochDay() + 1L;
        if (days > MAX_RANGE_DAYS) {
            throw new BizException(ErrorCode.STATS_DATE_RANGE_INVALID,
                    "统计区间最多 " + MAX_RANGE_DAYS + " 天");
        }
        StatsDateRange range = new StatsDateRange();
        range.setStartDate(start);
        range.setEndDate(end);
        return range;
    }
}
