package com.comeoften.mq.listener;

import com.comeoften.common.OrderKeys;
import com.comeoften.module.order.service.OrderService;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.message.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 待支付超时取消消费者。延迟消息到点触发，取消与库存回补由 OrderService 在同一事务内完成。
 * 重复投递（下单自动消息与运维补发消息并存）按 messageId 幂等，订单状态再兜一层。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutListener {

    private static final long IDEMPOTENT_TTL_HOURS = 24L;

    private final OrderService orderService;

    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConst.ORDER_TIMEOUT_QUEUE)
    public void handleOrderTimeout(OrderEventMessage message) {
        if (message == null || message.getMessageId() == null || message.getOrderNo() == null) {
            log.warn("订单超时消息缺少必要字段，已忽略");
            return;
        }
        // 幂等键按订单而非 messageId：下单自动消息与运维补发消息 messageId 不同，
        // 但同一订单只允许一次超时取消，状态条件更新再兜一层
        String idemKey = OrderKeys.consumeIdempotent(message.getOrderNo() + ":timeout-cancel");
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(idemKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(first)) {
            log.info("重复订单超时消息已忽略: messageId={}", message.getMessageId());
            return;
        }
        try {
            boolean cancelled = orderService.cancelOnTimeout(message.getOrderNo(), message.getMessageId());
            log.info("订单超时消息处理完成: orderNo={}, cancelled={}, messageId={}",
                    message.getOrderNo(), cancelled, message.getMessageId());
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(idemKey);
            log.error("订单超时取消处理失败: orderNo={}, messageId={}",
                    message.getOrderNo(), message.getMessageId(), e);
            throw e;
        }
    }
}
