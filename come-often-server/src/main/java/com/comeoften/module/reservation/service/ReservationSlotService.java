package com.comeoften.module.reservation.service;

import com.comeoften.module.reservation.entity.ReservationSlot;
import com.comeoften.module.reservation.vo.TableTypeStatVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 时段库存。容量桶按「日期 × 时段 × 桌型」预生成，capacity 由 store_table 聚合；
 * 扣减复用 StockService（Redis Lua 前置 + DB booked 条件更新兜底）。
 */
public interface ReservationSlotService {

    /**
     * 幂等生成未来 N 天的时段容量桶。重复调用不会覆盖已占用时段的容量。
     *
     * @return 本次处理的「日期 × 时段 × 桌型」组合数
     */
    int generate(Long storeId, Integer days);

    /**
     * 按 DB 真实 booked 重建 Redis 容量键，供 Redis 重启或验收前对齐。
     *
     * @return 重建的 key 数
     */
    int rebuildRedis(Long storeId);

    /**
     * 查询指定时段，不存在抛 30042。
     */
    ReservationSlot requireSlot(Long storeId, LocalDate reserveDate, String timeSlot, String tableType);

    /**
     * 查询指定时段，不存在返回 null。
     */
    ReservationSlot findSlot(Long storeId, LocalDate reserveDate, String timeSlot, String tableType);

    List<ReservationSlot> listByDate(Long storeId, LocalDate reserveDate);

    List<ReservationSlot> listBetween(Long storeId, LocalDate startDate, LocalDate endDate);

    /**
     * Redis 预占 + DB 条件占用。返回 false 表示容量已满（Redis 已回补）。
     * 事务回滚时自动回补本次 Redis 预占。
     */
    boolean occupy(ReservationSlot slot);

    /**
     * 事务内释放 DB 容量，提交后回补 Redis；无事务时立即回补。
     */
    boolean releaseInTransaction(Long slotId);

    List<TableTypeStatVO> statTableType(Long storeId);
}
