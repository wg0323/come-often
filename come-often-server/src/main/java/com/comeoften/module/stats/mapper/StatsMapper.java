package com.comeoften.module.stats.mapper;

import com.comeoften.module.stats.vo.StatsCouponRoiRow;
import com.comeoften.module.stats.vo.StatsDailyRefundRow;
import com.comeoften.module.stats.vo.StatsDailyRevenueRow;
import com.comeoften.module.stats.vo.StatsHighValueMemberRow;
import com.comeoften.module.stats.vo.StatsHotDishRow;
import com.comeoften.module.stats.vo.StatsMemberAggRow;
import com.comeoften.module.stats.vo.StatsReservationSlotVO;
import com.comeoften.module.stats.vo.StatsReservationStatusRow;
import com.comeoften.module.stats.vo.StatsRetentionRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表聚合 SQL。只读，不改任何业务状态。
 *
 * <p>口径三条，改 SQL 前先看这里：</p>
 * <ol>
 *   <li>营收一律按 {@code paid_time} 落在区间取数，退款按<b>原订单的 paid_time</b> 冲减，
 *       不按 refund_time，保证「营收 / 订单数 / 客单价」三个数对齐且不出现某天营收为负。</li>
 *   <li>金额基础集是 {@code status <> 'CANCELLED'}；有效订单数再排除 {@code REFUNDED}（整单退款不进分母）。</li>
 *   <li>trade_order 没有 paid_time 单列索引，过滤走 idx_store_status 的 store_id 前缀后回表筛时间，
 *       单店量级可接受；触发加索引的条件记在 deploy/init.md 差异 137。</li>
 * </ol>
 */
public interface StatsMapper {

    /**
     * 按支付自然日的已支付金额与有效订单数。
     */
    @Select("SELECT DATE(paid_time) AS statDate, "
            + "IFNULL(SUM(CASE WHEN status <> 'CANCELLED' THEN pay_amount ELSE 0 END), 0) AS paidAmount, "
            + "IFNULL(SUM(CASE WHEN status NOT IN ('CANCELLED', 'REFUNDED') THEN 1 ELSE 0 END), 0) AS orderCount "
            + "FROM trade_order WHERE store_id = #{storeId} AND is_deleted = 0 "
            + "AND paid_time >= #{from} AND paid_time < #{to} "
            + "GROUP BY DATE(paid_time) ORDER BY DATE(paid_time)")
    List<StatsDailyRevenueRow> dailyPaid(@Param("storeId") Long storeId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    /**
     * 按原订单支付自然日的已退款金额，只算审核通过的退款单。
     */
    @Select("SELECT DATE(o.paid_time) AS statDate, IFNULL(SUM(r.refund_amount), 0) AS refundAmount "
            + "FROM refund_record r INNER JOIN trade_order o ON o.id = r.order_id "
            + "WHERE o.store_id = #{storeId} AND o.is_deleted = 0 AND o.status <> 'CANCELLED' "
            + "AND r.status = 1 AND r.is_deleted = 0 "
            + "AND o.paid_time >= #{from} AND o.paid_time < #{to} "
            + "GROUP BY DATE(o.paid_time) ORDER BY DATE(o.paid_time)")
    List<StatsDailyRefundRow> dailyRefund(@Param("storeId") Long storeId,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);

    /**
     * 热销排行：按菜品聚合，同一菜品不同规格合并；菜名取 order_item 下单快照。
     */
    @Select("SELECT i.dish_id AS dishId, MAX(i.dish_name) AS dishName, "
            + "IFNULL(SUM(i.quantity), 0) AS quantity, IFNULL(SUM(i.amount), 0) AS amount, "
            + "COUNT(DISTINCT i.order_id) AS orderCount "
            + "FROM order_item i INNER JOIN trade_order o ON o.id = i.order_id "
            + "WHERE o.store_id = #{storeId} AND o.is_deleted = 0 AND i.is_deleted = 0 "
            + "AND o.status NOT IN ('CANCELLED', 'REFUNDED') "
            + "AND o.paid_time >= #{from} AND o.paid_time < #{to} "
            + "GROUP BY i.dish_id ORDER BY quantity DESC, amount DESC, i.dish_id ASC LIMIT #{topN}")
    List<StatsHotDishRow> hotDish(@Param("storeId") Long storeId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("topN") int topN);

    /**
     * 区间券 ROI。领取数按 receive_time、核销数按 use_time 落在区间；
     * 用券订单通过 trade_order.coupon_id = user_coupon.id 关联，金额与退款都取同一批订单。
     */
    @Select("SELECT t.id AS templateId, t.template_name AS templateName, t.coupon_type AS couponType, "
            + "t.total AS total, IFNULL(recv.receivedCount, 0) AS receivedCount, "
            + "IFNULL(used.usedCount, 0) AS usedCount, IFNULL(ord.orderCount, 0) AS orderCount, "
            + "IFNULL(ord.grossPaid, 0) AS grossPaid, IFNULL(ord.discountAmount, 0) AS discountAmount, "
            + "IFNULL(rf.refundAmount, 0) AS refundAmount "
            + "FROM coupon_template t "
            + "LEFT JOIN (SELECT template_id, COUNT(*) AS receivedCount FROM user_coupon "
            + "  WHERE is_deleted = 0 AND store_id = #{storeId} "
            + "  AND receive_time >= #{from} AND receive_time < #{to} "
            + "  GROUP BY template_id) recv ON recv.template_id = t.id "
            + "LEFT JOIN (SELECT template_id, COUNT(*) AS usedCount FROM user_coupon "
            + "  WHERE is_deleted = 0 AND store_id = #{storeId} AND status = 1 "
            + "  AND use_time >= #{from} AND use_time < #{to} "
            + "  GROUP BY template_id) used ON used.template_id = t.id "
            + "LEFT JOIN (SELECT uc.template_id, COUNT(DISTINCT o.id) AS orderCount, "
            + "  SUM(o.pay_amount) AS grossPaid, SUM(o.discount_amount) AS discountAmount "
            + "  FROM trade_order o INNER JOIN user_coupon uc ON uc.id = o.coupon_id "
            + "  WHERE o.store_id = #{storeId} AND o.is_deleted = 0 AND o.status <> 'CANCELLED' "
            + "  AND o.paid_time >= #{from} AND o.paid_time < #{to} GROUP BY uc.template_id) ord "
            + "  ON ord.template_id = t.id "
            + "LEFT JOIN (SELECT uc.template_id, SUM(r.refund_amount) AS refundAmount "
            + "  FROM refund_record r INNER JOIN trade_order o ON o.id = r.order_id "
            + "  INNER JOIN user_coupon uc ON uc.id = o.coupon_id "
            + "  WHERE o.store_id = #{storeId} AND o.is_deleted = 0 AND o.status <> 'CANCELLED' "
            + "  AND r.status = 1 AND r.is_deleted = 0 "
            + "  AND o.paid_time >= #{from} AND o.paid_time < #{to} GROUP BY uc.template_id) rf "
            + "  ON rf.template_id = t.id "
            + "WHERE t.store_id = #{storeId} AND t.is_deleted = 0 ORDER BY t.create_time DESC")
    List<StatsCouponRoiRow> couponRoi(@Param("storeId") Long storeId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    /**
     * 复购：分母是区间内有有效支付单的会员，分子是其中支付两单及以上的会员。
     */
    @Select("SELECT COUNT(*) AS paidMemberCount, "
            + "IFNULL(SUM(CASE WHEN t.orderCount >= 2 THEN 1 ELSE 0 END), 0) AS repurchaseMemberCount "
            + "FROM (SELECT o.member_id AS memberId, COUNT(*) AS orderCount FROM trade_order o "
            + "  WHERE o.store_id = #{storeId} AND o.is_deleted = 0 "
            + "  AND o.status NOT IN ('CANCELLED', 'REFUNDED') "
            + "  AND o.paid_time >= #{from} AND o.paid_time < #{to} "
            + "  GROUP BY o.member_id) t")
    StatsMemberAggRow memberAgg(@Param("storeId") Long storeId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    /**
     * 次日留存：cohort 是区间内下首单的会员，且首单次日已完整落在区间内（首单日 + 2 天 ≤ 区间结束）。
     * 「次日」按自然日算（DATE(firstPaid) + 1 天到 + 2 天），不是首单时刻 +24 小时：
     * 22:00 首单的会员，第二天 09:00 再消费必须算留存，否则留存率会被营业时段结构带偏。
     */
    @Select("SELECT COUNT(*) AS cohortSize, "
            + "IFNULL(SUM(CASE WHEN EXISTS (SELECT 1 FROM trade_order n WHERE n.member_id = f.memberId "
            + "  AND n.store_id = #{storeId} AND n.is_deleted = 0 AND n.status <> 'CANCELLED' "
            + "  AND n.paid_time >= DATE_ADD(DATE(f.firstPaid), INTERVAL 1 DAY) "
            + "  AND n.paid_time < DATE_ADD(DATE(f.firstPaid), INTERVAL 2 DAY)) THEN 1 ELSE 0 END), 0) "
            + "AS retainedCount "
            + "FROM (SELECT o.member_id AS memberId, MIN(o.paid_time) AS firstPaid FROM trade_order o "
            + "  WHERE o.store_id = #{storeId} AND o.is_deleted = 0 "
            + "  AND o.status NOT IN ('CANCELLED', 'REFUNDED') GROUP BY o.member_id) f "
            + "WHERE f.firstPaid >= #{from} AND f.firstPaid < #{to} "
            + "AND DATE_ADD(DATE(f.firstPaid), INTERVAL 2 DAY) <= #{to}")
    StatsRetentionRow retention(@Param("storeId") Long storeId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    /**
     * 高价值用户：区间累计净消费降序，phone 明文出库，出接口前脱敏。
     */
    @Select("SELECT o.member_id AS memberId, m.nickname AS nickname, m.phone AS phone, "
            + "l.level_name AS levelName, COUNT(*) AS orderCount, "
            + "IFNULL(SUM(o.pay_amount), 0) AS paidAmount, "
            + "IFNULL(SUM(rf.refundAmount), 0) AS refundAmount, MAX(o.paid_time) AS lastPaidTime "
            + "FROM trade_order o LEFT JOIN member m ON m.id = o.member_id "
            + "LEFT JOIN member_level l ON l.id = m.level_id "
            + "LEFT JOIN (SELECT order_id, SUM(refund_amount) AS refundAmount FROM refund_record "
            + "  WHERE status = 1 AND is_deleted = 0 GROUP BY order_id) rf ON rf.order_id = o.id "
            + "WHERE o.store_id = #{storeId} AND o.is_deleted = 0 AND o.status <> 'CANCELLED' "
            + "AND o.paid_time >= #{from} AND o.paid_time < #{to} "
            + "GROUP BY o.member_id, m.nickname, m.phone, l.level_name "
            + "ORDER BY (SUM(o.pay_amount) - IFNULL(SUM(rf.refundAmount), 0)) DESC, o.member_id ASC "
            + "LIMIT #{topN}")
    List<StatsHighValueMemberRow> highValueMember(@Param("storeId") Long storeId,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("to") LocalDateTime to,
                                                  @Param("topN") int topN);

    /**
     * 某日各状态预约单数。
     */
    @Select("SELECT status AS status, COUNT(*) AS count FROM reservation "
            + "WHERE store_id = #{storeId} AND reserve_date = #{reserveDate} AND is_deleted = 0 "
            + "GROUP BY status")
    List<StatsReservationStatusRow> reservationCounts(@Param("storeId") Long storeId,
                                                      @Param("reserveDate") LocalDate reserveDate);

    /**
     * 某日时段占用，各桌型相加后按时段升序。
     */
    @Select("SELECT time_slot AS timeSlot, IFNULL(SUM(capacity), 0) AS capacity, "
            + "IFNULL(SUM(booked), 0) AS booked FROM reservation_slot "
            + "WHERE store_id = #{storeId} AND reserve_date = #{reserveDate} AND is_deleted = 0 "
            + "GROUP BY time_slot ORDER BY time_slot")
    List<StatsReservationSlotVO> slotOccupancy(@Param("storeId") Long storeId,
                                               @Param("reserveDate") LocalDate reserveDate);
}
