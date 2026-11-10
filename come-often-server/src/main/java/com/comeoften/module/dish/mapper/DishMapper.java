package com.comeoften.module.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.dish.entity.Dish;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 菜品 Mapper。预售库存的扣减 / 回补与定时上下架都用带条件的原生 UPDATE。
 */
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 预售剩余库存 CAS 扣减；presale_stock 为空（不限量）的菜品不会命中。
     */
    @Update("UPDATE dish SET presale_stock = presale_stock - #{quantity}, update_time = NOW() "
            + "WHERE id = #{id} AND is_deleted = 0 AND presale_stock >= #{quantity} "
            + "AND presale_start_time IS NOT NULL")
    int deductPresaleStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * 订单取消 / 超时回补预售余量。判据必须与扣减完全一致，
     * 否则「只有库存、没有开售时间」的历史脏数据会被兑换单取消凭空刷大配额。
     */
    @Update("UPDATE dish SET presale_stock = presale_stock + #{quantity}, update_time = NOW() "
            + "WHERE id = #{id} AND is_deleted = 0 AND presale_stock IS NOT NULL "
            + "AND presale_start_time IS NOT NULL")
    int restorePresaleStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * 到点开售：只处理「开售时间刚落在最近一次扫描窗口内」且仍下架、未过结束时间的菜品。
     * 限定窗口是为了不让每分钟的定时任务在整段预售期内反复覆盖人工下架。
     */
    @Update("UPDATE dish SET status = 1, update_time = NOW() "
            + "WHERE is_deleted = 0 AND status = 0 AND presale_start_time IS NOT NULL "
            + "AND presale_start_time BETWEEN #{since} AND #{now} "
            + "AND (presale_end_time IS NULL OR presale_end_time > #{now})")
    int openPresale(@Param("now") LocalDateTime now, @Param("since") LocalDateTime since);

    /**
     * 预售结束自动下架：只处理结束时间刚落在扫描窗口内、仍上架的菜品。
     */
    @Update("UPDATE dish SET status = 0, update_time = NOW() "
            + "WHERE is_deleted = 0 AND status = 1 AND presale_end_time IS NOT NULL "
            + "AND presale_end_time BETWEEN #{since} AND #{now}")
    int closePresale(@Param("now") LocalDateTime now, @Param("since") LocalDateTime since);
}
