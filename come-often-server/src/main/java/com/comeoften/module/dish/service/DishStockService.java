package com.comeoften.module.dish.service;

import com.comeoften.module.dish.dto.DishStockDTO;
import com.comeoften.module.dish.vo.DishStockVO;

import java.util.List;

/**
 * 菜品每日库存。DB 是最终事实源，改库存时同一事务写 DB 并重置 Redis key。
 */
public interface DishStockService {

    void apply(Long storeId, DishStockDTO dto);

    DishStockVO get(Long storeId, Long dishId);

    List<DishStockVO> list(Long storeId);

    /**
     * 按 DB 真实值重建当日全部 Redis 库存键，返回处理条数。
     */
    int rebuildToday(Long storeId);
}
