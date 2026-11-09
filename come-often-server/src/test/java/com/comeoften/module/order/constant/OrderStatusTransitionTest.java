package com.comeoften.module.order.constant;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTransitionTest {

    @Test
    void should_allow_full_happy_path_when_transitions_are_legal() {
        assertThat(OrderStatusTransition.canTransition(OrderStatus.CREATED, OrderStatus.PAID)).isTrue();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.PAID, OrderStatus.ACCEPTED)).isTrue();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.ACCEPTED, OrderStatus.READY)).isTrue();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.READY, OrderStatus.COMPLETED)).isTrue();
    }

    @Test
    void should_reject_ready_to_created_when_transition_is_illegal() {
        assertThat(OrderStatusTransition.canTransition(OrderStatus.READY, OrderStatus.CREATED)).isFalse();
        assertThatThrownBy(() -> OrderStatusTransition.assertAllowed(OrderStatus.READY, OrderStatus.CREATED))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_STATUS_INVALID);
    }

    @Test
    void should_reject_transition_from_terminal_status_when_order_is_finished() {
        assertThat(OrderStatusTransition.canTransition(OrderStatus.COMPLETED, OrderStatus.READY)).isFalse();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.CANCELLED, OrderStatus.PAID)).isFalse();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.REFUNDED, OrderStatus.PAID)).isFalse();
    }

    @Test
    void should_allow_timeout_cancel_only_from_created_when_order_is_unpaid() {
        assertThat(OrderStatusTransition.canTransition(OrderStatus.CREATED, OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.PAID, OrderStatus.CANCELLED)).isTrue();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.READY, OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    void should_route_refunds_through_refunding_when_order_is_being_made() {
        assertThat(OrderStatusTransition.canTransition(OrderStatus.PAID, OrderStatus.REFUNDING)).isTrue();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.ACCEPTED, OrderStatus.REFUNDING)).isTrue();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.READY, OrderStatus.REFUNDING)).isTrue();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.REFUNDING, OrderStatus.REFUNDED)).isTrue();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.REFUNDING, OrderStatus.ACCEPTED)).isTrue();
    }

    @Test
    void should_keep_single_refund_path_when_transition_skips_audit() {
        // 制作前退款也必须走 REFUNDING→REFUNDED，不允许新增 PAID→REFUNDED 旁路
        assertThat(OrderStatusTransition.canTransition(OrderStatus.PAID, OrderStatus.REFUNDED)).isFalse();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.ACCEPTED, OrderStatus.REFUNDED)).isFalse();
    }

    @Test
    void should_reject_refund_from_terminal_status_when_order_is_closed() {
        assertThat(OrderStatusTransition.canTransition(OrderStatus.COMPLETED, OrderStatus.REFUNDING)).isFalse();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.CANCELLED, OrderStatus.REFUNDING)).isFalse();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.REFUNDED, OrderStatus.REFUNDING)).isFalse();
    }

    @Test
    void should_return_false_when_from_or_to_is_null() {
        assertThat(OrderStatusTransition.canTransition(null, OrderStatus.PAID)).isFalse();
        assertThat(OrderStatusTransition.canTransition(OrderStatus.CREATED, null)).isFalse();
    }
}
