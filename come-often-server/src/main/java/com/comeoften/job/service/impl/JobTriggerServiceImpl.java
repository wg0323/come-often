package com.comeoften.job.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.job.JobResult;
import com.comeoften.job.CouponExpireFallbackJob;
import com.comeoften.job.CouponRuleJob;
import com.comeoften.job.MemberTagJob;
import com.comeoften.job.MqRetryJob;
import com.comeoften.job.OrderAutoCompleteJob;
import com.comeoften.job.PointsExpireJob;
import com.comeoften.job.PresaleShelfJob;
import com.comeoften.job.QueueExpireJob;
import com.comeoften.job.ReservationNoShowJob;
import com.comeoften.job.ReservationSlotGenerateJob;
import com.comeoften.job.StockRebuildJob;
import com.comeoften.job.service.JobTriggerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 任务名到执行入口的注册表。名字即 @JobLock 的 key，也是锁键后缀，
 * 所以「调度 / 手动补跑 / 并发保护」三者天然对得上。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobTriggerServiceImpl implements JobTriggerService {

    private final MqRetryJob mqRetryJob;

    private final PointsExpireJob pointsExpireJob;

    private final ReservationNoShowJob reservationNoShowJob;

    private final ReservationSlotGenerateJob reservationSlotGenerateJob;

    private final StockRebuildJob stockRebuildJob;

    private final OrderAutoCompleteJob orderAutoCompleteJob;

    private final CouponExpireFallbackJob couponExpireFallbackJob;

    private final QueueExpireJob queueExpireJob;

    private final MemberTagJob memberTagJob;

    private final CouponRuleJob couponRuleJob;

    private final PresaleShelfJob presaleShelfJob;

    private final Map<String, Supplier<JobResult>> handlers = new LinkedHashMap<>();

    @PostConstruct
    void initHandlers() {
        handlers.put(MqRetryJob.JOB_NAME, mqRetryJob::resendPending);
        handlers.put(PointsExpireJob.JOB_NAME, pointsExpireJob::expireDue);
        handlers.put(ReservationNoShowJob.JOB_NAME, reservationNoShowJob::scan);
        handlers.put(ReservationSlotGenerateJob.JOB_NAME, reservationSlotGenerateJob::generate);
        handlers.put(StockRebuildJob.JOB_NAME, stockRebuildJob::rebuild);
        handlers.put(OrderAutoCompleteJob.JOB_NAME, orderAutoCompleteJob::complete);
        handlers.put(CouponExpireFallbackJob.JOB_NAME, couponExpireFallbackJob::remind);
        handlers.put(QueueExpireJob.JOB_NAME, queueExpireJob::expire);
        handlers.put(MemberTagJob.JOB_NAME, memberTagJob::refresh);
        handlers.put(CouponRuleJob.JOB_NAME, couponRuleJob::grant);
        handlers.put(PresaleShelfJob.JOB_NAME, presaleShelfJob::refresh);
    }

    @Override
    public JobResult run(String jobName) {
        Supplier<JobResult> handler = handlers.get(jobName);
        if (handler == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "未知任务：" + jobName);
        }
        return handler.get();
    }

    @Override
    public List<String> names() {
        return Collections.unmodifiableList(new ArrayList<>(handlers.keySet()));
    }
}
