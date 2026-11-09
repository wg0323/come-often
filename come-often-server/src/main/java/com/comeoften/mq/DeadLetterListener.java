package com.comeoften.mq;

import com.comeoften.mq.impl.MessageProducerImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 死信消费者：落库 + 告警，不做业务重试，避免毒消息回灌业务队列。
 * 日志只记消息长度与元信息，不打印消息体原文。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterListener {

    private final MqFailureHandler failureHandler;

    @RabbitListener(queues = RabbitConst.DEAD_QUEUE, containerFactory = "rawRabbitListenerContainerFactory")
    public void handleDeadLetter(Message message) {
        byte[] body = message.getBody();
        int bodyLength = body == null ? 0 : body.length;
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        String reason = xDeath == null ? "unknown" : xDeath.toString();
        String originalQueue = message.getMessageProperties().getReceivedRoutingKey();
        Object messageId = message.getMessageProperties().getHeaders()
                .get(MessageProducerImpl.HEADER_MESSAGE_ID);
        String id = messageId == null ? null : messageId.toString();
        log.error("收到死信消息: messageId={}, reason={}, originalQueue={}, bodyLength={}",
                id, reason, originalQueue, bodyLength);
        failureHandler.handleDeadLetter(id, reason, originalQueue, bodyLength);
    }
}
