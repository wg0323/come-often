package com.comeoften.module.reservation.service.impl;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.ReservationKeys;
import com.comeoften.config.ReservationProperties;
import com.comeoften.module.reservation.entity.ReservationSlot;
import com.comeoften.module.reservation.mapper.ReservationSlotMapper;
import com.comeoften.module.reservation.service.ReservationSlotService;
import com.comeoften.module.reservation.vo.TableTypeStatVO;
import com.comeoften.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时段容量实现。口径与菜品、券库存完全一致：Redis 只作快速判定，
 * reservation_slot.booked 是最终事实源；DB 影响行数为 0 时立即回补 Redis。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationSlotServiceImpl implements ReservationSlotService {

    private static final int MAX_GENERATE_DAYS = 30;

    private static final int SLOT_COUNT = 1;

    private final ReservationSlotMapper slotMapper;

    private final StockService stockService;

    private final ReservationProperties reservationProperties;

    private final IdentifierGenerator identifierGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generate(Long storeId, Integer days) {
        int span = days == null || days <= 0 ? reservationProperties.getDaysAhead() : days;
        span = Math.min(span, MAX_GENERATE_DAYS);
        Map<String, Integer> capacityByType = capacityByType(storeId);
        LocalDate start = LocalDate.now();
        int processed = 0;
        for (int offset = 0; offset < span; offset++) {
            LocalDate reserveDate = start.plusDays(offset);
            for (String timeSlot : reservationProperties.getTimeSlots()) {
                for (String tableType : reservationProperties.getTableTypes()) {
                    Integer capacity = capacityByType.get(tableType);
                    if (capacity == null || capacity <= 0) {
                        continue;
                    }
                    Long id = identifierGenerator.nextId(new ReservationSlot()).longValue();
                    slotMapper.upsert(id, storeId, reserveDate, timeSlot, tableType, capacity);
                    processed++;
                }
            }
        }
        log.info("预约时段已生成: storeId={}, days={}, processed={}", storeId, span, processed);
        return processed;
    }

    @Override
    public int rebuildRedis(Long storeId) {
        LocalDate start = LocalDate.now();
        List<ReservationSlot> slots = slotMapper.selectBetween(storeId, start,
                start.plusDays(Math.min(reservationProperties.getDaysAhead(), MAX_GENERATE_DAYS)));
        for (ReservationSlot slot : slots) {
            stockService.reset(ReservationKeys.slotCapacity(slot.getId()),
                    slot.getBooked() == null ? 0L : slot.getBooked());
        }
        log.info("预约容量键已按 DB 重建: storeId={}, keys={}", storeId, slots.size());
        return slots.size();
    }

    @Override
    public ReservationSlot requireSlot(Long storeId, LocalDate reserveDate, String timeSlot, String tableType) {
        ReservationSlot slot = findSlot(storeId, reserveDate, timeSlot, tableType);
        if (slot == null) {
            throw new BizException(ErrorCode.RESERVATION_SLOT_NOT_FOUND);
        }
        return slot;
    }

    @Override
    public ReservationSlot findSlot(Long storeId, LocalDate reserveDate, String timeSlot, String tableType) {
        return slotMapper.selectUnique(storeId, reserveDate, timeSlot, tableType);
    }

    @Override
    public List<ReservationSlot> listByDate(Long storeId, LocalDate reserveDate) {
        return slotMapper.selectByDate(storeId, reserveDate);
    }

    @Override
    public List<ReservationSlot> listBetween(Long storeId, LocalDate startDate, LocalDate endDate) {
        return slotMapper.selectBetween(storeId, startDate, endDate);
    }

    @Override
    public boolean occupy(ReservationSlot slot) {
        String key = ReservationKeys.slotCapacity(slot.getId());
        ensureRedisKey(slot, key);
        List<String> occupiedKeys = new ArrayList<>();
        registerRollbackCompensation(occupiedKeys);
        if (!stockService.tryOccupy(key, SLOT_COUNT, slot.getCapacity())) {
            return false;
        }
        if (slotMapper.occupy(slot.getId(), SLOT_COUNT) == 0) {
            // DB 已被并发抢完：只补本次 Redis 预占，DB 未发生变化
            safeRollbackRedis(key);
            return false;
        }
        occupiedKeys.add(key);
        return true;
    }

    @Override
    public boolean releaseInTransaction(Long slotId) {
        int released = slotMapper.release(slotId, SLOT_COUNT);
        if (released == 0) {
            log.warn("预约时段释放未命中，DB booked 已为 0: slotId={}", slotId);
            return false;
        }
        String key = ReservationKeys.slotCapacity(slotId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeRollbackRedis(key);
            return true;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeRollbackRedis(key);
            }
        });
        return true;
    }

    @Override
    public List<TableTypeStatVO> statTableType(Long storeId) {
        return slotMapper.statTableType(storeId);
    }

    private Map<String, Integer> capacityByType(Long storeId) {
        Map<String, Integer> capacityByType = new HashMap<>(8);
        for (TableTypeStatVO stat : slotMapper.statTableType(storeId)) {
            capacityByType.put(stat.getTableType(), stat.getTableCount() == null ? 0 : stat.getTableCount());
        }
        return capacityByType;
    }

    /**
     * key 缺失（首次预约 / Redis 重启 / 已过期）时按 DB booked 初始化，不从 0 起算。
     * 用 SETNX 而不是 SET：并发请求同时发现 key 缺失时，后到者不会覆盖先到者已 incrby 的值。
     */
    private void ensureRedisKey(ReservationSlot slot, String key) {
        stockService.initIfAbsent(key, slot.getBooked() == null ? 0L : slot.getBooked());
    }

    /**
     * 事务回滚时把已预占的 Redis 容量补回去；DB 由 Spring 回滚，两条路径不会重复回补。
     */
    private void registerRollbackCompensation(List<String> occupiedKeys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_ROLLED_BACK
                        && status != TransactionSynchronization.STATUS_UNKNOWN) {
                    return;
                }
                for (String key : occupiedKeys) {
                    try {
                        stockService.rollback(key, SLOT_COUNT, true);
                    } catch (RuntimeException e) {
                        log.error("预约容量 Redis 回补失败，等待重建兜底: key={}", key, e);
                    }
                }
            }
        });
    }

    private void safeRollbackRedis(String key) {
        try {
            stockService.rollback(key, SLOT_COUNT, true);
        } catch (RuntimeException e) {
            log.error("预约容量 Redis 回补失败，等待重建兜底: key={}", key, e);
        }
    }
}
