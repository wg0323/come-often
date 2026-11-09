package com.comeoften.mq.listener;

import com.comeoften.common.JobKeys;
import com.comeoften.common.OrderKeys;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.message.CouponExpireMessage;
import com.comeoften.mq.service.NotifyRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 券即将过期提醒消费者。消息由领券时经延迟交换机登记，到点投递到 comeoften.coupon.expire.queue，
 * 与订单通知同一套可靠性三件套：本地消息表登记、confirm 确认、Redis SETNX 幂等。
 *
 * <p><b>去重分两层</b>：按 messageId 去重挡住同一条消息的重投；按 {@code userCouponId} 去重
 * （{@code JobKeys.couponExpireReminded}）挡住「延迟消息 + 每小时兜底任务」两条链路对同一张券各提醒一次
 * ——两条链路写的是同一个 {@code message_record}，各用一套键就等于给顾客发两条一样的提醒。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpireListener {

    private static final long IDEMPOTENT_TTL_HOURS = 24L;

    private static final long FALLBACK_TTL_SECONDS = 25L * 3600L;

    private static final long EXTRA_TTL_SECONDS = 3600L;

    private final NotifyRecordService notifyRecordService;

    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitConst.COUPON_EXPIRE_QUEUE)
    public void handleCouponExpire(CouponExpireMessage message) {
        if (message == null || message.getMessageId() == null) {
            log.warn("券过期提醒缺少 messageId，已忽略");
            return;
        }
        String idemKey = OrderKeys.consumeIdempotent(message.getMessageId() + ":coupon-expire");
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(idemKey, "1", IDEMPOTENT_TTL_HOURS, TimeUnit.HOURS);
        if (!Boolean.TRUE.equals(first)) {
            log.info("重复券过期提醒已忽略: messageId={}", message.getMessageId());
            return;
        }
        if (!claimReminder(message)) {
            return;
        }
        try {
            notifyRecordService.saveCouponExpireNotice(message);
            log.info("券过期提醒处理完成: userCouponId={}, messageId={}",
                    message.getUserCouponId(), message.getMessageId());
        } catch (RuntimeException e) {
            // 失败必须回滚两个幂等键，否则容器重试 / 重投都会被挡住
            stringRedisTemplate.delete(idemKey);
            releaseReminder(message);
            log.error("券过期提醒处理失败: userCouponId={}, messageId={}",
                    message.getUserCouponId(), message.getMessageId(), e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    /**
     * 抢「这张券已提醒」的标记。消息没带券 ID 时退化为 messageId 一层去重。
     */
    private boolean claimReminder(CouponExpireMessage message) {
        if (message.getUserCouponId() == null) {
            return true;
        }
        String reminderKey = JobKeys.couponExpireReminded(message.getUserCouponId());
        Boolean claimed = stringRedisTemplate.opsForValue()
                .setIfAbsent(reminderKey, "1", reminderTtlSeconds(message), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(claimed)) {
            log.info("该券已提醒过，跳过（延迟消息与兜底任务共用一把去重键）: userCouponId={}, messageId={}",
                    message.getUserCouponId(), message.getMessageId());
            return false;
        }
        return true;
    }

    private void releaseReminder(CouponExpireMessage message) {
        if (message.getUserCouponId() != null) {
            stringRedisTemplate.delete(JobKeys.couponExpireReminded(message.getUserCouponId()));
        }
    }

    private static long reminderTtlSeconds(CouponExpireMessage message) {
        if (message.getExpireAtMillis() <= 0L) {
            return FALLBACK_TTL_SECONDS;
        }
        long remainingSeconds = (message.getExpireAtMillis() - System.currentTimeMillis()) / 1000L;
        return remainingSeconds > 0L ? remainingSeconds + EXTRA_TTL_SECONDS : FALLBACK_TTL_SECONDS;
    }
}
