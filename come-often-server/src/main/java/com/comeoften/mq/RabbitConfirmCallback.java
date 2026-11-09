package com.comeoften.mq;

import com.comeoften.mq.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 发布确认回调。ack=true 把本地消息表置 SENT；ack=false 最多重发 3 次（保留延迟处理器），
 * 达到上限交给 MqFailureHandler 落库并告警。
 */
@Slf4j
@Component
public class RabbitConfirmCallback implements RabbitTemplate.ConfirmCallback {

    private final RabbitTemplate rabbitTemplate;

    private final MqFailureHandler failureHandler;

    private final MqMessageService mqMessageService;

    private final long pendingTimeoutSeconds;

    private final int maxRetry;

    public RabbitConfirmCallback(@Lazy RabbitTemplate rabbitTemplate,
                                 MqFailureHandler failureHandler,
                                 MqMessageService mqMessageService,
                                 @Value("${comeoften.mq.pending-timeout-seconds:60}") long pendingTimeoutSeconds,
                                 @Value("${comeoften.mq.max-retry:3}") int maxRetry) {
        this.rabbitTemplate = rabbitTemplate;
        this.failureHandler = failureHandler;
        this.mqMessageService = mqMessageService;
        this.pendingTimeoutSeconds = pendingTimeoutSeconds;
        this.maxRetry = maxRetry;
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String messageId = correlationData == null ? null : correlationData.getId();
        if (ack) {
            if (messageId != null) {
                mqMessageService.markSent(messageId);
            }
            return;
        }
        if (!(correlationData instanceof CorrelationDataWithMessage)) {
            log.error("消息投递失败且缺少重发上下文: cause={}", cause);
            return;
        }
        CorrelationDataWithMessage cd = (CorrelationDataWithMessage) correlationData;
        int retry = cd.getRetryCount().incrementAndGet();
        if (retry <= maxRetry) {
            log.warn("消息投递失败，第 {} 次重发: id={}, exchange={}, routingKey={}, cause={}",
                    retry, cd.getId(), cd.getExchange(), cd.getRoutingKey(), cause);
            mqMessageService.incrementRetry(cd.getId(), LocalDateTime.now().plusSeconds(pendingTimeoutSeconds));
            try {
                rabbitTemplate.convertAndSend(cd.getExchange(), cd.getRoutingKey(), cd.getPayload(),
                        cd.getMessagePostProcessor(), cd);
            } catch (RuntimeException e) {
                log.error("消息重发异常，保留 PENDING 等待补偿: id={}", cd.getId(), e);
                mqMessageService.markPending(cd.getId(), e.getMessage(),
                        LocalDateTime.now().plusSeconds(pendingTimeoutSeconds));
            }
            return;
        }
        log.error("消息重发 {} 次仍失败，转兜底: id={}, exchange={}, routingKey={}",
                maxRetry, cd.getId(), cd.getExchange(), cd.getRoutingKey());
        failureHandler.saveSendFailure(cd.getId(), cd.getExchange(), cd.getRoutingKey(), cause);
        failureHandler.notifySendFailed(cd.getId(), cd.getExchange(), cd.getRoutingKey());
    }
}
