package com.comeoften.module.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单。状态 0-待审核、1-已退款、2-已拒绝，与订单 REFUNDING/REFUNDED 状态配套。
 */
@Data
@TableName("refund_record")
public class RefundRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String refundNo;

    private Long orderId;

    private Long paymentId;

    private BigDecimal refundAmount;

    private String reason;

    private Integer status;

    private Long auditUserId;

    private LocalDateTime refundTime;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
