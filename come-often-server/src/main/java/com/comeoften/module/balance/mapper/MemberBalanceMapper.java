package com.comeoften.module.balance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.balance.entity.MemberBalance;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 储值账户。余额写操作全部是带余额条件的 UPDATE，影响行数为 0 即并发下余额不足。
 */
public interface MemberBalanceMapper extends BaseMapper<MemberBalance> {

    @Select("SELECT id, member_id, balance, total_recharge, total_gift, total_consume, is_deleted, "
            + "create_time, update_time FROM member_balance WHERE member_id = #{memberId} AND is_deleted = 0")
    MemberBalance selectByMemberId(@Param("memberId") Long memberId);

    /** 下单抵扣：余额不足时影响行数为 0。 */
    @Update("UPDATE member_balance SET balance = balance - #{amount}, "
            + "total_consume = total_consume + #{amount}, update_time = NOW() "
            + "WHERE member_id = #{memberId} AND is_deleted = 0 AND balance >= #{amount}")
    int deduct(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);

    /** 充值本金入账。本金与赠送分两条 SQL，流水里的「变动后余额」才能逐条对齐。 */
    @Update("UPDATE member_balance SET balance = balance + #{amount}, "
            + "total_recharge = total_recharge + #{amount}, update_time = NOW() "
            + "WHERE member_id = #{memberId} AND is_deleted = 0")
    int addPrincipal(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);

    /** 充值赠送入账。 */
    @Update("UPDATE member_balance SET balance = balance + #{gift}, "
            + "total_gift = total_gift + #{gift}, update_time = NOW() "
            + "WHERE member_id = #{memberId} AND is_deleted = 0")
    int addGift(@Param("memberId") Long memberId, @Param("gift") BigDecimal gift);

    /** 取消 / 超时 / 退款退回：余额加回并冲减累计消费，与抵扣行正负相抵。 */
    @Update("UPDATE member_balance SET balance = balance + #{amount}, "
            + "total_consume = GREATEST(total_consume - #{amount}, 0), update_time = NOW() "
            + "WHERE member_id = #{memberId} AND is_deleted = 0")
    int restore(@Param("memberId") Long memberId, @Param("amount") BigDecimal amount);
}
