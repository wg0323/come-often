package com.comeoften.mq.listener;

import com.comeoften.common.OrderKeys;
import com.comeoften.module.merchant.service.MerchantNotifyService;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.message.OrderEventMessage;
import com.comeoften.mq.service.NotifyRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 订单状态变更通知消费者。默认落 message_record + 日志（LogNotifySender），
 * 不同步阻塞下单主流程；真实订阅消息在拿到模板 ID 后替换 NotifyRecordService 实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotifyListener {

    private static final long IDEMPOTENT_TTL_HOURS = 24L;

    private final NotifyRecordService notifyRecordService;

    private final MerchantNotifyService merchantNotifyService;

    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConst.ORDER_NOTIFY_QUEUE)
    public void handleOrderNotify(OrderEventMessage message) {
        if (message == null || message.getMessageId() == null) {
            log.warn("订单通知消息缺少 messageId，已忽略");
            return;
        }
        String idemKey = OrderKeys.consumeIdempotent(message.getMessageId() + ":notify");
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(idemKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(first)) {
            log.info("重复订单通知消息已忽略: messageId={}", message.getMessageId());
            return;
        }
        try {
            notifyRecordService.saveOrderStatusNotice(message);
            remindMerchantIfNewOrder(message);
            log.info("订单状态通知处理完成: orderNo={}, toStatus={}, messageId={}",
                    message.getOrderNo(), message.getToStatus(), message.getMessageId());
        } catch (RuntimeException e) {
            // 失败必须回滚幂等键，否则容器重试 / 重投会被幂等键挡住
            stringRedisTemplate.delete(idemKey);
            log.error("订单状态通知处理失败: orderNo={}, messageId={}",
                    message.getOrderNo(), message.getMessageId(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    /**
     * 支付成功即视为新订单：商家端在后台时只能靠自己的订阅消息。
     * 提醒失败不影响顾客端通知，也不让整条消息进死信。
     */
    private void remindMerchantIfNewOrder(OrderEventMessage message) {
        if (!OrderStatus.PAID.name().equals(message.getToStatus())) {
            return;
        }
        try {
            merchantNotifyService.remindNewOrder(message.getStoreId(), message.getOrderNo(), null);
        } catch (RuntimeException e) {
            log.warn("商家端新订单提醒失败，已忽略: orderNo={}", message.getOrderNo(), e);
        }
    }
}
