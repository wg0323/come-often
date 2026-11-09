package com.comeoften.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.StockKeys;
import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.module.coupon.constant.CouponTemplateStatus;
import com.comeoften.module.coupon.entity.CouponTemplate;
import com.comeoften.module.coupon.mapper.CouponTemplateMapper;
import com.comeoften.module.dish.service.DishStockService;
import com.comeoften.module.reservation.service.ReservationSlotService;
import com.comeoften.module.store.entity.Store;
import com.comeoften.module.store.mapper.StoreMapper;
import com.comeoften.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 每日 00:10 按 DB 真实值重建三类库存键（计划 3.5 / 3.10）。
 *
 * <p>正确性不依赖 Redis 持久化：菜品每日库存取 {@code dish_stock.remain}、券模板库存取
 * {@code coupon_template.remain}、预约时段容量取 {@code reservation_slot.booked}。任务只写 Redis，
 * 不改任何 DB 数据。</p>
 *
 * <p>关于并发：{@code reset} 是无条件 {@code SET}，理论上会覆盖同一瞬间发生的预扣。这**不会造成超卖**
 * ——DB 侧是条件扣减（`remain >= n`、`booked + n <= capacity`、`deductRemain` 命中判据），
 * Redis 只是前置判定层；最坏结果是「Redis 显示有货但下单失败一次」。凌晨重建时几乎没有并发，
 * 这个代价可以接受。</p>
 *
 * <p>只重建「当日菜品库存行」与「发放中的券模板」：没配当日库存的菜品按不限量处理（没有键），
 * 停用模板的键靠 48 小时 TTL 自然淘汰。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockRebuildJob {

    public static final String JOB_NAME = "stock-rebuild";

    private static final long LOCK_TTL_SECONDS = 600L;

    private final StoreMapper storeMapper;

    private final DishStockService dishStockService;

    private final CouponTemplateMapper couponTemplateMapper;

    private final ReservationSlotService reservationSlotService;

    private final StockService stockService;

    /**
     * 重建一次全部库存键，返回重建的 key 总数。
     */
    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult rebuild() {
        List<Store> stores = storeMapper.selectList(new LambdaQueryWrapper<>());
        int dishKeys = 0;
        int slotKeys = 0;
        int failedStores = 0;
        for (Store store : stores) {
            // 单店异常不拖累其他门店：下面两个 rebuild 都是按 DB 覆盖写，重跑一次就能补齐
            try {
                dishKeys += dishStockService.rebuildToday(store.getId());
                slotKeys += reservationSlotService.rebuildRedis(store.getId());
            } catch (RuntimeException e) {
                failedStores++;
                log.error("库存键重建失败，跳过该门店: storeId={}", store.getId(), e);
            }
        }
        int couponKeys = 0;
        try {
            couponKeys = rebuildCouponStock();
        } catch (RuntimeException e) {
            log.error("券模板库存键重建失败，本次跳过: ", e);
        }
        int total = dishKeys + couponKeys + slotKeys;
        log.info("库存键已按 DB 重建: stores={}, dishKeys={}, couponKeys={}, slotKeys={}, failedStores={}",
                stores.size(), dishKeys, couponKeys, slotKeys, failedStores);
        return JobResult.executed(JOB_NAME, total,
                "dish=" + dishKeys + ",coupon=" + couponKeys + ",slot=" + slotKeys
                        + ",failedStores=" + failedStores);
    }

    private int rebuildCouponStock() {
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getStatus, CouponTemplateStatus.ISSUING.getCode()));
        for (CouponTemplate template : templates) {
            int remain = Objects.isNull(template.getRemain()) ? 0 : template.getRemain();
            stockService.reset(StockKeys.coupon(template.getId()), remain);
        }
        return templates.size();
    }
}
