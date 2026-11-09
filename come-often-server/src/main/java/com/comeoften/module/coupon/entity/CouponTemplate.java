package com.comeoften.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板。remain 是 DB 兜底库存（最终事实源），Redis 只作快速判定层。
 */
@Data
@TableName("coupon_template")
public class CouponTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storeId;
    private String templateName;
    private String couponType;
    private BigDecimal faceValue;
    private BigDecimal discountRate;
    private BigDecimal thresholdAmount;
    private Long dishId;
    private Integer total;
    private Integer remain;
    private Integer validDays;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer stackable;
    private Integer status;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
