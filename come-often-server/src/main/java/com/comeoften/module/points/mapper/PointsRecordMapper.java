package com.comeoften.module.points.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.points.entity.PointsRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 积分流水。唯一索引 uk_points_idempotent 兜住并发重复发放，
 * 冻结转实扣用「带旧状态条件」的 UPDATE，影响行数是唯一并发判据。
 */
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    String COLUMNS = "id, member_id, change_type, change_amount, biz_type, biz_id, balance_after, "
            + "expire_at, remark, is_deleted, create_time, update_time";

    @Select("SELECT COUNT(*) FROM points_record WHERE member_id = #{memberId} "
            + "AND biz_type = #{bizType} AND biz_id = #{bizId} AND is_deleted = 0")
    Long countByBiz(@Param("memberId") Long memberId, @Param("bizType") String bizType,
                    @Param("bizId") String bizId);

    @Select("SELECT " + COLUMNS + " FROM points_record WHERE member_id = #{memberId} "
            + "AND biz_type = #{bizType} AND biz_id = #{bizId} AND is_deleted = 0 LIMIT 1")
    PointsRecord selectByBiz(@Param("memberId") Long memberId, @Param("bizType") String bizType,
                             @Param("bizId") String bizId);

    /** 冻结转实扣：只有仍处于 FREEZE 的流水能被改，重复执行影响行数为 0。 */
    @Update("UPDATE points_record SET change_type = #{toType}, update_time = NOW() "
            + "WHERE member_id = #{memberId} AND biz_type = #{bizType} AND biz_id = #{bizId} "
            + "AND change_type = #{fromType} AND is_deleted = 0")
    int changeStatus(@Param("memberId") Long memberId, @Param("bizType") String bizType,
                     @Param("bizId") String bizId, @Param("fromType") String fromType,
                     @Param("toType") String toType);

    /**
     * 每日过期批处理扫描：到点且还没有过期流水的入账流水，按到期时间升序。
     *
     * <p>NOT EXISTS 是必须的：源行的 change_type 与 expire_at 不会被改写，只靠「已生成过期流水」
     * 来标记处理状态。少了这个条件，已处理的行会一直排在最前面，攒满一批后新到期的积分永远扫不到。</p>
     */
    @Select("SELECT " + COLUMNS + " FROM points_record r WHERE r.change_type = 'EARN' "
            + "AND r.expire_at IS NOT NULL AND r.expire_at <= #{now} AND r.is_deleted = 0 "
            + "AND NOT EXISTS (SELECT 1 FROM points_record e WHERE e.member_id = r.member_id "
            + "AND e.biz_type = r.biz_type AND e.biz_id = CONCAT(CAST(r.id AS CHAR), '#EXPIRE') "
            + "AND e.is_deleted = 0) "
            + "ORDER BY r.expire_at ASC LIMIT #{limit}")
    List<PointsRecord> selectDueForExpire(@Param("now") LocalDateTime now, @Param("limit") Integer limit);
}
