package com.comeoften.module.points.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PointsBizKeysTest {

    @Test
    void should_keep_earn_redeem_and_rollback_keys_distinct_for_same_order() {
        String earn = PointsBizKeys.orderEarn("CO20261108120000ABCDEF");
        String redeem = PointsBizKeys.orderRedeem("CO20261108120000ABCDEF");
        String rollback = PointsBizKeys.orderRollback("CO20261108120000ABCDEF");
        assertThat(earn).isNotEqualTo(redeem);
        assertThat(redeem).isNotEqualTo(rollback);
        assertThat(earn).isNotEqualTo(rollback);
        assertThat(earn).startsWith("CO20261108120000ABCDEF#");
    }

    @Test
    void should_build_expire_key_from_source_record_id() {
        assertThat(PointsBizKeys.expire(1234L)).isEqualTo("1234#EXPIRE");
    }

    @Test
    void should_build_same_day_key_for_sign_in_date() {
        assertThat(PointsBizKeys.byDay(LocalDate.of(2026, 11, 8))).isEqualTo("20261108");
    }

    @Test
    void should_build_single_profile_key() {
        assertThat(PointsBizKeys.profile()).isEqualTo("PROFILE");
    }

    @Test
    void should_build_review_key_from_review_id() {
        assertThat(PointsBizKeys.review(88L)).isEqualTo("88");
    }

    @Test
    void should_keep_exchange_keys_distinct_from_order_redeem() {
        String exchange = PointsBizKeys.exchange("CO20261110090000ABCDEF");
        String rollback = PointsBizKeys.exchangeRollback("CO20261110090000ABCDEF");
        assertThat(exchange).isEqualTo("CO20261110090000ABCDEF#EXCHANGE");
        assertThat(rollback).isEqualTo("CO20261110090000ABCDEF#EXCHANGE_ROLLBACK");
        assertThat(exchange).isNotEqualTo(rollback);
        assertThat(exchange).isNotEqualTo(PointsBizKeys.orderRedeem("CO20261110090000ABCDEF"));
    }
}
