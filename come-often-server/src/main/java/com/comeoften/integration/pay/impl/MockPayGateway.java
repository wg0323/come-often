package com.comeoften.integration.pay.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.integration.pay.PayGateway;
import com.comeoften.integration.pay.model.PayNotifyResult;
import com.comeoften.integration.pay.model.PayOrder;
import com.comeoften.integration.pay.model.PrepayResult;
import com.comeoften.integration.pay.model.RefundOrder;
import com.comeoften.integration.pay.model.RefundResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 本地 Mock 网关。prepay 返回假参数并同步确认收款，由 PaymentService 走同一条入账回调；
 * 另外提供 /api/mock/pay/success 手动触发回调，用于验证重放幂等。
 *
 * <p>不启用外部回调验签：verifySign 恒 false，避免 Mock 模式下 /api/pay/notify 被伪造刷单。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "comeoften.pay.gateway", havingValue = "mock", matchIfMissing = true)
public class MockPayGateway implements PayGateway {

    private static final String CHANNEL = "MOCK";

    private static final String TRADE_NO_PREFIX = "MOCK";

    private static final String PREPAY_ID_PREFIX = "MOCK_PREPAY_";

    private static final String CHANNEL_REFUND_PREFIX = "MOCK_REFUND_";

    private static final String FIELD_OUT_TRADE_NO = "outTradeNo";

    private static final String FIELD_TRADE_NO = "tradeNo";

    private static final String FIELD_PAY_AMOUNT = "payAmount";

    private final ObjectMapper objectMapper;

    @Override
    public PrepayResult prepay(PayOrder order) {
        PrepayResult result = new PrepayResult();
        result.setGateway(CHANNEL);
        result.setOutTradeNo(order.getOutTradeNo());
        result.setTradeNo(TRADE_NO_PREFIX + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        result.setPrepayId(PREPAY_ID_PREFIX + order.getOutTradeNo());
        result.setPayAmount(order.getPayAmount());
        result.setSyncPaid(Boolean.TRUE);
        log.info("Mock 预下单完成: outTradeNo={}, payAmount={}", order.getOutTradeNo(), order.getPayAmount());
        return result;
    }

    @Override
    public RefundResult refund(RefundOrder order) {
        RefundResult result = new RefundResult();
        result.setRefundNo(order.getRefundNo());
        result.setChannelRefundNo(CHANNEL_REFUND_PREFIX + order.getRefundNo());
        result.setSuccess(Boolean.TRUE);
        log.info("Mock 渠道退款受理成功: refundNo={}, refundAmount={}",
                order.getRefundNo(), order.getRefundAmount());
        return result;
    }

    @Override
    public PayNotifyResult parseNotify(String body, Map<String, String> headers) {
        try {
            JsonNode root = objectMapper.readTree(body);
            PayNotifyResult result = new PayNotifyResult();
            result.setOutTradeNo(text(root, FIELD_OUT_TRADE_NO));
            result.setTradeNo(text(root, FIELD_TRADE_NO));
            result.setPayAmount(new BigDecimal(text(root, FIELD_PAY_AMOUNT)));
            result.setChannel(CHANNEL);
            result.setSuccess(Boolean.TRUE);
            result.setPaidAt(LocalDateTime.now());
            return result;
        } catch (IOException | NumberFormatException | NullPointerException e) {
            throw new BizException(ErrorCode.PAY_NOTIFY_INVALID, "Mock 回调报文无法解析", e);
        }
    }

    @Override
    public boolean verifySign(String body, Map<String, String> headers) {
        log.warn("Mock 网关不接受外部支付回调，请改用 /api/mock/pay/success");
        return false;
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !org.springframework.util.StringUtils.hasText(node.asText())) {
            throw new BizException(ErrorCode.PAY_NOTIFY_INVALID, "Mock 回调缺少字段：" + field);
        }
        return node.asText();
    }
}
