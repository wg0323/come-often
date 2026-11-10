package com.comeoften.module.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项。
 */
@Data
@Schema(description = "订单列表项")
public class OrderSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单类型：PICKUP/DINE_IN")
    private String orderType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "状态中文说明")
    private String statusText;

    @Schema(description = "商品总额，单位元")
    private BigDecimal totalAmount;

    @Schema(description = "在线支付应付额，单位元")
    private BigDecimal payAmount;

    @Schema(description = "会员折扣金额，单位元")
    private BigDecimal memberDiscountAmount;

    @Schema(description = "储值余额抵扣金额，单位元；与到店尾款不是同一含义")
    private BigDecimal balanceDeductAmount;

    @Schema(description = "定金，单位元")
    private BigDecimal depositAmount;

    @Schema(description = "取餐码 / 到店核销码")
    private String pickupCode;

    @Schema(description = "菜品摘要")
    private String itemSummary;

    @Schema(description = "是否可评价：订单已完成且尚未评价，由服务端判定")
    private Boolean canReview;

    @Schema(description = "是否已评价")
    private Boolean reviewed;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "下单时间")
    private LocalDateTime createTime;
}
