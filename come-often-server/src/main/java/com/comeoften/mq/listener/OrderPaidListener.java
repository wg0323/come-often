package com.comeoften.mq.listener;

import com.comeoften.common.OrderKeys;
import com.comeoften.module.coupon.service.CouponGrantService;
import com.comeoften.module.coupon.service.CouponRuleService;
import com.comeoften.module.points.service.PointsAccountService;
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
 * 支付成功事件消费者。M4 做幂等落库，M5 在同一事件上发新人券（首单），M6 把积分抵现的冻结
 * 转为实扣，不另起本地消息表或生产者；消息体仍由 MessageProducer 经 mq_message(PENDING) 投递。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPaidListener {

    private static final long IDEMPOTENT_TTL_HOURS = 24L;

    private final NotifyRecordService notifyRecordService;

    private final CouponGrantService couponGrantService;

    private final CouponRuleService couponRuleService;

    private final PointsAccountService pointsAccountService;

    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConst.ORDER_PAID_QUEUE)
    public void handleOrderPaid(OrderEventMessage message) {
        if (message == null || message.getMessageId() == null) {
            log.warn("支付成功消息缺少 messageId，已忽略");
            return;
        }
        String idemKey = OrderKeys.consumeIdempotent(message.getMessageId() + ":paid");
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(idemKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        // 只有明确返回 false 才是重复消息；Redis 返回 null 时继续走业务，靠数据库幂等兜底
        if (Boolean.FALSE.equals(first)) {
            log.info("重复支付成功消息已忽略: messageId={}", message.getMessageId());
            return;
        }
        try {
            notifyRecordService.savePaySucceededEvent(message);
            // 新人券：同一会员同一模板只发一次，非首单直接返回 0
            couponGrantService.grantNewcomerIfFirstPaidOrder(message.getMemberId(),
                    message.getStoreId(), message.getOrderId());
            // 规则化新客券：与上面的配置型新人券并存，谁先发到同一模板，另一条按次数与幂等键跳过
            couponRuleService.onFirstPaidOrder(message.getMemberId(), message.getStoreId(),
                    message.getOrderId());
            // 积分抵现：冻结转实扣，未用积分或已处理的订单直接返回
            pointsAccountService.confirmUseForOrder(message.getMemberId(), message.getOrderNo());
            log.info("支付成功事件处理完成: orderNo={}, messageId={}",
                    message.getOrderNo(), message.getMessageId());
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(idemKey);
            log.error("支付成功事件处理失败: orderNo={}, messageId={}",
                    message.getOrderNo(), message.getMessageId(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
