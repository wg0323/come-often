package com.comeoften.job;

import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.mq.MessageProducer;
import com.comeoften.mq.MqFailureHandler;
import com.comeoften.mq.entity.MqMessage;
import com.comeoften.mq.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 未确认消息定时重发（可靠性三件套之一）。扫描超过 pending-timeout 仍为 PENDING 的记录，
 * 重发 3 次仍失败转 FAILED + 告警。
 *
 * <p>单实例互斥由 {@link JobLock} 承担，锁 TTL 55 秒小于每分钟的扫描间隔，避免上一轮异常退出后
 * 长期挡住下一轮；调度入口在 {@code JobScheduler}，运维端点走同一入口，两条路径共享同一把锁。</p>
 */
@Slf4j
@Component
public class MqRetryJob {

    public static final String JOB_NAME = "mq-retry";

    private static final int BATCH_SIZE = 100;

    /**
     * 锁 TTL 取 90 秒：单批 100 条「DB 累加重试次数 + 重新投递」在中间件抖动时可能超过 55 秒，
     * 锁提前过期会让另一实例把同一批再扫一遍、把 retryCount 多推一格提前转 FAILED。
     * 调度是 fixedDelay（上一轮结束 60 秒后才开下一轮），所以 TTL 加大不会挡住本实例的下一轮。
     */
    private static final long LOCK_TTL_SECONDS = 90L;

    private final MqMessageService mqMessageService;

    private final MessageProducer messageProducer;

    private final MqFailureHandler failureHandler;

    private final int maxRetry;

    private final long pendingTimeoutSeconds;

    public MqRetryJob(MqMessageService mqMessageService, MessageProducer messageProducer,
                      MqFailureHandler failureHandler,
                      @Value("${comeoften.mq.max-retry:3}") int maxRetry,
                      @Value("${comeoften.mq.pending-timeout-seconds:60}") long pendingTimeoutSeconds) {
        this.mqMessageService = mqMessageService;
        this.messageProducer = messageProducer;
        this.failureHandler = failureHandler;
        this.maxRetry = maxRetry;
        this.pendingTimeoutSeconds = pendingTimeoutSeconds;
    }

    /**
     * 补偿扫描一次，返回本次处理（重发或转 FAILED）的条数。
     */
    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult resendPending() {
        List<MqMessage> pending = mqMessageService.listPendingForRetry(LocalDateTime.now(), BATCH_SIZE);
        for (MqMessage item : pending) {
            resend(item);
        }
        if (!pending.isEmpty()) {
            log.info("未确认消息补偿扫描完成: count={}", pending.size());
        }
        return JobResult.executed(JOB_NAME, pending.size());
    }

    private void resend(MqMessage item) {
        int retries = item.getRetryCount() == null ? 0 : item.getRetryCount();
        if (retries >= maxRetry) {
            mqMessageService.markFailed(item.getMessageId(), "补偿重发次数已耗尽");
            failureHandler.notifySendFailed(item.getMessageId(),
                    item.getExchangeName(), item.getRoutingKey());
            return;
        }
        mqMessageService.incrementRetry(item.getMessageId(),
                LocalDateTime.now().plusSeconds(pendingTimeoutSeconds));
        log.warn("重发未确认消息: messageId={}, exchange={}, routingKey={}, retryCount={}",
                item.getMessageId(), item.getExchangeName(), item.getRoutingKey(), retries + 1);
        messageProducer.republish(item.getMessageId());
    }
}
