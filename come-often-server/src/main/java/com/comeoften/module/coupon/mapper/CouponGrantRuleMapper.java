package com.comeoften.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.coupon.entity.CouponGrantRule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 券发放规则。{@code selectByCode} 不过滤 is_deleted：唯一索引不含该列，复活要走原生 UPDATE。
 */
public interface CouponGrantRuleMapper extends BaseMapper<CouponGrantRule> {

    @Select("SELECT * FROM coupon_grant_rule WHERE store_id = #{storeId} AND rule_code = #{ruleCode}")
    CouponGrantRule selectByCode(@Param("storeId") Long storeId, @Param("ruleCode") String ruleCode);

    /**
     * 今天的生日会员：member.birthday 的月日与当前月日一致。
     */
    @Select("SELECT id FROM member WHERE is_deleted = 0 AND birthday IS NOT NULL "
            + "AND MONTH(birthday) = #{month} AND DAY(birthday) = #{day} ORDER BY id LIMIT #{limit}")
    List<Long> selectBirthdayMemberIds(@Param("month") int month, @Param("day") int day,
                                       @Param("limit") int limit);
}
