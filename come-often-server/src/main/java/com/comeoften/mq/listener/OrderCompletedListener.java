package com.comeoften.mq.listener;

import com.comeoften.common.OrderKeys;
import com.comeoften.module.points.service.PointsAccountService;
import com.comeoften.module.points.service.PointsLevelService;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.message.OrderEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 订单完成事件消费者：发消费积分 + 重算会员等级。
 *
 * <p>幂等两层：Redis 按 messageId 去重挡住重投；points_record 的
 * uk_points_idempotent(member_id, biz_type='ORDER', biz_id='{orderNo}#EARN') 是最终判据，
 * 即使 Redis 键被清掉或过期，同一订单也只会落一条积分流水。</p>
 *
 * <p>等级在同一个消费者里算，不另开定时任务（计划 3.7）；升级失败整条消息进死信可人工重放。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedListener {

    private static final long IDEMPOTENT_TTL_HOURS = 24L;

    private final PointsAccountService pointsAccountService;

    private final PointsLevelService pointsLevelService;

    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConst.POINTS_COMPLETED_QUEUE)
    public void handleOrderCompleted(OrderEventMessage message) {
        if (message == null || message.getMessageId() == null) {
            log.warn("订单完成消息缺少 messageId，已忽略");
            return;
        }
        String idemKey = OrderKeys.consumeIdempotent(message.getMessageId() + ":completed");
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(idemKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        // 只有明确返回 false 才是重复消息；Redis 异常返回 null 时继续走业务，
        // 幂等的最终判据仍是 points_record 的唯一索引
        if (Boolean.FALSE.equals(first)) {
            log.info("重复订单完成消息已忽略: messageId={}", message.getMessageId());
            return;
        }
        try {
            int points = pointsAccountService.earnForCompletedOrder(message.getOrderNo());
            pointsLevelService.refreshLevel(message.getMemberId());
            log.info("订单完成事件处理完成: orderNo={}, points={}, messageId={}",
                    message.getOrderNo(), points, message.getMessageId());
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(idemKey);
            log.error("订单完成事件处理失败: orderNo={}, messageId={}",
                    message.getOrderNo(), message.getMessageId(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
