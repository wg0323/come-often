package com.comeoften.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.module.queue.constant.QueueTicketStatus;
import com.comeoften.module.queue.entity.QueueTicket;
import com.comeoften.module.queue.mapper.QueueTicketMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 过号兜底（M14 B2）：{@code biz_date} 早于今天、状态还停在 {@code WAITING} / {@code CALLED} 的排队号置 {@code EXPIRED}。
 *
 * <p><b>为什么必须有这个任务：</b>M13 的 {@code EXPIRED} 状态只有枚举，没有任何写入点。
 * 顾客取号后不取消就走人，号会永久占着「当天有效号」的唯一判定，第二天再来取号仍然命中「今日已有排队号」之外的脏数据，
 * 后台排队看板也会永远挂着一批昨天的等待号。</p>
 *
 * <p><b>幂等口径：</b>状态推进用带旧状态条件的 UPDATE（{@code status IN (WAITING, CALLED) → EXPIRED}），
 * 重跑时已过期记录不再命中，天然不重复写；不需要额外的 Redis 去重键。
 * {@code queue_ticket} 没有独立的状态日志表，判断依据就是 {@code update_time}，所以也不存在重复日志的问题。</p>
 *
 * <p>一次全局扫描（不按门店循环）：单店日级数据量，索引 {@code idx_store_status} 足够；
 * 单条失败只跳过该条，不影响同批其他号。</p>
 */
@Slf4j
@Component
public class QueueExpireJob {

    public static final String JOB_NAME = "queue-expire";

    private static final long LOCK_TTL_SECONDS = 300L;

    private static final List<String> EXPIREABLE_STATUS = Arrays.asList(
            QueueTicketStatus.WAITING.name(), QueueTicketStatus.CALLED.name());

    private final QueueTicketMapper queueTicketMapper;

    private final int batchSize;

    public QueueExpireJob(QueueTicketMapper queueTicketMapper,
                          @Value("${comeoften.job.queue-expire-batch-size:200}") int batchSize) {
        this.queueTicketMapper = queueTicketMapper;
        this.batchSize = batchSize;
    }

    /**
     * 扫描一次非当天的等待中 / 已叫号排队号，返回本次置为 EXPIRED 的条数。
     */
    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult expire() {
        LocalDate today = LocalDate.now();
        List<QueueTicket> stale = queueTicketMapper.selectList(new LambdaQueryWrapper<QueueTicket>()
                .lt(QueueTicket::getBizDate, today)
                .in(QueueTicket::getStatus, EXPIREABLE_STATUS)
                .orderByAsc(QueueTicket::getId)
                .last("LIMIT " + batchSize));
        int expired = 0;
        for (QueueTicket ticket : stale) {
            int rows = queueTicketMapper.update(null, new LambdaUpdateWrapper<QueueTicket>()
                    .eq(QueueTicket::getId, ticket.getId())
                    .in(QueueTicket::getStatus, EXPIREABLE_STATUS)
                    .set(QueueTicket::getStatus, QueueTicketStatus.EXPIRED.name()));
            if (rows > 0) {
                expired++;
            }
        }
        log.info("排队过号扫描结束: bizDate={}, scanned={}, expired={}", today, stale.size(), expired);
        return JobResult.executed(JOB_NAME, expired,
                "scanned=" + stale.size() + ",expired=" + expired);
    }
}
