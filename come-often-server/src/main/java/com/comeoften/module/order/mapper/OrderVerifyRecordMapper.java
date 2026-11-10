package com.comeoften.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.order.entity.OrderVerifyRecord;
import com.comeoften.module.order.vo.OrderVerifyRecordVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 核销记录。表上没有 member_id，顾客端分页用 order_id 关联 trade_order 过滤归属。
 */
public interface OrderVerifyRecordMapper extends BaseMapper<OrderVerifyRecord> {

    @Select("SELECT r.id AS id, r.order_id AS orderId, o.order_no AS orderNo, "
            + "r.store_id AS storeId, r.verify_code AS verifyCode, r.verify_channel AS verifyChannel, "
            + "r.verify_channel AS channelText, r.operator_type AS operatorType, "
            + "r.verify_time AS verifyTime, r.biz_id AS bizId, r.remark AS remark "
            + "FROM order_verify_record r INNER JOIN trade_order o ON o.id = r.order_id AND o.is_deleted = 0 "
            + "WHERE r.is_deleted = 0 AND r.store_id = #{storeId} AND o.member_id = #{memberId} "
            + "ORDER BY r.verify_time DESC, r.id DESC LIMIT #{offset}, #{size}")
    List<OrderVerifyRecordVO> pageForMember(@Param("memberId") Long memberId,
                                            @Param("storeId") Long storeId,
                                            @Param("offset") long offset,
                                            @Param("size") long size);

    @Select("SELECT COUNT(*) FROM order_verify_record r "
            + "INNER JOIN trade_order o ON o.id = r.order_id AND o.is_deleted = 0 "
            + "WHERE r.is_deleted = 0 AND r.store_id = #{storeId} AND o.member_id = #{memberId}")
    Long countForMember(@Param("memberId") Long memberId, @Param("storeId") Long storeId);

    @Select("SELECT r.id AS id, r.order_id AS orderId, o.order_no AS orderNo, "
            + "r.store_id AS storeId, r.verify_code AS verifyCode, r.verify_channel AS verifyChannel, "
            + "r.verify_channel AS channelText, r.operator_type AS operatorType, "
            + "r.verify_time AS verifyTime, r.biz_id AS bizId, r.remark AS remark "
            + "FROM order_verify_record r INNER JOIN trade_order o ON o.id = r.order_id AND o.is_deleted = 0 "
            + "WHERE r.id = #{recordId} AND r.is_deleted = 0 AND r.store_id = #{storeId} "
            + "AND o.member_id = #{memberId}")
    OrderVerifyRecordVO selectOwned(@Param("recordId") Long recordId,
                                    @Param("memberId") Long memberId,
                                    @Param("storeId") Long storeId);
}
