package com.comeoften.module.points.util;

import com.comeoften.module.points.entity.MemberLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class MemberLevelResolverTest {

    @Test
    void should_return_highest_level_when_both_thresholds_are_met() {
        MemberLevel level = MemberLevelResolver.highest(
                Arrays.asList(level("NORMAL", "0.00", 0, 1), level("SILVER", "1000.00", 1000, 2),
                        level("GOLD", "3000.00", 3000, 3)),
                new BigDecimal("3500.00"), 3200);
        assertThat(level).isNotNull();
        assertThat(level.getLevelCode()).isEqualTo("GOLD");
    }

    @Test
    void should_fall_back_to_lowest_level_when_amount_threshold_not_met() {
        MemberLevel level = MemberLevelResolver.highest(
                Arrays.asList(level("NORMAL", "0.00", 0, 1), level("SILVER", "1000.00", 1000, 2)),
                new BigDecimal("999.99"), 5000);
        assertThat(level.getLevelCode()).isEqualTo("NORMAL");
    }

    @Test
    void should_return_null_when_points_threshold_not_met_and_no_level_fits() {
        assertThat(MemberLevelResolver.highest(
                Collections.singletonList(level("SILVER", "1000.00", 1000, 2)),
                new BigDecimal("2000.00"), 999)).isNull();
    }

    @Test
    void should_ignore_blank_configuration_values() {
        MemberLevel broken = new MemberLevel();
        broken.setLevelCode("BROKEN");
        broken.setSort(9);
        MemberLevel level = MemberLevelResolver.highest(Arrays.asList(broken, level("NORMAL", "0.00", 0, 1)),
                new BigDecimal("10.00"), 10);
        assertThat(level.getLevelCode()).isEqualTo("NORMAL");
    }

    @Test
    void should_return_null_when_no_level_configured() {
        assertThat(MemberLevelResolver.highest(Collections.emptyList(), BigDecimal.ZERO, 0)).isNull();
    }

    @Test
    void should_treat_null_sort_as_zero() {
        MemberLevel level = level("NORMAL", "0.00", 0, null);
        assertThat(MemberLevelResolver.sortOf(level)).isZero();
        assertThat(MemberLevelResolver.sortOf(null)).isZero();
    }

    private static MemberLevel level(String code, String minAmount, int minPoints, Integer sort) {
        MemberLevel level = new MemberLevel();
        level.setLevelCode(code);
        level.setLevelName(code);
        level.setMinAmount(new BigDecimal(minAmount));
        level.setMinPoints(minPoints);
        level.setSort(sort);
        return level;
    }
}
