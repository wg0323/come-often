package com.comeoften.module.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 自提 / 到店订单主表。金额字段为下单时快照，禁止事后重算。
 */
@Data
@TableName("trade_order")
public class TradeOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long storeId;
    private Long memberId;
    private String orderType;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal pointsAmount;
    /** 会员折扣独立列，不折进 discountAmount（会污染 M5 券 ROI）。 */
    private BigDecimal memberDiscount;
    /** 储值余额抵扣，与 OrderVO.balanceAmount（到店待付尾款）不是一个含义。 */
    private BigDecimal balanceAmount;
    private BigDecimal depositAmount;
    /** 在线支付应付额，禁止事后重算；M10 净额需按 B5 口径加回 balanceAmount。 */
    private BigDecimal payAmount;
    private Integer pointsUsed;
    private Long couponId;
    private Long tableId;
    /** 团购套餐 ID，普通订单为空；取消回补套餐库存与 #BUNDLE 核销幂等键都靠它识别。 */
    private Long bundleId;
    private String pickupCode;
    private LocalDate bizDate;
    private LocalDateTime pickupTime;
    private LocalDateTime dineInTime;
    private String remark;
    private LocalDateTime paidTime;
    private LocalDateTime completedTime;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
