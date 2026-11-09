package com.comeoften.mq.listener;

import com.comeoften.common.OrderKeys;
import com.comeoften.module.reservation.service.ReservationService;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.message.ReservationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 预约超时未签到消费者。延迟消息到点触发，置 NO_SHOW 与释放容量在同一事务内完成。
 *
 * <p>幂等两层：Redis 按预约单号去重挡住重投；DB 侧以「带旧状态条件的 UPDATE」为最终判据，
 * 即使 Redis 键被清掉也不会重复释放容量。改期后的旧消息按预约时刻不一致直接丢弃。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTimeoutListener {

    private static final long IDEMPOTENT_TTL_HOURS = 48L;

    private final ReservationService reservationService;

    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConst.RESERVATION_TIMEOUT_QUEUE)
    public void handleTimeout(ReservationMessage message) {
        if (message == null || message.getReserveNo() == null) {
            log.warn("预约超时消息缺少预约单号，已忽略");
            return;
        }
        // 幂等键带预约时刻：改期后会为同一单号投递新的超时消息，若只按单号去重，
        // 旧消息先在旧时刻写下的键会把新消息吞掉，导致 NO_SHOW 永不落库、容量永不释放。
        String idemKey = OrderKeys.consumeIdempotent(
                message.getReserveNo() + ":reservation-timeout:" + message.getReserveAtMillis());
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(idemKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        // 只有明确返回 false 才是重复消息；Redis 异常返回 null 时继续走业务，DB 状态条件是最终判据
        if (Boolean.FALSE.equals(first)) {
            log.info("重复预约超时消息已忽略: reserveNo={}", message.getReserveNo());
            return;
        }
        try {
            boolean marked = reservationService.markNoShow(message.getReserveNo(),
                    message.getReserveAtMillis());
            log.info("预约超时消息处理完成: reserveNo={}, marked={}, messageId={}",
                    message.getReserveNo(), marked, message.getMessageId());
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(idemKey);
            log.error("预约超时处理失败: reserveNo={}, messageId={}",
                    message.getReserveNo(), message.getMessageId(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
