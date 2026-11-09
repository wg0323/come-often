package com.comeoften.module.order.util;

import com.comeoften.common.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickupCodeGeneratorTest {

    @Test
    void should_generate_a001_when_sequence_is_first_of_day() {
        assertThat(PickupCodeGenerator.pickup(1L)).isEqualTo("A001");
        assertThat(PickupCodeGenerator.pickup(2L)).isEqualTo("A002");
        assertThat(PickupCodeGenerator.pickup(12L)).isEqualTo("A012");
    }

    @Test
    void should_roll_over_to_next_letter_when_sequence_exceeds_999() {
        assertThat(PickupCodeGenerator.pickup(999L)).isEqualTo("A999");
        assertThat(PickupCodeGenerator.pickup(1000L)).isEqualTo("B001");
    }

    @Test
    void should_use_d_letter_for_dine_in_verification_code() {
        assertThat(PickupCodeGenerator.dineIn(1L)).isEqualTo("D001");
        assertThat(PickupCodeGenerator.pickup(1L)).isNotEqualTo(PickupCodeGenerator.dineIn(1L));
    }

    @Test
    void should_reject_when_sequence_is_out_of_range() {
        assertThatThrownBy(() -> PickupCodeGenerator.pickup(0L)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> PickupCodeGenerator.pickup(4000L)).isInstanceOf(BizException.class);
    }
}
