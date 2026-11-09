package com.comeoften.mq.impl;

import com.comeoften.mq.CorrelationDataWithMessage;
import com.comeoften.mq.MessageProducer;
import com.comeoften.mq.message.MqEnvelope;
import com.comeoften.mq.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 投递不丢：业务事务内写 mq_message(PENDING) → afterCommit 投递 → confirm 成功置 SENT。
 * 投递动作本身失败（如 RabbitMQ 停服）时把记录重置为 PENDING，交给每分钟补偿任务重发。
 */
@Slf4j
@Component
public class MessageProducerImpl implements MessageProducer {

    public static final String HEADER_MESSAGE_ID = "x-comeoften-message-id";

    private static final long MAX_DELAY_MILLIS = Integer.MAX_VALUE;

    private final RabbitTemplate rabbitTemplate;

    private final MqMessageService mqMessageService;

    private final long pendingTimeoutSeconds;

    public MessageProducerImpl(RabbitTemplate rabbitTemplate, MqMessageService mqMessageService,
                               @Value("${comeoften.mq.pending-timeout-seconds:60}") long pendingTimeoutSeconds) {
        this.rabbitTemplate = rabbitTemplate;
        this.mqMessageService = mqMessageService;
        this.pendingTimeoutSeconds = pendingTimeoutSeconds;
    }

    @Override
    public void publish(MqEnvelope envelope) {
        mqMessageService.record(envelope);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch(envelope);
                }
            });
            return;
        }
        dispatch(envelope);
    }

    @Override
    public void republish(String messageId) {
        MqEnvelope envelope = mqMessageService.load(messageId);
        if (envelope == null) {
            log.warn("补偿重发跳过：本地消息不存在或消息体不可解析: messageId={}", messageId);
            return;
        }
        dispatch(envelope);
    }

    private void dispatch(MqEnvelope envelope) {
        try {
            MessagePostProcessor processor = buildPostProcessor(envelope);
            CorrelationDataWithMessage correlationData = new CorrelationDataWithMessage(
                    envelope.getMessageId(), envelope.getExchangeName(), envelope.getRoutingKey(),
                    envelope.getBody(), processor);
            rabbitTemplate.convertAndSend(envelope.getExchangeName(), envelope.getRoutingKey(),
                    envelope.getBody(), processor, correlationData);
            log.info("消息已投递: messageId={}, exchange={}, routingKey={}, delayMillis={}",
                    envelope.getMessageId(), envelope.getExchangeName(),
                    envelope.getRoutingKey(), envelope.getDelayMillis());
        } catch (RuntimeException e) {
            log.error("消息投递异常，保留 PENDING 等待补偿: messageId={}, exchange={}, routingKey={}",
                    envelope.getMessageId(), envelope.getExchangeName(), envelope.getRoutingKey(), e);
            mqMessageService.markPending(envelope.getMessageId(), e.getMessage(),
                    LocalDateTime.now().plusSeconds(pendingTimeoutSeconds));
        }
    }

    private static MessagePostProcessor buildPostProcessor(MqEnvelope envelope) {
        return message -> {
            message.getMessageProperties().setMessageId(envelope.getMessageId());
            message.getMessageProperties().setCorrelationId(envelope.getMessageId());
            message.getMessageProperties().setHeader(HEADER_MESSAGE_ID, envelope.getMessageId());
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            if (envelope.getDelayMillis() > 0L) {
                int delay = (int) Math.min(envelope.getDelayMillis(), MAX_DELAY_MILLIS);
                message.getMessageProperties().setDelay(delay);
            }
            return message;
        };
    }
}
