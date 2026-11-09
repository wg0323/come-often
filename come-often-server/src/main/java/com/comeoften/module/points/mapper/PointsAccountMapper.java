package com.comeoften.module.points.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.points.entity.PointsAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 积分账户。所有余额写操作都是带余额条件的 UPDATE，影响行数为 0 即代表并发下余额不足，
 * 与 M5 券锁定的 CAS 口径一致。
 */
public interface PointsAccountMapper extends BaseMapper<PointsAccount> {

    @Select("SELECT id, member_id, balance, total_earned, total_used, is_deleted, create_time, update_time "
            + "FROM points_account WHERE member_id = #{memberId} AND is_deleted = 0")
    PointsAccount selectByMemberId(@Param("memberId") Long memberId);

    /** 冻结或过期扣减：余额不足时影响行数为 0，调用方必须据此判定失败。 */
    @Update("UPDATE points_account SET balance = balance - #{points}, update_time = NOW() "
            + "WHERE member_id = #{memberId} AND is_deleted = 0 AND balance >= #{points}")
    int deduct(@Param("memberId") Long memberId, @Param("points") Integer points);

    /** 取消 / 超时回滚：把冻结的积分加回余额。 */
    @Update("UPDATE points_account SET balance = balance + #{points}, update_time = NOW() "
            + "WHERE member_id = #{memberId} AND is_deleted = 0")
    int restore(@Param("memberId") Long memberId, @Param("points") Integer points);

    /** 积分入账：余额与累计获得同时增加。 */
    @Update("UPDATE points_account SET balance = balance + #{points}, total_earned = total_earned + #{points}, "
            + "update_time = NOW() WHERE member_id = #{memberId} AND is_deleted = 0")
    int addEarned(@Param("memberId") Long memberId, @Param("points") Integer points);

    /** 冻结转实扣：余额在冻结时已扣减，这里只累计已使用。 */
    @Update("UPDATE points_account SET total_used = total_used + #{points}, update_time = NOW() "
            + "WHERE member_id = #{memberId} AND is_deleted = 0")
    int addUsed(@Param("memberId") Long memberId, @Param("points") Integer points);
}
