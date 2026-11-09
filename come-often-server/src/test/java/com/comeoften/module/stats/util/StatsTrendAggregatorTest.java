package com.comeoften.module.stats.util;

import com.comeoften.module.stats.constant.StatsGranularity;
import com.comeoften.module.stats.vo.StatsDailyRefundRow;
import com.comeoften.module.stats.vo.StatsDailyRevenueRow;
import com.comeoften.module.stats.vo.StatsDateRange;
import com.comeoften.module.stats.vo.StatsTrendPointVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatsTrendAggregatorTest {

    @Test
    void should_fill_missing_days_with_zero_and_net_refunds() {
        List<StatsTrendPointVO> points = StatsTrendAggregator.aggregate(
                Arrays.asList(paid("2026-11-02", "100.00", 2L), paid("2026-11-04", "50.00", 1L)),
                Collections.singletonList(refund("2026-11-04", "20.00")),
                range("2026-11-02", "2026-11-08"), StatsGranularity.DAY);

        assertThat(points).hasSize(7);
        assertThat(points.get(0).getBucketStart()).isEqualTo("2026-11-02");
        assertThat(points.get(0).getNetRevenue()).isEqualByComparingTo("100.00");
        assertThat(points.get(0).getAvgOrderAmount()).isEqualByComparingTo("50.00");
        assertThat(points.get(2).getBucketStart()).isEqualTo("2026-11-04");
        assertThat(points.get(2).getNetRevenue()).isEqualByComparingTo("30.00");
        assertThat(points.get(2).getRefundAmount()).isEqualByComparingTo("20.00");
        assertThat(points.get(2).getOrderCount()).isEqualTo(1L);
        assertThat(points.get(6).getNetRevenue()).isEqualByComparingTo("0.00");
        assertThat(points.get(6).getAvgOrderAmount()).isNull();
    }

    @Test
    void should_bucket_by_monday_when_granularity_is_week() {
        List<StatsTrendPointVO> points = StatsTrendAggregator.aggregate(
                Arrays.asList(paid("2026-11-02", "100.00", 2L), paid("2026-11-04", "50.00", 1L)),
                Collections.singletonList(refund("2026-11-04", "20.00")),
                range("2026-11-02", "2026-11-08"), StatsGranularity.WEEK);

        assertThat(points).hasSize(1);
        StatsTrendPointVO point = points.get(0);
        assertThat(point.getBucketStart()).isEqualTo("2026-11-02");
        assertThat(point.getLabel()).isEqualTo("11-02");
        assertThat(point.getNetRevenue()).isEqualByComparingTo("130.00");
        assertThat(point.getOrderCount()).isEqualTo(3L);
        assertThat(point.getAvgOrderAmount()).isEqualByComparingTo("43.33");
    }

    @Test
    void should_bucket_by_month_and_keep_partial_range() {
        List<StatsTrendPointVO> points = StatsTrendAggregator.aggregate(
                Collections.singletonList(paid("2026-11-01", "88.00", 1L)),
                Collections.emptyList(), range("2026-10-30", "2026-11-02"),
                StatsGranularity.MONTH);

        assertThat(points).hasSize(2);
        assertThat(points.get(0).getBucketStart()).isEqualTo("2026-10-01");
        assertThat(points.get(0).getLabel()).isEqualTo("2026-10");
        assertThat(points.get(0).getNetRevenue()).isEqualByComparingTo("0.00");
        assertThat(points.get(1).getBucketStart()).isEqualTo("2026-11-01");
        assertThat(points.get(1).getNetRevenue()).isEqualByComparingTo("88.00");
    }

    @Test
    void should_keep_granularity_totals_equal_to_daily_total() {
        List<StatsDailyRevenueRow> paidRows = Arrays.asList(
                paid("2026-10-30", "10.00", 1L), paid("2026-11-01", "88.00", 2L),
                paid("2026-11-09", "5.50", 1L));
        List<StatsDailyRefundRow> refundRows = Collections.singletonList(refund("2026-11-09", "2.50"));
        StatsDateRange range = range("2026-10-30", "2026-11-09");

        BigDecimal dayTotal = total(StatsTrendAggregator.aggregate(paidRows, refundRows, range,
                StatsGranularity.DAY));
        BigDecimal weekTotal = total(StatsTrendAggregator.aggregate(paidRows, refundRows, range,
                StatsGranularity.WEEK));
        BigDecimal monthTotal = total(StatsTrendAggregator.aggregate(paidRows, refundRows, range,
                StatsGranularity.MONTH));

        assertThat(dayTotal).isEqualByComparingTo("101.00");
        assertThat(weekTotal).isEqualByComparingTo(dayTotal);
        assertThat(monthTotal).isEqualByComparingTo(dayTotal);
    }

    private BigDecimal total(List<StatsTrendPointVO> points) {
        BigDecimal total = BigDecimal.ZERO;
        for (StatsTrendPointVO point : points) {
            total = total.add(point.getNetRevenue());
        }
        return total;
    }

    private StatsDailyRevenueRow paid(String date, String amount, Long count) {
        StatsDailyRevenueRow row = new StatsDailyRevenueRow();
        row.setStatDate(LocalDate.parse(date));
        row.setPaidAmount(new BigDecimal(amount));
        row.setOrderCount(count);
        return row;
    }

    private StatsDailyRefundRow refund(String date, String amount) {
        StatsDailyRefundRow row = new StatsDailyRefundRow();
        row.setStatDate(LocalDate.parse(date));
        row.setRefundAmount(new BigDecimal(amount));
        return row;
    }

    private StatsDateRange range(String start, String end) {
        StatsDateRange range = new StatsDateRange();
        range.setStartDate(LocalDate.parse(start));
        range.setEndDate(LocalDate.parse(end));
        return range;
    }
}
