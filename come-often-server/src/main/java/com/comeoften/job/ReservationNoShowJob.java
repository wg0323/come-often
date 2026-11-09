package com.comeoften.job;

import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.config.ReservationProperties;
import com.comeoften.module.reservation.converter.ReservationConverter;
import com.comeoften.module.reservation.entity.Reservation;
import com.comeoften.module.reservation.mapper.ReservationMapper;
import com.comeoften.module.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 预约未到店兜底扫描。实时事项仍由延迟消息承担，本任务只补偿消息丢失 / 进死信的情况，
 * 与消费者共用 ReservationService.markNoShow（状态条件 UPDATE 保证不会重复释放）。
 *
 * <p>单实例互斥由 {@link JobLock} 承担，锁 TTL 5 分钟与扫描间隔一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationNoShowJob {

    public static final String JOB_NAME = "reservation-no-show";

    private static final long LOCK_TTL_SECONDS = 300L;

    private final ReservationMapper reservationMapper;

    private final ReservationService reservationService;

    private final ReservationProperties reservationProperties;

    /**
     * 扫描一次超时未签到预约，返回本次置为 NO_SHOW 的条数。
     */
    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult scan() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusMinutes(reservationProperties.getNoShowMinutes());
        List<Reservation> overdue = reservationMapper.selectOverdue(cutoff,
                reservationProperties.getNoShowScanBatchSize());
        int marked = 0;
        for (Reservation entity : overdue) {
            LocalDateTime reserveAt = ReservationConverter.reserveAt(entity);
            if (reserveAt == null) {
                continue;
            }
            try {
                if (reservationService.markNoShow(entity.getReserveNo(),
                        reserveAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())) {
                    marked++;
                }
            } catch (RuntimeException e) {
                log.error("预约未到店兜底处理失败: reserveNo={}", entity.getReserveNo(), e);
            }
        }
        if (!overdue.isEmpty()) {
            log.info("预约未到店兜底扫描完成: scanned={}, marked={}", overdue.size(), marked);
        }
        return JobResult.executed(JOB_NAME, marked);
    }
}
