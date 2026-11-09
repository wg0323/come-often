package com.comeoften.mq;

import com.comeoften.mq.message.MqEnvelope;

/**
 * 消息发送统一入口。业务事务内登记本地消息表，事务提交后才真正投递 RabbitMQ。
 */
public interface MessageProducer {

    /**
     * 登记并发送。必须在业务事务内调用；无事务时立即投递。
     */
    void publish(MqEnvelope envelope);

    /**
     * 从本地消息表还原信封并重新投递，供补偿任务与人工重放入口使用。
     */
    void republish(String messageId);
}
