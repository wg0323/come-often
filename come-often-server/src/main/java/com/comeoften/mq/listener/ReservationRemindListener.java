package com.comeoften.mq.listener;

import com.comeoften.common.OrderKeys;
import com.comeoften.module.reservation.service.ReservationService;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.message.ReservationMessage;
import com.comeoften.mq.service.NotifyRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 预约到店提醒消费者。提前 1 小时经延迟交换机触发，真实订阅消息模板未接入前落 message_record。
 * 预约已取消、已签到或已改期时静默丢弃。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationRemindListener {

    private static final long IDEMPOTENT_TTL_HOURS = 48L;

    private final ReservationService reservationService;

    private final NotifyRecordService notifyRecordService;

    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConst.RESERVATION_REMIND_QUEUE)
    public void handleRemind(ReservationMessage message) {
        if (message == null || message.getReserveNo() == null) {
            log.warn("预约提醒消息缺少预约单号，已忽略");
            return;
        }
        // 幂等键带预约时刻：改期后新提醒不能被旧提醒写下的键吞掉
        String idemKey = OrderKeys.consumeIdempotent(
                message.getReserveNo() + ":reservation-remind:" + message.getReserveAtMillis());
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(idemKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(first)) {
            log.info("重复预约提醒消息已忽略: reserveNo={}", message.getReserveNo());
            return;
        }
        try {
            if (!reservationService.shouldRemind(message.getReserveNo(), message.getReserveAtMillis())) {
                log.info("预约已失效或已改期，跳过提醒: reserveNo={}", message.getReserveNo());
                return;
            }
            notifyRecordService.saveReservationRemindNotice(message);
            log.info("预约提醒处理完成: reserveNo={}, messageId={}",
                    message.getReserveNo(), message.getMessageId());
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(idemKey);
            log.error("预约提醒处理失败: reserveNo={}, messageId={}",
                    message.getReserveNo(), message.getMessageId(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
