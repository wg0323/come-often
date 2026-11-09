package com.comeoften.module.payment.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单视图。orderStatus 便于顾客端与后台确认退款是否进入审核态。
 */
@Data
@Schema(description = "退款单视图")
public class RefundVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "退款单 ID")
    private Long id;

    @Schema(description = "退款单号")
    private String refundNo;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单当前状态")
    private String orderStatus;

    @Schema(description = "退款金额，单位元；只退已收金额")
    private BigDecimal refundAmount;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "状态：0-待审核，1-已退款，2-已拒绝")
    private Integer status;

    @Schema(description = "状态说明")
    private String statusText;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "审核人 ID")
    private Long auditUserId;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "退款完成时间")
    private LocalDateTime refundTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "申请时间")
    private LocalDateTime createTime;
}
