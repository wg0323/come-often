package com.comeoften.module.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.reservation.entity.Reservation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约单 Mapper。所有状态写都是带旧状态条件的 UPDATE，影响行数为唯一并发判据；
 * 禁止「先查再改」。
 */
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 按预约单号查询。reserve_no 有唯一索引，签到码解析后靠它定位。
     */
    @Select("SELECT id, reserve_no, store_id, member_id, reserve_date, time_slot, table_type, "
            + "people_count, contact_name, contact_phone, remark, status, table_id, checkin_user_id, "
            + "checkin_time, is_deleted, create_time, update_time "
            + "FROM reservation WHERE reserve_no = #{reserveNo} AND is_deleted = 0")
    Reservation selectByReserveNo(@Param("reserveNo") String reserveNo);

    /**
     * 兜底扫描：已过「预约时刻 + 超时分钟」仍未签到 / 未取消的预约。
     * 只作延迟消息丢失时的补偿，与消费者走同一个 markNoShow。
     */
    @Select("SELECT id, reserve_no, store_id, member_id, reserve_date, time_slot, table_type, "
            + "people_count, contact_name, contact_phone, remark, status, table_id, checkin_user_id, "
            + "checkin_time, is_deleted, create_time, update_time "
            + "FROM reservation WHERE status IN ('PENDING','CONFIRMED') AND is_deleted = 0 "
            + "AND TIMESTAMP(reserve_date, time_slot) <= #{cutoff} "
            + "ORDER BY reserve_date, time_slot LIMIT #{limit}")
    List<Reservation> selectOverdue(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);

    /**
     * 指定包间的占用判定：该门店该日期时段仍有效的预约已经锁了哪些具体桌台。
     * 取消 / 超时 / 拒绝后的预约不在 IN 列表里，包间自动释放。
     */
    @Select("SELECT DISTINCT table_id FROM reservation "
            + "WHERE store_id = #{storeId} AND reserve_date = #{reserveDate} AND time_slot = #{timeSlot} "
            + "AND table_id IS NOT NULL AND is_deleted = 0 "
            + "AND status IN ('PENDING','CONFIRMED','CHECKED_IN')")
    List<Long> selectOccupiedTableIds(@Param("storeId") Long storeId,
                                      @Param("reserveDate") LocalDate reserveDate,
                                      @Param("timeSlot") String timeSlot);

    /**
     * 后台确认：只允许 PENDING→CONFIRMED。
     */
    @Update("UPDATE reservation SET status = 'CONFIRMED', update_time = NOW() "
            + "WHERE id = #{id} AND status = 'PENDING' AND is_deleted = 0")
    int confirm(@Param("id") Long id);

    /**
     * 取消：待确认与已确认都可取消，影响行数为 0 表示已被并发改掉。
     */
    @Update("UPDATE reservation SET status = 'CANCELLED', update_time = NOW() "
            + "WHERE id = #{id} AND status IN ('PENDING','CONFIRMED') AND is_deleted = 0")
    int cancel(@Param("id") Long id);

    /**
     * 超时未签到置 NO_SHOW，条件与取消一致。
     */
    @Update("UPDATE reservation SET status = 'NO_SHOW', update_time = NOW() "
            + "WHERE id = #{id} AND status IN ('PENDING','CONFIRMED') AND is_deleted = 0")
    int markNoShow(@Param("id") Long id);

    /**
     * 到店签到。tableId 为空时保留原有绑定，不清空。
     */
    @Update("UPDATE reservation SET status = 'CHECKED_IN', checkin_user_id = #{userId}, "
            + "checkin_time = #{checkinTime}, table_id = IFNULL(#{tableId}, table_id), update_time = NOW() "
            + "WHERE id = #{id} AND status IN ('PENDING','CONFIRMED') AND is_deleted = 0")
    int checkIn(@Param("id") Long id, @Param("userId") Long userId,
                @Param("checkinTime") LocalDateTime checkinTime, @Param("tableId") Long tableId);

    /**
     * 改期。把旧日期/时段/桌型一起放进条件，挡住并发重复改期。
     */
    @Update("UPDATE reservation SET reserve_date = #{reserveDate}, time_slot = #{timeSlot}, "
            + "table_type = #{tableType}, update_time = NOW() "
            + "WHERE id = #{id} AND status IN ('PENDING','CONFIRMED') AND is_deleted = 0 "
            + "AND reserve_date = #{oldDate} AND time_slot = #{oldTimeSlot} AND table_type = #{oldTableType}")
    int reschedule(@Param("id") Long id, @Param("reserveDate") LocalDate reserveDate,
                   @Param("timeSlot") String timeSlot, @Param("tableType") String tableType,
                   @Param("oldDate") LocalDate oldDate, @Param("oldTimeSlot") String oldTimeSlot,
                   @Param("oldTableType") String oldTableType);
}
