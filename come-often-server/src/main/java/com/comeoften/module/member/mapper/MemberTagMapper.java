package com.comeoften.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.member.entity.MemberTag;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员标签。沉睡判定直接下推到 SQL：
 * 「最近 {@code from} 之后没有支付成功订单」且「不是刚注册、从未下过单的新客」才算沉睡。
 */
public interface MemberTagMapper extends BaseMapper<MemberTag> {

    /**
     * 待打沉睡标的会员：已存在历史支付单或注册时间早于阈值，且阈值之后没有支付成功订单。
     */
    @Select("SELECT m.id FROM member m WHERE m.is_deleted = 0 "
            + "AND NOT EXISTS (SELECT 1 FROM member_tag t WHERE t.member_id = m.id "
            + "  AND t.tag_code = #{tagCode} AND t.is_deleted = 0) "
            + "AND NOT EXISTS (SELECT 1 FROM trade_order o WHERE o.member_id = m.id "
            + "  AND o.paid_time >= #{from} AND o.is_deleted = 0) "
            + "AND (m.create_time < #{from} OR EXISTS (SELECT 1 FROM trade_order p "
            + "  WHERE p.member_id = m.id AND p.paid_time IS NOT NULL AND p.is_deleted = 0)) "
            + "ORDER BY m.id LIMIT #{limit}")
    List<Long> selectSleepingCandidates(@Param("tagCode") String tagCode,
                                        @Param("from") LocalDateTime from,
                                        @Param("limit") int limit);

    /**
     * 已回访、需要摘掉沉睡标的会员标签行 ID。
     */
    @Select("SELECT t.id FROM member_tag t WHERE t.tag_code = #{tagCode} AND t.is_deleted = 0 "
            + "AND EXISTS (SELECT 1 FROM trade_order o WHERE o.member_id = t.member_id "
            + "  AND o.paid_time >= #{from} AND o.is_deleted = 0) "
            + "ORDER BY t.id LIMIT #{limit}")
    List<Long> selectRevivedTagIds(@Param("tagCode") String tagCode,
                                   @Param("from") LocalDateTime from,
                                   @Param("limit") int limit);

    /**
     * 逻辑删除后重建同一标签会撞 uk_member_tag，用原生 UPDATE 复活。
     */
    @Update("UPDATE member_tag SET is_deleted = 0, expire_time = NULL, update_time = NOW() "
            + "WHERE member_id = #{memberId} AND tag_code = #{tagCode}")
    int revive(@Param("memberId") Long memberId, @Param("tagCode") String tagCode);

    /**
     * 按标签取会员 ID，供后台会员列表按标签筛选与沉睡券发放。
     */
    @Select("SELECT member_id FROM member_tag WHERE tag_code = #{tagCode} AND is_deleted = 0 "
            + "ORDER BY member_id LIMIT #{limit}")
    List<Long> selectMemberIdsByTag(@Param("tagCode") String tagCode, @Param("limit") int limit);
}
