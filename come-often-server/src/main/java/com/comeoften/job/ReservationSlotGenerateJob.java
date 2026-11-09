package com.comeoften.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.module.reservation.service.ReservationSlotService;
import com.comeoften.module.store.entity.Store;
import com.comeoften.module.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 每日 00:00 按门店滚动预生成未来 N 天预约时段，补齐 M7 遗留的「每日点一次按钮」欠账。
 *
 * <p>生成走 {@code uk_slot} 幂等 upsert：只在新容量不小于已占桌数时刷新 capacity，
 * 从不重置 booked，因此重复执行安全，已有预约的时段不会被冲掉。</p>
 *
 * <p>遍历 store 表而不是取默认门店，多门店时每店独立生成；单店异常隔离，不影响其他门店。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationSlotGenerateJob {

    public static final String JOB_NAME = "reservation-slot-generate";

    private static final long LOCK_TTL_SECONDS = 600L;

    private final StoreMapper storeMapper;

    private final ReservationSlotService reservationSlotService;

    /**
     * 滚动生成一次全部门店的时段库存，返回本次处理的组合数。
     */
    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult generate() {
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<>());
        int processed = 0;
        int failedStores = 0;
        for (Store store : stores) {
            // 单店失败不影响其他门店：下一次调度或手动补跑会用幂等 upsert 补齐
            try {
                processed += reservationSlotService.generate(store.getId(), null);
            } catch (RuntimeException e) {
                failedStores++;
                log.error("预约时段生成失败，跳过该门店: storeId={}", store.getId(), e);
            }
        }
        log.info("预约时段已滚动生成: stores={}, slots={}, failedStores={}",
                stores.size(), processed, failedStores);
        return JobResult.executed(JOB_NAME, processed,
                "stores=" + stores.size() + ",failedStores=" + failedStores);
    }
}
