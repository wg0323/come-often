package com.comeoften.job;

import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.module.dish.mapper.DishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 预售定时开售 / 结束下架（M14 B4）。只处理配了预售时间窗的菜品，普通菜品的 status 完全由后台人工控制。
 *
 * <p>幂等靠条件 UPDATE（{@code status = 0/1} 才改），重跑 opened/closed 都是 0；
 * 与 queue-expire 一样不引入额外去重键。cron 每分钟跑一次，让开售时间的误差不超过一分钟。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresaleShelfJob {

    public static final String JOB_NAME = "presale-shelf";

    private static final long LOCK_TTL_SECONDS = 120L;

    /** 扫描窗口：cron 每分钟跑一次，取 2 分钟保证与上一轮有重叠、不漏边界，又不会整段预售期反复覆盖人工上下架。 */
    private static final long SCAN_WINDOW_MINUTES = 2L;

    private final DishMapper dishMapper;

    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult refresh() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusMinutes(SCAN_WINDOW_MINUTES);
        int opened = dishMapper.openPresale(now, since);
        int closed = dishMapper.closePresale(now, since);
        log.info("预售定时上下架完成: now={}, since={}, opened={}, closed={}", now, since, opened, closed);
        return JobResult.executed(JOB_NAME, opened + closed,
                "opened=" + opened + ",closed=" + closed);
    }
}
