package com.comeoften.module.reservation.constant;

import com.comeoften.module.order.constant.VerifyChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 包间类型与核销渠道映射：三态枚举、桌型对齐、渠道不落到 SYSTEM。
 */
class ReservationRoomTypeTest {

    @Test
    void should_accept_only_known_room_types() {
        assertThat(ReservationRoomType.isValid("PRIVATE_SMALL")).isTrue();
        assertThat(ReservationRoomType.isValid("PRIVATE_LARGE")).isTrue();
        assertThat(ReservationRoomType.isValid("HALL")).isTrue();
        assertThat(ReservationRoomType.isValid("")).isFalse();
        assertThat(ReservationRoomType.isValid("HALL_CARD")).isFalse();
    }

    @Test
    void should_only_treat_private_rooms_as_bookable() {
        assertThat(ReservationRoomType.isPrivate("PRIVATE_SMALL")).isTrue();
        assertThat(ReservationRoomType.isPrivate("PRIVATE_LARGE")).isTrue();
        assertThat(ReservationRoomType.isPrivate("HALL")).isFalse();
        assertThat(ReservationRoomType.describe("PRIVATE_LARGE")).isEqualTo("大包间");
    }

    @Test
    void should_map_operator_to_verify_channel() {
        assertThat(VerifyChannel.of(com.comeoften.module.order.constant.OperatorType.MERCHANT))
                .isEqualTo("MERCHANT_SCAN");
        assertThat(VerifyChannel.of(com.comeoften.module.order.constant.OperatorType.ADMIN))
                .isEqualTo("ADMIN_INPUT");
        assertThat(VerifyChannel.of(com.comeoften.module.order.constant.OperatorType.SYSTEM))
                .isEqualTo("SYSTEM");
        assertThat(VerifyChannel.describe("ADMIN_INPUT")).isEqualTo("PC 后台确认");
    }
}
