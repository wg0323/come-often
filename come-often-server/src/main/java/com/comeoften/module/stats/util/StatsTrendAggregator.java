package com.comeoften.module.stats.util;

import com.comeoften.module.stats.constant.StatsGranularity;
import com.comeoften.module.stats.vo.StatsDailyRefundRow;
import com.comeoften.module.stats.vo.StatsDailyRevenueRow;
import com.comeoften.module.stats.vo.StatsDateRange;
import com.comeoften.module.stats.vo.StatsTrendPointVO;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 趋势分桶。SQL 只出到「按日」，周 / 月桶在这里聚合：
 * 好处是三个粒度加起来永远等于区间汇总，且区间内没有订单的日期会补 0 而不是被跳过。
 */
public final class StatsTrendAggregator {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("yyyy-MM");

    private StatsTrendAggregator() {
    }

    public static List<StatsTrendPointVO> aggregate(List<StatsDailyRevenueRow> paidRows,
                                                    List<StatsDailyRefundRow> refundRows,
                                                    StatsDateRange range,
                                                    StatsGranularity granularity) {
        Map<LocalDate, BigDecimal> paidByDay = new HashMap<>();
        Map<LocalDate, Long> countByDay = new HashMap<>();
        if (paidRows != null) {
            for (StatsDailyRevenueRow row : paidRows) {
                if (row.getStatDate() == null) {
                    continue;
                }
                paidByDay.merge(row.getStatDate(), StatsAmountUtils.zeroIfNull(row.getPaidAmount()),
                        BigDecimal::add);
                countByDay.merge(row.getStatDate(), row.getOrderCount() == null ? 0L : row.getOrderCount(),
                        Long::sum);
            }
        }
        Map<LocalDate, BigDecimal> refundByDay = new HashMap<>();
        if (refundRows != null) {
            for (StatsDailyRefundRow row : refundRows) {
                if (row.getStatDate() == null) {
                    continue;
                }
                refundByDay.merge(row.getStatDate(), StatsAmountUtils.zeroIfNull(row.getRefundAmount()),
                        BigDecimal::add);
            }
        }

        Map<LocalDate, Bucket> buckets = new LinkedHashMap<>();
        LocalDate cursor = range.getStartDate();
        while (!cursor.isAfter(range.getEndDate())) {
            BigDecimal paid = paidByDay.getOrDefault(cursor, BigDecimal.ZERO);
            BigDecimal refund = refundByDay.getOrDefault(cursor, BigDecimal.ZERO);
            long count = countByDay.getOrDefault(cursor, 0L);
            LocalDate bucketKey = bucketOf(cursor, granularity);
            Bucket bucket = buckets.computeIfAbsent(bucketKey, key -> new Bucket());
            bucket.netRevenue = bucket.netRevenue.add(paid).subtract(refund);
            bucket.refundAmount = bucket.refundAmount.add(refund);
            bucket.orderCount += count;
            cursor = cursor.plusDays(1L);
        }

        List<StatsTrendPointVO> points = new ArrayList<>(buckets.size());
        for (Map.Entry<LocalDate, Bucket> entry : buckets.entrySet()) {
            Bucket bucket = entry.getValue();
            StatsTrendPointVO point = new StatsTrendPointVO();
            point.setBucketStart(entry.getKey().toString());
            point.setLabel(labelOf(entry.getKey(), granularity));
            point.setNetRevenue(StatsAmountUtils.scale(bucket.netRevenue));
            point.setRefundAmount(StatsAmountUtils.scale(bucket.refundAmount));
            point.setOrderCount(bucket.orderCount);
            point.setAvgOrderAmount(StatsAmountUtils.average(
                    StatsAmountUtils.scale(bucket.netRevenue), bucket.orderCount));
            points.add(point);
        }
        return points;
    }

    /**
     * 桶键：按日为当天，按周为所在周周一，按月为当月 1 日。
     */
    public static LocalDate bucketOf(LocalDate date, StatsGranularity granularity) {
        if (granularity == StatsGranularity.WEEK) {
            return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if (granularity == StatsGranularity.MONTH) {
            return date.withDayOfMonth(1);
        }
        return date;
    }

    private static String labelOf(LocalDate bucketStart, StatsGranularity granularity) {
        return granularity == StatsGranularity.MONTH
                ? bucketStart.format(MONTH_LABEL) : bucketStart.format(DAY_LABEL);
    }

    /**
     * 累加器。金额用 BigDecimal，订单数用 long，避免中途丢精度。
     */
    private static final class Bucket {

        private BigDecimal netRevenue = BigDecimal.ZERO;

        private BigDecimal refundAmount = BigDecimal.ZERO;

        private long orderCount;
    }
}
