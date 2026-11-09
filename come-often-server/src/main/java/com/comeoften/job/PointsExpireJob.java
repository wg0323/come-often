package com.comeoften.job;

import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.module.points.service.PointsAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 每日积分过期批处理（计划 3.10 的 00:05 任务）。
 *
 * <p>单实例互斥由 {@link JobLock} 承担，锁 TTL 10 分钟覆盖整批处理耗时；调度入口在
 * {@code JobScheduler}，运维端点 {@code POST /api/admin/points/expire/run} 复用同一个方法，
 * 两条路径共享同一把锁，不会并发扣减。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpireJob {

    public static final String JOB_NAME = "points-expire";

    private static final long LOCK_TTL_SECONDS = 600L;

    private final PointsAccountService pointsAccountService;

    /**
     * 执行一次过期批处理。锁被其他实例持有时返回 {@code executed=false}，不做任何扣减。
     */
    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult expireDue() {
        try {
            int expired = pointsAccountService.expireDue();
            log.info("积分过期任务执行完成: 过期积分={}", expired);
            return JobResult.executed(JOB_NAME, expired);
        } catch (RuntimeException e) {
            log.error("积分过期任务执行失败", e);
            return JobResult.failed(JOB_NAME, e.getMessage());
        }
    }
}
