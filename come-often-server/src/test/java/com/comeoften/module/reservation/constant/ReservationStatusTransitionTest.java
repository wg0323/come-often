package com.comeoften.module.reservation.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationStatusTransitionTest {

    @Test
    void should_allow_pending_to_confirmed_checked_in_no_show_and_cancelled() {
        assertThat(ReservationStatusTransition.canTransit("PENDING", "CONFIRMED")).isTrue();
        assertThat(ReservationStatusTransition.canTransit("PENDING", "CHECKED_IN")).isTrue();
        assertThat(ReservationStatusTransition.canTransit("PENDING", "NO_SHOW")).isTrue();
        assertThat(ReservationStatusTransition.canTransit("PENDING", "CANCELLED")).isTrue();
    }

    @Test
    void should_allow_confirmed_to_flow_forward_only() {
        assertThat(ReservationStatusTransition.canTransit("CONFIRMED", "CHECKED_IN")).isTrue();
        assertThat(ReservationStatusTransition.canTransit("CONFIRMED", "NO_SHOW")).isTrue();
        assertThat(ReservationStatusTransition.canTransit("CONFIRMED", "CANCELLED")).isTrue();
        assertThat(ReservationStatusTransition.canTransit("CONFIRMED", "PENDING")).isFalse();
    }

    @Test
    void should_reject_any_transition_from_terminal_status() {
        assertThat(ReservationStatusTransition.canTransit("CHECKED_IN", "CANCELLED")).isFalse();
        assertThat(ReservationStatusTransition.canTransit("NO_SHOW", "CONFIRMED")).isFalse();
        assertThat(ReservationStatusTransition.canTransit("CANCELLED", "CONFIRMED")).isFalse();
    }

    @Test
    void should_reject_unknown_status() {
        assertThat(ReservationStatusTransition.canTransit("PENDING", "UNKNOWN")).isFalse();
        assertThat(ReservationStatusTransition.canTransit(null, "CONFIRMED")).isFalse();
    }

    @Test
    void should_only_treat_pending_and_confirmed_as_active() {
        assertThat(ReservationStatus.active("PENDING")).isTrue();
        assertThat(ReservationStatus.active("CONFIRMED")).isTrue();
        assertThat(ReservationStatus.active("CHECKED_IN")).isFalse();
        assertThat(ReservationStatus.active("NO_SHOW")).isFalse();
        assertThat(ReservationStatus.active("CANCELLED")).isFalse();
    }
}
