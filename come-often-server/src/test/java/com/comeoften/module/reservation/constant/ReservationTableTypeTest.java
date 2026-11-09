package com.comeoften.module.reservation.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationTableTypeTest {

    @Test
    void should_recommend_hall_card_when_people_within_four() {
        assertThat(ReservationTableType.recommend(1)).isEqualTo("HALL_CARD");
        assertThat(ReservationTableType.recommend(4)).isEqualTo("HALL_CARD");
    }

    @Test
    void should_recommend_private_small_when_people_between_six_and_ten() {
        assertThat(ReservationTableType.recommend(6)).isEqualTo("PRIVATE_SMALL");
        assertThat(ReservationTableType.recommend(10)).isEqualTo("PRIVATE_SMALL");
    }

    @Test
    void should_recommend_private_large_when_people_between_twelve_and_twenty() {
        assertThat(ReservationTableType.recommend(12)).isEqualTo("PRIVATE_LARGE");
        assertThat(ReservationTableType.recommend(20)).isEqualTo("PRIVATE_LARGE");
    }

    @Test
    void should_return_null_when_no_type_matches() {
        assertThat(ReservationTableType.recommend(5)).isNull();
        assertThat(ReservationTableType.recommend(21)).isNull();
    }

    @Test
    void should_reject_eight_people_in_large_private_room() {
        assertThat(ReservationTableType.supports("PRIVATE_LARGE", 8)).isFalse();
        assertThat(ReservationTableType.supports("PRIVATE_LARGE", 12)).isTrue();
    }

    @Test
    void should_reject_unknown_table_type() {
        assertThat(ReservationTableType.supports("UNKNOWN", 2)).isFalse();
        assertThat(ReservationTableType.describe("UNKNOWN")).isEqualTo("UNKNOWN");
    }
}
