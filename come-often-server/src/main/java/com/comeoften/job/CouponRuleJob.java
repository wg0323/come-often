package com.comeoften.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.module.coupon.constant.CouponRuleType;
import com.comeoften.module.coupon.service.CouponRuleService;
import com.comeoften.module.store.entity.Store;
import com.comeoften.module.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 券发放规则任务（M14 B3）：每日 00:40 执行生日券与沉睡唤醒券，按门店逐个跑。
 *
 * <p>新客券与复购券是事件驱动（首单支付成功 / 订单完成），不在本任务里扫：
 * 用定时任务判「首单」需要全量扫订单，收益远低于在事件里顺手判一次。</p>
 *
 * <p>幂等由 {@code user_coupon.grant_biz_id} 保证，重跑只会得到 {@code granted=0}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponRuleJob {

    public static final String JOB_NAME = "coupon-rule";

    private static final long LOCK_TTL_SECONDS = 300L;

    private final CouponRuleService couponRuleService;

    private final StoreMapper storeMapper;

    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult grant() {
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<Store>());
        int birthday = 0;
        int sleeping = 0;
        int failed = 0;
        for (Store store : stores) {
            try {
                birthday += couponRuleService.run(store.getId(), CouponRuleType.BIRTHDAY.name());
                sleeping += couponRuleService.run(store.getId(), CouponRuleType.SLEEPING.name());
            } catch (RuntimeException e) {
                // 单店异常不中断其他门店：规则数据是运营配置，坏一条不该让整批发不出去
                failed++;
                log.error("券发放规则执行失败: storeId={}", store.getId(), e);
            }
        }
        int total = birthday + sleeping;
        log.info("券发放规则任务完成: birthday={}, sleeping={}, failedStore={}",
                birthday, sleeping, failed);
        return JobResult.executed(JOB_NAME, total,
                "birthday=" + birthday + ",sleeping=" + sleeping + ",failed=" + failed);
    }
}
