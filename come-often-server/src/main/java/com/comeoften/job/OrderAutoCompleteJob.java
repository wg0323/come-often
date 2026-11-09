package com.comeoften.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.module.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单自动完成（计划 3.10 的 00:05 任务）：{@code READY} 超过配置时长仍无人取餐的订单，
 * 以 SYSTEM 身份推进到 {@code COMPLETED}，走与商家端「确认取餐」完全相同的
 * {@link OrderService#transition}，因此状态日志、积分事件、等级刷新都只有一份实现。
 *
 * <p><b>时间判据是 {@code trade_order.update_time}</b>：订单进入 READY 后，除了状态迁移不会再写该行
 * （取餐码在创建时生成、金额快照不再变），DDL 的 {@code ON UPDATE CURRENT_TIMESTAMP} 让
 * 「迁移到 READY 的那一刻」就等于该行的 update_time。**这个判据成立的前提是 READY 期间无人再写该行**
 * ——将来若给订单加备注编辑、退款标记之类的写操作，必须改成独立的时间列，否则计时会被重置。</p>
 *
 * <p>一次全局扫描（不按门店循环）：{@code idx_store_status} 提供 status 前缀，数据量是单店日级。
 * 只扫 {@code status='READY'}，取消 / 退款 / 制作中的单不受影响；单条失败只跳过该条。</p>
 *
 * <p>不做「自动完成后退钱退积分」：未取餐订单钱已收、餐已出，退款是商家的人工判断，
 * 任务只负责把流程收口，不替商家做资金决策。</p>
 */
@Slf4j
@Component
public class OrderAutoCompleteJob {

    public static final String JOB_NAME = "order-auto-complete";

    private static final long LOCK_TTL_SECONDS = 600L;

    private final TradeOrderMapper tradeOrderMapper;

    private final OrderService orderService;

    private final long autoCompleteHours;

    private final int batchSize;

    public OrderAutoCompleteJob(TradeOrderMapper tradeOrderMapper, OrderService orderService,
                                @Value("${comeoften.order.auto-complete-hours:24}") long autoCompleteHours,
                                @Value("${comeoften.order.auto-complete-batch-size:200}") int batchSize) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.orderService = orderService;
        this.autoCompleteHours = autoCompleteHours;
        this.batchSize = batchSize;
    }

    /**
     * 扫描一次超时未取餐订单，返回本次推进到 COMPLETED 的条数。
     */
    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult complete() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(autoCompleteHours);
        List<TradeOrder> stale = tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getStatus, OrderStatus.READY.name())
                .lt(TradeOrder::getUpdateTime, cutoff)
                .orderByAsc(TradeOrder::getUpdateTime)
                .last("LIMIT " + batchSize));
        int completed = 0;
        for (TradeOrder order : stale) {
            if (completeOne(order)) {
                completed++;
            }
        }
        log.info("订单自动完成扫描结束: cutoff={}, scanned={}, completed={}",
                cutoff, stale.size(), completed);
        return JobResult.executed(JOB_NAME, completed,
                "hours=" + autoCompleteHours + ",scanned=" + stale.size() + ",completed=" + completed);
    }

    private boolean completeOne(TradeOrder order) {
        try {
            orderService.transition(order.getStoreId(), order.getOrderNo(), OrderStatus.COMPLETED.name(),
                    "超时未取餐自动完成", OperatorType.SYSTEM, null);
            return true;
        } catch (RuntimeException e) {
            // 并发下可能已被商家端确认取餐，或订单刚被退款改走状态：跳过并保留 error 便于核对
            log.warn("订单自动完成跳过: orderNo={}, reason={}", order.getOrderNo(), e.getMessage());
            return false;
        }
    }
}
