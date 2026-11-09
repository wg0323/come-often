package com.comeoften.mq;

import lombok.Getter;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 保存投递上下文，供 ack=false 时重发。延迟消息的 MessagePostProcessor 必须保留，
 * 否则重发会丢掉 setDelay 变成立即投递。
 */
@Getter
public class CorrelationDataWithMessage extends CorrelationData {

    private static final MessagePostProcessor NO_OP = message -> message;

    private final String exchange;
    private final String routingKey;
    private final Object payload;
    private final MessagePostProcessor messagePostProcessor;
    private final AtomicInteger retryCount = new AtomicInteger(0);

    public CorrelationDataWithMessage(String id, String exchange, String routingKey, Object payload) {
        this(id, exchange, routingKey, payload, null);
    }

    public CorrelationDataWithMessage(String id, String exchange, String routingKey, Object payload,
                                      MessagePostProcessor messagePostProcessor) {
        super(id);
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.payload = payload;
        this.messagePostProcessor = messagePostProcessor == null ? NO_OP : messagePostProcessor;
    }
}
