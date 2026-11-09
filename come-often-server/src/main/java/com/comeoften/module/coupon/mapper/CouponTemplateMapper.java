package com.comeoften.module.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.coupon.entity.CouponTemplate;
import com.comeoften.module.coupon.vo.CouponRoiVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 券模板 Mapper。remain 的条件扣减是 DB 侧并发兜底，Redis Lua 只作前置判定。
 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    /**
     * 库存兜底扣减：影响行数为 0 表示已被并发领完，调用方回补 Redis 后返回「已领完」。
     */
    @Update("UPDATE coupon_template SET remain = remain - 1 "
            + "WHERE id = #{id} AND remain >= 1 AND is_deleted = 0")
    int deductRemain(@Param("id") Long id);

    /**
     * 发放失败或订单取消时回补 DB 余量。
     */
    @Update("UPDATE coupon_template SET remain = remain + #{count} "
            + "WHERE id = #{id} AND is_deleted = 0")
    int restoreRemain(@Param("id") Long id, @Param("count") int count);

    /**
     * 券 ROI。用券订单通过 user_coupon.order_id 关联 trade_order，只统计已支付订单的营收；
     * 现场扫码核销不绑定订单，计入使用数但不计入营收。
     */
    @Select("<script>"
            + "SELECT t.id AS templateId, t.template_name AS templateName, t.coupon_type AS couponType, "
            + "t.total AS total, t.remain AS remain, "
            + "(SELECT COUNT(*) FROM user_coupon uc WHERE uc.template_id = t.id "
            + "AND uc.is_deleted = 0) AS receivedCount, "
            + "(SELECT COUNT(*) FROM user_coupon uc WHERE uc.template_id = t.id "
            + "AND uc.status = 1 AND uc.is_deleted = 0) AS usedCount, "
            + "(SELECT COUNT(DISTINCT o.id) FROM user_coupon uc "
            + "INNER JOIN trade_order o ON o.id = uc.order_id "
            + "WHERE uc.template_id = t.id AND uc.is_deleted = 0 AND o.is_deleted = 0) AS orderCount, "
            + "(SELECT IFNULL(SUM(o.discount_amount), 0) FROM user_coupon uc "
            + "INNER JOIN trade_order o ON o.id = uc.order_id "
            + "WHERE uc.template_id = t.id AND uc.is_deleted = 0 AND o.is_deleted = 0) AS discountAmount, "
            + "(SELECT IFNULL(SUM(o.pay_amount), 0) FROM user_coupon uc "
            + "INNER JOIN trade_order o ON o.id = uc.order_id "
            + "WHERE uc.template_id = t.id AND uc.is_deleted = 0 AND o.is_deleted = 0 "
            + "AND o.paid_time IS NOT NULL) AS revenueAmount "
            + "FROM coupon_template t "
            + "WHERE t.store_id = #{storeId} AND t.is_deleted = 0 "
            + "<if test='templateId != null'>AND t.id = #{templateId} </if>"
            + "ORDER BY t.create_time DESC"
            + "</script>")
    List<CouponRoiVO> roiByStore(@Param("storeId") Long storeId, @Param("templateId") Long templateId);
}
