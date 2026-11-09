package com.comeoften.module.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.reservation.entity.ReservationSlot;
import com.comeoften.module.reservation.vo.TableTypeStatVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

/**
 * 时段容量桶 Mapper。uk_slot 兜住重复生成；booked 的条件增减是 DB 侧并发兜底，
 * Redis Lua 只作前置判定，与菜品、券库存同一口径。
 */
public interface ReservationSlotMapper extends BaseMapper<ReservationSlot> {

    /**
     * 幂等生成：已存在则不重建（booked 保持），仅当没有任何预约时刷新容量，
     * 逻辑删除的行顺带复活。uk_slot 不含 is_deleted，不能直接 insert。
     */
    @Insert("INSERT INTO reservation_slot (id, store_id, reserve_date, time_slot, table_type, "
            + "capacity, booked, is_deleted, create_time, update_time) "
            + "VALUES (#{id}, #{storeId}, #{reserveDate}, #{timeSlot}, #{tableType}, "
            + "#{capacity}, 0, 0, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE "
            + "capacity = IF(#{capacity} >= booked, #{capacity}, capacity), "
            + "is_deleted = 0, update_time = NOW()")
    int upsert(@Param("id") Long id, @Param("storeId") Long storeId,
               @Param("reserveDate") LocalDate reserveDate, @Param("timeSlot") String timeSlot,
               @Param("tableType") String tableType, @Param("capacity") Integer capacity);

    /**
     * DB 兜底占用：影响行数为 0 表示容量已被并发抢完，调用方回补 Redis 后返回「该时段已满」。
     */
    @Update("UPDATE reservation_slot SET booked = booked + #{count}, update_time = NOW() "
            + "WHERE id = #{id} AND booked + #{count} <= capacity AND is_deleted = 0")
    int occupy(@Param("id") Long id, @Param("count") int count);

    /**
     * 取消、改期、超时释放容量。
     */
    @Update("UPDATE reservation_slot SET booked = booked - #{count}, update_time = NOW() "
            + "WHERE id = #{id} AND booked >= #{count} AND is_deleted = 0")
    int release(@Param("id") Long id, @Param("count") int count);

    @Select("SELECT id, store_id, reserve_date, time_slot, table_type, capacity, booked, is_deleted, "
            + "create_time, update_time FROM reservation_slot "
            + "WHERE store_id = #{storeId} AND reserve_date = #{reserveDate} "
            + "AND time_slot = #{timeSlot} AND table_type = #{tableType} AND is_deleted = 0")
    ReservationSlot selectUnique(@Param("storeId") Long storeId, @Param("reserveDate") LocalDate reserveDate,
                                 @Param("timeSlot") String timeSlot, @Param("tableType") String tableType);

    @Select("SELECT id, store_id, reserve_date, time_slot, table_type, capacity, booked, is_deleted, "
            + "create_time, update_time FROM reservation_slot "
            + "WHERE store_id = #{storeId} AND reserve_date = #{reserveDate} AND is_deleted = 0 "
            + "ORDER BY time_slot, table_type")
    List<ReservationSlot> selectByDate(@Param("storeId") Long storeId,
                                       @Param("reserveDate") LocalDate reserveDate);

    @Select("SELECT id, store_id, reserve_date, time_slot, table_type, capacity, booked, is_deleted, "
            + "create_time, update_time FROM reservation_slot "
            + "WHERE store_id = #{storeId} AND reserve_date BETWEEN #{startDate} AND #{endDate} "
            + "AND is_deleted = 0 ORDER BY reserve_date, time_slot, table_type")
    List<ReservationSlot> selectBetween(@Param("storeId") Long storeId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    /**
     * 桌型聚合：桌数即容量桶的 capacity，单桌容量只用于展示与人数校验参考。
     */
    @Select("SELECT table_type AS tableType, COUNT(*) AS tableCount, "
            + "MIN(capacity) AS minCapacity, MAX(capacity) AS maxCapacity "
            + "FROM store_table WHERE store_id = #{storeId} AND is_deleted = 0 GROUP BY table_type")
    List<TableTypeStatVO> statTableType(@Param("storeId") Long storeId);
}
