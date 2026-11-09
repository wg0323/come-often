package com.comeoften.module.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.dish.entity.DishStock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * 菜品每日库存 Mapper。uk_store_dish_date 唯一索引不含 is_deleted，
 * 用一条原生 UPDATE 覆盖「已存在」与「曾被逻辑删除」两种情况，避免查后插的竞态。
 */
public interface DishStockMapper extends BaseMapper<DishStock> {

    /**
     * 按业务唯一键更新库存并复活已删记录，返回影响行数；0 表示需要插入。
     */
    @Update("UPDATE dish_stock SET total = #{total}, remain = #{remain}, "
            + "is_deleted = 0, update_time = NOW() "
            + "WHERE store_id = #{storeId} AND dish_id = #{dishId} AND stock_date = #{stockDate}")
    int updateExisting(@Param("storeId") Long storeId, @Param("dishId") Long dishId,
                       @Param("stockDate") LocalDate stockDate,
                       @Param("total") Integer total, @Param("remain") Integer remain);

    /**
     * 下单扣减 DB 余量，带 remain >= count 条件兜底；返回 0 表示并发下已不足。
     */
    @Update("UPDATE dish_stock SET remain = remain - #{count}, update_time = NOW() "
            + "WHERE store_id = #{storeId} AND dish_id = #{dishId} AND stock_date = #{stockDate} "
            + "AND is_deleted = 0 AND remain >= #{count}")
    int deduct(@Param("storeId") Long storeId, @Param("dishId") Long dishId,
               @Param("stockDate") LocalDate stockDate, @Param("count") Integer count);

    /**
     * 取消 / 超时回补 DB 余量，超时取消与用户取消共用。
     */
    @Update("UPDATE dish_stock SET remain = remain + #{count}, update_time = NOW() "
            + "WHERE store_id = #{storeId} AND dish_id = #{dishId} AND stock_date = #{stockDate} "
            + "AND is_deleted = 0")
    int restore(@Param("storeId") Long storeId, @Param("dishId") Long dishId,
                @Param("stockDate") LocalDate stockDate, @Param("count") Integer count);
}
