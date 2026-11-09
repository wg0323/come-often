package com.comeoften.job;

import com.comeoften.common.job.JobResult;
import com.comeoften.job.service.JobTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 兜底任务的 cron 接线。这里只负责触发，不含业务逻辑：
 * 实际执行体是被 @JobLock 代理的任务 Bean，通过 {@link JobTriggerService} 调用，
 * 因此 cron 路径与运维补跑路径一定走同一把锁（自调用会绕过代理，所以不把 @Scheduled 写在任务类上）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobTriggerService jobTriggerService;

    /**
     * 未确认消息补偿重发，默认每分钟。
     */
    @Scheduled(fixedDelayString = "${comeoften.mq.retry-interval-ms:60000}")
    public void mqRetry() {
        trigger(MqRetryJob.JOB_NAME);
    }

    /**
     * 积分过期批处理，默认每日 00:05。
     */
    @Scheduled(cron = "${comeoften.points.expire-cron:0 5 0 * * ?}")
    public void pointsExpire() {
        trigger(PointsExpireJob.JOB_NAME);
    }

    /**
     * 预约未到店兜底扫描，默认每 5 分钟。
     */
    @Scheduled(cron = "${comeoften.reservation.no-show-scan-cron:0 */5 * * * ?}")
    public void reservationNoShow() {
        trigger(ReservationNoShowJob.JOB_NAME);
    }

    /**
     * 预约时段滚动预生成，默认每日 00:00（早于 00:10 的库存键重建）。
     */
    @Scheduled(cron = "${comeoften.job.reservation-slot-generate-cron:0 0 0 * * ?}")
    public void reservationSlotGenerate() {
        trigger(ReservationSlotGenerateJob.JOB_NAME);
    }

    /**
     * 三类库存键按 DB 重建，默认每日 00:10。
     */
    @Scheduled(cron = "${comeoften.job.stock-rebuild-cron:0 10 0 * * ?}")
    public void stockRebuild() {
        trigger(StockRebuildJob.JOB_NAME);
    }

    /**
     * 订单自动完成，默认每日 00:05。
     */
    @Scheduled(cron = "${comeoften.job.order-auto-complete-cron:0 5 0 * * ?}")
    public void orderAutoComplete() {
        trigger(OrderAutoCompleteJob.JOB_NAME);
    }

    /**
     * 券过期提醒兜底，默认每小时整点。
     */
    @Scheduled(cron = "${comeoften.job.coupon-expire-cron:0 0 * * * ?}")
    public void couponExpireFallback() {
        trigger(CouponExpireFallbackJob.JOB_NAME);
    }

    private void trigger(String jobName) {
        try {
            JobResult result = jobTriggerService.run(jobName);
            if (result != null && result.isExecuted()) {
                log.info("定时任务执行完成: job={}, count={}, detail={}",
                        result.getJobName(), result.getCount(), result.getDetail());
            }
        } catch (RuntimeException e) {
            log.error("定时任务执行失败: job={}", jobName, e);
        }
    }
}
