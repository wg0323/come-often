package com.comeoften.module.balance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.balance.entity.BalanceRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 储值流水。幂等判据是唯一索引 {@code uk_biz_idempotent}，这里只提供读方法。
 */
public interface BalanceRecordMapper extends BaseMapper<BalanceRecord> {

    @Select("SELECT * FROM balance_record WHERE member_id = #{memberId} AND biz_type = #{bizType} "
            + "AND biz_id = #{bizId} AND is_deleted = 0 ORDER BY id DESC LIMIT 1")
    BalanceRecord selectByBiz(@Param("memberId") Long memberId, @Param("bizType") String bizType,
                              @Param("bizId") String bizId);

    @Select("SELECT COUNT(*) FROM balance_record WHERE member_id = #{memberId} AND biz_type = #{bizType} "
            + "AND biz_id = #{bizId}")
    long countByBiz(@Param("memberId") Long memberId, @Param("bizType") String bizType,
                    @Param("bizId") String bizId);
}
