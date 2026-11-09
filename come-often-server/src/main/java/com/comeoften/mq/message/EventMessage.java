package com.comeoften.mq.message;

import lombok.Data;

import java.io.Serializable;

/**
 * 本地消息体基类。信封 MqEnvelope.body 声明为该类型，具体子类由字段上的 @JsonTypeInfo 区分，
 * 这样不同域（订单、券）共用同一张 mq_message 表与同一套补偿重发逻辑。
 */
@Data
public abstract class EventMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 与 mq_message.message_id、CorrelationData.id 一致，用于幂等与死信回溯。 */
    private String messageId;

    /** 事件类型。订单见 OrderEventMessage.TYPE_*，券见 CouponExpireMessage.TYPE_*。 */
    private String eventType;

    /** 发生时间，epoch 毫秒，避免 Jackson 需要额外的 JSR310 模块。 */
    private long occurredAtMillis;
}
