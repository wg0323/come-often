package com.comeoften.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 用户券 Mapper。领取、核销、退回一律走带旧状态条件的 UPDATE，禁止「先查再改」。
 */
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 现场核销：状态 0→1，影响行数为 0 表示已被并发核销，调用方返回「已使用」。
     */
    @Update("UPDATE user_coupon SET status = 1, use_time = #{useTime} "
            + "WHERE id = #{id} AND status = 0 AND is_deleted = 0")
    int markUsedForVerify(@Param("id") Long id, @Param("useTime") LocalDateTime useTime);

    /**
     * 下单锁定：同一券只能锁一次，且必须属于当前会员。
     */
    @Update("UPDATE user_coupon SET status = 1, use_time = #{useTime}, order_id = #{orderId} "
            + "WHERE id = #{id} AND member_id = #{memberId} AND status = 0 AND is_deleted = 0")
    int lockForOrder(@Param("id") Long id, @Param("memberId") Long memberId,
                     @Param("orderId") Long orderId, @Param("useTime") LocalDateTime useTime);

    /**
     * 订单取消退回：只退回被该订单锁定的券，已核销（order_id 为空）的券不会被误退。
     */
    @Update("UPDATE user_coupon SET status = 0, use_time = NULL, order_id = NULL "
            + "WHERE id = #{id} AND status = 1 AND order_id = #{orderId} AND is_deleted = 0")
    int releaseForOrder(@Param("id") Long id, @Param("orderId") Long orderId);

    /**
     * 过期置态，仅在券仍为可用时生效。
     */
    @Update("UPDATE user_coupon SET status = 2 "
            + "WHERE id = #{id} AND status = 0 AND is_deleted = 0")
    int markExpired(@Param("id") Long id);

    /**
     * 新人券幂等判据：同一会员对同一模板只发一次。
     */
    @Select("SELECT COUNT(*) FROM user_coupon WHERE member_id = #{memberId} "
            + "AND template_id = #{templateId} AND is_deleted = 0")
    Long countByMemberAndTemplate(@Param("memberId") Long memberId, @Param("templateId") Long templateId);
}
