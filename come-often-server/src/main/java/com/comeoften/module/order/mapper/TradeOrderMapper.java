package com.comeoften.module.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.order.entity.TradeOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单主表 Mapper。
 */
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    /**
     * 取餐码查重不带 is_deleted 条件：uk_store_date_pickup 是物理唯一索引，
     * 逻辑删除的订单仍然占用码，必须一并算进来。
     */
    @Select("SELECT COUNT(*) FROM trade_order WHERE store_id = #{storeId} "
            + "AND biz_date = #{bizDate} AND pickup_code = #{pickupCode}")
    Long countByPickupCode(@Param("storeId") Long storeId, @Param("bizDate") LocalDate bizDate,
                           @Param("pickupCode") String pickupCode);

    /**
     * 会员已支付订单数，供 M5 判定「首单」后发新人券；支付成功事件消费时本次订单已计入。
     */
    @Select("SELECT COUNT(*) FROM trade_order WHERE member_id = #{memberId} "
            + "AND paid_time IS NOT NULL AND is_deleted = 0")
    Long countPaidByMember(@Param("memberId") Long memberId);

    /**
     * 会员在指定时间区间内的有效实付总额，供 M6 计算年度消费额后判定等级。
     * 已取消与已退款订单不计入，避免退款刷等级。
     */
    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM trade_order WHERE member_id = #{memberId} "
            + "AND paid_time >= #{from} AND paid_time < #{to} "
            + "AND status NOT IN ('CANCELLED', 'REFUNDED') AND is_deleted = 0")
    BigDecimal sumPaidAmount(@Param("memberId") Long memberId, @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    /**
     * 门店当日取餐码反查订单，供 M9 商家端扫取餐码核销。只取未删除记录：
     * 已删除订单的码仍占着物理唯一索引，但不应再被取餐。
     */
    @Select("SELECT * FROM trade_order WHERE store_id = #{storeId} AND biz_date = #{bizDate} "
            + "AND pickup_code = #{pickupCode} AND is_deleted = 0 ORDER BY id DESC LIMIT 1")
    TradeOrder selectByPickupCode(@Param("storeId") Long storeId, @Param("bizDate") LocalDate bizDate,
                                  @Param("pickupCode") String pickupCode);

    /**
     * 门店当日营收：按 paid_time 落在区间内的实付金额汇总，取消与退款订单不计入。
     */
    @Select("SELECT IFNULL(SUM(pay_amount), 0) FROM trade_order WHERE store_id = #{storeId} "
            + "AND paid_time >= #{from} AND paid_time < #{to} "
            + "AND status NOT IN ('CANCELLED', 'REFUNDED') AND is_deleted = 0")
    BigDecimal sumStorePaidAmount(@Param("storeId") Long storeId, @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);
}
