package com.comeoften.module.dish.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 金额试算结果。页面直接展示，不参与计算。
 */
@Data
@Schema(description = "金额试算结果")
public class DishQuoteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品 ID")
    private Long dishId;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "基础价，单位元；会员下单且菜品设了会员价时为会员价")
    private BigDecimal basePrice;

    @Schema(description = "会员价，单位元；未设置为空")
    private BigDecimal memberPrice;

    @Schema(description = "本次是否按会员价计价")
    private Boolean memberPriceApplied;

    @Schema(description = "规格加价合计，单位元")
    private BigDecimal specAmount;

    @Schema(description = "加料加价合计，单位元")
    private BigDecimal addonAmount;

    @Schema(description = "单位价 = 基础价 + 规格加价 + 加料加价")
    private BigDecimal unitPrice;

    @Schema(description = "份数")
    private Integer quantity;

    @Schema(description = "行金额 = 单位价 × 份数")
    private BigDecimal totalPrice;

    @Schema(description = "是否预定菜")
    private Boolean preorder;

    @Schema(description = "整单定金，预定菜为 200.00，非预定菜为 0.00")
    private BigDecimal depositAmount;
}
