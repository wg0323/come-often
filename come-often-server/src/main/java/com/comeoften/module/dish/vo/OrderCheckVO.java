package com.comeoften.module.dish.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 下单前校验结果。校验不通过时由 Service 抛业务异常，不返回该对象。
 */
@Data
@Schema(description = "下单前校验结果")
public class OrderCheckVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否可下单；返回本对象时恒为 true")
    private Boolean orderable;

    @Schema(description = "整单金额，单位元")
    private BigDecimal totalAmount;

    @Schema(description = "是否含预定菜，含则强制 DINE_IN")
    private Boolean preorder;

    @Schema(description = "整单定金，单位元")
    private BigDecimal depositAmount;

    @Schema(description = "明细金额")
    private List<ItemVO> items;

    /**
     * 明细行。
     */
    @Data
    @Schema(description = "校验明细行")
    public static class ItemVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "菜品 ID")
        private Long dishId;

        @Schema(description = "菜品名称")
        private String dishName;

        @Schema(description = "份数")
        private Integer quantity;

        @Schema(description = "单位价，单位元")
        private BigDecimal unitPrice;

        @Schema(description = "行金额，单位元")
        private BigDecimal amount;

        @Schema(description = "本行是否按会员价计价")
        private Boolean memberPriceApplied;
    }
}
