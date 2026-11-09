package com.comeoften.module.stats.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.stats.vo.StatsDateRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatsRangeResolverTest {

    @Test
    void should_default_to_last_seven_days_when_range_missing() {
        StatsDateRange range = StatsRangeResolver.resolve(null, null);
        assertThat(range.getEndDate()).isEqualTo(LocalDate.now());
        assertThat(range.days()).isEqualTo(StatsRangeResolver.DEFAULT_RANGE_DAYS);
    }

    @Test
    void should_keep_both_ends_inclusive_when_range_given() {
        StatsDateRange range = StatsRangeResolver.resolve(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 9));
        assertThat(range.days()).isEqualTo(9L);
        assertThat(range.fromInclusive()).isEqualTo("2026-11-01T00:00");
        assertThat(range.toExclusive()).isEqualTo("2026-11-10T00:00");
    }

    @Test
    void should_reject_when_start_after_end() {
        assertThatThrownBy(() -> StatsRangeResolver.resolve(LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 1)))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.STATS_DATE_RANGE_INVALID);
    }

    @Test
    void should_reject_when_range_longer_than_limit() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = start.plusDays(StatsRangeResolver.MAX_RANGE_DAYS);
        assertThatThrownBy(() -> StatsRangeResolver.resolve(start, end))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.STATS_DATE_RANGE_INVALID);
    }

    @Test
    void should_accept_range_exactly_on_limit() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = start.plusDays(StatsRangeResolver.MAX_RANGE_DAYS - 1L);
        assertThat(StatsRangeResolver.resolve(start, end).days())
                .isEqualTo(StatsRangeResolver.MAX_RANGE_DAYS);
    }
}
