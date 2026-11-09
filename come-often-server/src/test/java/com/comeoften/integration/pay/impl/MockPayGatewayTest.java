package com.comeoften.integration.pay.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.integration.pay.model.PayNotifyResult;
import com.comeoften.integration.pay.model.PayOrder;
import com.comeoften.integration.pay.model.PrepayResult;
import com.comeoften.integration.pay.model.RefundOrder;
import com.comeoften.integration.pay.model.RefundResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockPayGatewayTest {

    private MockPayGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new MockPayGateway(new ObjectMapper());
    }

    @Test
    void should_mark_prepay_as_sync_paid_when_gateway_is_mock() {
        PayOrder order = new PayOrder();
        order.setOutTradeNo("CO202611080001");
        order.setPayAmount(new BigDecimal("68.00"));

        PrepayResult result = gateway.prepay(order);

        assertThat(result.getSyncPaid()).isTrue();
        assertThat(result.getGateway()).isEqualTo("MOCK");
        assertThat(result.getOutTradeNo()).isEqualTo("CO202611080001");
        assertThat(result.getPayAmount()).isEqualByComparingTo("68.00");
        assertThat(result.getTradeNo()).startsWith("MOCK");
    }

    @Test
    void should_parse_notify_when_all_fields_present() {
        String body = "{\"outTradeNo\":\"CO1\",\"tradeNo\":\"MOCK1\",\"payAmount\":\"200.00\"}";

        PayNotifyResult result = gateway.parseNotify(body, Collections.emptyMap());

        assertThat(result.getOutTradeNo()).isEqualTo("CO1");
        assertThat(result.getTradeNo()).isEqualTo("MOCK1");
        assertThat(result.getPayAmount()).isEqualByComparingTo("200.00");
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("MOCK");
    }

    @Test
    void should_reject_notify_when_trade_no_is_missing() {
        String body = "{\"outTradeNo\":\"CO1\",\"payAmount\":\"200.00\"}";

        assertThatThrownBy(() -> gateway.parseNotify(body, Collections.emptyMap()))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAY_NOTIFY_INVALID);
    }

    @Test
    void should_reject_notify_when_pay_amount_is_not_numeric() {
        String body = "{\"outTradeNo\":\"CO1\",\"tradeNo\":\"MOCK1\",\"payAmount\":\"abc\"}";

        assertThatThrownBy(() -> gateway.parseNotify(body, Collections.emptyMap()))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAY_NOTIFY_INVALID);
    }

    @Test
    void should_reject_external_notify_when_gateway_is_mock() {
        assertThat(gateway.verifySign("{}", Collections.emptyMap())).isFalse();
    }

    @Test
    void should_return_channel_refund_no_when_refund_is_accepted() {
        RefundOrder order = new RefundOrder();
        order.setRefundNo("RF202611080001");
        order.setRefundAmount(new BigDecimal("68.00"));

        RefundResult result = gateway.refund(order);

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getRefundNo()).isEqualTo("RF202611080001");
        assertThat(result.getChannelRefundNo()).isEqualTo("MOCK_REFUND_RF202611080001");
    }
}
