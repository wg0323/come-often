package com.comeoften.module.merchant.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.auth.service.PermissionService;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.service.OrderService;
import com.comeoften.module.order.vo.OrderVO;
import com.comeoften.module.payment.service.RefundService;
import com.comeoften.module.payment.vo.RefundVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantOrderServiceImplTest {

    private static final Long STORE_ID = 1L;

    private static final Long OPERATOR_ID = 2L;

    private static final String ORDER_NO = "CO20261109120000000001";

    @Mock
    private OrderService orderService;

    @Mock
    private RefundService refundService;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private MerchantOrderServiceImpl merchantOrderService;

    @Test
    void should_reject_cancel_when_merchant_advances_order() {
        assertThatThrownBy(() -> merchantOrderService.advance(STORE_ID, ORDER_NO, "CANCELLED", "", OPERATOR_ID))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_STATUS_INVALID);
        verify(orderService, never()).transition(any(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void should_accept_lowercase_target_when_merchant_advances_order() {
        when(orderService.transition(eq(STORE_ID), eq(ORDER_NO), eq("ACCEPTED"), anyString(),
                eq(OperatorType.MERCHANT), eq(OPERATOR_ID))).thenReturn(new OrderVO());
        OrderVO result = merchantOrderService.advance(STORE_ID, ORDER_NO, " accepted ", "remark", OPERATOR_ID);
        assertThat(result).isNotNull();
        verify(orderService).transition(STORE_ID, ORDER_NO, "ACCEPTED", "remark", OperatorType.MERCHANT,
                OPERATOR_ID);
    }

    @Test
    void should_reject_refund_when_staff_is_waiter() {
        when(permissionService.listRoleCodes(OPERATOR_ID)).thenReturn(Collections.singletonList("WAITER"));
        assertThatThrownBy(() -> merchantOrderService.refund(STORE_ID, ORDER_NO, "reason", OPERATOR_ID))
                .isInstanceOf(BizException.class)
                .extracting(error -> ((BizException) error).getErrorCode())
                .isEqualTo(ErrorCode.PERMISSION_DENIED);
        verify(refundService, never()).applyByAdmin(any(), anyString(), any(), any());
    }

    @Test
    void should_allow_refund_when_staff_is_store_manager() {
        when(permissionService.listRoleCodes(OPERATOR_ID)).thenReturn(Collections.singletonList("STORE_MANAGER"));
        when(refundService.applyByAdmin(eq(STORE_ID), eq(ORDER_NO), any(), eq(OPERATOR_ID)))
                .thenReturn(new RefundVO());
        RefundVO result = merchantOrderService.refund(STORE_ID, ORDER_NO, "reason", OPERATOR_ID);
        assertThat(result).isNotNull();
    }
}
