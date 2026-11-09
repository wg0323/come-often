package com.comeoften.mq.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.io.Serializable;

/**
 * 本地消息表的 payload 信封。表结构没有延迟时间列，delayMillis 随信封一起落库，
 * 补偿重发时才能原样保留延迟消息（不会重发成即时消息）。
 *
 * <p>body 的多态信息只写在字段上：消息本身作为根对象投递给 RabbitMQ 时不带 bodyType，
 * 消费端按 __TypeId__ 头还原具体类型，历史 payload 缺 bodyType 时默认按订单事件解析。</p>
 */
@Data
public class MqEnvelope implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;

    private String exchangeName;

    private String routingKey;

    private long delayMillis;

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "bodyType", defaultImpl = OrderEventMessage.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = OrderEventMessage.class, name = "ORDER"),
            @JsonSubTypes.Type(value = CouponExpireMessage.class, name = "COUPON"),
            @JsonSubTypes.Type(value = ReservationMessage.class, name = "RESERVATION")
    })
    private EventMessage body;
}
