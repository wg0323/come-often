package com.comeoften.module.review.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRatingCalculatorTest {

    @Test
    void should_return_zero_with_one_decimal_when_no_rating() {
        assertThat(ReviewRatingCalculator.average(Collections.emptyList()))
                .isEqualByComparingTo(new BigDecimal("0.0"));
        assertThat(ReviewRatingCalculator.average((BigDecimal) null))
                .isEqualByComparingTo(new BigDecimal("0.0"));
    }

    @Test
    void should_round_half_up_to_one_decimal() {
        assertThat(ReviewRatingCalculator.average(Arrays.asList(5, 4)))
                .isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(ReviewRatingCalculator.average(Arrays.asList(5, 4, 4)))
                .isEqualByComparingTo(new BigDecimal("4.3"));
        assertThat(ReviewRatingCalculator.average(Arrays.asList(4, 4, 5, 5)))
                .isEqualByComparingTo(new BigDecimal("4.5"));
    }

    @Test
    void should_ignore_null_rating() {
        assertThat(ReviewRatingCalculator.average(Arrays.asList(5, null, 3)))
                .isEqualByComparingTo(new BigDecimal("4.0"));
    }

    @Test
    void should_scale_single_sql_average() {
        assertThat(ReviewRatingCalculator.average(new BigDecimal("4.333333")))
                .isEqualByComparingTo(new BigDecimal("4.3"));
    }
}
