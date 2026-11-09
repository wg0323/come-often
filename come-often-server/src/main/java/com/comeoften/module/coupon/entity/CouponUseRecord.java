package com.comeoften.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 券核销记录。operator_user_id 为核销员工，计划第七章要求正确落库供审计。
 * uk_user_coupon 唯一索引兜住并发重复核销。
 */
@Data
@TableName("coupon_use_record")
public class CouponUseRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userCouponId;
    private Long memberId;
    private Long storeId;
    private Long orderId;
    private BigDecimal discountAmount;
    private Long operatorUserId;
    private String verifyChannel;
    private LocalDateTime verifyTime;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
