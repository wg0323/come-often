package com.comeoften.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.integration.pay.PayGateway;
import com.comeoften.integration.pay.model.PayNotifyResult;
import com.comeoften.integration.pay.model.PayOrder;
import com.comeoften.integration.pay.model.PrepayResult;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.constant.OrderStatusTransition;
import com.comeoften.module.order.converter.OrderConverter;
import com.comeoften.module.order.dto.OrderPayDTO;
import com.comeoften.module.order.entity.OrderItem;
import com.comeoften.module.order.entity.OrderStatusLog;
import com.comeoften.module.order.entity.PaymentRecord;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.OrderItemMapper;
import com.comeoften.module.order.mapper.OrderStatusLogMapper;
import com.comeoften.module.order.mapper.PaymentRecordMapper;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.module.order.util.OrderAmountCalculator;
import com.comeoften.module.order.vo.OrderVO;
import com.comeoften.module.payment.service.PaymentService;
import com.comeoften.mq.MessageProducer;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.message.MqEnvelope;
import com.comeoften.mq.message.OrderEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 支付成功入账。计划 3.4 的回调幂等：先按 out_trade_no 查流水，命中成功即返回，
 * 绝不二次改订单状态；唯一索引 uk_out_trade_no 兜住并发插入；状态写一律带旧状态条件。
 *
 * <p>入账与两条消息（状态变更给订阅消息、PAY_SUCCEEDED 给 M5/M6）在同一事务内经
 * MessageProducer 登记 mq_message(PENDING)，提交后投递，复用 M3 的可靠性三件套。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final int PAY_STATUS_SUCCESS = 1;

    private static final String FIELD_OUT_TRADE_NO = "outTradeNo";

    private static final String FIELD_TRADE_NO = "tradeNo";

    private static final String FIELD_PAY_AMOUNT = "payAmount";

    private final TradeOrderMapper tradeOrderMapper;

    private final PaymentRecordMapper paymentRecordMapper;

    private final OrderItemMapper orderItemMapper;

    private final OrderStatusLogMapper orderStatusLogMapper;

    private final PayGateway payGateway;

    private final MessageProducer messageProducer;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO prepay(Long memberId, Long storeId, String orderNo, OrderPayDTO dto) {
        TradeOrder order = requireOwned(memberId, storeId, orderNo);
        OrderStatus current = OrderStatus.of(order.getStatus());
        if (current == OrderStatus.PAID) {
            return toVO(order);
        }
        OrderStatusTransition.assertAllowed(current, OrderStatus.PAID);
        BigDecimal due = OrderAmountCalculator.firstPayment(order.getPayAmount(), order.getDepositAmount());
        if (dto != null && dto.getPayAmount() != null && dto.getPayAmount().compareTo(due) != 0) {
            throw new BizException(ErrorCode.ORDER_PAY_AMOUNT_INVALID,
                    "本次应付 " + due.toPlainString() + " 元");
        }
        PayOrder payOrder = new PayOrder();
        payOrder.setOutTradeNo(orderNo);
        payOrder.setPayAmount(due);
        payOrder.setSubject("come-often 订单 " + orderNo);
        PrepayResult result = payGateway.prepay(payOrder);
        if (!Boolean.TRUE.equals(result.getSyncPaid())) {
            log.info("支付网关等待异步回调: orderNo={}, gateway={}", orderNo, result.getGateway());
            return toVO(order);
        }
        return applySuccess(parseMockBody(orderNo, result.getTradeNo(), due));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO simulateSuccess(String orderNo) {
        TradeOrder order = requireByOrderNo(orderNo);
        BigDecimal due = OrderAmountCalculator.firstPayment(order.getPayAmount(), order.getDepositAmount());
        String tradeNo = "MOCK_NOTIFY" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return applySuccess(parseMockBody(orderNo, tradeNo, due));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO handleNotify(String body, Map<String, String> headers) {
        Map<String, String> safeHeaders = headers == null ? Collections.emptyMap() : headers;
        if (!payGateway.verifySign(body, safeHeaders)) {
            throw new BizException(ErrorCode.PAY_SIGN_INVALID);
        }
        return applySuccess(payGateway.parseNotify(body, safeHeaders));
    }

    /**
     * 唯一的入账入口。回调重放、Mock 手动触发、预支付同步确认都走这里，规则不复制第二份。
     */
    private OrderVO applySuccess(PayNotifyResult notify) {
        if (notify == null || !StringUtils.hasText(notify.getOutTradeNo())
                || !Boolean.TRUE.equals(notify.getSuccess())) {
            throw new BizException(ErrorCode.PAY_NOTIFY_INVALID, "支付回调缺少订单号或支付未成功");
        }
        TradeOrder order = requireByOrderNo(notify.getOutTradeNo());
        PaymentRecord existing = findRecord(notify.getOutTradeNo());
        if (existing != null && Integer.valueOf(PAY_STATUS_SUCCESS).equals(existing.getStatus())) {
            log.info("支付回调重复，已入账过，直接返回成功: outTradeNo={}", notify.getOutTradeNo());
            return toVO(order);
        }
        BigDecimal due = OrderAmountCalculator.firstPayment(order.getPayAmount(), order.getDepositAmount());
        if (notify.getPayAmount() == null || notify.getPayAmount().compareTo(due) != 0) {
            throw new BizException(ErrorCode.ORDER_PAY_AMOUNT_INVALID,
                    "回调金额与服务端应付 " + due.toPlainString() + " 元不一致");
        }
        if (!saveRecord(order, existing, notify, due)) {
            // 撞 uk_out_trade_no：另一个支付回调已经入账，本事务未写任何数据，重查订单后返回
            return toVO(requireByOrderNo(notify.getOutTradeNo()));
        }
        OrderStatus from = OrderStatus.of(order.getStatus());
        if (from == OrderStatus.PAID) {
            return toVO(order);
        }
        OrderStatusTransition.assertAllowed(from, OrderStatus.PAID);
        LocalDateTime paidAt = notify.getPaidAt() == null ? LocalDateTime.now() : notify.getPaidAt();
        if (!compareAndSetPaid(order, from, paidAt)) {
            // 订单被取消 / 超时等非支付迁移抢走：必须回滚，否则留下 SUCCESS 流水 + CANCELLED 订单，
            // 之后所有重放都会被幂等分支短路，资金再也补不回来。
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "订单状态已变更，支付入账回滚");
        }
        writeLog(order, from, due, notify.getChannel());
        publishStatusChanged(order, from);
        publishPaySucceeded(order);
        return toVO(order);
    }

    /**
     * 先查后插，唯一索引兜底并发；返回 false 表示并发已入账，调用方不得继续改订单状态。
     */
    private boolean saveRecord(TradeOrder order, PaymentRecord existing,
                               PayNotifyResult notify, BigDecimal due) {
        if (existing != null) {
            existing.setStatus(PAY_STATUS_SUCCESS);
            existing.setPayAmount(due);
            existing.setTradeNo(StringUtils.hasText(notify.getTradeNo())
                    ? notify.getTradeNo() : existing.getTradeNo());
            existing.setNotifyTime(LocalDateTime.now());
            paymentRecordMapper.updateById(existing);
            return true;
        }
        PaymentRecord record = new PaymentRecord();
        record.setOutTradeNo(notify.getOutTradeNo());
        record.setTradeNo(StringUtils.hasText(notify.getTradeNo()) ? notify.getTradeNo() : "");
        record.setOrderId(order.getId());
        record.setPayAmount(due);
        record.setPayChannel(StringUtils.hasText(notify.getChannel()) ? notify.getChannel() : "MOCK");
        record.setStatus(PAY_STATUS_SUCCESS);
        record.setNotifyTime(LocalDateTime.now());
        try {
            paymentRecordMapper.insert(record);
            return true;
        } catch (DuplicateKeyException e) {
            log.info("支付流水并发插入撞唯一索引，按已入账处理: outTradeNo={}", notify.getOutTradeNo());
            return false;
        }
    }

    private boolean compareAndSetPaid(TradeOrder order, OrderStatus from, LocalDateTime paidAt) {
        LambdaUpdateWrapper<TradeOrder> update = new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, order.getId())
                .eq(TradeOrder::getStatus, from.name())
                .set(TradeOrder::getStatus, OrderStatus.PAID.name())
                .set(TradeOrder::getPaidTime, paidAt);
        if (tradeOrderMapper.update(null, update) == 0) {
            return false;
        }
        order.setStatus(OrderStatus.PAID.name());
        order.setPaidTime(paidAt);
        return true;
    }

    private void writeLog(TradeOrder order, OrderStatus from, BigDecimal due, String channel) {
        OrderStatusLog record = new OrderStatusLog();
        record.setOrderId(order.getId());
        record.setFromStatus(from == null ? "" : from.name());
        record.setToStatus(OrderStatus.PAID.name());
        record.setOperatorType(OperatorType.SYSTEM.name());
        record.setRemark("支付回调成功，渠道=" + channel + "，本次收款 " + due.toPlainString() + " 元");
        orderStatusLogMapper.insert(record);
    }

    private void publishStatusChanged(TradeOrder order, OrderStatus from) {
        OrderEventMessage message = baseMessage(order, OrderEventMessage.TYPE_STATUS_CHANGED);
        message.setFromStatus(from == null ? "" : from.name());
        message.setToStatus(OrderStatus.PAID.name());
        publish(message, RabbitConst.ORDER_EXCHANGE, RabbitConst.RK_ORDER_NOTIFY);
    }

    private void publishPaySucceeded(TradeOrder order) {
        OrderEventMessage message = baseMessage(order, OrderEventMessage.TYPE_PAY_SUCCEEDED);
        message.setFromStatus("");
        message.setToStatus(OrderStatus.PAID.name());
        publish(message, RabbitConst.ORDER_EXCHANGE, RabbitConst.RK_ORDER_PAID);
    }

    private static OrderEventMessage baseMessage(TradeOrder order, String eventType) {
        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(newMessageId());
        message.setEventType(eventType);
        message.setOrderNo(order.getOrderNo());
        message.setOrderId(order.getId());
        message.setMemberId(order.getMemberId());
        message.setStoreId(order.getStoreId());
        message.setOccurredAtMillis(System.currentTimeMillis());
        return message;
    }

    private void publish(OrderEventMessage message, String exchange, String routingKey) {
        MqEnvelope envelope = new MqEnvelope();
        envelope.setMessageId(message.getMessageId());
        envelope.setExchangeName(exchange);
        envelope.setRoutingKey(routingKey);
        envelope.setDelayMillis(0L);
        envelope.setBody(message);
        messageProducer.publish(envelope);
    }

    private PayNotifyResult parseMockBody(String orderNo, String tradeNo, BigDecimal due) {
        Map<String, Object> payload = new LinkedHashMap<>(3);
        payload.put(FIELD_OUT_TRADE_NO, orderNo);
        payload.put(FIELD_TRADE_NO, tradeNo);
        payload.put(FIELD_PAY_AMOUNT, due.toPlainString());
        try {
            return payGateway.parseNotify(objectMapper.writeValueAsString(payload), Collections.emptyMap());
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.PAY_NOTIFY_INVALID, "Mock 回调报文组装失败", e);
        }
    }

    private PaymentRecord findRecord(String outTradeNo) {
        return paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOutTradeNo, outTradeNo));
    }

    private TradeOrder requireByOrderNo(String orderNo) {
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private TradeOrder requireOwned(Long memberId, Long storeId, String orderNo) {
        TradeOrder order = requireByOrderNo(orderNo);
        if (!memberId.equals(order.getMemberId()) || !storeId.equals(order.getStoreId())) {
            throw new BizException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    private OrderVO toVO(TradeOrder order) {
        return OrderConverter.toVO(order,
                orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())
                        .orderByAsc(OrderItem::getCreateTime)),
                orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, order.getId())
                        .orderByAsc(OrderStatusLog::getCreateTime)));
    }

    private static String newMessageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
