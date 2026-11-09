package com.comeoften.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.StockKeys;
import com.comeoften.module.dish.dto.DishStockDTO;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishStock;
import com.comeoften.module.dish.mapper.DishMapper;
import com.comeoften.module.dish.mapper.DishStockMapper;
import com.comeoften.module.dish.service.DishStockService;
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.vo.DishStockVO;
import com.comeoften.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 每日库存。DB 的 remain 是最终事实源。
 *
 * <p>写库用按业务唯一键的 UPDATE（可复活已逻辑删除记录）避免撞 uk_store_dish_date；
 * Redis 重置放到事务 afterCommit，DB 回滚时不会留下新值；库存 key 带 TTL，
 * 另有 rebuildToday 供每日 00:10 兜底任务按 DB 全量重建。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishStockServiceImpl implements DishStockService {

    private final DishStockMapper dishStockMapper;

    private final DishMapper dishMapper;

    private final StockService stockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(Long storeId, DishStockDTO dto) {
        Dish dish = requireDish(storeId, dto.getDishId());
        LocalDate today = LocalDate.now();
        DishStock existing = loadToday(storeId, dish.getId());
        boolean soldOut = Boolean.TRUE.equals(dto.getSoldOut());
        int total = dto.getTotal() == null
                ? (existing == null ? 0 : existing.getTotal()) : dto.getTotal();
        int remain = soldOut ? 0 : total;
        if (dishStockMapper.updateExisting(storeId, dish.getId(), today, total, remain) == 0) {
            DishStock record = new DishStock();
            record.setStoreId(storeId);
            record.setDishId(dish.getId());
            record.setStockDate(today);
            record.setTotal(total);
            record.setRemain(remain);
            try {
                dishStockMapper.insert(record);
            } catch (DuplicateKeyException e) {
                // 并发插入时由赢家写入，这里改为覆盖，DB 仍是最终事实源
                dishStockMapper.updateExisting(storeId, dish.getId(), today, total, remain);
            }
        }
        scheduleResetAfterCommit(storeId, dish.getId(), today, remain);
        log.info("每日库存已更新: storeId={}, dishId={}, total={}, remain={}, soldOut={}",
                storeId, dish.getId(), total, remain, soldOut);
    }

    @Override
    public DishStockVO get(Long storeId, Long dishId) {
        Dish dish = requireDish(storeId, dishId);
        return toVO(dish, loadToday(storeId, dishId));
    }

    @Override
    public List<DishStockVO> list(Long storeId) {
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStoreId, storeId)
                .eq(Dish::getStatus, DishConstants.STATUS_ON_SHELF)
                .orderByAsc(Dish::getSort));
        if (dishes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> dishIds = dishes.stream().map(Dish::getId).collect(Collectors.toList());
        Map<Long, DishStock> stockMap = dishStockMapper.selectList(new LambdaQueryWrapper<DishStock>()
                        .eq(DishStock::getStoreId, storeId)
                        .eq(DishStock::getStockDate, LocalDate.now())
                        .in(DishStock::getDishId, dishIds))
                .stream().collect(Collectors.toMap(DishStock::getDishId,
                        stock -> stock, (left, right) -> left, HashMap::new));
        List<DishStockVO> result = new ArrayList<>();
        for (Dish dish : dishes) {
            result.add(toVO(dish, stockMap.get(dish.getId())));
        }
        return result;
    }

    @Override
    public int rebuildToday(Long storeId) {
        LocalDate today = LocalDate.now();
        List<DishStock> stocks = dishStockMapper.selectList(new LambdaQueryWrapper<DishStock>()
                .eq(DishStock::getStoreId, storeId)
                .eq(DishStock::getStockDate, today));
        for (DishStock stock : stocks) {
            stockService.reset(StockKeys.dish(storeId, stock.getDishId(), today), stock.getRemain());
        }
        log.info("已按 DB 重建当日菜品库存键: storeId={}, count={}", storeId, stocks.size());
        return stocks.size();
    }

    private void scheduleResetAfterCommit(Long storeId, Long dishId, LocalDate date, int remain) {
        final String key = StockKeys.dish(storeId, dishId, date);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stockService.reset(key, remain);
            }
        });
    }

    private DishStockVO toVO(Dish dish, DishStock stock) {
        DishStockVO vo = new DishStockVO();
        vo.setDishId(dish.getId());
        vo.setDishName(dish.getDishName());
        vo.setStockDate(LocalDate.now().toString());
        if (stock == null) {
            vo.setConfigured(Boolean.FALSE);
            vo.setTotal(0);
            vo.setRemain(0);
            vo.setSoldOut(Boolean.FALSE);
            return vo;
        }
        vo.setConfigured(Boolean.TRUE);
        vo.setTotal(stock.getTotal());
        vo.setRemain(stock.getRemain());
        vo.setSoldOut(stock.getRemain() <= 0);
        return vo;
    }

    private DishStock loadToday(Long storeId, Long dishId) {
        return dishStockMapper.selectOne(new LambdaQueryWrapper<DishStock>()
                .eq(DishStock::getStoreId, storeId)
                .eq(DishStock::getDishId, dishId)
                .eq(DishStock::getStockDate, LocalDate.now()));
    }

    private Dish requireDish(Long storeId, Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null || !Objects.equals(dish.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.DISH_NOT_FOUND);
        }
        return dish;
    }
}
