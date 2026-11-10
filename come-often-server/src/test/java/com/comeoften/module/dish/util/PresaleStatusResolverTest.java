package com.comeoften.module.dish.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 预售时间窗判定：无开售时间、未开售、开售中、已结束、结束时间为空。
 */
class PresaleStatusResolverTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Test
    void should_not_be_presale_when_start_time_is_null() {
        assertThat(PresaleStatusResolver.resolve(null, null, NOW)).isEmpty();
    }

    @Test
    void should_be_not_started_when_now_is_before_start_time() {
        assertThat(PresaleStatusResolver.resolve(NOW.plusHours(1L), NOW.plusDays(1L), NOW))
                .isEqualTo(PresaleStatusResolver.NOT_STARTED);
        assertThat(PresaleStatusResolver.text(PresaleStatusResolver.NOT_STARTED, NOW.plusHours(1L)))
                .isEqualTo("11月10日 13:00 开售");
    }

    @Test
    void should_be_selling_between_start_and_end() {
        assertThat(PresaleStatusResolver.resolve(NOW.minusHours(1L), NOW.plusHours(1L), NOW))
                .isEqualTo(PresaleStatusResolver.SELLING);
        assertThat(PresaleStatusResolver.resolve(NOW.minusHours(1L), null, NOW))
                .isEqualTo(PresaleStatusResolver.SELLING);
    }

    @Test
    void should_be_ended_when_now_reaches_end_time() {
        assertThat(PresaleStatusResolver.resolve(NOW.minusDays(1L), NOW, NOW))
                .isEqualTo(PresaleStatusResolver.ENDED);
        assertThat(PresaleStatusResolver.resolve(NOW.minusDays(1L), NOW.minusHours(1L), NOW))
                .isEqualTo(PresaleStatusResolver.ENDED);
    }
}
