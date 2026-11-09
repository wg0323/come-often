package com.comeoften.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 路由失败回调。日志只记消息长度，不打印消息体原文。
 *
 * <p><b>延迟交换机的 returned 回调不算失败</b>：`rabbitmq_delayed_message_exchange` 在消息到期前
 * 会先把它返回一次（replyCode 312 NO_ROUTE），到点才真正路由。M3/M4 阶段没有区分这一点，
 * 导致每条延迟消息都被标成 FAILED 并落一条告警，真实的路由失败被淹没（M11 修）。
 * 真实业务交换机（order / member）的 returned 仍然按投递失败处理。</p>
 */
@Slf4j
@Component
public class RabbitReturnsCallback implements RabbitTemplate.ReturnsCallback {

    private static final int REPLY_CODE_NO_ROUTE = 312;

    private final MqFailureHandler failureHandler;

    public RabbitReturnsCallback(MqFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
    }

    @Override
    public void returnedMessage(ReturnedMessage returned) {
        byte[] body = returned.getMessage().getBody();
        int bodyLength = body == null ? 0 : body.length;
        if (RabbitConst.DELAY_EXCHANGE.equals(returned.getExchange())
                && returned.getReplyCode() == REPLY_CODE_NO_ROUTE) {
            // 用 warn 而不是 debug：延迟交换机上的 5 个路由键全部有绑定，这里如果持续出现
            // 说明绑定被删或路由键写错，broker 仍会 ack 并把状态置 SENT，只能在日志里看出来。
            log.warn("延迟消息到期前被 returned（插件行为，保留本地消息表原状态）: routingKey={}, bodyLength={}",
                    returned.getRoutingKey(), bodyLength);
            return;
        }
        log.error("消息路由失败: exchange={}, routingKey={}, replyCode={}, replyText={}, bodyLength={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(),
                returned.getReplyText(), bodyLength);
        failureHandler.saveSendFailure(returned.getMessage().getMessageProperties().getMessageId(),
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
    }
}
