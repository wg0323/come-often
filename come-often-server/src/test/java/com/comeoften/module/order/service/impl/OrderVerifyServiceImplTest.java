package com.comeoften.module.order.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.entity.OrderVerifyRecord;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.OrderVerifyRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 核销审计：普通单 #PICKUP、套餐单 #BUNDLE、渠道映射、非法码与重复核销。
 */
@ExtendWith(MockitoExtension.class)
class OrderVerifyServiceImplTest {

    private static final Long STORE_ID = 1L;

    private static final Long ORDER_ID = 8001L;

    private static final String ORDER_NO = "CO20261110120000ABCDEF";

    @Mock
    private OrderVerifyRecordMapper orderVerifyRecordMapper;

    @InjectMocks
    private OrderVerifyServiceImpl orderVerifyService;

    @Test
    void should_write_pickup_biz_id_when_normal_order_completed() {
        orderVerifyService.record(order(null), OperatorType.MERCHANT, 9L);

        ArgumentCaptor<OrderVerifyRecord> captor = ArgumentCaptor.forClass(OrderVerifyRecord.class);
        verify(orderVerifyRecordMapper).insert(captor.capture());
        OrderVerifyRecord saved = captor.getValue();
        assertThat(saved.getBizId()).isEqualTo(ORDER_NO + "#PICKUP");
        assertThat(saved.getVerifyCode()).isEqualTo("A001");
        assertThat(saved.getVerifyChannel()).isEqualTo("MERCHANT_SCAN");
        assertThat(saved.getOperatorType()).isEqualTo("MERCHANT");
        assertThat(saved.getOperatorId()).isEqualTo(9L);
        assertThat(saved.getRemark()).isEqualTo("取餐码核销");
    }

    @Test
    void should_write_bundle_biz_id_when_bundle_order_completed() {
        orderVerifyService.record(order(77L), OperatorType.ADMIN, 5L);

        ArgumentCaptor<OrderVerifyRecord> captor = ArgumentCaptor.forClass(OrderVerifyRecord.class);
        verify(orderVerifyRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getBizId()).isEqualTo(ORDER_NO + "#BUNDLE");
        assertThat(captor.getValue().getVerifyChannel()).isEqualTo("ADMIN_INPUT");
        assertThat(captor.getValue().getRemark()).isEqualTo("团购套餐核销");
    }

    @Test
    void should_skip_audit_without_blocking_when_verify_code_invalid() {
      TradeOrder invalid = order(null);
      invalid.setPickupCode("BAD-CODE");

      // 空 / 脏码订单必须仍能完成，审计只跳过不抛异常
      orderVerifyService.record(invalid, OperatorType.MERCHANT, 9L);

      verify(orderVerifyRecordMapper, never()).insert(any(OrderVerifyRecord.class));
    }

    @Test
    void should_reject_duplicate_verification() {
        when(orderVerifyRecordMapper.insert(any(OrderVerifyRecord.class)))
                .thenThrow(new DuplicateKeyException("duplicate biz_id"));

        BizException ex = catchThrowableOfType(
                () -> orderVerifyService.record(order(null), OperatorType.MERCHANT, 9L), BizException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ORDER_ALREADY_VERIFIED);
    }

    private static TradeOrder order(Long bundleId) {
        TradeOrder order = new TradeOrder();
        order.setId(ORDER_ID);
        order.setStoreId(STORE_ID);
        order.setOrderNo(ORDER_NO);
        order.setPickupCode("A001");
        order.setBundleId(bundleId);
        return order;
    }
}
